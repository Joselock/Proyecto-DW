package com.mycompany.gestorui.model.services.reportes;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.util.HashMap;
import java.util.LinkedList;

import com.mycompany.gestorui.model.entidades.Departamento;
import com.mycompany.gestorui.model.entidades.Hospital;
import com.mycompany.gestorui.model.entidades.Informe;
import com.mycompany.gestorui.model.entidades.Registro;
import com.mycompany.gestorui.model.entidades.Turno;
import com.mycompany.gestorui.model.entidades.Unidad;
import com.mycompany.gestorui.model.utils.BaseDatos;

public class HospitalService {

    // Reporte 3: Obtener resumen de hospitales
    public LinkedList<Hospital> resumenHospitales() {
        LinkedList<Hospital> resumen = new LinkedList<>();

        String function = "SELECT * FROM public.resumenHospitales()";

        try (java.sql.Connection con = BaseDatos.getConnection();) {
            PreparedStatement ps = con.prepareStatement(function);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String hospital = rs.getString("hospital");
                    int cantDep = rs.getInt("cantDepartamentos");
                    int cantUni = rs.getInt("cantUnidades");
                    int cantMed = rs.getInt("cantMedicos");
                    int cantPac = rs.getInt("cantPacientes");

                    resumen.add(new Hospital(null, hospital, cantDep, cantUni, cantMed, cantPac));
                }
            }

        } catch (SQLException ex) {
            System.getLogger(HospitalService.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }

        return resumen;
    }

    // Reporte 4: Obtener listado de hospitales con mas de 100 pacientes(5 primeros)
    public LinkedList<Hospital> hospitalesMayorCantidadPacientes() {
        LinkedList<Hospital> listado = new LinkedList<>();

        try (java.sql.Connection con = BaseDatos.getConnection();) {

            String function = "SELECT * FROM public.listadoMasDeCien()";

            PreparedStatement ps = con.prepareStatement(function);
            try (ResultSet rs = ps.executeQuery();) {
                while (rs.next()) {
                    String hospital = rs.getString("hospital");
                    int cantPacientes = rs.getInt("cantPacientes");

                    listado.add(new Hospital(null, hospital, 0, 0, 0, cantPacientes));

                }
            }

        } catch (Exception ex) {
            System.getLogger(HospitalService.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }

        return listado;
    }

    // Reporte 7: Resumen del proceso
    public LinkedList<Hospital> resumenProceso() {
        LinkedList<Hospital> resumen = new LinkedList<>();
        HashMap<String, Hospital> hospitalMap = new HashMap<>();

        String function = "SELECT * FROM public.resumenProceso()";

        try (java.sql.Connection con = BaseDatos.getConnection();) {
            PreparedStatement ps = con.prepareStatement(function);

            try (ResultSet rs = ps.executeQuery();) {
                while (rs.next()) {
                    String hospital = rs.getString("hospital");
                    String departamento = rs.getString("departamento");
                    String unidad = rs.getString("unidad");
                    int numeroTurno = rs.getInt("numeroTurno");
                    Time horaInforme = rs.getTime("horaInforme");
                    int cantPacInicioConsulta = rs.getInt("cantPacInicioConsulta");
                    int cantPacAtendidos = rs.getInt("cantPacAtendidos");
                    int cantPacTotal = rs.getInt("cantPacTotal");
                    float porcentajePacAtend = rs.getFloat("porcentajePacAtend");
                    int cantPacNoAtendidos = rs.getInt("cantPacNoAtendidos");
                    int cantPacAlta = rs.getInt("cantPacAlta");
                    int cantPacExtranjero = rs.getInt("cantPacExtranjero");
                    int cantPacProvincia = rs.getInt("cantPacProvincia");
                    int cantPacOtraUnidad = rs.getInt("cantPacOtraUnidad");
                    int cantPacOtrasCausas = rs.getInt("cantPacOtrasCausas");
                    int cantPacDesconoce = rs.getInt("cantPacDesconoce");

                    Turno t = new Turno(numeroTurno, cantPacNoAtendidos, null);
                    Informe i = new Informe(horaInforme, null, null, cantPacAtendidos, cantPacAlta, 0,
                            cantPacTotal, cantPacInicioConsulta, porcentajePacAtend, cantPacExtranjero,
                            cantPacProvincia, cantPacOtraUnidad, cantPacOtrasCausas, cantPacDesconoce);

                    Hospital h = hospitalMap.get(hospital);
                    if (h == null) {
                        h = new Hospital(null, hospital, new LinkedList<>());
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
            System.getLogger(HospitalService.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }

        return resumen;
    }

}
