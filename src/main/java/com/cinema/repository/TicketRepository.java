package com.cinema.repository;

import com.cinema.model.Booking;
import com.cinema.model.CinemaRoom;
import com.cinema.model.Customer;
import com.cinema.model.CustomerType;
import com.cinema.model.Movie;
import com.cinema.model.Payment;
import com.cinema.model.PaymentStatus;
import com.cinema.model.Seat;
import com.cinema.model.SeatStatus;
import com.cinema.model.SeatType;
import com.cinema.model.Showtime;
import com.cinema.model.Ticket;
import com.cinema.utils.DatabaseConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Repository quản lý truy vấn và lưu trữ dữ liệu liên quan đến vé và thanh toán:
 * bảng bookings, tickets, payments trong MySQL.
 * Hỗ trợ giao dịch đặt vé, xuất vé, tra cứu vé và cập nhật trạng thái đơn đặt.
 */
public class TicketRepository {

    // =========================================================================
    // 1. CÁC THAO TÁC VỚI BẢNG BOOKINGS
    // =========================================================================

    /**
     * Lưu thông tin đơn đặt vé mới vào bảng bookings.
     * Tự động lấy ID sinh tự động và gán ngược lại vào booking.setBookingId().
     *
     * @param booking đối tượng đơn đặt vé
     * @return true nếu lưu thành công, false nếu thất bại
     */
    public boolean saveBooking(Booking booking) {
        if (booking == null) {
            System.err.println("[TicketRepository] Không thể lưu booking null.");
            return false;
        }

        String sql = "INSERT INTO bookings (booking_code, customer_id, showtime_id, total_amount, "
                   + "                      discount_amount, final_amount, status, booking_time) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, booking.getBookingCode());
            ps.setInt(2, booking.getCustomerId());
            ps.setInt(3, booking.getShowtimeId());
            ps.setDouble(4, booking.getTotalAmount());
            ps.setDouble(5, booking.getDiscountAmount());
            ps.setDouble(6, booking.getFinalAmount());

            String statusStr = (booking.getStatus() != null)
                    ? booking.getStatus().getDbValue()
                    : PaymentStatus.CHUA_THANH_TOAN.getDbValue();
            ps.setString(7, statusStr);

            if (booking.getBookingTime() != null) {
                ps.setTimestamp(8, Timestamp.valueOf(booking.getBookingTime()));
            } else {
                ps.setTimestamp(8, Timestamp.valueOf(LocalDateTime.now()));
            }

            int affectedRows = ps.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        booking.setBookingId(generatedKeys.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("[TicketRepository] Lỗi khi lưu booking (" + booking.getBookingCode() + "): " + e.getMessage());
        }
        return false;
    }

    /**
     * Phương thức đa hình lưu Booking (bí danh tương thích save).
     */
    public boolean save(Booking booking) {
        return saveBooking(booking);
    }

