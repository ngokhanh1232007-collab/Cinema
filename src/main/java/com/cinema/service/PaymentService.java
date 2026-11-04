package com.cinema.service;

import com.cinema.model.BankTransferPayment;
import com.cinema.model.Booking;
import com.cinema.model.CashPayment;
import com.cinema.model.EWalletPayment;
import com.cinema.model.Payment;
import com.cinema.model.PaymentMethod;
import com.cinema.model.PaymentStatus;
import com.cinema.model.Seat;
import com.cinema.model.SeatStatus;
import com.cinema.model.Ticket;
import com.cinema.repository.ShowtimeRepository;
import com.cinema.repository.TicketRepository;
import com.cinema.utils.BusinessException;
import com.cinema.utils.IdGenerator;
import com.cinema.utils.TicketFileExporter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service xử lý các nghiệp vụ liên quan đến thanh toán vé và đơn đặt vé (PaymentService).
 * <ul>
 *   <li>Kiểm tra sự tồn tại của đơn hàng / vé và tính hợp lệ trước khi thanh toán.</li>
 *   <li>Hỗ trợ đa hình PaymentMethod (Tiền mặt, Chuyển khoản, Ví điện tử).</li>
 *   <li>Thanh toán thành công: Cập nhật DA_THANH_TOAN trong MySQL, lưu bảng payments, và xuất file vé .txt.</li>
 *   <li>Thanh toán thất bại: Giữ nguyên CHUA_THANH_TOAN để khách thử lại phương thức khác;
 *       hoặc hủy đơn giải phóng ghế về CON_TRONG khi khách chủ động hủy giao dịch.</li>
 * </ul>
 */
public class PaymentService {

    private final TicketRepository ticketRepository;
    private final ShowtimeRepository showtimeRepository;

    /**
     * Constructor mặc định.
     */
    public PaymentService() {
        this.ticketRepository = new TicketRepository();
        this.showtimeRepository = new ShowtimeRepository();
    }

    /**
     * Constructor hỗ trợ Dependency Injection.
     *
     * @param ticketRepository   repository vé và thanh toán
     * @param showtimeRepository repository suất chiếu và ghế
     */
    public PaymentService(TicketRepository ticketRepository, ShowtimeRepository showtimeRepository) {
        this.ticketRepository = ticketRepository;
        this.showtimeRepository = showtimeRepository;
    }

    /**
     * Nghiệp vụ chính: Xử lý thanh toán cho một đơn đặt vé (Booking).
     * Áp dụng tính đa hình đối với đối tượng PaymentMethod.
     *
     * @param bookingId     mã đơn đặt vé cần thanh toán
     * @param paymentMethod phương thức thanh toán đa hình (CashPayment, BankTransferPayment, EWalletPayment)
     * @return đối tượng Payment đã lưu thành công nếu thanh toán hoàn tất
     * @throws BusinessException nếu đơn hàng không tồn tại, đã thanh toán, đã hủy, hoặc thanh toán thất bại
     */
    public Payment processPayment(int bookingId, PaymentMethod paymentMethod) throws BusinessException {
        return processPayment(bookingId, paymentMethod, false);
    }

