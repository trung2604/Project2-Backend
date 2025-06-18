package com.project2.BookStore.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookReviewSummaryDTO {
    private String bookId;
    private String bookName;
    private Double averageRating;
    private Integer totalReviews;
    private Integer verifiedReviews;
    private RatingDistribution ratingDistribution;
    private List<ReviewResponseDTO> recentReviews;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RatingDistribution {
        private Integer oneStar;
        private Integer twoStar;
        private Integer threeStar;
        private Integer fourStar;
        private Integer fiveStar;
    }
} 