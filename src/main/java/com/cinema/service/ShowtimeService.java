package com.cinema.service;

import com.cinema.model.Movie;
import com.cinema.model.MovieStatus;
import com.cinema.model.Seat;
import com.cinema.model.SeatStatus;
import com.cinema.model.SeatType;
import com.cinema.model.Showtime;
import com.cinema.repository.MovieRepository;
import com.cinema.repository.ShowtimeRepository;
import com.cinema.utils.BusinessException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Service xử lý các nghiệp vụ liên quan đến suất chiếu (Showtime) và sơ đồ ghế ngồi.
 * Chịu trách nhiệm kiểm tra suất chiếu hợp lệ, kiểm tra phim đang chiếu,
 * kiểm tra ghế trống và vẽ sơ đồ phòng chiếu ra giao diện console.
 */
public class ShowtimeService {

    private final ShowtimeRepository showtimeRepository;
    private final MovieRepository movieRepository;

    /**
     * Constructor mặc định.
     */
    public ShowtimeService() {
        this.showtimeRepository = new ShowtimeRepository();
        this.movieRepository = new MovieRepository();
    }

    /**
     * Constructor cho phép tiêm phụ thuộc repository (Dependency Injection).
     *
     * @param showtimeRepository repository suất chiếu
     * @param movieRepository    repository phim
     */
    public ShowtimeService(ShowtimeRepository showtimeRepository, MovieRepository movieRepository) {
        this.showtimeRepository = showtimeRepository;
        this.movieRepository = movieRepository;
    }

    /**
     * Lấy thông tin suất chiếu theo mã và kiểm tra đầy đủ các quy tắc nghiệp vụ:
     * 1. Suất chiếu phải tồn tại trong cơ sở dữ liệu.
     * 2. Suất chiếu không được ở trạng thái đã bị hủy (CANCELLED).
     * 3. Phim của suất chiếu phải tồn tại và đang ở trạng thái ĐANG CHIẾU (SHOWING).
     *
     * @param showtimeId mã suất chiếu cần lấy
     * @return đối tượng Showtime hợp lệ
     * @throws BusinessException nếu vi phạm bất kỳ quy tắc nào
     */
    public Showtime getValidShowtime(int showtimeId) throws BusinessException {
        if (showtimeId <= 0) {
            throw new BusinessException(BusinessException.INVALID_INPUT, "Mã suất chiếu phải lớn hơn 0.");
        }

        Showtime showtime = showtimeRepository.findById(showtimeId);
        if (showtime == null) {
            throw new BusinessException(BusinessException.SHOWTIME_NOT_FOUND,
                    "Không tìm thấy suất chiếu với mã ID: " + showtimeId);
        }

        if (showtime.isCancelled()) {
            throw new BusinessException(BusinessException.SHOWTIME_CANCELLED,
                    "Suất chiếu mã " + showtimeId + " đã BỊ HỦY. Không thể xem hoặc đặt vé.");
        }

        // Kiểm tra phim tương ứng
        Movie movie = showtime.getMovie();
        if (movie == null) {
            movie = movieRepository.findById(showtime.getMovieId());
        }

        if (movie == null) {
            throw new BusinessException(BusinessException.MOVIE_NOT_SHOWING,
                    "Không tìm thấy thông tin phim gắn với suất chiếu này.");
        }

        if (movie.getStatus() != MovieStatus.DANG_CHIEU) {
            throw new BusinessException(BusinessException.MOVIE_NOT_SHOWING,
                    "Phim '" + movie.getTitle() + "' đã NGỪNG CHIẾU. Không thể thao tác với suất chiếu này.");
        }

        return showtime;
    }