    /**
     * Tìm đơn đặt vé theo mã số định danh booking_id.
     * Nạp kèm thông tin khách hàng, suất chiếu và danh sách vé đã đặt.
     *
     * @param bookingId mã đơn đặt
     * @return đối tượng Booking hoặc null nếu không tồn tại
     */
    public Booking findBookingById(int bookingId) {
        String sql = "SELECT b.booking_id, b.booking_code, b.customer_id, b.showtime_id, "
                   + "       b.total_amount, b.discount_amount, b.final_amount, b.status, "
                   + "       b.booking_time, b.updated_at, "
                   + "       c.full_name AS customer_name, c.email AS customer_email, "
                   + "       c.phone AS customer_phone, c.customer_type, c.student_id, "
                   + "       s.show_date, s.start_time, s.end_time, s.base_price, "
                   + "       m.title AS movie_title, r.room_name "
                   + "FROM bookings b "
                   + "JOIN customers c ON b.customer_id = c.customer_id "
                   + "JOIN showtimes s ON b.showtime_id = s.showtime_id "
                   + "JOIN movies m ON s.movie_id = m.movie_id "
                   + "JOIN cinema_rooms r ON s.room_id = r.room_id "
                   + "WHERE b.booking_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, bookingId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Booking booking = mapResultSetToBooking(rs);
                    // Nạp danh sách vé thuộc booking này
                    List<Ticket> tickets = findTicketsByBookingId(bookingId);
                    booking.setTickets(tickets);
                    // Nạp thanh toán nếu có
                    Payment payment = findPaymentByBookingId(bookingId);
                    booking.setPayment(payment);
                    return booking;
                }
            }
        } catch (SQLException e) {
            System.err.println("[TicketRepository] Lỗi khi tìm booking theo ID (" + bookingId + "): " + e.getMessage());
        }
        return null;
    }

    /**
     * Tìm đơn đặt vé theo mã hiển thị (booking_code, ví dụ: BK-20261105-0001).
     *
     * @param bookingCode mã code đơn đặt
     * @return đối tượng Booking hoặc null
     */
    public Booking findBookingByCode(String bookingCode) {
        if (bookingCode == null || bookingCode.trim().isEmpty()) {
            return null;
        }

        String sql = "SELECT booking_id FROM bookings WHERE booking_code = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, bookingCode.trim());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return findBookingById(rs.getInt("booking_id"));
                }
            }
        } catch (SQLException e) {
            System.err.println("[TicketRepository] Lỗi khi tìm booking theo mã (" + bookingCode + "): " + e.getMessage());
        }
        return null;
    }

    /**
     * Cập nhật trạng thái thanh toán của đơn đặt vé trong bảng bookings.
     *
     * @param bookingId mã đơn đặt vé
     * @param status    trạng thái mới (CHUA_THANH_TOAN, DA_THANH_TOAN, DA_HUY)
     * @return true nếu cập nhật thành công, false nếu thất bại
     */
    public boolean updatePaymentStatus(int bookingId, PaymentStatus status) {
        if (bookingId <= 0 || status == null) {
            return false;
        }

        String sql = "UPDATE bookings SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE booking_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, status.getDbValue());
            ps.setInt(2, bookingId);

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[TicketRepository] Lỗi khi cập nhật trạng thái thanh toán booking (" + bookingId + "): " + e.getMessage());
        }
        return false;
    }

    // =========================================================================
    // 2. CÁC THAO TÁC VỚI BẢNG TICKETS
    // =========================================================================

    /**
     * Lưu một vé mới vào bảng tickets trong MySQL.
     * Tự động lấy ticket_id vừa sinh gán vào ticket.setTicketId().
     *
     * @param ticket đối tượng Ticket cần lưu
     * @return true nếu lưu thành công, false nếu thất bại
     */
    public boolean saveTicket(Ticket ticket) {
        if (ticket == null) {
            System.err.println("[TicketRepository] Không thể lưu ticket null.");
            return false;
        }

        String sql = "INSERT INTO tickets (ticket_code, booking_id, showtime_seat_id, seat_price, created_at) "
                   + "VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, ticket.getTicketCode());
            ps.setInt(2, ticket.getBookingId());
            ps.setInt(3, ticket.getShowtimeSeatId());
            ps.setDouble(4, ticket.getSeatPrice());

            if (ticket.getCreatedAt() != null) {
                ps.setTimestamp(5, Timestamp.valueOf(ticket.getCreatedAt()));
            } else {
                ps.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
            }

            int affectedRows = ps.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        ticket.setTicketId(generatedKeys.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("[TicketRepository] Lỗi khi lưu ticket (" + ticket.getTicketCode() + "): " + e.getMessage());
        }
        return false;
    }

    /**
     * Phương thức đa hình lưu Ticket (bí danh tương thích save).
     */
    public boolean save(Ticket ticket) {
        return saveTicket(ticket);
    }

    /**
     * Tìm vé theo mã định danh ticket_id.
     * Nạp đầy đủ các đối tượng quan hệ: Customer, Movie, Showtime, Seat, PaymentStatus.
     *
     * @param ticketId mã vé cần tìm
     * @return đối tượng Ticket đầy đủ thông tin hoặc null nếu không tồn tại
     */
    public Ticket findById(int ticketId) {
        String sql = "SELECT t.ticket_id, t.ticket_code, t.booking_id, t.showtime_seat_id, "
                   + "       t.seat_price, t.created_at AS ticket_created_at, "
                   + "       b.booking_code, b.status AS booking_status, "
                   + "       c.customer_id, c.full_name AS customer_name, c.email AS customer_email, "
                   + "       c.phone AS customer_phone, c.customer_type, c.student_id, "
                   + "       s.showtime_id, s.show_date, s.start_time, s.end_time, s.base_price, "
                   + "       m.movie_id, m.title AS movie_title, m.genre, m.duration, "
                   + "       r.room_id, r.room_name, "
                   + "       st.seat_id, st.seat_row, st.seat_number, st.seat_type, "
                   + "       ss.status AS seat_status "
                   + "FROM tickets t "
                   + "JOIN bookings b ON t.booking_id = b.booking_id "
                   + "JOIN customers c ON b.customer_id = c.customer_id "
                   + "JOIN showtime_seats ss ON t.showtime_seat_id = ss.showtime_seat_id "
                   + "JOIN showtimes s ON ss.showtime_id = s.showtime_id "
                   + "JOIN movies m ON s.movie_id = m.movie_id "
                   + "JOIN cinema_rooms r ON s.room_id = r.room_id "
                   + "JOIN seats st ON ss.seat_id = st.seat_id "
                   + "WHERE t.ticket_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, ticketId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToTicket(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[TicketRepository] Lỗi khi tìm vé theo ID (" + ticketId + "): " + e.getMessage());
        }
        return null;
    }

    /**
     * Tìm vé theo ID (bí danh tương thích findTicketById).
     */
    public Ticket findTicketById(int ticketId) {
        return findById(ticketId);
    }

    /**
     * Lấy vé theo ID (bí danh tương thích getTicketById).
     */
    public Ticket getTicketById(int ticketId) {
        return findById(ticketId);
    }

    /**
     * Lấy đơn đặt theo ID (bí danh tương thích getBookingById).
     */
    public Booking getBookingById(int bookingId) {
        return findBookingById(bookingId);
    }

    /**
     * Tìm vé theo mã hiển thị (ticket_code, ví dụ: TK-20261105-0001).
     *
     * @param ticketCode mã vé hiển thị
     * @return đối tượng Ticket hoặc null
     */
    public Ticket findByCode(String ticketCode) {
        if (ticketCode == null || ticketCode.trim().isEmpty()) {
            return null;
        }

        String sql = "SELECT ticket_id FROM tickets WHERE ticket_code = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, ticketCode.trim());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return findById(rs.getInt("ticket_id"));
                }
            }
        } catch (SQLException e) {
            System.err.println("[TicketRepository] Lỗi khi tìm vé theo mã code (" + ticketCode + "): " + e.getMessage());
        }
        return null;
    }

    /**
     * Lấy toàn bộ danh sách vé thuộc về một đơn đặt (booking_id).
     *
     * @param bookingId mã đơn đặt
     * @return danh sách vé của đơn đặt đó
     */
    public List<Ticket> findTicketsByBookingId(int bookingId) {
        List<Ticket> list = new ArrayList<>();
        String sql = "SELECT t.ticket_id, t.ticket_code, t.booking_id, t.showtime_seat_id, "
                   + "       t.seat_price, t.created_at AS ticket_created_at, "
                   + "       b.booking_code, b.status AS booking_status, "
                   + "       c.customer_id, c.full_name AS customer_name, c.email AS customer_email, "
                   + "       c.phone AS customer_phone, c.customer_type, c.student_id, "
                   + "       s.showtime_id, s.show_date, s.start_time, s.end_time, s.base_price, "
                   + "       m.movie_id, m.title AS movie_title, m.genre, m.duration, "
                   + "       r.room_id, r.room_name, "
                   + "       st.seat_id, st.seat_row, st.seat_number, st.seat_type, "
                   + "       ss.status AS seat_status "
                   + "FROM tickets t "
                   + "JOIN bookings b ON t.booking_id = b.booking_id "
                   + "JOIN customers c ON b.customer_id = c.customer_id "
                   + "JOIN showtime_seats ss ON t.showtime_seat_id = ss.showtime_seat_id "
                   + "JOIN showtimes s ON ss.showtime_id = s.showtime_id "
                   + "JOIN movies m ON s.movie_id = m.movie_id "
                   + "JOIN cinema_rooms r ON s.room_id = r.room_id "
                   + "JOIN seats st ON ss.seat_id = st.seat_id "
                   + "WHERE t.booking_id = ? "
                   + "ORDER BY t.ticket_id ASC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, bookingId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToTicket(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[TicketRepository] Lỗi khi lấy danh sách vé theo booking (" + bookingId + "): " + e.getMessage());
        }
        return list;
    }

    /**
     * Lấy toàn bộ danh sách vé trong hệ thống.
     *
     * @return danh sách vé
     */
    public List<Ticket> findAll() {
        return findAllTickets();
    }

    /**
     * Lấy tất cả vé từ cơ sở dữ liệu.
     *
     * @return danh sách đối tượng Ticket
     */
    public List<Ticket> findAllTickets() {
        List<Ticket> list = new ArrayList<>();
        String sql = "SELECT t.ticket_id, t.ticket_code, t.booking_id, t.showtime_seat_id, "
                   + "       t.seat_price, t.created_at AS ticket_created_at, "
                   + "       b.booking_code, b.status AS booking_status, "
                   + "       c.customer_id, c.full_name AS customer_name, c.email AS customer_email, "
                   + "       c.phone AS customer_phone, c.customer_type, c.student_id, "
                   + "       s.showtime_id, s.show_date, s.start_time, s.end_time, s.base_price, "
                   + "       m.movie_id, m.title AS movie_title, m.genre, m.duration, "
                   + "       r.room_id, r.room_name, "
                   + "       st.seat_id, st.seat_row, st.seat_number, st.seat_type, "
                   + "       ss.status AS seat_status "
                   + "FROM tickets t "
                   + "JOIN bookings b ON t.booking_id = b.booking_id "
                   + "JOIN customers c ON b.customer_id = c.customer_id "
                   + "JOIN showtime_seats ss ON t.showtime_seat_id = ss.showtime_seat_id "
                   + "JOIN showtimes s ON ss.showtime_id = s.showtime_id "
                   + "JOIN movies m ON s.movie_id = m.movie_id "
                   + "JOIN cinema_rooms r ON s.room_id = r.room_id "
                   + "JOIN seats st ON ss.seat_id = st.seat_id "
                   + "ORDER BY t.ticket_id ASC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToTicket(rs));
            }
        } catch (SQLException e) {
            System.err.println("[TicketRepository] Lỗi khi lấy tất cả vé: " + e.getMessage());
        }
        return list;
    }

    /**
     * Lấy tất cả đơn đặt vé từ cơ sở dữ liệu.
     *
     * @return danh sách đối tượng Booking
     */
    public List<Booking> findAllBookings() {
        List<Booking> list = new ArrayList<>();
        String sql = "SELECT b.booking_id, b.booking_code, b.customer_id, b.showtime_id, "
                   + "       b.total_amount, b.discount_amount, b.final_amount, b.status, "
                   + "       b.booking_time, b.updated_at, "
                   + "       c.full_name AS customer_name, c.email AS customer_email, "
                   + "       c.phone AS customer_phone, c.customer_type, c.student_id, "
                   + "       s.show_date, s.start_time, s.end_time, s.base_price, "
                   + "       m.title AS movie_title, r.room_name "
                   + "FROM bookings b "
                   + "JOIN customers c ON b.customer_id = c.customer_id "
                   + "JOIN showtimes s ON b.showtime_id = s.showtime_id "
                   + "JOIN movies m ON s.movie_id = m.movie_id "
                   + "JOIN cinema_rooms r ON s.room_id = r.room_id "
                   + "ORDER BY b.booking_id DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Booking booking = mapResultSetToBooking(rs);
                List<Ticket> tickets = findTicketsByBookingId(booking.getBookingId());
                booking.setTickets(tickets);
                list.add(booking);
            }
        } catch (SQLException e) {
            System.err.println("[TicketRepository] Lỗi khi lấy danh sách booking: " + e.getMessage());
        }
        return list;
    }

    // =========================================================================
    // 3. CÁC THAO TÁC VỚI BẢNG PAYMENTS
    // =========================================================================

    /**
     * Lưu thông tin giao dịch thanh toán vào bảng payments.
     * Tự động lấy ID sinh tự động và gán ngược lại vào payment.setPaymentId().
     *
     * @param payment đối tượng Payment cần lưu
     * @return true nếu lưu thành công, false nếu thất bại
     */
    public boolean savePayment(Payment payment) {
        if (payment == null) {
            System.err.println("[TicketRepository] Không thể lưu payment null.");
            return false;
        }

        String sql = "INSERT INTO payments (payment_code, booking_id, amount, payment_method, payment_time, status) "
                   + "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, payment.getPaymentCode());
            ps.setInt(2, payment.getBookingId());
            ps.setDouble(3, payment.getAmount());
            ps.setString(4, payment.getPaymentMethod() != null ? payment.getPaymentMethod() : "CASH");

            if (payment.getPaymentTime() != null) {
                ps.setTimestamp(5, Timestamp.valueOf(payment.getPaymentTime()));
            } else {
                ps.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
            }

            ps.setString(6, payment.getStatus() != null ? payment.getStatus() : "SUCCESS");

            int affectedRows = ps.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        payment.setPaymentId(generatedKeys.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("[TicketRepository] Lỗi khi lưu payment (" + payment.getPaymentCode() + "): " + e.getMessage());
        }
        return false;
    }

    /**
     * Phương thức đa hình lưu Payment (bí danh tương thích save).
     */
    public boolean save(Payment payment) {
        return savePayment(payment);
    }

    /**
     * Tìm thông tin thanh toán theo mã đơn đặt vé (booking_id).
     *
     * @param bookingId mã đơn đặt vé
     * @return đối tượng Payment nếu đã thanh toán, hoặc null nếu chưa có
     */
    public Payment findPaymentByBookingId(int bookingId) {
        String sql = "SELECT payment_id, payment_code, booking_id, amount, payment_method, payment_time, status "
                   + "FROM payments WHERE booking_id = ? AND status = 'SUCCESS' "
                   + "ORDER BY payment_id DESC LIMIT 1";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, bookingId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int paymentId = rs.getInt("payment_id");
                    String paymentCode = rs.getString("payment_code");
                    double amount = rs.getDouble("amount");
                    String method = rs.getString("payment_method");
                    Timestamp timeTs = rs.getTimestamp("payment_time");
                    LocalDateTime time = (timeTs != null) ? timeTs.toLocalDateTime() : null;
                    String status = rs.getString("status");

                    return new Payment(paymentId, paymentCode, bookingId, amount, method, time, status);
                }
            }
        } catch (SQLException e) {
            System.err.println("[TicketRepository] Lỗi khi tìm payment theo booking (" + bookingId + "): " + e.getMessage());
        }
        return null;
    }

    /**
     * Lấy bản tóm tắt thông tin đơn đặt vé bằng Collection Map.
     * Sử dụng cấu trúc Map<String, Object> để thuận tiện chuyển dữ liệu sang lớp hiển thị hoặc xuất file.
     *
     * @param bookingId mã đơn hàng
     * @return Map chứa các thuộc tính tổng kết đơn hàng
     */
    public Map<String, Object> getBookingSummary(int bookingId) {
        Map<String, Object> summary = new LinkedHashMap<>();
        Booking booking = findBookingById(bookingId);
        if (booking == null) {
            return summary;
        }

        summary.put("bookingId", booking.getBookingId());
        summary.put("bookingCode", booking.getBookingCode());
        summary.put("customerName", booking.getCustomer() != null ? booking.getCustomer().getFullName() : "N/A");
        summary.put("customerType", booking.getCustomer() != null ? booking.getCustomer().getCustomerType().getDisplayName() : "N/A");
        summary.put("totalAmount", booking.getTotalAmount());
        summary.put("discountAmount", booking.getDiscountAmount());
        summary.put("finalAmount", booking.getFinalAmount());
        summary.put("status", booking.getStatus().getDisplayName());
        summary.put("ticketCount", booking.getTicketCount());

        List<String> seatLabels = new ArrayList<>();
        if (booking.getTickets() != null) {
            for (Ticket t : booking.getTickets()) {
                if (t.getSeat() != null) {
                    seatLabels.add(t.getSeat().getSeatLabel() + " (" + t.getSeat().getSeatType().getDisplayName() + ")");
                }
            }
        }
        summary.put("seats", seatLabels);

        return summary;
    }

    // =========================================================================
    // 4. HÀM ÁNH XẠ NỘI BỘ (MAP RESULT SET)
    // =========================================================================

    /**
     * Ánh xạ ResultSet thành đối tượng Booking.
     */
    private Booking mapResultSetToBooking(ResultSet rs) throws SQLException {
        int bookingId = rs.getInt("booking_id");
        String bookingCode = rs.getString("booking_code");
        int customerId = rs.getInt("customer_id");
        int showtimeId = rs.getInt("showtime_id");
        double totalAmount = rs.getDouble("total_amount");
        double discountAmount = rs.getDouble("discount_amount");
        double finalAmount = rs.getDouble("final_amount");
        String statusStr = rs.getString("status");

        Timestamp bookingTs = rs.getTimestamp("booking_time");
        LocalDateTime bookingTime = (bookingTs != null) ? bookingTs.toLocalDateTime() : null;

        Timestamp updatedTs = rs.getTimestamp("updated_at");
        LocalDateTime updatedAt = (updatedTs != null) ? updatedTs.toLocalDateTime() : null;

        PaymentStatus status;
        try {
            status = PaymentStatus.fromDbValue(statusStr);
        } catch (IllegalArgumentException e) {
            status = PaymentStatus.CHUA_THANH_TOAN;
        }

        // Tạo đối tượng Customer liên kết
        Customer customer = new Customer();
        customer.setId(customerId);
        customer.setFullName(rs.getString("customer_name"));
        customer.setEmail(rs.getString("customer_email"));
        customer.setPhone(rs.getString("customer_phone"));
        try {
            customer.setCustomerType(CustomerType.fromDbValue(rs.getString("customer_type")));
        } catch (Exception ignored) {
            customer.setCustomerType(CustomerType.KHACH_THUONG);
        }
        customer.setStudentId(rs.getString("student_id"));

        // Tạo đối tượng Showtime liên kết
        Showtime showtime = new Showtime();
        showtime.setShowtimeId(showtimeId);
        java.sql.Date showDate = rs.getDate("show_date");
        if (showDate != null) showtime.setShowDate(showDate.toLocalDate());
        java.sql.Time startTime = rs.getTime("start_time");
        if (startTime != null) showtime.setStartTime(startTime.toLocalTime());
        java.sql.Time endTime = rs.getTime("end_time");
        if (endTime != null) showtime.setEndTime(endTime.toLocalTime());
        showtime.setBasePrice(rs.getDouble("base_price"));

        Movie movie = new Movie();
        movie.setTitle(rs.getString("movie_title"));
        showtime.setMovie(movie);

        CinemaRoom room = new CinemaRoom();
        room.setRoomName(rs.getString("room_name"));
        showtime.setRoom(room);

        Booking booking = new Booking(bookingId, bookingCode, customerId, showtimeId,
                totalAmount, discountAmount, finalAmount, status, bookingTime, updatedAt);
        booking.setCustomer(customer);
        booking.setShowtime(showtime);
        return booking;
    }

    /**
     * Ánh xạ ResultSet thành đối tượng Ticket đầy đủ liên kết.
     */
    private Ticket mapResultSetToTicket(ResultSet rs) throws SQLException {
        int ticketId = rs.getInt("ticket_id");
        String ticketCode = rs.getString("ticket_code");
        int bookingId = rs.getInt("booking_id");
        int showtimeSeatId = rs.getInt("showtime_seat_id");
        double seatPrice = rs.getDouble("seat_price");

        Timestamp createdTs = rs.getTimestamp("ticket_created_at");
        LocalDateTime createdAt = (createdTs != null) ? createdTs.toLocalDateTime() : null;

        // Khách hàng
        Customer customer = new Customer();
        customer.setId(rs.getInt("customer_id"));
        customer.setFullName(rs.getString("customer_name"));
        customer.setEmail(rs.getString("customer_email"));
        customer.setPhone(rs.getString("customer_phone"));
        try {
            customer.setCustomerType(CustomerType.fromDbValue(rs.getString("customer_type")));
        } catch (Exception ignored) {
            customer.setCustomerType(CustomerType.KHACH_THUONG);
        }
        customer.setStudentId(rs.getString("student_id"));

        // Phim
        Movie movie = new Movie();
        movie.setMovieId(rs.getInt("movie_id"));
        movie.setTitle(rs.getString("movie_title"));
        movie.setGenre(rs.getString("genre"));
        movie.setDuration(rs.getInt("duration"));

        // Phòng chiếu
        CinemaRoom room = new CinemaRoom();
        room.setRoomId(rs.getInt("room_id"));
        room.setRoomName(rs.getString("room_name"));

        // Suất chiếu
        Showtime showtime = new Showtime();
        showtime.setShowtimeId(rs.getInt("showtime_id"));
        java.sql.Date showDate = rs.getDate("show_date");
        if (showDate != null) showtime.setShowDate(showDate.toLocalDate());
        java.sql.Time startTime = rs.getTime("start_time");
        if (startTime != null) showtime.setStartTime(startTime.toLocalTime());
        java.sql.Time endTime = rs.getTime("end_time");
        if (endTime != null) showtime.setEndTime(endTime.toLocalTime());
        showtime.setBasePrice(rs.getDouble("base_price"));
        showtime.setMovie(movie);
        showtime.setRoom(room);

        // Ghế
        int seatId = rs.getInt("seat_id");
        String seatRow = rs.getString("seat_row");
        int seatNumber = rs.getInt("seat_number");
        SeatType seatType;
        try {
            seatType = SeatType.fromDbValue(rs.getString("seat_type"));
        } catch (Exception ignored) {
            seatType = SeatType.GHE_THUONG;
        }
        SeatStatus seatStatus;
        try {
            seatStatus = SeatStatus.fromDbValue(rs.getString("seat_status"));
        } catch (Exception ignored) {
            seatStatus = SeatStatus.DA_DAT;
        }
        Seat seat = new Seat(seatId, room.getRoomId(), seatRow, seatNumber, seatType, seatStatus, showtimeSeatId, null);

        // Trạng thái thanh toán từ booking
        PaymentStatus paymentStatus;
        try {
            paymentStatus = PaymentStatus.fromDbValue(rs.getString("booking_status"));
        } catch (Exception ignored) {
            paymentStatus = PaymentStatus.CHUA_THANH_TOAN;
        }

        Ticket ticket = new Ticket(ticketId, ticketCode, bookingId, showtimeSeatId, seatPrice, createdAt);
        ticket.setCustomer(customer);
        ticket.setMovie(movie);
        ticket.setShowtime(showtime);
        ticket.setSeat(seat);
        ticket.setPaymentStatus(paymentStatus);

        return ticket;
    }
}

