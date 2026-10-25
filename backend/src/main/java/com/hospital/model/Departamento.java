package com.mycompany.gestorui.model.entidades;

import java.util.List;

public class Departamento{
    private String codigoDep;
    private String nombreDep;
    private String codigoHos;
    private List<Unidad>unidades;
    

    public Departamento(String codigoDep, String nombreDep, List<Unidad> unidades) {
        this.codigoDep = codigoDep;
        this.nombreDep = nombreDep;
        this.unidades = unidades;
    }


    public Departamento(String codigoDep, String nombreDep,String codigoHos) {
        this.codigoDep = codigoDep;
        this.nombreDep = nombreDep;
        this.codigoHos = codigoHos;
    }

    
    public String getCodigoDep() {
        return codigoDep;
    }

    public void setCodigoDep(String codigoDep) {
        this.codigoDep = codigoDep;
    }

    public String getNombreDep() {
        return nombreDep;
    }

    public void setNombreDep(String nombreDep) {
        this.nombreDep = nombreDep;
    }

    public List<Unidad> getUnidades() {
        return unidades;
    }

    public void setUnidades(List<Unidad> unidades) {
        this.unidades = unidades;
    }


    public String getCodigoHos() {
        return codigoHos;
    }


    public void setCodigoHos(String codigoHos) {
        this.codigoHos = codigoHos;
    }


    


}