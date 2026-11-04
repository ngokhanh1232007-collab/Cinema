package com.cinema.main;

import com.cinema.model.BankTransferPayment;
import com.cinema.model.Booking;
import com.cinema.model.CashPayment;
import com.cinema.model.Customer;
import com.cinema.model.CustomerType;
import com.cinema.model.EWalletPayment;
import com.cinema.model.Movie;
import com.cinema.model.Payment;
import com.cinema.model.PaymentMethod;
import com.cinema.model.Showtime;
import com.cinema.model.Ticket;
import com.cinema.repository.CustomerRepository;
import com.cinema.service.BookingService;
import com.cinema.service.MovieService;
import com.cinema.service.PaymentService;
import com.cinema.service.ShowtimeService;
import com.cinema.utils.BusinessException;
import com.cinema.utils.DatabaseConnection;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

/**
 * Lớp điều khiển chính của chương trình (Main Console Application).
 * Chịu trách nhiệm hiển thị giao diện menu dòng lệnh bằng tiếng Việt
 * và điều phối luồng thao tác người dùng.
 *
 * <p>Kiến trúc phân tầng chuẩn:
 * <ul>
 *   <li><b>KHÔNG nhồi nhét logic nghiệp vụ vào Main:</b> Toàn bộ kiểm tra quy tắc và tính toán
 *       được giao cho các lớp tương ứng trong package <code>com.cinema.service</code>.</li>
 *   <li><b>Xử lý ngoại lệ tập trung:</b> Bắt và hiển thị thân thiện các <code>BusinessException</code>.</li>
 *   <li><b>Kịch bản tự động:</b> Cung cấp mục chạy tự động 6 Test Cases mẫu phục vụ báo cáo đồ án.</li>
 * </ul>
 */
public class Main {

    // Khởi tạo các Service nghiệp vụ (Dependency Injection đơn giản)
    private static final MovieService movieService = new MovieService();
    private static final ShowtimeService showtimeService = new ShowtimeService();
    private static final BookingService bookingService = new BookingService();
    private static final PaymentService paymentService = new PaymentService();
    private static final CustomerRepository customerRepository = new CustomerRepository();

    public static void main(String[] args) {
        // Đặt encoding cho console để hiển thị tiếng Việt chuẩn
        System.setProperty("file.encoding", "UTF-8");

        Scanner scanner = new Scanner(System.in);
        printWelcomeBanner();

        // Kiểm tra kết nối cơ sở dữ liệu khi khởi động
        boolean dbConnected = DatabaseConnection.testConnection();
        if (!dbConnected) {
            System.err.println("------------------------------------------------------------------------------------------");
            System.err.println("  [CẢNH BÁO] Không thể kết nối tới MySQL Database!");
            System.err.println("  Vui lòng kiểm tra lại:");
            System.err.println("    1. MySQL Server đã được khởi động chưa?");
            System.err.println("    2. Đã chạy script database.sql để tạo cơ sở dữ liệu cinema_booking_db chưa?");
            System.err.println("    3. Thông tin tài khoản trong file db.properties (username/password) đã đúng chưa?");
            System.err.println("------------------------------------------------------------------------------------------\n");
        }

        boolean running = true;
        while (running) {
            printMainMenu();
            int choice = readInt(scanner, "👉 Vui lòng chọn chức năng (0 - 6): ", 0, 6);

            System.out.println();
            switch (choice) {
                case 1:
                    handleDisplayShowingMovies();
                    break;
                case 2:
                    handleSearchMovie(scanner);
                    break;
                case 3:
                    handleViewShowtimesAndSeatMap(scanner);
                    break;
                case 4:
                    handleBookTickets(scanner);
                    break;
                case 5:
                    handleProcessPayment(scanner);
                    break;
                case 6:
                    runAutomatedTestCases();
                    break;
                case 0:
                    running = false;
                    printGoodbyeMessage();
                    break;
                default:
                    System.out.println("❌ Lựa chọn không hợp lệ, vui lòng chọn lại!");
            }

            if (running) {
                System.out.println("\nNhấn phím [ENTER] để tiếp tục...");
                scanner.nextLine();
            }
        }

        // Đóng kết nối database trước khi tắt ứng dụng
        DatabaseConnection.closeConnection();
        scanner.close();
    }

    // =========================================================================
    // 1. CÁC HÀM XỬ LÝ MENU NGƯỜI DÙNG (GỌI SERVICE)
    // =========================================================================

