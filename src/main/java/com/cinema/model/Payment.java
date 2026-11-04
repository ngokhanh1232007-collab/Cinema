package com.cinema.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Lớp đại diện cho giao dịch thanh toán.
 * Tương ứng với bảng payments trong database.
 * Mỗi giao dịch thanh toán gắn với một đơn đặt vé (Booking).
 *
 * <p>Encapsulation: tất cả thuộc tính private, truy xuất qua getter/setter.</p>
 */
public class Payment {

    /** Định dạng ngày giờ hiển thị */
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    /** Mã thanh toán (auto increment trong DB) */
    private int paymentId;

    /** Mã thanh toán hiển thị (dạng PM-yyyyMMdd-XXXX) */
    private String paymentCode;

    /** Mã đơn đặt vé (foreign key) */
    private int bookingId;

    /** Số tiền thanh toán (VND) */
    private double amount;

    /** Phương thức thanh toán (CASH, CREDIT_CARD, E_WALLET, BANK_TRANSFER) */
    private String paymentMethod;

    /** Thời gian thanh toán */
    private LocalDateTime paymentTime;

    /** Trạng thái thanh toán (SUCCESS, FAILED, REFUNDED) */
    private String status;

    /** Đối tượng đơn đặt liên kết (eager loading) */
    private Booking booking;

    /**
     * Constructor mặc định.
     */
    public Payment() {
        this.status = "SUCCESS";
    }

    /**
     * Constructor với thông tin cơ bản.
     *
     * @param paymentId     mã thanh toán
     * @param paymentCode   mã thanh toán hiển thị
     * @param bookingId     mã đơn đặt
     * @param amount        số tiền
     * @param paymentMethod phương thức thanh toán
     */
    public Payment(int paymentId, String paymentCode, int bookingId,
                   double amount, String paymentMethod) {
        this.paymentId = paymentId;
        this.paymentCode = paymentCode;
        this.bookingId = bookingId;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.status = "SUCCESS";
    }

    /**
     * Constructor đầy đủ tham số.
     *
     * @param paymentId     mã thanh toán
     * @param paymentCode   mã thanh toán hiển thị
     * @param bookingId     mã đơn đặt
     * @param amount        số tiền
     * @param paymentMethod phương thức thanh toán
     * @param paymentTime   thời gian thanh toán
     * @param status        trạng thái
     */
    public Payment(int paymentId, String paymentCode, int bookingId,
                   double amount, String paymentMethod,
                   LocalDateTime paymentTime, String status) {
        this.paymentId = paymentId;
        this.paymentCode = paymentCode;
        this.bookingId = bookingId;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.paymentTime = paymentTime;
        this.status = status;
    }

    /**
     * Kiểm tra thanh toán thành công.
     *
     * @return true nếu thanh toán thành công
     */
    public boolean isSuccess() {
        return "SUCCESS".equalsIgnoreCase(status);
    }

    /**
     * Kiểm tra thanh toán đã hoàn tiền.
     *
     * @return true nếu đã hoàn tiền
     */
    public boolean isRefunded() {
        return "REFUNDED".equalsIgnoreCase(status);
    }

    /**
     * Lấy thời gian thanh toán dạng chuỗi hiển thị.
     *
     * @return thời gian thanh toán formatted
     */
    public String getPaymentTimeFormatted() {
        return paymentTime != null ? paymentTime.format(DATETIME_FORMAT) : "";
    }

    /**
     * Lấy tên phương thức thanh toán bằng tiếng Việt.
     *
     * @return tên phương thức thanh toán
     */
    public String getPaymentMethodDisplayName() {
        if (paymentMethod == null) return "Không xác định";
        switch (paymentMethod.toUpperCase()) {
            case "CASH":           return "Tiền mặt";
            case "CREDIT_CARD":    return "Thẻ tín dụng";
            case "E_WALLET":       return "Ví điện tử";
            case "BANK_TRANSFER":  return "Chuyển khoản ngân hàng";
            default:               return paymentMethod;
        }
    }

    /**
     * Trả về thông tin thanh toán dạng hiển thị.
     *
     * @return chuỗi thông tin thanh toán
     */
    public String getDisplayInfo() {
        return String.format("TT: %s | Đơn: BK#%d | Số tiền: %,.0f VND | PT: %s | TG: %s | Trạng thái: %s",
                paymentCode, bookingId, amount, getPaymentMethodDisplayName(),
                getPaymentTimeFormatted(), status);
    }

    // ===================== GETTER / SETTER =====================

    public int getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(int paymentId) {
        this.paymentId = paymentId;
    }

    public String getPaymentCode() {
        return paymentCode;
    }

    public void setPaymentCode(String paymentCode) {
        this.paymentCode = paymentCode;
    }

    public int getBookingId() {
        return bookingId;
    }

    public void setBookingId(int bookingId) {
        this.bookingId = bookingId;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public LocalDateTime getPaymentTime() {
        return paymentTime;
    }

    public void setPaymentTime(LocalDateTime paymentTime) {
        this.paymentTime = paymentTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Booking getBooking() {
        return booking;
    }

    public void setBooking(Booking booking) {
        this.booking = booking;
    }

    @Override
    public String toString() {
        return "Payment{" +
                "paymentId=" + paymentId +
                ", paymentCode='" + paymentCode + '\'' +
                ", bookingId=" + bookingId +
                ", amount=" + amount +
                ", paymentMethod='" + paymentMethod + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}

