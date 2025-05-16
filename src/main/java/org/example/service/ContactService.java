package org.example.service;

import org.example.config.AppConfig;
import org.example.dao.ContactDao;
import org.example.model.Contact;
import org.example.util.DbConnection;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ContactService {
    private static final Logger LOGGER = Logger.getLogger(ContactService.class.getName());
    private static final int BATCH_SIZE = 1000;

    private final ContactDao contactDao;
    private final String[] names;
    private final int numRecords;
    private final int maxContactsToUpdate;
    private final Random random = new Random();

    public ContactService(AppConfig config) {
        LOGGER.info("Инициализация ContactService с конфигурацией...");
        this.contactDao = new ContactDao(config.getTableName(), config);
        this.names = config.getNames();
        this.numRecords = config.getNumRecords();
        this.maxContactsToUpdate = config.getMaxContactsToUpdate();
    }

    public void initializeDatabase() throws SQLException {
        LOGGER.info("Начало инициализации базы данных...");
        int existingCount = contactDao.getCount();

        if (existingCount > 0) {
            LOGGER.info("Обнаружено существующих записей: " + existingCount);
            if (shouldRegenerateData()) {
                regenerateData();
            }
        } else {
            generateData();
        }

        //  Инициализация системы логирования
        LOGGER.info("Инициализация системы логирования...");
        contactDao.createLoggingInfrastructure();
    }

    private boolean shouldRegenerateData() {
        System.out.print("В таблице уже есть данные. Перегенерировать? (y/n): ");
        return new Scanner(System.in).next().equalsIgnoreCase("y");
    }

    public void regenerateData() throws SQLException {
        LOGGER.info("Перегенерация данных...");
        contactDao.deleteAll(contactDao.getTableName());
        generateData();
    }

    public void generateData() throws SQLException {
        LOGGER.info(String.format(
                "Начало генерации %d записей (пакетами по %d)...",
                numRecords, BATCH_SIZE
        ));

        long startTime = System.currentTimeMillis();

        try (Connection connection = DbConnection.getConnection()) {
            connection.setAutoCommit(false);
            contactDao.initBatch(connection);  // Инициализируем batch

            for (int i = 1; i <= numRecords; i++) {
                Contact contact = new Contact(
                        names[random.nextInt(names.length)],
                        generateRandomPhoneNumber()
                );

                contactDao.insertContactBatch(connection, contact);

                if (i % BATCH_SIZE == 0 || i == numRecords) {
                    contactDao.executeBatch(connection);   // Выполняем batch
                    LOGGER.info(String.format(
                            "Обработано: %d/%d (%.1f%%)",
                            i, numRecords, (i * 100.0 / numRecords)
                    ));

                    // Очищаем batch для следующего использования
                    contactDao.clearBatch();
                }
            }


        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Ошибка генерации данных", e);
            throw e;
        }

        double duration = (System.currentTimeMillis() - startTime) / 1000.0;
        LOGGER.info(String.format(
                "Генерация завершена за %.2f сек (%.1f записей/сек)",
                duration, numRecords / duration
        ));
    }


    public void updateRandomContactsPhones() throws SQLException {
        int numToUpdate = 1 + random.nextInt(maxContactsToUpdate);
        LOGGER.info("Обновление номеров для " + numToUpdate + " контактов");

        List<Integer> contactIds = contactDao.getRandomContactIds(numToUpdate);

        for (int id : contactIds) {
            String newPhone = generateRandomPhoneNumber();
            contactDao.updateContactPhone(id, newPhone);
            LOGGER.fine("Обновлен контакт ID=" + id + ", новый телефон: " + newPhone);
        }
    }

    private String generateRandomPhoneNumber() {
        return String.format("%03d-%05d", random.nextInt(900) + 100, random.nextInt(100000));
    }

    public void cleanupDatabase() {
        try {
            contactDao.dropLogTrigger();
            LOGGER.info("Очистка базы данных выполнена");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Ошибка при очистке базы данных", e);
        }
    }
    public ContactDao getContactDao() {
        return contactDao;
    }
}