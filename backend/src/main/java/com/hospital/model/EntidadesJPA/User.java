package com.hospital.model.EntidadesJPA;

import jakarta.persistence.*;
import java.util.Objects;

@Entity
// "user" es palabra reservada en PostgreSQL, por eso se mantiene entre comillas.
@Table(name = "\"user\"")
@IdClass(UserId.class)
public class User {

    @Id
    @Column(length = 100, nullable = false)
    private String username;

    @Id
    @Column(length = 100, nullable = false)
    private String email;

    @Column(length = 255, nullable = false)
    private String password;

    // Rol del usuario dentro del sistema: ADMIN o MEDICO
    @Column(length = 20)
    private String role;

    // Relación 1:1 con DataUser (perfil extendido)
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private DataUser dataUser;

    // Getters y setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public DataUser getDataUser() { return dataUser; }
    public void setDataUser(DataUser dataUser) { this.dataUser = dataUser; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        User user = (User) o;
        return Objects.equals(username, user.username)
            && Objects.equals(email, user.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username, email);
    }
}
