package org.example.scheduler;

import org.example.config.AppConfig;
import org.example.service.ContactService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PhoneUpdater implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(PhoneUpdater.class.getName());

    private final ContactService contactService;
    private final AppConfig config;
    private final ScheduledExecutorService scheduler;
    private int executionCount = 0;

    public PhoneUpdater(ContactService contactService, AppConfig config, ScheduledExecutorService scheduler) {
        this.contactService = contactService;
        this.config = config;
        this.scheduler = scheduler;
    }

    @Override
    public void run() {
        try {
            executionCount++;
            LOGGER.fine("Запуск обновления телефонных номеров (выполнение #" + executionCount);

            // Основная логика обновления
            contactService.updateRandomContactsPhones();

            // Генерируем новый интервал
            int newInterval = config.generateRandomPhoneUpdateInterval();
            LOGGER.fine(String.format(
                    "Обновление номеров завершено. Следующее выполнение через %d мс",
                    newInterval
            ));

            // Планируем следующее выполнение с новым интервалом
            scheduler.schedule(
                    this,
                    newInterval,
                    TimeUnit.MILLISECONDS
            );

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Ошибка при обновлении телефонных номеров", e);
        }
    }
}

