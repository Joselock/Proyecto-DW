package com.mycompany.gestorui.model.entidades;

public class Consulta {
    private String numH;
    private int numeroT;
    private boolean atend;
    private String causa;
    private Paciente paciente;

    public Consulta(String numH, int numeroT, boolean atend, String causa,Paciente paciente) {
        this.numH = numH;
        this.numeroT = numeroT;
        this.atend = atend;
        this.causa = causa;
        this.paciente = paciente;
    }

    public String getNumH() {
        return numH;
    }


    public void setNumH(String numH) {
        this.numH = numH;
    }

    public int getNumeroT() {
        return numeroT;
    }

    public void setNumeroT(int numeroT) {
        this.numeroT = numeroT;
    }

    public boolean isAtend() {
        return atend;
    }

    public void setAtend(boolean atend) {
        this.atend = atend;
    }

    public String getCausa() {
        return causa;
    }

    public void setCausa(String causa) {
        this.causa = causa;
    }

    public Paciente getPaciente() {
        return paciente;
    }

    public void setPaciente(Paciente paciente) {
        this.paciente = paciente;
    }
    
    
    
}
