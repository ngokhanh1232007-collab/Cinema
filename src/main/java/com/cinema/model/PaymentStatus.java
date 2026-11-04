package com.cinema.model;

/**
 * Enum định nghĩa trạng thái thanh toán của đơn đặt vé.
 */
public enum PaymentStatus {

    /** Chưa thanh toán - đơn hàng đang chờ xử lý */
    CHUA_THANH_TOAN("PENDING", "Chưa thanh toán"),

    /** Đã thanh toán thành công */
    DA_THANH_TOAN("CONFIRMED", "Đã thanh toán"),

    /** Đã hủy */
    DA_HUY("CANCELLED", "Đã hủy");

    /** Giá trị lưu trong database */
    private final String dbValue;

    /** Tên hiển thị tiếng Việt */
    private final String displayName;

    /**
     * Constructor cho PaymentStatus.
     *
     * @param dbValue     giá trị tương ứng trong database
     * @param displayName tên hiển thị tiếng Việt
     */
    PaymentStatus(String dbValue, String displayName) {
        this.dbValue = dbValue;
        this.displayName = displayName;
    }

    /**
     * Lấy giá trị lưu trong database.
     *
     * @return giá trị DB (PENDING, CONFIRMED, CANCELLED)
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
     * @param dbValue giá trị từ database (PENDING, CONFIRMED, CANCELLED)
     * @return PaymentStatus tương ứng
     * @throws IllegalArgumentException nếu giá trị không hợp lệ
     */
    public static PaymentStatus fromDbValue(String dbValue) {
        if (dbValue == null) {
            throw new IllegalArgumentException("Giá trị trạng thái thanh toán không được null.");
        }
        for (PaymentStatus status : values()) {
            if (status.dbValue.equalsIgnoreCase(dbValue.trim())) {
                return status;
            }
        }
        throw new IllegalArgumentException("Trạng thái thanh toán không hợp lệ: " + dbValue);
    }

    @Override
    public String toString() {
        return displayName;
    }
}

