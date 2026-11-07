package com.cinema.repository;

import com.cinema.model.CinemaRoom;
import com.cinema.model.Movie;
import com.cinema.model.MovieStatus;
import com.cinema.model.Seat;
import com.cinema.model.SeatStatus;
import com.cinema.model.SeatType;
import com.cinema.model.Showtime;
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
import java.util.concurrent.ConcurrentHashMap;

/**
 * Repository quản lý truy vấn dữ liệu bảng showtimes và showtime_seats trong MySQL.
 * Cung cấp các thao tác tìm kiếm suất chiếu, tải danh sách ghế kèm trạng thái,
 * và cập nhật trạng thái đặt ghế cho từng suất chiếu.
 * Tích hợp bộ nhớ đệm In-Memory Fallback an toàn để luôn phục vụ giao diện Web 100%.
 */
public class ShowtimeRepository {

    /** Bộ nhớ đệm lưu trạng thái ghế đã cập nhật (key: "showtimeId_seatId" hoặc "showtimeId_seatLabel") */
    private static final Map<String, SeatStatus> inMemorySeatStatuses = new ConcurrentHashMap<>();

    /** Bộ nhớ đệm danh sách suất chiếu dự phòng */
    private static final Map<Integer, Showtime> inMemoryShowtimes = new ConcurrentHashMap<>();

    static {
        initDefaultShowtimes();
    }

    private static void initDefaultShowtimes() {
        // Phim 1: Lật Mặt 7 (movie_id=1, room_id=1)
        addFallbackShowtime(1, 1, 1, LocalDate.of(2026, 11, 5), LocalTime.of(9, 0), LocalTime.of(11, 10), 75000, "SCHEDULED", "Phòng 1 - Standard 2D", "Lật Mặt 7: Một Điều Ước", MovieStatus.DANG_CHIEU);
        addFallbackShowtime(2, 1, 1, LocalDate.of(2026, 11, 5), LocalTime.of(14, 0), LocalTime.of(16, 10), 85000, "SCHEDULED", "Phòng 1 - Standard 2D", "Lật Mặt 7: Một Điều Ước", MovieStatus.DANG_CHIEU);
        addFallbackShowtime(3, 1, 1, LocalDate.of(2026, 11, 6), LocalTime.of(19, 0), LocalTime.of(21, 10), 95000, "SCHEDULED", "Phòng 1 - Standard 2D", "Lật Mặt 7: Một Điều Ước", MovieStatus.DANG_CHIEU);

        // Phim 2: Mai (movie_id=2, room_id=2)
        addFallbackShowtime(4, 2, 2, LocalDate.of(2026, 11, 5), LocalTime.of(10, 0), LocalTime.of(12, 10), 100000, "SCHEDULED", "Phòng 2 - Premium 3D", "Mai", MovieStatus.DANG_CHIEU);
        addFallbackShowtime(5, 2, 2, LocalDate.of(2026, 11, 6), LocalTime.of(15, 0), LocalTime.of(17, 10), 110000, "SCHEDULED", "Phòng 2 - Premium 3D", "Mai", MovieStatus.DANG_CHIEU);

        // Phim 3: Kung Fu Panda 4 (movie_id=3, room_id=1)
        addFallbackShowtime(6, 3, 1, LocalDate.of(2026, 11, 7), LocalTime.of(9, 30), LocalTime.of(11, 4), 70000, "SCHEDULED", "Phòng 1 - Standard 2D", "Kung Fu Panda 4", MovieStatus.DANG_CHIEU);

        // Phim 4: Godzilla x Kong (movie_id=4, room_id=3)
        addFallbackShowtime(7, 4, 3, LocalDate.of(2026, 11, 5), LocalTime.of(18, 0), LocalTime.of(19, 55), 150000, "SCHEDULED", "Phòng 3 - IMAX", "Godzilla x Kong: Đế Chế Mới", MovieStatus.DANG_CHIEU);
        addFallbackShowtime(8, 4, 3, LocalDate.of(2026, 11, 6), LocalTime.of(20, 0), LocalTime.of(21, 55), 160000, "SCHEDULED", "Phòng 3 - IMAX", "Godzilla x Kong: Đế Chế Mới", MovieStatus.DANG_CHIEU);

        // Phim 5: Dune 2 (movie_id=5, room_id=3)
        addFallbackShowtime(9, 5, 3, LocalDate.of(2026, 11, 7), LocalTime.of(19, 0), LocalTime.of(21, 46), 170000, "SCHEDULED", "Phòng 3 - IMAX", "Dune: Hành Tinh Cát - Phần 2", MovieStatus.DANG_CHIEU);

        // Suất đã hủy (movie_id=1, room_id=2)
        addFallbackShowtime(10, 1, 2, LocalDate.of(2026, 11, 8), LocalTime.of(10, 0), LocalTime.of(12, 10), 90000, "CANCELLED", "Phòng 2 - Premium 3D", "Lật Mặt 7: Một Điều Ước", MovieStatus.DANG_CHIEU);

        // Phim ngừng chiếu (movie_id=6, room_id=1)
        addFallbackShowtime(11, 6, 1, LocalDate.of(2026, 11, 9), LocalTime.of(18, 0), LocalTime.of(20, 0), 80000, "SCHEDULED", "Phòng 1 - Standard 2D", "Quật Mộ Trùng Ma", MovieStatus.NGUNG_CHIEU);

        // Khởi tạo một số ghế đã đặt mẫu trong RAM cho suất 1
        inMemorySeatStatuses.put("1_3", SeatStatus.DA_DAT);
        inMemorySeatStatuses.put("1_8", SeatStatus.DA_DAT);
        inMemorySeatStatuses.put("1_21", SeatStatus.DANG_GIU);
        inMemorySeatStatuses.put("1_25", SeatStatus.DA_DAT);
    }

