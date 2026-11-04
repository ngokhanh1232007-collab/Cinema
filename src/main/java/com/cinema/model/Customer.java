package com.cinema.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Lớp đại diện cho khách hàng trong hệ thống đặt vé xem phim.
 * Kế thừa từ lớp trừu tượng User và bổ sung thông tin riêng của khách hàng.
 *
 * <p>Áp dụng tính chất OOP:
 * <ul>
 *   <li><b>Inheritance:</b> Kế thừa từ abstract class User</li>
 *   <li><b>Polymorphism:</b> Override phương thức getRole() và getDisplayInfo()</li>
 *   <li><b>Encapsulation:</b> Tất cả thuộc tính private</li>
 * </ul>
 */
public class Customer extends User {

    /** Loại khách hàng (KHACH_THUONG, SINH_VIEN, VIP) */
    private CustomerType customerType;

    /** Mã số sinh viên (chỉ dành cho loại SINH_VIEN, có thể null) */
    private String studentId;

    /** Ngày sinh */
    private LocalDate dateOfBirth;

    /** Ngày tạo tài khoản */
    private LocalDateTime createdAt;

    /** Ngày cập nhật gần nhất */
    private LocalDateTime updatedAt;

    /**
     * Constructor mặc định.
     */
    public Customer() {
        super();
        this.customerType = CustomerType.KHACH_THUONG;
    }

    /**
     * Constructor với các thông tin cơ bản.
     *
     * @param id           mã khách hàng
     * @param fullName     họ tên
     * @param phone        số điện thoại
     * @param email        email
     * @param customerType loại khách hàng
     */
    public Customer(int id, String fullName, String phone, String email, CustomerType customerType) {
        super(id, fullName, phone, email);
        this.customerType = customerType;
    }

    /**
     * Constructor đầy đủ tham số.
     *
     * @param id           mã khách hàng
     * @param fullName     họ tên
     * @param phone        số điện thoại
     * @param email        email
     * @param customerType loại khách hàng
     * @param studentId    mã sinh viên (null nếu không phải sinh viên)
     * @param dateOfBirth  ngày sinh
     * @param createdAt    ngày tạo
     * @param updatedAt    ngày cập nhật
     */
    public Customer(int id, String fullName, String phone, String email,
                    CustomerType customerType, String studentId,
                    LocalDate dateOfBirth, LocalDateTime createdAt, LocalDateTime updatedAt) {
        super(id, fullName, phone, email);
        this.customerType = customerType;
        this.studentId = studentId;
        this.dateOfBirth = dateOfBirth;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * Override phương thức trừu tượng từ User.
     * Trả về vai trò dựa trên loại khách hàng.
     *
     * @return vai trò của khách hàng (ví dụ: "Khách thường", "Sinh viên", "Khách VIP")
     */
    @Override
    public String getRole() {
        return customerType.getDisplayName();
    }

    /**
     * Override phương thức trừu tượng từ User.
     * Trả về thông tin hiển thị chi tiết của khách hàng.
     *
     * @return chuỗi thông tin chi tiết
     */
    @Override
    public String getDisplayInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("Khách hàng: ").append(getFullName());
        sb.append(" | Loại: ").append(customerType.getDisplayName());
        sb.append(" | SĐT: ").append(getPhone());
        sb.append(" | Email: ").append(getEmail());
        if (customerType == CustomerType.SINH_VIEN && studentId != null) {
            sb.append(" | MSSV: ").append(studentId);
        }
        return sb.toString();
    }

    /**
     * Kiểm tra khách hàng có phải sinh viên không.
     *
     * @return true nếu là sinh viên
     */
    public boolean isStudent() {
        return customerType == CustomerType.SINH_VIEN;
    }

    /**
     * Kiểm tra khách hàng có phải VIP không.
     *
     * @return true nếu là VIP
     */
    public boolean isVip() {
        return customerType == CustomerType.VIP;
    }

    // ===================== GETTER / SETTER =====================

    /**
     * Lấy loại khách hàng.
     *
     * @return loại khách hàng
     */
    public CustomerType getCustomerType() {
        return customerType;
    }

    /**
     * Đặt loại khách hàng.
     *
     * @param customerType loại khách hàng
     */
    public void setCustomerType(CustomerType customerType) {
        this.customerType = customerType;
    }

    /**
     * Lấy mã số sinh viên.
     *
     * @return mã sinh viên (null nếu không phải sinh viên)
     */
    public String getStudentId() {
        return studentId;
    }

    /**
     * Đặt mã số sinh viên.
     *
     * @param studentId mã sinh viên
     */
    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    /**
     * Lấy ngày sinh.
     *
     * @return ngày sinh
     */
    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    /**
     * Đặt ngày sinh.
     *
     * @param dateOfBirth ngày sinh
     */
    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    /**
     * Lấy ngày tạo tài khoản.
     *
     * @return ngày tạo
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * Đặt ngày tạo tài khoản.
     *
     * @param createdAt ngày tạo
     */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Lấy ngày cập nhật gần nhất.
     *
     * @return ngày cập nhật
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Đặt ngày cập nhật gần nhất.
     *
     * @param updatedAt ngày cập nhật
     */
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "Customer{" +
                "id=" + getId() +
                ", fullName='" + getFullName() + '\'' +
                ", phone='" + getPhone() + '\'' +
                ", email='" + getEmail() + '\'' +
                ", customerType=" + customerType +
                ", studentId='" + studentId + '\'' +
                ", dateOfBirth=" + dateOfBirth +
                '}';
    }
}

