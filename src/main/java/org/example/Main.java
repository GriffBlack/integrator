package org.example;

import org.example.config.AppConfig;
import org.example.service.ContactService;
import org.example.scheduler.SchedulerManager;
import org.example.util.DbConnection;

import java.util.Random;
import java.util.Timer;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());
    private static SchedulerManager schedulerManager;

    public static void main(String[] args) {

        try {
            // 1. Загрузка конфигурации
            LOGGER.info("Инициализация конфигурации...");
            AppConfig config = new AppConfig();
            DbConnection.initialize(config);

            // 2. Инициализация сервисов
            LOGGER.info("Инициализация сервисов...");
            ContactService contactService = new ContactService(config);

            // 3. Подготовка базы данных
            LOGGER.info("Подготовка базы данных...");
            contactService.initializeDatabase();

            LOGGER.info("Запуск планировщиков...");
            schedulerManager = new SchedulerManager(contactService, config);
            schedulerManager.startAll();
//            // Завершение работы
//                try {
//                    LOGGER.info("2. Перед вызовом cleanupDatabase()");
//                    contactService.cleanupDatabase();
//                    LOGGER.info("3. После успешного выполнения cleanupDatabase()");
//                } catch (Exception e) {
//                    LOGGER.severe("ОШИБКА в shutdown hook" + e.getMessage());
//                } finally {
//                    LOGGER.info("4. Завершение shutdown hook");
//                }
//                LOGGER.info("11Завершение работы приложения...");

            // 5. Настройка завершения работы
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                LOGGER.info("Получен сигнал завершения...");
                LOGGER.info("исполнен");
                schedulerManager.shutdown();
            }));

            // 6. Ожидание завершения
            keepMainThreadAlive();

        } catch (Exception e) {
            LOGGER.severe("Критическая ошибка: " + e.getMessage());
            System.exit(1);
        }
    }

        private static void keepMainThreadAlive() {
            LOGGER.info("Приложение успешно запущено и работает...");
            synchronized (Main.class) {
                try {
                    Main.class.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    LOGGER.warning("Главный поток прерван");
                }
            }
        }
}