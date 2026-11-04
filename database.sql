-- =====================================================================
-- HỆ THỐNG QUẢN LÝ MUA VÉ XEM PHIM CHO SINH VIÊN - NHÓM 06
-- Database: cinema_booking_db
-- =====================================================================

DROP DATABASE IF EXISTS cinema_booking_db;
CREATE DATABASE cinema_booking_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE cinema_booking_db;

-- =====================================================================
-- 1. BẢNG CUSTOMERS (Khách hàng: REGULAR, STUDENT, VIP)
-- =====================================================================
CREATE TABLE customers (
    customer_id     INT AUTO_INCREMENT PRIMARY KEY,
    full_name       VARCHAR(100)    NOT NULL,
    email           VARCHAR(150)    NOT NULL UNIQUE,
    phone           VARCHAR(15)     NOT NULL,
    customer_type   ENUM('REGULAR', 'STUDENT', 'VIP') NOT NULL DEFAULT 'REGULAR',
    student_id      VARCHAR(20)     NULL COMMENT 'Mã số sinh viên (chỉ dành cho STUDENT)',
    date_of_birth   DATE            NULL,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- =====================================================================
-- 2. BẢNG MOVIES (Phim)
-- =====================================================================
CREATE TABLE movies (
    movie_id        INT AUTO_INCREMENT PRIMARY KEY,
    title           VARCHAR(200)    NOT NULL,
    genre           VARCHAR(100)    NOT NULL,
    duration        INT             NOT NULL COMMENT 'Thời lượng phim (phút)',
    director        VARCHAR(100)    NOT NULL,
    release_date    DATE            NOT NULL,
    rating          DECIMAL(3,1)    NULL COMMENT 'Điểm đánh giá (0.0 - 10.0)',
    description     TEXT            NULL,
    status          ENUM('SHOWING', 'STOPPED') NOT NULL DEFAULT 'SHOWING',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- =====================================================================
-- 3. BẢNG CINEMA_ROOMS (Phòng chiếu)
-- =====================================================================
CREATE TABLE cinema_rooms (
    room_id         INT AUTO_INCREMENT PRIMARY KEY,
    room_name       VARCHAR(50)     NOT NULL UNIQUE,
    total_seats     INT             NOT NULL,
    room_type       ENUM('2D', '3D', 'IMAX') NOT NULL DEFAULT '2D',
    status          ENUM('ACTIVE', 'MAINTENANCE') NOT NULL DEFAULT 'ACTIVE',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- =====================================================================
-- 4. BẢNG SEATS (Ghế ngồi trong phòng chiếu)
-- =====================================================================
CREATE TABLE seats (
    seat_id         INT AUTO_INCREMENT PRIMARY KEY,
    room_id         INT             NOT NULL,
    seat_row        CHAR(1)         NOT NULL COMMENT 'Hàng ghế: A, B, C, ...',
    seat_number     INT             NOT NULL COMMENT 'Số ghế trong hàng',
    seat_type       ENUM('REGULAR', 'DOUBLE', 'VIP') NOT NULL DEFAULT 'REGULAR',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_seat_position (room_id, seat_row, seat_number),
    CONSTRAINT fk_seat_room FOREIGN KEY (room_id) REFERENCES cinema_rooms(room_id)
        ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB;

-- =====================================================================
-- 5. BẢNG SHOWTIMES (Suất chiếu)
-- =====================================================================
CREATE TABLE showtimes (
    showtime_id     INT AUTO_INCREMENT PRIMARY KEY,
    movie_id        INT             NOT NULL,
    room_id         INT             NOT NULL,
    show_date       DATE            NOT NULL,
    start_time      TIME            NOT NULL,
    end_time        TIME            NOT NULL,
    base_price      DECIMAL(12,2)   NOT NULL COMMENT 'Giá vé cơ bản (VND)',
    status          ENUM('SCHEDULED', 'CANCELLED') NOT NULL DEFAULT 'SCHEDULED',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_showtime_movie FOREIGN KEY (movie_id) REFERENCES movies(movie_id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_showtime_room FOREIGN KEY (room_id) REFERENCES cinema_rooms(room_id)
        ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB;

-- =====================================================================
-- 6. BẢNG SHOWTIME_SEATS (Trạng thái ghế theo từng suất chiếu)
-- =====================================================================
CREATE TABLE showtime_seats (
    showtime_seat_id INT AUTO_INCREMENT PRIMARY KEY,
    showtime_id      INT            NOT NULL,
    seat_id          INT            NOT NULL,
    status           ENUM('AVAILABLE', 'BOOKED', 'RESERVED') NOT NULL DEFAULT 'AVAILABLE',
    updated_at       DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_showtime_seat (showtime_id, seat_id),
    CONSTRAINT fk_ss_showtime FOREIGN KEY (showtime_id) REFERENCES showtimes(showtime_id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_ss_seat FOREIGN KEY (seat_id) REFERENCES seats(seat_id)
        ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB;

-- =====================================================================
-- 7. BẢNG BOOKINGS (Đơn đặt vé)
-- =====================================================================
CREATE TABLE bookings (
    booking_id      INT AUTO_INCREMENT PRIMARY KEY,
    booking_code    VARCHAR(20)     NOT NULL UNIQUE COMMENT 'Mã đơn hàng (BK-yyyyMMdd-XXXX)',
    customer_id     INT             NOT NULL,
    showtime_id     INT             NOT NULL,
    total_amount    DECIMAL(12,2)   NOT NULL DEFAULT 0 COMMENT 'Tổng tiền (VND)',
    discount_amount DECIMAL(12,2)   NOT NULL DEFAULT 0 COMMENT 'Số tiền giảm giá',
    final_amount    DECIMAL(12,2)   NOT NULL DEFAULT 0 COMMENT 'Thành tiền sau giảm',
    status          ENUM('PENDING', 'CONFIRMED', 'CANCELLED') NOT NULL DEFAULT 'PENDING',
    booking_time    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_booking_customer FOREIGN KEY (customer_id) REFERENCES customers(customer_id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_booking_showtime FOREIGN KEY (showtime_id) REFERENCES showtimes(showtime_id)
        ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB;

-- =====================================================================
-- 8. BẢNG TICKETS (Vé xem phim)
-- =====================================================================
CREATE TABLE tickets (
    ticket_id       INT AUTO_INCREMENT PRIMARY KEY,
    ticket_code     VARCHAR(20)     NOT NULL UNIQUE COMMENT 'Mã vé (TK-yyyyMMdd-XXXX)',
    booking_id      INT             NOT NULL,
    showtime_seat_id INT            NOT NULL,
    seat_price      DECIMAL(12,2)   NOT NULL COMMENT 'Giá vé đã tính theo loại ghế',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ticket_booking FOREIGN KEY (booking_id) REFERENCES bookings(booking_id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_ticket_showtime_seat FOREIGN KEY (showtime_seat_id) REFERENCES showtime_seats(showtime_seat_id)
        ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB;

-- =====================================================================
-- 9. BẢNG PAYMENTS (Thanh toán)
-- =====================================================================
CREATE TABLE payments (
    payment_id      INT AUTO_INCREMENT PRIMARY KEY,
    payment_code    VARCHAR(20)     NOT NULL UNIQUE COMMENT 'Mã thanh toán (PM-yyyyMMdd-XXXX)',
    booking_id      INT             NOT NULL,
    amount          DECIMAL(12,2)   NOT NULL,
    payment_method  ENUM('CASH', 'CREDIT_CARD', 'E_WALLET', 'BANK_TRANSFER') NOT NULL DEFAULT 'CASH',
    payment_time    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status          ENUM('SUCCESS', 'FAILED', 'REFUNDED') NOT NULL DEFAULT 'SUCCESS',
    CONSTRAINT fk_payment_booking FOREIGN KEY (booking_id) REFERENCES bookings(booking_id)
        ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB;


-- =====================================================================
-- DỮ LIỆU MẪU
-- =====================================================================

-- ----- CUSTOMERS (3 loại: REGULAR, STUDENT, VIP) -----
INSERT INTO customers (full_name, email, phone, customer_type, student_id, date_of_birth) VALUES
('Nguyễn Văn An',       'an.nguyen@gmail.com',      '0901234567', 'REGULAR',  NULL,           '1995-03-15'),
('Trần Thị Bích',       'bich.tran@gmail.com',       '0912345678', 'REGULAR',  NULL,           '1998-07-22'),
('Lê Hoàng Nam',        'nam.le@student.edu.vn',     '0923456789', 'STUDENT',  'SV20210001',   '2003-01-10'),
('Phạm Minh Tú',        'tu.pham@student.edu.vn',    '0934567890', 'STUDENT',  'SV20210042',   '2003-05-28'),
('Đỗ Thị Hương',        'huong.do@student.edu.vn',   '0945678901', 'STUDENT',  'SV20220115',   '2004-11-03'),
('Vũ Quốc Bảo',         'bao.vu@gmail.com',          '0956789012', 'VIP',      NULL,           '1990-09-18'),
('Hoàng Thị Mai Linh',  'linh.hoang@gmail.com',      '0967890123', 'VIP',      NULL,           '1988-12-05');

-- ----- MOVIES (Đang chiếu và Ngừng chiếu) -----
INSERT INTO movies (title, genre, duration, director, release_date, rating, description, status) VALUES
('Lật Mặt 7: Một Điều Ước',      'Hành động, Gia đình',   130, 'Lý Hải',            '2024-04-26', 8.2, 'Phần 7 của loạt phim Lật Mặt, xoay quanh câu chuyện gia đình cảm động.',                'SHOWING'),
('Mai',                            'Tâm lý, Tình cảm',     130, 'Trấn Thành',         '2024-02-10', 7.5, 'Câu chuyện tình yêu và cuộc đời của cô gái tên Mai.',                                   'SHOWING'),
('Kung Fu Panda 4',                'Hoạt hình, Hài',        94,  'Mike Mitchell',      '2024-03-08', 7.0, 'Po đối mặt với phản diện mới – phù thủy Chameleon.',                                    'SHOWING'),
('Godzilla x Kong: Đế Chế Mới',   'Hành động, Viễn tưởng', 115, 'Adam Wingard',       '2024-03-29', 7.8, 'Godzilla và Kong hợp lực chống lại mối đe dọa từ lòng đất.',                            'SHOWING'),
('Dune: Hành Tinh Cát - Phần 2',  'Khoa học viễn tưởng',   166, 'Denis Villeneuve',   '2024-03-01', 8.5, 'Paul Atreides gia nhập người Fremen và tìm cách ngăn chặn tương lai khủng khiếp.',      'SHOWING'),
('Quật Mộ Trùng Ma',              'Kinh dị',               112, 'Trần Hữu Tấn',      '2023-10-20', 6.8, 'Phim kinh dị Việt Nam về truyền thuyết dân gian.',                                       'STOPPED'),
('Oppenheimer',                    'Tiểu sử, Lịch sử',     180, 'Christopher Nolan',  '2023-07-21', 8.9, 'Câu chuyện về cha đẻ bom nguyên tử J. Robert Oppenheimer.',                              'STOPPED');

-- ----- CINEMA_ROOMS (3 phòng chiếu) -----
INSERT INTO cinema_rooms (room_name, total_seats, room_type, status) VALUES
('Phòng 1 - Standard 2D',  30, '2D',   'ACTIVE'),
('Phòng 2 - Premium 3D',   25, '3D',   'ACTIVE'),
('Phòng 3 - IMAX',         35, 'IMAX', 'ACTIVE');

-- ----- SEATS (Phòng 1: 30 ghế, Phòng 2: 25 ghế, Phòng 3: 35 ghế) -----

-- Phòng 1 (room_id = 1): 5 hàng x 6 ghế
-- Hàng A, B, C: REGULAR | Hàng D: DOUBLE | Hàng E: VIP
INSERT INTO seats (room_id, seat_row, seat_number, seat_type) VALUES
-- Hàng A (REGULAR)
(1, 'A', 1, 'REGULAR'), (1, 'A', 2, 'REGULAR'), (1, 'A', 3, 'REGULAR'),
(1, 'A', 4, 'REGULAR'), (1, 'A', 5, 'REGULAR'), (1, 'A', 6, 'REGULAR'),
-- Hàng B (REGULAR)
(1, 'B', 1, 'REGULAR'), (1, 'B', 2, 'REGULAR'), (1, 'B', 3, 'REGULAR'),
(1, 'B', 4, 'REGULAR'), (1, 'B', 5, 'REGULAR'), (1, 'B', 6, 'REGULAR'),
-- Hàng C (REGULAR)
(1, 'C', 1, 'REGULAR'), (1, 'C', 2, 'REGULAR'), (1, 'C', 3, 'REGULAR'),
(1, 'C', 4, 'REGULAR'), (1, 'C', 5, 'REGULAR'), (1, 'C', 6, 'REGULAR'),
-- Hàng D (DOUBLE)
(1, 'D', 1, 'DOUBLE'), (1, 'D', 2, 'DOUBLE'), (1, 'D', 3, 'DOUBLE'),
(1, 'D', 4, 'DOUBLE'), (1, 'D', 5, 'DOUBLE'), (1, 'D', 6, 'DOUBLE'),
-- Hàng E (VIP)
(1, 'E', 1, 'VIP'), (1, 'E', 2, 'VIP'), (1, 'E', 3, 'VIP'),
(1, 'E', 4, 'VIP'), (1, 'E', 5, 'VIP'), (1, 'E', 6, 'VIP');

-- Phòng 2 (room_id = 2): 5 hàng x 5 ghế
-- Hàng A, B: REGULAR | Hàng C: DOUBLE | Hàng D, E: VIP
INSERT INTO seats (room_id, seat_row, seat_number, seat_type) VALUES
-- Hàng A (REGULAR)
(2, 'A', 1, 'REGULAR'), (2, 'A', 2, 'REGULAR'), (2, 'A', 3, 'REGULAR'),
(2, 'A', 4, 'REGULAR'), (2, 'A', 5, 'REGULAR'),
-- Hàng B (REGULAR)
(2, 'B', 1, 'REGULAR'), (2, 'B', 2, 'REGULAR'), (2, 'B', 3, 'REGULAR'),
(2, 'B', 4, 'REGULAR'), (2, 'B', 5, 'REGULAR'),
-- Hàng C (DOUBLE)
(2, 'C', 1, 'DOUBLE'), (2, 'C', 2, 'DOUBLE'), (2, 'C', 3, 'DOUBLE'),
(2, 'C', 4, 'DOUBLE'), (2, 'C', 5, 'DOUBLE'),
-- Hàng D (VIP)
(2, 'D', 1, 'VIP'), (2, 'D', 2, 'VIP'), (2, 'D', 3, 'VIP'),
(2, 'D', 4, 'VIP'), (2, 'D', 5, 'VIP'),
-- Hàng E (VIP)
(2, 'E', 1, 'VIP'), (2, 'E', 2, 'VIP'), (2, 'E', 3, 'VIP'),
(2, 'E', 4, 'VIP'), (2, 'E', 5, 'VIP');

-- Phòng 3 (room_id = 3): 5 hàng x 7 ghế
-- Hàng A, B, C: REGULAR | Hàng D: DOUBLE | Hàng E: VIP
INSERT INTO seats (room_id, seat_row, seat_number, seat_type) VALUES
-- Hàng A (REGULAR)
(3, 'A', 1, 'REGULAR'), (3, 'A', 2, 'REGULAR'), (3, 'A', 3, 'REGULAR'),
(3, 'A', 4, 'REGULAR'), (3, 'A', 5, 'REGULAR'), (3, 'A', 6, 'REGULAR'), (3, 'A', 7, 'REGULAR'),
-- Hàng B (REGULAR)
(3, 'B', 1, 'REGULAR'), (3, 'B', 2, 'REGULAR'), (3, 'B', 3, 'REGULAR'),
(3, 'B', 4, 'REGULAR'), (3, 'B', 5, 'REGULAR'), (3, 'B', 6, 'REGULAR'), (3, 'B', 7, 'REGULAR'),
-- Hàng C (REGULAR)
(3, 'C', 1, 'REGULAR'), (3, 'C', 2, 'REGULAR'), (3, 'C', 3, 'REGULAR'),
(3, 'C', 4, 'REGULAR'), (3, 'C', 5, 'REGULAR'), (3, 'C', 6, 'REGULAR'), (3, 'C', 7, 'REGULAR'),
-- Hàng D (DOUBLE)
(3, 'D', 1, 'DOUBLE'), (3, 'D', 2, 'DOUBLE'), (3, 'D', 3, 'DOUBLE'),
(3, 'D', 4, 'DOUBLE'), (3, 'D', 5, 'DOUBLE'), (3, 'D', 6, 'DOUBLE'), (3, 'D', 7, 'DOUBLE'),
-- Hàng E (VIP)
(3, 'E', 1, 'VIP'), (3, 'E', 2, 'VIP'), (3, 'E', 3, 'VIP'),
(3, 'E', 4, 'VIP'), (3, 'E', 5, 'VIP'), (3, 'E', 6, 'VIP'), (3, 'E', 7, 'VIP');

-- ----- SHOWTIMES (Suất chiếu) -----
INSERT INTO showtimes (movie_id, room_id, show_date, start_time, end_time, base_price, status) VALUES
-- Phim 1: Lật Mặt 7 - Phòng 1
(1, 1, '2026-11-05', '09:00:00', '11:10:00',  75000, 'SCHEDULED'),
(1, 1, '2026-11-05', '14:00:00', '16:10:00',  85000, 'SCHEDULED'),
(1, 1, '2026-11-06', '19:00:00', '21:10:00',  95000, 'SCHEDULED'),
-- Phim 2: Mai - Phòng 2
(2, 2, '2026-11-05', '10:00:00', '12:10:00', 100000, 'SCHEDULED'),
(2, 2, '2026-11-06', '15:00:00', '17:10:00', 110000, 'SCHEDULED'),
-- Phim 3: Kung Fu Panda 4 - Phòng 1
(3, 1, '2026-11-07', '09:30:00', '11:04:00',  70000, 'SCHEDULED'),
-- Phim 4: Godzilla x Kong - Phòng 3 (IMAX)
(4, 3, '2026-11-05', '18:00:00', '19:55:00', 150000, 'SCHEDULED'),
(4, 3, '2026-11-06', '20:00:00', '21:55:00', 160000, 'SCHEDULED'),
-- Phim 5: Dune 2 - Phòng 3 (IMAX)
(5, 3, '2026-11-07', '19:00:00', '21:46:00', 170000, 'SCHEDULED'),
-- Suất đã hủy
(1, 2, '2026-11-08', '10:00:00', '12:10:00',  90000, 'CANCELLED'),
-- Suất chiếu của phim đã ngừng chiếu (Phim 6: Quật Mộ Trùng Ma - STOPPED, showtime_id=11)
(6, 1, '2026-11-09', '18:00:00', '20:00:00',  80000, 'SCHEDULED');

-- ----- SHOWTIME_SEATS (Trạng thái ghế cho các suất chiếu) -----
-- Tạo showtime_seats cho suất chiếu 11 (phim đã ngừng chiếu, room_id=1, 30 ghế)
INSERT INTO showtime_seats (showtime_id, seat_id, status)
SELECT 11, seat_id, 'AVAILABLE' FROM seats WHERE room_id = 1;
-- Tạo showtime_seats cho suất chiếu 1 (showtime_id=1, room_id=1, 30 ghế: seat_id 1-30)
INSERT INTO showtime_seats (showtime_id, seat_id, status) VALUES
(1, 1, 'AVAILABLE'), (1, 2, 'AVAILABLE'), (1, 3, 'BOOKED'),   (1, 4, 'AVAILABLE'), (1, 5, 'AVAILABLE'), (1, 6, 'AVAILABLE'),
(1, 7, 'AVAILABLE'), (1, 8, 'BOOKED'),   (1, 9, 'AVAILABLE'), (1, 10, 'AVAILABLE'), (1, 11, 'AVAILABLE'), (1, 12, 'AVAILABLE'),
(1, 13, 'AVAILABLE'), (1, 14, 'AVAILABLE'), (1, 15, 'AVAILABLE'), (1, 16, 'AVAILABLE'), (1, 17, 'AVAILABLE'), (1, 18, 'AVAILABLE'),
(1, 19, 'AVAILABLE'), (1, 20, 'AVAILABLE'), (1, 21, 'RESERVED'), (1, 22, 'AVAILABLE'), (1, 23, 'AVAILABLE'), (1, 24, 'AVAILABLE'),
(1, 25, 'BOOKED'),   (1, 26, 'AVAILABLE'), (1, 27, 'AVAILABLE'), (1, 28, 'AVAILABLE'), (1, 29, 'AVAILABLE'), (1, 30, 'AVAILABLE');

-- Tạo showtime_seats cho suất chiếu 2 (showtime_id=2, room_id=1, 30 ghế: seat_id 1-30)
INSERT INTO showtime_seats (showtime_id, seat_id, status) VALUES
(2, 1, 'AVAILABLE'), (2, 2, 'AVAILABLE'), (2, 3, 'AVAILABLE'), (2, 4, 'AVAILABLE'), (2, 5, 'BOOKED'), (2, 6, 'AVAILABLE'),
(2, 7, 'AVAILABLE'), (2, 8, 'AVAILABLE'), (2, 9, 'AVAILABLE'), (2, 10, 'BOOKED'), (2, 11, 'AVAILABLE'), (2, 12, 'AVAILABLE'),
(2, 13, 'AVAILABLE'), (2, 14, 'AVAILABLE'), (2, 15, 'AVAILABLE'), (2, 16, 'AVAILABLE'), (2, 17, 'AVAILABLE'), (2, 18, 'AVAILABLE'),
(2, 19, 'AVAILABLE'), (2, 20, 'AVAILABLE'), (2, 21, 'AVAILABLE'), (2, 22, 'AVAILABLE'), (2, 23, 'AVAILABLE'), (2, 24, 'AVAILABLE'),
(2, 25, 'AVAILABLE'), (2, 26, 'AVAILABLE'), (2, 27, 'AVAILABLE'), (2, 28, 'BOOKED'), (2, 29, 'AVAILABLE'), (2, 30, 'AVAILABLE');

-- Tạo showtime_seats cho suất chiếu 4 (showtime_id=4, room_id=2, 25 ghế: seat_id 31-55)
INSERT INTO showtime_seats (showtime_id, seat_id, status) VALUES
(4, 31, 'AVAILABLE'), (4, 32, 'BOOKED'),   (4, 33, 'AVAILABLE'), (4, 34, 'AVAILABLE'), (4, 35, 'AVAILABLE'),
(4, 36, 'AVAILABLE'), (4, 37, 'AVAILABLE'), (4, 38, 'AVAILABLE'), (4, 39, 'BOOKED'),   (4, 40, 'AVAILABLE'),
(4, 41, 'AVAILABLE'), (4, 42, 'AVAILABLE'), (4, 43, 'AVAILABLE'), (4, 44, 'AVAILABLE'), (4, 45, 'AVAILABLE'),
(4, 46, 'AVAILABLE'), (4, 47, 'BOOKED'),   (4, 48, 'AVAILABLE'), (4, 49, 'AVAILABLE'), (4, 50, 'AVAILABLE'),
(4, 51, 'AVAILABLE'), (4, 52, 'AVAILABLE'), (4, 53, 'AVAILABLE'), (4, 54, 'AVAILABLE'), (4, 55, 'AVAILABLE');

-- Tạo showtime_seats cho suất chiếu 7 (showtime_id=7, room_id=3, 35 ghế: seat_id 56-90)
INSERT INTO showtime_seats (showtime_id, seat_id, status) VALUES
(7, 56, 'AVAILABLE'), (7, 57, 'AVAILABLE'), (7, 58, 'BOOKED'),   (7, 59, 'AVAILABLE'), (7, 60, 'AVAILABLE'), (7, 61, 'AVAILABLE'), (7, 62, 'AVAILABLE'),
(7, 63, 'AVAILABLE'), (7, 64, 'AVAILABLE'), (7, 65, 'AVAILABLE'), (7, 66, 'AVAILABLE'), (7, 67, 'AVAILABLE'), (7, 68, 'BOOKED'),   (7, 69, 'AVAILABLE'),
(7, 70, 'AVAILABLE'), (7, 71, 'AVAILABLE'), (7, 72, 'AVAILABLE'), (7, 73, 'AVAILABLE'), (7, 74, 'AVAILABLE'), (7, 75, 'AVAILABLE'), (7, 76, 'AVAILABLE'),
(7, 77, 'AVAILABLE'), (7, 78, 'AVAILABLE'), (7, 79, 'AVAILABLE'), (7, 80, 'AVAILABLE'), (7, 81, 'RESERVED'), (7, 82, 'AVAILABLE'), (7, 83, 'AVAILABLE'),
(7, 84, 'BOOKED'),   (7, 85, 'AVAILABLE'), (7, 86, 'AVAILABLE'), (7, 87, 'AVAILABLE'), (7, 88, 'AVAILABLE'), (7, 89, 'AVAILABLE'), (7, 90, 'AVAILABLE');

-- ----- BOOKINGS (Đơn đặt vé mẫu) -----
INSERT INTO bookings (booking_code, customer_id, showtime_id, total_amount, discount_amount, final_amount, status, booking_time) VALUES
('BK-20261105-0001', 1, 1,  75000,     0,  75000, 'CONFIRMED', '2026-11-04 10:30:00'),
('BK-20261105-0002', 3, 1, 150000, 30000, 120000, 'CONFIRMED', '2026-11-04 11:00:00'),
('BK-20261105-0003', 6, 1, 120000,     0, 120000, 'CONFIRMED', '2026-11-04 11:30:00'),
('BK-20261105-0004', 4, 4, 200000, 40000, 160000, 'CONFIRMED', '2026-11-04 12:00:00'),
('BK-20261105-0005', 2, 2, 170000,     0, 170000, 'PENDING',   '2026-11-04 14:00:00'),
('BK-20261105-0006', 7, 7, 300000,     0, 300000, 'CONFIRMED', '2026-11-04 15:00:00');

-- ----- TICKETS (Vé xem phim mẫu) -----
-- Booking 1: Khách thường Nguyễn Văn An, ghế A3 (seat_id=3) → showtime_seat_id=3
INSERT INTO tickets (ticket_code, booking_id, showtime_seat_id, seat_price) VALUES
('TK-20261105-0001', 1, 3, 75000);

-- Booking 2: Sinh viên Lê Hoàng Nam, ghế B2 (seat_id=8) và D3 (seat_id=21) → showtime_seat_id=8, 21
INSERT INTO tickets (ticket_code, booking_id, showtime_seat_id, seat_price) VALUES
('TK-20261105-0002', 2, 8,  60000),
('TK-20261105-0003', 2, 21, 90000);

-- Booking 3: VIP Vũ Quốc Bảo, ghế E1 (seat_id=25) → showtime_seat_id=25
INSERT INTO tickets (ticket_code, booking_id, showtime_seat_id, seat_price) VALUES
('TK-20261105-0004', 3, 25, 120000);

-- Booking 4: Sinh viên Phạm Minh Tú, ghế A2 và B4 phòng 2 → showtime_seat_id=32, 39
INSERT INTO tickets (ticket_code, booking_id, showtime_seat_id, seat_price) VALUES
('TK-20261105-0005', 4, 32, 80000),
('TK-20261105-0006', 4, 39, 120000);

-- Booking 6: VIP Hoàng Thị Mai Linh, ghế A3 và B5 và E1 phòng 3 → showtime_seat_id
INSERT INTO tickets (ticket_code, booking_id, showtime_seat_id, seat_price) VALUES
('TK-20261105-0007', 6, 58, 150000),
('TK-20261105-0008', 6, 68, 150000);

-- ----- PAYMENTS (Thanh toán mẫu) -----
INSERT INTO payments (payment_code, booking_id, amount, payment_method, payment_time, status) VALUES
('PM-20261105-0001', 1,  75000, 'CASH',          '2026-11-04 10:35:00', 'SUCCESS'),
('PM-20261105-0002', 2, 120000, 'E_WALLET',       '2026-11-04 11:05:00', 'SUCCESS'),
('PM-20261105-0003', 3, 120000, 'CREDIT_CARD',    '2026-11-04 11:35:00', 'SUCCESS'),
('PM-20261105-0004', 4, 160000, 'BANK_TRANSFER',  '2026-11-04 12:10:00', 'SUCCESS'),
('PM-20261105-0005', 6, 300000, 'CREDIT_CARD',    '2026-11-04 15:10:00', 'SUCCESS');

-- =====================================================================
-- KẾT THÚC - Database cinema_booking_db đã sẵn sàng!
-- =====================================================================

