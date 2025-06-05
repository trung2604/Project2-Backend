package com.project2.BookStore.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateBookRequest {
    @NotBlank(message = "ID sách không được để trống")
    private String id;

    private String mainText;
    private String author;
    
    @Min(value = 0, message = "Giá sách phải lớn hơn hoặc bằng 0")
    private Long price;
    
    @Min(value = 0, message = "Số lượng đã bán phải lớn hơn hoặc bằng 0")
    private Integer sold;
    
    @Min(value = 0, message = "Số lượng trong kho phải lớn hơn hoặc bằng 0")
    private Integer quantity;
    
    private String categoryId;
    private String imageUrl;
    private MultipartFile imageFile;
} 