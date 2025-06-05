package com.project2.BookStore.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookDTO {
    private String id;
    private ImageDTO image;
    private String mainText;
    private String author;
    private double price;
    private int sold;
    private int quantity;
    private String categoryId;
} 