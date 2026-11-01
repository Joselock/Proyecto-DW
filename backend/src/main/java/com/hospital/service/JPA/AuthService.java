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

        User user = new User();
        user.setUsername(normalizedUsername);
        user.setEmail(normalizedEmail);
        user.setPassword(passwordEncoder.encode(password));

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

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
