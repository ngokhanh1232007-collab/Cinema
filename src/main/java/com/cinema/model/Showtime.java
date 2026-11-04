package com.cinema.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Lớp đại diện cho suất chiếu phim.
 * Tương ứng với bảng showtimes trong database.
 * Mỗi suất chiếu gắn với một phim, một phòng chiếu, và có danh sách ghế riêng.
 *
 * <p>Encapsulation: tất cả thuộc tính private, truy xuất qua getter/setter.</p>
 */
public class Showtime {

    /** Định dạng ngày hiển thị */
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /** Định dạng giờ hiển thị */
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    /** Mã suất chiếu (auto increment trong DB) */
    private int showtimeId;

    /** Mã phim (foreign key) */
    private int movieId;

    /** Mã phòng chiếu (foreign key) */
    private int roomId;

    /** Đối tượng phim liên kết (eager loading) */
    private Movie movie;

    /** Đối tượng phòng chiếu liên kết (eager loading) */
    private CinemaRoom room;

    /** Ngày chiếu */
    private LocalDate showDate;

    /** Giờ bắt đầu chiếu */
    private LocalTime startTime;

    /** Giờ kết thúc chiếu */
    private LocalTime endTime;

    /** Giá vé cơ bản (VND) */
    private double basePrice;

    /** Trạng thái suất chiếu (SCHEDULED, CANCELLED) */
    private String status;

    /** Ngày tạo bản ghi */
    private LocalDateTime createdAt;

    /** Danh sách ghế theo suất chiếu (từ bảng showtime_seats) */
    private List<Seat> seats;

    /**
     * Constructor mặc định.
     */
    public Showtime() {
        this.status = "SCHEDULED";
        this.seats = new ArrayList<>();
    }

    /**
     * Constructor với thông tin cơ bản.
     *
     * @param showtimeId mã suất chiếu
     * @param movieId    mã phim
     * @param roomId     mã phòng
     * @param showDate   ngày chiếu
     * @param startTime  giờ bắt đầu
     * @param endTime    giờ kết thúc
     * @param basePrice  giá vé cơ bản
     */
    public Showtime(int showtimeId, int movieId, int roomId,
                    LocalDate showDate, LocalTime startTime, LocalTime endTime,
                    double basePrice) {
        this.showtimeId = showtimeId;
        this.movieId = movieId;
        this.roomId = roomId;
        this.showDate = showDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.basePrice = basePrice;
        this.status = "SCHEDULED";
        this.seats = new ArrayList<>();
    }

    /**
     * Constructor đầy đủ tham số.
     *
     * @param showtimeId mã suất chiếu
     * @param movieId    mã phim
     * @param roomId     mã phòng
     * @param showDate   ngày chiếu
     * @param startTime  giờ bắt đầu
     * @param endTime    giờ kết thúc
     * @param basePrice  giá vé cơ bản
     * @param status     trạng thái
     * @param createdAt  ngày tạo
     */
    public Showtime(int showtimeId, int movieId, int roomId,
                    LocalDate showDate, LocalTime startTime, LocalTime endTime,
                    double basePrice, String status, LocalDateTime createdAt) {
        this.showtimeId = showtimeId;
        this.movieId = movieId;
        this.roomId = roomId;
        this.showDate = showDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.basePrice = basePrice;
        this.status = status;
        this.createdAt = createdAt;
        this.seats = new ArrayList<>();
    }

    /**
     * Kiểm tra suất chiếu có đang hoạt động (chưa bị hủy) không.
     *
     * @return true nếu suất chiếu đang hoạt động
     */
    public boolean isScheduled() {
        return "SCHEDULED".equalsIgnoreCase(status);
    }

    /**
     * Kiểm tra suất chiếu đã bị hủy chưa.
     *
     * @return true nếu suất chiếu đã bị hủy
     */
    public boolean isCancelled() {
        return "CANCELLED".equalsIgnoreCase(status);
    }

    /**
     * Thêm một ghế vào danh sách ghế suất chiếu.
     *
     * @param seat ghế cần thêm
     */
    public void addSeat(Seat seat) {
        if (seat != null) {
            this.seats.add(seat);
        }
    }

    /**
     * Đếm số ghế còn trống trong suất chiếu.
     *
     * @return số ghế còn trống
     */
    public int countAvailableSeats() {
        int count = 0;
        for (Seat seat : seats) {
            if (seat.isAvailable()) {
                count++;
            }
        }
        return count;
    }

    /**
     * Lấy ngày chiếu dạng chuỗi hiển thị (dd/MM/yyyy).
     *
     * @return ngày chiếu formatted
     */
    public String getShowDateFormatted() {
        return showDate != null ? showDate.format(DATE_FORMAT) : "";
    }

    /**
     * Lấy giờ bắt đầu dạng chuỗi hiển thị (HH:mm).
     *
     * @return giờ bắt đầu formatted
     */
    public String getStartTimeFormatted() {
        return startTime != null ? startTime.format(TIME_FORMAT) : "";
    }

    /**
     * Lấy giờ kết thúc dạng chuỗi hiển thị (HH:mm).
     *
     * @return giờ kết thúc formatted
     */
    public String getEndTimeFormatted() {
        return endTime != null ? endTime.format(TIME_FORMAT) : "";
    }

    /**
     * Trả về thông tin suất chiếu dạng hiển thị.
     *
     * @return chuỗi thông tin suất chiếu
     */
    public String getDisplayInfo() {
        String movieTitle = (movie != null) ? movie.getTitle() : "Phim #" + movieId;
        String roomName = (room != null) ? room.getRoomName() : "Phòng #" + roomId;
        return String.format("[%d] %s | %s | Ngày: %s | %s - %s | Giá: %,.0f VND",
                showtimeId, movieTitle, roomName,
                getShowDateFormatted(), getStartTimeFormatted(), getEndTimeFormatted(),
                basePrice);
    }

    // ===================== GETTER / SETTER =====================

    public int getShowtimeId() {
        return showtimeId;
    }

    public void setShowtimeId(int showtimeId) {
        this.showtimeId = showtimeId;
    }

    public int getMovieId() {
        return movieId;
    }

    public void setMovieId(int movieId) {
        this.movieId = movieId;
    }

    public int getRoomId() {
        return roomId;
    }

    public void setRoomId(int roomId) {
        this.roomId = roomId;
    }

    public Movie getMovie() {
        return movie;
    }

    public void setMovie(Movie movie) {
        this.movie = movie;
    }

    public CinemaRoom getRoom() {
        return room;
    }

    public void setRoom(CinemaRoom room) {
        this.room = room;
    }

    public LocalDate getShowDate() {
        return showDate;
    }

    public void setShowDate(LocalDate showDate) {
        this.showDate = showDate;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public double getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(double basePrice) {
        this.basePrice = basePrice;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<Seat> getSeats() {
        return seats;
    }

    public void setSeats(List<Seat> seats) {
        this.seats = seats;
    }

    @Override
    public String toString() {
        return "Showtime{" +
                "showtimeId=" + showtimeId +
                ", movieId=" + movieId +
                ", roomId=" + roomId +
                ", showDate=" + showDate +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", basePrice=" + basePrice +
                ", status='" + status + '\'' +
                '}';
    }
}

