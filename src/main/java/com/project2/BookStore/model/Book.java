package com.project2.BookStore.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "books")
public class Book {
    @Id
    private String id;
    private Image image;
    private List<String> slider;
    private String mainText;
    private String author;
    private long price;
    private int sold;
    private int quantity;
    private String category;
    private Date createdAt;
    private Date updatedAt;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Image {
        private String thumbnail;
        private String medium;
        private String original;
        private String format;
        private Long size;
    }
} 