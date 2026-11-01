package com.hospital.service.REPORTES;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;

import com.hospital.model.Entidades.Departamento;
import com.hospital.model.Entidades.Hospital;
import com.hospital.model.Entidades.Medico;
import com.hospital.model.Entidades.Registro;
import com.hospital.model.Entidades.Unidad;

@org.springframework.stereotype.Service
public class MedicoService extends com.hospital.service.JdbcServiceSupport {

    public MedicoService(javax.sql.DataSource dataSource) {
        super(dataSource);
    }

    // Reporte 2: Obtener listado de medicos
    public LinkedList<Hospital> obtenerListadosMedicos() {
        LinkedList<Hospital> listado = new LinkedList<>();
        HashMap<String, Hospital> hospitalMap = new HashMap<>();

        try (java.sql.Connection con = getConnection()) {
            String function = "SELECT * FROM public.listadoMedicos()";

            PreparedStatement ps = con.prepareStatement(function);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String hospital = rs.getString("hospital");
                    String departamento = rs.getString("departamento");
                    String unidad = rs.getString("unidad");
                    String nombre = rs.getString("nombre");
                    String licencia = rs.getString("licencia");
                    String especialidad = rs.getString("especialidad");
                    String telefono = rs.getString("telefono");
                    int experiencia = rs.getInt("experiencia");
                    String datCon = rs.getString("datosCon");

                    Medico m = new Medico(null, nombre, especialidad, licencia, datCon, experiencia, telefono);

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

                    u.getMedicos().add(m);

                }
            }

        } catch (SQLException ex) {
            throw new IllegalStateException("No se pudo consultar el listado de médicos.", ex);
        }

        return listado;
    }

}
