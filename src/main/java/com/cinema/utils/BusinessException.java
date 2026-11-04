package com.cinema.utils;

/**
 * Custom Exception dùng để bắt và thông báo rõ nguyên nhân các lỗi nghiệp vụ
 * trong hệ thống quản lý đặt vé xem phim.
 *
 * Ví dụ các lỗi nghiệp vụ:
 * - Ghế đã được đặt trước
 * - Suất chiếu đã hủy
 * - Khách hàng không tồn tại
 * - Mã sinh viên không hợp lệ
 * - Thanh toán thất bại
 */
public class BusinessException extends Exception {

    private final String errorCode;

    /**
     * Tạo BusinessException chỉ với thông báo lỗi.
     *
     * @param message thông báo mô tả lỗi nghiệp vụ
     */
    public BusinessException(String message) {
        super(message);
        this.errorCode = "BUSINESS_ERROR";
    }

    /**
     * Tạo BusinessException với mã lỗi và thông báo lỗi.
     *
     * @param errorCode mã lỗi (ví dụ: "SEAT_ALREADY_BOOKED", "INVALID_STUDENT_ID")
     * @param message   thông báo mô tả lỗi nghiệp vụ
     */
    public BusinessException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * Tạo BusinessException với thông báo lỗi và nguyên nhân gốc.
     *
     * @param message thông báo mô tả lỗi nghiệp vụ
     * @param cause   exception gốc gây ra lỗi
     */
    public BusinessException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "BUSINESS_ERROR";
    }

    /**
     * Tạo BusinessException với mã lỗi, thông báo và nguyên nhân gốc.
     *
     * @param errorCode mã lỗi
     * @param message   thông báo mô tả lỗi nghiệp vụ
     * @param cause     exception gốc gây ra lỗi
     */
    public BusinessException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    /**
     * Lấy mã lỗi nghiệp vụ.
     *
     * @return mã lỗi (ví dụ: "SEAT_ALREADY_BOOKED")
     */
    public String getErrorCode() {
        return errorCode;
    }

    @Override
    public String toString() {
        return "[" + errorCode + "] " + getMessage();
    }

    // =====================================================================
    // Các hằng số mã lỗi nghiệp vụ phổ biến
    // =====================================================================
    public static final String SEAT_ALREADY_BOOKED = "SEAT_ALREADY_BOOKED";
    public static final String SHOWTIME_NOT_FOUND = "SHOWTIME_NOT_FOUND";
    public static final String SHOWTIME_CANCELLED = "SHOWTIME_CANCELLED";
    public static final String MOVIE_NOT_SHOWING = "MOVIE_NOT_SHOWING";
    public static final String CUSTOMER_NOT_FOUND = "CUSTOMER_NOT_FOUND";
    public static final String INVALID_STUDENT_ID = "INVALID_STUDENT_ID";
    public static final String INVALID_INPUT = "INVALID_INPUT";
    public static final String BOOKING_NOT_FOUND = "BOOKING_NOT_FOUND";
    public static final String BOOKING_ALREADY_CANCELLED = "BOOKING_ALREADY_CANCELLED";
    public static final String PAYMENT_FAILED = "PAYMENT_FAILED";
    public static final String PAYMENT_ALREADY_DONE = "PAYMENT_ALREADY_DONE";
    public static final String INSUFFICIENT_AMOUNT = "INSUFFICIENT_AMOUNT";
    public static final String ROOM_NOT_AVAILABLE = "ROOM_NOT_AVAILABLE";
}

