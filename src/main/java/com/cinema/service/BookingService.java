package com.cinema.service;

import com.cinema.model.Booking;
import com.cinema.model.Customer;
import com.cinema.model.CustomerType;
import com.cinema.model.Movie;
import com.cinema.model.MovieStatus;
import com.cinema.model.NormalCustomerPricePolicy;
import com.cinema.model.PaymentStatus;
import com.cinema.model.Seat;
import com.cinema.model.SeatStatus;
import com.cinema.model.SeatType;
import com.cinema.model.Showtime;
import com.cinema.model.StudentPricePolicy;
import com.cinema.model.Ticket;
import com.cinema.model.TicketPricePolicy;
import com.cinema.model.VipPricePolicy;
import com.cinema.repository.CustomerRepository;
import com.cinema.repository.MovieRepository;
import com.cinema.repository.ShowtimeRepository;
import com.cinema.repository.TicketRepository;
import com.cinema.utils.BusinessException;
import com.cinema.utils.IdGenerator;
import com.cinema.utils.Validator;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Service xử lý logic nghiệp vụ đặt vé xem phim (BookingService).
 * Kiểm tra nghiêm ngặt toàn bộ các Business Rules trước khi lưu dữ liệu vào MySQL:
 * <ul>
 *   <li>Rule 1: Khách hàng phải tồn tại trong cơ sở dữ liệu.</li>
 *   <li>Rule 2: Phim phải đang ở trạng thái ĐANG CHIẾU (SHOWING).</li>
 *   <li>Rule 3: Suất chiếu phải tồn tại và chưa bị hủy.</li>
 *   <li>Rule 4: Ghế chọn phải tồn tại trong phòng chiếu và đang ở trạng thái CÒN TRỐNG (AVAILABLE).</li>
 *   <li>Rule 5: Áp dụng phụ phí ghế đôi (+30k), ghế VIP (+50k) theo đúng cấu hình.</li>
 *   <li>Rule 6: Áp dụng đa hình TicketPricePolicy (Sinh viên giảm 20%, VIP giảm 15%, Thường 0%), giá vé phải > 0.</li>
 *   <li>Rule 7: Sinh mã tự động, cập nhật trạng thái ghế thành DA_DAT và lưu vé với trạng thái CHUA_THANH_TOAN.</li>
 * </ul>
 */
public class BookingService {

    private final CustomerRepository customerRepository;
    private final MovieRepository movieRepository;
    private final ShowtimeRepository showtimeRepository;
    private final TicketRepository ticketRepository;

    /**
     * Constructor mặc định.
     */
    public BookingService() {
        this.customerRepository = new CustomerRepository();
        this.movieRepository = new MovieRepository();
        this.showtimeRepository = new ShowtimeRepository();
        this.ticketRepository = new TicketRepository();
    }

    /**
     * Constructor hỗ trợ Dependency Injection.
     */
    public BookingService(CustomerRepository customerRepository,
                          MovieRepository movieRepository,
                          ShowtimeRepository showtimeRepository,
                          TicketRepository ticketRepository) {
        this.customerRepository = customerRepository;
        this.movieRepository = movieRepository;
        this.showtimeRepository = showtimeRepository;
        this.ticketRepository = ticketRepository;
    }

