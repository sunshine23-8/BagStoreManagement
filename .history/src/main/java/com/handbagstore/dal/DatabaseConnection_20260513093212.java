package com.handbagstore.dal;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import com.handbagstore.utils.Config;

/**
 * Quản lý kết nối database SQL Server.
 * Singleton pattern — tái sử dụng connection.
 */
public class DatabaseConnection {
    private static DatabaseConnection instance;
    private Connection connection;

    private DatabaseConnection() {
        // Load SQL Server JDBC Driver
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        } catch (ClassNotFoundException e) {
            System.err.println("SQL Server JDBC Driver không tìm thấy: " + e.getMessage());
        }
    }

    public static synchronized DatabaseConnection getInstance() {
        if (instance == null) {
            instance = new DatabaseConnection();
        }
        return instance;
    }

    /**
     * Lấy connection hiện tại, tạo mới nếu chưa có hoặc đã đóng.
     */
    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            Config config = Config.getInstance();
            String url = config.getDbUrl();
            String username = config.getDbUsername();
            String password = config.getDbPassword();

            connection = DriverManager.getConnection(url, username, password);
            System.out.println("Kết nối database thành công!");
        }
        return connection;
    }

    /**
     * Đóng connection.
     */
    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("Đã đóng kết nối database.");
            }
        } catch (SQLException e) {
            System.err.println("Lỗi đóng connection: " + e.getMessage());
        }
    }

    // ==================== ĐOẠN CODE CHẠY THỬ (TEST) ====================
// public static void main(String[] args) {
//     System.out.println("--- Bắt đầu kiểm tra kết nối Database ---");
//     try {
//         // 1. Lấy instance của DatabaseConnection
//         DatabaseConnection db = DatabaseConnection.getInstance();
        
//         // 2. Thử gọi hàm getConnection()
//         Connection conn = db.getConnection();
        
//         // 3. Nếu không văng lỗi và conn không null nghĩa là thành công
//         if (conn != null && !conn.isClosed()) {
//             System.out.println("Chúc mừng! Kết nối tới SQL Server thành công 100%!");
            
//             // Đóng kết nối sau khi test xong
//             db.closeConnection(); 
//         }
//     } catch (Exception e) {
//         System.err.println("❌ KẾT NỐI THẤT BẠI! Hãy kiểm tra các lỗi sau:");
//         e.printStackTrace();
//     }
// }

}