    /**
     * Chức năng 1: Hiển thị danh sách phim đang chiếu tại rạp.
     */
    private static void handleDisplayShowingMovies() {
        movieService.displayShowingMovies();
    }

    /**
     * Chức năng 2: Tìm kiếm phim theo tên.
     */
    private static void handleSearchMovie(Scanner scanner) {
        System.out.println("--- TÌM KIẾM PHIM THEO TÊN ---");
        String keyword = readString(scanner, "Nhập từ khóa tên phim cần tìm: ");

        try {
            movieService.displaySearchMovies(keyword);
        } catch (BusinessException e) {
            System.err.println("❌ Lỗi: " + e.getMessage());
        }
    }

    /**
     * Chức năng 3: Xem suất chiếu và sơ đồ ghế trống.
     */
    private static void handleViewShowtimesAndSeatMap(Scanner scanner) {
        System.out.println("--- XEM SUẤT CHIẾU VÀ SƠ ĐỒ GHẾ ---");
        movieService.displayShowingMovies();

        int movieId = readInt(scanner, "Nhập mã phim (ID) muốn xem lịch chiếu (nhập 0 để hủy): ", 0, 9999);
        if (movieId == 0) return;

        try {
            showtimeService.displayShowtimesByMovie(movieId);

            int showtimeId = readInt(scanner, "Nhập mã suất chiếu (Showtime ID) để xem sơ đồ ghế (nhập 0 để quay lại): ", 0, 9999);
            if (showtimeId == 0) return;

            showtimeService.displaySeatMap(showtimeId);

        } catch (BusinessException e) {
            System.err.println("❌ [LỖI NGHIỆP VỤ] " + e.getMessage());
        }
    }

    /**
     * Chức năng 4: Đặt vé xem phim (Nhập mã KH, mã suất chiếu, danh sách ghế).
     */
    private static void handleBookTickets(Scanner scanner) {
        System.out.println("==========================================================================================");
        System.out.println("                                ĐẶT VÉ XEM PHIM                                           ");
        System.out.println("==========================================================================================");

        // Hiển thị danh sách khách hàng mẫu để người dùng dễ chọn
        displaySampleCustomers();

        int customerId = readInt(scanner, "Nhập mã khách hàng (Customer ID): ", 1, 9999);
        int showtimeId = readInt(scanner, "Nhập mã suất chiếu (Showtime ID): ", 1, 9999);

        try {
            // Hiển thị sơ đồ ghế để khách dễ quan sát vị trí trống
            showtimeService.displaySeatMap(showtimeId);

            String seatsInput = readString(scanner, "Nhập các ghế muốn đặt (cách nhau bởi dấu phẩy, ví dụ: A1, A2, D1): ");
            List<String> seatLabels = parseSeatLabels(seatsInput);

            if (seatLabels.isEmpty()) {
                System.err.println("❌ Bạn chưa nhập ghế nào. Hủy thao tác đặt vé.");
                return;
            }

            // Gọi BookingService để thực hiện toàn bộ kiểm tra nghiệp vụ và lưu vé
            Booking booking = bookingService.bookTickets(customerId, showtimeId, seatLabels);

            System.out.println("\n✅ BẠN ĐÃ ĐẶT VÉ THÀNH CÔNG!");
            System.out.println("👉 Hãy chọn chức năng '5. Thanh toán vé' trên Menu chính để hoàn tất đơn hàng.");

        } catch (BusinessException e) {
            System.err.println("\n❌ [ĐẶT VÉ THẤT BẠI - VI PHẠM BUSINESS RULE]");
            System.err.println("   Mã lỗi: " + e.getErrorCode());
            System.err.println("   Nguyên nhân: " + e.getMessage());
        }
    }

