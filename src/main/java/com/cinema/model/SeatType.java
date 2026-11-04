package com.cinema.model;

/**
 * Enum định nghĩa các loại ghế trong phòng chiếu.
 * Mỗi loại ghế có mức phụ phí khác nhau cộng thêm vào giá vé cơ bản.
 */
public enum SeatType {

    /** Ghế thường - không phụ phí (hệ số x1.0) */
    GHE_THUONG("REGULAR", "Ghế thường", 0, 1.0),

    /** Ghế đôi - phụ phí 30.000 VND (hệ số x1.5) */
    GHE_DOI("DOUBLE", "Ghế đôi", 30000, 1.5),

    /** Ghế VIP - phụ phí 50.000 VND (hệ số x1.8) */
    GHE_VIP("VIP", "Ghế VIP", 50000, 1.8);

    /** Giá trị lưu trong database */
    private final String dbValue;

    /** Tên hiển thị tiếng Việt */
    private final String displayName;

    /** Phụ phí cố định (VND) */
    private final double surcharge;

    /** Hệ số nhân giá */
    private final double priceMultiplier;

    /**
     * Constructor cho SeatType.
     *
     * @param dbValue         giá trị tương ứng trong database
     * @param displayName     tên hiển thị tiếng Việt
     * @param surcharge       phụ phí cố định (VND)
     * @param priceMultiplier hệ số nhân giá
     */
    SeatType(String dbValue, String displayName, double surcharge, double priceMultiplier) {
        this.dbValue = dbValue;
        this.displayName = displayName;
        this.surcharge = surcharge;
        this.priceMultiplier = priceMultiplier;
    }

    /**
     * Lấy giá trị lưu trong database.
     *
     * @return giá trị DB (REGULAR, DOUBLE, VIP)
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
     * Lấy phụ phí cố định của loại ghế (VND).
     *
     * @return phụ phí (0 cho ghế thường, 30000 cho ghế đôi, 50000 cho ghế VIP)
     */
    public double getSurcharge() {
        return surcharge;
    }

    /**
     * Lấy hệ số nhân giá của loại ghế.
     *
     * @return hệ số (1.0, 1.5, 1.8)
     */
    public double getPriceMultiplier() {
        return priceMultiplier;
    }

    /**
     * Chuyển đổi từ giá trị database sang enum.
     *
     * @param dbValue giá trị từ database (REGULAR, DOUBLE, VIP)
     * @return SeatType tương ứng
     * @throws IllegalArgumentException nếu giá trị không hợp lệ
     */
    public static SeatType fromDbValue(String dbValue) {
        if (dbValue == null) {
            throw new IllegalArgumentException("Giá trị loại ghế không được null.");
        }
        for (SeatType type : values()) {
            if (type.dbValue.equalsIgnoreCase(dbValue.trim())) {
                return type;
            }
        }
        throw new IllegalArgumentException("Loại ghế không hợp lệ: " + dbValue);
    }

    @Override
    public String toString() {
        return displayName;
    }
}

