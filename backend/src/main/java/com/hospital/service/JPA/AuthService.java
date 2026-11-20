package com.hospital.service.JPA;

import com.hospital.model.EntidadesJPA.User;
import com.hospital.repository.JPA.UserRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
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

    @Transactional
    public Map<String, Object> solicitarRecuperacion(String email, String baseUrl) {
        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail == null) {
            throw new IllegalArgumentException("Ingresa un correo electrónico válido.");
        }

        // Limpiar tokens expirados o ya usados para no acumular basura.
        jdbcTemplate.update("""
                DELETE FROM public."reset_tokens"
                WHERE "usado" = true
                   OR "fecha_expiracion" < now()
                """);

        boolean existe = userRepository.findByEmail(normalizedEmail).isPresent();
        Map<String, Object> resultado = new LinkedHashMap<>();
        // Respuesta genérica para no revelar si el correo existe.
        resultado.put("message",
                "Si el correo está registrado, recibirás un enlace para restablecer tu contraseña.");

        if (!existe) {
            return resultado;
        }

        // Invalidar tokens previos del mismo correo.
        jdbcTemplate.update("""
                UPDATE public."reset_tokens"
                SET "usado" = true
                WHERE "email" = ? AND "usado" = false
                """, normalizedEmail);

        String tokenPlano = generarTokenPlano();
        String tokenHash = hashToken(tokenPlano);

        jdbcTemplate.update("""
                INSERT INTO public."reset_tokens" ("email", "token", "fecha_expiracion")
                VALUES (?, ?, now() + interval '30 minutes')
                """, normalizedEmail, tokenHash);

        String enlace = (baseUrl == null ? "" : baseUrl.replaceAll("/$", ""))
                + "/index.html?reset_token=" + tokenPlano
                + "&email=" + urlEncode(normalizedEmail);

        resultado.put("resetUrl", enlace);
        resultado.put("expiresInMinutes", 30);
        return resultado;
    }

    @Transactional
    public void restablecerContrasena(String email, String tokenPlano, String nuevaPassword) {
        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail == null) {
            throw new IllegalArgumentException("El enlace de recuperación no es válido.");
        }
        if (tokenPlano == null || tokenPlano.isBlank()) {
            throw new IllegalArgumentException("El enlace de recuperación no es válido.");
        }
        if (nuevaPassword == null || nuevaPassword.length() < 6) {
            throw new IllegalArgumentException(
                    "La nueva contraseña debe tener al menos 6 caracteres.");
        }

        String tokenHash = hashToken(tokenPlano.trim());
        List<Long> tokens = jdbcTemplate.queryForList("""
                SELECT "id"
                FROM public."reset_tokens"
                WHERE "email" = ?
                  AND "token" = ?
                  AND "usado" = false
                  AND "fecha_expiracion" > now()
                ORDER BY "fecha_creacion" DESC
                LIMIT 1
                """, Long.class, normalizedEmail, tokenHash);

        if (tokens.isEmpty()) {
            throw new IllegalArgumentException(
                    "El enlace de recuperación no es válido o ha expirado.");
        }

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new IllegalArgumentException(
                        "El enlace de recuperación no es válido o ha expirado."));

        user.setPassword(passwordEncoder.encode(nuevaPassword));
        userRepository.save(user);

        jdbcTemplate.update("""
                UPDATE public."reset_tokens"
                SET "usado" = true
                WHERE "id" = ?
                """, tokens.get(0));

        // Invalidar cualquier otro token activo del mismo correo.
        jdbcTemplate.update("""
                UPDATE public."reset_tokens"
                SET "usado" = true
                WHERE "email" = ? AND "usado" = false
                """, normalizedEmail);
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) return null;
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        return normalized.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$") ? normalized : null;
    }

    private String generarTokenPlano() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String tokenPlano) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(tokenPlano.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("No se pudo procesar el token de recuperación.", e);
        }
    }

    private String urlEncode(String value) {
        return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