    /**
     * Nghiệp vụ thanh toán cho đơn đặt vé với cờ giả lập thất bại (hỗ trợ kiểm thử/demo).
     *
     * @param bookingId     mã đơn đặt vé
     * @param paymentMethod phương thức thanh toán
     * @param simulateFail  true để kích hoạt kịch bản thanh toán thất bại
     * @return đối tượng Payment đã lưu
     * @throws BusinessException khi xảy ra lỗi hoặc giả lập thất bại
     */
    public Payment processPayment(int bookingId, PaymentMethod paymentMethod, boolean simulateFail) throws BusinessException {

        // =====================================================================
        // BƯỚC 1: KIỂM TRA ĐƠN ĐẶT VÉ CÓ TỒN TẠI KHÔNG
        // =====================================================================
        if (bookingId <= 0) {
            throw new BusinessException(BusinessException.INVALID_INPUT, "Mã đơn hàng thanh toán phải lớn hơn 0.");
        }

        Booking booking = ticketRepository.findBookingById(bookingId);
        if (booking == null) {
            throw new BusinessException(BusinessException.BOOKING_NOT_FOUND,
                    "Không tìm thấy đơn đặt vé với mã ID: " + bookingId + ". Không thể thực hiện thanh toán.");
        }

        if (booking.isPaid()) {
            throw new BusinessException(BusinessException.PAYMENT_ALREADY_DONE,
                    "Đơn đặt vé '" + booking.getBookingCode() + "' ĐÃ ĐƯỢC THANH TOÁN trước đó. Không thể thanh toán lại.");
        }

        if (booking.isCancelled()) {
            throw new BusinessException(BusinessException.BOOKING_ALREADY_CANCELLED,
                    "Đơn đặt vé '" + booking.getBookingCode() + "' ĐÃ BỊ HỦY. Không thể thực hiện thanh toán.");
        }

        List<Ticket> tickets = booking.getTickets();
        if (tickets == null || tickets.isEmpty()) {
            throw new BusinessException(BusinessException.INVALID_INPUT,
                    "Đơn đặt vé '" + booking.getBookingCode() + "' không có vé nào để thanh toán.");
        }

        // =====================================================================
        // BƯỚC 2: CHỌN ĐA HÌNH PHƯƠNG THỨC THANH TOÁN (PaymentMethod)
        // =====================================================================
        if (paymentMethod == null) {
            throw new BusinessException(BusinessException.INVALID_INPUT,
                    "Vui lòng chọn một phương thức thanh toán hợp lệ (Tiền mặt, Chuyển khoản, hoặc Ví điện tử).");
        }

        double finalAmount = booking.getFinalAmount();
        if (finalAmount <= 0) {
            throw new BusinessException(BusinessException.INVALID_INPUT,
                    "Số tiền cần thanh toán không hợp lệ: " + finalAmount + " VND.");
        }

        System.out.printf("%n[PaymentService] Bắt đầu xử lý thanh toán cho đơn: %s%n", booking.getBookingCode());
        System.out.printf("  Phương thức: %s | Số tiền: %,.0f VND%n", paymentMethod.getMethodName(), finalAmount);

        // Kiểm tra cờ giả lập thanh toán thất bại
        if (simulateFail) {
            System.err.printf("[PaymentService] [GIẢ LẬP] Thanh toán đơn hàng %s qua phương thức %s THẤT BẠI.%n",
                    booking.getBookingCode(), paymentMethod.getMethodName());
            throw new BusinessException(BusinessException.PAYMENT_FAILED,
                    "MÔ PHỎNG THANH TOÁN THẤT BẠI: Số dư tài khoản không đủ hoặc thẻ bị từ chối qua "
                            + paymentMethod.getMethodName() + "! "
                            + "Hệ thống vẫn giữ ghế tạm (trạng thái CHƯA THANH TOÁN) để quý khách có thể thử lại.");
        }

        // Thực thi đa hình: gọi hàm pay() của đối tượng thanh toán cụ thể
        boolean paymentSuccess = paymentMethod.pay(finalAmount);

        // =====================================================================
        // BƯỚC 3: XỬ LÝ KẾT QUẢ THANH TOÁN
        // =====================================================================
        if (paymentSuccess) {
            // ----- 3A. THANH TOÁN THÀNH CÔNG -----
            // 1. Cập nhật trạng thái đơn đặt vé thành DA_THANH_TOAN (CONFIRMED) trong MySQL
            boolean statusUpdated = ticketRepository.updatePaymentStatus(bookingId, PaymentStatus.DA_THANH_TOAN);
            if (!statusUpdated) {
                System.err.println("[PaymentService] Cảnh báo: Không thể cập nhật trạng thái đơn đặt vé " + bookingId);
            }

            // 2. Sinh mã thanh toán tự động và lưu vào bảng payments
            String paymentCode = IdGenerator.generatePaymentCode();
            Payment payment = new Payment(
                    0,
                    paymentCode,
                    bookingId,
                    finalAmount,
                    paymentMethod.getMethodCode(),
                    LocalDateTime.now(),
                    "SUCCESS"
            );

            boolean paymentSaved = ticketRepository.savePayment(payment);
            if (!paymentSaved) {
                System.err.println("[PaymentService] Cảnh báo: Không thể lưu thông tin bảng payments cho đơn " + bookingId);
            }

            booking.setStatus(PaymentStatus.DA_THANH_TOAN);
            booking.setPayment(payment);

            // 3. Gọi TicketFileExporter xuất hóa đơn và vé ra file .txt (File IO)
            exportTicketReceipt(booking, payment, paymentMethod.getMethodName());

            System.out.println("==========================================================================================");
            System.out.printf("  THANH TOÁN THÀNH CÔNG! Mã giao dịch: %s%n", paymentCode);
            System.out.printf("  Đơn hàng: %s | Số tiền: %,.0f VND | Hình thức: %s%n",
                    booking.getBookingCode(), finalAmount, paymentMethod.getMethodName());
            System.out.println("  Trạng thái: ĐÃ THANH TOÁN. Vé đã được xuất ra thư mục exported_tickets/");
            System.out.println("==========================================================================================");

            return payment;

        } else {
            // ----- 3B. THANH TOÁN THẤT BẠI -----
            // THIẾT KẾ RÕ RÀNG:
            // Khi thanh toán qua cổng thất bại, trạng thái đơn hàng và vé vẫn giữ nguyên là CHUA_THANH_TOAN,
            // ghế vẫn được giữ tạm (DA_DAT) để khách hàng có cơ hội đổi phương thức khác (VD: chuyển sang Tiền mặt).
            // Nếu khách muốn hủy hẳn, họ có thể gọi cancelBooking() để giải phóng ghế về CON_TRONG.
            System.err.printf("[PaymentService] Thanh toán đơn hàng %s qua phương thức %s THẤT BẠI.%n",
                    booking.getBookingCode(), paymentMethod.getMethodName());

            throw new BusinessException(BusinessException.PAYMENT_FAILED,
                    "Giao dịch thanh toán qua " + paymentMethod.getMethodName()
                            + " THẤT BẠI. Đơn đặt vé vẫn ở trạng thái CHƯA THANH TOÁN. "
                            + "Quý khách có thể thử lại hoặc chọn phương thức khác.");
        }
    }