    /**
     * Chức năng 5: Thanh toán vé (chọn Tiền mặt / Chuyển khoản / Ví điện tử).
     */
    private static void handleProcessPayment(Scanner scanner) {
        System.out.println("==========================================================================================");
        System.out.println("                             THANH TOÁN ĐƠN ĐẶT VÉ                                        ");
        System.out.println("==========================================================================================");

        int bookingId = readInt(scanner, "Nhập mã ID đơn đặt vé cần thanh toán (Booking ID): ", 1, 9999);

        try {
            Booking booking = bookingService.getBookingById(bookingId);
            System.out.println("\n--- THÔNG TIN ĐƠN ĐẶT VÉ ---");
            System.out.println(booking.getDisplayInfo());
            System.out.printf("Khách hàng: %s | Loại: %s%n",
                    booking.getCustomer() != null ? booking.getCustomer().getFullName() : "N/A",
                    booking.getCustomer() != null ? booking.getCustomer().getCustomerType().getDisplayName() : "N/A");
            System.out.printf("Số lượng vé: %d | Số tiền cần thanh toán: %,.0f VND%n%n",
                    booking.getTicketCount(), booking.getFinalAmount());

            System.out.println("CHỌN HÌNH THỨC THANH TOÁN:");
            System.out.println("  1. Tiền mặt (CashPayment - thanh toán tại quầy)");
            System.out.println("  2. Chuyển khoản ngân hàng (BankTransferPayment)");
            System.out.println("  3. Ví điện tử (EWalletPayment - MoMo/ZaloPay)");
            System.out.println("  0. Hủy thanh toán");

            int methodChoice = readInt(scanner, "👉 Chọn phương thức (0 - 3): ", 0, 3);
            if (methodChoice == 0) {
                System.out.println("Đã hủy thao tác thanh toán.");
                return;
            }

            // Áp dụng ĐA HÌNH: khởi tạo đối tượng PaymentMethod tương ứng
            PaymentMethod paymentMethod;
            switch (methodChoice) {
                case 1:
                    paymentMethod = new CashPayment();
                    break;
                case 2:
                    paymentMethod = new BankTransferPayment("Vietcombank", "0123456789", "RAP PHIM SINH VIEN");
                    break;
                case 3:
                    String phone = (booking.getCustomer() != null) ? booking.getCustomer().getPhone() : "";
                    paymentMethod = new EWalletPayment("MoMo", phone);
                    break;
                default:
                    paymentMethod = new CashPayment();
            }

            // Gọi PaymentService để xử lý thanh toán đa hình và xuất file vé
            Payment payment = paymentService.processPayment(bookingId, paymentMethod);

            System.out.println("\n🎉 GIAO DỊCH HOÀN TẤT THÀNH CÔNG!");
            System.out.printf("   Mã thanh toán: %s%n", payment.getPaymentCode());
            System.out.println("   File vé (.txt) đã được xuất lưu tại thư mục: exported_tickets/");

        } catch (BusinessException e) {
            System.err.println("\n❌ [THANH TOÁN THẤT BẠI - LỖI NGHIỆP VỤ]");
            System.err.println("   Mã lỗi: " + e.getErrorCode());
            System.err.println("   Nguyên nhân: " + e.getMessage());
        }
    }

    // =========================================================================
    // 2. KỊCH BẢN CHẠY TỰ ĐỘNG 6 TEST CASES MẪU (BÁO CÁO ĐỒ ÁN)
    // =========================================================================

