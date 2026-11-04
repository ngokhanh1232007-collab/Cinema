package com.cinema.service;

import com.cinema.model.Movie;
import com.cinema.model.MovieStatus;
import com.cinema.repository.MovieRepository;
import com.cinema.utils.BusinessException;
import com.cinema.utils.Validator;

import java.util.List;

/**
 * Service xử lý các nghiệp vụ liên quan đến phim (Movie).
 * Chịu trách nhiệm hiển thị danh sách phim đang chiếu, tra cứu phim theo tên,
 * và kiểm tra trạng thái phim trước khi đặt vé.
 */
public class MovieService {

    private final MovieRepository movieRepository;

    /**
     * Constructor mặc định khởi tạo repository tương ứng.
     */
    public MovieService() {
        this.movieRepository = new MovieRepository();
    }

    /**
     * Constructor cho phép truyền repository từ bên ngoài (hỗ trợ Dependency Injection).
     *
     * @param movieRepository repository phim
     */
    public MovieService(MovieRepository movieRepository) {
        this.movieRepository = movieRepository;
    }

    /**
     * Lấy danh sách tất cả các phim đang được chiếu tại rạp.
     *
     * @return danh sách phim có trạng thái SHOWING
     */
    public List<Movie> getShowingMovies() {
        return movieRepository.findShowingMovies();
    }

    /**
     * Hiển thị danh sách các phim đang chiếu ra màn hình console theo định dạng bảng trực quan.
     */
    public void displayShowingMovies() {
        List<Movie> showingMovies = movieRepository.findShowingMovies();

        System.out.println("==========================================================================================");
        System.out.println("                              DANH SÁCH PHIM ĐANG CHIẾU                                   ");
        System.out.println("==========================================================================================");

        if (showingMovies.isEmpty()) {
            System.out.println("  Hiện tại rạp chưa có phim nào đang chiếu.");
            System.out.println("==========================================================================================");
            return;
        }

        System.out.printf("%-4s | %-28s | %-20s | %-10s | %-6s | %-10s%n",
                "ID", "Tên phim", "Thể loại", "Thời lượng", "Đ.Giá", "Trạng thái");
        System.out.println("------------------------------------------------------------------------------------------");

        for (Movie m : showingMovies) {
            System.out.printf("%-4d | %-28s | %-20s | %-7d p | %-4.1f★  | %-10s%n",
                    m.getMovieId(),
                    truncate(m.getTitle(), 28),
                    truncate(m.getGenre(), 20),
                    m.getDuration(),
                    m.getRating(),
                    m.getStatus().getDisplayName());
        }
        System.out.println("==========================================================================================");
        System.out.printf("  Tổng cộng: %d phim đang chiếu.%n", showingMovies.size());
    }

    /**
     * Tìm kiếm phim theo tên (tìm gần đúng theo từ khóa).
     *
     * @param title từ khóa tên phim cần tìm
     * @return danh sách phim thỏa mãn
     * @throws BusinessException nếu từ khóa để trống hoặc rỗng
     */
    public List<Movie> searchMovieByTitle(String title) throws BusinessException {
        if (Validator.isNullOrEmpty(title)) {
            throw new BusinessException(BusinessException.INVALID_INPUT, "Từ khóa tìm kiếm phim không được để trống.");
        }
        return movieRepository.searchByTitle(title.trim());
    }

    /**
     * Hiển thị kết quả tìm kiếm phim theo tên ra console.
     *
     * @param keyword từ khóa tìm kiếm
     * @throws BusinessException nếu từ khóa không hợp lệ
     */
    public void displaySearchMovies(String keyword) throws BusinessException {
        List<Movie> results = searchMovieByTitle(keyword);

        System.out.println("==========================================================================================");
        System.out.printf("                    KẾT QUẢ TÌM KIẾM PHIM VỚI TỪ KHÓA: \"%s\"%n", keyword);
        System.out.println("==========================================================================================");

        if (results.isEmpty()) {
            System.out.printf("  Không tìm thấy phim nào khớp với từ khóa \"%s\".%n", keyword);
            System.out.println("==========================================================================================");
            return;
        }

        System.out.printf("%-4s | %-28s | %-20s | %-10s | %-6s | %-10s%n",
                "ID", "Tên phim", "Thể loại", "Thời lượng", "Đ.Giá", "Trạng thái");
        System.out.println("------------------------------------------------------------------------------------------");

        for (Movie m : results) {
            System.out.printf("%-4d | %-28s | %-20s | %-7d p | %-4.1f★  | %-10s%n",
                    m.getMovieId(),
                    truncate(m.getTitle(), 28),
                    truncate(m.getGenre(), 20),
                    m.getDuration(),
                    m.getRating(),
                    m.getStatus().getDisplayName());
        }
        System.out.println("==========================================================================================");
        System.out.printf("  Tìm thấy %d kết quả phù hợp.%n", results.size());
    }

    /**
     * Lấy thông tin phim theo mã định danh (movie_id).
     * Kiểm tra chặt chẽ sự tồn tại của phim.
     *
     * @param movieId mã phim cần tìm
     * @return đối tượng Movie nếu tồn tại
     * @throws BusinessException nếu phim không tồn tại
     */
    public Movie getMovieById(int movieId) throws BusinessException {
        if (movieId <= 0) {
            throw new BusinessException(BusinessException.INVALID_INPUT, "Mã phim phải lớn hơn 0.");
        }

        Movie movie = movieRepository.findById(movieId);
        if (movie == null) {
            throw new BusinessException(BusinessException.MOVIE_NOT_SHOWING,
                    "Không tìm thấy phim với mã ID: " + movieId);
        }
        return movie;
    }

    /**
     * Kiểm tra xem phim có đang được phép đặt vé hay không (phim phải tồn tại và đang chiếu).
     *
     * @param movieId mã phim cần kiểm tra
     * @return đối tượng Movie nếu hợp lệ
     * @throws BusinessException nếu phim không tồn tại hoặc đã ngừng chiếu
     */
    public Movie requireMovieShowing(int movieId) throws BusinessException {
        Movie movie = getMovieById(movieId);
        if (movie.getStatus() != MovieStatus.DANG_CHIEU) {
            throw new BusinessException(BusinessException.MOVIE_NOT_SHOWING,
                    "Phim '" + movie.getTitle() + "' đã NGỪNG CHIẾU. Không thể đặt vé cho phim này.");
        }
        return movie;
    }

    /**
     * Lấy toàn bộ danh sách phim (cả đang chiếu và ngừng chiếu).
     *
     * @return danh sách toàn bộ phim
     */
    public List<Movie> getAllMovies() {
        return movieRepository.findAll();
    }

    /**
     * Hàm phụ trợ cắt ngắn chuỗi để định dạng hiển thị đẹp mắt.
     */
    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }
}

