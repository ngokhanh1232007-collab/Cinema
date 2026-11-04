package com.cinema.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Lớp tiện ích quản lý kết nối tới cơ sở dữ liệu MySQL (MySQL 8.4).
 * Đọc cấu hình từ file db.properties (File IO) và cung cấp
 * đối tượng Connection dùng chung cho toàn bộ ứng dụng.
 *
 * <p>Cấu hình mặc định cho đồ án:
 * <ul>
 *   <li>Host: localhost:3306</li>
 *   <li>Database/Schema: cinema_db</li>
 *   <li>User: admin</li>
 *   <li>Password: MatKhau123@</li>
 *   <li>Cờ kết nối: useSSL=false, serverTimezone=UTC, allowPublicKeyRetrieval=true</li>
 * </ul>
 * </p>
 */
public class DatabaseConnection {

    private static final String PROPERTIES_FILE = "db.properties";

    // Cấu hình kết nối mặc định tới MySQL 8.4
    public static final String DEFAULT_URL = "jdbc:mysql://localhost:3306/cinema_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&characterEncoding=UTF-8";
    public static final String DEFAULT_USERNAME = "admin";
    public static final String DEFAULT_PASSWORD = "MatKhau123@";

    private static String dbUrl = DEFAULT_URL;
    private static String dbUsername = DEFAULT_USERNAME;
    private static String dbPassword = DEFAULT_PASSWORD;

    // Biến lưu kết nối duy nhất (Singleton pattern đơn giản)
    private static Connection connection;

    // Khối static: nạp cấu hình ngay khi class được tải
    static {
        loadProperties();
    }

    // Constructor private để ngăn tạo instance từ bên ngoài
    private DatabaseConnection() {
    }

    /**
     * Đọc cấu hình kết nối từ file db.properties nằm trong classpath
     * (thư mục src/main/resources) hoặc thư mục làm việc.
     * Nếu không tìm thấy file, tự động sử dụng cấu hình mặc định (admin / MatKhau123@ / cinema_db).
     */
    private static void loadProperties() {
        Properties properties = new Properties();
        boolean loaded = false;

        // Cách 1: Đọc từ classpath (src/main/resources)
        try (InputStream inputStream = DatabaseConnection.class
                .getClassLoader()
                .getResourceAsStream(PROPERTIES_FILE)) {

            if (inputStream != null) {
                properties.load(inputStream);
                loaded = true;
                System.out.println("[DatabaseConnection] Đã nạp cấu hình từ classpath: " + PROPERTIES_FILE);
            }
        } catch (IOException e) {
            System.err.println("[DatabaseConnection] Cảnh báo đọc từ classpath: " + e.getMessage());
        }

        // Cách 2: Nếu chưa nạp được, thử đọc trực tiếp từ thư mục src/main/resources hoặc thư mục hiện tại
        if (!loaded) {
            File file = new File("src/main/resources/" + PROPERTIES_FILE);
            if (!file.exists()) {
                file = new File(PROPERTIES_FILE);
            }

            if (file.exists() && file.isFile()) {
                try (InputStream fis = new FileInputStream(file)) {
                    properties.load(fis);
                    loaded = true;
                    System.out.println("[DatabaseConnection] Đã nạp cấu hình từ file: " + file.getAbsolutePath());
                } catch (IOException e) {
                    System.err.println("[DatabaseConnection] Cảnh báo đọc file: " + e.getMessage());
                }
            }
        }

        if (loaded) {
            String url = properties.getProperty("db.url");
            String user = properties.getProperty("db.username");
            String pass = properties.getProperty("db.password");

            if (url != null && !url.isBlank()) {
                dbUrl = url.trim();
            }
            if (user != null && !user.isBlank()) {
                dbUsername = user.trim();
            }
            if (pass != null) {
                dbPassword = pass.trim();
            }
        } else {
            System.out.println("[DatabaseConnection] Không tìm thấy db.properties, sử dụng cấu hình mặc định (User: "
                    + DEFAULT_USERNAME + ", DB: cinema_db, Timezone: UTC).");
        }
    }