    /**
     * Nghiệp vụ chính: Đặt vé xem phim theo danh sách nhãn ghế (ví dụ: ["A1", "A2"]).
     * Thực hiện kiểm tra đầy đủ 7 Business Rules bắt buộc.
     *
     * @param customerId mã khách hàng thực hiện đặt vé
     * @param showtimeId mã suất chiếu
     * @param seatLabels danh sách nhãn ghế cần đặt (ví dụ: List.of("A1", "B2"))
     * @return đối tượng Booking đã được tạo và lưu thành công vào MySQL
     * @throws BusinessException nếu vi phạm bất kỳ Business Rule nào
     */
    public Booking bookTickets(int customerId, int showtimeId, List<String> seatLabels) throws BusinessException {

        // =====================================================================
        // KIỂM TRA ĐẦU VÀO CƠ BẢN
        // =====================================================================
        if (seatLabels == null || seatLabels.isEmpty()) {
            throw new BusinessException(BusinessException.INVALID_INPUT,
                    "Danh sách ghế đặt không được để trống. Vui lòng chọn ít nhất 1 ghế.");
        }

        // Kiểm tra trùng lặp ghế trong danh sách người dùng nhập
        Set<String> uniqueSeats = new HashSet<>();
        for (String label : seatLabels) {
            if (Validator.isNullOrEmpty(label)) {
                throw new BusinessException(BusinessException.INVALID_INPUT, "Tên ghế không được để trống.");
            }
            String upper = label.trim().toUpperCase();
            if (!uniqueSeats.add(upper)) {
                throw new BusinessException(BusinessException.INVALID_INPUT,
                        "Danh sách ghế bị trùng lặp: ghế '" + upper + "' được chọn nhiều lần.");
            }
        }

        // =====================================================================
        // RULE 1: KIỂM TRA KHÁCH HÀNG CÓ TỒN TẠI KHÔNG
        // =====================================================================
        Customer customer = customerRepository.findById(customerId);
        if (customer == null) {
            throw new BusinessException(BusinessException.CUSTOMER_NOT_FOUND,
                    "Khách hàng với mã ID " + customerId + " không tồn tại trong hệ thống.");
        }

        // =====================================================================
        // RULE 3: KIỂM TRA SUẤT CHIẾU CÓ TỒN TẠI KHÔNG
        // =====================================================================
        Showtime showtime = showtimeRepository.findById(showtimeId);
        if (showtime == null) {
            throw new BusinessException(BusinessException.SHOWTIME_NOT_FOUND,
                    "Suất chiếu với mã ID " + showtimeId + " không tồn tại trong hệ thống.");
        }

        if (showtime.isCancelled()) {
            throw new BusinessException(BusinessException.SHOWTIME_CANCELLED,
                    "Suất chiếu mã #" + showtimeId + " đã BỊ HỦY. Không thể đặt vé cho suất chiếu này.");
        }

        // =====================================================================
        // RULE 2: KIỂM TRA PHIM CÓ ĐANG CHIẾU KHÔNG
        // =====================================================================
        Movie movie = showtime.getMovie();
        if (movie == null) {
            movie = movieRepository.findById(showtime.getMovieId());
        }

        if (movie == null || movie.getStatus() != MovieStatus.DANG_CHIEU) {
            String title = (movie != null) ? movie.getTitle() : ("Mã #" + showtime.getMovieId());
            throw new BusinessException(BusinessException.MOVIE_NOT_SHOWING,
                    "Phim '" + title + "' hiện đã NGỪNG CHIẾU. Hệ thống không cho phép đặt vé cho phim này.");
        }

        // =====================================================================
        // RULE 4: KIỂM TRA GHẾ CÓ TỒN TẠI VÀ CÒN TRỐNG KHÔNG
        // =====================================================================
        Map<String, Seat> seatMap = showtimeRepository.getSeatMapByShowtimeId(showtimeId);
        if (seatMap.isEmpty()) {
            throw new BusinessException(BusinessException.ROOM_NOT_AVAILABLE,
                    "Phòng chiếu chưa được thiết lập danh sách ghế cho suất chiếu #" + showtimeId);
        }

        List<Seat> selectedSeats = new ArrayList<>();
        for (String label : uniqueSeats) {
            Seat seat = seatMap.get(label);

            if (seat == null) {
                throw new BusinessException(BusinessException.INVALID_INPUT,
                        "Ghế '" + label + "' KHÔNG TỒN TẠI trong phòng chiếu của suất này.");
            }

            if (!seat.isAvailable()) {
                throw new BusinessException(BusinessException.SEAT_ALREADY_BOOKED,
                        "Ghế '" + label + "' ĐÃ ĐƯỢC ĐẶT hoặc đang giữ tạm. Vui lòng chọn ghế khác.");
            }

            selectedSeats.add(seat);
        }

        // =====================================================================
        // RULE 5 & 6: ÁP DỤNG PHỤ PHÍ GHẾ VÀ ĐA HÌNH CHÍNH SÁCH GIÁ VÉ (TicketPricePolicy)
        // =====================================================================
        TicketPricePolicy pricePolicy = resolvePricePolicy(customer.getCustomerType());

        double basePrice = showtime.getBasePrice();
        double totalAmount = 0.0;
        double discountAmount = 0.0;
        double finalAmount = 0.0;
        List<Double> ticketPrices = new ArrayList<>();

        for (Seat seat : selectedSeats) {
            // Rule 5: Phụ phí theo loại ghế (Thường: 0, Đôi: 30k, VIP: 50k)
            double seatSurcharge = seat.getSeatType().getSurcharge();
            double originalSeatPrice = basePrice + seatSurcharge;

            // Rule 6: Đa hình TicketPricePolicy tính giá cuối sau giảm
            double finalSeatPrice = pricePolicy.calculateFinalPrice(basePrice, seatSurcharge);

            // Kiểm tra giá vé phải > 0
            if (finalSeatPrice <= 0) {
                throw new BusinessException(BusinessException.INVALID_INPUT,
                        "Giá vé tính toán không hợp lệ (phải lớn hơn 0): " + finalSeatPrice + " VND.");
            }

            double seatDiscount = originalSeatPrice - finalSeatPrice;

            totalAmount += originalSeatPrice;
            discountAmount += seatDiscount;
            finalAmount += finalSeatPrice;
            ticketPrices.add(finalSeatPrice);
        }

        // =====================================================================
        // RULE 7: SINH MÃ TỰ ĐỘNG, CẬP NHẬT TRẠNG THÁI GHẾ THÀNH DA_DAT VÀ LƯU VÉ
        // =====================================================================
        String bookingCode = IdGenerator.generateBookingCode();

        Booking booking = new Booking();
        booking.setBookingCode(bookingCode);
        booking.setCustomerId(customerId);
        booking.setShowtimeId(showtimeId);
        booking.setTotalAmount(totalAmount);
        booking.setDiscountAmount(discountAmount);
        booking.setFinalAmount(finalAmount);
        booking.setStatus(PaymentStatus.CHUA_THANH_TOAN);
        booking.setBookingTime(LocalDateTime.now());
        booking.setCustomer(customer);
        booking.setShowtime(showtime);

        // Lưu đơn đặt vào MySQL
        boolean bookingSaved = ticketRepository.saveBooking(booking);
        if (!bookingSaved || booking.getBookingId() <= 0) {
            throw new BusinessException(BusinessException.INVALID_INPUT,
                    "Không thể lưu đơn đặt vé vào cơ sở dữ liệu. Vui lòng thử lại.");
        }

        // Tạo từng vé tương ứng và cập nhật trạng thái ghế
        List<Ticket> tickets = new ArrayList<>();
        for (int i = 0; i < selectedSeats.size(); i++) {
            Seat seat = selectedSeats.get(i);
            double seatPrice = ticketPrices.get(i);

            // Cập nhật trạng thái ghế trong MySQL thành DA_DAT
            boolean seatUpdated = showtimeRepository.updateSeatStatus(showtimeId, seat.getSeatId(), SeatStatus.DA_DAT);
            if (!seatUpdated) {
                System.err.println("[BookingService] Cảnh báo: Không thể cập nhật trạng thái ghế " + seat.getSeatLabel());
            }

            // Sinh mã vé tự động
            String ticketCode = IdGenerator.generateTicketCode();

            Ticket ticket = new Ticket();
            ticket.setTicketCode(ticketCode);
            ticket.setBookingId(booking.getBookingId());
            ticket.setShowtimeSeatId(seat.getShowtimeSeatId());
            ticket.setSeatPrice(seatPrice);
            ticket.setCreatedAt(LocalDateTime.now());
            ticket.setCustomer(customer);
            ticket.setMovie(movie);
            ticket.setShowtime(showtime);
            ticket.setSeat(seat);
            ticket.setPaymentStatus(PaymentStatus.CHUA_THANH_TOAN);

            // Lưu vé vào bảng tickets
            ticketRepository.saveTicket(ticket);
            tickets.add(ticket);
        }

        booking.setTickets(tickets);

        System.out.println("==========================================================================================");
        System.out.printf("  ĐẶT VÉ THÀNH CÔNG! Mã đơn: %s | Số lượng vé: %d%n", bookingCode, tickets.size());
        System.out.printf("  Khách hàng: %s (%s) - Áp dụng chính sách: %s%n",
                customer.getFullName(), customer.getCustomerType().getDisplayName(), pricePolicy.getPolicyName());
        System.out.printf("  Tổng tiền gốc: %,.0f VND | Giảm giá: %,.0f VND | Cần thanh toán: %,.0f VND%n",
                totalAmount, discountAmount, finalAmount);
        System.out.println("  Trạng thái hiện tại: CHƯA THANH TOÁN (Vui lòng thanh toán để nhận vé chính thức).");
        System.out.println("==========================================================================================");

        return booking;
    }

