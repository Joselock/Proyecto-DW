package com.mycompany.gestorui.model.login.loginSevice;

import com.mycompany.gestorui.model.login.entidad.User;
import com.mycompany.gestorui.model.login.entidad.DataUser;
import com.mycompany.gestorui.model.utils.BaseDatosLogin;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Verificacion {

    public static boolean verificarUsuario(String usernameOrEmail, String password) throws SQLException {
        boolean existe = false;
        String sql = "SELECT func_login(?, ?)";

        try (java.sql.Connection con = BaseDatosLogin.getConnection()) {

             PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, usernameOrEmail);
            ps.setString(2, password);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    existe = rs.getBoolean(1);
                }
            }
        }
        return existe;
    }

    public static User obtenerUsuario(String usernameOrEmail) throws SQLException {
        User user = null;
        String sql = "SELECT * FROM func_obtener_usuario(?)";

        try (java.sql.Connection con = BaseDatosLogin.getConnection()) {

            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, usernameOrEmail);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {

                    user = new User(
                            rs.getString("username"),
                            rs.getString("email"),
                            null,
                            new DataUser(
                                    rs.getString("nombre"),
                                    rs.getString("especialidad"),
                                    rs.getString("direccion"),
                                    rs.getString("telefono")
                            )
                    );

                }
            }
        }
        return user;
    }
}
