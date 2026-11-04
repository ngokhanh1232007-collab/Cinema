package com.cinema.repository;

import com.cinema.model.Customer;
import com.cinema.model.CustomerType;
import com.cinema.utils.DatabaseConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Repository quản lý truy vấn dữ liệu bảng customers trong MySQL.
 * Hỗ trợ các thao tác CRUD và tìm kiếm khách hàng bằng PreparedStatement.
 */
public class CustomerRepository {

    /**
     * Tìm khách hàng theo mã định danh (customer_id).
     *
     * @param id mã khách hàng cần tìm
     * @return đối tượng Customer nếu tìm thấy, hoặc null nếu không tồn tại
     */
    public Customer findById(int id) {
        String sql = "SELECT customer_id, full_name, email, phone, customer_type, "
                   + "student_id, date_of_birth, created_at, updated_at "
                   + "FROM customers WHERE customer_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToCustomer(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[CustomerRepository] Lỗi khi tìm khách hàng theo id (" + id + "): " + e.getMessage());
        }
        return null;
    }

    /**
     * Lấy toàn bộ danh sách khách hàng trong hệ thống.
     * Sử dụng Collection List để lưu trữ kết quả trả về.
     *
     * @return danh sách tất cả Customer
     */
    public List<Customer> findAll() {
        List<Customer> customerList = new ArrayList<>();
        String sql = "SELECT customer_id, full_name, email, phone, customer_type, "
                   + "student_id, date_of_birth, created_at, updated_at "
                   + "FROM customers ORDER BY customer_id ASC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                customerList.add(mapResultSetToCustomer(rs));
            }
        } catch (SQLException e) {
            System.err.println("[CustomerRepository] Lỗi khi lấy danh sách khách hàng: " + e.getMessage());
        }
        return customerList;
    }

    /**
     * Thêm một khách hàng mới vào cơ sở dữ liệu.
     * Tự động lấy ID sinh tự động (auto-increment) và gán lại vào đối tượng Customer.
     *
     * @param customer đối tượng khách hàng cần lưu
     * @return true nếu thêm thành công, false nếu thất bại
     */
    public boolean save(Customer customer) {
        if (customer == null) {
            System.err.println("[CustomerRepository] Không thể lưu customer null.");
            return false;
        }

        String sql = "INSERT INTO customers (full_name, email, phone, customer_type, student_id, date_of_birth) "
                   + "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, customer.getFullName());
            ps.setString(2, customer.getEmail());
            ps.setString(3, customer.getPhone());
            ps.setString(4, customer.getCustomerType() != null ? customer.getCustomerType().getDbValue() : CustomerType.KHACH_THUONG.getDbValue());

            if (customer.getStudentId() != null && !customer.getStudentId().trim().isEmpty()) {
                ps.setString(5, customer.getStudentId().trim());
            } else {
                ps.setNull(5, java.sql.Types.VARCHAR);
            }

            if (customer.getDateOfBirth() != null) {
                ps.setDate(6, Date.valueOf(customer.getDateOfBirth()));
            } else {
                ps.setNull(6, java.sql.Types.DATE);
            }

            int affectedRows = ps.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        customer.setId(generatedKeys.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("[CustomerRepository] Lỗi khi thêm khách hàng (" + customer.getFullName() + "): " + e.getMessage());
        }
        return false;
    }

    /**
     * Cập nhật thông tin khách hàng đã có trong cơ sở dữ liệu.
     *
     * @param customer đối tượng khách hàng cần cập nhật
     * @return true nếu cập nhật thành công, false nếu thất bại
     */
    public boolean update(Customer customer) {
        if (customer == null || customer.getId() <= 0) {
            return false;
        }

        String sql = "UPDATE customers SET full_name = ?, email = ?, phone = ?, "
                   + "customer_type = ?, student_id = ?, date_of_birth = ? "
                   + "WHERE customer_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, customer.getFullName());
            ps.setString(2, customer.getEmail());
            ps.setString(3, customer.getPhone());
            ps.setString(4, customer.getCustomerType() != null ? customer.getCustomerType().getDbValue() : CustomerType.KHACH_THUONG.getDbValue());

            if (customer.getStudentId() != null && !customer.getStudentId().trim().isEmpty()) {
                ps.setString(5, customer.getStudentId().trim());
            } else {
                ps.setNull(5, java.sql.Types.VARCHAR);
            }

            if (customer.getDateOfBirth() != null) {
                ps.setDate(6, Date.valueOf(customer.getDateOfBirth()));
            } else {
                ps.setNull(6, java.sql.Types.DATE);
            }

            ps.setInt(7, customer.getId());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[CustomerRepository] Lỗi khi cập nhật khách hàng ID " + customer.getId() + ": " + e.getMessage());
        }
        return false;
    }

    /**
     * Tìm khách hàng theo địa chỉ email.
     *
     * @param email địa chỉ email
     * @return Customer nếu tìm thấy, null nếu không có
     */
    public Customer findByEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return null;
        }

        String sql = "SELECT customer_id, full_name, email, phone, customer_type, "
                   + "student_id, date_of_birth, created_at, updated_at "
                   + "FROM customers WHERE email = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, email.trim());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToCustomer(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[CustomerRepository] Lỗi khi tìm khách hàng theo email (" + email + "): " + e.getMessage());
        }
        return null;
    }

    /**
     * Tìm khách hàng theo số điện thoại.
     *
     * @param phone số điện thoại
     * @return Customer nếu tìm thấy, null nếu không có
     */
    public Customer findByPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return null;
        }

        String sql = "SELECT customer_id, full_name, email, phone, customer_type, "
                   + "student_id, date_of_birth, created_at, updated_at "
                   + "FROM customers WHERE phone = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, phone.trim());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToCustomer(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[CustomerRepository] Lỗi khi tìm khách hàng theo phone (" + phone + "): " + e.getMessage());
        }
        return null;
    }

    /**
     * Tìm khách hàng theo mã số sinh viên.
     *
     * @param studentId mã số sinh viên
     * @return Customer nếu tìm thấy, null nếu không có
     */
    public Customer findByStudentId(String studentId) {
        if (studentId == null || studentId.trim().isEmpty()) {
            return null;
        }

        String sql = "SELECT customer_id, full_name, email, phone, customer_type, "
                   + "student_id, date_of_birth, created_at, updated_at "
                   + "FROM customers WHERE student_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, studentId.trim());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToCustomer(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[CustomerRepository] Lỗi khi tìm khách hàng theo MSSV (" + studentId + "): " + e.getMessage());
        }
        return null;
    }

    /**
     * Lấy thống kê số lượng khách hàng theo từng loại (Collection Map).
     *
     * @return Map với key là loại khách hàng, value là số lượng tương ứng
     */
    public Map<CustomerType, Integer> countCustomersByType() {
        Map<CustomerType, Integer> stats = new HashMap<>();
        for (CustomerType type : CustomerType.values()) {
            stats.put(type, 0);
        }

        String sql = "SELECT customer_type, COUNT(*) AS total FROM customers GROUP BY customer_type";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String typeStr = rs.getString("customer_type");
                int count = rs.getInt("total");
                try {
                    CustomerType type = CustomerType.fromDbValue(typeStr);
                    stats.put(type, count);
                } catch (IllegalArgumentException ignored) {
                }
            }
        } catch (SQLException e) {
            System.err.println("[CustomerRepository] Lỗi thống kê khách hàng: " + e.getMessage());
        }
        return stats;
    }

    /**
     * Ánh xạ từ dòng dữ liệu ResultSet sang đối tượng Customer.
     *
     * @param rs ResultSet đang trỏ tới dòng hiện tại
     * @return đối tượng Customer đã nạp đầy đủ thông tin
     * @throws SQLException nếu có lỗi truy xuất dữ liệu
     */
    private Customer mapResultSetToCustomer(ResultSet rs) throws SQLException {
        int id = rs.getInt("customer_id");
        String fullName = rs.getString("full_name");
        String email = rs.getString("email");
        String phone = rs.getString("phone");
        String typeStr = rs.getString("customer_type");
        String studentId = rs.getString("student_id");

        Date dobSql = rs.getDate("date_of_birth");
        LocalDate dateOfBirth = (dobSql != null) ? dobSql.toLocalDate() : null;

        Timestamp createdTs = rs.getTimestamp("created_at");
        LocalDateTime createdAt = (createdTs != null) ? createdTs.toLocalDateTime() : null;

        Timestamp updatedTs = rs.getTimestamp("updated_at");
        LocalDateTime updatedAt = (updatedTs != null) ? updatedTs.toLocalDateTime() : null;

        CustomerType customerType;
        try {
            customerType = CustomerType.fromDbValue(typeStr);
        } catch (IllegalArgumentException e) {
            customerType = CustomerType.KHACH_THUONG;
        }

        return new Customer(id, fullName, phone, email, customerType, studentId, dateOfBirth, createdAt, updatedAt);
    }
}

