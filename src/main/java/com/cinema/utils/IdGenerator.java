package com.cinema.utils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Lớp tiện ích sinh mã tự động cho các đối tượng trong hệ thống.
 * Định dạng mã: PREFIX-yyyyMMdd-XXXX
 *
 * Ví dụ:
 * - Mã vé:        TK-20261105-0001
 * - Mã đơn hàng:  BK-20261105-0001
 * - Mã thanh toán: PM-20261105-0001
 */
public class IdGenerator {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    // Bộ đếm tuần tự cho từng prefix, dùng AtomicInteger đảm bảo an toàn luồng
    private static final AtomicInteger ticketCounter = new AtomicInteger(0);
    private static final AtomicInteger bookingCounter = new AtomicInteger(0);
    private static final AtomicInteger paymentCounter = new AtomicInteger(0);

    // Lưu ngày hiện tại để reset counter khi sang ngày mới
    private static String lastDateForTicket = "";
    private static String lastDateForBooking = "";
    private static String lastDateForPayment = "";

    // Constructor private để ngăn tạo instance
    private IdGenerator() {
    }

    /**
     * Sinh mã vé tự động.
     * Định dạng: TK-yyyyMMdd-XXXX
     *
     * @return mã vé mới (ví dụ: TK-20261105-0001)
     */
    public static String generateTicketCode() {
        return generateCode("TK", ticketCounter, lastDateForTicket, newDate -> lastDateForTicket = newDate);
    }

    /**
     * Sinh mã đơn đặt vé (booking) tự động.
     * Định dạng: BK-yyyyMMdd-XXXX
     *
     * @return mã đơn hàng mới (ví dụ: BK-20261105-0001)
     */
    public static String generateBookingCode() {
        return generateCode("BK", bookingCounter, lastDateForBooking, newDate -> lastDateForBooking = newDate);
    }

    /**
     * Sinh mã thanh toán tự động.
     * Định dạng: PM-yyyyMMdd-XXXX
     *
     * @return mã thanh toán mới (ví dụ: PM-20261105-0001)
     */
    public static String generatePaymentCode() {
        return generateCode("PM", paymentCounter, lastDateForPayment, newDate -> lastDateForPayment = newDate);
    }

    /**
     * Phương thức nội bộ sinh mã với prefix tùy ý.
     *
     * @param prefix      tiền tố (TK, BK, PM)
     * @param counter     bộ đếm tương ứng
     * @param lastDate    ngày gần nhất đã sinh mã
     * @param dateSetter  callback để cập nhật lastDate
     * @return mã mới theo định dạng PREFIX-yyyyMMdd-XXXX
     */
    private static synchronized String generateCode(String prefix, AtomicInteger counter,
                                                     String lastDate,
                                                     java.util.function.Consumer<String> dateSetter) {
        String today = LocalDate.now().format(DATE_FORMAT);

        // Nếu sang ngày mới → reset bộ đếm về 0
        if (!today.equals(lastDate)) {
            counter.set(0);
            dateSetter.accept(today);
        }

        int sequence = counter.incrementAndGet();
        return String.format("%s-%s-%04d", prefix, today, sequence);
    }

    /**
     * Sinh mã tùy chỉnh với prefix và số thứ tự cho trước.
     * Dùng cho trường hợp cần tạo mã cụ thể (ví dụ: import dữ liệu).
     *
     * @param prefix   tiền tố
     * @param date     ngày cần gắn vào mã
     * @param sequence số thứ tự
     * @return mã theo định dạng PREFIX-yyyyMMdd-XXXX
     */
    public static String generateCustomCode(String prefix, LocalDate date, int sequence) {
        String dateStr = date.format(DATE_FORMAT);
        return String.format("%s-%s-%04d", prefix, dateStr, sequence);
    }

    /**
     * Đặt lại bộ đếm ban đầu cho tất cả loại mã.
     * Hữu ích khi khởi động ứng dụng, cần đồng bộ với dữ liệu đã có trong DB.
     *
     * @param lastTicketSeq  số thứ tự vé cuối cùng trong DB
     * @param lastBookingSeq số thứ tự booking cuối cùng trong DB
     * @param lastPaymentSeq số thứ tự payment cuối cùng trong DB
     */
    public static void initializeCounters(int lastTicketSeq, int lastBookingSeq, int lastPaymentSeq) {
        String today = LocalDate.now().format(DATE_FORMAT);

        ticketCounter.set(lastTicketSeq);
        lastDateForTicket = today;

        bookingCounter.set(lastBookingSeq);
        lastDateForBooking = today;

        paymentCounter.set(lastPaymentSeq);
        lastDateForPayment = today;

        System.out.println("[IdGenerator] Đã khởi tạo bộ đếm:");
        System.out.println("  - Ticket counter: " + lastTicketSeq);
        System.out.println("  - Booking counter: " + lastBookingSeq);
        System.out.println("  - Payment counter: " + lastPaymentSeq);
    }
}

