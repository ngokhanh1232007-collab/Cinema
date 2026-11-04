package com.cinema.model;

/**
 * Lớp trừu tượng (Abstract) đại diện cho người dùng trong hệ thống.
 * Chứa các thông tin cơ bản chung cho tất cả loại người dùng.
 *
 * <p>Áp dụng tính chất OOP:
 * <ul>
 *   <li><b>Abstraction:</b> Khai báo abstract, không thể tạo instance trực tiếp</li>
 *   <li><b>Encapsulation:</b> Tất cả thuộc tính private, truy xuất qua getter/setter</li>
 *   <li><b>Inheritance:</b> Các lớp con (Customer) sẽ kế thừa</li>
 *   <li><b>Polymorphism:</b> Phương thức getRole() được override bởi lớp con</li>
 * </ul>
 */
public abstract class User {

    /** Mã định danh người dùng (tương ứng customer_id trong DB) */
    private int id;

    /** Họ và tên đầy đủ */
    private String fullName;

    /** Số điện thoại */
    private String phone;

    /** Địa chỉ email */
    private String email;

    /**
     * Constructor mặc định.
     */
    protected User() {
    }

    /**
     * Constructor đầy đủ tham số.
     *
     * @param id       mã định danh
     * @param fullName họ và tên
     * @param phone    số điện thoại
     * @param email    địa chỉ email
     */
    protected User(int id, String fullName, String phone, String email) {
        this.id = id;
        this.fullName = fullName;
        this.phone = phone;
        this.email = email;
    }

    /**
     * Phương thức trừu tượng trả về vai trò của người dùng.
     * Bắt buộc lớp con phải override (đa hình).
     *
     * @return chuỗi mô tả vai trò
     */
    public abstract String getRole();

    /**
     * Phương thức trừu tượng hiển thị thông tin người dùng.
     * Bắt buộc lớp con phải override (đa hình).
     *
     * @return chuỗi thông tin chi tiết
     */
    public abstract String getDisplayInfo();

    // ===================== GETTER / SETTER =====================

    /**
     * Lấy mã định danh người dùng.
     *
     * @return id
     */
    public int getId() {
        return id;
    }

    /**
     * Đặt mã định danh người dùng.
     *
     * @param id mã định danh
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * Lấy họ và tên đầy đủ.
     *
     * @return họ tên
     */
    public String getFullName() {
        return fullName;
    }

    /**
     * Đặt họ và tên đầy đủ.
     *
     * @param fullName họ tên
     */
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    /**
     * Lấy số điện thoại.
     *
     * @return số điện thoại
     */
    public String getPhone() {
        return phone;
    }

    /**
     * Đặt số điện thoại.
     *
     * @param phone số điện thoại
     */
    public void setPhone(String phone) {
        this.phone = phone;
    }

    /**
     * Lấy địa chỉ email.
     *
     * @return email
     */
    public String getEmail() {
        return email;
    }

    /**
     * Đặt địa chỉ email.
     *
     * @param email địa chỉ email
     */
    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", fullName='" + fullName + '\'' +
                ", phone='" + phone + '\'' +
                ", email='" + email + '\'' +
                '}';
    }
}

