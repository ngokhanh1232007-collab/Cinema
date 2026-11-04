package com.cinema.model;

import java.time.LocalDateTime;

/**
 * Lớp đại diện cho một ghế ngồi trong phòng chiếu.
 * Tương ứng với bảng seats trong database.
 * Trạng thái ghế (status) được quản lý theo từng suất chiếu qua bảng showtime_seats.
 *
 * <p>Encapsulation: tất cả thuộc tính private, truy xuất qua getter/setter.</p>
 */
public class Seat {

    /** Mã ghế (auto increment trong DB) */
    private int seatId;

    /** Mã phòng chiếu chứa ghế này */
    private int roomId;

    /** Hàng ghế (A, B, C, ...) */
    private String seatRow;

    /** Số ghế trong hàng (1, 2, 3, ...) */
    private int seatNumber;

    /** Loại ghế (GHE_THUONG, GHE_DOI, GHE_VIP) */
    private SeatType seatType;

    /** Trạng thái ghế trong suất chiếu cụ thể (CON_TRONG, DA_DAT, DANG_GIU) */
    private SeatStatus status;

    /** Mã showtime_seat (từ bảng showtime_seats, dùng khi truy vấn theo suất chiếu) */
    private int showtimeSeatId;

    /** Ngày tạo bản ghi */
    private LocalDateTime createdAt;

    /**
     * Constructor mặc định.
     */
    public Seat() {
        this.seatType = SeatType.GHE_THUONG;
        this.status = SeatStatus.CON_TRONG;
    }

    /**
     * Constructor với thông tin cơ bản.
     *
     * @param seatId     mã ghế
     * @param roomId     mã phòng chiếu
     * @param seatRow    hàng ghế
     * @param seatNumber số ghế
     * @param seatType   loại ghế
     */
    public Seat(int seatId, int roomId, String seatRow, int seatNumber, SeatType seatType) {
        this.seatId = seatId;
        this.roomId = roomId;
        this.seatRow = seatRow;
        this.seatNumber = seatNumber;
        this.seatType = seatType;
        this.status = SeatStatus.CON_TRONG;
    }

    /**
     * Constructor đầy đủ tham số (bao gồm trạng thái theo suất chiếu).
     *
     * @param seatId         mã ghế
     * @param roomId         mã phòng chiếu
     * @param seatRow        hàng ghế
     * @param seatNumber     số ghế
     * @param seatType       loại ghế
     * @param status         trạng thái ghế
     * @param showtimeSeatId mã showtime_seat
     * @param createdAt      ngày tạo
     */
    public Seat(int seatId, int roomId, String seatRow, int seatNumber,
                SeatType seatType, SeatStatus status, int showtimeSeatId,
                LocalDateTime createdAt) {
        this.seatId = seatId;
        this.roomId = roomId;
        this.seatRow = seatRow;
        this.seatNumber = seatNumber;
        this.seatType = seatType;
        this.status = status;
        this.showtimeSeatId = showtimeSeatId;
        this.createdAt = createdAt;
    }

    /**
     * Kiểm tra ghế có còn trống không.
     *
     * @return true nếu ghế còn trống
     */
    public boolean isAvailable() {
        return status == SeatStatus.CON_TRONG;
    }

    /**
     * Kiểm tra ghế đã được đặt chưa.
     *
     * @return true nếu ghế đã đặt
     */
    public boolean isBooked() {
        return status == SeatStatus.DA_DAT;
    }

    /**
     * Lấy tên hiển thị của ghế (ví dụ: "A3", "D1").
     *
     * @return tên ghế dạng hàng + số
     */
    public String getSeatLabel() {
        return seatRow + seatNumber;
    }

    /**
     * Lấy thông tin hiển thị đầy đủ của ghế.
     *
     * @return chuỗi thông tin (ví dụ: "A3 - Ghế thường [Còn trống]")
     */
    public String getDisplayInfo() {
        return String.format("%s - %s [%s]",
                getSeatLabel(), seatType.getDisplayName(), status.getDisplayName());
    }

    // ===================== GETTER / SETTER =====================

    public int getSeatId() {
        return seatId;
    }

    public void setSeatId(int seatId) {
        this.seatId = seatId;
    }

    public int getRoomId() {
        return roomId;
    }

    public void setRoomId(int roomId) {
        this.roomId = roomId;
    }

    public String getSeatRow() {
        return seatRow;
    }

    public void setSeatRow(String seatRow) {
        this.seatRow = seatRow;
    }

    public int getSeatNumber() {
        return seatNumber;
    }

    public void setSeatNumber(int seatNumber) {
        this.seatNumber = seatNumber;
    }

    public SeatType getSeatType() {
        return seatType;
    }

    public void setSeatType(SeatType seatType) {
        this.seatType = seatType;
    }

    public SeatStatus getStatus() {
        return status;
    }

    public void setStatus(SeatStatus status) {
        this.status = status;
    }

    public int getShowtimeSeatId() {
        return showtimeSeatId;
    }

    public void setShowtimeSeatId(int showtimeSeatId) {
        this.showtimeSeatId = showtimeSeatId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Seat{" +
                "seatId=" + seatId +
                ", roomId=" + roomId +
                ", seat=" + getSeatLabel() +
                ", seatType=" + seatType +
                ", status=" + status +
                '}';
    }
}