    /**
     * Trả về đối tượng Connection tới MySQL.
     * Nếu chưa có hoặc connection đã đóng, tạo kết nối mới qua DriverManager.
     *
     * @return Connection tới database cinema_db
     * @throws SQLException nếu không thể kết nối tới database
     */
    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            try {
                // Nạp driver MySQL
                Class.forName("com.mysql.cj.jdbc.Driver");

                String url = (dbUrl != null && !dbUrl.isBlank()) ? dbUrl : DEFAULT_URL;
                String user = (dbUsername != null && !dbUsername.isBlank()) ? dbUsername : DEFAULT_USERNAME;
                String pass = (dbPassword != null) ? dbPassword : DEFAULT_PASSWORD;

                connection = DriverManager.getConnection(url, user, pass);
                System.out.println("[DatabaseConnection] Kết nối MySQL (cinema_db) thành công!");

            } catch (ClassNotFoundException e) {
                System.err.println("[DatabaseConnection] Không tìm thấy MySQL JDBC Driver.");
                throw new SQLException("Không tìm thấy MySQL JDBC Driver. "
                        + "Hãy kiểm tra dependency mysql-connector-j trong pom.xml.", e);
            } catch (SQLException e) {
                System.err.println("[DatabaseConnection] Không thể kết nối tới database: " + e.getMessage());
                throw e;
            }
        }
        return connection;
    }

    /**
     * Khởi tạo kết nối mới theo tham số chỉ định (hỗ trợ kiểm thử linh hoạt).
     *
     * @param url      chuỗi JDBC URL
     * @param username tên tài khoản MySQL
     * @param password mật khẩu
     * @return Connection mới
     * @throws SQLException nếu kết nối thất bại
     */
    public static Connection getConnection(String url, String username, String password) throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            return DriverManager.getConnection(url, username, password);
        } catch (ClassNotFoundException e) {
            throw new SQLException("Không tìm thấy MySQL JDBC Driver: " + e.getMessage(), e);
        }
    }

    /**
     * Cập nhật thông tin tài khoản kết nối động khi đang chạy.
     */
    public static void setDbCredentials(String url, String username, String password) {
        closeConnection();
        if (url != null && !url.isBlank()) dbUrl = url;
        if (username != null && !username.isBlank()) dbUsername = username;
        if (password != null) dbPassword = password;
    }

    public static String getDbUrl() {
        return (dbUrl != null && !dbUrl.isBlank()) ? dbUrl : DEFAULT_URL;
    }

    public static String getDbUsername() {
        return (dbUsername != null && !dbUsername.isBlank()) ? dbUsername : DEFAULT_USERNAME;
    }

    /**
     * Đóng kết nối tới database.
     * Nên gọi khi ứng dụng kết thúc để giải phóng tài nguyên.
     */
    public static void closeConnection() {
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    connection.close();
                    System.out.println("[DatabaseConnection] Đã đóng kết nối MySQL.");
                }
            } catch (SQLException e) {
                System.err.println("[DatabaseConnection] Lỗi khi đóng kết nối: " + e.getMessage());
            } finally {
                connection = null;
            }
        }
    }

    /**
     * Kiểm tra kết nối tới database có hoạt động hay không.
     *
     * @return true nếu kết nối thành công, false nếu thất bại
     */
    public static boolean testConnection() {
        try {
            Connection conn = getConnection();
            if (conn != null && !conn.isClosed()) {
                System.out.println("[DatabaseConnection] Test kết nối MySQL: OK");
                System.out.println("  - URL: " + getDbUrl());
                System.out.println("  - User: " + getDbUsername());
                System.out.println("  - Database: " + conn.getCatalog());
                return true;
            }
        } catch (SQLException e) {
            System.err.println("[DatabaseConnection] Test kết nối THẤT BẠI: " + e.getMessage());
        }
        return false;
    }
}
