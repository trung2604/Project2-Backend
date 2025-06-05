package com.project2.BookStore.dto;

import com.project2.BookStore.model.Book;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CartItemResponseDTO {
    private String bookId;
    private String bookTitle;
    private Book.Image bookImage;
    private long price;
    private int quantity;
    private long totalPrice;
} 