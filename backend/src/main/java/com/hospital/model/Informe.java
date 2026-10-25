package com.mycompany.gestorui.model.entidades;

import java.sql.Date;
import java.sql.Time;

public class Informe {
    private Time hora;
    private Date fecha;
    private String numIn;
    private int pacAtend;
    private int pacAlta;
    private int cantAdm;
    private int total;
    private int cantIni;

    private int cantAnterior;
    

    // Variables para el reporte 7
    private float porcentajePacAtend;
    private int cantPacExtranjero;
    private int cantPacProvincia;
    private int cantPacOtraUnidad;
    private int cantPacOtrasCausas;
    private int cantPacDesconoce;
    private String codigoUni;
    private int numeroTurno;

    // Informe-consulta
    public Informe(Time hora, Date fecha, String numIn, int pacAtend, int pacAlta, int cantAdm, int total, int cantIni,
            int cantAnterior,int numeroTurno,String codigoUni) {

        this.hora = hora;
        this.fecha = fecha;
        this.numIn = numIn;
        this.pacAtend = pacAtend;
        this.pacAlta = pacAlta;
        this.cantAdm = cantAdm;
        this.total = total;
        this.cantIni = cantIni;
        this.cantAnterior = cantAnterior;
        this.codigoUni = codigoUni;
        this.numeroTurno = numeroTurno;
    }

    public Informe(Time hora, Date fecha, String numIn, int pacAtend, int pacAlta, int cantAdm, int total, int cantIni,
            int cantAnterior) {

        this.hora = hora;
        this.fecha = fecha;
        this.numIn = numIn;
        this.pacAtend = pacAtend;
        this.pacAlta = pacAlta;
        this.cantAdm = cantAdm;
        this.total = total;
        this.cantIni = cantIni;
        this.cantAnterior = cantAnterior;
    }

    public Informe(Time hora, Date fecha, String numIn, int pacAtend, int pacAlta, int cantAdm, int total, int cantIni,
            float porcentajePacAtend, int cantPacExtranjero, int cantPacProvincia,
            int cantPacOtraUnidad, int cantPacOtrasCausas, int cantPacDesconoce) {
        this.hora = hora;
        this.fecha = fecha;
        this.numIn = numIn;
        this.pacAtend = pacAtend;
        this.pacAlta = pacAlta;
        this.cantAdm = cantAdm;
        this.total = total;
        this.cantIni = cantIni;
        this.porcentajePacAtend = porcentajePacAtend;
        this.cantPacExtranjero = cantPacExtranjero;
        this.cantPacProvincia = cantPacProvincia;
        this.cantPacOtraUnidad = cantPacOtraUnidad;
        this.cantPacOtrasCausas = cantPacOtrasCausas;
        this.cantPacDesconoce = cantPacDesconoce;
    }

    public Time getHora() {
        return hora;
    }

    public void setHora(Time hora) {
        this.hora = hora;
    }

    public Date getFecha() {
        return fecha;
    }

    public void setFecha(Date fecha) {
        this.fecha = fecha;
    }

    public String getNumIn() {
        return numIn;
    }

    public void setNumIn(String numIn) {
        this.numIn = numIn;
    }

    public int getPacAtend() {
        return pacAtend;
    }

    public void setPacAtend(int pacAtend) {
        this.pacAtend = pacAtend;
    }

    public int getPacAlta() {
        return pacAlta;
    }

    public void setPacAlta(int pacAlta) {
        this.pacAlta = pacAlta;
    }

    public int getCantAdm() {
        return cantAdm;
    }

    public void setCantAdm(int cantAdm) {
        this.cantAdm = cantAdm;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getCantIni() {
        return cantIni;
    }

    public void setCantIni(int cantIni) {
        this.cantIni = cantIni;
    }

    public int getCantAnterior() {
        return cantAnterior;
    }

    public void setCantAnterior(int cantAnterior) {
        this.cantAnterior = cantAnterior;
    }


    public String getCodigoUni() {
        return codigoUni;
    }

    public void setCodigoUni(String codigoUni) {
        this.codigoUni = codigoUni;
    }

    public int getNumeroTurno() {
        return numeroTurno;
    }

    public void setNumeroTurno(int numeroTurno) {
        this.numeroTurno = numeroTurno;
    }



    
    // metodos getters y setters para el reporte 7

    public float getPorcentajePacAtend() {
        return porcentajePacAtend;
    }

    public void setPorcentajePacAtend(float porcentajePacAtend) {
        this.porcentajePacAtend = porcentajePacAtend;
    }

    public int getCantPacExtranjero() {
        return cantPacExtranjero;
    }

    public void setCantPacExtranjero(int cantPacExtranjero) {
        this.cantPacExtranjero = cantPacExtranjero;
    }

    public int getCantPacProvincia() {
        return cantPacProvincia;
    }

    public void setCantPacProvincia(int cantPacProvincia) {
        this.cantPacProvincia = cantPacProvincia;
    }

    public int getCantPacOtraUnidad() {
        return cantPacOtraUnidad;
    }

    public void setCantPacOtraUnidad(int cantPacOtraUnidad) {
        this.cantPacOtraUnidad = cantPacOtraUnidad;
    }

    public int getCantPacOtrasCausas() {
        return cantPacOtrasCausas;
    }

    public void setCantPacOtrasCausas(int cantPacOtrasCausas) {
        this.cantPacOtrasCausas = cantPacOtrasCausas;
    }

    public int getCantPacDesconoce() {
        return cantPacDesconoce;
    }

    public void setCantPacDesconoce(int cantPacDesconoce) {
        this.cantPacDesconoce = cantPacDesconoce;
    }


}
