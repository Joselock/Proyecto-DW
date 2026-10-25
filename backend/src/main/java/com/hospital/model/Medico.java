package com.mycompany.gestorui.model.entidades;

public class Medico {
    private String codigoMed;
    private String nombreMed;
    private String especialidad;
    private String numeroLic;
    private String datosC;
    private int experiencia;
    private String telefono;
    private String codigoUni;
    
    public Medico(String codigoMed, String nombreMed, String especialidad, String numeroLic, String datosC,int experiencia,String telefono) {
        this.codigoMed = codigoMed;
        this.nombreMed = nombreMed;
        this.especialidad = especialidad;
        this.numeroLic = numeroLic;
        this.datosC = datosC;
        this.experiencia = experiencia;
        this.telefono = telefono;
    }

    public Medico(String codigoMed, String nombreMed, String especialidad, String numeroLic, String datosC,int experiencia,String telefono,String codigoUni) {
        this.codigoMed = codigoMed;
        this.nombreMed = nombreMed;
        this.especialidad = especialidad;
        this.numeroLic = numeroLic;
        this.datosC = datosC;
        this.experiencia = experiencia;
        this.telefono = telefono;
        this.codigoUni = codigoUni;
    }

    public String getCodigoMed() {
        return codigoMed;
    }

    public void setCodigoMed(String codigoMed) {
        this.codigoMed = codigoMed;
    }

    public String getNombreMed() {
        return nombreMed;
    }

    public void setNombreMed(String nombreMed) {
        this.nombreMed = nombreMed;
    }

    public String getEspecialidad() {
        return especialidad;
    }

    public void setEspecialidad(String especialidad) {
        this.especialidad = especialidad;
    }

    public String getNumeroLic() {
        return numeroLic;
    }

    public void setNumeroLic(String numeroLic) {
        this.numeroLic = numeroLic;
    }

    public String getDatosC() {
        return datosC;
    }

    public void setDatosC(String datosC) {
        this.datosC = datosC;
    }

    public int getExperiencia() {
        return experiencia;
    }

    public void setExperiencia(int experiencia) {
        this.experiencia = experiencia;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getCodigoUni() {
        return codigoUni;
    }

    public void setCodigoUni(String codigoUni) {
        this.codigoUni = codigoUni;
    }

    
    
}