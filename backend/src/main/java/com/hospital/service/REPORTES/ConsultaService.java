package com.mycompany.gestorui.model.services.reportes;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.util.HashMap;
import java.util.LinkedList;

import com.mycompany.gestorui.model.entidades.Departamento;
import com.mycompany.gestorui.model.entidades.Hospital;
import com.mycompany.gestorui.model.entidades.Informe;
import com.mycompany.gestorui.model.entidades.Medico;
import com.mycompany.gestorui.model.entidades.Registro;
import com.mycompany.gestorui.model.entidades.Turno;
import com.mycompany.gestorui.model.entidades.Unidad;
import com.mycompany.gestorui.model.utils.BaseDatos;

public class ConsultaService {

    // Reporte 5: Informe durante las consultas
    public LinkedList<Hospital> obtenerInformeHospital(String area, String codigo) throws SQLException {
        LinkedList<Hospital> resultado = new LinkedList<>();

        HashMap<String, Hospital> hospitalMap = new HashMap<>();

        String function = null;

        switch (area) {
            case "Hospital" -> function = "SELECT * FROM public.informeDuranteConsultas(?,NULL,NULL)";
            case "Departamento" -> function = "SELECT * FROM public.informeDuranteConsultas(NULL,?,NULL)";
            case "Unidad" -> function = "SELECT * FROM public.informeDuranteConsultas(NULL,NULL,?)";
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
                    Date fecha = rs.getDate("fecha");
                    int numTurno = rs.getInt("numTurno");
                    Time hora = rs.getTime("hora_informe");
                    String numInforme = rs.getString("numInforme");
                    int cantInicial = rs.getInt("cantIniPac");
                    int cantAdmitida = rs.getInt("cantAdmPac");
                    int cantAlta = rs.getInt("cantAltPac");
                    int cantAnterior = rs.getInt("cantAnterior");
                    int cantDia = rs.getInt("cantDia");

                    Informe informe = new Informe(hora, fecha, numInforme, 0, cantAlta, cantAdmitida, cantDia, cantInicial, cantAnterior);
                    Turno turno = new Turno(numTurno, 0, null, null, null);

                    Hospital h = hospitalMap.get(hospital);
                    if (h == null) {
                        h = new Hospital(codigo, hospital, new LinkedList<>());
                        hospitalMap.put(hospital, h);
                        resultado.add(h);
                    }

                    Departamento d = null;
                    for (Departamento dep : h.getDepartamentos()) {
                        if (dep.getNombreDep().equals(departamento)) {
                            d = dep;
                            break;
                        }
                    }
                    if (d == null) {
                        d = new Departamento(null, departamento, new LinkedList<>());
                        h.getDepartamentos().add(d);
                    }

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

                    u.getInformes().add(informe);
                    u.getTurnos().add(turno);

                }
            }

        } catch (SQLException ex) {
            System.getLogger(ConsultaService.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }

        return resultado;
    }

    //Reporte 9: Resumen de consultas exitosas
    public LinkedList<Hospital> resumenConsultasExitosas(String area, String codigo) {
        LinkedList<Hospital> resumen = new LinkedList<>();
        HashMap<String, Hospital> hospitalMap = new HashMap<>();

        String function = null;

            switch (area) {
            case "Hospital" -> function = "SELECT * FROM public.consultasExitosas(?,NULL)";
            case "Unidad" -> function = "SELECT * FROM public.consultasExitosas(NULL,?)";
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
                    int numeroTurno = rs.getInt("numeroTurno");
                    String medico = rs.getString("medico");
                    int totalPacAten = rs.getInt("totalPacAten");
                    int pacienteAtendTurnos = rs.getInt("pacienteAtend");

                    Informe i = new Informe(null, null, null, totalPacAten, 0, 0, 0, 0, 0);
                    Turno t = new Turno(numeroTurno, pacienteAtendTurnos, null, null, new Medico(null, medico, null, null, null, 0, null));

                    Hospital h = hospitalMap.get(hospital);
                    if (h == null) {
                        h = new Hospital(codigo, hospital, new LinkedList<>());
                        hospitalMap.put(hospital, h);
                        resumen.add(h);
                    }

                    Departamento d = null;
                    for (Departamento dep : h.getDepartamentos()) {
                        if (dep.getNombreDep().equals(departamento)) {
                            d = dep;
                            break;
                        }
                    }
                    if (d == null) {
                        d = new Departamento(null, departamento, new LinkedList<>());
                        h.getDepartamentos().add(d);
                    }

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

                    u.getInformes().add(i);
                    u.getTurnos().add(t);

                }
            }

        } catch (SQLException ex) {
            System.getLogger(ConsultaService.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }

        return resumen;
    }

}
