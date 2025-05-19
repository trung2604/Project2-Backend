package com.project2.BookStore.dto;

import com.project2.BookStore.model.Book;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BookSimpleDTO {
    private String id;
    private Book.Image image;
    private String mainText;
    private String author;
    private long price;
    private int sold;
    private int quantity;
    private String category;
} 