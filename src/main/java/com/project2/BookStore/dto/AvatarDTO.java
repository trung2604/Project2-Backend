package com.project2.BookStore.dto;

import lombok.Data;
import lombok.Builder;
import java.time.LocalDateTime;

@Data
@Builder
public class AvatarDTO {
    private String thumbnail;  // URL for small size (150x150)
    private String medium;     // URL for medium size (300x300)
    private String original;   // URL for original size
    private String publicId;   // Cloudinary public ID
    private String format;     // Image format (webp/jpeg/png)
    private LocalDateTime createdAt; // When the avatar was uploaded
} 