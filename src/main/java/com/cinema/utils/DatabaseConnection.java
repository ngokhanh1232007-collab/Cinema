package com.cinema.utils;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Lớp tiện ích quản lý kết nối tới cơ sở dữ liệu MySQL.
 * Đọc cấu hình từ file db.properties (File IO) và cung cấp
 * đối tượng Connection dùng chung cho toàn bộ ứng dụng.
 */
public class DatabaseConnection {

    private static final String PROPERTIES_FILE = "db.properties";

    private static String dbUrl;
    private static String dbUsername;
    private static String dbPassword;

    // Biến lưu kết nối duy nhất (Singleton pattern đơn giản)
    private static Connection connection;

    // Khối static: đọc file db.properties ngay khi class được tải
    static {
        loadProperties();
    }

    // Constructor private để ngăn tạo instance từ bên ngoài
    private DatabaseConnection() {
    }

    /**
     * Đọc cấu hình kết nối từ file db.properties nằm trong classpath
     * (thư mục src/main/resources).
     */
    private static void loadProperties() {
        Properties properties = new Properties();
        try (InputStream inputStream = DatabaseConnection.class
                .getClassLoader()
                .getResourceAsStream(PROPERTIES_FILE)) {

            if (inputStream == null) {
                throw new IOException("Không tìm thấy file cấu hình: " + PROPERTIES_FILE
                        + ". Hãy đảm bảo file nằm trong thư mục src/main/resources/");
            }

            properties.load(inputStream);

            dbUrl = properties.getProperty("db.url");
            dbUsername = properties.getProperty("db.username");
            dbPassword = properties.getProperty("db.password");

            if (dbUrl == null || dbUrl.isBlank()) {
                throw new IOException("Thiếu thuộc tính 'db.url' trong file " + PROPERTIES_FILE);
            }
            if (dbUsername == null) {
                throw new IOException("Thiếu thuộc tính 'db.username' trong file " + PROPERTIES_FILE);
            }
            if (dbPassword == null) {
                throw new IOException("Thiếu thuộc tính 'db.password' trong file " + PROPERTIES_FILE);
            }

            System.out.println("[DatabaseConnection] Đã đọc cấu hình từ " + PROPERTIES_FILE + " thành công.");

        } catch (IOException e) {
            System.err.println("[DatabaseConnection] Lỗi đọc file cấu hình: " + e.getMessage());
            throw new RuntimeException("Không thể khởi tạo kết nối database - lỗi đọc cấu hình.", e);
        }
    }

    /**
     * Trả về đối tượng Connection tới MySQL.
     * Nếu chưa có hoặc connection đã đóng, tạo kết nối mới.
     *
     * @return Connection tới database cinema_booking_db
     * @throws SQLException nếu không thể kết nối tới database
     */
    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            try {
                // Nạp driver MySQL (đảm bảo tương thích cả phiên bản cũ và mới)
                Class.forName("com.mysql.cj.jdbc.Driver");

                connection = DriverManager.getConnection(dbUrl, dbUsername, dbPassword);
                System.out.println("[DatabaseConnection] Kết nối MySQL thành công!");

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
                System.out.println("[DatabaseConnection] Test kết nối: OK");
                System.out.println("  - URL: " + dbUrl);
                System.out.println("  - User: " + dbUsername);
                System.out.println("  - Database: " + conn.getCatalog());
                return true;
            }
        } catch (SQLException e) {
            System.err.println("[DatabaseConnection] Test kết nối THẤT BẠI: " + e.getMessage());
        }
        return false;
    }
}

