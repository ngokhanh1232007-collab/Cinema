package com.cinema.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Lớp đại diện cho phim trong hệ thống.
 * Tương ứng với bảng movies trong database.
 *
 * <p>Encapsulation: tất cả thuộc tính private, truy xuất qua getter/setter.</p>
 */
public class Movie {

    /** Mã phim (auto increment trong DB) */
    private int movieId;

    /** Tên phim */
    private String title;

    /** Thể loại phim (ví dụ: "Hành động, Gia đình") */
    private String genre;

    /** Thời lượng phim (phút) */
    private int duration;

    /** Đạo diễn */
    private String director;

    /** Ngày phát hành */
    private LocalDate releaseDate;

    /** Điểm đánh giá (0.0 - 10.0) */
    private double rating;

    /** Mô tả nội dung phim */
    private String description;

    /** Trạng thái phim (DANG_CHIEU, NGUNG_CHIEU) */
    private MovieStatus status;

    /** Ngày tạo bản ghi */
    private LocalDateTime createdAt;

    /** Ngày cập nhật gần nhất */
    private LocalDateTime updatedAt;

    /**
     * Constructor mặc định.
     */
    public Movie() {
        this.status = MovieStatus.DANG_CHIEU;
    }

    /**
     * Constructor với các thông tin cơ bản.
     *
     * @param movieId  mã phim
     * @param title    tên phim
     * @param genre    thể loại
     * @param duration thời lượng (phút)
     * @param director đạo diễn
     * @param status   trạng thái phim
     */
    public Movie(int movieId, String title, String genre, int duration,
                 String director, MovieStatus status) {
        this.movieId = movieId;
        this.title = title;
        this.genre = genre;
        this.duration = duration;
        this.director = director;
        this.status = status;
    }

    /**
     * Constructor đầy đủ tham số.
     *
     * @param movieId     mã phim
     * @param title       tên phim
     * @param genre       thể loại
     * @param duration    thời lượng (phút)
     * @param director    đạo diễn
     * @param releaseDate ngày phát hành
     * @param rating      điểm đánh giá
     * @param description mô tả
     * @param status      trạng thái
     * @param createdAt   ngày tạo
     * @param updatedAt   ngày cập nhật
     */
    public Movie(int movieId, String title, String genre, int duration,
                 String director, LocalDate releaseDate, double rating,
                 String description, MovieStatus status,
                 LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.movieId = movieId;
        this.title = title;
        this.genre = genre;
        this.duration = duration;
        this.director = director;
        this.releaseDate = releaseDate;
        this.rating = rating;
        this.description = description;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * Kiểm tra phim có đang chiếu không.
     *
     * @return true nếu phim đang chiếu
     */
    public boolean isShowing() {
        return status == MovieStatus.DANG_CHIEU;
    }

    /**
     * Trả về thông tin phim dạng hiển thị.
     *
     * @return chuỗi thông tin phim
     */
    public String getDisplayInfo() {
        return String.format("[%d] %s | %s | %d phút | ĐD: %s | %.1f★ | %s",
                movieId, title, genre, duration, director, rating,
                status.getDisplayName());
    }

    // ===================== GETTER / SETTER =====================

    public int getMovieId() {
        return movieId;
    }

    public void setMovieId(int movieId) {
        this.movieId = movieId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public String getDirector() {
        return director;
    }

    public void setDirector(String director) {
        this.director = director;
    }

    public LocalDate getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(LocalDate releaseDate) {
        this.releaseDate = releaseDate;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public MovieStatus getStatus() {
        return status;
    }

    public void setStatus(MovieStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "Movie{" +
                "movieId=" + movieId +
                ", title='" + title + '\'' +
                ", genre='" + genre + '\'' +
                ", duration=" + duration +
                ", director='" + director + '\'' +
                ", status=" + status +
                '}';
    }
}

