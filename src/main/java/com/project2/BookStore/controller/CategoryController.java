package com.project2.BookStore.controller;

import com.project2.BookStore.dto.CategoryDTO;
import com.project2.BookStore.dto.ApiResponseDTO;
import com.project2.BookStore.service.CategoryService;
import com.project2.BookStore.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {
    @Autowired
    private CategoryService categoryService;

    @PostMapping
    public ResponseEntity<ApiResponseDTO<CategoryDTO>> createCategory(@Valid @RequestBody CategoryDTO categoryDTO) {
        try {
            CategoryDTO createdCategory = categoryService.createCategory(categoryDTO);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponseDTO<>(201, "Tạo danh mục thành công", createdCategory));
        } catch (BadRequestException e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponseDTO<>(400, e.getMessage(), null));
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> getCategories(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageRequest pageRequest = PageRequest.of(page, size);
        Page<CategoryDTO> categoriesPage = categoryService.getCategoriesPaged(pageRequest);

        Map<String, Object> response = new HashMap<>();
        response.put("result", categoriesPage.getContent());
        response.put("totalElements", categoriesPage.getTotalElements());
        response.put("totalPages", categoriesPage.getTotalPages());
        response.put("currentPage", page);
        response.put("pageSize", size);

        return ResponseEntity.ok(new ApiResponseDTO<>(200, "Lấy danh sách danh mục thành công", response));
    }

    @PutMapping("/{categoryId}")
    public ResponseEntity<ApiResponseDTO<CategoryDTO>> updateCategory(
            @PathVariable String categoryId,
            @Valid @RequestBody CategoryDTO categoryDTO) {
        try {
            CategoryDTO updatedCategory = categoryService.updateCategory(categoryId, categoryDTO);
            return ResponseEntity.ok(new ApiResponseDTO<>(200, "Cập nhật danh mục thành công", updatedCategory));
        } catch (BadRequestException e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponseDTO<>(400, e.getMessage(), null));
        }
    }

    @DeleteMapping("/{categoryId}")
    public ResponseEntity<ApiResponseDTO<Void>> deleteCategory(@PathVariable String categoryId) {
        try {
            categoryService.deleteCategory(categoryId);
            return ResponseEntity.ok(new ApiResponseDTO<>(200, "Xóa danh mục thành công", null));
        } catch (BadRequestException e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponseDTO<>(400, e.getMessage(), null));
        }
    }

    @GetMapping("/{categoryId}")
    public ResponseEntity<ApiResponseDTO<CategoryDTO>> getCategoryById(@PathVariable String categoryId) {
        try {
            CategoryDTO category = categoryService.getCategoryById(categoryId);
            return ResponseEntity.ok(new ApiResponseDTO<>(200, "Lấy thông tin danh mục thành công", category));
        } catch (BadRequestException e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponseDTO<>(400, e.getMessage(), null));
        }
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponseDTO<List<CategoryDTO>>> searchCategories(@RequestParam String keyword) {
        List<CategoryDTO> categories = categoryService.searchCategories(keyword);
        return ResponseEntity.ok(new ApiResponseDTO<>(200, "Tìm kiếm danh mục thành công", categories));
    }
} 