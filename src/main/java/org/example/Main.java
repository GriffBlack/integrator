package org.example;

import org.example.config.AppConfig;
import org.example.service.ContactService;

import java.util.Random;
import java.util.Timer;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {

        try {
            // 1. Загрузка конфигурации
            AppConfig config = new AppConfig();

            // 2. Инициализация сервиса и генерация данных
            ContactService contactService = new ContactService(config);
            contactService.initializeDatabase();

        } catch (Exception e) {
            LOGGER.severe("Критическая ошибка: " + e.getMessage());
            System.exit(1);
        }

    }
}