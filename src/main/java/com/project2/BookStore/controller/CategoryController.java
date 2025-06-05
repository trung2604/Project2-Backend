package com.project2.BookStore.controller;

import com.project2.BookStore.dto.*;
import com.project2.BookStore.exception.BadRequestException;
import com.project2.BookStore.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import java.util.Map;
import java.util.HashMap;

@Slf4j
@RestController
@RequestMapping("/api/bookStore/category")
public class CategoryController {
    @Autowired
    private CategoryService categoryService;

    @GetMapping("/category-count")
    public ResponseEntity<ApiResponse<List<CategoryCountDTO>>> getBookCountByCategory() {
        try {
            List<CategoryCountDTO> result = categoryService.countCategories();
            return ResponseEntity.ok(ApiResponse.success(result, "Lấy thống kê số lượng sách theo danh mục thành công"));
        } catch (BadRequestException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.serverError("Lỗi server: " + e.getMessage()));
        }
    }

    @GetMapping()
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAllCategories(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<CategoryResponseDTO> categories = categoryService.getAllCategories(pageable);
            
            Map<String, Object> meta = new HashMap<>();
            meta.put("current", page);
            meta.put("pageSize", size);
            meta.put("pages", categories.getTotalPages());
            meta.put("total", categories.getTotalElements());

            Map<String, Object> data = new HashMap<>();
            data.put("meta", meta);
            data.put("result", categories.getContent());

            return ResponseEntity.ok(ApiResponse.success(data, "Lấy danh sách danh mục thành công"));
        } catch (BadRequestException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.serverError("Lỗi server: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponseDTO>> getCategoryById(@PathVariable String id) {
        try {
            CategoryResponseDTO category = categoryService.getCategoryById(id);
            return ResponseEntity.ok(ApiResponse.success(category, "Lấy thông tin danh mục thành công"));
        } catch (BadRequestException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.serverError("Lỗi server: " + e.getMessage()));
        }
    }

    @PostMapping("/add")
    public ResponseEntity<ApiResponse<CategoryResponseDTO>> addCategory(
            @Valid @RequestBody AddCategoryRequest addCategoryRequest) {
        try {
            CategoryResponseDTO category = categoryService.addCategory(addCategoryRequest);
            return ResponseEntity.ok(ApiResponse.success(category, "Thêm danh mục thành công"));
        } catch (BadRequestException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.serverError("Lỗi server: " + e.getMessage()));
        }
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<ApiResponse<CategoryResponseDTO>> updateCategory(
            @PathVariable String id,
            @Valid @RequestBody UpdateCategoryRequest updateCategoryRequest) {
        log.info("Bắt đầu cập nhật danh mục. CategoryId: {}", id);
        try {
            updateCategoryRequest.setId(id);  // Set ID from path variable
            CategoryResponseDTO category = categoryService.updateCategory(updateCategoryRequest);
            log.info("Cập nhật danh mục thành công. CategoryId: {}", id);
            return ResponseEntity.ok(ApiResponse.success(category, "Cập nhật danh mục thành công"));
        } catch (BadRequestException e) {
            log.warn("Lỗi khi cập nhật danh mục: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Lỗi không mong muốn khi cập nhật danh mục: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.serverError("Lỗi server: " + e.getMessage()));
        }
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable String id) {
        try {
            categoryService.deleteCategory(id);
            return ResponseEntity.ok(ApiResponse.success(null, "Xóa danh mục thành công"));
        } catch (BadRequestException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.serverError("Lỗi server: " + e.getMessage()));
        }
    }
} 