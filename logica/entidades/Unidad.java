package com.mycompany.gestorui.model.entidades;

import java.util.List;

public class Unidad {
    private String codigoUni;
    private String nombreUni;
    private String ubicacion;
    private Registro registro;
    private List<Medico>medicos;
    private List<Informe>informes;
    private List<Turno>turnos;
    private String codigoDep;
    

    public Unidad(String codigoUni, String nombreUni, String ubicacion) {
        this.codigoUni = codigoUni;
        this.nombreUni = nombreUni;
        this.ubicacion = ubicacion;
    }

    public Unidad(String codigoUni, String nombreUni, String ubicacion,String codigoDep) {
        this.codigoUni = codigoUni;
        this.nombreUni = nombreUni;
        this.ubicacion = ubicacion;
        this.codigoDep = codigoDep;
    }

    public Unidad(String codigoUni, String nombreUni, String ubicacion, Registro registro, List<Medico> medicos,
            List<Informe> informes,List<Turno>turnos) {
        this.codigoUni = codigoUni;
        this.nombreUni = nombreUni;
        this.ubicacion = ubicacion;
        this.registro = registro;
        this.medicos = medicos;
        this.informes = informes;
        this.turnos = turnos;
    }



    public String getCodigoUni() {
        return codigoUni;
    }

    public void setCodigoUni(String codigoUni) {
        this.codigoUni = codigoUni;
    }

    public String getNombreUni() {
        return nombreUni;
    }

    public void setNombreUni(String nombreUni) {
        this.nombreUni = nombreUni;
    }

    public String getUbicacion() {
        return ubicacion;
    }

    public void setUbicacion(String ubicacion) {
        this.ubicacion = ubicacion;
    }

    public List<Medico> getMedicos() {
        return medicos;
    }

    public void setMedicos(List<Medico> medicos) {
        this.medicos = medicos;
    }

    public List<Informe> getInformes() {
        return informes;
    }

    public void setInformes(List<Informe> informes) {
        this.informes = informes;
    }


    public List<Turno> getTurnos() {
        return turnos;
    }


    public void setTurnos(List<Turno> turnos) {
        this.turnos = turnos;
    }



    public Registro getRegistro() {
        return registro;
    }



    public void setRegistro(Registro registro) {
        this.registro = registro;
    }

    public String getCodigoDep() {
        return codigoDep;
    }

    public void setCodigoDep(String codigoDep) {
        this.codigoDep = codigoDep;
    }
    
    
    
}