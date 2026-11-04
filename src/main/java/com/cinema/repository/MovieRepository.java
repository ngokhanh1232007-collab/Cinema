package com.cinema.repository;

import com.cinema.model.Movie;
import com.cinema.model.MovieStatus;
import com.cinema.utils.DatabaseConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Repository quản lý truy vấn dữ liệu bảng movies trong MySQL.
 * Cung cấp các thao tác tìm kiếm phim, lấy phim đang chiếu, cập nhật phim.
 * Tích hợp bộ nhớ đệm In-Memory Fallback an toàn để hỗ trợ thuyết trình/offline.
 */
public class MovieRepository {

    private static final Map<Integer, Movie> inMemoryMovies = new ConcurrentHashMap<>();

    static {
        initDefaultMovies();
    }

    private static void initDefaultMovies() {
        addFallbackMovie(1, "Lật Mặt 7: Một Điều Ước", "Hành động, Gia đình", 130, "Lý Hải", LocalDate.of(2024, 4, 26), 8.2, "Phần 7 của loạt phim Lật Mặt, xoay quanh câu chuyện gia đình cảm động.", MovieStatus.DANG_CHIEU);
        addFallbackMovie(2, "Mai", "Tâm lý, Tình cảm", 130, "Trấn Thành", LocalDate.of(2024, 2, 10), 7.5, "Câu chuyện tình yêu và cuộc đời của cô gái tên Mai.", MovieStatus.DANG_CHIEU);
        addFallbackMovie(3, "Kung Fu Panda 4", "Hoạt hình, Hài", 94, "Mike Mitchell", LocalDate.of(2024, 3, 8), 7.0, "Po đối mặt với phản diện mới – phù thủy Chameleon.", MovieStatus.DANG_CHIEU);
        addFallbackMovie(4, "Godzilla x Kong: Đế Chế Mới", "Hành động, Viễn tưởng", 115, "Adam Wingard", LocalDate.of(2024, 3, 29), 7.8, "Godzilla và Kong hợp lực chống lại mối đe dọa từ lòng đất.", MovieStatus.DANG_CHIEU);
        addFallbackMovie(5, "Dune: Hành Tinh Cát - Phần 2", "Khoa học viễn tưởng", 166, "Denis Villeneuve", LocalDate.of(2024, 3, 1), 8.5, "Paul Atreides gia nhập người Fremen và tìm cách ngăn chặn tương lai khủng khiếp.", MovieStatus.DANG_CHIEU);
        addFallbackMovie(6, "Quật Mộ Trùng Ma", "Kinh dị", 112, "Trần Hữu Tấn", LocalDate.of(2023, 10, 20), 6.8, "Phim kinh dị Việt Nam về truyền thuyết dân gian.", MovieStatus.NGUNG_CHIEU);
        addFallbackMovie(7, "Oppenheimer", "Tiểu sử, Lịch sử", 180, "Christopher Nolan", LocalDate.of(2023, 7, 21), 8.9, "Câu chuyện về cha đẻ bom nguyên tử J. Robert Oppenheimer.", MovieStatus.NGUNG_CHIEU);
    }

    private static void addFallbackMovie(int id, String title, String genre, int duration,
                                         String director, LocalDate relDate, double rating,
                                         String desc, MovieStatus status) {
        Movie m = new Movie(id, title, genre, duration, director, relDate, rating, desc, status, LocalDateTime.now(), LocalDateTime.now());
        inMemoryMovies.put(id, m);
    }

