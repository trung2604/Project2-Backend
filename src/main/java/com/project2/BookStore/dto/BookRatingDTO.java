package com.project2.BookStore.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookRatingDTO {
    private String bookId;
    private String bookName;
    private Double averageRating;
    private Integer totalReviews;
    private Integer verifiedReviews;
    
    // Constructor cho trường hợp không có đánh giá
    public BookRatingDTO(String bookId, String bookName) {
        this.bookId = bookId;
        this.bookName = bookName;
        this.averageRating = 0.0;
        this.totalReviews = 0;
        this.verifiedReviews = 0;
    }
} 