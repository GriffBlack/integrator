package org.example.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.Random;
import java.util.logging.Logger;

public class AppConfig {
    private static final Logger LOGGER = Logger.getLogger(AppConfig.class.getName());

    private String dbName;
    private String tableName;
    private String logTableName;

    private String[] names;

    private int numRecords;
    private int phoneUpdateMinInterval;
    private int phoneUpdateMaxInterval;
    private int dbDumpMinInterval;
    private int dbDumpMaxInterval;
    private int maxContactsToUpdate;
    private int logRetentionSeconds; // Сгенерированное M
    private int maxLogRetentionSeconds;

    public AppConfig() {
        LOGGER.info("Инициализация конфигурации...");
        loadDefaults();
        loadConfig();
        logConfig();
    }

    private void loadDefaults() {
        dbName = "random_data.db";
        tableName = "contacts";
        logTableName = "contact_logs";
        numRecords = 100000;
        names = new String[]{"Alice", "Bob", "Charlie"};
        phoneUpdateMinInterval = 500;
        phoneUpdateMaxInterval = 5000;
        dbDumpMinInterval = 60;
        dbDumpMaxInterval = 300;
        maxContactsToUpdate = 10;
        maxLogRetentionSeconds = 30;
        logRetentionSeconds = 29;
    }

    private void loadConfig() {
        Properties prop = new Properties();
        try (FileInputStream input = new FileInputStream("src/main/resources/config.properties")) {
            prop.load(input);

            dbName = prop.getProperty("db.name", dbName);
            tableName = prop.getProperty("db.table.name", tableName);
            logTableName = prop.getProperty("log.table.name", logTableName);
            numRecords = Integer.parseInt(prop.getProperty("records.count", String.valueOf(numRecords)));
            phoneUpdateMinInterval = Integer.parseInt(prop.getProperty("phone.update.min.interval",
                    String.valueOf(phoneUpdateMinInterval)));
            phoneUpdateMaxInterval = Integer.parseInt(prop.getProperty("phone.update.max.interval",
                    String.valueOf(phoneUpdateMaxInterval)));
            dbDumpMinInterval = Integer.parseInt(prop.getProperty("db.dump.min.interval",
                    String.valueOf(dbDumpMinInterval)));
            dbDumpMaxInterval = Integer.parseInt(prop.getProperty("db.dump.max.interval",
                    String.valueOf(dbDumpMaxInterval)));
            maxContactsToUpdate = Integer.parseInt(prop.getProperty("max.contacts.to.update",
                    String.valueOf(maxContactsToUpdate)));
            maxLogRetentionSeconds = Integer.parseInt(prop.getProperty("max.log.retention.seconds",
                    String.valueOf(maxLogRetentionSeconds)));

            String namesList = prop.getProperty("names.list");
            if (namesList != null) {
                names = namesList.split(",");
                for (int i = 0; i < names.length; i++) {
                    names[i] = names[i].trim();
                }
            }

            // Генерируем M один раз при создании конфига
            Random random = new Random();
            logRetentionSeconds = 1 + random.nextInt(maxLogRetentionSeconds - 1); // 1 <= M < max

            LOGGER.info("Конфигурация успешно загружена из файла");
        } catch (IOException e) {
            LOGGER.warning("Файл конфигурации не найден, используются значения по умолчанию");
        } catch (NumberFormatException e) {
            LOGGER.warning("Ошибка формата числа в конфиге, используются значения по умолчанию");
        }
    }

    private void logConfig() {
        LOGGER.info("Текущая конфигурация:\n" +
                "DB: " + dbName + "\n" +
                "Table: " + tableName + "\n" +
                "Table Logs: " + logTableName + "\n" +
                "Records: " + numRecords + "\n" +
                "Phone update interval: " + phoneUpdateMinInterval + "-" + phoneUpdateMaxInterval + "ms\n" +
                "DB dump interval: " + dbDumpMinInterval + "-" + dbDumpMaxInterval + "s\n" +
                "Max log retention in seconds: " + logRetentionSeconds + "s\n" +
                "Max contacts to update: " + maxContactsToUpdate);
    }

    // Геттеры
    public String getDbName() { return dbName; }
    public String getTableName() { return tableName; }
    public int getNumRecords() { return numRecords; }
    public String[] getNames() { return names; }
    public int getPhoneUpdateMinInterval() { return phoneUpdateMinInterval; }
    public int getPhoneUpdateMaxInterval() { return phoneUpdateMaxInterval; }
    public int getDbDumpMinInterval() { return dbDumpMinInterval; }
    public int getDbDumpMaxInterval() { return dbDumpMaxInterval; }
    public int getMaxContactsToUpdate() { return maxContactsToUpdate; }
    public int getLogRetentionSeconds() { return logRetentionSeconds; }
    public String getLogTableName() { return logTableName; }
}
