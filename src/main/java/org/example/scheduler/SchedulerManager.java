package org.example.scheduler;

import org.example.config.AppConfig;
import org.example.service.ContactService;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SchedulerManager {
    private static final Logger LOGGER = Logger.getLogger(SchedulerManager.class.getName());

    private final ScheduledExecutorService scheduler;
    private final ContactService contactService;
    private final AppConfig config;
    private volatile boolean isRunning = false;

    public SchedulerManager(ContactService contactService, AppConfig config) {
        this.contactService = contactService;
        this.config = config;
        this.scheduler = Executors.newScheduledThreadPool(2);
    }

    public void startAll() {
        isRunning = true;
        startPhoneUpdater();
        startDatabaseDumper();
        LOGGER.info("Все планировщики запущены");
    }

    private void startPhoneUpdater() {
        // Генерируем начальный интервал
        int initialInterval = config.generateRandomPhoneUpdateInterval();

        LOGGER.info(() -> String.format(
                "PhoneUpdater запущен с начальным интервалом %d мс",
                initialInterval
        ));

        // Запускаем первый раз с динамическим планированием следующего выполнения
        scheduler.schedule(
                new PhoneUpdater(contactService, config, scheduler),
                initialInterval,
                TimeUnit.MILLISECONDS
        );
    }

    private void startDatabaseDumper() {
//        int initialDelay = config.getDbDumpMinInterval();
        int interval = config.getDbDumpInterval();

        scheduler.scheduleAtFixedRate(
                new DatabaseDumper(
                        contactService.getContactDao(),
                        config,
                        this::shutdown
                ),
                interval,
                interval,
                TimeUnit.SECONDS
        );

        LOGGER.info(() -> String.format(
                "DatabaseDumper запущен с начальной задержкой %d",
                interval
        ));
    }

//    public void stopAll() {
//        scheduler.shutdown();
//        try {
//            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
//                scheduler.shutdownNow();
//            }
//        } catch (InterruptedException e) {
//            scheduler.shutdownNow();
//            Thread.currentThread().interrupt();
//        }
//        LOGGER.info("Все планировщики остановлены");
//    }
public void shutdown() {
    if (isRunning) {
        isRunning = false;
        try {
            // 1. Остановка планировщиков
            scheduler.shutdownNow();

//            try {
//            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
//                scheduler.shutdownNow();
//                LOGGER.info("Получен сигнал завершения gjckt if");
//
//            }
//            } catch (InterruptedException e) {
//                scheduler.shutdownNow();
//                Thread.currentThread().interrupt();
//            }

            // 2. Очистка триггеров базы данных
            contactService.cleanupDatabase();

            LOGGER.info("Все сервисы остановлены и база данных очищена");
            System.exit(0);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Ошибка при завершении работы", e);
            System.exit(1);
        }
    }
}
}