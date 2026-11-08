package com.hospital.model.EntidadesJPA;

import jakarta.persistence.*;

@Entity
@Table(name = "\"data_user\"")
@IdClass(UserId.class)   // reutilizamos la misma PK compuesta
public class DataUser {

    @Id
    @Column(length = 100)
    private String username;

    @Id
    @Column(length = 100)
    private String email;

    @Column(length = 100)
    private String nombre;

    @Column(length = 100)
    private String especialidad;

    @Column(length = 100)
    private String direccion;

    @Column(length = 20)
    private String telefono;

    // FK compuesta → User
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumns({
        @JoinColumn(name = "username", referencedColumnName = "username",
                    insertable = false, updatable = false),
        @JoinColumn(name = "email", referencedColumnName = "email",
                    insertable = false, updatable = false)
    })
    private User user;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getEspecialidad() {
        return especialidad;
    }

    public void setEspecialidad(String especialidad) {
        this.especialidad = especialidad;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    
}
