package com.project2.BookStore.service;

import com.project2.BookStore.dto.ReviewRequestDTO;
import com.project2.BookStore.dto.ReviewResponseDTO;
import com.project2.BookStore.model.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface ReviewService {
    ReviewResponseDTO createReview(ReviewRequestDTO request, String userId);
    ReviewResponseDTO getReviewById(String reviewId);
    Page<ReviewResponseDTO> getBookReviews(String bookId, Pageable pageable);
    Page<ReviewResponseDTO> getUserReviews(String userId, Pageable pageable);
    ReviewResponseDTO getUserReviewForBook(String userId, String bookId);
    ReviewResponseDTO updateReview(String reviewId, ReviewRequestDTO request, String userId);
    ReviewResponseDTO deleteReview(String reviewId, String userId);
    Page<ReviewResponseDTO> getAllReviewsForAdmin(Pageable pageable, String search, Integer rating);
    
    // Thêm method mới để lấy thông tin rating cơ bản cho sách
    Map<String, Object> getBookRating(String bookId);
    Map<String, Object> getBookReviewSummary(String bookId);
    
    // Debug methods
    long countAllReviews();
    List<ReviewResponseDTO> getSampleReviewsForDebug();
    Map<String, Object> debugSearch(String search);
} 