    /**
     * Lấy danh sách suất chiếu của một phim cụ thể.
     * Kiểm tra phim có tồn tại và đang chiếu hay không trước khi trả về.
     *
     * @param movieId mã phim
     * @return danh sách suất chiếu còn hiệu lực của phim
     * @throws BusinessException nếu phim không tồn tại hoặc đã ngừng chiếu
     */
    public List<Showtime> getShowtimesByMovieId(int movieId) throws BusinessException {
        if (movieId <= 0) {
            throw new BusinessException(BusinessException.INVALID_INPUT, "Mã phim phải lớn hơn 0.");
        }

        Movie movie = movieRepository.findById(movieId);
        if (movie == null) {
            throw new BusinessException(BusinessException.MOVIE_NOT_SHOWING,
                    "Không tìm thấy phim với mã ID: " + movieId);
        }

        if (movie.getStatus() != MovieStatus.DANG_CHIEU) {
            throw new BusinessException(BusinessException.MOVIE_NOT_SHOWING,
                    "Phim '" + movie.getTitle() + "' đã ngừng chiếu, không có suất chiếu khả dụng.");
        }

        return showtimeRepository.findByMovieId(movieId);
    }

    /**
     * Hiển thị danh sách các suất chiếu của một phim ra console.
     *
     * @param movieId mã phim
     * @throws BusinessException nếu có lỗi nghiệp vụ
     */
    public void displayShowtimesByMovie(int movieId) throws BusinessException {
        Movie movie = movieRepository.findById(movieId);
        if (movie == null) {
            throw new BusinessException(BusinessException.MOVIE_NOT_SHOWING, "Không tìm thấy phim mã: " + movieId);
        }

        List<Showtime> showtimes = getShowtimesByMovieId(movieId);

        System.out.println("==========================================================================================");
        System.out.printf("               LỊCH CHIẾU PHIM: %s%n", movie.getTitle().toUpperCase());
        System.out.println("==========================================================================================");

        if (showtimes.isEmpty()) {
            System.out.println("  Hiện tại phim này chưa có suất chiếu nào được lên lịch.");
            System.out.println("==========================================================================================");
            return;
        }

        System.out.printf("%-4s | %-12s | %-15s | %-22s | %-14s | %-10s%n",
                "ID", "Ngày chiếu", "Khung giờ", "Phòng chiếu", "Giá vé gốc", "Ghế trống");
        System.out.println("------------------------------------------------------------------------------------------");

        for (Showtime st : showtimes) {
            List<Seat> seats = showtimeRepository.getSeatsByShowtimeId(st.getShowtimeId());
            long availableCount = seats.stream().filter(Seat::isAvailable).count();
            String roomName = (st.getRoom() != null) ? st.getRoom().getRoomName() : "Phòng #" + st.getRoomId();

            System.out.printf("%-4d | %-12s | %s - %s   | %-22s | %,10.0f đ   | %d/%d ghế%n",
                    st.getShowtimeId(),
                    st.getShowDateFormatted(),
                    st.getStartTimeFormatted(),
                    st.getEndTimeFormatted(),
                    roomName,
                    st.getBasePrice(),
                    availableCount,
                    seats.size());
        }
        System.out.println("==========================================================================================");
    }