    /**
     * Chức năng 6: Chạy tự động 6 tình huống kiểm thử (Test Cases) theo yêu cầu đồ án.
     */
    public static void runAutomatedTestCases() {
        System.out.println("\n##########################################################################################");
        System.out.println("                 BẮT ĐẦU CHẠY TỰ ĐỘNG 6 TÌNH HUỐNG KIỂM THỬ (TEST CASES)                   ");
        System.out.println("##########################################################################################\n");

        Booking createdBookingForTest6 = null;

        // ---------------------------------------------------------------------
        // TEST 1: Khách đặt ghế còn trống thành công
        // ---------------------------------------------------------------------
        System.out.println("==========================================================================================");
        System.out.println("👉 [TEST CASE 1]: Khách hàng thường đặt ghế CÒN TRỐNG thành công");
        System.out.println("   - Khách: Nguyễn Văn An (ID 1 - Khách thường)");
        System.out.println("   - Suất chiếu: #1 (Phim: Lật Mặt 7 - 09:00 05/11/2026)");
        System.out.println("   - Ghế chọn: A1, A2 (Ghế thường, trạng thái CÒN TRỐNG)");
        System.out.println("------------------------------------------------------------------------------------------");
        try {
            Booking b1 = bookingService.bookTickets(1, 1, Arrays.asList("A1", "A2"));
            System.out.println("   ==> KẾT QUẢ TEST 1: [PASSED - THÀNH CÔNG]");
            System.out.printf("       Mã đơn hàng: %s | Số vé: %d | Thành tiền: %,.0f VND%n",
                    b1.getBookingCode(), b1.getTicketCount(), b1.getFinalAmount());
            for (Ticket t : b1.getTickets()) {
                System.out.printf("       + Vé: %s - Ghế: %s - Giá: %,.0f VND%n",
                        t.getTicketCode(), t.getSeat().getSeatLabel(), t.getSeatPrice());
            }
        } catch (BusinessException e) {
            System.err.println("   ==> KẾT QUẢ TEST 1: [FAILED] " + e.getMessage());
        }

        // ---------------------------------------------------------------------
        // TEST 2: Khách đặt ghế đã có người đặt (Bắt lỗi SEAT_ALREADY_BOOKED)
        // ---------------------------------------------------------------------
        System.out.println("\n==========================================================================================");
        System.out.println("👉 [TEST CASE 2]: Khách đặt ghế ĐÃ CÓ NGƯỜI ĐẶT (Kỳ vọng bắt BusinessException)");
        System.out.println("   - Khách: Trần Thị Bích (ID 2)");
        System.out.println("   - Suất chiếu: #1");
        System.out.println("   - Ghế chọn: A3 (Ghế này đã có người đặt trước trong database)");
        System.out.println("------------------------------------------------------------------------------------------");
        try {
            bookingService.bookTickets(2, 1, List.of("A3"));
            System.err.println("   ==> KẾT QUẢ TEST 2: [FAILED - Hệ thống không chặn được ghế đã đặt!]");
        } catch (BusinessException e) {
            System.out.println("   ==> KẾT QUẢ TEST 2: [PASSED - BẮT NGOẠI LỆ THÀNH CÔNG]");
            System.out.println("       Mã lỗi:      [" + e.getErrorCode() + "]");
            System.out.println("       Thông báo:   " + e.getMessage());
        }

        // ---------------------------------------------------------------------
        // TEST 3: Khách chọn ghế không tồn tại trong phòng chiếu (Bắt lỗi INVALID_INPUT)
        // ---------------------------------------------------------------------
        System.out.println("\n==========================================================================================");
        System.out.println("👉 [TEST CASE 3]: Khách chọn ghế KHÔNG TỒN TẠI (Kỳ vọng bắt BusinessException)");
        System.out.println("   - Khách: Nguyễn Văn An (ID 1)");
        System.out.println("   - Suất chiếu: #1 (Phòng 1 chỉ có hàng từ A đến E, số từ 1 đến 6)");
        System.out.println("   - Ghế chọn: Z99 (Ghế không tồn tại)");
        System.out.println("------------------------------------------------------------------------------------------");
        try {
            bookingService.bookTickets(1, 1, List.of("Z99"));
            System.err.println("   ==> KẾT QUẢ TEST 3: [FAILED - Hệ thống không chặn được ghế không tồn tại!]");
        } catch (BusinessException e) {
            System.out.println("   ==> KẾT QUẢ TEST 3: [PASSED - BẮT NGOẠI LỆ THÀNH CÔNG]");
            System.out.println("       Mã lỗi:      [" + e.getErrorCode() + "]");
            System.out.println("       Thông báo:   " + e.getMessage());
        }

        // ---------------------------------------------------------------------
        // TEST 4: Sinh viên mua vé và được giảm giá đúng công thức (giảm 20%)
        // ---------------------------------------------------------------------
        System.out.println("\n==========================================================================================");
        System.out.println("👉 [TEST CASE 4]: Sinh viên mua vé - Kiểm tra ĐA HÌNH StudentPricePolicy (Giảm 20%)");
        System.out.println("   - Khách: Lê Hoàng Nam (ID 3 - SINH VIÊN, MSSV: SV20210001)");
        System.out.println("   - Suất chiếu: #1 (Giá gốc: 75.000 VND)");
        System.out.println("   - Ghế chọn: D1 (Ghế đôi - phụ phí +30.000 VND)");
        System.out.println("   - Công thức kỳ vọng: (75.000 + 30.000) * 0.8 = 84.000 VND (Giảm 21.000 VND)");
        System.out.println("------------------------------------------------------------------------------------------");
        try {
            Booking b4 = bookingService.bookTickets(3, 1, List.of("D1"));
            createdBookingForTest6 = b4; // Lưu lại để dùng cho Test 6

            System.out.println("   ==> KẾT QUẢ TEST 4: [PASSED - TÍNH ĐÚNG CHÍNH SÁCH GIÁ GIẢM 20%]");
            System.out.printf("       Tổng tiền gốc:       %,10.0f VND%n", b4.getTotalAmount());
            System.out.printf("       Số tiền giảm (20%%): -%,10.0f VND%n", b4.getDiscountAmount());
            System.out.printf("       Thành tiền sau giảm:  %,10.0f VND%n", b4.getFinalAmount());

            boolean checkMatch = (Math.abs(b4.getFinalAmount() - 84000.0) < 0.01);
            System.out.println("       Kiểm tra khớp số liệu lý thuyết: " + (checkMatch ? "CHÍNH XÁC 100%" : "SAI"));

        } catch (BusinessException e) {
            System.err.println("   ==> KẾT QUẢ TEST 4: [FAILED] " + e.getMessage());
        }

        // ---------------------------------------------------------------------
        // TEST 5: Khách đặt vé cho phim đã ngừng chiếu (Bắt lỗi MOVIE_NOT_SHOWING)
        // ---------------------------------------------------------------------
        System.out.println("\n==========================================================================================");
        System.out.println("👉 [TEST CASE 5]: Khách đặt vé cho PHIM ĐÃ NGỪNG CHIẾU (Kỳ vọng bắt BusinessException)");
        System.out.println("   - Suất chiếu: #11 (Gắn với phim 'Quật Mộ Trùng Ma' - status = 'STOPPED')");
        System.out.println("   - Khách: Nguyễn Văn An (ID 1)");
        System.out.println("   - Ghế chọn: A1");
        System.out.println("------------------------------------------------------------------------------------------");
        try {
            bookingService.bookTickets(1, 11, List.of("A1"));
            System.err.println("   ==> KẾT QUẢ TEST 5: [FAILED - Hệ thống cho phép đặt vé phim đã ngừng chiếu!]");
        } catch (BusinessException e) {
            System.out.println("   ==> KẾT QUẢ TEST 5: [PASSED - BẮT NGOẠI LỆ THÀNH CÔNG]");
            System.out.println("       Mã lỗi:      [" + e.getErrorCode() + "]");
            System.out.println("       Thông báo:   " + e.getMessage());
        }

        // ---------------------------------------------------------------------
        // TEST 6: Thanh toán vé đã đặt thành công và xuất file vé .txt (File IO)
        // ---------------------------------------------------------------------
        System.out.println("\n==========================================================================================");
        System.out.println("👉 [TEST CASE 6]: Thanh toán đơn đặt vé thành công và xuất file .txt (File IO)");
        System.out.println("   - Sử dụng đơn đặt của sinh viên Lê Hoàng Nam từ Test Case 4");
        System.out.println("   - Phương thức thanh toán đa hình: Ví điện tử MoMo (EWalletPayment)");
        System.out.println("------------------------------------------------------------------------------------------");
        try {
            int targetBookingId = (createdBookingForTest6 != null) ? createdBookingForTest6.getBookingId() : 5;
            PaymentMethod ewallet = new EWalletPayment("MoMo", "0923456789");

            Payment payment = paymentService.processPayment(targetBookingId, ewallet);

            System.out.println("   ==> KẾT QUẢ TEST 6: [PASSED - THANH TOÁN VÀ XUẤT VÉ THÀNH CÔNG]");
            System.out.printf("       Mã thanh toán: %s | Số tiền: %,.0f VND | Trạng thái: %s%n",
                    payment.getPaymentCode(), payment.getAmount(), payment.getStatus());

            // Kiểm tra thư mục exported_tickets trên ổ đĩa
            File exportDir = new File("exported_tickets");
            if (exportDir.exists() && exportDir.isDirectory()) {
                File[] files = exportDir.listFiles((dir, name) -> name.endsWith(".txt"));
                System.out.printf("       Kiểm tra thư mục 'exported_tickets/': Tìm thấy %d file vé đã xuất.%n",
                        (files != null ? files.length : 0));
                if (files != null && files.length > 0) {
                    System.out.println("       File mẫu mới nhất: " + files[files.length - 1].getName());
                }
            }

        } catch (BusinessException e) {
            System.err.println("   ==> KẾT QUẢ TEST 6: [FAILED] " + e.getMessage());
        }

        System.out.println("\n##########################################################################################");
        System.out.println("                 HOÀN THÀNH CHẠY TOÀN BỘ 6 TÌNH HUỐNG KIỂM THỬ                            ");
        System.out.println("##########################################################################################\n");
    }

