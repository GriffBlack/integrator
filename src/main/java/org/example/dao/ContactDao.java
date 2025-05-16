package org.example.dao;

import org.example.config.AppConfig;
import org.example.model.Contact;
import org.example.util.DbConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class ContactDao {
    private static final Logger LOGGER = Logger.getLogger(ContactDao.class.getName());
    private final String tableName;
    private PreparedStatement batchStatement;
    private final AppConfig config;

    // Поправить добавление конфига и таблицы из него
    public ContactDao(String tableName, AppConfig config) {
        this.tableName = tableName;
        createTableIfNotExists();
        this.config = config;
    }

    // Добавляем геттер для имени таблицы
    public String getTableName() {
        return tableName;
    }

    private void createTableIfNotExists() {
        String sql = String.format("CREATE TABLE IF NOT EXISTS %s " +
                "(id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, phone TEXT)", tableName);

        try (Connection conn = DbConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            LOGGER.info("Таблица '" + tableName + "' создана/проверена");
        } catch (SQLException e) {
            LOGGER.severe("Ошибка создания таблицы: " + e.getMessage());
        }
    }

    public void initBatch(Connection connection) throws SQLException {
        String sql = String.format("INSERT INTO %s (name, phone) VALUES (?, ?)", tableName);
        this.batchStatement = connection.prepareStatement(sql);
    }

    public void insertContactBatch(Connection connection, Contact contact) throws SQLException {
        batchStatement.setString(1, contact.getName());
        batchStatement.setString(2, contact.getPhone());
        batchStatement.addBatch();
    }

    public void executeBatch(Connection connection) throws SQLException {
        try {
            batchStatement.executeBatch();  // Выполняем накопленные запросы
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        }
    }

    public void clearBatch() throws SQLException {
        if (this.batchStatement != null && !this.batchStatement.isClosed()) {
            this.batchStatement.clearBatch(); // Очищаем накопленные batch-запросы
        }
    }

    public void clearDatabase() throws SQLException {
        try (Connection connection = DbConnection.getConnection()) {
            connection.setAutoCommit(false);
            String sql = String.format("DELETE FROM %s", tableName);
            try (Statement stmt = connection.createStatement()) {
                stmt.executeUpdate(sql);
                connection.commit();
                LOGGER.info("База данных очищена");
            }
        }
    }


    public void updateContactPhone(int id, String newPhone) throws SQLException {
        String sql = String.format("UPDATE %s SET phone = ? WHERE id = ?", tableName);
        LOGGER.fine(String.format("Обновление телефона для ID=%d на %s", id, newPhone));

        try (Connection conn = DbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newPhone);
            pstmt.setInt(2, id);
            pstmt.executeUpdate();
        }
    }

    public List<Integer> getRandomContactIds(int limit) throws SQLException {
        String sql = String.format("SELECT id FROM %s ORDER BY RANDOM() LIMIT ?", tableName);
        List<Integer> ids = new ArrayList<>();
        LOGGER.fine("Получение " + limit + " случайных ID контактов");

        try (Connection conn = DbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, limit);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getInt("id"));
                }
            }
        }
        return ids;
    }

    public int getCount() throws SQLException {
        String sql = String.format("SELECT COUNT(*) FROM %s", tableName);
        LOGGER.fine("Подсчет количества записей");

        try (Connection conn = DbConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return rs.getInt(1);
        }
    }

    public void deleteAll(String tableName) throws SQLException {
        String sql = String.format("DELETE FROM %s", tableName);
        try (Connection conn = DbConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
            LOGGER.info("Таблица " + tableName + " очищена");
        }
    }

//    public void insertContact(Contact contact) throws SQLException {
//        String sql = String.format("INSERT INTO %s (name, phone) VALUES (?, ?)", tableName);
//        LOGGER.fine("Вставка контакта: " + contact);
//
//        try (Connection conn = DbConnection.getConnection();
//             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
//            pstmt.setString(1, contact.getName());
//            pstmt.setString(2, contact.getPhone());
//            pstmt.executeUpdate();
//
//            try (ResultSet rs = pstmt.getGeneratedKeys()) {
//                if (rs.next()) {
//                    contact.setId(rs.getInt(1));
//                }
//            }
//        }
//    }

//    public List<Contact> getAllContacts(int limit) throws SQLException {
//        String sql = String.format("SELECT id, name, phone FROM %s LIMIT ?", tableName);
//        List<Contact> contacts = new ArrayList<>();
//        LOGGER.fine("Получение " + limit + " контактов");
//
//        try (Connection conn = DbConnection.getConnection();
//             PreparedStatement pstmt = conn.prepareStatement(sql)) {
//            pstmt.setInt(1, limit);
//
//            try (ResultSet rs = pstmt.executeQuery()) {
//                while (rs.next()) {
//                    Contact contact = new Contact(rs.getString("name"), rs.getString("phone"));
//                    contact.setId(rs.getInt("id"));
//                    contacts.add(contact);
//                }
//            }
//        }
//        return contacts;
//    }

    public void createLoggingInfrastructure() throws SQLException {
        int m = config.getLogRetentionSeconds();
        String logsTableName = config.getLogTableName();

        // вынести функцию создания таблицы в инициацию БД
        // убрать конфиг из логики вынести название таблицы логов

        try (Connection conn = DbConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute("CREATE TABLE IF NOT EXISTS " + logsTableName +  " (" +
                    "log_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "contact_id INTEGER NOT NULL, " +
                    "old_phone TEXT NOT NULL, " +
                    "change_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "FOREIGN KEY(contact_id) REFERENCES " + tableName + "(id))");

            deleteAll(logsTableName);

            String triggerSQL = String.format(
                    "CREATE TRIGGER IF NOT EXISTS log_contact_update " +
                            "AFTER UPDATE OF phone ON %s " +
                            "FOR EACH ROW " +
                            "BEGIN " +
                            "  INSERT INTO %s(contact_id, old_phone) " +
                            "  VALUES (OLD.id, OLD.phone); " +
                            "  " +
                            "  DELETE FROM %s " +
                            "  WHERE contact_id = OLD.id " +
                            "  AND change_time < datetime('now', '-%d seconds'); " +
                            "END",
                    tableName, logsTableName, logsTableName, m);

            stmt.execute(triggerSQL);
        }
    }

    public void dropLogTrigger() throws SQLException {
        try (Connection conn = DbConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute("DROP TRIGGER IF EXISTS " + config.getLogTableName() );
            LOGGER.info("Триггер логирования удален");
        }
    }
}