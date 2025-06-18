package com.project2.BookStore.controller;

import com.project2.BookStore.dto.ApiResponseDTO;
import com.project2.BookStore.dto.ReviewRequestDTO;
import com.project2.BookStore.dto.ReviewResponseDTO;
import com.project2.BookStore.exception.BadRequestException;
import com.project2.BookStore.model.Review;
import com.project2.BookStore.repository.UserRepository;
import com.project2.BookStore.service.ReviewService;
import com.project2.BookStore.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/bookStore/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    private String getCurrentUserId() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new BadRequestException("Token không hợp lệ");
        }
        String token = authHeader.substring(7);
        return jwtUtil.getUserIdFromToken(token);
    }

    @PostMapping
    public ResponseEntity<ApiResponseDTO> createReview(@Valid @RequestBody ReviewRequestDTO request) {
        log.info("Creating review for book. BookId: {}", request.getBookId());
        try {
            String userId = getCurrentUserId();
            log.debug("Creating review for user: {} and book: {}", userId, request.getBookId());
            
            ReviewResponseDTO createdReview = reviewService.createReview(request, userId);
            log.info("Review created successfully. ReviewId: {}", createdReview.getId());
            
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponseDTO(true, "Tạo đánh giá thành công", createdReview));
        } catch (BadRequestException e) {
            log.warn("Failed to create review: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(new ApiResponseDTO(false, e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error creating review: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponseDTO(false, "Lỗi server khi tạo đánh giá", null));
        }
    }

    @GetMapping("/{reviewId}")
    public ResponseEntity<ApiResponseDTO> getReviewById(@PathVariable String reviewId) {
        log.info("Getting review by ID. ReviewId: {}", reviewId);
        try {
            ReviewResponseDTO review = reviewService.getReviewById(reviewId);
            log.debug("Review found. ReviewId: {}", reviewId);
            
            return ResponseEntity.ok(new ApiResponseDTO(true, "Lấy đánh giá thành công", review));
        } catch (BadRequestException e) {
            log.warn("Failed to get review: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(new ApiResponseDTO(false, e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error getting review: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponseDTO(false, "Lỗi server khi lấy đánh giá", null));
        }
    }

    @GetMapping("/book/{bookId}")
    public ResponseEntity<ApiResponseDTO> getBookReviews(
            @PathVariable String bookId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Getting reviews for book. BookId: {}, Page: {}, Size: {}", bookId, page, size);
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<ReviewResponseDTO> reviewsPage = reviewService.getBookReviews(bookId, pageable);
            log.debug("Found {} reviews for book", reviewsPage.getTotalElements());

            Map<String, Object> meta = new HashMap<>();
            meta.put("current", page);
            meta.put("pageSize", size);
            meta.put("pages", reviewsPage.getTotalPages());
            meta.put("total", reviewsPage.getTotalElements());

            Map<String, Object> data = new HashMap<>();
            data.put("meta", meta);
            data.put("result", reviewsPage.getContent());

            return ResponseEntity.ok(new ApiResponseDTO(true, "Lấy danh sách đánh giá thành công", data));
        } catch (BadRequestException e) {
            log.warn("Failed to get book reviews: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(new ApiResponseDTO(false, e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error getting book reviews: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponseDTO(false, "Lỗi server khi lấy danh sách đánh giá", null));
        }
    }

    @GetMapping("/book/{bookId}/rating")
    public ResponseEntity<ApiResponseDTO> getBookRating(@PathVariable String bookId) {
        log.info("Getting rating for book. BookId: {}", bookId);
        try {
            Map<String, Object> ratingInfo = reviewService.getBookRating(bookId);
            log.debug("Rating info retrieved for book: {}", bookId);
            
            return ResponseEntity.ok(new ApiResponseDTO(true, "Lấy thông tin đánh giá thành công", ratingInfo));
        } catch (BadRequestException e) {
            log.warn("Failed to get book rating: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(new ApiResponseDTO(false, e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error getting book rating: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponseDTO(false, "Lỗi server khi lấy thông tin đánh giá", null));
        }
    }

    @GetMapping("/book/{bookId}/summary")
    public ResponseEntity<ApiResponseDTO> getBookReviewSummary(@PathVariable String bookId) {
        log.info("Getting review summary for book. BookId: {}", bookId);
        try {
            Map<String, Object> summary = reviewService.getBookReviewSummary(bookId);
            log.debug("Review summary retrieved for book: {}", bookId);
            
            return ResponseEntity.ok(new ApiResponseDTO(true, "Lấy tóm tắt đánh giá thành công", summary));
        } catch (BadRequestException e) {
            log.warn("Failed to get review summary: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(new ApiResponseDTO(false, e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error getting review summary: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponseDTO(false, "Lỗi server khi lấy tóm tắt đánh giá", null));
        }
    }

    @GetMapping("/user")
    public ResponseEntity<ApiResponseDTO> getUserReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Getting user reviews. Page: {}, Size: {}", page, size);
        try {
            String userId = getCurrentUserId();
            log.debug("Getting reviews for user: {}", userId);
            
            Pageable pageable = PageRequest.of(page, size);
            Page<ReviewResponseDTO> reviewsPage = reviewService.getUserReviews(userId, pageable);
            log.debug("Found {} reviews for user", reviewsPage.getTotalElements());

            Map<String, Object> meta = new HashMap<>();
            meta.put("current", page);
            meta.put("pageSize", size);
            meta.put("pages", reviewsPage.getTotalPages());
            meta.put("total", reviewsPage.getTotalElements());

            Map<String, Object> data = new HashMap<>();
            data.put("meta", meta);
            data.put("result", reviewsPage.getContent());

            return ResponseEntity.ok(new ApiResponseDTO(true, "Lấy danh sách đánh giá thành công", data));
        } catch (BadRequestException e) {
            log.warn("Failed to get user reviews: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(new ApiResponseDTO(false, e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error getting user reviews: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponseDTO(false, "Lỗi server khi lấy danh sách đánh giá", null));
        }
    }

    @GetMapping("/user/book/{bookId}")
    public ResponseEntity<ApiResponseDTO> getUserReviewForBook(@PathVariable String bookId) {
        log.info("Getting user review for book. BookId: {}", bookId);
        try {
            String userId = getCurrentUserId();
            log.debug("Getting review for user: {} and book: {}", userId, bookId);
            
            ReviewResponseDTO review = reviewService.getUserReviewForBook(userId, bookId);
            log.debug("Review found for user and book");
            
            return ResponseEntity.ok(new ApiResponseDTO(true, "Lấy đánh giá thành công", review));
        } catch (BadRequestException e) {
            log.warn("Failed to get user review for book: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(new ApiResponseDTO(false, e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error getting user review for book: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponseDTO(false, "Lỗi server khi lấy đánh giá", null));
        }
    }

    @PutMapping("/{reviewId}")
    public ResponseEntity<ApiResponseDTO> updateReview(
            @PathVariable String reviewId,
            @Valid @RequestBody ReviewRequestDTO request) {
        log.info("Updating review. ReviewId: {}", reviewId);
        try {
            String userId = getCurrentUserId();
            log.debug("Updating review {} for user: {}", reviewId, userId);
            
            ReviewResponseDTO updatedReview = reviewService.updateReview(reviewId, request, userId);
            log.info("Review updated successfully. ReviewId: {}", reviewId);
            
            return ResponseEntity.ok(new ApiResponseDTO(true, "Cập nhật đánh giá thành công", updatedReview));
        } catch (BadRequestException e) {
            log.warn("Failed to update review: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(new ApiResponseDTO(false, e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error updating review: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponseDTO(false, "Lỗi server khi cập nhật đánh giá", null));
        }
    }

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<ApiResponseDTO> deleteReview(@PathVariable String reviewId) {
        log.info("Deleting review. ReviewId: {}", reviewId);
        try {
            String userId = getCurrentUserId();
            log.debug("Deleting review {} for user: {}", reviewId, userId);
            
            ReviewResponseDTO deletedReview = reviewService.deleteReview(reviewId, userId);
            log.info("Review deleted successfully. ReviewId: {}", reviewId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("deletedReview", deletedReview);
            response.put("message", "Đã xóa đánh giá " + reviewId);
            return ResponseEntity.ok(new ApiResponseDTO(true, "Xóa đánh giá thành công", response));
        } catch (BadRequestException e) {
            log.warn("Failed to delete review: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(new ApiResponseDTO(false, e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error deleting review: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponseDTO(false, "Lỗi server khi xóa đánh giá", null));
        }
    }

    // Admin endpoints
    @GetMapping("/admin")
    public ResponseEntity<ApiResponseDTO> getAllReviewsForAdmin(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Integer rating) {
        log.info("Getting all reviews for admin. Page: {}, Size: {}, Search: {}, Rating: {}", 
            page, size, search, rating);
        try {
            // Kiểm tra quyền admin
            HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponseDTO(false, "Token không hợp lệ", null));
            }
            String token = authHeader.substring(7);

            if (!jwtUtil.hasRole(token, "ROLE_ADMIN")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponseDTO(false, "Không có quyền truy cập", null));
            }

            Pageable pageable = PageRequest.of(page, size);
            Page<ReviewResponseDTO> reviewsPage = reviewService.getAllReviewsForAdmin(pageable, search, rating);
            log.debug("Found {} reviews for admin", reviewsPage.getTotalElements());

            Map<String, Object> meta = new HashMap<>();
            meta.put("current", page);
            meta.put("pageSize", size);
            meta.put("pages", reviewsPage.getTotalPages());
            meta.put("total", reviewsPage.getTotalElements());

            Map<String, Object> data = new HashMap<>();
            data.put("meta", meta);
            data.put("result", reviewsPage.getContent());

            return ResponseEntity.ok(new ApiResponseDTO(true, "Lấy danh sách đánh giá thành công", data));
        } catch (BadRequestException e) {
            log.warn("Failed to get reviews for admin: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(new ApiResponseDTO(false, e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error getting reviews for admin: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponseDTO(false, "Lỗi server khi lấy danh sách đánh giá", null));
        }
    }

    // Debug endpoint
    @GetMapping("/debug")
    public ResponseEntity<ApiResponseDTO> debugReviews(@RequestParam(required = false) String search) {
        log.info("Debug reviews. Search: {}", search);
        try {
            // Kiểm tra quyền admin
            HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponseDTO(false, "Token không hợp lệ", null));
            }
            String token = authHeader.substring(7);

            if (!jwtUtil.hasRole(token, "ROLE_ADMIN")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponseDTO(false, "Không có quyền truy cập", null));
            }

            Map<String, Object> debugInfo = new HashMap<>();
            
            // Đếm tổng số review
            long totalReviews = reviewService.countAllReviews();
            debugInfo.put("totalReviews", totalReviews);
            
            // Lấy một số review để debug
            List<ReviewResponseDTO> sampleReviews = reviewService.getSampleReviewsForDebug();
            debugInfo.put("sampleReviews", sampleReviews);
            
            if (search != null && !search.trim().isEmpty()) {
                // Tìm kiếm debug
                Map<String, Object> searchResults = reviewService.debugSearch(search.trim());
                debugInfo.put("searchResults", searchResults);
            }
            
            return ResponseEntity.ok(new ApiResponseDTO(true, "Debug info retrieved", debugInfo));
        } catch (Exception e) {
            log.error("Error in debug: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponseDTO(false, "Lỗi server: " + e.getMessage(), null));
        }
    }
} 