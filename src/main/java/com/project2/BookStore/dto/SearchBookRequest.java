package com.project2.BookStore.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class SearchBookRequest {
    private String keyword; // Từ khóa tìm kiếm (tên sách, tác giả)
    private String categoryId; // ID danh mục
    private BigDecimal minPrice; // Giá tối thiểu
    private BigDecimal maxPrice; // Giá tối đa
    private Boolean inStock; // Còn hàng hay không
    private String sortBy; // Sắp xếp theo trường nào (price, sold, createdAt)
    private String sortDirection; // Hướng sắp xếp (asc, desc)
    private Integer page = 0; // Trang hiện tại
    private Integer size = 10; // Số lượng item trên mỗi trang
} 