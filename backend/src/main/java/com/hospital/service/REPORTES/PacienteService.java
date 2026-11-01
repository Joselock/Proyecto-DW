package com.mycompany.gestorui.model.services.reportes;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;

import com.mycompany.gestorui.model.entidades.Consulta;
import com.mycompany.gestorui.model.entidades.Departamento;
import com.mycompany.gestorui.model.entidades.Hospital;
import com.mycompany.gestorui.model.entidades.Paciente;
import com.mycompany.gestorui.model.entidades.Registro;
import com.mycompany.gestorui.model.entidades.Turno;
import com.mycompany.gestorui.model.entidades.Unidad;
import com.mycompany.gestorui.model.utils.BaseDatos;

public class PacienteService {

    // Reporte 1: Listado de pacientes de un departamento , departamentos o una unidad
    public LinkedList<Hospital> obtenerPacientesDepartamento(String area, String codigo) {
        LinkedList<Hospital> lista = new LinkedList<>();
        HashMap<String, Hospital> hospitalMap = new HashMap<>();

        String function = null;

        switch (area) {
            case "Hospital" ->
                function = "SELECT * FROM public.listadoPacientes(?,NULL,NULL)";
            case "Departamento" ->
                function = "SELECT * FROM public.listadoPacientes(NULL,?,NULL)";
            case "Unidad" ->
                function = "SELECT * FROM public.listadoPacientes(NULL,NULL,?)";
            default -> {
            }
        }

        try (java.sql.Connection con = BaseDatos.getConnection()) {

            PreparedStatement ps = con.prepareStatement(function);

            // Solo establecer el parámetro si la función lo requiere
            if (function.contains("?")) {
                if (codigo == null || codigo.trim().isEmpty()) {
                    throw new IllegalArgumentException("Se requiere código para área: " + area);
                }
                ps.setString(1, codigo);
            }
            try (ResultSet rs = ps.executeQuery();) {
                while (rs.next()) {
                    String hospital = rs.getString("hospital");
                    String departamento = rs.getString("departamento");
                    String unidad = rs.getString("unidad");
                    String numHis = rs.getString("numHis");
                    String nombre = rs.getString("nombre");
                    Date fechaN = rs.getDate("fechaN");
                    String direccion = rs.getString("direccion");

                    Paciente p = new Paciente(numHis, nombre, fechaN, direccion, null);

                    //Buscar o crear hospital
                    Hospital h = hospitalMap.get(hospital);
                    if (h == null) {
                        h = new Hospital(null, hospital, new LinkedList<>());
                        hospitalMap.put(hospital, h);
                        lista.add(h);
                    }

                    //Buscar o crear departameento dentro del hospital
                    Departamento d = null;
                    for (Departamento dpto : h.getDepartamentos()) {
                        if (dpto.getNombreDep().equals(departamento)) {
                            d = dpto;
                            break;
                        }
                    }
                    if (d == null) {
                        d = new Departamento(null, departamento, new LinkedList<>());
                        h.getDepartamentos().add(d);
                    }

                    //Buscar o crear una unidad
                    Unidad u = null;
                    for (Unidad uni : d.getUnidades()) {
                        if (uni.getNombreUni().equals(unidad)) {
                            u = uni;
                            break;
                        }
                    }
                    if (u == null) {
                        u = new Unidad(null, unidad, null, new Registro(null, new LinkedList<>()),
                                new LinkedList<>(), new LinkedList<>(), new LinkedList<>());
                        d.getUnidades().add(u);
                    }

                    u.getRegistro().getPacientes().add(p);

                }
            }

        } catch (SQLException ex) {
            System.getLogger(PacienteService.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }

        return lista;
    }

    // Reporte 6: Listado de pacinetes que no fueron atendidos\
    public LinkedList<Hospital> listadoNoAtendidos(String area, String codigo) {
        LinkedList<Hospital> lista = new LinkedList<>();
        HashMap<String, Hospital> hospitalMap = new HashMap<>();

        String function = null;

        switch (area) {
            case "Hospital" ->
                function = "SELECT * FROM public.pacientesNoAtendidos(?,NULL,NULL)";
            case "Departamento" ->
                function = "SELECT * FROM public.pacientesNoAtendidos(NULL,?,NULL)";
            case "Unidad" ->
                function = "SELECT * FROM public.pacientesNoAtendidos(NULL,NULL,?)";
            default -> {
            }
        }

        try (java.sql.Connection con = BaseDatos.getConnection()) {
            PreparedStatement ps = con.prepareStatement(function);
            ps.setString(1, codigo);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String hospital = rs.getString("hospital");
                    String departamento = rs.getString("departamento");
                    String unidad = rs.getString("unidad");
                    int numeroTurno = rs.getInt("numTur");
                    int cantidadNoAtendidos = rs.getInt("cantNoAtendidos");
                    String numHistoria = rs.getString("numHistoria");
                    String nombrePaciente = rs.getString("nombrePac");
                    String direccion = rs.getString("direccion");
                    String causa = rs.getString("causa");

                    // Crear objetos
                    Paciente p = new Paciente(numHistoria, nombrePaciente, null, direccion, null);
                    Consulta c = new Consulta(numHistoria, numeroTurno, false, causa, p);
                    Turno t = new Turno(numeroTurno, cantidadNoAtendidos, c);

                    // Buscar o crear Hospital
                    Hospital h = hospitalMap.get(hospital);
                    if (h == null) {
                        h = new Hospital(null, hospital, new LinkedList<>());
                        hospitalMap.put(hospital, h);
                        lista.add(h);
                    }

                    // Buscar o crear Departamento dentro del Hospital
                    Departamento d = null;
                    for (Departamento depto : h.getDepartamentos()) {
                        if (depto.getNombreDep().equals(departamento)) {
                            d = depto;
                            break;
                        }
                    }
                    if (d == null) {
                        d = new Departamento(null, departamento, new LinkedList<>());
                        h.getDepartamentos().add(d);
                    }

                    // Buscar o crear Unidad dentro del Departamento
                    Unidad u = null;
                    for (Unidad uni : d.getUnidades()) {
                        if (uni.getNombreUni().equals(unidad)) {
                            u = uni;
                            break;
                        }
                    }
                    if (u == null) {
                        u = new Unidad(null, unidad, null, new Registro(null, new LinkedList<>()),
                                new LinkedList<>(), new LinkedList<>(), new LinkedList<>());
                        d.getUnidades().add(u);
                    }

                    // Agregar el turno a la unidad
                    u.getTurnos().add(t);

                }
            }

        } catch (SQLException ex) {
            System.getLogger(PacienteService.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }

        return lista;
    }

}