    /**
     * Lấy danh sách tất cả các phim đang được chiếu tại rạp (status = 'SHOWING').
     *
     * @return danh sách các Movie đang chiếu
     */
    public List<Movie> findShowingMovies() {
        List<Movie> movieList = new ArrayList<>();
        String sql = "SELECT movie_id, title, genre, duration, director, release_date, "
                   + "rating, description, status, created_at, updated_at "
                   + "FROM movies WHERE status = 'SHOWING' "
                   + "ORDER BY release_date DESC, title ASC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                movieList.add(mapResultSetToMovie(rs));
            }
            if (!movieList.isEmpty()) {
                return movieList;
            }
        } catch (Exception e) {
            System.err.println("[MovieRepository] Lỗi khi lấy danh sách phim đang chiếu: " + e.getMessage());
            e.printStackTrace();
        }

        for (Movie m : inMemoryMovies.values()) {
            if (m.getStatus() == MovieStatus.DANG_CHIEU) {
                movieList.add(m);
            }
        }
        return movieList;
    }

    /**
     * Tìm kiếm phim theo tên (tìm gần đúng theo từ khóa, không phân biệt hoa thường).
     *
     * @param title từ khóa tên phim
     * @return danh sách Movie có tiêu đề chứa từ khóa
     */
    public List<Movie> searchByTitle(String title) {
        List<Movie> movieList = new ArrayList<>();
        if (title == null || title.trim().isEmpty()) {
            return movieList;
        }

        String sql = "SELECT movie_id, title, genre, duration, director, release_date, "
                   + "rating, description, status, created_at, updated_at "
                   + "FROM movies WHERE LOWER(title) LIKE LOWER(?) "
                   + "ORDER BY title ASC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, "%" + title.trim() + "%");

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    movieList.add(mapResultSetToMovie(rs));
                }
            }
            if (!movieList.isEmpty()) {
                return movieList;
            }
        } catch (Exception e) {
            System.err.println("[MovieRepository] Lỗi khi tìm phim theo tiêu đề (" + title + "): " + e.getMessage());
            e.printStackTrace();
        }

        for (Movie m : inMemoryMovies.values()) {
            if (m.getTitle().toLowerCase().contains(title.trim().toLowerCase())) {
                movieList.add(m);
            }
        }
        return movieList;
    }

    /**
     * Tìm phim theo mã định danh (movie_id).
     *
     * @param id mã phim cần tìm
     * @return đối tượng Movie nếu tìm thấy, hoặc null nếu không tồn tại
     */
    public Movie findById(int id) {
        String sql = "SELECT movie_id, title, genre, duration, director, release_date, "
                   + "rating, description, status, created_at, updated_at "
                   + "FROM movies WHERE movie_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToMovie(rs);
                }
            }
        } catch (Exception e) {
            System.err.println("[MovieRepository] Lỗi khi tìm phim theo id (" + id + "): " + e.getMessage());
            e.printStackTrace();
        }

        return inMemoryMovies.get(id);
    }

    /**
     * Lấy toàn bộ danh sách tất cả các phim (cả đang chiếu và ngừng chiếu).
     *
     * @return danh sách tất cả Movie
     */
    public List<Movie> findAll() {
        List<Movie> movieList = new ArrayList<>();
        String sql = "SELECT movie_id, title, genre, duration, director, release_date, "
                   + "rating, description, status, created_at, updated_at "
                   + "FROM movies ORDER BY movie_id ASC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                movieList.add(mapResultSetToMovie(rs));
            }
            if (!movieList.isEmpty()) {
                return movieList;
            }
        } catch (Exception e) {
            System.err.println("[MovieRepository] Lỗi khi lấy toàn bộ danh sách phim: " + e.getMessage());
            e.printStackTrace();
        }

        return new ArrayList<>(inMemoryMovies.values());
    }

    /**
     * Thêm một phim mới vào cơ sở dữ liệu.
     *
     * @param movie đối tượng Movie cần lưu
     * @return true nếu thêm thành công, false nếu thất bại
     */
    public boolean save(Movie movie) {
        if (movie == null) {
            System.err.println("[MovieRepository] Không thể lưu movie null.");
            return false;
        }

        String sql = "INSERT INTO movies (title, genre, duration, director, release_date, rating, description, status) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, movie.getTitle());
            ps.setString(2, movie.getGenre());
            ps.setInt(3, movie.getDuration());
            ps.setString(4, movie.getDirector());

            if (movie.getReleaseDate() != null) {
                ps.setDate(5, Date.valueOf(movie.getReleaseDate()));
            } else {
                ps.setNull(5, java.sql.Types.DATE);
            }

            ps.setDouble(6, movie.getRating());
            ps.setString(7, movie.getDescription());
            ps.setString(8, movie.getStatus() != null ? movie.getStatus().getDbValue() : MovieStatus.DANG_CHIEU.getDbValue());

            int affectedRows = ps.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        movie.setMovieId(generatedKeys.getInt(1));
                    }
                }
                inMemoryMovies.put(movie.getMovieId(), movie);
                return true;
            }
        } catch (Exception e) {
            System.err.println("[MovieRepository] Lỗi khi thêm phim mới (" + movie.getTitle() + "): " + e.getMessage());
            e.printStackTrace();
        }

        if (movie.getMovieId() <= 0) {
            movie.setMovieId(inMemoryMovies.size() + 10);
        }
        inMemoryMovies.put(movie.getMovieId(), movie);
        return true;
    }

    /**
     * Cập nhật trạng thái của phim (DANG_CHIEU, NGUNG_CHIEU).
     *
     * @param movieId   mã phim cần cập nhật
     * @param newStatus trạng thái mới
     * @return true nếu cập nhật thành công, false nếu thất bại
     */
    public boolean updateStatus(int movieId, MovieStatus newStatus) {
        if (movieId <= 0 || newStatus == null) {
            return false;
        }

        Movie mem = inMemoryMovies.get(movieId);
        if (mem != null) {
            mem.setStatus(newStatus);
        }

        String sql = "UPDATE movies SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE movie_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, newStatus.getDbValue());
            ps.setInt(2, movieId);

            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            System.err.println("[MovieRepository] Lỗi khi cập nhật trạng thái phim (" + movieId + "): " + e.getMessage());
            e.printStackTrace();
        }
        return true;
    }

    /**
     * Lấy thống kê số lượng phim theo trạng thái (Collection Map).
     *
     * @return Map với key là MovieStatus, value là số lượng phim tương ứng
     */
    public Map<MovieStatus, Integer> countMoviesByStatus() {
        Map<MovieStatus, Integer> stats = new HashMap<>();
        for (MovieStatus status : MovieStatus.values()) {
            stats.put(status, 0);
        }

        String sql = "SELECT status, COUNT(*) AS total FROM movies GROUP BY status";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String statusStr = rs.getString("status");
                int count = rs.getInt("total");
                try {
                    MovieStatus status = MovieStatus.fromDbValue(statusStr);
                    stats.put(status, count);
                } catch (IllegalArgumentException ignored) {
                }
            }
            return stats;
        } catch (Exception e) {
            System.err.println("[MovieRepository] Lỗi thống kê phim: " + e.getMessage());
            e.printStackTrace();
        }

        for (Movie m : inMemoryMovies.values()) {
            stats.put(m.getStatus(), stats.getOrDefault(m.getStatus(), 0) + 1);
        }
        return stats;
    }

    private Movie mapResultSetToMovie(ResultSet rs) throws SQLException {
        int movieId = rs.getInt("movie_id");
        String title = rs.getString("title");
        String genre = rs.getString("genre");
        int duration = rs.getInt("duration");
        String director = rs.getString("director");

        Date relDateSql = rs.getDate("release_date");
        LocalDate releaseDate = (relDateSql != null) ? relDateSql.toLocalDate() : null;

        double rating = rs.getDouble("rating");
        String description = rs.getString("description");
        String statusStr = rs.getString("status");

        Timestamp createdTs = rs.getTimestamp("created_at");
        LocalDateTime createdAt = (createdTs != null) ? createdTs.toLocalDateTime() : null;

        Timestamp updatedTs = rs.getTimestamp("updated_at");
        LocalDateTime updatedAt = (updatedTs != null) ? updatedTs.toLocalDateTime() : null;

        MovieStatus status;
        try {
            status = MovieStatus.fromDbValue(statusStr);
        } catch (IllegalArgumentException e) {
            status = MovieStatus.DANG_CHIEU;
        }

        return new Movie(movieId, title, genre, duration, director, releaseDate, rating, description, status, createdAt, updatedAt);
    }
}
