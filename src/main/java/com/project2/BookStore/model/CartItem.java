package com.project2.BookStore.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "cart_items")
public class CartItem {
    @Id
    private String id;
    private String bookId;
    private String bookTitle;
    private String bookImage;
    private long price;
    private int quantity;
    private String userId;
    private long totalPrice; // price * quantity
} 