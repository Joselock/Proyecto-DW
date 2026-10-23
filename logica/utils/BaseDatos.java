package com.mycompany.gestorui.model.utils;

import java.sql.SQLException;

public class BaseDatos {

    public static java.sql.Connection getConnection(){
		Connection connection = null;
		try {
			connection = new Connection("localhost", "Gestion Hospitalaria", "postgres", "admin123");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return connection.getConnection();
	}

}