    /**
     * Nghiệp vụ thanh toán vé theo mã vé lẻ (ticket_id).
     * Tự động tìm đơn đặt chứa vé đó và tiến hành thanh toán.
     *
     * @param ticketId      mã vé cần thanh toán
     * @param paymentMethod phương thức thanh toán
     * @return đối tượng Payment đã lưu
     * @throws BusinessException nếu vé không tồn tại hoặc có lỗi thanh toán
     */
    public Payment processPaymentByTicketId(int ticketId, PaymentMethod paymentMethod) throws BusinessException {
        return processPaymentByTicketId(ticketId, paymentMethod, false);
    }

    /**
     * Thanh toán vé theo mã vé lẻ kèm cờ giả lập thất bại.
     */
    public Payment processPaymentByTicketId(int ticketId, PaymentMethod paymentMethod, boolean simulateFail) throws BusinessException {
        if (ticketId <= 0) {
            throw new BusinessException(BusinessException.INVALID_INPUT, "Mã vé phải lớn hơn 0.");
        }

        Ticket ticket = ticketRepository.findById(ticketId);
        if (ticket == null) {
            throw new BusinessException(BusinessException.INVALID_INPUT,
                    "Không tìm thấy vé xem phim với mã ID: " + ticketId + ". Không thể thanh toán.");
        }

        return processPayment(ticket.getBookingId(), paymentMethod, simulateFail);
    }

    /**
     * Bí danh thanh toán đơn đặt vé (pay).
     */
    public Payment pay(int bookingId, PaymentMethod paymentMethod) throws BusinessException {
        return processPayment(bookingId, paymentMethod, false);
    }

    /**
     * Bí danh thanh toán đơn đặt vé có cờ giả lập thất bại (pay).
     */
    public Payment pay(int bookingId, PaymentMethod paymentMethod, boolean simulateFail) throws BusinessException {
        return processPayment(bookingId, paymentMethod, simulateFail);
    }

    /**
     * Bí danh thanh toán theo vé lẻ (payTicket).
     */
    public Payment payTicket(int ticketId, PaymentMethod paymentMethod) throws BusinessException {
        return processPaymentByTicketId(ticketId, paymentMethod, false);
    }

    /**
     * Bí danh thanh toán theo vé lẻ có cờ giả lập thất bại (payTicket).
     */
    public Payment payTicket(int ticketId, PaymentMethod paymentMethod, boolean simulateFail) throws BusinessException {
        return processPaymentByTicketId(ticketId, paymentMethod, simulateFail);
    }

    /**
     * Hủy đơn đặt vé theo mã vé lẻ (giải phóng ghế và cập nhật DA_HUY).
     */
    public boolean cancelBookingByTicketId(int ticketId) throws BusinessException {
        if (ticketId <= 0) {
            throw new BusinessException(BusinessException.INVALID_INPUT, "Mã vé phải lớn hơn 0.");
        }
        Ticket ticket = ticketRepository.findById(ticketId);
        if (ticket == null) {
            throw new BusinessException(BusinessException.INVALID_INPUT, "Không tìm thấy vé với ID: " + ticketId);
        }
        return cancelBooking(ticket.getBookingId());
    }

