package com.project2.BookStore.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class UploadAvatarRequest {
    private MultipartFile file;
    private String userId;
} 