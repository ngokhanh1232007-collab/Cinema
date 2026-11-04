package com.cinema.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Lớp tiện ích ghi thông tin vé đã thanh toán ra file .txt.
 * Đáp ứng yêu cầu kỹ thuật File IO (ghi file) và lưu trữ vé.
 *
 * Các file vé được lưu trong thư mục "exported_tickets/" tại thư mục chạy chương trình.
 */
public class TicketFileExporter {

    private static final String EXPORT_DIRECTORY = "exported_tickets";
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final DateTimeFormatter FILE_NAME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final String SEPARATOR_LINE = "============================================================";
    private static final String THIN_LINE =      "------------------------------------------------------------";

    // Constructor private để ngăn tạo instance
    private TicketFileExporter() {
    }

    /**
     * Ghi thông tin một vé đã thanh toán ra file .txt.
     *
     * @param ticketCode    mã vé (ví dụ: TK-20261105-0001)
     * @param bookingCode   mã đơn hàng (ví dụ: BK-20261105-0001)
     * @param customerName  tên khách hàng
     * @param customerType  loại khách hàng (REGULAR / STUDENT / VIP)
     * @param movieTitle    tên phim
     * @param roomName      tên phòng chiếu
     * @param seatInfo      thông tin ghế (ví dụ: "A3 - Ghế thường")
     * @param showDate      ngày chiếu
     * @param showTime      giờ chiếu
     * @param seatPrice     giá vé
     * @param paymentMethod phương thức thanh toán
     * @param paymentCode   mã thanh toán
     * @return đường dẫn tuyệt đối tới file đã xuất
     * @throws IOException nếu ghi file thất bại
     */
    public static String exportTicket(String ticketCode, String bookingCode,
                                       String customerName, String customerType,
                                       String movieTitle, String roomName,
                                       String seatInfo, String showDate,
                                       String showTime, double seatPrice,
                                       String paymentMethod, String paymentCode) throws IOException {

        // Tạo thư mục exported_tickets nếu chưa tồn tại
        File directory = new File(EXPORT_DIRECTORY);
        if (!directory.exists()) {
            boolean created = directory.mkdirs();
            if (created) {
                System.out.println("[TicketFileExporter] Đã tạo thư mục: " + directory.getAbsolutePath());
            }
        }

        // Tạo tên file: VE_<ticketCode>_<timestamp>.txt
        String timestamp = LocalDateTime.now().format(FILE_NAME_FORMAT);
        String fileName = "VE_" + ticketCode.replace("-", "") + "_" + timestamp + ".txt";
        File outputFile = new File(directory, fileName);

        // Ghi nội dung vé ra file bằng BufferedWriter (File IO)
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            writer.write(SEPARATOR_LINE);
            writer.newLine();
            writer.write("           VÉ XEM PHIM - CINEMA BOOKING SYSTEM");
            writer.newLine();
            writer.write("                    Nhóm 06 - OOP");
            writer.newLine();
            writer.write(SEPARATOR_LINE);
            writer.newLine();
            writer.newLine();

            writer.write("  Mã vé:              " + ticketCode);
            writer.newLine();
            writer.write("  Mã đơn hàng:        " + bookingCode);
            writer.newLine();
            writer.write("  Mã thanh toán:       " + paymentCode);
            writer.newLine();
            writer.newLine();

            writer.write(THIN_LINE);
            writer.newLine();
            writer.write("  THÔNG TIN KHÁCH HÀNG");
            writer.newLine();
            writer.write(THIN_LINE);
            writer.newLine();
            writer.write("  Họ tên:              " + customerName);
            writer.newLine();
            writer.write("  Loại khách hàng:     " + formatCustomerType(customerType));
            writer.newLine();
            writer.newLine();

            writer.write(THIN_LINE);
            writer.newLine();
            writer.write("  THÔNG TIN SUẤT CHIẾU");
            writer.newLine();
            writer.write(THIN_LINE);
            writer.newLine();
            writer.write("  Phim:                " + movieTitle);
            writer.newLine();
            writer.write("  Phòng chiếu:         " + roomName);
            writer.newLine();
            writer.write("  Ghế:                 " + seatInfo);
            writer.newLine();
            writer.write("  Ngày chiếu:          " + showDate);
            writer.newLine();
            writer.write("  Giờ chiếu:           " + showTime);
            writer.newLine();
            writer.newLine();

            writer.write(THIN_LINE);
            writer.newLine();
            writer.write("  THANH TOÁN");
            writer.newLine();
            writer.write(THIN_LINE);
            writer.newLine();
            writer.write("  Giá vé:              " + String.format("%,.0f VND", seatPrice));
            writer.newLine();
            writer.write("  Phương thức:         " + formatPaymentMethod(paymentMethod));
            writer.newLine();
            writer.newLine();

            writer.write(THIN_LINE);
            writer.newLine();
            writer.write("  Thời gian xuất vé:   " + LocalDateTime.now().format(DATE_TIME_FORMAT));
            writer.newLine();
            writer.write(SEPARATOR_LINE);
            writer.newLine();
            writer.write("  Cảm ơn quý khách đã sử dụng dịch vụ!");
            writer.newLine();
            writer.write("  Chúc quý khách xem phim vui vẻ!");
            writer.newLine();
            writer.write(SEPARATOR_LINE);
            writer.newLine();

            writer.flush();
        }

