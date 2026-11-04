package com.cinema.model;

/**
 * Enum định nghĩa trạng thái của ghế trong một suất chiếu cụ thể.
 */
public enum SeatStatus {

    /** Ghế còn trống, có thể đặt */
    CON_TRONG("AVAILABLE", "Còn trống"),

    /** Ghế đã được đặt */
    DA_DAT("BOOKED", "Đã đặt"),

    /** Ghế đang được giữ tạm (đang trong quá trình đặt) */
    DANG_GIU("RESERVED", "Đang giữ");

    /** Giá trị lưu trong database */
    private final String dbValue;

    /** Tên hiển thị tiếng Việt */
    private final String displayName;

    /**
     * Constructor cho SeatStatus.
     *
     * @param dbValue     giá trị tương ứng trong database
     * @param displayName tên hiển thị tiếng Việt
     */
    SeatStatus(String dbValue, String displayName) {
        this.dbValue = dbValue;
        this.displayName = displayName;
    }

    /**
     * Lấy giá trị lưu trong database.
     *
     * @return giá trị DB (AVAILABLE, BOOKED, RESERVED)
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
     * @param dbValue giá trị từ database (AVAILABLE, BOOKED, RESERVED)
     * @return SeatStatus tương ứng
     * @throws IllegalArgumentException nếu giá trị không hợp lệ
     */
    public static SeatStatus fromDbValue(String dbValue) {
        if (dbValue == null) {
            throw new IllegalArgumentException("Giá trị trạng thái ghế không được null.");
        }
        for (SeatStatus status : values()) {
            if (status.dbValue.equalsIgnoreCase(dbValue.trim())) {
                return status;
            }
        }
        throw new IllegalArgumentException("Trạng thái ghế không hợp lệ: " + dbValue);
    }

    @Override
    public String toString() {
        return displayName;
    }
}