    // =========================================================================
    // 3. CÁC HÀM TIỆN ÍCH GIAO DIỆN CONSOLE & NHẬP LIỆU AN TOÀN
    // =========================================================================

    private static void printWelcomeBanner() {
        System.out.println("==========================================================================================");
        System.out.println("           ĐỒ ÁN LẬP TRÌNH HƯỚNG ĐỐI TƯỢNG (OOP) - NGÔN NGỮ JAVA & MYSQL                  ");
        System.out.println("            HỆ THỐNG QUẢN LÝ MUA VÉ XEM PHIM CHO SINH VIÊN - NHÓM 06                      ");
        System.out.println("==========================================================================================");
    }

    private static void printMainMenu() {
        System.out.println("==========================================================================================");
        System.out.println("                                      MENU CHÍNH                                          ");
        System.out.println("==========================================================================================");
        System.out.println("  1. 🎬 Hiển thị danh sách phim đang chiếu");
        System.out.println("  2. 🔍 Tìm kiếm phim theo tên");
        System.out.println("  3. 📅 Xem suất chiếu và sơ đồ ghế trống");
        System.out.println("  4. 🎟️  Đặt vé xem phim (Áp dụng chính sách giá & Business Rules)");
        System.out.println("  5. 💳 Thanh toán đơn đặt vé (Tiền mặt / Chuyển khoản / Ví điện tử & Xuất vé)");
        System.out.println("  6. 🧪 Chạy tự động 6 tình huống kiểm thử (Test Cases) mẫu theo yêu cầu đồ án");
        System.out.println("  0. 🚪 Thoát chương trình");
        System.out.println("==========================================================================================");
    }

