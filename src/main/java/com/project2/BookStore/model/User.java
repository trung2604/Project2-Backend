package com.project2.BookStore.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "users")
public class User {
    @Id
    private String id;
    private String fullName;
    private String email;
    private String password;
    private String phone;
    private String role;
    private Avatar avatar;
    private boolean isActive;
    private Date createdAt;
    private Date updatedAt;

    @Data
    public static class Avatar {
        private String thumbnail;  // URL for small size
        private String medium;     // URL for medium size
        private String original;   // URL for original size
        private String format;     // Image format
        private Long size;         // File size in bytes
    }
} 