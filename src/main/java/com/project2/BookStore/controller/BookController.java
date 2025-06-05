package com.project2.BookStore.controller;

import com.project2.BookStore.dto.*;
import com.project2.BookStore.exception.BadRequestException;
import com.project2.BookStore.service.BookService;
import com.project2.BookStore.service.ImageProcessingService;
import com.project2.BookStore.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import lombok.extern.slf4j.Slf4j;
import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.security.access.prepost.PreAuthorize;

@Slf4j
@RestController
@RequestMapping("/api/bookStore/book")
public class BookController {
    @Autowired
    private BookService bookService;

    @Autowired
    private ImageProcessingService imageProcessingService;

    @Autowired
    private JwtUtil jwtUtil;

    private void validateAdminToken(HttpServletRequest request) {
        log.info("Validating admin token");
        String authHeader = request.getHeader("Authorization");
        log.debug("Authorization header: {}", authHeader);
        
        if (authHeader == null) {
            log.warn("Authorization header is null");
            throw new BadRequestException("Không tìm thấy token xác thực");
        }
        
        if (!authHeader.startsWith("Bearer ")) {
            log.warn("Authorization header does not start with 'Bearer '");
            throw new BadRequestException("Token không đúng định dạng");
        }
        
        String token = authHeader.substring(7);
        log.debug("Extracted token: {}", token);
        
        try {
            String role = jwtUtil.getRoleFromToken(token);
            log.info("User role: {}", role);
            if (!"ROLE_ADMIN".equals(role)) {
                log.warn("User does not have ADMIN role");
                throw new BadRequestException("Không có quyền thực hiện thao tác này");
            }
        } catch (Exception e) {
            log.error("Error validating token: {}", e.getMessage());
            throw new BadRequestException("Token không hợp lệ: " + e.getMessage());
        }
    }

    @GetMapping("/simple")
    public ResponseEntity<ApiResponse<List<BookResponseDTO>>> getAllBooks() {
        try {
            List<BookResponseDTO> books = bookService.getAllBooks();
            return ResponseEntity.ok(ApiResponse.success(books, "Lấy danh sách sách thành công"));
        } catch (BadRequestException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.serverError("Lỗi server: " + e.getMessage()));
        }
    }

    @GetMapping("/paged")
    public ResponseEntity<ApiResponse<Page<BookResponseDTO>>> getBooksPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Page<BookResponseDTO> books = bookService.getBooksPaged(PageRequest.of(page, size));
            return ResponseEntity.ok(ApiResponse.success(books, "Lấy danh sách sách phân trang thành công"));
        } catch (BadRequestException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.serverError("Lỗi server: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BookResponseDTO>> getBookById(@PathVariable String id) {
        try {
            BookResponseDTO book = bookService.getBookById(id);
            return ResponseEntity.ok(ApiResponse.success(book, "Lấy thông tin sách thành công"));
        } catch (BadRequestException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.serverError("Lỗi server: " + e.getMessage()));
        }
    }

    @PostMapping("/add")
    public ResponseEntity<ApiResponse<BookResponseDTO>> addBook(
            @RequestPart("book") String bookJson,
            @RequestPart(value = "imageFile", required = false) MultipartFile imageFile) {
        try {
            log.info("Received request to add book with JSON: {}", bookJson);
            
            // Parse JSON string to AddBookRequest
            ObjectMapper mapper = new ObjectMapper();
            AddBookRequest request = mapper.readValue(bookJson, AddBookRequest.class);
            log.info("Parsed book request: {}", request);
            
            // Set image file if provided
            if (imageFile != null && !imageFile.isEmpty()) {
                log.info("Image file provided: {} ({} bytes)", imageFile.getOriginalFilename(), imageFile.getSize());
                request.setImageFile(imageFile);
            } else {
                log.info("No image file provided");
            }
            
            BookResponseDTO response = bookService.addBook(request);
            log.info("Book added successfully with ID: {}", response.getId());
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Thêm sách thành công"));
        } catch (JsonProcessingException e) {
            log.error("Invalid book data format: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Định dạng dữ liệu sách không hợp lệ: " + e.getMessage()));
        } catch (BadRequestException e) {
            log.error("Bad request: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error adding book: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Lỗi khi thêm sách: " + e.getMessage()));
        }
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<BookResponseDTO>> updateBook(
            @PathVariable String id,
            @RequestPart(value = "bookData", required = true) UpdateBookRequest request,
            @RequestPart(value = "imageFile", required = false) MultipartFile imageFile) {
        try {
            request.setId(id);
            request.setImageFile(imageFile);
            BookResponseDTO updatedBook = bookService.updateBook(request);
            return ResponseEntity.ok(ApiResponse.success(updatedBook, "Cập nhật sách thành công"));
        } catch (BadRequestException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.serverError("Lỗi server: " + e.getMessage()));
        }
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteBook(@PathVariable String id) {
        try {
            bookService.deleteBook(id);
            return ResponseEntity.ok(ApiResponse.success(null, "Xóa sách thành công"));
        } catch (BadRequestException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.serverError("Lỗi server: " + e.getMessage()));
        }
    }

    @PostMapping("/image")
    public ResponseEntity<ApiResponse<String>> uploadBookImage(
            @RequestParam("file") MultipartFile file) {
        try {
            String imageUrl = imageProcessingService.uploadImageToCloudinary(file);
            return ResponseEntity.ok(ApiResponse.success(imageUrl, "Tải lên ảnh sách thành công"));
        } catch (BadRequestException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.serverError("Lỗi khi tải lên ảnh sách: " + e.getMessage()));
        }
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<ApiResponse<Page<BookResponseDTO>>> getBooksByCategory(
            @PathVariable String categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Page<BookResponseDTO> books = bookService.getBooksByCategoryPaged(categoryId, PageRequest.of(page, size));
            return ResponseEntity.ok(ApiResponse.success(books, "Lấy danh sách sách theo danh mục thành công"));
        } catch (BadRequestException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.serverError("Lỗi server: " + e.getMessage()));
        }
    }
} 