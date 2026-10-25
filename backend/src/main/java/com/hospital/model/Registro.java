package com.mycompany.gestorui.model.entidades;

import java.util.List;

public class Registro {
    private String codUnidad;
    private List<Paciente>pacientes;
    
    public Registro(String codUnidad, List<Paciente> pacientes) {
        this.codUnidad = codUnidad;
        this.pacientes = pacientes;
    }

    public String getCodUnidad() {
        return codUnidad;
    }

    public void setCodUnidad(String codUnidad) {
        this.codUnidad = codUnidad;
    }

    public List<Paciente> getPacientes() {
        return pacientes;
    }

    public void setPacientes(List<Paciente> pacientes) {
        this.pacientes = pacientes;
    }

    

}
