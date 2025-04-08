package org.example;

import java.sql.*;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Hello world!
 *
 */
public class App 
{
    public static Connection connect;
    public static Statement statemant;
    public static ResultSet resSet;
    private static final Random RANDOM = new Random();
    private static final Logger LOGGER = Logger.getLogger(App.class.getName());
    private static final int NUM_RECORDS = 100000;

    private static final String DB_NAME = "test1.db"; // Название базы данных
    private static final String DB_URL = "jdbc:sqlite:" + System.getProperty("user.dir") + "/" + DB_NAME; // Путь к базе данных
    private static final String TABLE_NAME = "contacts"; // Название таблицы
    private static final String CREATE_TABLE_SQL = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, phone TEXT)";
    private static final String INSERT_SQL = "INSERT INTO " + TABLE_NAME + " (name, phone) VALUES (?, ?)";
    private static final String DELETE_SQL = "DELETE FROM " + TABLE_NAME; // SQL-запрос для удаления всех записей

//     private static final String INSERT_SQL = "INSERT INTO contacts (name, phone) VALUES (?, ?)";


    // Массив с предопределенными именами для генерации случайных записей
    private static final String[] NAMES = {
            "Alice", "Bob", "Charlie", "David", "Eva", "Frank", "Grace", "Hannah",
            "Ivy", "Jack", "Kathy", "Leo", "Mia", "Nina", "Oscar", "Paul"
    };

    public static void main( String[] args ) throws SQLException {
        LOGGER.info("Запуск программы генерации случайных данных.");
        App program = new App();
        if (program.open()) {
            program.set_table();
            deleteAllData(connect); // Удаляем все записи перед генерацией новых
            insertRandomData(connect);
//            set_value("maikl", "123457");
            get_table();
            program.close();
        }
        LOGGER.info("Завершение работы программы.");
    }

    private boolean open()
    {
        try {
            // Указываем путь к базе данных
            connect = DriverManager.getConnection(DB_URL);
            System.out.println("Connect.");
            LOGGER.log(Level.SEVERE, "Connect DB: ");
            return  true;
        }
        catch  (Exception e) {
            System.out.println(e.getMessage());
            LOGGER.log(Level.SEVERE, "Ошибка при работе с базой данных: ", e);
            return false;
        }
    }
    private void close()
    {
        try {
            connect.close();
            System.out.println("connect close.");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Ошибка при создании таблицы: ", e);
            System.out.println(e.getMessage());
        }
    }
    private void set_table()
    {
//        String query_create_table =
//                "CREATE TABLE IF NOT EXISTS contacts( " +
//                        "id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
//                        "name WARCHAR(50), " +
//                        "phone VARCHAR(50))";
        try {
            statemant = connect.createStatement();
            statemant.executeUpdate(CREATE_TABLE_SQL);

            LOGGER.info("Таблица 'contacts' успешно создана или уже существует.");
            System.out.println("create table.");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

    }

    /**
     * Вставляет заданное количество случайных записей в таблицу.
     *
     * @param connection соединение с базой данных
     * @throws SQLException если возникает ошибка при выполнении SQL-запроса
     */
    private static void insertRandomData(Connection connection) throws SQLException {
        // Отключаем автокоммит
        connection.setAutoCommit(false);
        try (PreparedStatement preparedStatement = connection.prepareStatement(INSERT_SQL)) {
            for (int i = 0; i < NUM_RECORDS; i++) {
                String name = getRandomName();
                String phone = getRandomPhoneNumber();
                preparedStatement.setString(1, name);
                preparedStatement.setString(2, phone);
                preparedStatement.addBatch();

                // Выполняем пакетную вставку каждые 1000 записей для повышения производительности
                if (i % 1000 == 0) {
                    preparedStatement.executeBatch();
                    LOGGER.info("Вставлено " + (i + 1) + " записей.");
                }
            }
            // Выполняем оставшиеся записи в последнем пакете
            preparedStatement.executeBatch();
            LOGGER.info("Вставлено всего записей: " + NUM_RECORDS);

            // Коммитим транзакцию
            connection.commit();
        } catch (SQLException e) {
            // В случае ошибки откатываем транзакцию
            connection.rollback();
            throw e; // Пробрасываем исключение дальше
        } finally {
            // Включаем автокоммит обратно
            connection.setAutoCommit(true);
        }
    }

    /**
     * Генерирует случайное имя из предопределенного массива имен.
     *
     * @return случайное имя
     */
    private static String getRandomName() {
        return NAMES[RANDOM.nextInt(NAMES.length)];
    }

    /**
     * Генерирует случайный номер телефона в формате XXX-XXXXX.
     *
     * @return случайный номер телефона
     */
    private static String getRandomPhoneNumber() {
        return String.format("%03d-%05d", RANDOM.nextInt(900) + 100, RANDOM.nextInt(100000));
    }

    /**
     * Удаляет все записи из таблицы contacts.
     *
     * @param connection соединение с базой данных
     * @throws SQLException если возникает ошибка при выполнении SQL-запроса
     */
    private static void deleteAllData(Connection connection) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement(DELETE_SQL)) {
            int deletedRows = preparedStatement.executeUpdate();
            LOGGER.info("Удалено записей: " + deletedRows);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Ошибка при удалении данных: ", e);
        }
    }

    static private void set_value(String name, String phone)
    {
        String query_insert_value =
                "INSERT INTO contacts (name, phone) " +
                        "VALUES ('" + name + "', '" + phone + "');";
        try {
            statemant = connect.createStatement();
            statemant.executeUpdate(query_insert_value);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    static private void get_table()
    {
        try {
//                resSet = statemant.executeQuery("SELECT * FROM users");
            resSet = statemant.executeQuery("SELECT COUNT(*) FROM contacts");

//            while (resSet.next()) {
//                int id = resSet.getInt("id");
//                String name = resSet.getString("name");
//                String phone = resSet.getString("phone");
//                System.out.println("ID = " + id + "name = " + name + " phone = " + phone);
//            }
            System.out.println(resSet.getString(1));
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
}
