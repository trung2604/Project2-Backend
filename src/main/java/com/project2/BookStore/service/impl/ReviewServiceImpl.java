package com.project2.BookStore.service.impl;

import com.project2.BookStore.dto.ReviewRequestDTO;
import com.project2.BookStore.dto.ReviewResponseDTO;
import com.project2.BookStore.exception.BadRequestException;
import com.project2.BookStore.model.Book;
import com.project2.BookStore.model.Order;
import com.project2.BookStore.model.Review;
import com.project2.BookStore.model.User;
import com.project2.BookStore.repository.BookRepository;
import com.project2.BookStore.repository.OrderRepository;
import com.project2.BookStore.repository.ReviewRepository;
import com.project2.BookStore.repository.UserRepository;
import com.project2.BookStore.service.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    @Override
    @Transactional
    public ReviewResponseDTO createReview(ReviewRequestDTO request, String userId) {
        log.info("Creating review for user: {} and book: {}", userId, request.getBookId());
        
        // Kiểm tra sách tồn tại
        Book book = bookRepository.findById(request.getBookId())
            .orElseThrow(() -> new BadRequestException("Không tìm thấy sách"));
        
        // Kiểm tra user tồn tại
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new BadRequestException("Không tìm thấy người dùng"));
        
        // Kiểm tra user đã review sách này chưa
        if (reviewRepository.existsByUserIdAndBookIdAndStatus(userId, request.getBookId(), Review.ReviewStatus.ACTIVE)) {
            throw new BadRequestException("Bạn đã đánh giá sách này rồi");
        }
        
        // Kiểm tra user đã mua sách chưa (verified purchase)
        boolean isVerifiedPurchase = reviewRepository.hasUserPurchasedBook(userId, request.getBookId());
        
        // Tìm order thực tế nếu user đã mua sách
        Order order = null;
        if (request.getOrderId() != null && !request.getOrderId().trim().isEmpty()) {
            // Nếu có orderId trong request, tìm order đó
            order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new BadRequestException("Không tìm thấy đơn hàng"));
            
            // Kiểm tra order có thuộc về user này không
            if (!order.getUser().getId().equals(userId)) {
                throw new BadRequestException("Đơn hàng không thuộc về bạn");
            }
            
            // Kiểm tra order có chứa sách này không
            boolean hasBook = order.getOrderItems().stream()
                .anyMatch(item -> item.getBook().getId().equals(request.getBookId()));
            if (!hasBook) {
                throw new BadRequestException("Đơn hàng không chứa sách này");
            }
            
            // Kiểm tra order đã được giao chưa
            if (order.getStatus() != Order.OrderStatus.DELIVERED) {
                throw new BadRequestException("Chỉ có thể đánh giá sách sau khi đơn hàng đã được giao");
            }
            
            isVerifiedPurchase = true;
        } else if (isVerifiedPurchase) {
            // Nếu không có orderId nhưng user đã mua sách, tìm order gần nhất
            List<Order> orders = orderRepository.findByUserIdAndOrderItemsBookIdAndStatusOrderByCreatedAtDesc(
                userId, request.getBookId(), Order.OrderStatus.DELIVERED);
            if (!orders.isEmpty()) {
                order = orders.get(0); // Lấy order gần nhất
            }
        }
        
        // Tạo review mới
        Review review = new Review();
        review.setBook(book);
        review.setUser(user);
        review.setOrder(order); // Có thể null nếu user chưa mua
        review.setRating(request.getRating());
        review.setComment(request.getComment());
        review.setIsVerifiedPurchase(isVerifiedPurchase);
        review.setStatus(Review.ReviewStatus.ACTIVE); // Mặc định là ACTIVE
        review.setCreatedAt(LocalDateTime.now());
        review.setUpdatedAt(LocalDateTime.now());
        
        Review savedReview = reviewRepository.save(review);
        log.info("Review created successfully. ReviewId: {}", savedReview.getId());
        
        return convertToResponseDTO(savedReview);
    }

    @Override
    public ReviewResponseDTO getReviewById(String reviewId) {
        log.info("Getting review by ID: {}", reviewId);
        
        Review review = reviewRepository.findById(reviewId)
            .orElseThrow(() -> new BadRequestException("Không tìm thấy đánh giá"));
        
        return convertToResponseDTO(review);
    }

    @Override
    public Page<ReviewResponseDTO> getBookReviews(String bookId, Pageable pageable) {
        log.info("Getting reviews for book: {}", bookId);
        
        // Kiểm tra sách tồn tại
        if (!bookRepository.existsById(bookId)) {
            throw new BadRequestException("Không tìm thấy sách");
        }
        
        // Chỉ lấy review có trạng thái ACTIVE
        Page<Review> reviews = reviewRepository.findByBookIdAndStatusOrderByCreatedAtDesc(bookId, Review.ReviewStatus.ACTIVE, pageable);
        
        return reviews.map(this::convertToResponseDTO);
    }

    @Override
    public Page<ReviewResponseDTO> getUserReviews(String userId, Pageable pageable) {
        log.info("Getting reviews for user: {}", userId);
        
        // Kiểm tra user tồn tại
        if (!userRepository.existsById(userId)) {
            throw new BadRequestException("Không tìm thấy người dùng");
        }
        
        Page<Review> reviews = reviewRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        
        return reviews.map(this::convertToResponseDTO);
    }

    @Override
    public ReviewResponseDTO getUserReviewForBook(String userId, String bookId) {
        log.info("Getting user review for book. UserId: {}, BookId: {}", userId, bookId);
        
        // Kiểm tra user tồn tại
        if (!userRepository.existsById(userId)) {
            throw new BadRequestException("Không tìm thấy người dùng");
        }
        
        // Kiểm tra sách tồn tại
        if (!bookRepository.existsById(bookId)) {
            throw new BadRequestException("Không tìm thấy sách");
        }
        
        Review review = reviewRepository.findByUserIdAndBookIdAndStatus(userId, bookId, Review.ReviewStatus.ACTIVE)
            .orElseThrow(() -> new BadRequestException("Không tìm thấy đánh giá của bạn cho sách này"));
        
        return convertToResponseDTO(review);
    }

    @Override
    @Transactional
    public ReviewResponseDTO updateReview(String reviewId, ReviewRequestDTO request, String userId) {
        log.info("Updating review. ReviewId: {}, UserId: {}", reviewId, userId);
        
        Review review = reviewRepository.findById(reviewId)
            .orElseThrow(() -> new BadRequestException("Không tìm thấy đánh giá"));
        
        // Kiểm tra quyền sở hữu
        if (!review.getUser().getId().equals(userId)) {
            throw new BadRequestException("Bạn không có quyền cập nhật đánh giá này");
        }
        
        // Cập nhật thông tin
        review.setRating(request.getRating());
        review.setComment(request.getComment());
        review.setUpdatedAt(LocalDateTime.now());
        
        Review updatedReview = reviewRepository.save(review);
        log.info("Review updated successfully. ReviewId: {}", reviewId);
        
        return convertToResponseDTO(updatedReview);
    }

    @Override
    @Transactional
    public ReviewResponseDTO deleteReview(String reviewId, String userId) {
        log.info("Deleting review. ReviewId: {}, UserId: {}", reviewId, userId);
        
        Review review = reviewRepository.findById(reviewId)
            .orElseThrow(() -> new BadRequestException("Không tìm thấy đánh giá"));
        
        // Kiểm tra quyền sở hữu
        if (!review.getUser().getId().equals(userId)) {
            throw new BadRequestException("Bạn không có quyền xóa đánh giá này");
        }
        
        ReviewResponseDTO deletedReview = convertToResponseDTO(review);
        
        reviewRepository.delete(review);
        log.info("Review deleted successfully. ReviewId: {}", reviewId);
        
        return deletedReview;
    }

    @Override
    public Page<ReviewResponseDTO> getAllReviewsForAdmin(Pageable pageable, String search, Integer rating) {
        log.info("Getting all reviews for admin. Search: {}, Rating: {}", search, rating);
        Page<Review> reviews;
        
        // Xử lý tìm kiếm và lọc theo các điều kiện
        boolean hasSearch = search != null && !search.trim().isEmpty();
        boolean hasRating = rating != null;
        
        if (hasSearch && hasRating) {
            // Tìm kiếm tổng hợp + lọc theo số sao
            reviews = reviewRepository.findByCommentOrUserNameOrBookNameContainingIgnoreCaseAndRating(
                search.trim(), rating, pageable);
        } else if (hasSearch) {
            // Chỉ tìm kiếm tổng hợp - sử dụng cách tiếp cận khác
            reviews = searchReviewsByMultipleFields(search.trim(), pageable);
        } else if (hasRating) {
            // Chỉ lọc theo số sao
            reviews = reviewRepository.findByRating(rating, pageable);
        } else {
            // Không có điều kiện nào, lấy tất cả
            reviews = reviewRepository.findAllByOrderByCreatedAtDesc(pageable);
        }
        
        log.info("Found {} reviews for admin", reviews.getTotalElements());
        return reviews.map(this::convertToResponseDTO);
    }
    
    // Helper method để tìm kiếm theo nhiều trường
    private Page<Review> searchReviewsByMultipleFields(String search, Pageable pageable) {
        log.info("Searching reviews by multiple fields: {}", search);
        
        // Lấy tất cả review để tìm kiếm
        List<Review> allReviews = reviewRepository.findAllReviewsForDebug();
        
        // Lọc theo từ khóa tìm kiếm
        List<Review> filteredReviews = allReviews.stream()
            .filter(review -> {
                String searchLower = search.toLowerCase();
                
                // Tìm trong comment
                boolean matchComment = review.getComment() != null && 
                    review.getComment().toLowerCase().contains(searchLower);
                
                // Tìm trong tên user
                boolean matchUserName = review.getUser().getFullName() != null && 
                    review.getUser().getFullName().toLowerCase().contains(searchLower);
                
                // Tìm trong tên sách
                boolean matchBookName = review.getBook().getMainText() != null && 
                    review.getBook().getMainText().toLowerCase().contains(searchLower);
                
                return matchComment || matchUserName || matchBookName;
            })
            .sorted((r1, r2) -> r2.getCreatedAt().compareTo(r1.getCreatedAt())) // Sắp xếp theo thời gian tạo DESC
            .collect(Collectors.toList());
        
        // Tính toán phân trang
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filteredReviews.size());
        
        if (start > filteredReviews.size()) {
            return new PageImpl<>(new ArrayList<>(), pageable, filteredReviews.size());
        }
        
        List<Review> pageContent = filteredReviews.subList(start, end);
        return new PageImpl<>(pageContent, pageable, filteredReviews.size());
    }

    @Override
    public Map<String, Object> getBookRating(String bookId) {
        log.info("Getting book rating for bookId: {}", bookId);
        
        // Kiểm tra sách tồn tại
        if (!bookRepository.existsById(bookId)) {
            throw new BadRequestException("Không tìm thấy sách với ID: " + bookId);
        }
        
        // Lấy tất cả rating của sách
        List<Review> reviews = reviewRepository.findByBookIdAndStatus(bookId, Review.ReviewStatus.ACTIVE);
        
        if (reviews.isEmpty()) {
            Map<String, Object> result = new HashMap<>();
            result.put("averageRating", 0.0);
            result.put("totalReviews", 0);
            result.put("ratingDistribution", new HashMap<>());
            return result;
        }
        
        // Tính toán rating
        double totalRating = 0.0;
        Map<Integer, Integer> ratingDistribution = new HashMap<>();
        
        for (Review review : reviews) {
            if (review.getRating() != null) {
                int rating = review.getRating();
                totalRating += rating;
                ratingDistribution.put(rating, ratingDistribution.getOrDefault(rating, 0) + 1);
            }
        }
        
        double averageRating = totalRating / reviews.size();
        
        Map<String, Object> result = new HashMap<>();
        result.put("averageRating", Math.round(averageRating * 10.0) / 10.0); // Làm tròn 1 chữ số thập phân
        result.put("totalReviews", reviews.size());
        result.put("ratingDistribution", ratingDistribution);
        
        log.info("Book rating calculated. Average: {}, Total: {}", averageRating, reviews.size());
        return result;
    }

    @Override
    public Map<String, Object> getBookReviewSummary(String bookId) {
        log.info("Getting book review summary for bookId: {}", bookId);
        
        // Kiểm tra sách tồn tại
        if (!bookRepository.existsById(bookId)) {
            throw new BadRequestException("Không tìm thấy sách với ID: " + bookId);
        }
        
        // Lấy thông tin rating
        Map<String, Object> ratingInfo = getBookRating(bookId);
        
        // Lấy thống kê trạng thái review
        long totalReviews = reviewRepository.countByBookId(bookId);
        long activeReviews = reviewRepository.countByBookIdAndStatus(bookId, Review.ReviewStatus.ACTIVE);
        long hiddenReviews = reviewRepository.countByBookIdAndStatus(bookId, Review.ReviewStatus.HIDDEN);
        long deletedReviews = reviewRepository.countByBookIdAndStatus(bookId, Review.ReviewStatus.DELETED);
        
        Map<String, Object> summary = new HashMap<>();
        summary.put("bookId", bookId);
        summary.put("totalReviews", totalReviews);
        summary.put("activeReviews", activeReviews);
        summary.put("hiddenReviews", hiddenReviews);
        summary.put("deletedReviews", deletedReviews);
        summary.put("averageRating", ratingInfo.get("averageRating"));
        summary.put("ratingDistribution", ratingInfo.get("ratingDistribution"));
        
        log.info("Review summary calculated. Total: {}, Active: {}, Hidden: {}, Deleted: {}", 
            totalReviews, activeReviews, hiddenReviews, deletedReviews);
        return summary;
    }

    // Debug methods
    @Override
    public long countAllReviews() {
        return reviewRepository.countAllReviews();
    }
    
    @Override
    public List<ReviewResponseDTO> getSampleReviewsForDebug() {
        List<Review> reviews = reviewRepository.findAllReviewsForDebug();
        return reviews.stream()
            .limit(5) // Chỉ lấy 5 review đầu tiên
            .map(this::convertToResponseDTO)
            .collect(Collectors.toList());
    }
    
    @Override
    public Map<String, Object> debugSearch(String search) {
        Map<String, Object> results = new HashMap<>();
        
        // Tìm kiếm theo comment
        List<Review> commentResults = reviewRepository.findByCommentContainingDebug(search);
        results.put("commentResults", commentResults.stream()
            .map(this::convertToResponseDTO)
            .collect(Collectors.toList()));
        
        // Tìm kiếm theo tên user
        List<Review> userResults = reviewRepository.findByUserNameContainingDebug(search);
        results.put("userResults", userResults.stream()
            .map(this::convertToResponseDTO)
            .collect(Collectors.toList()));
        
        // Tìm kiếm theo tên sách
        List<Review> bookResults = reviewRepository.findByBookNameContainingDebug(search);
        results.put("bookResults", bookResults.stream()
            .map(this::convertToResponseDTO)
            .collect(Collectors.toList()));
        
        return results;
    }

    private ReviewResponseDTO convertToResponseDTO(Review review) {
        ReviewResponseDTO dto = new ReviewResponseDTO();
        dto.setId(review.getId());
        dto.setBookId(review.getBook().getId());
        dto.setBookName(review.getBook().getMainText());
        dto.setUserId(review.getUser().getId());
        dto.setUserName(review.getUser().getFullName());
        dto.setUserEmail(review.getUser().getEmail());
        dto.setOrderId(review.getOrder() != null ? review.getOrder().getId() : null);
        dto.setRating(review.getRating());
        dto.setComment(review.getComment());
        dto.setIsVerifiedPurchase(review.getIsVerifiedPurchase());
        dto.setStatus(review.getStatus());
        dto.setCreatedAt(review.getCreatedAt());
        dto.setUpdatedAt(review.getUpdatedAt());
        return dto;
    }
} 