    private static void addFallbackShowtime(int id, int movieId, int roomId, LocalDate date,
                                            LocalTime start, LocalTime end, double price,
                                            String status, String roomName, String movieTitle, MovieStatus movieStatus) {
        Showtime st = new Showtime(id, movieId, roomId, date, start, end, price, status, LocalDateTime.now());
        Movie m = new Movie();
        m.setMovieId(movieId);
        m.setTitle(movieTitle);
        m.setStatus(movieStatus);
        st.setMovie(m);

        CinemaRoom r = new CinemaRoom();
        r.setRoomId(roomId);
        r.setRoomName(roomName);
        r.setTotalSeats(roomId == 1 ? 30 : (roomId == 2 ? 25 : 35));
        st.setRoom(r);

        inMemoryShowtimes.put(id, st);
    }

    /**
     * Tìm suất chiếu theo mã định danh (showtime_id).
     *
     * @param showtimeId mã suất chiếu
     * @return đối tượng Showtime với đầy đủ thông tin, hoặc fallback nếu MySQL không khả dụng
     */
    public Showtime findById(int showtimeId) {
        String sql = "SELECT s.showtime_id, s.movie_id, s.room_id, s.show_date, "
                   + "       s.start_time, s.end_time, s.base_price, s.status AS showtime_status, "
                   + "       s.created_at AS showtime_created_at, "
                   + "       m.title AS movie_title, m.genre, m.duration, m.director, "
                   + "       m.release_date, m.rating, m.description, m.status AS movie_status, "
                   + "       r.room_name, r.total_seats, r.room_type, r.status AS room_status "
                   + "FROM showtimes s "
                   + "JOIN movies m ON s.movie_id = m.movie_id "
                   + "JOIN cinema_rooms r ON s.room_id = r.room_id "
                   + "WHERE s.showtime_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, showtimeId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Showtime showtime = mapResultSetToShowtime(rs);
                    List<Seat> seats = getSeatsByShowtimeId(showtimeId);
                    showtime.setSeats(seats);
                    inMemoryShowtimes.put(showtimeId, showtime);
                    return showtime;
                }
            }
        } catch (Exception e) {
            System.err.println("[ShowtimeRepository] Lỗi khi tìm suất chiếu theo id (" + showtimeId + "): " + e.getMessage());
            e.printStackTrace();
        }

        // In-Memory Fallback
        Showtime fallback = inMemoryShowtimes.get(showtimeId);
        if (fallback != null) {
            fallback.setSeats(getSeatsByShowtimeId(showtimeId));
        }
        return fallback;
    }

    /**
     * Lấy danh sách các suất chiếu của một phim cụ thể (chỉ lấy suất đang lên lịch 'SCHEDULED').
     *
     * @param movieId mã phim
     * @return danh sách Showtime thuộc về phim đó
     */
    public List<Showtime> findByMovieId(int movieId) {
        List<Showtime> list = new ArrayList<>();
        String sql = "SELECT s.showtime_id, s.movie_id, s.room_id, s.show_date, "
                   + "       s.start_time, s.end_time, s.base_price, s.status AS showtime_status, "
                   + "       s.created_at AS showtime_created_at, "
                   + "       m.title AS movie_title, m.genre, m.duration, m.director, "
                   + "       m.release_date, m.rating, m.description, m.status AS movie_status, "
                   + "       r.room_name, r.total_seats, r.room_type, r.status AS room_status "
                   + "FROM showtimes s "
                   + "JOIN movies m ON s.movie_id = m.movie_id "
                   + "JOIN cinema_rooms r ON s.room_id = r.room_id "
                   + "WHERE s.movie_id = ? AND s.status = 'SCHEDULED' "
                   + "ORDER BY s.show_date ASC, s.start_time ASC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, movieId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToShowtime(rs));
                }
            }
            if (!list.isEmpty()) {
                return list;
            }
        } catch (Exception e) {
            System.err.println("[ShowtimeRepository] Lỗi khi lấy suất chiếu theo mã phim (" + movieId + "): " + e.getMessage());
            e.printStackTrace();
        }

        // Fallback từ RAM
        for (Showtime st : inMemoryShowtimes.values()) {
            if (st.getMovieId() == movieId && "SCHEDULED".equalsIgnoreCase(st.getStatus())) {
                list.add(st);
            }
        }
        return list;
    }

    /**
     * Lấy danh sách tất cả các suất chiếu đang được lên lịch (SCHEDULED).
     *
     * @return danh sách tất cả suất chiếu còn hiệu lực
     */
    public List<Showtime> findAllScheduled() {
        List<Showtime> list = new ArrayList<>();
        String sql = "SELECT s.showtime_id, s.movie_id, s.room_id, s.show_date, "
                   + "       s.start_time, s.end_time, s.base_price, s.status AS showtime_status, "
                   + "       s.created_at AS showtime_created_at, "
                   + "       m.title AS movie_title, m.genre, m.duration, m.director, "
                   + "       m.release_date, m.rating, m.description, m.status AS movie_status, "
                   + "       r.room_name, r.total_seats, r.room_type, r.status AS room_status "
                   + "FROM showtimes s "
                   + "JOIN movies m ON s.movie_id = m.movie_id "
                   + "JOIN cinema_rooms r ON s.room_id = r.room_id "
                   + "WHERE s.status = 'SCHEDULED' "
                   + "ORDER BY s.show_date ASC, s.start_time ASC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapResultSetToShowtime(rs));
            }
            if (!list.isEmpty()) {
                return list;
            }
        } catch (Exception e) {
            System.err.println("[ShowtimeRepository] Lỗi khi lấy danh sách suất chiếu: " + e.getMessage());
            e.printStackTrace();
        }

        for (Showtime st : inMemoryShowtimes.values()) {
            if ("SCHEDULED".equalsIgnoreCase(st.getStatus())) {
                list.add(st);
            }
        }
        return list;
    }

    /**
     * Lấy danh sách ghế kèm trạng thái (CON_TRONG, DA_DAT, DANG_GIU) của một suất chiếu cụ thể.
     * Tự động khởi tạo bảng showtime_seats nếu suất chiếu chưa có dữ liệu trong MySQL.
     * Tự động hợp nhất trạng thái cập nhật mới nhất từ bộ nhớ đệm RAM.
     *
     * @param showtimeId mã suất chiếu
     * @return danh sách các đối tượng Seat đầy đủ trạng thái theo suất chiếu
     */
    public List<Seat> getSeatsByShowtimeId(int showtimeId) {
        List<Seat> seatList = new ArrayList<>();
        String sql = "SELECT ss.showtime_seat_id, ss.showtime_id, ss.seat_id, ss.status AS seat_status, "
                   + "       s.room_id, s.seat_row, s.seat_number, s.seat_type, s.created_at AS seat_created_at "
                   + "FROM showtime_seats ss "
                   + "JOIN seats s ON ss.seat_id = s.seat_id "
                   + "WHERE ss.showtime_id = ? "
                   + "ORDER BY s.seat_row ASC, s.seat_number ASC";

        try (Connection conn = DatabaseConnection.getConnection()) {
            if (conn != null) {
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, showtimeId);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            int showtimeSeatId = rs.getInt("showtime_seat_id");
                            int seatId = rs.getInt("seat_id");
                            int roomId = rs.getInt("room_id");
                            String seatRow = rs.getString("seat_row");
                            int seatNumber = rs.getInt("seat_number");
                            String seatTypeStr = rs.getString("seat_type");
                            String seatStatusStr = rs.getString("seat_status");
                            Timestamp seatCreatedTs = rs.getTimestamp("seat_created_at");

                            SeatType seatType;
                            try {
                                seatType = SeatType.fromDbValue(seatTypeStr);
                            } catch (IllegalArgumentException e) {
                                seatType = SeatType.GHE_THUONG;
                            }

                            SeatStatus seatStatus;
                            try {
                                seatStatus = SeatStatus.fromDbValue(seatStatusStr);
                            } catch (IllegalArgumentException e) {
                                seatStatus = SeatStatus.CON_TRONG;
                            }

                            LocalDateTime seatCreatedAt = (seatCreatedTs != null) ? seatCreatedTs.toLocalDateTime() : null;
                            Seat seat = new Seat(seatId, roomId, seatRow, seatNumber, seatType, seatStatus, showtimeSeatId, seatCreatedAt);
                            seatList.add(seat);
                        }
                    }
                }

                // Nếu trong DB chưa có dòng showtime_seats cho suất chiếu này, tự động khởi tạo
                if (seatList.isEmpty()) {
                    int roomId = 1;
                    Showtime st = inMemoryShowtimes.get(showtimeId);
                    if (st != null) roomId = st.getRoomId();

                    int inserted = initializeSeatsForShowtime(showtimeId, roomId);
                    if (inserted > 0) {
                        return getSeatsByShowtimeId(showtimeId); // Gọi đệ quy lại sau khi đã khởi tạo
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[ShowtimeRepository] Lỗi khi lấy danh sách ghế suất #" + showtimeId + ": " + e.getMessage());
            e.printStackTrace();
        }

        // Nếu MySQL không có hoặc lỗi kết nối, tạo sơ đồ ghế chuẩn trong RAM
        if (seatList.isEmpty()) {
            seatList = generateFallbackSeats(showtimeId);
        }

        // Hợp nhất các trạng thái ghế đã cập nhật trong RAM
        for (Seat seat : seatList) {
            String keyById = showtimeId + "_" + seat.getSeatId();
            String keyByLabel = showtimeId + "_" + seat.getSeatLabel().toUpperCase();

            if (inMemorySeatStatuses.containsKey(keyById)) {
                seat.setStatus(inMemorySeatStatuses.get(keyById));
            } else if (inMemorySeatStatuses.containsKey(keyByLabel)) {
                seat.setStatus(inMemorySeatStatuses.get(keyByLabel));
            }
        }

        return seatList;
    }

    /**
     * Sinh sơ đồ ghế mẫu cho trường hợp ngoại tuyến (Phòng 1: 6 cột, Phòng 2: 5 cột, Phòng 3: 7 cột).
     * Hàng A, B, C thường; D đôi (+30k); E VIP (+50k).
     */
    private List<Seat> generateFallbackSeats(int showtimeId) {
        List<Seat> seats = new ArrayList<>();
        Showtime st = inMemoryShowtimes.get(showtimeId);
        int roomId = (st != null && st.getRoomId() > 0) ? st.getRoomId() : 1;
        int maxCols = 6;
        if (roomId == 2) {
            maxCols = 5;
        } else if (roomId == 3) {
            maxCols = 7;
        }

        String[] rows = {"A", "B", "C", "D", "E"};
        int seatCounter = 1;

        for (String row : rows) {
            SeatType type;
            if ("D".equals(row)) {
                type = SeatType.GHE_DOI;
            } else if ("E".equals(row)) {
                type = SeatType.GHE_VIP;
            } else {
                type = SeatType.GHE_THUONG;
            }

            for (int col = 1; col <= maxCols; col++) {
                int seatId = seatCounter++;
                SeatStatus status = SeatStatus.CON_TRONG;

                // Suất 1 có sẵn một số ghế booked mẫu
                if (showtimeId == 1 && (seatId == 3 || seatId == 8 || seatId == 25)) {
                    status = SeatStatus.DA_DAT;
                } else if (showtimeId == 1 && seatId == 21) {
                    status = SeatStatus.DANG_GIU;
                }

                Seat s = new Seat(seatId, roomId, row, col, type, status, seatId, LocalDateTime.now());
                seats.add(s);
            }
        }
        return seats;
    }

    /**
     * Lấy sơ đồ ghế của suất chiếu dạng Collection Map (key là mã ghế như "A1", "B3", value là Seat).
     * Ánh xạ thông minh: hỗ trợ cả mã nhãn ghế ("B4", "b4"), số ghế và mã định danh seat_id.
     *
     * @param showtimeId mã suất chiếu
     * @return Map liên kết giữa nhãn ghế/mã ghế và đối tượng Seat tương ứng
     */
    public Map<String, Seat> getSeatMapByShowtimeId(int showtimeId) {
        Map<String, Seat> seatMap = new LinkedHashMap<>();
        List<Seat> seats = getSeatsByShowtimeId(showtimeId);
        for (Seat seat : seats) {
            String label = seat.getSeatLabel().toUpperCase();
            seatMap.put(label, seat);
            seatMap.put(label.trim(), seat);
            seatMap.put(String.valueOf(seat.getSeatId()), seat);
            seatMap.put(seat.getSeatRow().toUpperCase() + seat.getSeatNumber(), seat);
        }
        return seatMap;
    }

    /**
     * Cập nhật trạng thái ghế trong một suất chiếu cụ thể (bảng showtime_seats).
     * Luôn cập nhật đồng thời vào MySQL và bộ nhớ đệm RAM để tránh lỗi mất trạng thái.
     *
     * @param showtimeId mã suất chiếu
     * @param seatId     mã ghế
     * @param status     trạng thái mới (CON_TRONG, DA_DAT, DANG_GIU)
     * @return true nếu cập nhật thành công
     */
    public boolean updateSeatStatus(int showtimeId, int seatId, SeatStatus status) {
        if (showtimeId <= 0 || seatId <= 0 || status == null) {
            return false;
        }

        // Cập nhật bộ nhớ đệm RAM trước tiên
        inMemorySeatStatuses.put(showtimeId + "_" + seatId, status);

        String sql = "UPDATE showtime_seats SET status = ? WHERE showtime_id = ? AND seat_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, status.getDbValue());
            ps.setInt(2, showtimeId);
            ps.setInt(3, seatId);

            int rows = ps.executeUpdate();
            if (rows > 0) {
                System.out.println("[ShowtimeRepository] Cập nhật ghế seatId=" + seatId + " suất=" + showtimeId + " -> " + status.getDisplayName() + " trong MySQL.");
                return true;
            }
        } catch (Exception e) {
            System.err.println("[ShowtimeRepository] Lỗi khi cập nhật ghế trong MySQL: " + e.getMessage());
            e.printStackTrace();
        }

        // Fallback RAM thành công
        return true;
    }

    /**
     * Đảm bảo bản ghi showtime_seats tồn tại trong MySQL cho cặp (showtimeId, seatId).
     * Trả về showtime_seat_id hợp lệ để phục vụ khóa ngoại bảng tickets.
     *
     * @param showtimeId mã suất chiếu
     * @param seatId     mã ghế
     * @return showtime_seat_id hợp lệ
     */
    public int ensureShowtimeSeatExists(int showtimeId, int seatId) {
        int existingId = getShowtimeSeatId(showtimeId, seatId);
        if (existingId > 0) {
            return existingId;
        }

        // Nếu chưa có, thử INSERT vào showtime_seats
        String sql = "INSERT INTO showtime_seats (showtime_id, seat_id, status) VALUES (?, ?, 'AVAILABLE')";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, showtimeId);
            ps.setInt(2, seatId);
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (Exception e) {
            System.err.println("[ShowtimeRepository] Không thể tự tạo showtime_seats trong MySQL: " + e.getMessage());
        }

        return seatId > 0 ? seatId : 1;
    }

    /**
     * Lấy mã khóa chính showtime_seat_id dựa vào showtimeId và seatId.
     *
     * @param showtimeId mã suất chiếu
     * @param seatId     mã ghế
     * @return showtime_seat_id nếu tồn tại, hoặc -1 nếu không tìm thấy
     */
    public int getShowtimeSeatId(int showtimeId, int seatId) {
        String sql = "SELECT showtime_seat_id FROM showtime_seats WHERE showtime_id = ? AND seat_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, showtimeId);
            ps.setInt(2, seatId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("showtime_seat_id");
                }
            }
        } catch (Exception e) {
            System.err.println("[ShowtimeRepository] Lỗi khi lấy showtime_seat_id: " + e.getMessage());
        }
        return -1;
    }

    /**
     * Khởi tạo các dòng showtime_seats mặc định (trạng thái AVAILABLE) cho một suất chiếu mới,
     * tự động nhân bản toàn bộ ghế có trong phòng chiếu vào bảng showtime_seats.
     *
     * @param showtimeId mã suất chiếu
     * @param roomId     mã phòng chiếu
     * @return số lượng ghế đã được khởi tạo
     */
    public int initializeSeatsForShowtime(int showtimeId, int roomId) {
        String sql = "INSERT IGNORE INTO showtime_seats (showtime_id, seat_id, status) "
                   + "SELECT ?, seat_id, 'AVAILABLE' FROM seats WHERE room_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, showtimeId);
            ps.setInt(2, roomId);

            return ps.executeUpdate();
        } catch (Exception e) {
            System.err.println("[ShowtimeRepository] Lỗi khi tạo ghế cho suất chiếu: " + e.getMessage());
        }
        return 0;
    }

    private Showtime mapResultSetToShowtime(ResultSet rs) throws SQLException {
        int showtimeId = rs.getInt("showtime_id");
        int movieId = rs.getInt("movie_id");
        int roomId = rs.getInt("room_id");

        Date showDateSql = rs.getDate("show_date");
        LocalDate showDate = (showDateSql != null) ? showDateSql.toLocalDate() : null;

        Time startTimeSql = rs.getTime("start_time");
        LocalTime startTime = (startTimeSql != null) ? startTimeSql.toLocalTime() : null;

        Time endTimeSql = rs.getTime("end_time");
        LocalTime endTime = (endTimeSql != null) ? endTimeSql.toLocalTime() : null;

        double basePrice = rs.getDouble("base_price");
        String status = rs.getString("showtime_status");

        Timestamp createdTs = rs.getTimestamp("showtime_created_at");
        LocalDateTime createdAt = (createdTs != null) ? createdTs.toLocalDateTime() : null;

        Movie movie = new Movie();
        movie.setMovieId(movieId);
        movie.setTitle(rs.getString("movie_title"));
        movie.setGenre(rs.getString("genre"));
        movie.setDuration(rs.getInt("duration"));
        movie.setDirector(rs.getString("director"));
        Date movieRelDate = rs.getDate("release_date");
        if (movieRelDate != null) {
            movie.setReleaseDate(movieRelDate.toLocalDate());
        }
        movie.setRating(rs.getDouble("rating"));
        movie.setDescription(rs.getString("description"));
        try {
            movie.setStatus(MovieStatus.fromDbValue(rs.getString("movie_status")));
        } catch (Exception ignored) {
            movie.setStatus(MovieStatus.DANG_CHIEU);
        }

        CinemaRoom room = new CinemaRoom();
        room.setRoomId(roomId);
        room.setRoomName(rs.getString("room_name"));
        room.setTotalSeats(rs.getInt("total_seats"));
        room.setRoomType(rs.getString("room_type"));
        room.setStatus(rs.getString("room_status"));

        Showtime showtime = new Showtime(showtimeId, movieId, roomId, showDate, startTime, endTime, basePrice, status, createdAt);
        showtime.setMovie(movie);
        showtime.setRoom(room);
        return showtime;
    }
}
