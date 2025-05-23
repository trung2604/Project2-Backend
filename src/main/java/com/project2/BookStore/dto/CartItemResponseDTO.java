package com.project2.BookStore.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CartItemResponseDTO {
    private String bookId;
    private String bookTitle;
    private String bookImage;
    private long price;
    private int quantity;
    private long totalPrice;
} 