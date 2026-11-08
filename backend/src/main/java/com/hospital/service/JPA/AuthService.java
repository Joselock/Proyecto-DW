package com.hospital.service.JPA;

import com.hospital.model.EntidadesJPA.DataUser;
import com.hospital.model.EntidadesJPA.User;
import com.hospital.repository.JPA.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Locale;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
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

    @Transactional
    public void registrar(String username, String email, String password, String nombre,
                          String especialidad, String direccion, String telefono) {
        String normalizedUsername = username == null ? "" : username.trim();
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
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
        // poder configurar el resto del sistema. Los siguientes son médicos.
        boolean esPrimerUsuario = userRepository.count() == 0;

        User user = new User();
        user.setUsername(normalizedUsername);
        user.setEmail(normalizedEmail);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(esPrimerUsuario ? "ADMIN" : "MEDICO");

        DataUser profile = new DataUser();
        profile.setUsername(normalizedUsername);
        profile.setEmail(normalizedEmail);
        profile.setNombre(blankToNull(nombre));
        profile.setEspecialidad(blankToNull(especialidad));
        profile.setDireccion(blankToNull(direccion));
        profile.setTelefono(blankToNull(telefono));
        profile.setUser(user);
        user.setDataUser(profile);

        userRepository.save(user);
    }

    @Transactional
    public User actualizarPerfil(String username, String nombre, String especialidad,
                                 String direccion, String telefono) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("El usuario no existe."));

        DataUser profile = user.getDataUser();
        if (profile == null) {
            profile = new DataUser();
            profile.setUsername(user.getUsername());
            profile.setEmail(user.getEmail());
            profile.setUser(user);
            user.setDataUser(profile);
        }

        profile.setNombre(validarCampo("nombre", nombre, 100));
        profile.setEspecialidad(validarCampo("especialidad", especialidad, 100));
        profile.setDireccion(validarCampo("dirección", direccion, 100));
        profile.setTelefono(validarCampo("teléfono", telefono, 20));

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
