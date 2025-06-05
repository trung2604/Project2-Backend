package com.project2.BookStore.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryCountDTO {
    private String categoryId;
    private String categoryName;
    private long bookCount;
} 