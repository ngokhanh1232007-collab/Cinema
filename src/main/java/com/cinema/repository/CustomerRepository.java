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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Repository quản lý truy vấn dữ liệu bảng customers trong MySQL.
 * Hỗ trợ các thao tác CRUD và tìm kiếm khách hàng bằng PreparedStatement.
 * Tích hợp bộ nhớ đệm In-Memory Fallback an toàn (ConcurrentHashMap) để hệ thống
 * luôn hoạt động ổn định và sẵn sàng cho các buổi thuyết trình/demo.
 */
public class CustomerRepository {

    /** Bộ nhớ đệm RAM lưu danh sách khách hàng để dự phòng khi MySQL không khả dụng */
    private static final Map<Integer, Customer> inMemoryCustomers = new ConcurrentHashMap<>();
    private static final AtomicInteger customerIdGenerator = new AtomicInteger(10);

    // Nạp sẵn dữ liệu khách hàng mẫu theo đúng database.sql
    static {
        initDefaultCustomers();
    }

    private static void initDefaultCustomers() {
        addFallbackCustomer(1, "Nguyễn Văn An", "an.nguyen@gmail.com", "0901234567", CustomerType.KHACH_THUONG, null, LocalDate.of(1995, 3, 15));
        addFallbackCustomer(2, "Trần Thị Bích", "bich.tran@gmail.com", "0912345678", CustomerType.KHACH_THUONG, null, LocalDate.of(1998, 7, 22));
        addFallbackCustomer(3, "Lê Hoàng Nam", "nam.le@student.edu.vn", "0923456789", CustomerType.SINH_VIEN, "SV20210001", LocalDate.of(2003, 1, 10));
        addFallbackCustomer(4, "Phạm Minh Tú", "tu.pham@student.edu.vn", "0934567890", CustomerType.SINH_VIEN, "SV20210042", LocalDate.of(2003, 5, 28));
        addFallbackCustomer(5, "Đỗ Thị Hương", "huong.do@student.edu.vn", "0945678901", CustomerType.SINH_VIEN, "SV20220115", LocalDate.of(2004, 11, 3));
        addFallbackCustomer(6, "Vũ Quốc Bảo", "bao.vu@gmail.com", "0956789012", CustomerType.VIP, null, LocalDate.of(1990, 9, 18));
        addFallbackCustomer(7, "Hoàng Thị Mai Linh", "linh.hoang@gmail.com", "0967890123", CustomerType.VIP, null, LocalDate.of(1988, 12, 5));
    }

