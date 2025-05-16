package org.example.scheduler;

import org.example.config.AppConfig;
import org.example.dao.ContactDao;
import org.example.model.Contact;
import org.example.util.DbConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class DatabaseDumper implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(DatabaseDumper.class.getName());

    private final AppConfig config;
    private final Runnable shutdownCallback;

    public DatabaseDumper(ContactDao contactDao, AppConfig config, Runnable shutdownCallback) {
        this.config = config;
        this.shutdownCallback = shutdownCallback;
    }

    @Override
    public void run() {
        try {
            // 1. Получаем объединенные данные
            List<ContactWithHistory> contactsWithHistory = getContactsWithHistory(config.getLogRetentionSeconds());

            // 2. Выводим в лог
            logContactsWithHistory(contactsWithHistory);

            // 3. Инициируем остановку
            shutdownCallback.run();

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Ошибка при выполнении дампа", e);
            System.exit(1);
        }
    }

    private List<ContactWithHistory> getContactsWithHistory(int retentionSeconds) throws SQLException {
        // Получаем текущие контакты
        List<Contact> currentContacts = getCurrentContacts();

        // Получаем исторические данные за указанный период
        Map<Integer, PhoneHistory> latestPhoneHistory = getLatestPhoneHistory(retentionSeconds);

        // Объединяем данные
        List<ContactWithHistory> result = new ArrayList<>();
        for (Contact contact : currentContacts) {
            PhoneHistory history = latestPhoneHistory.get(contact.getId());
            result.add(new ContactWithHistory(
                    contact,
                    history != null ? history.oldPhone() : null,
                    history != null ? history.changeTime() : null
            ));
        }

        return result;
    }

    private List<Contact> getCurrentContacts() throws SQLException {
        String sql = "SELECT id, name, phone FROM " + config.getTableName();

        List<Contact> contacts = new ArrayList<>();
        try (Connection conn = DbConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Contact contact = new Contact(
                        rs.getString("name"),
                        rs.getString("phone")
                );
                contact.setId(rs.getInt("id"));
                contacts.add(contact);
            }
        }
        return contacts;
    }

    private Map<Integer, PhoneHistory> getLatestPhoneHistory(int retentionSeconds) throws SQLException {
        String sql = String.format(
                "SELECT contact_id, old_phone, change_time FROM " +
                        "(SELECT contact_id, old_phone, change_time, " +
                        "ROW_NUMBER() OVER (PARTITION BY contact_id ORDER BY change_time DESC) as rn " +
                        "FROM %s WHERE change_time >= datetime('now', '-%d seconds')) " +
                        "WHERE rn = 1",
                config.getLogTableName(),
                retentionSeconds
        );

        Map<Integer, PhoneHistory> historyMap = new HashMap<>();
        try (Connection conn = DbConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                historyMap.put(rs.getInt("contact_id"),
                        new PhoneHistory(
                                rs.getString("old_phone"),
                                rs.getTimestamp("change_time")
                        ));
            }
        }
        return historyMap;
    }

    private void logContactsWithHistory(List<ContactWithHistory> contacts) {
        LOGGER.info("=== ТЕКУЩЕЕ СОСТОЯНИЕ КОНТАКТОВ С ИСТОРИЕЙ ===");
        LOGGER.info(String.format("%-4s | %-15s | %-12s | %-12s | %-20s",
                "ID", "Имя", "Текущий тел.", "Старый тел.", "Время изменения"));

        contacts.forEach(c -> {
            String oldPhone = c.oldPhone() != null ? c.oldPhone() : "N/A";
            String changeTime = c.changeTime() != null ? c.changeTime().toString() : "N/A";

            LOGGER.info(String.format(
                    "%-4d | %-15s | %-12s | %-12s | %-20s",
                    c.contact().getId(),
                    c.contact().getName(),
                    c.contact().getPhone(),
                    oldPhone,
                    changeTime
            ));
        });

        long withHistory = contacts.stream().filter(c -> c.oldPhone() != null).count();
        LOGGER.info("\n=== ИТОГО ===");
        LOGGER.info("Всего контактов: " + contacts.size());
        LOGGER.info("С историей изменений: " + withHistory);
        LOGGER.info("Без истории изменений: " + (contacts.size() - withHistory));
    }

    private record PhoneHistory(String oldPhone, Timestamp changeTime) {}
    private record ContactWithHistory(Contact contact, String oldPhone, Timestamp changeTime) {}
}