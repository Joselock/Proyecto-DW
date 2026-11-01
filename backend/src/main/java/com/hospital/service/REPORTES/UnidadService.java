package com.hospital.service.REPORTES;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;

import com.hospital.model.Entidades.Departamento;
import com.hospital.model.Entidades.Hospital;
import com.hospital.model.Entidades.Informe;
import com.hospital.model.Entidades.Medico;
import com.hospital.model.Entidades.Registro;
import com.hospital.model.Entidades.Unidad;

@org.springframework.stereotype.Service
public class UnidadService extends com.hospital.service.JdbcServiceSupport {

    public UnidadService(javax.sql.DataSource dataSource) {
        super(dataSource);
    }

    //Reporte 8: Listado de unidades que deben revisar turno 
    public LinkedList<Hospital> listadoUnidades() {
        LinkedList<Hospital> listado = new LinkedList<>();
        HashMap<String, Hospital> hospitalMap = new HashMap<>();

        String function = "SELECT * FROM public.revisarTurnos()";

        try (java.sql.Connection con = getConnection()) {
            PreparedStatement ps = con.prepareStatement(function);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String hospital = rs.getString("hospital");
                    String departamento = rs.getString("departamento");
                    String unidad = rs.getString("unidad");
                    int totalPacientes = rs.getInt("totalPacientes");
                    String nombreMedico = rs.getString("nombreMedico");
                    int cantPacAten = rs.getInt("cantPacAten");
                    float porcentajeAten = rs.getFloat("porcentajeAten");

                    Medico m = new Medico(null, nombreMedico, null, null, null, 0, null);
                    Informe i = new Informe(null, null, null, cantPacAten, 0, 0, totalPacientes, 0, porcentajeAten,
                            0, 0, 0, 0, 0);

                    Hospital h = hospitalMap.get(hospital);
                    if (h == null) {
                        h = new Hospital(null, hospital, new LinkedList<>());
                        hospitalMap.put(hospital, h);
                        listado.add(h);
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
                    u.getMedicos().add(m);

                }
            }

        } catch (SQLException ex) {
            throw new IllegalStateException("No se pudo consultar las unidades con turnos por revisar.", ex);
        }

        return listado;
    }

}