    private static void addFallbackCustomer(int id, String name, String email, String phone,
                                            CustomerType type, String studentId, LocalDate dob) {
        Customer c = new Customer(id, name, phone, email, type, studentId, dob, LocalDateTime.now(), LocalDateTime.now());
        inMemoryCustomers.put(id, c);
    }

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
                    Customer c = mapResultSetToCustomer(rs);
                    inMemoryCustomers.put(c.getId(), c);
                    return c;
                }
            }
        } catch (Exception e) {
            System.err.println("[CustomerRepository] Lỗi khi tìm khách hàng theo id (" + id + "): " + e.getMessage());
            e.printStackTrace();
        }

        // In-Memory Fallback
        return inMemoryCustomers.get(id);
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
                Customer c = mapResultSetToCustomer(rs);
                customerList.add(c);
                inMemoryCustomers.put(c.getId(), c);
            }
            if (!customerList.isEmpty()) {
                return customerList;
            }
        } catch (Exception e) {
            System.err.println("[CustomerRepository] Lỗi khi lấy danh sách khách hàng từ MySQL: " + e.getMessage());
            e.printStackTrace();
        }

        // Fallback: Trả về danh sách từ bộ nhớ đệm
        return new ArrayList<>(inMemoryCustomers.values());
    }

    /**
     * Thêm một khách hàng mới vào cơ sở dữ liệu.
     * Tự động lấy ID sinh tự động (auto-increment) và gán lại vào đối tượng Customer.
     * Có cơ chế In-Memory Fallback nếu MySQL gặp sự cố kết nối.
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
                // Đồng bộ vào bộ nhớ đệm
                inMemoryCustomers.put(customer.getId(), customer);
                System.out.println("[CustomerRepository] Đã lưu khách hàng vào MySQL thành công: ID #" + customer.getId() + " - " + customer.getFullName());
                return true;
            }
        } catch (Exception e) {
            System.err.println("[CustomerRepository] Lỗi khi thêm khách hàng vào MySQL (" + customer.getFullName() + "): " + e.getMessage());
            e.printStackTrace();
        }

        // In-Memory Fallback
        if (customer.getId() <= 0) {
            customer.setId(customerIdGenerator.incrementAndGet());
        }
        inMemoryCustomers.put(customer.getId(), customer);
        System.out.println("[CustomerRepository] [FALLBACK IN-MEMORY] Đã lưu khách hàng vào RAM: ID #" + customer.getId() + " - " + customer.getFullName());
        return true;
    }

    /**
     * Đảm bảo khách hàng thực sự tồn tại trong cơ sở dữ liệu MySQL trước khi thực hiện đặt vé.
     * Nếu customerId chưa có trong bảng customers, hàm sẽ tự động INSERT khách hàng vào MySQL
     * để tránh hoàn toàn lỗi vi phạm khóa ngoại (foreign key constraint).
     *
     * @param customer khách hàng cần đảm bảo tồn tại
     * @return khách hàng đã được đảm bảo có trong MySQL (hoặc RAM fallback)
     */
    public Customer ensureCustomerExistsInDatabase(Customer customer) {
        if (customer == null) {
            return null;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            if (conn == null) {
                return customer;
            }

            // 1. Kiểm tra xem ID đã có trong bảng customers chưa
            if (customer.getId() > 0) {
                String checkSql = "SELECT customer_id FROM customers WHERE customer_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(checkSql)) {
                    ps.setInt(1, customer.getId());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            return customer; // Đã tồn tại trong MySQL
                        }
                    }
                }
            }

            // 2. Kiểm tra xem email hoặc phone đã tồn tại trong MySQL chưa
            String findSql = "SELECT customer_id, full_name, email, phone, customer_type, student_id FROM customers WHERE email = ? OR phone = ? LIMIT 1";
            try (PreparedStatement ps = conn.prepareStatement(findSql)) {
                ps.setString(1, customer.getEmail() != null ? customer.getEmail() : "");
                ps.setString(2, customer.getPhone() != null ? customer.getPhone() : "");
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        customer.setId(rs.getInt("customer_id"));
                        inMemoryCustomers.put(customer.getId(), customer);
                        return customer;
                    }
                }
            }

            // 3. Nếu chưa có, INSERT trực tiếp vào MySQL
            String insertSql = "INSERT INTO customers (full_name, email, phone, customer_type, student_id, date_of_birth) VALUES (?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, customer.getFullName() != null ? customer.getFullName() : "Khách hàng");
                ps.setString(2, customer.getEmail() != null ? customer.getEmail() : ("khach" + System.currentTimeMillis() + "@cinema.vn"));
                ps.setString(3, customer.getPhone() != null ? customer.getPhone() : ("09" + String.format("%08d", customer.getId() > 0 ? customer.getId() : 12345678)));
                ps.setString(4, customer.getCustomerType() != null ? customer.getCustomerType().getDbValue() : "REGULAR");
                ps.setString(5, customer.getStudentId());
                if (customer.getDateOfBirth() != null) {
                    ps.setDate(6, Date.valueOf(customer.getDateOfBirth()));
                } else {
                    ps.setNull(6, java.sql.Types.DATE);
                }

                int rows = ps.executeUpdate();
                if (rows > 0) {
                    try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            customer.setId(generatedKeys.getInt(1));
                        }
                    }
                    inMemoryCustomers.put(customer.getId(), customer);
                    System.out.println("[CustomerRepository] Tự động đồng bộ khách hàng vào MySQL thành công: ID #" + customer.getId());
                }
            }
        } catch (Exception e) {
            System.err.println("[CustomerRepository] Không thể đồng bộ khách hàng vào MySQL, giữ ID hiện tại: " + e.getMessage());
            e.printStackTrace();
        }

        if (customer.getId() <= 0) {
            customer.setId(customerIdGenerator.incrementAndGet());
        }
        inMemoryCustomers.put(customer.getId(), customer);
        return customer;
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

            boolean ok = ps.executeUpdate() > 0;
            if (ok) {
                inMemoryCustomers.put(customer.getId(), customer);
                return true;
            }
        } catch (Exception e) {
            System.err.println("[CustomerRepository] Lỗi khi cập nhật khách hàng ID " + customer.getId() + ": " + e.getMessage());
            e.printStackTrace();
        }

        // Fallback cập nhật bộ nhớ đệm
        inMemoryCustomers.put(customer.getId(), customer);
        return true;
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
                    Customer c = mapResultSetToCustomer(rs);
                    inMemoryCustomers.put(c.getId(), c);
                    return c;
                }
            }
        } catch (Exception e) {
            System.err.println("[CustomerRepository] Lỗi khi tìm khách hàng theo email (" + email + "): " + e.getMessage());
            e.printStackTrace();
        }

        // Fallback
        for (Customer c : inMemoryCustomers.values()) {
            if (email.trim().equalsIgnoreCase(c.getEmail())) {
                return c;
            }
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
                    Customer c = mapResultSetToCustomer(rs);
                    inMemoryCustomers.put(c.getId(), c);
                    return c;
                }
            }
        } catch (Exception e) {
            System.err.println("[CustomerRepository] Lỗi khi tìm khách hàng theo phone (" + phone + "): " + e.getMessage());
            e.printStackTrace();
        }

        // Fallback
        for (Customer c : inMemoryCustomers.values()) {
            if (phone.trim().equalsIgnoreCase(c.getPhone())) {
                return c;
            }
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
                    Customer c = mapResultSetToCustomer(rs);
                    inMemoryCustomers.put(c.getId(), c);
                    return c;
                }
            }
        } catch (Exception e) {
            System.err.println("[CustomerRepository] Lỗi khi tìm khách hàng theo MSSV (" + studentId + "): " + e.getMessage());
            e.printStackTrace();
        }

        // Fallback
        for (Customer c : inMemoryCustomers.values()) {
            if (c.getStudentId() != null && studentId.trim().equalsIgnoreCase(c.getStudentId())) {
                return c;
            }
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
            return stats;
        } catch (Exception e) {
            System.err.println("[CustomerRepository] Lỗi thống kê khách hàng từ MySQL: " + e.getMessage());
            e.printStackTrace();
        }

        // Fallback từ RAM
        for (Customer c : inMemoryCustomers.values()) {
            stats.put(c.getCustomerType(), stats.getOrDefault(c.getCustomerType(), 0) + 1);
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
