package com.cinema.model;

import java.time.LocalDateTime;

/**
 * Lớp đại diện cho vé xem phim.
 * Tương ứng với bảng tickets trong database.
 * Mỗi vé liên kết với một đơn đặt (Booking), một ghế theo suất chiếu (showtime_seat),
 * và ghi nhận giá vé cuối cùng.
 *
 * <p>Encapsulation: tất cả thuộc tính private, truy xuất qua getter/setter.</p>
 */
public class Ticket {

    /** Mã vé (auto increment trong DB) */
    private int ticketId;

    /** Mã vé hiển thị (dạng TK-yyyyMMdd-XXXX) */
    private String ticketCode;

    /** Mã đơn đặt vé (foreign key) */
    private int bookingId;

    /** Mã showtime_seat (foreign key) */
    private int showtimeSeatId;

    /** Giá vé cuối cùng đã tính theo loại ghế (VND) */
    private double seatPrice;

    /** Ngày tạo vé */
    private LocalDateTime createdAt;

    /** Đối tượng khách hàng liên kết (eager loading - lấy từ booking) */
    private Customer customer;

    /** Đối tượng phim liên kết (eager loading) */
    private Movie movie;

    /** Đối tượng suất chiếu liên kết (eager loading) */
    private Showtime showtime;

    /** Đối tượng ghế liên kết (eager loading) */
    private Seat seat;

    /** Trạng thái thanh toán */
    private PaymentStatus paymentStatus;

    /**
     * Constructor mặc định.
     */
    public Ticket() {
        this.paymentStatus = PaymentStatus.CHUA_THANH_TOAN;
    }

    /**
     * Constructor với thông tin cơ bản.
     *
     * @param ticketId       mã vé
     * @param ticketCode     mã vé hiển thị
     * @param bookingId      mã đơn đặt
     * @param showtimeSeatId mã showtime_seat
     * @param seatPrice      giá vé
     */
    public Ticket(int ticketId, String ticketCode, int bookingId,
                  int showtimeSeatId, double seatPrice) {
        this.ticketId = ticketId;
        this.ticketCode = ticketCode;
        this.bookingId = bookingId;
        this.showtimeSeatId = showtimeSeatId;
        this.seatPrice = seatPrice;
        this.paymentStatus = PaymentStatus.CHUA_THANH_TOAN;
    }

    /**
     * Constructor đầy đủ tham số.
     *
     * @param ticketId       mã vé
     * @param ticketCode     mã vé hiển thị
     * @param bookingId      mã đơn đặt
     * @param showtimeSeatId mã showtime_seat
     * @param seatPrice      giá vé
     * @param createdAt      ngày tạo
     */
    public Ticket(int ticketId, String ticketCode, int bookingId,
                  int showtimeSeatId, double seatPrice, LocalDateTime createdAt) {
        this.ticketId = ticketId;
        this.ticketCode = ticketCode;
        this.bookingId = bookingId;
        this.showtimeSeatId = showtimeSeatId;
        this.seatPrice = seatPrice;
        this.createdAt = createdAt;
        this.paymentStatus = PaymentStatus.CHUA_THANH_TOAN;
    }

    /**
     * Constructor liên kết đầy đủ đối tượng Entity (phục vụ repository / service).
     */
    public Ticket(int ticketId, Customer customer, Movie movie, Showtime showtime,
                  Seat seat, double seatPrice, PaymentStatus paymentStatus) {
        this.ticketId = ticketId;
        this.customer = customer;
        this.movie = movie;
        this.showtime = showtime;
        this.seat = seat;
        this.seatPrice = seatPrice;
        this.paymentStatus = (paymentStatus != null) ? paymentStatus : PaymentStatus.CHUA_THANH_TOAN;
        if (showtime != null && seat != null) {
            this.showtimeSeatId = seat.getShowtimeSeatId();
        }
    }

    /**
     * Constructor liên kết đầy đủ kèm mã vé hiển thị.
     */
    public Ticket(int ticketId, String ticketCode, Customer customer, Movie movie,
                  Showtime showtime, Seat seat, double seatPrice, PaymentStatus paymentStatus) {
        this(ticketId, customer, movie, showtime, seat, seatPrice, paymentStatus);
        this.ticketCode = ticketCode;
    }

    /**
     * Trả về thông tin vé dạng hiển thị.
     *
     * @return chuỗi thông tin vé
     */
    public String getDisplayInfo() {
        String movieTitle = (movie != null) ? movie.getTitle() : "N/A";
        String seatLabel = (seat != null) ? seat.getDisplayInfo() : "N/A";
        String custName = (customer != null) ? customer.getFullName() : "N/A";
        return String.format("Vé: %s | Phim: %s | Ghế: %s | KH: %s | Giá: %,.0f VND | TT: %s",
                ticketCode, movieTitle, seatLabel, custName, seatPrice,
                paymentStatus.getDisplayName());
    }

    // ===================== GETTER / SETTER =====================

    public int getTicketId() {
        return ticketId;
    }

    public void setTicketId(int ticketId) {
        this.ticketId = ticketId;
    }

    public int getId() {
        return ticketId;
    }

    public void setId(int id) {
        this.ticketId = id;
    }

    public double getFinalPrice() {
        return seatPrice;
    }

    public void setFinalPrice(double finalPrice) {
        this.seatPrice = finalPrice;
    }

    public double getPrice() {
        return seatPrice;
    }

    public void setPrice(double price) {
        this.seatPrice = price;
    }

    public String getTicketCode() {
        return ticketCode;
    }

    public void setTicketCode(String ticketCode) {
        this.ticketCode = ticketCode;
    }

    public int getBookingId() {
        return bookingId;
    }

    public void setBookingId(int bookingId) {
        this.bookingId = bookingId;
    }

    public int getShowtimeSeatId() {
        return showtimeSeatId;
    }

    public void setShowtimeSeatId(int showtimeSeatId) {
        this.showtimeSeatId = showtimeSeatId;
    }

    public double getSeatPrice() {
        return seatPrice;
    }

    public void setSeatPrice(double seatPrice) {
        this.seatPrice = seatPrice;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public Movie getMovie() {
        return movie;
    }

    public void setMovie(Movie movie) {
        this.movie = movie;
    }

    public Showtime getShowtime() {
        return showtime;
    }

    public void setShowtime(Showtime showtime) {
        this.showtime = showtime;
    }

    public Seat getSeat() {
        return seat;
    }

    public void setSeat(Seat seat) {
        this.seat = seat;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    @Override
    public String toString() {
        return "Ticket{" +
                "ticketId=" + ticketId +
                ", ticketCode='" + ticketCode + '\'' +
                ", bookingId=" + bookingId +
                ", showtimeSeatId=" + showtimeSeatId +
                ", seatPrice=" + seatPrice +
                ", paymentStatus=" + paymentStatus +
                '}';
    }
}