    /**
     * Lựa chọn đối tượng đa hình TicketPricePolicy dựa trên phân loại khách hàng.
     *
     * @param customerType loại khách hàng (KHACH_THUONG, SINH_VIEN, VIP)
     * @return đối tượng thực thi cụ thể của TicketPricePolicy
     */
    public TicketPricePolicy resolvePricePolicy(CustomerType customerType) {
        if (customerType == null) {
            return new NormalCustomerPricePolicy();
        }
        switch (customerType) {
            case SINH_VIEN:
                return new StudentPricePolicy();
            case VIP:
                return new VipPricePolicy();
            case KHACH_THUONG:
            default:
                return new NormalCustomerPricePolicy();
        }
    }

    /**
     * Tra cứu thông tin đơn đặt vé theo mã code (ví dụ: BK-20261105-0001).
     *
     * @param bookingCode mã code đơn đặt
     * @return đối tượng Booking
     * @throws BusinessException nếu không tìm thấy đơn đặt
     */
    public Booking getBookingByCode(String bookingCode) throws BusinessException {
        if (Validator.isNullOrEmpty(bookingCode)) {
            throw new BusinessException(BusinessException.INVALID_INPUT, "Mã đơn hàng không được để trống.");
        }

        Booking booking = ticketRepository.findBookingByCode(bookingCode.trim());
        if (booking == null) {
            throw new BusinessException(BusinessException.BOOKING_NOT_FOUND,
                    "Không tìm thấy đơn đặt vé với mã: " + bookingCode);
        }
        return booking;
    }

