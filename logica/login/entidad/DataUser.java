package com.mycompany.gestorui.model.login.entidad;

public class DataUser {
    private String nombreCompleto;
    private String especialidad;
    private String direccion;
    private String telefono;
    
    public DataUser(String nombreCompleto, String especialidad, String direccion, String telefono) {
        this.nombreCompleto = nombreCompleto;
        this.especialidad = especialidad;
        this.direccion = direccion;
        this.telefono = telefono;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public void setNombreCompleto(String nombreCompleto) {
        this.nombreCompleto = nombreCompleto;
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

    

}
