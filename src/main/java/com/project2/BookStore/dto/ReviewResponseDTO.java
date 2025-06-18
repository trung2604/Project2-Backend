package com.project2.BookStore.dto;

import com.project2.BookStore.model.Review;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponseDTO {
    private String id;
    private String userId;
    private String userName;
    private String userEmail;
    private String bookId;
    private String bookName;
    private String orderId;
    private Integer rating;
    private String comment;
    private Boolean isVerifiedPurchase;
    private Review.ReviewStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
} 