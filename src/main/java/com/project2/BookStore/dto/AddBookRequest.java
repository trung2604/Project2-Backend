package com.project2.BookStore.dto;

import com.project2.BookStore.model.Book;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddBookRequest {
    @NotBlank(message = "Tên sách không được để trống")
    @Size(min = 2, max = 200, message = "Tên sách phải từ 2 đến 200 ký tự")
    private String mainText;

    @NotBlank(message = "Tác giả không được để trống")
    @Size(min = 2, max = 100, message = "Tên tác giả phải từ 2 đến 100 ký tự")
    private String author;

    @NotNull(message = "Giá sách không được để trống")
    @Min(value = 0, message = "Giá sách phải lớn hơn hoặc bằng 0")
    private Long price;

    @NotNull(message = "Số lượng đã bán không được để trống")
    @Min(value = 0, message = "Số lượng đã bán phải lớn hơn hoặc bằng 0")
    private Integer sold;

    @NotNull(message = "Số lượng trong kho không được để trống")
    @Min(value = 0, message = "Số lượng trong kho phải lớn hơn hoặc bằng 0")
    private Integer quantity;

    @NotBlank(message = "Tên danh mục không được để trống")
    @Size(min = 2, max = 100, message = "Tên danh mục phải từ 2 đến 100 ký tự")
    private String categoryName;

    private String imageUrl;
    private MultipartFile imageFile;
    private Book.Image processedImage;
} 