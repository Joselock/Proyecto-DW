package com.mycompany.gestorui.model.services.reportes;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;

import com.mycompany.gestorui.model.entidades.Departamento;
import com.mycompany.gestorui.model.entidades.Hospital;
import com.mycompany.gestorui.model.entidades.Informe;
import com.mycompany.gestorui.model.entidades.Medico;
import com.mycompany.gestorui.model.entidades.Registro;
import com.mycompany.gestorui.model.entidades.Unidad;
import com.mycompany.gestorui.model.utils.BaseDatos;

public class UnidadService {

    //Reporte 8: Listado de unidades que deben revisar turno 
    public LinkedList<Hospital> listadoUnidades() {
        LinkedList<Hospital> listado = new LinkedList<>();
        HashMap<String, Hospital> hospitalMap = new HashMap<>();

        String function = "SELECT * FROM public.revisarTurnos()";

        try (java.sql.Connection con = BaseDatos.getConnection()) {
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
            System.getLogger(UnidadService.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }

        return listado;
    }

}