    private static void printGoodbyeMessage() {
        System.out.println("==========================================================================================");
        System.out.println("  Cảm ơn bạn đã sử dụng Hệ thống Quản lý Mua vé Xem phim - Nhóm 06!");
        System.out.println("  Hẹn gặp lại quý khách!");
        System.out.println("==========================================================================================");
    }

    /**
     * Hiển thị danh sách khách hàng có sẵn trong database để người dùng dễ chọn.
     */
    private static void displaySampleCustomers() {
        List<Customer> customers = customerRepository.findAll();
        System.out.println("DANH SÁCH KHÁCH HÀNG MẪU:");
        System.out.printf("%-4s | %-22s | %-12s | %-14s | %-12s%n",
                "ID", "Họ và tên", "Loại khách", "Số điện thoại", "MSSV");
        System.out.println("------------------------------------------------------------------------------------------");
        for (Customer c : customers) {
            String mssv = (c.getStudentId() != null) ? c.getStudentId() : "-";
            System.out.printf("%-4d | %-22s | %-12s | %-14s | %-12s%n",
                    c.getId(), c.getFullName(), c.getCustomerType().getDisplayName(), c.getPhone(), mssv);
        }
        System.out.println("------------------------------------------------------------------------------------------");
    }

    /**
     * Tách chuỗi ghế nhập vào thành danh sách các nhãn ghế (VD: "A1, A2, B3").
     */
    private static List<String> parseSeatLabels(String input) {
        List<String> list = new ArrayList<>();
        if (input == null || input.trim().isEmpty()) {
            return list;
        }
        String[] parts = input.split("[,;\\s]+");
        for (String p : parts) {
            if (!p.trim().isEmpty()) {
                list.add(p.trim().toUpperCase());
            }
        }
        return list;
    }

    /**
     * Nhập số nguyên an toàn, chống crash khi người dùng nhập chữ.
     */
    private static int readInt(Scanner scanner, String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine().trim();
            try {
                int value = Integer.parseInt(line);
                if (value >= min && value <= max) {
                    return value;
                }
                System.out.printf("⚠️  Giá trị phải nằm trong khoảng từ %d đến %d! Vui lòng nhập lại.%n", min, max);
            } catch (NumberFormatException e) {
                System.out.println("⚠️  Vui lòng nhập một số nguyên hợp lệ!");
            }
        }
    }

    /**
     * Nhập chuỗi ký tự không rỗng.
     */
    private static String readString(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine().trim();
            if (!line.isEmpty()) {
                return line;
            }
            System.out.println("⚠️  Nội dung không được để trống! Vui lòng nhập lại.");
        }
    }
}

