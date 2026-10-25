package com.mycompany.gestorui.model.entidades;

import java.util.List;

public class Hospital {
    private String codigoHos;
    private String nombreHos;
    private int cantDep;
    private int cantUni;
    private int cantMed;
    private int cantPac;
    private List<Departamento> departamentos;

    public Hospital(){}

    
    public Hospital(String codigoHos, String nombreHos,List<Departamento> departamentos) {
        this.codigoHos = codigoHos;
        this.nombreHos = nombreHos;
        this.departamentos = departamentos;
    }


    public Hospital(String codigoHos, String nombreHos, int cantDep, int cantUni, int cantMed, int cantPac) {
        this.codigoHos = codigoHos;
        this.nombreHos = nombreHos;
        this.cantDep = cantDep;
        this.cantUni = cantUni;
        this.cantMed = cantMed;
        this.cantPac = cantPac;
    }

    public String getCodigoHos() {
        return codigoHos;
    }

    public void setCodigoHos(String codigoHos) {
        this.codigoHos = codigoHos;
    }

    public String getNombreHos() {
        return nombreHos;
    }

    public void setNombreHos(String nombreHos) {
        this.nombreHos = nombreHos;
    }

    public int getCantDep() {
        return cantDep;
    }

    public void setCantDep(int cantDep) {
        this.cantDep = cantDep;
    }

    public int getCantUni() {
        return cantUni;
    }

    public void setCantUni(int cantUni) {
        this.cantUni = cantUni;
    }

    public int getCantMed() {
        return cantMed;
    }

    public void setCantMed(int cantMed) {
        this.cantMed = cantMed;
    }

    public int getCantPac() {
        return cantPac;
    }

    public void setCantPac(int cantPac) {
        this.cantPac = cantPac;
    }

    public List<Departamento> getDepartamentos() {
        return departamentos;
    }

    public void setDepartamentos(List<Departamento> departamentos) {
        this.departamentos = departamentos;
    }

}