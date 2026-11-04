package com.cinema.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Lớp đại diện cho đơn đặt vé (phiếu đặt vé).
 * Tương ứng với bảng bookings trong database.
 * Mỗi đơn đặt thuộc về một khách hàng, gắn với một suất chiếu,
 * và chứa danh sách vé (tickets).
 *
 * <p>Encapsulation: tất cả thuộc tính private, truy xuất qua getter/setter.</p>
 */
public class Booking {

    /** Định dạng ngày giờ hiển thị */
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    /** Mã đơn đặt (auto increment trong DB) */
    private int bookingId;

    /** Mã đơn hàng hiển thị (dạng BK-yyyyMMdd-XXXX) */
    private String bookingCode;

    /** Mã khách hàng (foreign key) */
    private int customerId;

    /** Mã suất chiếu (foreign key) */
    private int showtimeId;

    /** Tổng tiền gốc (VND) */
    private double totalAmount;

    /** Số tiền giảm giá (VND) */
    private double discountAmount;

    /** Thành tiền sau giảm giá (VND) */
    private double finalAmount;

    /** Trạng thái đơn đặt (PENDING, CONFIRMED, CANCELLED) */
    private PaymentStatus status;

    /** Thời gian đặt vé */
    private LocalDateTime bookingTime;

    /** Ngày cập nhật gần nhất */
    private LocalDateTime updatedAt;

    /** Đối tượng khách hàng liên kết (eager loading) */
    private Customer customer;

    /** Đối tượng suất chiếu liên kết (eager loading) */
    private Showtime showtime;

    /** Danh sách vé trong đơn đặt */
    private List<Ticket> tickets;

    /** Đối tượng thanh toán liên kết */
    private Payment payment;

    /**
     * Constructor mặc định.
     */
    public Booking() {
        this.status = PaymentStatus.CHUA_THANH_TOAN;
        this.tickets = new ArrayList<>();
    }

    /**
     * Constructor liên kết Entity (Customer, Showtime, Tickets).
     */
    public Booking(int bookingId, Customer customer, Showtime showtime, List<Ticket> tickets, PaymentStatus status) {
        this.bookingId = bookingId;
        this.customer = customer;
        this.showtime = showtime;
        this.tickets = (tickets != null) ? tickets : new ArrayList<>();
        this.status = (status != null) ? status : PaymentStatus.CHUA_THANH_TOAN;
        if (customer != null) this.customerId = customer.getId();
        if (showtime != null) this.showtimeId = showtime.getShowtimeId();
    }

    /**
     * Constructor với thông tin cơ bản.
     *
     * @param bookingId   mã đơn đặt
     * @param bookingCode mã đơn hàng hiển thị
     * @param customerId  mã khách hàng
     * @param showtimeId  mã suất chiếu
     */
    public Booking(int bookingId, String bookingCode, int customerId, int showtimeId) {
        this.bookingId = bookingId;
        this.bookingCode = bookingCode;
        this.customerId = customerId;
        this.showtimeId = showtimeId;
        this.status = PaymentStatus.CHUA_THANH_TOAN;
        this.tickets = new ArrayList<>();
    }

    /**
     * Constructor đầy đủ tham số.
     *
     * @param bookingId      mã đơn đặt
     * @param bookingCode    mã đơn hàng
     * @param customerId     mã khách hàng
     * @param showtimeId     mã suất chiếu
     * @param totalAmount    tổng tiền gốc
     * @param discountAmount số tiền giảm giá
     * @param finalAmount    thành tiền
     * @param status         trạng thái
     * @param bookingTime    thời gian đặt
     * @param updatedAt      ngày cập nhật
     */
    public Booking(int bookingId, String bookingCode, int customerId, int showtimeId,
                   double totalAmount, double discountAmount, double finalAmount,
                   PaymentStatus status, LocalDateTime bookingTime, LocalDateTime updatedAt) {
        this.bookingId = bookingId;
        this.bookingCode = bookingCode;
        this.customerId = customerId;
        this.showtimeId = showtimeId;
        this.totalAmount = totalAmount;
        this.discountAmount = discountAmount;
        this.finalAmount = finalAmount;
        this.status = status;
        this.bookingTime = bookingTime;
        this.updatedAt = updatedAt;
        this.tickets = new ArrayList<>();
    }