    /**
     * Tra cứu thông tin đơn đặt vé theo ID.
     *
     * @param bookingId mã ID đơn đặt
     * @return đối tượng Booking
     * @throws BusinessException nếu không tìm thấy
     */
    public Booking getBookingById(int bookingId) throws BusinessException {
        if (bookingId <= 0) {
            throw new BusinessException(BusinessException.INVALID_INPUT, "Mã ID đơn hàng phải lớn hơn 0.");
        }

        Booking booking = ticketRepository.findBookingById(bookingId);
        if (booking == null) {
            throw new BusinessException(BusinessException.BOOKING_NOT_FOUND,
                    "Không tìm thấy đơn đặt vé với mã ID: " + bookingId);
        }
        return booking;
    }

    /**
     * Đặt vé theo nhãn một ghế đơn lẻ (ví dụ: "A1").
     */
    public Booking bookTickets(int customerId, int showtimeId, String seatNumber) throws BusinessException {
        if (seatNumber == null || seatNumber.trim().isEmpty()) {
            throw new BusinessException(BusinessException.INVALID_INPUT, "Số ghế không được để trống.");
        }
        return bookTickets(customerId, showtimeId, Collections.singletonList(seatNumber.trim()));
    }

    /**
     * Bí danh đặt một ghế đơn lẻ (bookTicket).
     */
    public Booking bookTicket(int customerId, int showtimeId, String seatNumber) throws BusinessException {
        return bookTickets(customerId, showtimeId, seatNumber);
    }

    /**
     * Bí danh đặt danh sách ghế (bookTicket với List<String>).
     */
    public Booking bookTicket(int customerId, int showtimeId, List<String> seatLabels) throws BusinessException {
        return bookTickets(customerId, showtimeId, seatLabels);
    }

    /**
     * Đặt vé theo danh sách tham số varargs.
     */
    public Booking bookTickets(int customerId, int showtimeId, String... seatLabels) throws BusinessException {
        if (seatLabels == null || seatLabels.length == 0) {
            throw new BusinessException(BusinessException.INVALID_INPUT, "Danh sách ghế không được để trống.");
        }
        return bookTickets(customerId, showtimeId, Arrays.asList(seatLabels));
    }

    /**
     * Đặt một vé đơn và trả về đối tượng Ticket đại diện.
     */
    public Ticket bookSingleTicket(int customerId, int showtimeId, String seatNumber) throws BusinessException {
        Booking booking = bookTickets(customerId, showtimeId, seatNumber);
        if (booking.getTickets() != null && !booking.getTickets().isEmpty()) {
            return booking.getTickets().get(0);
        }
        throw new BusinessException(BusinessException.INVALID_INPUT, "Không thể tạo vé cho ghế: " + seatNumber);
    }

    /**
     * Bí danh tìm Booking theo mã ID.
     */
    public Booking findBookingById(int bookingId) throws BusinessException {
        return getBookingById(bookingId);
    }

    /**
     * Bí danh tìm Booking theo mã code.
     */
    public Booking findBookingByCode(String bookingCode) throws BusinessException {
        return getBookingByCode(bookingCode);
    }

    /**
     * Tra cứu vé xem phim theo mã ID vé.
     */
    public Ticket getTicketById(int ticketId) throws BusinessException {
        if (ticketId <= 0) {
            throw new BusinessException(BusinessException.INVALID_INPUT, "Mã ID vé phải lớn hơn 0.");
        }
        Ticket ticket = ticketRepository.findById(ticketId);
        if (ticket == null) {
            throw new BusinessException(BusinessException.INVALID_INPUT, "Không tìm thấy vé xem phim với mã ID: " + ticketId);
        }
        return ticket;
    }

    /**
     * Bí danh tìm vé theo mã ID vé.
     */
    public Ticket findTicketById(int ticketId) throws BusinessException {
        return getTicketById(ticketId);
    }
}

