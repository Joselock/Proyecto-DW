package com.hospital.service;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessResourceFailureException;

public abstract class JdbcServiceSupport {

    private final DataSource dataSource;

    @Autowired
    protected JdbcServiceSupport(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    protected final java.sql.Connection getConnection() {
        try {
            return dataSource.getConnection();
        } catch (java.sql.SQLException exception) {
            throw new DataAccessResourceFailureException("No se pudo conectar con PostgreSQL.", exception);
        }
    }
}
