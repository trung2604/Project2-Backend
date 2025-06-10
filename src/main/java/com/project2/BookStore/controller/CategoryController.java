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
    public ResponseEntity<ApiResponseDTO> getBookCountByCategory() {
        try {
            List<CategoryCountDTO> result = categoryService.countCategories();
            return ResponseEntity.ok(new ApiResponseDTO(true, "Lấy thống kê số lượng sách theo danh mục thành công", result));
        } catch (BadRequestException e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponseDTO(false, e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponseDTO(false, "Lỗi server: " + e.getMessage(), null));
        }
    }

    @GetMapping()
    public ResponseEntity<ApiResponseDTO> getAllCategories(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<CategoryDTO> categories = categoryService.getAllCategories(pageable);
            
            Map<String, Object> meta = new HashMap<>();
            meta.put("current", page);
            meta.put("pageSize", size);
            meta.put("pages", categories.getTotalPages());
            meta.put("total", categories.getTotalElements());

            Map<String, Object> data = new HashMap<>();
            data.put("meta", meta);
            data.put("result", categories.getContent());

            return ResponseEntity.ok(new ApiResponseDTO(true, "Lấy danh sách danh mục thành công", data));
        } catch (BadRequestException e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponseDTO(false, e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponseDTO(false, "Lỗi server: " + e.getMessage(), null));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDTO> getCategoryById(@PathVariable String id) {
        try {
            CategoryDTO category = categoryService.getCategoryById(id);
            return ResponseEntity.ok(new ApiResponseDTO(true, "Lấy thông tin danh mục thành công", category));
        } catch (BadRequestException e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponseDTO(false, e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponseDTO(false, "Lỗi server: " + e.getMessage(), null));
        }
    }

    @PostMapping("/add")
    public ResponseEntity<ApiResponseDTO> addCategory(
            @Valid @RequestBody AddCategoryRequest addCategoryRequest) {
        try {
            CategoryDTO category = categoryService.addCategory(addCategoryRequest);
            return ResponseEntity.ok(new ApiResponseDTO(true, "Thêm danh mục thành công", category));
        } catch (BadRequestException e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponseDTO(false, e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponseDTO(false, "Lỗi server: " + e.getMessage(), null));
        }
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<ApiResponseDTO> updateCategory(
            @PathVariable String id,
            @Valid @RequestBody UpdateCategoryRequest updateCategoryRequest) {
        log.info("Bắt đầu cập nhật danh mục. CategoryId: {}", id);
        try {
            updateCategoryRequest.setId(id);  // Set ID from path variable
            CategoryDTO category = categoryService.updateCategory(updateCategoryRequest);
            log.info("Cập nhật danh mục thành công. CategoryId: {}", id);
            return ResponseEntity.ok(new ApiResponseDTO(true, "Cập nhật danh mục thành công", category));
        } catch (BadRequestException e) {
            log.warn("Lỗi khi cập nhật danh mục: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(new ApiResponseDTO(false, e.getMessage(), null));
        } catch (Exception e) {
            log.error("Lỗi không mong muốn khi cập nhật danh mục: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponseDTO(false, "Lỗi server: " + e.getMessage(), null));
        }
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<ApiResponseDTO> deleteCategory(@PathVariable String id) {
        log.info("Bắt đầu xóa danh mục. CategoryId: {}", id);
        try {
            categoryService.deleteCategory(id);
            log.info("Xóa danh mục thành công. CategoryId: {}", id);
            return ResponseEntity.ok(new ApiResponseDTO(true, "Xóa danh mục thành công", null));
        } catch (BadRequestException e) {
            log.warn("Lỗi khi xóa danh mục: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(new ApiResponseDTO(false, e.getMessage(), null));
        } catch (Exception e) {
            log.error("Lỗi không mong muốn khi xóa danh mục: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponseDTO(false, "Lỗi server: " + e.getMessage(), null));
        }
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponseDTO> searchCategories(@RequestParam String keyword) {
        log.info("Bắt đầu tìm kiếm danh mục với từ khóa: {}", keyword);
        try {
            List<CategoryDTO> categories = categoryService.searchCategories(keyword);
            return ResponseEntity.ok(new ApiResponseDTO(true, "Tìm kiếm danh mục thành công", categories));
        } catch (BadRequestException e) {
            log.warn("Lỗi khi tìm kiếm danh mục: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(new ApiResponseDTO(false, e.getMessage(), null));
        } catch (Exception e) {
            log.error("Lỗi không mong muốn khi tìm kiếm danh mục: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponseDTO(false, "Lỗi server: " + e.getMessage(), null));
        }
    }
} 