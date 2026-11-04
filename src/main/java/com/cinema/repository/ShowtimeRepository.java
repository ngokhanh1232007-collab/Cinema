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
 * Repository quản lý truy vấn dữ liệu bảng showtimes và showtime_seats trong MySQL.
 * Cung cấp các thao tác tìm kiếm suất chiếu, tải danh sách ghế kèm trạng thái,
 * và cập nhật trạng thái đặt ghế cho từng suất chiếu.
 */
public class ShowtimeRepository {

    /**
     * Tìm suất chiếu theo mã định danh (showtime_id).
     * Tự động JOIN với bảng movies và cinema_rooms để nạp đầy đủ thông tin thực thể liên kết,
     * đồng thời nạp danh sách ghế và trạng thái tương ứng.
     *
     * @param showtimeId mã suất chiếu
     * @return đối tượng Showtime với đầy đủ thông tin, hoặc null nếu không tìm thấy
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
                    // Nạp danh sách ghế cho suất chiếu này
                    List<Seat> seats = getSeatsByShowtimeId(showtimeId);
                    showtime.setSeats(seats);
                    return showtime;
                }
            }
        } catch (SQLException e) {
            System.err.println("[ShowtimeRepository] Lỗi khi tìm suất chiếu theo id (" + showtimeId + "): " + e.getMessage());
        }
        return null;
    }

    /**
     * Lấy danh sách các suất chiếu của một phim cụ thể (chỉ lấy suất đang lên lịch 'SCHEDULED').
     * Sắp xếp theo ngày chiếu và giờ bắt đầu tăng dần.
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
                    Showtime showtime = mapResultSetToShowtime(rs);
                    list.add(showtime);
                }
            }
        } catch (SQLException e) {
            System.err.println("[ShowtimeRepository] Lỗi khi lấy suất chiếu theo mã phim (" + movieId + "): " + e.getMessage());
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
        } catch (SQLException e) {
            System.err.println("[ShowtimeRepository] Lỗi khi lấy danh sách suất chiếu: " + e.getMessage());
        }
        return list;
    }

    /**
     * Lấy danh sách ghế kèm trạng thái (CON_TRONG, DA_DAT, DANG_GIU) của một suất chiếu cụ thể.
     * Truy vấn kết hợp bảng showtime_seats và bảng seats.
     * Sắp xếp theo hàng ghế và số ghế để hiển thị sơ đồ chuẩn.
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

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

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
        } catch (SQLException e) {
            System.err.println("[ShowtimeRepository] Lỗi khi lấy danh sách ghế của suất chiếu (" + showtimeId + "): " + e.getMessage());
        }
        return seatList;
    }

    /**
     * Lấy sơ đồ ghế của suất chiếu dạng Collection Map (key là mã ghế như "A1", "B3", value là Seat).
     * Giúp tra cứu nhanh ghế theo tên khi người dùng chọn vị trí.
     *
     * @param showtimeId mã suất chiếu
     * @return Map liên kết giữa nhãn ghế (A1, A2...) và đối tượng Seat tương ứng
     */
    public Map<String, Seat> getSeatMapByShowtimeId(int showtimeId) {
        Map<String, Seat> seatMap = new LinkedHashMap<>();
        List<Seat> seats = getSeatsByShowtimeId(showtimeId);
        for (Seat seat : seats) {
            seatMap.put(seat.getSeatLabel().toUpperCase(), seat);
        }
        return seatMap;
    }

    /**
     * Cập nhật trạng thái ghế trong một suất chiếu cụ thể (bảng showtime_seats).
     *
     * @param showtimeId mã suất chiếu
     * @param seatId     mã ghế
     * @param status     trạng thái mới (CON_TRONG, DA_DAT, DANG_GIU)
     * @return true nếu cập nhật thành công, false nếu thất bại
     */
    public boolean updateSeatStatus(int showtimeId, int seatId, SeatStatus status) {
        if (showtimeId <= 0 || seatId <= 0 || status == null) {
            return false;
        }

        String sql = "UPDATE showtime_seats SET status = ? WHERE showtime_id = ? AND seat_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, status.getDbValue());
            ps.setInt(2, showtimeId);
            ps.setInt(3, seatId);

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[ShowtimeRepository] Lỗi cập nhật trạng thái ghế (showtime: " + showtimeId
                    + ", seat: " + seatId + "): " + e.getMessage());
        }
        return false;
    }

    /**
     * Cập nhật trạng thái ghế thông qua khóa chính showtime_seat_id.
     *
     * @param showtimeSeatId mã bản ghi showtime_seat
     * @param status         trạng thái mới
     * @return true nếu cập nhật thành công, false nếu thất bại
     */
    public boolean updateSeatStatusById(int showtimeSeatId, SeatStatus status) {
        if (showtimeSeatId <= 0 || status == null) {
            return false;
        }

        String sql = "UPDATE showtime_seats SET status = ? WHERE showtime_seat_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, status.getDbValue());
            ps.setInt(2, showtimeSeatId);

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[ShowtimeRepository] Lỗi cập nhật showtime_seat_id (" + showtimeSeatId + "): " + e.getMessage());
        }
        return false;
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
        } catch (SQLException e) {
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
        } catch (SQLException e) {
            System.err.println("[ShowtimeRepository] Lỗi khi tạo ghế cho suất chiếu: " + e.getMessage());
        }
        return 0;
    }

    /**
     * Ánh xạ ResultSet thành đối tượng Showtime (bao gồm Movie và CinemaRoom cơ bản).
     *
     * @param rs ResultSet đang trỏ tới bản ghi
     * @return đối tượng Showtime
     * @throws SQLException nếu có lỗi đọc cột
     */
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

        // Xây dựng đối tượng Movie liên kết
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

        // Xây dựng đối tượng CinemaRoom liên kết
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

