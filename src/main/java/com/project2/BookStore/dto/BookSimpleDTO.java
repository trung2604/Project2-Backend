package com.project2.BookStore.dto;

import com.project2.BookStore.model.Book.Image;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BookSimpleDTO {
    private String id;
    private Image image;
    private String mainText;
    private String author;
    private long price;
    private int sold;
    private int quantity;
    private String categoryId;
} 