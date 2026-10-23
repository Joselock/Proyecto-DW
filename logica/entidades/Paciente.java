package com.mycompany.gestorui.model.entidades;

import java.sql.Date;

public class Paciente {
    private String numHisCli;
    private String nombrePac;
    private Date fechaN;
    private String direccionP;
    private String estado;
    String codigoUni;
    
    
    public Paciente() {
    }

    public Paciente(String numHisCli, String nombrePac, Date fechaN, String direccionP,String estado) {
        this.numHisCli = numHisCli;
        this.nombrePac = nombrePac;
        this.fechaN = fechaN;
        this.direccionP = direccionP;
        this.estado = estado;
    }

    public Paciente(String numHisCli, String nombrePac, Date fechaN, String direccionP,String estado,String codigoUni) {
        this.numHisCli = numHisCli;
        this.nombrePac = nombrePac;
        this.fechaN = fechaN;
        this.direccionP = direccionP;
        this.estado = estado;
        this.codigoUni = codigoUni;
    }

    public String getNumHisCli() {
        return numHisCli;
    }

    public void setNumHisCli(String numHisCli) {
        this.numHisCli = numHisCli;
    }

    public String getNombrePac() {
        return nombrePac;
    }

    public void setNombrePac(String nombrePac) {
        this.nombrePac = nombrePac;
    }

    public Date getFechaN() {
        return fechaN;
    }

    public void setFechaN(Date fechaN) {
        this.fechaN = fechaN;
    }

    public String getDireccionP() {
        return direccionP;
    }

    public void setDireccionP(String direccionP) {
        this.direccionP = direccionP;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getCodigoUni() {
        return codigoUni;
    }

    public void setCodigoUni(String codigoUni) {
        this.codigoUni = codigoUni;
    }

    

}
