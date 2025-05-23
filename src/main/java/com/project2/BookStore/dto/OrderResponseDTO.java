package com.project2.BookStore.dto;

import com.project2.BookStore.model.Order;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponseDTO {
    private String id;
    private String userId;
    private String fullName;
    private String phone;
    private String address;
    private String email;
    private Order.OrderStatus status;
    private Double totalAmount;
    private String paymentMethod;
    private String paymentStatus;
    private List<OrderItemResponseDTO> items;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemResponseDTO {
        private String id;
        private String bookId;
        private String bookTitle;
        private String bookImage;
        private Integer quantity;
        private Double price;
        private Double subtotal;
    }
} 