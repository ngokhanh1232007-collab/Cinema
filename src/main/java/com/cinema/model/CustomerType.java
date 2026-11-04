package com.cinema.model;

/**
 * Enum định nghĩa các loại khách hàng trong hệ thống.
 * Mỗi loại khách hàng có chính sách giá vé khác nhau.
 */
public enum CustomerType {

    /** Khách hàng thường - không được giảm giá */
    KHACH_THUONG("REGULAR", "Khách thường"),

    /** Sinh viên - được giảm 20% giá vé */
    SINH_VIEN("STUDENT", "Sinh viên"),

    /** Khách VIP - được giảm 15% giá vé */
    VIP("VIP", "Khách VIP");

    /** Giá trị lưu trong database */
    private final String dbValue;

    /** Tên hiển thị tiếng Việt */
    private final String displayName;

    /**
     * Constructor cho CustomerType.
     *
     * @param dbValue     giá trị tương ứng trong database
     * @param displayName tên hiển thị tiếng Việt
     */
    CustomerType(String dbValue, String displayName) {
        this.dbValue = dbValue;
        this.displayName = displayName;
    }

    /**
     * Lấy giá trị lưu trong database.
     *
     * @return giá trị DB (REGULAR, STUDENT, VIP)
     */
    public String getDbValue() {
        return dbValue;
    }

    /**
     * Lấy tên hiển thị tiếng Việt.
     *
     * @return tên hiển thị
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Chuyển đổi từ giá trị database sang enum.
     *
     * @param dbValue giá trị từ database (REGULAR, STUDENT, VIP)
     * @return CustomerType tương ứng
     * @throws IllegalArgumentException nếu giá trị không hợp lệ
     */
    public static CustomerType fromDbValue(String dbValue) {
        if (dbValue == null) {
            throw new IllegalArgumentException("Giá trị loại khách hàng không được null.");
        }
        for (CustomerType type : values()) {
            if (type.dbValue.equalsIgnoreCase(dbValue.trim())) {
                return type;
            }
        }
        throw new IllegalArgumentException("Loại khách hàng không hợp lệ: " + dbValue);
    }

    /**
     * Chuyển đổi linh hoạt từ chuỗi nhập vào (REGULAR, STUDENT, VIP hoặc tiếng Việt).
     */
    public static CustomerType fromString(String val) {
        if (val == null || val.isBlank()) return KHACH_THUONG;
        String upper = val.trim().toUpperCase();
        for (CustomerType type : values()) {
            if (type.name().equalsIgnoreCase(upper) || type.getDbValue().equalsIgnoreCase(upper)) {
                return type;
            }
        }
        if (upper.contains("SINH") || upper.contains("STUDENT")) return SINH_VIEN;
        if (upper.contains("VIP")) return VIP;
        return KHACH_THUONG;
    }

    @Override
    public String toString() {
        return displayName;
    }
}

