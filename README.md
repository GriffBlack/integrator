# Система мониторинга изменений контактов

![Java](https://img.shields.io/badge/Java-17%2B-blue)
![SQLite](https://img.shields.io/badge/SQLite-3-lightgrey)
![Лицензия](https://img.shields.io/badge/Лицензия-MIT-green)

Многопоточное Java-приложение для отслеживания и логирования изменений телефонных номеров в базе данных с настраиваемыми интервалами обновления.

## Основные возможности

- 🕒 Периодическое обновление номеров со случайными интервалами
- 📝 Подробное логирование изменений с периодом хранения
- ⚙️ Гибкая настройка через `config.properties`
- 🛑 Корректное завершение с отчетом об изменениях
- 📊 База данных SQLite с триггерами отслеживания изменений


## Технологии

- **Ядро**: Java 17+
- **База данных**: SQLite (через JDBC)
- **Многопоточность**: `ScheduledExecutorService`
- **Логирование**: `java.util.logging` с ротацией файлов
- **Сборка**: Совместимость с Maven/Gradle

## Схема базы данных

```sql
CREATE TABLE contacts (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    phone TEXT NOT NULL
);

CREATE TABLE contact_logs (
    log_id INTEGER PRIMARY KEY AUTOINCREMENT,
    contact_id INTEGER NOT NULL,
    old_phone TEXT NOT NULL,
    change_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY(contact_id) REFERENCES contacts(id)
);

CREATE TRIGGER log_contact_update 
AFTER UPDATE OF phone ON contacts
FOR EACH ROW
BEGIN
    INSERT INTO contact_logs(contact_id, old_phone)
    VALUES (OLD.id, OLD.phone);
END;