    /**
     * Thêm vé vào đơn đặt.
     *
     * @param ticket vé cần thêm
     */
    public void addTicket(Ticket ticket) {
        if (ticket != null) {
            this.tickets.add(ticket);
        }
    }

    /**
     * Lấy số lượng vé trong đơn đặt.
     *
     * @return số vé
     */
    public int getTicketCount() {
        return tickets.size();
    }

    /**
     * Kiểm tra đơn đặt đã thanh toán chưa.
     *
     * @return true nếu đã thanh toán (CONFIRMED)
     */
    public boolean isPaid() {
        return status == PaymentStatus.DA_THANH_TOAN;
    }

    /**
     * Kiểm tra đơn đặt đã bị hủy chưa.
     *
     * @return true nếu đã hủy
     */
    public boolean isCancelled() {
        return status == PaymentStatus.DA_HUY;
    }

    /**
     * Kiểm tra đơn đặt đang chờ thanh toán.
     *
     * @return true nếu đang chờ
     */
    public boolean isPending() {
        return status == PaymentStatus.CHUA_THANH_TOAN;
    }

    /**
     * Lấy thời gian đặt vé dạng chuỗi hiển thị.
     *
     * @return thời gian đặt formatted
     */
    public String getBookingTimeFormatted() {
        return bookingTime != null ? bookingTime.format(DATETIME_FORMAT) : "";
    }

    /**
     * Trả về thông tin đơn đặt dạng hiển thị.
     *
     * @return chuỗi thông tin đơn đặt
     */
    public String getDisplayInfo() {
        String custName = (customer != null) ? customer.getFullName() : "KH #" + customerId;
        return String.format("Đơn: %s | KH: %s | Số vé: %d | Tổng: %,.0f VND | Giảm: %,.0f VND | Thanh toán: %,.0f VND | TT: %s",
                bookingCode, custName, getTicketCount(), totalAmount, discountAmount,
                finalAmount, status.getDisplayName());
    }

    // ===================== GETTER / SETTER =====================

    public int getBookingId() {
        return bookingId;
    }

    public void setBookingId(int bookingId) {
        this.bookingId = bookingId;
    }

    public int getId() {
        return bookingId;
    }

    public void setId(int id) {
        this.bookingId = id;
    }

    public double getFinalPrice() {
        return finalAmount;
    }

    public void setFinalPrice(double finalPrice) {
        this.finalAmount = finalPrice;
    }

    public double getTotalPrice() {
        return totalAmount;
    }

    public void setTotalPrice(double totalPrice) {
        this.totalAmount = totalPrice;
    }

    public String getBookingCode() {
        return bookingCode;
    }

    public void setBookingCode(String bookingCode) {
        this.bookingCode = bookingCode;
    }

    public int getCustomerId() {
        return customerId;
    }

    public void setCustomerId(int customerId) {
        this.customerId = customerId;
    }

    public int getShowtimeId() {
        return showtimeId;
    }

    public void setShowtimeId(int showtimeId) {
        this.showtimeId = showtimeId;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public double getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(double discountAmount) {
        this.discountAmount = discountAmount;
    }

    public double getFinalAmount() {
        return finalAmount;
    }

    public void setFinalAmount(double finalAmount) {
        this.finalAmount = finalAmount;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public LocalDateTime getBookingTime() {
        return bookingTime;
    }

    public void setBookingTime(LocalDateTime bookingTime) {
        this.bookingTime = bookingTime;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public Showtime getShowtime() {
        return showtime;
    }

    public void setShowtime(Showtime showtime) {
        this.showtime = showtime;
    }

    public List<Ticket> getTickets() {
        return tickets;
    }

    public void setTickets(List<Ticket> tickets) {
        this.tickets = tickets;
    }

    public Payment getPayment() {
        return payment;
    }

    public void setPayment(Payment payment) {
        this.payment = payment;
    }

    @Override
    public String toString() {
        return "Booking{" +
                "bookingId=" + bookingId +
                ", bookingCode='" + bookingCode + '\'' +
                ", customerId=" + customerId +
                ", showtimeId=" + showtimeId +
                ", totalAmount=" + totalAmount +
                ", discountAmount=" + discountAmount +
                ", finalAmount=" + finalAmount +
                ", status=" + status +
                '}';
    }
}

