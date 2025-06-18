package com.project2.BookStore.dto;

import com.project2.BookStore.model.Order.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class ReportDTO {
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RevenueReport {
        private BigDecimal totalRevenue;
        private int totalOrders;
        private BigDecimal averageOrderValue;
        private List<DailyRevenue> dailyStats;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyRevenue {
        private LocalDate date;
        private BigDecimal revenue;
        private int orderCount;
        private BigDecimal averageOrderValue;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderStatusReport {
        private OrderStatus status;
        private int orderCount;
        private BigDecimal totalAmount;
        private double percentage;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopSellingBook {
        private String bookId;
        private String mainText;
        private String author;
        private String categoryName;
        private long totalSold;
        private BigDecimal totalRevenue;
        private double averageRating;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryReport {
        private String categoryId;
        private String categoryName;
        private int totalBooks;
        private long totalSold;
        private BigDecimal totalRevenue;
        private double percentage;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InventoryReport {
        private String bookId;
        private String mainText;
        private String author;
        private String categoryName;
        private int currentStock;
        private long totalSold;
        private BigDecimal totalRevenue;
        private StockStatus stockStatus;
    }

    public enum StockStatus {
        NORMAL,
        LOW,
        OUT_OF_STOCK
    }
} 