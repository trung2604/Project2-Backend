package com.project2.BookStore.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DBRef;

@Data
@Document(collection = "order_items")
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {
    @Id
    private String id;

    @DBRef
    private Book book;

    private int quantity;

    private double price;

    private double subtotal;
} 