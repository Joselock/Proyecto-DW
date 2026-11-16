package com.hospital.service.JPA;

import com.hospital.model.EntidadesJPA.User;
import com.hospital.repository.JPA.UserRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       JdbcTemplate jdbcTemplate) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(readOnly = true)
    public User autenticar(String username, String passwordPlano) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BadCredentialsException("Credenciales inválidas"));

        if (!passwordEncoder.matches(passwordPlano, user.getPassword())) {
            throw new BadCredentialsException("Credenciales inválidas");
        }
        return user;
    }

    @Transactional(readOnly = true)
    public User autenticarPorEmail(String email, String passwordPlano) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Credenciales inválidas"));
        if (!passwordEncoder.matches(passwordPlano, user.getPassword())) {
            throw new BadCredentialsException("Credenciales inválidas");
        }
        return user;
    }

    @Transactional(readOnly = true)
    public User obtenerPorUsername(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }

    /**
     * Devuelve el rol efectivo del usuario. Los usuarios creados antes de
     * existir la columna role se consideran MEDICO hasta que un administrador
     * los promueva.
     */
    public String rolEfectivo(User user) {
        String role = user.getRole();
        return role == null || role.isBlank() ? "MEDICO" : role.toUpperCase(Locale.ROOT);
    }

    /**
     * Crea una cuenta de acceso.
     *
     * El primer usuario del sistema se crea como ADMIN y no se vincula a un
     * médico. A partir del segundo, la cuenta es MEDICO y debe indicar el
     * médico existente al que pertenece (codMed sin usuario).
     */
    @Transactional
    public void registrar(String username, String email, String password, String codMed) {
        String normalizedUsername = username == null ? "" : username.trim();
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        String normalizedCodMed = codMed == null ? "" : codMed.trim();

        if (normalizedUsername.isEmpty() || normalizedEmail.isEmpty()
                || password == null || password.isBlank()) {
            throw new IllegalArgumentException("Completa los campos obligatorios.");
        }
        if (userRepository.existsByUsername(normalizedUsername)) {
            throw new IllegalStateException("Ese nombre de usuario ya está registrado.");
        }
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalStateException("Ese correo electrónico ya está registrado.");
        }

        // El primer usuario que se registra queda como administrador para
        // poder configurar el resto del sistema. Los siguientes son médicos
        // y deben quedar vinculados a una ficha profesional existente.
        boolean esPrimerUsuario = userRepository.count() == 0;

        if (!esPrimerUsuario) {
            if (normalizedCodMed.isEmpty()) {
                throw new IllegalArgumentException(
                        "Selecciona el médico al que pertenece la cuenta.");
            }

            Integer disponibles = jdbcTemplate.queryForObject(
                    "SELECT count(*) FROM public.\"Medico\" "
                            + "WHERE \"codMed\" = ? AND username IS NULL",
                    Integer.class, normalizedCodMed);

            if (disponibles == null || disponibles == 0) {
                throw new IllegalStateException(
                        "El médico seleccionado no existe o ya tiene una cuenta.");
            }
        }

        User user = new User();
        user.setUsername(normalizedUsername);
        user.setEmail(normalizedEmail);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(esPrimerUsuario ? "ADMIN" : "MEDICO");

        // saveAndFlush garantiza que la fila de "user" exista antes de que la
        // FK de "Medico" la referencie.
        userRepository.saveAndFlush(user);

        if (!esPrimerUsuario) {
            int vinculados = jdbcTemplate.update(
                    "UPDATE public.\"Medico\" SET username = ? "
                            + "WHERE \"codMed\" = ? AND username IS NULL",
                    normalizedUsername, normalizedCodMed);

            if (vinculados == 0) {
                throw new IllegalStateException(
                        "El médico seleccionado acaba de ser vinculado a otra cuenta.");
            }
        }
    }

    /**
     * Datos del médico vinculado, que también son el perfil del usuario.
     * Devuelve null si la cuenta no tiene médico (por ejemplo, un ADMIN).
     */
    @Transactional(readOnly = true)
    public Map<String, Object> perfilMedico(String username) {
        if (username == null || username.isBlank()) {
            return null;
        }

        String sql = """
                SELECT m."codMed" AS "codMed",
                       m."nomMed" AS "nombre",
                       m."espMed" AS "especialidad",
                       m."dirMed" AS "direccion",
                       m."telMed" AS "telefono",
                       m."codUni" AS "codUni",
                       u."nomUni" AS "unidad",
                       d."codHos" AS "codHos",
                       h."nomHos" AS "hospital"
                FROM public."Medico" m
                INNER JOIN public."Unidad" u ON u."codUni" = m."codUni"
                INNER JOIN public."Departamento" d ON d."codDep" = u."codDep"
                INNER JOIN public."Hospital" h ON h."codHos" = d."codHos"
                WHERE m.username = ?
                LIMIT 1
                """;

        List<Map<String, Object>> filas = jdbcTemplate.query(sql, (rs, rowNum) -> {
            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("codMed", rs.getString("codMed"));
            fila.put("nombre", rs.getString("nombre"));
            fila.put("especialidad", rs.getString("especialidad"));
            fila.put("direccion", rs.getString("direccion"));
            fila.put("telefono", rs.getString("telefono"));
            fila.put("codUni", rs.getString("codUni"));
            fila.put("unidad", rs.getString("unidad"));
            fila.put("codHos", rs.getString("codHos"));
            fila.put("hospital", rs.getString("hospital"));
            return fila;
        }, username);

        return filas.isEmpty() ? null : filas.get(0);
    }

    /**
     * Opciones para el registro: hospitales, unidades y médicos que todavía
     * no tienen cuenta. Si el sistema no tiene usuarios, indica que el
     * próximo registro será el administrador.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> opcionesRegistro() {
        Map<String, Object> data = new LinkedHashMap<>();
        boolean primerUsuario = userRepository.count() == 0;
        data.put("primerUsuario", primerUsuario);

        if (primerUsuario) {
            data.put("hospitales", List.of());
            data.put("unidades", List.of());
            data.put("medicos", List.of());
            return data;
        }

        data.put("hospitales", jdbcTemplate.query("""
                SELECT DISTINCT h."codHos" AS "codHos", h."nomHos" AS "nomHos"
                FROM public."Medico" m
                INNER JOIN public."Unidad" u ON u."codUni" = m."codUni"
                INNER JOIN public."Departamento" d ON d."codDep" = u."codDep"
                INNER JOIN public."Hospital" h ON h."codHos" = d."codHos"
                WHERE m.username IS NULL
                ORDER BY h."nomHos"
                """, (rs, rowNum) -> {
            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("codHos", rs.getString("codHos"));
            fila.put("nomHos", rs.getString("nomHos"));
            return fila;
        }));

        data.put("unidades", jdbcTemplate.query("""
                SELECT DISTINCT u."codUni" AS "codUni",
                                u."nomUni" AS "nomUni",
                                d."codHos" AS "codHos"
                FROM public."Medico" m
                INNER JOIN public."Unidad" u ON u."codUni" = m."codUni"
                INNER JOIN public."Departamento" d ON d."codDep" = u."codDep"
                WHERE m.username IS NULL
                ORDER BY u."nomUni"
                """, (rs, rowNum) -> {
            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("codUni", rs.getString("codUni"));
            fila.put("nomUni", rs.getString("nomUni"));
            fila.put("codHos", rs.getString("codHos"));
            return fila;
        }));

        data.put("medicos", jdbcTemplate.query("""
                SELECT m."codMed" AS "codMed",
                       m."nomMed" AS "nomMed",
                       m."espMed" AS "espMed",
                       m."codUni" AS "codUni"
                FROM public."Medico" m
                WHERE m.username IS NULL
                ORDER BY m."nomMed"
                """, (rs, rowNum) -> {
            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("codMed", rs.getString("codMed"));
            fila.put("nomMed", rs.getString("nomMed"));
            fila.put("espMed", rs.getString("espMed"));
            fila.put("codUni", rs.getString("codUni"));
            return fila;
        }));

        return data;
    }

    /**
     * Actualiza el perfil del usuario. Como el perfil son los datos del
     * médico vinculado, se actualiza la ficha de "Medico".
     * Los campos vacíos de nombre, especialidad y teléfono conservan el valor
     * actual; la dirección vacía se guarda como NULL.
     */
    @Transactional
    public User actualizarPerfil(String username, String nombre, String especialidad,
                                 String direccion, String telefono) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("El usuario no existe."));

        int actualizados = jdbcTemplate.update("""
                UPDATE public."Medico"
                SET "nomMed" = COALESCE(?, "nomMed"),
                    "espMed" = COALESCE(?, "espMed"),
                    "dirMed" = ?,
                    "telMed" = COALESCE(?, "telMed")
                WHERE username = ?
                """,
                validarCampo("nombre", nombre, 100),
                validarCampo("especialidad", especialidad, 100),
                validarCampo("dirección", direccion, 100),
                validarCampo("teléfono", telefono, 20),
                username);

        if (actualizados == 0) {
            throw new IllegalArgumentException(
                    "Tu cuenta no tiene una ficha de médico asociada.");
        }

        return user;
    }

    private String validarCampo(String campo, String value, int maxLength) {
        String normalized = blankToNull(value);
        if (normalized != null && normalized.length() > maxLength) {
            throw new IllegalArgumentException(
                    "El campo " + campo + " no puede superar " + maxLength + " caracteres.");
        }
        return normalized;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