    /**
     * Kiểm tra xem một suất chiếu có còn ít nhất một ghế trống hay không.
     *
     * @param showtimeId mã suất chiếu
     * @return true nếu còn ít nhất 1 ghế trống, false nếu đã hết sạch vé
     * @throws BusinessException nếu suất chiếu không hợp lệ
     */
    public boolean hasAvailableSeats(int showtimeId) throws BusinessException {
        Showtime showtime = getValidShowtime(showtimeId);
        List<Seat> seats = showtimeRepository.getSeatsByShowtimeId(showtime.getShowtimeId());
        for (Seat seat : seats) {
            if (seat.isAvailable()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Lấy toàn bộ danh sách ghế của suất chiếu (kèm trạng thái thực tế).
     *
     * @param showtimeId mã suất chiếu
     * @return danh sách ghế
     * @throws BusinessException nếu suất chiếu không hợp lệ
     */
    public List<Seat> getSeatsByShowtime(int showtimeId) throws BusinessException {
        Showtime showtime = getValidShowtime(showtimeId);
        return showtimeRepository.getSeatsByShowtimeId(showtime.getShowtimeId());
    }

    /**
     * Hiển thị sơ đồ ghế ngồi trực quan của suất chiếu ra màn hình console.
     * Thể hiện rõ vị trí từng ghế, loại ghế (Thường/Đôi/VIP) và tình trạng đặt (Trống/Đã đặt/Đang giữ).
     *
     * @param showtimeId mã suất chiếu
     * @throws BusinessException nếu suất chiếu không tồn tại hoặc không hợp lệ
     */
    public void displaySeatMap(int showtimeId) throws BusinessException {
        Showtime showtime = getValidShowtime(showtimeId);
        List<Seat> seats = showtimeRepository.getSeatsByShowtimeId(showtimeId);

        if (seats.isEmpty()) {
            throw new BusinessException(BusinessException.ROOM_NOT_AVAILABLE,
                    "Suất chiếu này chưa được khởi tạo sơ đồ ghế trong hệ thống.");
        }

        String movieTitle = (showtime.getMovie() != null) ? showtime.getMovie().getTitle() : "N/A";
        String roomName = (showtime.getRoom() != null) ? showtime.getRoom().getRoomName() : "N/A";

        System.out.println("==========================================================================================");
        System.out.printf("                             SƠ ĐỒ GHẾ NGỒI - SUẤT CHIẾU #%d%n", showtimeId);
        System.out.printf("  Phim:        %s%n", movieTitle);
        System.out.printf("  Phòng chiếu: %s | Ngày: %s | Giờ: %s - %s%n",
                roomName, showtime.getShowDateFormatted(), showtime.getStartTimeFormatted(), showtime.getEndTimeFormatted());
        System.out.printf("  Giá vé gốc:  %,.0f VND%n", showtime.getBasePrice());
        System.out.println("==========================================================================================");
        System.out.println("                                    [ MÀN HÌNH CHIẾU ]                                    ");
        System.out.println("------------------------------------------------------------------------------------------");
        System.out.println();

        // Nhóm các ghế theo hàng (Hàng A, B, C...)
        Map<String, List<Seat>> rowsMap = new LinkedHashMap<>();
        int availableSeats = 0;
        int bookedSeats = 0;
        int reservedSeats = 0;

        for (Seat seat : seats) {
            rowsMap.computeIfAbsent(seat.getSeatRow(), k -> new ArrayList<>()).add(seat);

            if (seat.getStatus() == SeatStatus.CON_TRONG) {
                availableSeats++;
            } else if (seat.getStatus() == SeatStatus.DA_DAT) {
                bookedSeats++;
            } else {
                reservedSeats++;
            }
        }

        // In từng hàng ghế
        for (Map.Entry<String, List<Seat>> entry : rowsMap.entrySet()) {
            String rowName = entry.getKey();
            List<Seat> rowSeats = entry.getValue();

            System.out.printf("   Hàng %s:  ", rowName);
            for (Seat s : rowSeats) {
                String typeCode = getSeatTypeCode(s.getSeatType());
                String statusCode = getStatusCode(s.getStatus());
                // Định dạng hiển thị mỗi ghế: [A1:T:O]
                System.out.printf("[%s:%s:%s] ", s.getSeatLabel(), typeCode, statusCode);
            }
            System.out.println();
        }

        System.out.println();
        System.out.println("------------------------------------------------------------------------------------------");
        System.out.println("  [CHÚ THÍCH LOẠI GHẾ]  T: Ghế thường (+0đ) | D: Ghế đôi (+30.000đ) | V: Ghế VIP (+50.000đ)");
        System.out.println("  [CHÚ THÍCH TRẠNG THÁI] O: Còn trống         | X: Đã đặt             | -: Đang giữ tạm");
        System.out.println("------------------------------------------------------------------------------------------");
        System.out.printf("  THỐNG KÊ: Còn trống: %d ghế | Đã đặt: %d ghế | Đang giữ: %d ghế | Tổng: %d ghế%n",
                availableSeats, bookedSeats, reservedSeats, seats.size());
        System.out.println("==========================================================================================");
    }

    /**
     * Ký hiệu viết tắt cho loại ghế.
     */
    private String getSeatTypeCode(SeatType type) {
        if (type == null) return "T";
        switch (type) {
            case GHE_VIP:  return "V";
            case GHE_DOI:  return "D";
            default:       return "T";
        }
    }

    /**
     * Ký hiệu viết tắt cho trạng thái ghế.
     */
    private String getStatusCode(SeatStatus status) {
        if (status == null) return "O";
        switch (status) {
            case DA_DAT:   return "X";
            case DANG_GIU: return "-";
            default:       return "O";
        }
    }
}