        String absolutePath = outputFile.getAbsolutePath();
        System.out.println("[TicketFileExporter] Đã xuất vé thành công: " + absolutePath);
        return absolutePath;
    }

    /**
     * Ghi thông tin nhiều vé của một đơn đặt (booking) ra một file .txt duy nhất.
     *
     * @param bookingCode   mã đơn hàng
     * @param customerName  tên khách hàng
     * @param customerType  loại khách hàng
     * @param movieTitle    tên phim
     * @param roomName      tên phòng chiếu
     * @param showDate      ngày chiếu
     * @param showTime      giờ chiếu
     * @param ticketCodes   mảng mã vé
     * @param seatInfos     mảng thông tin ghế tương ứng
     * @param seatPrices    mảng giá vé tương ứng
     * @param totalAmount   tổng tiền gốc
     * @param discountAmount số tiền giảm giá
     * @param finalAmount   thành tiền sau giảm
     * @param paymentMethod phương thức thanh toán
     * @param paymentCode   mã thanh toán
     * @return đường dẫn tuyệt đối tới file đã xuất
     * @throws IOException nếu ghi file thất bại
     */
    public static String exportBookingTickets(String bookingCode, String customerName,
                                               String customerType, String movieTitle,
                                               String roomName, String showDate,
                                               String showTime, String[] ticketCodes,
                                               String[] seatInfos, double[] seatPrices,
                                               double totalAmount, double discountAmount,
                                               double finalAmount, String paymentMethod,
                                               String paymentCode) throws IOException {

        // Tạo thư mục exported_tickets nếu chưa tồn tại
        File directory = new File(EXPORT_DIRECTORY);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        // Tạo tên file: DONHANG_<bookingCode>_<timestamp>.txt
        String timestamp = LocalDateTime.now().format(FILE_NAME_FORMAT);
        String fileName = "DONHANG_" + bookingCode.replace("-", "") + "_" + timestamp + ".txt";
        File outputFile = new File(directory, fileName);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            writer.write(SEPARATOR_LINE);
            writer.newLine();
            writer.write("        HÓA ĐƠN ĐẶT VÉ - CINEMA BOOKING SYSTEM");
            writer.newLine();
            writer.write("                    Nhóm 06 - OOP");
            writer.newLine();
            writer.write(SEPARATOR_LINE);
            writer.newLine();
            writer.newLine();

            writer.write("  Mã đơn hàng:        " + bookingCode);
            writer.newLine();
            writer.write("  Mã thanh toán:       " + paymentCode);
            writer.newLine();
            writer.newLine();

            writer.write(THIN_LINE);
            writer.newLine();
            writer.write("  THÔNG TIN KHÁCH HÀNG");
            writer.newLine();
            writer.write(THIN_LINE);
            writer.newLine();
            writer.write("  Họ tên:              " + customerName);
            writer.newLine();
            writer.write("  Loại khách hàng:     " + formatCustomerType(customerType));
            writer.newLine();
            writer.newLine();

            writer.write(THIN_LINE);
            writer.newLine();
            writer.write("  THÔNG TIN SUẤT CHIẾU");
            writer.newLine();
            writer.write(THIN_LINE);
            writer.newLine();
            writer.write("  Phim:                " + movieTitle);
            writer.newLine();
            writer.write("  Phòng chiếu:         " + roomName);
            writer.newLine();
            writer.write("  Ngày chiếu:          " + showDate);
            writer.newLine();
            writer.write("  Giờ chiếu:           " + showTime);
            writer.newLine();
            writer.newLine();

            writer.write(THIN_LINE);
            writer.newLine();
            writer.write("  DANH SÁCH VÉ");
            writer.newLine();
            writer.write(THIN_LINE);
            writer.newLine();

            for (int i = 0; i < ticketCodes.length; i++) {
                writer.write("  " + (i + 1) + ". Mã vé: " + ticketCodes[i]);
                writer.newLine();
                writer.write("     Ghế: " + seatInfos[i]);
                writer.newLine();
                writer.write("     Giá: " + String.format("%,.0f VND", seatPrices[i]));
                writer.newLine();
                writer.newLine();
            }

            writer.write(THIN_LINE);
            writer.newLine();
            writer.write("  TỔNG KẾT THANH TOÁN");
            writer.newLine();
            writer.write(THIN_LINE);
            writer.newLine();
            writer.write("  Tổng tiền gốc:      " + String.format("%,.0f VND", totalAmount));
            writer.newLine();
            writer.write("  Giảm giá:            " + String.format("%,.0f VND", discountAmount));
            writer.newLine();
            writer.write("  Thành tiền:          " + String.format("%,.0f VND", finalAmount));
            writer.newLine();
            writer.write("  Phương thức:         " + formatPaymentMethod(paymentMethod));
            writer.newLine();
            writer.newLine();

            writer.write(THIN_LINE);
            writer.newLine();
            writer.write("  Thời gian xuất hóa đơn: " + LocalDateTime.now().format(DATE_TIME_FORMAT));
            writer.newLine();
            writer.write(SEPARATOR_LINE);
            writer.newLine();
            writer.write("  Cảm ơn quý khách đã sử dụng dịch vụ!");
            writer.newLine();
            writer.write("  Chúc quý khách xem phim vui vẻ!");
            writer.newLine();
            writer.write(SEPARATOR_LINE);
            writer.newLine();

            writer.flush();
        }

        String absolutePath = outputFile.getAbsolutePath();
        System.out.println("[TicketFileExporter] Đã xuất hóa đơn thành công: " + absolutePath);
        return absolutePath;
    }

    /**
     * Chuyển mã loại khách hàng sang tiếng Việt hiển thị thân thiện.
     */
    private static String formatCustomerType(String type) {
        if (type == null) return "Không xác định";
        switch (type.toUpperCase()) {
            case "REGULAR":  return "Khách thường";
            case "STUDENT":  return "Sinh viên";
            case "VIP":      return "Khách VIP";
            default:         return type;
        }
    }

    /**
     * Chuyển mã phương thức thanh toán sang tiếng Việt hiển thị thân thiện.
     */
    private static String formatPaymentMethod(String method) {
        if (method == null) return "Không xác định";
        switch (method.toUpperCase()) {
            case "CASH":           return "Tiền mặt";
            case "CREDIT_CARD":    return "Thẻ tín dụng";
            case "E_WALLET":       return "Ví điện tử";
            case "BANK_TRANSFER":  return "Chuyển khoản ngân hàng";
            default:               return method;
        }
    }
}

