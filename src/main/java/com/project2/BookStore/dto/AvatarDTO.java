package com.project2.BookStore.dto;

import lombok.Data;
import lombok.Builder;

@Data
@Builder
public class AvatarDTO {
    private String thumbnail;  // URL for small size (150x150)
    private String medium;     // URL for medium size (300x300)
    private String original;   // URL for original size
    private String format;     // Image format (webp/jpeg)
    private Long size;         // File size in bytes
} 