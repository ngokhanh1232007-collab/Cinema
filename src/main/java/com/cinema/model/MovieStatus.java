package com.cinema.model;

/**
 * Enum định nghĩa trạng thái của phim.
 */
public enum MovieStatus {

    /** Phim đang được chiếu tại rạp */
    DANG_CHIEU("SHOWING", "Đang chiếu"),

    /** Phim đã ngừng chiếu */
    NGUNG_CHIEU("STOPPED", "Ngừng chiếu");

    /** Giá trị lưu trong database */
    private final String dbValue;

    /** Tên hiển thị tiếng Việt */
    private final String displayName;

    /**
     * Constructor cho MovieStatus.
     *
     * @param dbValue     giá trị tương ứng trong database
     * @param displayName tên hiển thị tiếng Việt
     */
    MovieStatus(String dbValue, String displayName) {
        this.dbValue = dbValue;
        this.displayName = displayName;
    }

    /**
     * Lấy giá trị lưu trong database.
     *
     * @return giá trị DB (SHOWING, STOPPED)
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
     * @param dbValue giá trị từ database (SHOWING, STOPPED)
     * @return MovieStatus tương ứng
     * @throws IllegalArgumentException nếu giá trị không hợp lệ
     */
    public static MovieStatus fromDbValue(String dbValue) {
        if (dbValue == null) {
            throw new IllegalArgumentException("Giá trị trạng thái phim không được null.");
        }
        for (MovieStatus status : values()) {
            if (status.dbValue.equalsIgnoreCase(dbValue.trim())) {
                return status;
            }
        }
        throw new IllegalArgumentException("Trạng thái phim không hợp lệ: " + dbValue);
    }

    @Override
    public String toString() {
        return displayName;
    }
}

