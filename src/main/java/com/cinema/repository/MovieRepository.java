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

/**
 * Repository quản lý truy vấn dữ liệu bảng movies trong MySQL.
 * Cung cấp các thao tác tìm kiếm phim, lấy phim đang chiếu, cập nhật phim.
 */
public class MovieRepository {

    /**
     * Lấy danh sách tất cả các phim đang được chiếu tại rạp (status = 'SHOWING').
     * Sử dụng Collection List để lưu kết quả.
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
        } catch (SQLException e) {
            System.err.println("[MovieRepository] Lỗi khi lấy danh sách phim đang chiếu: " + e.getMessage());
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
        } catch (SQLException e) {
            System.err.println("[MovieRepository] Lỗi khi tìm phim theo tiêu đề (" + title + "): " + e.getMessage());
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
        } catch (SQLException e) {
            System.err.println("[MovieRepository] Lỗi khi tìm phim theo id (" + id + "): " + e.getMessage());
        }
        return null;
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
        } catch (SQLException e) {
            System.err.println("[MovieRepository] Lỗi khi lấy toàn bộ danh sách phim: " + e.getMessage());
        }
        return movieList;
    }

    /**
     * Thêm một phim mới vào cơ sở dữ liệu.
     * Tự động gán movie_id vừa sinh vào đối tượng Movie.
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
                ps.setDate(5, Date.valueOf(LocalDate.now()));
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
                return true;
            }
        } catch (SQLException e) {
            System.err.println("[MovieRepository] Lỗi khi thêm phim mới (" + movie.getTitle() + "): " + e.getMessage());
        }
        return false;
    }

    /**
     * Cập nhật thông tin chi tiết của phim.
     *
     * @param movie đối tượng phim cần cập nhật
     * @return true nếu cập nhật thành công, false nếu thất bại
     */
    public boolean update(Movie movie) {
        if (movie == null || movie.getMovieId() <= 0) {
            return false;
        }

        String sql = "UPDATE movies SET title = ?, genre = ?, duration = ?, director = ?, "
                   + "release_date = ?, rating = ?, description = ?, status = ? "
                   + "WHERE movie_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

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
            ps.setInt(9, movie.getMovieId());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[MovieRepository] Lỗi khi cập nhật phim ID " + movie.getMovieId() + ": " + e.getMessage());
        }
        return false;
    }

    /**
     * Cập nhật trạng thái hiển thị của phim (DANG_CHIEU hoặc NGUNG_CHIEU).
     *
     * @param movieId mã phim
     * @param status  trạng thái mới
     * @return true nếu cập nhật thành công, false nếu thất bại
     */
    public boolean updateStatus(int movieId, MovieStatus status) {
        if (movieId <= 0 || status == null) {
            return false;
        }

        String sql = "UPDATE movies SET status = ? WHERE movie_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, status.getDbValue());
            ps.setInt(2, movieId);

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[MovieRepository] Lỗi khi đổi trạng thái phim ID " + movieId + ": " + e.getMessage());
        }
        return false;
    }

    /**
     * Lấy danh sách phim theo thể loại kết hợp Collection Map để phân nhóm.
     *
     * @return Map với key là thể loại, value là danh sách phim thuộc thể loại đó
     */
    public Map<String, List<Movie>> groupMoviesByGenre() {
        Map<String, List<Movie>> map = new HashMap<>();
        List<Movie> all = findAll();
        for (Movie movie : all) {
            String[] genres = movie.getGenre().split(",");
            for (String g : genres) {
                String cleanG = g.trim();
                map.computeIfAbsent(cleanG, k -> new ArrayList<>()).add(movie);
            }
        }
        return map;
    }

    /**
     * Ánh xạ từ dòng ResultSet sang đối tượng Movie.
     *
     * @param rs ResultSet đang trỏ tới dòng hiện tại
     * @return đối tượng Movie đã nạp đầy đủ thông tin
     * @throws SQLException nếu có lỗi truy xuất dữ liệu
     */
    private Movie mapResultSetToMovie(ResultSet rs) throws SQLException {
        int id = rs.getInt("movie_id");
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

        return new Movie(id, title, genre, duration, director, releaseDate, rating, description, status, createdAt, updatedAt);
    }
}