    /**
     * Hủy đơn đặt vé chưa thanh toán và giải phóng ghế ngồi về trạng thái CON_TRONG.
     * Sử dụng khi khách hàng muốn hủy đặt vé hoặc thanh toán không thành công và muốn từ bỏ đơn.
     *
     * @param bookingId mã đơn đặt vé cần hủy
     * @return true nếu hủy thành công
     * @throws BusinessException nếu đơn hàng không tồn tại hoặc đã thanh toán
     */
    public boolean cancelBooking(int bookingId) throws BusinessException {
        Booking booking = ticketRepository.findBookingById(bookingId);
        if (booking == null) {
            throw new BusinessException(BusinessException.BOOKING_NOT_FOUND,
                    "Không tìm thấy đơn đặt vé với mã ID: " + bookingId);
        }

        if (booking.isPaid()) {
            throw new BusinessException(BusinessException.BOOKING_ALREADY_CANCELLED,
                    "Đơn đặt vé đã thanh toán thành công, không thể tự ý hủy tại đây (cần chính sách hoàn vé).");
        }

        if (booking.isCancelled()) {
            throw new BusinessException(BusinessException.BOOKING_ALREADY_CANCELLED,
                    "Đơn đặt vé này đã ở trạng thái ĐÃ HỦY trước đó.");
        }

        // Cập nhật trạng thái đơn thành DA_HUY trong MySQL
        boolean statusUpdated = ticketRepository.updatePaymentStatus(bookingId, PaymentStatus.DA_HUY);

        // Giải phóng toàn bộ ghế thuộc đơn đặt này về trạng thái CON_TRONG (AVAILABLE)
        List<Ticket> tickets = booking.getTickets();
        if (tickets != null) {
            for (Ticket ticket : tickets) {
                if (ticket.getSeat() != null) {
                    showtimeRepository.updateSeatStatus(
                            booking.getShowtimeId(),
                            ticket.getSeat().getSeatId(),
                            SeatStatus.CON_TRONG
                    );
                }
            }
        }

        System.out.println("[PaymentService] Đã hủy đơn đặt vé: " + booking.getBookingCode() + " và hoàn trả toàn bộ ghế về CÒN TRỐNG.");
        return statusUpdated;
    }

    /**
     * Xuất hóa đơn và vé ra file .txt bằng TicketFileExporter (File IO).
     */
    private void exportTicketReceipt(Booking booking, Payment payment, String methodName) {
        try {
            String customerName = (booking.getCustomer() != null) ? booking.getCustomer().getFullName() : "N/A";
            String customerType = (booking.getCustomer() != null) ? booking.getCustomer().getCustomerType().getDisplayName() : "N/A";

            String movieTitle = "N/A";
            String roomName = "N/A";
            String showDate = "N/A";
            String showTime = "N/A";

            if (booking.getShowtime() != null) {
                showDate = booking.getShowtime().getShowDateFormatted();
                showTime = booking.getShowtime().getStartTimeFormatted() + " - " + booking.getShowtime().getEndTimeFormatted();
                if (booking.getShowtime().getMovie() != null) {
                    movieTitle = booking.getShowtime().getMovie().getTitle();
                }
                if (booking.getShowtime().getRoom() != null) {
                    roomName = booking.getShowtime().getRoom().getRoomName();
                }
            }

            List<Ticket> tickets = booking.getTickets();
            int n = (tickets != null) ? tickets.size() : 0;
            String[] ticketCodes = new String[n];
            String[] seatInfos = new String[n];
            double[] seatPrices = new double[n];

            if (tickets != null) {
                for (int i = 0; i < n; i++) {
                    Ticket t = tickets.get(i);
                    ticketCodes[i] = t.getTicketCode();
                    seatInfos[i] = (t.getSeat() != null) ? t.getSeat().getDisplayInfo() : "Ghế #" + t.getShowtimeSeatId();
                    seatPrices[i] = t.getSeatPrice();

                    // Xuất thêm từng vé lẻ
                    try {
                        TicketFileExporter.exportTicket(
                                t.getTicketCode(),
                                booking.getBookingCode(),
                                customerName,
                                customerType,
                                movieTitle,
                                roomName,
                                seatInfos[i],
                                showDate,
                                showTime,
                                t.getSeatPrice(),
                                methodName,
                                payment.getPaymentCode()
                        );
                    } catch (IOException e) {
                        System.err.println("[PaymentService] Lỗi khi xuất file vé lẻ " + t.getTicketCode() + ": " + e.getMessage());
                    }
                }
            }

            // Xuất hóa đơn tổng hợp cho cả đơn đặt
            String invoicePath = TicketFileExporter.exportBookingTickets(
                    booking.getBookingCode(),
                    customerName,
                    customerType,
                    movieTitle,
                    roomName,
                    showDate,
                    showTime,
                    ticketCodes,
                    seatInfos,
                    seatPrices,
                    booking.getTotalAmount(),
                    booking.getDiscountAmount(),
                    booking.getFinalAmount(),
                    methodName,
                    payment.getPaymentCode()
            );

            System.out.println("[PaymentService] Đã xuất hóa đơn đặt vé ra file: " + invoicePath);

        } catch (Exception e) {
            System.err.println("[PaymentService] Lỗi khi xuất file vé: " + e.getMessage());
        }
    }
}

