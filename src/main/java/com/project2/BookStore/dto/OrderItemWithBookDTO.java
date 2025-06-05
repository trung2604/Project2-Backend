package com.project2.BookStore.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemWithBookDTO {
    private String id;
    private BookDTO book;  // Có thể null nếu sách đã bị xóa
    private int quantity;
    private double price;
} 