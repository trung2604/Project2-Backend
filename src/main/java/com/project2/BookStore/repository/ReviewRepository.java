package com.project2.BookStore.repository;

import com.project2.BookStore.model.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, String> {
    
    // Tìm tất cả review của một sách (chỉ ACTIVE)
    Page<Review> findByBookIdAndStatusOrderByCreatedAtDesc(String bookId, Review.ReviewStatus status, Pageable pageable);
    
    // Tìm tất cả review của một user
    Page<Review> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
    
    // Kiểm tra user đã đánh giá sách này chưa
    boolean existsByUserIdAndBookIdAndStatus(String userId, String bookId, Review.ReviewStatus status);
    
    // Tìm review của user cho một sách cụ thể
    Optional<Review> findByUserIdAndBookIdAndStatus(String userId, String bookId, Review.ReviewStatus status);
    
    // Kiểm tra user đã mua sách này chưa (có đơn hàng DELIVERED)
    @Query("SELECT COUNT(o) > 0 FROM Order o " +
           "JOIN o.orderItems oi " +
           "WHERE o.user.id = :userId " +
           "AND oi.book.id = :bookId " +
           "AND o.status = 'DELIVERED'")
    boolean hasUserPurchasedBook(@Param("userId") String userId, @Param("bookId") String bookId);
    
    // Lấy thống kê rating của một sách
    @Query("SELECT AVG(r.rating), COUNT(r), " +
           "SUM(CASE WHEN r.rating = 1 THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN r.rating = 2 THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN r.rating = 3 THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN r.rating = 4 THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN r.rating = 5 THEN 1 ELSE 0 END) " +
           "FROM Review r WHERE r.book.id = :bookId AND r.status = 'ACTIVE' AND r.rating IS NOT NULL")
    Object[] getBookRatingStats(@Param("bookId") String bookId);
    
    // Lấy số sao đánh giá trung bình của một sách
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.book.id = :bookId AND r.status = 'ACTIVE' AND r.rating IS NOT NULL")
    Double getAverageRatingByBookId(@Param("bookId") String bookId);
    
    // Đếm số review đã xác minh mua hàng
    @Query("SELECT COUNT(r) FROM Review r WHERE r.book.id = :bookId AND r.isVerifiedPurchase = true AND r.status = 'ACTIVE'")
    Integer countVerifiedReviewsByBookId(@Param("bookId") String bookId);
    
    // Tìm review theo order và book
    Optional<Review> findByOrderIdAndBookId(String orderId, String bookId);
    
    // Lấy review theo trạng thái (cho admin)
    Page<Review> findByStatusOrderByCreatedAtDesc(Review.ReviewStatus status, Pageable pageable);
    
    // Tìm kiếm review theo từ khóa (cho admin)
    @Query("SELECT r FROM Review r WHERE r.comment LIKE %:keyword% AND r.status = :status")
    Page<Review> findByCommentContainingAndStatus(@Param("keyword") String keyword, 
                                                 @Param("status") Review.ReviewStatus status, 
                                                 Pageable pageable);
    
    // Debug methods để kiểm tra số lượng review
    long countByBookId(String bookId);
    long countByBookIdAndStatus(String bookId, Review.ReviewStatus status);
    
    // Lấy tất cả review của một sách (không phân biệt trạng thái)
    List<Review> findByBookId(String bookId);
    
    // Query đơn giản để test rating calculation
    @Query("SELECT r.rating FROM Review r WHERE r.book.id = :bookId AND r.status = 'ACTIVE'")
    List<Integer> getRatingsByBookId(@Param("bookId") String bookId);
    
    // Method để tìm kiếm theo comment (không lọc trạng thái)
    @Query("SELECT r FROM Review r WHERE r.comment LIKE %:keyword% ORDER BY r.createdAt DESC")
    Page<Review> findByCommentContaining(@Param("keyword") String keyword, Pageable pageable);
    
    // Method để lấy tất cả review sắp xếp theo thời gian tạo
    Page<Review> findAllByOrderByCreatedAtDesc(Pageable pageable);
    
    // Method để lấy review theo bookId và status
    List<Review> findByBookIdAndStatus(String bookId, Review.ReviewStatus status);
    
    // ===== CÁC METHOD MỚI CHO TÌM KIẾM VÀ LỌC =====
    
    // Tìm kiếm theo tên người đánh giá
    @Query("SELECT r FROM Review r WHERE LOWER(r.user.fullName) LIKE LOWER(CONCAT('%', :userName, '%')) ORDER BY r.createdAt DESC")
    Page<Review> findByUserNameContainingIgnoreCase(@Param("userName") String userName, Pageable pageable);
    
    // Tìm kiếm theo tên sách
    @Query("SELECT r FROM Review r WHERE LOWER(r.book.mainText) LIKE LOWER(CONCAT('%', :bookName, '%')) ORDER BY r.createdAt DESC")
    Page<Review> findByBookNameContainingIgnoreCase(@Param("bookName") String bookName, Pageable pageable);
    
    // Lọc theo số sao đánh giá
    @Query("SELECT r FROM Review r WHERE r.rating = :rating ORDER BY r.createdAt DESC")
    Page<Review> findByRating(@Param("rating") Integer rating, Pageable pageable);
    
    // Tìm kiếm theo comment và tên người đánh giá
    @Query("SELECT r FROM Review r WHERE r.comment LIKE %:search% AND LOWER(r.user.fullName) LIKE LOWER(CONCAT('%', :userName, '%')) ORDER BY r.createdAt DESC")
    Page<Review> findByCommentContainingAndUserNameContainingIgnoreCase(
        @Param("search") String search, 
        @Param("userName") String userName, 
        Pageable pageable);
    
    // Tìm kiếm theo comment và tên sách
    @Query("SELECT r FROM Review r WHERE r.comment LIKE %:search% AND LOWER(r.book.mainText) LIKE LOWER(CONCAT('%', :bookName, '%')) ORDER BY r.createdAt DESC")
    Page<Review> findByCommentContainingAndBookNameContainingIgnoreCase(
        @Param("search") String search, 
        @Param("bookName") String bookName, 
        Pageable pageable);
    
    // Tìm kiếm theo comment và số sao
    @Query("SELECT r FROM Review r WHERE r.comment LIKE %:search% AND r.rating = :rating ORDER BY r.createdAt DESC")
    Page<Review> findByCommentContainingAndRating(
        @Param("search") String search, 
        @Param("rating") Integer rating, 
        Pageable pageable);
    
    // Tìm kiếm theo tên người đánh giá và tên sách
    @Query("SELECT r FROM Review r WHERE LOWER(r.user.fullName) LIKE LOWER(CONCAT('%', :userName, '%')) AND LOWER(r.book.mainText) LIKE LOWER(CONCAT('%', :bookName, '%')) ORDER BY r.createdAt DESC")
    Page<Review> findByUserNameContainingIgnoreCaseAndBookNameContainingIgnoreCase(
        @Param("userName") String userName, 
        @Param("bookName") String bookName, 
        Pageable pageable);
    
    // Tìm kiếm theo tên người đánh giá và số sao
    @Query("SELECT r FROM Review r WHERE LOWER(r.user.fullName) LIKE LOWER(CONCAT('%', :userName, '%')) AND r.rating = :rating ORDER BY r.createdAt DESC")
    Page<Review> findByUserNameContainingIgnoreCaseAndRating(
        @Param("userName") String userName, 
        @Param("rating") Integer rating, 
        Pageable pageable);
    
    // Tìm kiếm theo tên sách và số sao
    @Query("SELECT r FROM Review r WHERE LOWER(r.book.mainText) LIKE LOWER(CONCAT('%', :bookName, '%')) AND r.rating = :rating ORDER BY r.createdAt DESC")
    Page<Review> findByBookNameContainingIgnoreCaseAndRating(
        @Param("bookName") String bookName, 
        @Param("rating") Integer rating, 
        Pageable pageable);
    
    // Tìm kiếm theo comment, tên người đánh giá và tên sách
    @Query("SELECT r FROM Review r WHERE r.comment LIKE %:search% AND LOWER(r.user.fullName) LIKE LOWER(CONCAT('%', :userName, '%')) AND LOWER(r.book.mainText) LIKE LOWER(CONCAT('%', :bookName, '%')) ORDER BY r.createdAt DESC")
    Page<Review> findByCommentContainingAndUserNameContainingIgnoreCaseAndBookNameContainingIgnoreCase(
        @Param("search") String search, 
        @Param("userName") String userName, 
        @Param("bookName") String bookName, 
        Pageable pageable);
    
    // Tìm kiếm theo comment, tên người đánh giá và số sao
    @Query("SELECT r FROM Review r WHERE r.comment LIKE %:search% AND LOWER(r.user.fullName) LIKE LOWER(CONCAT('%', :userName, '%')) AND r.rating = :rating ORDER BY r.createdAt DESC")
    Page<Review> findByCommentContainingAndUserNameContainingIgnoreCaseAndRating(
        @Param("search") String search, 
        @Param("userName") String userName, 
        @Param("rating") Integer rating, 
        Pageable pageable);
    
    // Tìm kiếm theo comment, tên sách và số sao
    @Query("SELECT r FROM Review r WHERE r.comment LIKE %:search% AND LOWER(r.book.mainText) LIKE LOWER(CONCAT('%', :bookName, '%')) AND r.rating = :rating ORDER BY r.createdAt DESC")
    Page<Review> findByCommentContainingAndBookNameContainingIgnoreCaseAndRating(
        @Param("search") String search, 
        @Param("bookName") String bookName, 
        @Param("rating") Integer rating, 
        Pageable pageable);
    
    // Tìm kiếm theo tên người đánh giá, tên sách và số sao
    @Query("SELECT r FROM Review r WHERE LOWER(r.user.fullName) LIKE LOWER(CONCAT('%', :userName, '%')) AND LOWER(r.book.mainText) LIKE LOWER(CONCAT('%', :bookName, '%')) AND r.rating = :rating ORDER BY r.createdAt DESC")
    Page<Review> findByUserNameContainingIgnoreCaseAndBookNameContainingIgnoreCaseAndRating(
        @Param("userName") String userName, 
        @Param("bookName") String bookName, 
        @Param("rating") Integer rating, 
        Pageable pageable);
    
    // Tìm kiếm theo tất cả điều kiện: comment, tên người đánh giá, tên sách và số sao
    @Query("SELECT r FROM Review r WHERE r.comment LIKE %:search% AND LOWER(r.user.fullName) LIKE LOWER(CONCAT('%', :userName, '%')) AND LOWER(r.book.mainText) LIKE LOWER(CONCAT('%', :bookName, '%')) AND r.rating = :rating ORDER BY r.createdAt DESC")
    Page<Review> findByCommentContainingAndUserNameContainingIgnoreCaseAndBookNameContainingIgnoreCaseAndRating(
        @Param("search") String search, 
        @Param("userName") String userName, 
        @Param("bookName") String bookName, 
        @Param("rating") Integer rating, 
        Pageable pageable);
    
    // ===== METHOD MỚI CHO TÌM KIẾM TỔNG HỢP =====
    
    // Tìm kiếm tổng hợp: comment HOẶC tên người đánh giá HOẶC tên sách
    @Query("SELECT DISTINCT r FROM Review r " +
           "WHERE r.comment LIKE %:search% " +
           "OR LOWER(r.user.fullName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(r.book.mainText) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "ORDER BY r.createdAt DESC")
    Page<Review> findByCommentOrUserNameOrBookNameContainingIgnoreCase(
        @Param("search") String search, 
        Pageable pageable);
    
    // Tìm kiếm tổng hợp + lọc theo số sao
    @Query("SELECT DISTINCT r FROM Review r " +
           "WHERE (r.comment LIKE %:search% " +
           "OR LOWER(r.user.fullName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(r.book.mainText) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "AND r.rating = :rating " +
           "ORDER BY r.createdAt DESC")
    Page<Review> findByCommentOrUserNameOrBookNameContainingIgnoreCaseAndRating(
        @Param("search") String search, 
        @Param("rating") Integer rating, 
        Pageable pageable);
    
    // ===== METHOD DEBUG =====
    
    // Đếm tổng số review
    @Query("SELECT COUNT(r) FROM Review r")
    long countAllReviews();
    
    // Lấy một số review để debug
    @Query("SELECT r FROM Review r ORDER BY r.createdAt DESC")
    List<Review> findAllReviewsForDebug();
    
    // Tìm kiếm theo comment đơn giản
    @Query("SELECT r FROM Review r WHERE r.comment LIKE %:search%")
    List<Review> findByCommentContainingDebug(@Param("search") String search);
    
    // Tìm kiếm theo tên user đơn giản
    @Query("SELECT r FROM Review r WHERE LOWER(r.user.fullName) LIKE LOWER(CONCAT('%', :search, '%'))")
    List<Review> findByUserNameContainingDebug(@Param("search") String search);
    
    // Tìm kiếm theo tên sách đơn giản
    @Query("SELECT r FROM Review r WHERE LOWER(r.book.mainText) LIKE LOWER(CONCAT('%', :search, '%'))")
    List<Review> findByBookNameContainingDebug(@Param("search") String search);
} 