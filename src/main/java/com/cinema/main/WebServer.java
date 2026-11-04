package com.cinema.main;

import com.cinema.model.BankTransferPayment;
import com.cinema.model.Booking;
import com.cinema.model.CashPayment;
import com.cinema.model.Customer;
import com.cinema.model.EWalletPayment;
import com.cinema.model.Movie;
import com.cinema.model.MovieStatus;
import com.cinema.model.Payment;
import com.cinema.model.PaymentMethod;
import com.cinema.model.PaymentStatus;
import com.cinema.model.Seat;
import com.cinema.model.SeatStatus;
import com.cinema.model.Showtime;
import com.cinema.model.Ticket;
import com.cinema.repository.CustomerRepository;
import com.cinema.repository.MovieRepository;
import com.cinema.repository.ShowtimeRepository;
import com.cinema.repository.TicketRepository;
import com.cinema.service.BookingService;
import com.cinema.service.MovieService;
import com.cinema.service.PaymentService;
import com.cinema.service.ShowtimeService;
import com.cinema.utils.BusinessException;
import com.cinema.utils.DatabaseConnection;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lớp WebServer cung cấp giao diện Web và các REST API qua HTTP.
 * Sử dụng thư viện có sẵn com.sun.net.httpserver.HttpServer của Java JDK,
 * không cần cài đặt Apache Tomcat hay web container bên ngoài.
 *
 * <p>Cổng mặc định: <b>8080</b> (truy cập tại: <code>http://localhost:8080/</code>)</p>
 *
 * <p>Danh sách các REST API:
 * <ul>
 *   <li><code>GET  /</code>: Phục vụ file giao diện tĩnh index.html</li>
 *   <li><code>GET  /api/customers</code>: Lấy danh sách khách hàng</li>
 *   <li><code>GET  /api/movies</code>: Lấy danh sách phim (hỗ trợ ?keyword=...)</li>
 *   <li><code>GET  /api/showtimes?movieId=...</code>: Lấy suất chiếu & sơ đồ ghế từ MySQL</li>
 *   <li><code>POST /api/bookings</code>: Đặt vé xem phim (kiểm soát 7 Business Rules)</li>
 *   <li><code>POST /api/payments</code>: Thanh toán vé (Cash / Bank / EWallet & xuất file .txt)</li>
 *   <li><code>POST /api/run-tests</code>: Chạy tự động 6 Test Cases và trả về JSON kết quả</li>
 * </ul>
 */
public class WebServer {

    private static final int PORT = 8080;

    // Các tầng Service & Repository phụ trách nghiệp vụ
    private static final CustomerRepository customerRepository = new CustomerRepository();
    private static final MovieRepository movieRepository = new MovieRepository();
    private static final ShowtimeRepository showtimeRepository = new ShowtimeRepository();
    private static final TicketRepository ticketRepository = new TicketRepository();

    private static final MovieService movieService = new MovieService(movieRepository);
    private static final ShowtimeService showtimeService = new ShowtimeService(showtimeRepository, movieRepository);
    private static final BookingService bookingService = new BookingService(customerRepository, movieRepository, showtimeRepository, ticketRepository);
    private static final PaymentService paymentService = new PaymentService(ticketRepository, showtimeRepository);

    public static void main(String[] args) {
        try {
            startServer(PORT);
        } catch (IOException e) {
            System.err.println("❌ Lỗi không thể khởi động WebServer: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Khởi động HTTP Server và đăng ký các đường dẫn (endpoints).
     *
     * @param port cổng mạng (8080)
     * @throws IOException nếu không thể bind cổng mạng
     */
    public static void startServer(int port) throws IOException {
        // Kiểm tra kết nối MySQL trước
        DatabaseConnection.testConnection();

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        // 1. Phục vụ file tĩnh (Web Frontend)
        server.createContext("/", new StaticFileHandler());

        // 2. Các REST API endpoints
        server.createContext("/api/customers", new CustomersApiHandler());
        server.createContext("/api/movies", new MoviesApiHandler());
        server.createContext("/api/showtimes", new ShowtimesApiHandler());
        server.createContext("/api/bookings", new BookingsApiHandler());
        server.createContext("/api/payments", new PaymentsApiHandler());
        server.createContext("/api/run-tests", new RunTestsApiHandler());

        server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
        server.start();

        System.out.println("==========================================================================================");
        System.out.println(" 🌐 HỆ THỐNG QUẢN LÝ MUA VÉ XEM PHIM CHO SINH VIÊN - WEB BACKEND SERVER");
        System.out.println("==========================================================================================");
        System.out.printf("  🚀 Server đang chạy tại:  http://localhost:%d/%n", port);
        System.out.printf("  📄 Giao diện Web chính:   http://localhost:%d/index.html%n", port);
        System.out.printf("  📡 REST API Customers:    http://localhost:%d/api/customers%n", port);
        System.out.printf("  📡 REST API Movies:       http://localhost:%d/api/movies%n", port);
        System.out.printf("  📡 REST API Showtimes:    http://localhost:%d/api/showtimes?movieId=1%n", port);
        System.out.println("==========================================================================================");
    }

    // =========================================================================
    // 1. STATIC FILE HANDLER (Phục vụ giao diện tĩnh index.html)
    // =========================================================================
    static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendCorsPreflight(exchange);
                return;
            }

            String path = exchange.getRequestURI().getPath();
            if (path == null || path.equals("/") || path.isEmpty()) {
                path = "/index.html";
            }

            // Tìm file tại src/main/resources/web hoặc resources của project
            File file = new File("src/main/resources/web" + path);
            if (!file.exists()) {
                file = new File("web" + path);
            }

            if (file.exists() && file.isFile()) {
                String contentType = getContentType(path);
                byte[] bytes = readAllBytes(new FileInputStream(file));
                sendResponse(exchange, 200, contentType, bytes);
                return;
            }

            // Tìm qua ClassLoader resource
            InputStream resourceStream = WebServer.class.getResourceAsStream("/web" + path);
            if (resourceStream != null) {
                String contentType = getContentType(path);
                byte[] bytes = readAllBytes(resourceStream);
                sendResponse(exchange, 200, contentType, bytes);
                return;
            }

            // Nếu file index.html chưa tạo, hiển thị trang chào mừng và hướng dẫn
            String placeholderHtml = "<!DOCTYPE html>\n"
                    + "<html lang=\"vi\">\n"
                    + "<head><meta charset=\"UTF-8\"><title>Cinema Booking System API</title>"
                    + "<style>body{font-family:Arial,sans-serif;background:#0f172a;color:#f8fafc;padding:40px;line-height:1.6;}"
                    + ".card{background:#1e293b;padding:30px;border-radius:12px;max-width:800px;margin:auto;box-shadow:0 8px 24px rgba(0,0,0,0.3);}"
                    + "h1{color:#38bdf8;}a{color:#38bdf8;}code{background:#334155;padding:3px 8px;border-radius:4px;color:#f472b6;}</style></head>\n"
                    + "<body><div class=\"card\">"
                    + "<h1>🎬 Cinema Booking System - Backend Server</h1>"
                    + "<p>Máy chủ Web Java đã khởi động thành công trên cổng <b>" + PORT + "</b>!</p>"
                    + "<p>File giao diện <code>src/main/resources/web/index.html</code> sẽ được tải lên ở Bước tiếp theo.</p>"
                    + "<h3>Các REST API đang hoạt động:</h3>"
                    + "<ul>"
                    + "<li><a href=\"/api/customers\"><code>GET /api/customers</code></a> - Danh sách khách hàng</li>"
                    + "<li><a href=\"/api/movies\"><code>GET /api/movies</code></a> - Danh sách phim (hỗ trợ ?keyword=...)</li>"
                    + "<li><a href=\"/api/showtimes?movieId=1\"><code>GET /api/showtimes?movieId=1</code></a> - Lịch chiếu & sơ đồ ghế</li>"
                    + "<li><code>POST /api/bookings</code> - Đặt vé xem phim</li>"
                    + "<li><code>POST /api/payments</code> - Thanh toán vé & xuất file vé .txt</li>"
                    + "<li><code>POST /api/run-tests</code> - Chạy tự động 6 Test Cases</li>"
                    + "</ul></div></body></html>";

            sendResponse(exchange, 200, "text/html; charset=UTF-8", placeholderHtml.getBytes(StandardCharsets.UTF_8));
        }

        private String getContentType(String path) {
            if (path.endsWith(".html") || path.endsWith(".htm")) return "text/html; charset=UTF-8";
            if (path.endsWith(".css")) return "text/css; charset=UTF-8";
            if (path.endsWith(".js")) return "application/javascript; charset=UTF-8";
            if (path.endsWith(".json")) return "application/json; charset=UTF-8";
            if (path.endsWith(".png")) return "image/png";
            if (path.endsWith(".jpg") || path.endsWith(".jpeg")) return "image/jpeg";
            if (path.endsWith(".svg")) return "image/svg+xml";
            return "text/plain; charset=UTF-8";
        }
    }

    // =========================================================================
    // 2. /api/customers (GET: Lấy danh sách KH, POST: Thêm khách hàng mới)
    // =========================================================================
    static class CustomersApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendCorsPreflight(exchange);
                return;
            }

            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                handleGetCustomers(exchange);
                return;
            }

            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                handleCreateCustomer(exchange);
                return;
            }

            sendJsonResponse(exchange, 405, "{\"success\":false,\"message\":\"Method Not Allowed\"}");
        }

        private void handleGetCustomers(HttpExchange exchange) throws IOException {
            List<Customer> list = customerRepository.findAll();
            List<Map<String, Object>> result = new ArrayList<>();

            for (Customer c : list) {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("id", c.getId());
                map.put("customerId", c.getId());
                map.put("fullName", c.getFullName());
                map.put("email", c.getEmail());
                map.put("phone", c.getPhone());
                map.put("customerType", c.getCustomerType().getDbValue());
                map.put("customerTypeName", c.getCustomerType().getDisplayName());
                map.put("studentId", c.getStudentId());
                map.put("dateOfBirth", c.getDateOfBirth() != null ? c.getDateOfBirth().toString() : null);
                result.add(map);
            }

            sendJsonResponse(exchange, 200, SimpleJson.toJson(result));
        }

        private void handleCreateCustomer(HttpExchange exchange) throws IOException {
            try {
                String body = readRequestBody(exchange);
                Map<String, String> json = SimpleJson.parseFlatObject(body);

                String fullName = json.get("fullName");
                String phone = json.get("phone");
                String email = json.get("email");
                String customerTypeStr = json.getOrDefault("customerType", "REGULAR");
                String studentId = json.get("studentId");

                // Validate họ và tên
                if (fullName == null || fullName.trim().length() < 2) {
                    sendJsonResponse(exchange, 400, "{\"success\":false,\"message\":\"Họ và tên khách hàng phải có ít nhất 2 ký tự.\"}");
                    return;
                }

                // Validate số điện thoại (phải đủ đúng 10 số)
                if (phone == null || !phone.trim().matches("^[0-9]{10}$")) {
                    sendJsonResponse(exchange, 400, "{\"success\":false,\"message\":\"Số điện thoại không hợp lệ! Vui lòng nhập đúng 10 chữ số (Ví dụ: 0912345678).\"}");
                    return;
                }

                // Validate email
                if (email == null || !email.trim().matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
                    sendJsonResponse(exchange, 400, "{\"success\":false,\"message\":\"Địa chỉ email không đúng định dạng (Ví dụ: name@email.com).\"}");
                    return;
                }

                // Kiểm tra trùng lặp email
                Customer existingEmail = customerRepository.findByEmail(email.trim());
                if (existingEmail != null) {
                    sendJsonResponse(exchange, 400, "{\"success\":false,\"message\":\"Email '" + email.trim() + "' đã được sử dụng bởi khách hàng khác.\"}");
                    return;
                }

                // Kiểm tra trùng lặp số điện thoại
                Customer existingPhone = customerRepository.findByPhone(phone.trim());
                if (existingPhone != null) {
                    sendJsonResponse(exchange, 400, "{\"success\":false,\"message\":\"Số điện thoại '" + phone.trim() + "' đã tồn tại trong hệ thống.\"}");
                    return;
                }

                // Phân loại khách hàng
                com.cinema.model.CustomerType type = com.cinema.model.CustomerType.fromString(customerTypeStr);
                if (type == com.cinema.model.CustomerType.SINH_VIEN && (studentId == null || studentId.trim().isEmpty())) {
                    studentId = "SV" + (System.currentTimeMillis() % 1000000);
                }

                Customer customer = new Customer();
                customer.setFullName(fullName.trim());
                customer.setPhone(phone.trim());
                customer.setEmail(email.trim());
                customer.setCustomerType(type);
                if (studentId != null && !studentId.trim().isEmpty()) {
                    customer.setStudentId(studentId.trim().toUpperCase());
                }

                boolean saved = customerRepository.save(customer);
                if (!saved || customer.getId() <= 0) {
                    sendJsonResponse(exchange, 500, "{\"success\":false,\"message\":\"Lỗi hệ thống: Không thể lưu khách hàng vào cơ sở dữ liệu MySQL!\"}");
                    return;
                }

                Map<String, Object> resp = new LinkedHashMap<>();
                resp.put("success", true);
                resp.put("message", "Thêm khách hàng '" + customer.getFullName() + "' thành công!");
                resp.put("id", customer.getId());
                resp.put("customerId", customer.getId());
                resp.put("fullName", customer.getFullName());
                resp.put("email", customer.getEmail());
                resp.put("phone", customer.getPhone());
                resp.put("customerType", customer.getCustomerType().getDbValue());
                resp.put("customerTypeName", customer.getCustomerType().getDisplayName());
                resp.put("studentId", customer.getStudentId());

                sendJsonResponse(exchange, 201, SimpleJson.toJson(resp));

            } catch (Exception e) {
                sendJsonResponse(exchange, 500, "{\"success\":false,\"message\":\"Lỗi xử lý tạo khách hàng: " + e.getMessage() + "\"}");
            }
        }
    }

    // =========================================================================
    // 3. GET /api/movies (Lấy danh sách phim, hỗ trợ tìm kiếm ?keyword=...)
    // =========================================================================
    static class MoviesApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendCorsPreflight(exchange);
                return;
            }

            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, "{\"success\":false,\"message\":\"Method Not Allowed\"}");
                return;
            }

            Map<String, String> queryParams = parseQueryParams(exchange.getRequestURI().getQuery());
            String keyword = queryParams.get("keyword");

            List<Movie> list;
            if (keyword != null && !keyword.trim().isEmpty()) {
                list = movieRepository.searchByTitle(keyword.trim());
            } else {
                list = movieRepository.findAll();
            }

            List<Map<String, Object>> result = new ArrayList<>();
            for (Movie m : list) {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("movieId", m.getMovieId());
                map.put("title", m.getTitle());
                map.put("genre", m.getGenre());
                map.put("duration", m.getDuration());
                map.put("director", m.getDirector());
                map.put("releaseDate", m.getReleaseDate() != null ? m.getReleaseDate().toString() : null);
                map.put("rating", m.getRating());
                map.put("description", m.getDescription());
                map.put("status", m.getStatus().getDbValue());
                map.put("statusName", m.getStatus().getDisplayName());
                map.put("isShowing", m.isShowing());
                result.add(map);
            }

            sendJsonResponse(exchange, 200, SimpleJson.toJson(result));
        }
    }

    // =========================================================================
    // 4. GET /api/showtimes?movieId=... (Lấy danh sách suất chiếu & sơ đồ ghế)
    // =========================================================================
    static class ShowtimesApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendCorsPreflight(exchange);
                return;
            }

            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, "{\"success\":false,\"message\":\"Method Not Allowed\"}");
                return;
            }

            Map<String, String> queryParams = parseQueryParams(exchange.getRequestURI().getQuery());
            String movieIdStr = queryParams.get("movieId");

            List<Showtime> showtimeList;
            if (movieIdStr != null && !movieIdStr.trim().isEmpty()) {
                try {
                    int movieId = Integer.parseInt(movieIdStr.trim());
                    showtimeList = showtimeRepository.findByMovieId(movieId);
                } catch (NumberFormatException e) {
                    sendJsonResponse(exchange, 400, "{\"success\":false,\"message\":\"Mã phim movieId không hợp lệ\"}");
                    return;
                }
            } else {
                showtimeList = showtimeRepository.findAllScheduled();
            }

            List<Map<String, Object>> result = new ArrayList<>();
            for (Showtime st : showtimeList) {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("showtimeId", st.getShowtimeId());
                map.put("movieId", st.getMovieId());
                map.put("movieTitle", st.getMovie() != null ? st.getMovie().getTitle() : "Phim #" + st.getMovieId());
                map.put("roomId", st.getRoomId());
                map.put("roomName", st.getRoom() != null ? st.getRoom().getRoomName() : "Phòng #" + st.getRoomId());
                map.put("roomType", st.getRoom() != null ? st.getRoom().getRoomType() : "2D");
                map.put("showDate", st.getShowDateFormatted());
                map.put("startTime", st.getStartTimeFormatted());
                map.put("endTime", st.getEndTimeFormatted());
                map.put("basePrice", st.getBasePrice());
                map.put("status", st.getStatus());

                // Nạp danh sách ghế chi tiết kèm trạng thái và loại ghế
                List<Seat> seats = showtimeRepository.getSeatsByShowtimeId(st.getShowtimeId());
                List<Map<String, Object>> seatList = new ArrayList<>();
                int availableCount = 0;

                for (Seat seat : seats) {
                    Map<String, Object> sMap = new LinkedHashMap<>();
                    sMap.put("seatId", seat.getSeatId());
                    sMap.put("showtimeSeatId", seat.getShowtimeSeatId());
                    sMap.put("seatRow", seat.getSeatRow());
                    sMap.put("seatNumber", seat.getSeatNumber());
                    sMap.put("seatLabel", seat.getSeatLabel());
                    sMap.put("seatType", seat.getSeatType().getDbValue());
                    sMap.put("seatTypeName", seat.getSeatType().getDisplayName());
                    sMap.put("surcharge", seat.getSeatType().getSurcharge());
                    sMap.put("status", seat.getStatus().getDbValue());
                    sMap.put("statusName", seat.getStatus().getDisplayName());
                    sMap.put("isAvailable", seat.isAvailable());

                    if (seat.isAvailable()) {
                        availableCount++;
                    }
                    seatList.add(sMap);
                }

                map.put("totalSeats", seats.size());
                map.put("availableSeats", availableCount);
                map.put("seats", seatList);
                result.add(map);
            }

            sendJsonResponse(exchange, 200, SimpleJson.toJson(result));
        }
    }

    // =========================================================================
    // 5. POST /api/bookings (Đặt vé - kiểm soát 7 Business Rules)
    // =========================================================================
    static class BookingsApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendCorsPreflight(exchange);
                return;
            }

            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, "{\"success\":false,\"message\":\"Method Not Allowed\"}");
                return;
            }

            String body = readRequestBody(exchange);
            Map<String, String> json = SimpleJson.parseFlatObject(body);

            int customerId = SimpleJson.getInt(json, "customerId", 0);
            int showtimeId = SimpleJson.getInt(json, "showtimeId", 0);
            String seatNumber = json.get("seatNumber");
            String seatsCsv = json.get("seats");

            List<String> seatLabels = new ArrayList<>();
            if (seatNumber != null && !seatNumber.trim().isEmpty()) {
                seatLabels.add(seatNumber.trim().toUpperCase());
            } else if (seatsCsv != null && !seatsCsv.trim().isEmpty()) {
                String[] parts = seatsCsv.split("[,;\\s]+");
                for (String p : parts) {
                    if (!p.trim().isEmpty()) seatLabels.add(p.trim().toUpperCase());
                }
            }

            try {
                // Gọi BookingService để thực thi kiểm tra 7 Business Rules và tính giá đa hình
                Booking booking = bookingService.bookTickets(customerId, showtimeId, seatLabels);

                Map<String, Object> resp = new LinkedHashMap<>();
                resp.put("success", true);
                resp.put("message", "Đặt vé xem phim thành công!");
                resp.put("bookingId", booking.getBookingId());
                resp.put("bookingCode", booking.getBookingCode());
                resp.put("customerName", booking.getCustomer().getFullName());
                resp.put("customerType", booking.getCustomer().getCustomerType().getDisplayName());
                resp.put("totalAmount", booking.getTotalAmount());
                resp.put("discountAmount", booking.getDiscountAmount());
                resp.put("finalAmount", booking.getFinalAmount());
                resp.put("status", booking.getStatus().getDisplayName());

                List<Map<String, Object>> ticketsResp = new ArrayList<>();
                for (Ticket t : booking.getTickets()) {
                    Map<String, Object> tMap = new LinkedHashMap<>();
                    tMap.put("ticketId", t.getTicketId());
                    tMap.put("ticketCode", t.getTicketCode());
                    tMap.put("seatLabel", t.getSeat() != null ? t.getSeat().getSeatLabel() : "");
                    tMap.put("seatPrice", t.getSeatPrice());
                    ticketsResp.add(tMap);
                }
                resp.put("tickets", ticketsResp);

                sendJsonResponse(exchange, 200, SimpleJson.toJson(resp));

            } catch (BusinessException e) {
                // Trả về thông báo lỗi vi phạm Business Rule rõ ràng
                Map<String, Object> err = new LinkedHashMap<>();
                err.put("success", false);
                err.put("errorCode", e.getErrorCode());
                err.put("message", e.getMessage());
                sendJsonResponse(exchange, 400, SimpleJson.toJson(err));
            } catch (Exception e) {
                Map<String, Object> err = new LinkedHashMap<>();
                err.put("success", false);
                err.put("errorCode", "SERVER_ERROR");
                err.put("message", "Lỗi máy chủ nội bộ: " + e.getMessage());
                sendJsonResponse(exchange, 500, SimpleJson.toJson(err));
            }
        }
    }

    // =========================================================================
    // 6. POST /api/payments (Thanh toán đa hình & Xuất file vé .txt)
    // =========================================================================
    static class PaymentsApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendCorsPreflight(exchange);
                return;
            }

            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, "{\"success\":false,\"message\":\"Method Not Allowed\"}");
                return;
            }

            String body = readRequestBody(exchange);
            Map<String, String> json = SimpleJson.parseFlatObject(body);

            int bookingId = SimpleJson.getInt(json, "bookingId", 0);
            int ticketId = SimpleJson.getInt(json, "ticketId", 0);
            String methodStr = json.getOrDefault("paymentMethod", "CASH").toUpperCase();
            boolean simulateFail = "true".equalsIgnoreCase(json.get("simulateFail"));

            try {
                // Nếu người dùng truyền ticketId thay vì bookingId
                if (bookingId <= 0 && ticketId > 0) {
                    Ticket ticket = ticketRepository.findById(ticketId);
                    if (ticket == null) {
                        throw new BusinessException(BusinessException.INVALID_INPUT, "Không tìm thấy vé với ID: " + ticketId);
                    }
                    bookingId = ticket.getBookingId();
                }

                if (bookingId <= 0) {
                    throw new BusinessException(BusinessException.INVALID_INPUT, "Mã đơn hàng bookingId hoặc ticketId là bắt buộc.");
                }

                Booking booking = ticketRepository.findBookingById(bookingId);
                if (booking == null) {
                    throw new BusinessException(BusinessException.BOOKING_NOT_FOUND, "Không tìm thấy đơn đặt vé #" + bookingId);
                }

                // Áp dụng ĐA HÌNH PaymentMethod
                PaymentMethod paymentMethod;
                switch (methodStr) {
                    case "BANK":
                    case "BANK_TRANSFER":
                        paymentMethod = new BankTransferPayment("VietinBank - CN SON LA - HOI SO", "102883583080", "NGO HOANG KHANH");
                        break;
                    case "EWALLET":
                    case "MOMO":
                    case "ZALOPAY":
                        paymentMethod = new EWalletPayment("VietinBank QR", "102883583080");
                        break;
                    case "CASH":
                    default:
                        paymentMethod = new CashPayment();
                        break;
                }

                // Thực hiện thanh toán và tự động xuất vé .txt (xử lý cả simulateFail qua PaymentService)
                Payment payment = paymentService.processPayment(bookingId, paymentMethod, simulateFail);

                Map<String, Object> resp = new LinkedHashMap<>();
                resp.put("success", true);
                resp.put("message", "Thanh toán thành công! Vé xem phim đã được xuất ra file .txt.");
                resp.put("paymentId", payment.getPaymentId());
                resp.put("paymentCode", payment.getPaymentCode());
                resp.put("bookingId", bookingId);
                resp.put("bookingCode", booking.getBookingCode());
                resp.put("customerName", booking.getCustomer() != null ? booking.getCustomer().getFullName() : "Khách hàng");
                resp.put("customerType", booking.getCustomer() != null ? booking.getCustomer().getCustomerType().getDisplayName() : "");
                resp.put("amount", payment.getAmount());
                resp.put("paymentMethod", paymentMethod.getMethodName());
                resp.put("status", payment.getStatus());
                resp.put("paymentTime", payment.getPaymentTimeFormatted());
                resp.put("exportedDirectory", "exported_tickets/");

                String movieTitle = (booking.getShowtime() != null && booking.getShowtime().getMovie() != null)
                        ? booking.getShowtime().getMovie().getTitle() : "Phim chiếu rạp";
                String roomName = (booking.getShowtime() != null && booking.getShowtime().getRoom() != null)
                        ? booking.getShowtime().getRoom().getRoomName() : "Phòng chiếu";
                String showTime = (booking.getShowtime() != null)
                        ? booking.getShowtime().getShowDateFormatted() + " (" + booking.getShowtime().getStartTimeFormatted() + " - " + booking.getShowtime().getEndTimeFormatted() + ")"
                        : "";
                List<String> seatsList = new ArrayList<>();
                List<String> ticketCodesList = new ArrayList<>();
                if (booking.getTickets() != null) {
                    for (Ticket t : booking.getTickets()) {
                        if (t.getSeat() != null) seatsList.add(t.getSeat().getSeatLabel());
                        ticketCodesList.add(t.getTicketCode());
                    }
                }
                resp.put("movieTitle", movieTitle);
                resp.put("roomName", roomName);
                resp.put("showTime", showTime);
                resp.put("seats", seatsList);
                resp.put("ticketCodes", ticketCodesList);

                sendJsonResponse(exchange, 200, SimpleJson.toJson(resp));

            } catch (BusinessException e) {
                Map<String, Object> err = new LinkedHashMap<>();
                err.put("success", false);
                err.put("errorCode", e.getErrorCode());
                err.put("message", e.getMessage());
                sendJsonResponse(exchange, 400, SimpleJson.toJson(err));
            } catch (Exception e) {
                Map<String, Object> err = new LinkedHashMap<>();
                err.put("success", false);
                err.put("errorCode", "SERVER_ERROR");
                err.put("message", "Lỗi xử lý thanh toán: " + e.getMessage());
                sendJsonResponse(exchange, 500, SimpleJson.toJson(err));
            }
        }
    }

    // =========================================================================
    // 7. POST /api/run-tests (Tự động chạy 6 Test Cases & trả JSON kết quả)
    // =========================================================================
    static class RunTestsApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendCorsPreflight(exchange);
                return;
            }

            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod()) && !"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, "{\"success\":false,\"message\":\"Method Not Allowed\"}");
                return;
            }

            List<Map<String, Object>> testResults = new ArrayList<>();
            Booking bookingTest4 = null;

            // ----- TEST CASE 1 -----
            Map<String, Object> tc1 = new LinkedHashMap<>();
            tc1.put("testId", "TC-01");
            tc1.put("name", "Khách hàng thường đặt ghế CÒN TRỐNG thành công");
            tc1.put("input", "KH: Nguyễn Văn An (ID 1 - Khách thường), Suất chiếu #1, Ghế: A1, A2");
            tc1.put("expected", "Đặt vé thành công, tạo đơn BK-..., ghế chuyển sang DA_DAT, trạng thái CHUA_THANH_TOAN");
            try {
                Booking b1 = bookingService.bookTickets(1, 1, Arrays.asList("A1", "A2"));
                tc1.put("passed", true);
                tc1.put("actual", "Tạo đơn " + b1.getBookingCode() + " thành công (" + b1.getTicketCount() + " vé, "
                        + String.format("%,.0f đ", b1.getFinalAmount()) + ", trạng thái: " + b1.getStatus().getDisplayName() + ")");
            } catch (Exception e) {
                tc1.put("passed", false);
                tc1.put("actual", "Lỗi không mong muốn: " + e.getMessage());
            }
            testResults.add(tc1);

            // ----- TEST CASE 2 -----
            Map<String, Object> tc2 = new LinkedHashMap<>();
            tc2.put("testId", "TC-02");
            tc2.put("name", "Khách đặt ghế ĐÃ CÓ NGƯỜI ĐẶT TRƯỚC");
            tc2.put("input", "KH: Trần Thị Bích (ID 2), Suất chiếu #1, Ghế: A3 (Ghế A3 đã có người đặt trước)");
            tc2.put("expected", "Bắt ngoại lệ BusinessException với mã lỗi SEAT_ALREADY_BOOKED");
            try {
                bookingService.bookTickets(2, 1, Collections.singletonList("A3"));
                tc2.put("passed", false);
                tc2.put("actual", "THẤT BẠI: Hệ thống cho phép đặt ghế đã có người đặt!");
            } catch (BusinessException e) {
                tc2.put("passed", true);
                tc2.put("actual", "BẮT NGOẠI LỆ THÀNH CÔNG: [" + e.getErrorCode() + "] " + e.getMessage());
            } catch (Exception e) {
                tc2.put("passed", false);
                tc2.put("actual", "Lỗi không mong muốn: " + e.getMessage());
            }
            testResults.add(tc2);

            // ----- TEST CASE 3 -----
            Map<String, Object> tc3 = new LinkedHashMap<>();
            tc3.put("testId", "TC-03");
            tc3.put("name", "Khách chọn ghế KHÔNG TỒN TẠI trong phòng chiếu");
            tc3.put("input", "KH: Nguyễn Văn An (ID 1), Suất chiếu #1, Ghế: Z99 (Ghế không tồn tại)");
            tc3.put("expected", "Bắt ngoại lệ BusinessException với mã lỗi INVALID_INPUT");
            try {
                bookingService.bookTickets(1, 1, Collections.singletonList("Z99"));
                tc3.put("passed", false);
                tc3.put("actual", "THẤT BẠI: Hệ thống không chặn được ghế không tồn tại!");
            } catch (BusinessException e) {
                tc3.put("passed", true);
                tc3.put("actual", "BẮT NGOẠI LỆ THÀNH CÔNG: [" + e.getErrorCode() + "] " + e.getMessage());
            } catch (Exception e) {
                tc3.put("passed", false);
                tc3.put("actual", "Lỗi: " + e.getMessage());
            }
            testResults.add(tc3);

            // ----- TEST CASE 4 -----
            Map<String, Object> tc4 = new LinkedHashMap<>();
            tc4.put("testId", "TC-04");
            tc4.put("name", "Sinh viên mua vé - Kiểm tra ĐA HÌNH StudentPricePolicy (Giảm 20%)");
            tc4.put("input", "KH: Lê Hoàng Nam (ID 3 - Sinh viên), Suất #1 (75.000đ), Ghế đôi D1 (+30.000đ)");
            tc4.put("expected", "Tổng gốc: 105.000đ -> Giảm 20%: -21.000đ -> Thành tiền: 84.000đ (khớp 100%)");
            try {
                Booking b4 = bookingService.bookTickets(3, 1, Collections.singletonList("D1"));
                bookingTest4 = b4;
                boolean matched = Math.abs(b4.getFinalAmount() - 84000.0) < 0.01;
                tc4.put("passed", matched);
                tc4.put("actual", "Đơn " + b4.getBookingCode() + ": Gốc " + String.format("%,.0f đ", b4.getTotalAmount())
                        + " - Giảm 20%: " + String.format("%,.0f đ", b4.getDiscountAmount())
                        + " = Thành tiền: " + String.format("%,.0f đ", b4.getFinalAmount())
                        + (matched ? " (KHỚP 100%)" : " (SAI SỐ LIỆU)"));
            } catch (Exception e) {
                tc4.put("passed", false);
                tc4.put("actual", "Lỗi: " + e.getMessage());
            }
            testResults.add(tc4);

            // ----- TEST CASE 5 -----
            Map<String, Object> tc5 = new LinkedHashMap<>();
            tc5.put("testId", "TC-05");
            tc5.put("name", "Khách đặt vé cho PHIM ĐÃ NGỪNG CHIẾU");
            tc5.put("input", "KH: Nguyễn Văn An (ID 1), Suất #11 (Gắn với phim Quật Mộ Trùng Ma - STOPPED)");
            tc5.put("expected", "Bắt ngoại lệ BusinessException với mã lỗi MOVIE_NOT_SHOWING");
            try {
                bookingService.bookTickets(1, 11, Collections.singletonList("A1"));
                tc5.put("passed", false);
                tc5.put("actual", "THẤT BẠI: Hệ thống cho phép đặt vé phim đã ngừng chiếu!");
            } catch (BusinessException e) {
                tc5.put("passed", true);
                tc5.put("actual", "BẮT NGOẠI LỆ THÀNH CÔNG: [" + e.getErrorCode() + "] " + e.getMessage());
            } catch (Exception e) {
                tc5.put("passed", false);
                tc5.put("actual", "Lỗi: " + e.getMessage());
            }
            testResults.add(tc5);

            // ----- TEST CASE 6 -----
            Map<String, Object> tc6 = new LinkedHashMap<>();
            tc6.put("testId", "TC-06");
            tc6.put("name", "Thanh toán vé thành công và xuất file vé .txt (File IO)");
            tc6.put("input", "Sử dụng đơn đặt từ TC-04, Phương thức đa hình: Ví điện tử MoMo (EWalletPayment)");
            tc6.put("expected", "Trạng thái chuyển sang DA_THANH_TOAN, lưu payments, xuất file vé ra thư mục exported_tickets/");
            try {
                int targetId = (bookingTest4 != null) ? bookingTest4.getBookingId() : 5;
                PaymentMethod ewallet = new EWalletPayment("MoMo", "0923456789");
                Payment p = paymentService.processPayment(targetId, ewallet);

                File dir = new File("exported_tickets");
                int fileCount = (dir.exists() && dir.isDirectory() && dir.listFiles() != null) ? dir.listFiles().length : 0;

                tc6.put("passed", true);
                tc6.put("actual", "Thanh toán mã " + p.getPaymentCode() + " thành công (" + String.format("%,.0f đ", p.getAmount())
                        + "). Thư mục exported_tickets/ hiện có " + fileCount + " file vé.");
            } catch (Exception e) {
                tc6.put("passed", false);
                tc6.put("actual", "Lỗi: " + e.getMessage());
            }
            testResults.add(tc6);

            Map<String, Object> finalResp = new LinkedHashMap<>();
            finalResp.put("success", true);
            finalResp.put("totalTests", testResults.size());
            finalResp.put("testCases", testResults);

            sendJsonResponse(exchange, 200, SimpleJson.toJson(finalResp));
        }
    }

    // =========================================================================
    // 8. CÁC HÀM TIỆN ÍCH HTTP & JSON (Không phụ thuộc thư viện ngoài)
    // =========================================================================

    private static void sendCorsPreflight(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
        exchange.sendResponseHeaders(204, -1);
    }

    private static void sendJsonResponse(HttpExchange exchange, int statusCode, String jsonResponse) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        byte[] bytes = jsonResponse.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static void sendResponse(HttpExchange exchange, int statusCode, String contentType, byte[] bytes) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static String readRequestBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            byte[] bytes = readAllBytes(is);
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }

    private static byte[] readAllBytes(InputStream is) throws IOException {
        java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[8192];
        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        return buffer.toByteArray();
    }

    private static Map<String, String> parseQueryParams(String query) {
        Map<String, String> params = new HashMap<>();
        if (query == null || query.trim().isEmpty()) {
            return params;
        }
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            if (idx > 0 && idx < pair.length() - 1) {
                String key = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
                String val = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
                params.put(key, val);
            }
        }
        return params;
    }

    // =========================================================================
    // 9. BỘ CHUYỂN ĐỔI JSON THUẦN JAVA (Độc lập, an toàn, không cần JAR bên ngoài)
    // =========================================================================
    public static class SimpleJson {

        public static String toJson(Object obj) {
            if (obj == null) return "null";
            if (obj instanceof String) {
                return "\"" + escape((String) obj) + "\"";
            }
            if (obj instanceof Number || obj instanceof Boolean) {
                return String.valueOf(obj);
            }
            if (obj instanceof Map) {
                StringBuilder sb = new StringBuilder("{");
                boolean first = true;
                for (Map.Entry<?, ?> entry : ((Map<?, ?>) obj).entrySet()) {
                    if (!first) sb.append(",");
                    sb.append("\"").append(escape(String.valueOf(entry.getKey()))).append("\":");
                    sb.append(toJson(entry.getValue()));
                    first = false;
                }
                sb.append("}");
                return sb.toString();
            }
            if (obj instanceof Iterable) {
                StringBuilder sb = new StringBuilder("[");
                boolean first = true;
                for (Object item : (Iterable<?>) obj) {
                    if (!first) sb.append(",");
                    sb.append(toJson(item));
                    first = false;
                }
                sb.append("]");
                return sb.toString();
            }
            return "\"" + escape(obj.toString()) + "\"";
        }

        public static String escape(String s) {
            if (s == null) return "";
            StringBuilder sb = new StringBuilder();
            for (char c : s.toCharArray()) {
                switch (c) {
                    case '"':  sb.append("\\\""); break;
                    case '\\': sb.append("\\\\"); break;
                    case '\b': sb.append("\\b"); break;
                    case '\f': sb.append("\\f"); break;
                    case '\n': sb.append("\\n"); break;
                    case '\r': sb.append("\\r"); break;
                    case '\t': sb.append("\\t"); break;
                    default:   sb.append(c);
                }
            }
            return sb.toString();
        }

        public static Map<String, String> parseFlatObject(String json) {
            Map<String, String> map = new HashMap<>();
            if (json == null || json.trim().isEmpty()) {
                return map;
            }
            Pattern p = Pattern.compile("\"([a-zA-Z0-9_]+)\"\\s*:\\s*(\"([^\"]*)\"|([0-9.]+)|true|false|null|\\[([^\\]]*)\\])");
            Matcher m = p.matcher(json);
            while (m.find()) {
                String key = m.group(1);
                String val;
                if (m.group(3) != null) {
                    val = m.group(3); // Chuỗi nằm trong dấu nháy kép
                } else if (m.group(5) != null) {
                    val = m.group(5); // Mảng [A1, A2]
                } else {
                    val = m.group(2); // Số hoặc boolean
                }
                map.put(key, val);
            }
            return map;
        }

        public static int getInt(Map<String, String> map, String key, int def) {
            String val = map.get(key);
            if (val == null) return def;
            try {
                return Integer.parseInt(val.trim());
            } catch (NumberFormatException e) {
                return def;
            }
        }
    }
}

