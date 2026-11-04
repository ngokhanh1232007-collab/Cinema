package com.cinema.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Lớp đại diện cho phòng chiếu phim.
 * Tương ứng với bảng cinema_rooms trong database.
 * Mỗi phòng chiếu chứa một danh sách ghế (composition).
 *
 * <p>Encapsulation: tất cả thuộc tính private, truy xuất qua getter/setter.</p>
 */
public class CinemaRoom {

    /** Mã phòng chiếu (auto increment trong DB) */
    private int roomId;

    /** Tên phòng chiếu (ví dụ: "Phòng 1 - Standard 2D") */
    private String roomName;

    /** Tổng số ghế trong phòng */
    private int totalSeats;

    /** Loại phòng chiếu (2D, 3D, IMAX) */
    private String roomType;

    /** Trạng thái phòng (ACTIVE, MAINTENANCE) */
    private String status;

    /** Ngày tạo bản ghi */
    private LocalDateTime createdAt;

    /** Danh sách ghế trong phòng (quan hệ composition) */
    private List<Seat> seats;

    /**
     * Constructor mặc định.
     */
    public CinemaRoom() {
        this.seats = new ArrayList<>();
        this.status = "ACTIVE";
    }

    /**
     * Constructor với thông tin cơ bản.
     *
     * @param roomId     mã phòng
     * @param roomName   tên phòng
     * @param totalSeats tổng số ghế
     * @param roomType   loại phòng (2D, 3D, IMAX)
     */
    public CinemaRoom(int roomId, String roomName, int totalSeats, String roomType) {
        this.roomId = roomId;
        this.roomName = roomName;
        this.totalSeats = totalSeats;
        this.roomType = roomType;
        this.status = "ACTIVE";
        this.seats = new ArrayList<>();
    }

    /**
     * Constructor đầy đủ tham số.
     *
     * @param roomId     mã phòng
     * @param roomName   tên phòng
     * @param totalSeats tổng số ghế
     * @param roomType   loại phòng
     * @param status     trạng thái
     * @param createdAt  ngày tạo
     */
    public CinemaRoom(int roomId, String roomName, int totalSeats,
                      String roomType, String status, LocalDateTime createdAt) {
        this.roomId = roomId;
        this.roomName = roomName;
        this.totalSeats = totalSeats;
        this.roomType = roomType;
        this.status = status;
        this.createdAt = createdAt;
        this.seats = new ArrayList<>();
    }

    /**
     * Thêm một ghế vào phòng chiếu.
     *
     * @param seat ghế cần thêm
     */
    public void addSeat(Seat seat) {
        if (seat != null) {
            this.seats.add(seat);
        }
    }

    /**
     * Đếm số ghế còn trống trong phòng.
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
     * Kiểm tra phòng chiếu có đang hoạt động không.
     *
     * @return true nếu phòng đang hoạt động
     */
    public boolean isActive() {
        return "ACTIVE".equalsIgnoreCase(status);
    }

    /**
     * Trả về thông tin phòng chiếu dạng hiển thị.
     *
     * @return chuỗi thông tin phòng
     */
    public String getDisplayInfo() {
        return String.format("[%d] %s | Loại: %s | Tổng ghế: %d | Trạng thái: %s",
                roomId, roomName, roomType, totalSeats, status);
    }

    // ===================== GETTER / SETTER =====================

    public int getRoomId() {
        return roomId;
    }

    public void setRoomId(int roomId) {
        this.roomId = roomId;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public int getTotalSeats() {
        return totalSeats;
    }

    public void setTotalSeats(int totalSeats) {
        this.totalSeats = totalSeats;
    }

    public String getRoomType() {
        return roomType;
    }

    public void setRoomType(String roomType) {
        this.roomType = roomType;
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
        return "CinemaRoom{" +
                "roomId=" + roomId +
                ", roomName='" + roomName + '\'' +
                ", totalSeats=" + totalSeats +
                ", roomType='" + roomType + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}

