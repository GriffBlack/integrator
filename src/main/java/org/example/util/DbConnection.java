package org.example.util;

import org.example.config.AppConfig;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Logger;

public class DbConnection {
    private static final Logger LOGGER = Logger.getLogger(DbConnection.class.getName());
    private static AppConfig config = new AppConfig();

    public static Connection getConnection() throws SQLException {
        String url = "jdbc:sqlite:" + System.getProperty("user.dir") + "/" + config.getDbName();
        LOGGER.fine("Подключение к базе данных: " + url);
        return DriverManager.getConnection(url);
    }

    public static void testConnection() {
        try (Connection conn = getConnection()) {
            LOGGER.info("Тест подключения к БД успешен");
        } catch (SQLException e) {
            LOGGER.severe("Ошибка подключения к БД: " + e.getMessage());
        }
    }
}