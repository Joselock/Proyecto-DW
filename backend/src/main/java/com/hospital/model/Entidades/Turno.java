package com.hospital.model.Entidades;

import java.sql.Time;
import java.sql.Date;

public class Turno {
    private int numTurn;
    private int cantAten;
    private int cantNoAten;
    private String estTur;
    private String codUni;
    private Consulta consulta;
    private Medico medico;
    private String codigoUni;
    private Date fecha;
    private Time hora;
    private String notaClinica;
    
    
    public Turno(int numTurn, int cantAten, String estTur, String codUni,Medico medico) {
        this.numTurn = numTurn;
        this.cantAten = cantAten;
        this.estTur = estTur;
        this.codUni = codUni;
        this.medico = medico;
    }

    

    public Turno(int numTurn, int cantAten, String estTur, String codUni, Medico medico, Date fecha, Time hora,
            String notaClinica) {
        this.numTurn = numTurn;
        this.cantAten = cantAten;
        this.estTur = estTur;
        this.codUni = codUni;
        this.medico = medico;
        this.fecha = fecha;
        this.hora = hora;
        this.notaClinica = notaClinica;
    }



    public Turno(int numTurn, int cantAten, String estTur, String codUni,Medico medico,String codigoUni) {
        this.numTurn = numTurn;
        this.cantAten = cantAten;
        this.estTur = estTur;
        this.codUni = codUni;
        this.medico = medico;
        this.codigoUni = codigoUni;
    }
    
    
    public Turno(int numTurn, int cantNoAten,Consulta consulta) {
        this.numTurn = numTurn;
        this.cantNoAten = cantNoAten;
        this.consulta = consulta;
    }


    public int getNumTurn() {
        return numTurn;
    }

    public void setNumTurn(int numTurn) {
        this.numTurn = numTurn;
    }

    public int getCanAten() {
        return cantAten;
    }

    public void setCanAten(int cantAten) {
        this.cantAten = cantAten;
    }

    public String getEstTur() {
        return estTur;
    }

    public void setEstTur(String estTur) {
        this.estTur = estTur;
    }

    public String getCodUni() {
        return codUni;
    }

    public void setCodUni(String codUni) {
        this.codUni = codUni;
    }


    public Consulta getConsulta() {
        return consulta;
    }

    public void setConsulta(Consulta consulta) {
        this.consulta = consulta;
    }


    public int getCantAten() {
        return cantAten;
    }


    public void setCantAten(int cantAten) {
        this.cantAten = cantAten;
    }


    public int getCantNoAten() {
        return cantNoAten;
    }


    public void setCantNoAten(int cantNoAten) {
        this.cantNoAten = cantNoAten;
    }


    public Medico getMedico() {
        return medico;
    }


    public void setMedico(Medico medico) {
        this.medico = medico;
    }

    public String getCodigoUni() {
        return codigoUni;
    }

    public void setCodigoUni(String codigoUni) {
        this.codigoUni = codigoUni;
    }



    public Date getFecha() {
        return fecha;
    }



    public void setFecha(Date fecha) {
        this.fecha = fecha;
    }



    public Time getHora() {
        return hora;
    }



    public void setHora(Time hora) {
        this.hora = hora;
    }



    public String getNotaClinica() {
        return notaClinica;
    }



    public void setNotaClinica(String notaClinica) {
        this.notaClinica = notaClinica;
    }

    
    

}
