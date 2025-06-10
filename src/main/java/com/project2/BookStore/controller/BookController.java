package com.project2.BookStore.controller;

import com.project2.BookStore.dto.*;
import com.project2.BookStore.exception.BadRequestException;
import com.project2.BookStore.model.Book;
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
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import lombok.extern.slf4j.Slf4j;
import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import com.project2.BookStore.dto.PageResponse;

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

    @GetMapping("/simple")
    public ResponseEntity<ApiResponseDTO> getAllBooks() {
        try {
            log.info("Lấy danh sách tất cả sách");
            List<BookResponseDTO> books = bookService.getAllBooks();
            return ResponseEntity.ok(new ApiResponseDTO(true, "Lấy danh sách sách thành công", books));
        } catch (BadRequestException e) {
            log.error("Lỗi khi lấy danh sách sách: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new ApiResponseDTO(false, e.getMessage()));
        } catch (Exception e) {
            log.error("Lỗi không xác định khi lấy danh sách sách: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponseDTO(false, "Lỗi server khi lấy danh sách sách"));
        }
    }

    @GetMapping("/paged")
    public ResponseEntity<ApiResponseDTO> getBooksPaged(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) int size) {
        try {
            log.info("Lấy danh sách sách phân trang - Trang: {}, Kích thước: {}", page, size);
            Pageable pageable = PageRequest.of(page, size);
            Page<BookResponseDTO> books = bookService.getBooksPaged(pageable);
            return ResponseEntity.ok(new ApiResponseDTO(true, "Lấy danh sách sách phân trang thành công", books));
        } catch (BadRequestException e) {
            log.error("Lỗi khi lấy danh sách sách phân trang: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new ApiResponseDTO(false, e.getMessage()));
        } catch (Exception e) {
            log.error("Lỗi không xác định khi lấy danh sách sách phân trang: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponseDTO(false, "Lỗi server khi lấy danh sách sách phân trang"));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDTO> getBookById(@PathVariable @NotBlank String id) {
        try {
            log.info("Lấy thông tin sách với ID: {}", id);
            BookResponseDTO book = bookService.getBookById(id);
            return ResponseEntity.ok(new ApiResponseDTO(true, "Lấy thông tin sách thành công", book));
        } catch (BadRequestException e) {
            log.error("Lỗi khi lấy thông tin sách: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new ApiResponseDTO(false, e.getMessage()));
        } catch (Exception e) {
            log.error("Lỗi không xác định khi lấy thông tin sách: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponseDTO(false, "Lỗi server khi lấy thông tin sách"));
        }
    }

    @PostMapping(value = "/add", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponseDTO> addBook(
            @RequestParam("mainText") String mainText,
            @RequestParam("author") String author,
            @RequestParam("price") Long price,
            @RequestParam("sold") Integer sold,
            @RequestParam("quantity") Integer quantity,
            @RequestParam("categoryName") String categoryName,
            @RequestPart(value = "imageFile", required = false) MultipartFile imageFile) {
        try {
            AddBookRequest request = new AddBookRequest();
            request.setMainText(mainText);
            request.setAuthor(author);
            request.setPrice(price);
            request.setSold(sold);
            request.setQuantity(quantity);
            request.setCategoryName(categoryName);
            if (imageFile != null && !imageFile.isEmpty()) {
                request.setImageFile(imageFile);
            }

            BookResponseDTO book = bookService.addBook(request);
            return ResponseEntity.ok(new ApiResponseDTO(true, "Thêm sách thành công", book));
        } catch (BadRequestException e) {
            return ResponseEntity.badRequest().body(new ApiResponseDTO(false, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponseDTO(false, "Lỗi server khi thêm sách"));
        }
    }

    @PutMapping(value = "/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponseDTO> updateBook(
            @RequestParam("id") String id,
            @RequestParam(value = "mainText", required = false) String mainText,
            @RequestParam(value = "author", required = false) String author,
            @RequestParam(value = "price", required = false) Long price,
            @RequestParam(value = "sold", required = false) Integer sold,
            @RequestParam(value = "quantity", required = false) Integer quantity,
            @RequestParam(value = "categoryName", required = false) String categoryName,
            @RequestPart(value = "imageFile", required = false) MultipartFile imageFile) {
        try {
            UpdateBookRequest request = new UpdateBookRequest();
            request.setId(id);
            request.setMainText(mainText);
            request.setAuthor(author);
            request.setPrice(price);
            request.setSold(sold);
            request.setQuantity(quantity);
            request.setCategoryName(categoryName);
            if (imageFile != null && !imageFile.isEmpty()) {
                request.setImageFile(imageFile);
            }
            BookResponseDTO book = bookService.updateBook(request);
            return ResponseEntity.ok(new ApiResponseDTO(true, "Cập nhật sách thành công", book));
        } catch (BadRequestException e) {
            return ResponseEntity.badRequest().body(new ApiResponseDTO(false, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponseDTO(false, "Lỗi server khi cập nhật sách"));
        }
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<ApiResponseDTO> deleteBook(@PathVariable @NotBlank String id) {
        try {
            log.info("Bắt đầu xóa sách. BookId: {}", id);
            bookService.deleteBook(id);
            log.info("Xóa sách thành công. BookId: {}", id);
            return ResponseEntity.ok(new ApiResponseDTO(true, "Xóa sách thành công"));
        } catch (BadRequestException e) {
            log.warn("Lỗi khi xóa sách: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new ApiResponseDTO(false, e.getMessage()));
        } catch (Exception e) {
            log.error("Lỗi không xác định khi xóa sách: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponseDTO(false, "Lỗi server khi xóa sách"));
        }
    }

    @PostMapping("/image")
    public ResponseEntity<ApiResponseDTO> uploadBookImage(
            @RequestParam("id") @NotBlank String id,
            @RequestParam("image") MultipartFile imageFile) {
        try {
            log.info("Nhận yêu cầu upload ảnh cho sách với ID: {}", id);
            if (imageFile == null || imageFile.isEmpty()) {
                throw new BadRequestException("File ảnh không được để trống");
            }
            
            BookResponseDTO book = bookService.getBookById(id);
            Book.Image image = imageProcessingService.processAndUploadBookImage(imageFile);
            log.info("Upload ảnh thành công cho sách ID: {}, URL: {}", id, image.getOriginal());
            
            UpdateBookRequest updateRequest = new UpdateBookRequest();
            updateRequest.setId(id);
            updateRequest.setImageUrl(image.getOriginal());
            BookResponseDTO updatedBook = bookService.updateBook(updateRequest);
            
            return ResponseEntity.ok(new ApiResponseDTO(true, "Upload ảnh sách thành công", updatedBook));
        } catch (BadRequestException e) {
            log.error("Lỗi khi upload ảnh sách: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new ApiResponseDTO(false, e.getMessage()));
        } catch (Exception e) {
            log.error("Lỗi không xác định khi upload ảnh sách: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponseDTO(false, "Lỗi server khi upload ảnh sách"));
        }
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<ApiResponseDTO> getBooksByCategory(
            @PathVariable @NotBlank String categoryId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) int size) {
        try {
            log.info("Lấy danh sách sách theo danh mục - CategoryId: {}, Trang: {}, Kích thước: {}", categoryId, page, size);
            Pageable pageable = PageRequest.of(page, size);
            Page<BookResponseDTO> books = bookService.getBooksByCategoryPaged(categoryId, pageable);
            return ResponseEntity.ok(new ApiResponseDTO(true, "Lấy danh sách sách theo danh mục thành công", new PageResponse<>(books)));
        } catch (BadRequestException e) {
            log.error("Lỗi khi lấy danh sách sách theo danh mục: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new ApiResponseDTO(false, e.getMessage()));
        } catch (Exception e) {
            log.error("Lỗi không xác định khi lấy danh sách sách theo danh mục: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponseDTO(false, "Lỗi server khi lấy danh sách sách theo danh mục"));
        }
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponseDTO> searchBooks(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) Long minPrice,
            @RequestParam(required = false) Long maxPrice,
            @RequestParam(required = false) Boolean inStock,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {
        try {
            log.info("Tìm kiếm sách với các tiêu chí - Keyword: {}, CategoryId: {}, MinPrice: {}, MaxPrice: {}, InStock: {}, SortBy: {}, SortDirection: {}, Page: {}, Size: {}",
                keyword, categoryId, minPrice, maxPrice, inStock, sortBy, sortDirection, page, size);

            Pageable pageable = PageRequest.of(
                page, 
                size, 
                Sort.by(Sort.Direction.fromString(sortDirection), sortBy)
            );

            Page<BookResponseDTO> result = bookService.searchBooks(
                keyword,
                categoryId,
                minPrice,
                maxPrice,
                inStock,
                pageable
            );

            return ResponseEntity.ok(new ApiResponseDTO(true, "Tìm kiếm sách thành công", new PageResponse<>(result)));
        } catch (BadRequestException e) {
            log.error("Lỗi khi tìm kiếm sách: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new ApiResponseDTO(false, e.getMessage()));
        } catch (Exception e) {
            log.error("Lỗi không xác định khi tìm kiếm sách: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponseDTO(false, "Lỗi server khi tìm kiếm sách"));
        }
    }

    @GetMapping("/top-selling")
    public ResponseEntity<ApiResponseDTO> getTopSellingBooks(
            @RequestParam(defaultValue = "10") @Min(1) int limit) {
        try {
            log.info("Lấy danh sách {} sách bán chạy nhất", limit);
            List<BookResponseDTO> books = bookService.getTopSellingBooks(limit);
            return ResponseEntity.ok(new ApiResponseDTO(true, "Lấy danh sách sách bán chạy thành công", books));
        } catch (BadRequestException e) {
            log.error("Lỗi khi lấy danh sách sách bán chạy: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new ApiResponseDTO(false, e.getMessage()));
        } catch (Exception e) {
            log.error("Lỗi không xác định khi lấy danh sách sách bán chạy: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponseDTO(false, "Lỗi server khi lấy danh sách sách bán chạy"));
        }
    }

    @GetMapping("/latest")
    public ResponseEntity<ApiResponseDTO> getLatestBooks(
            @RequestParam(defaultValue = "10") @Min(1) int limit) {
        try {
            log.info("Lấy danh sách {} sách mới nhất", limit);
            List<BookResponseDTO> books = bookService.getLatestBooks(limit);
            return ResponseEntity.ok(new ApiResponseDTO(true, "Lấy danh sách sách mới nhất thành công", books));
        } catch (BadRequestException e) {
            log.error("Lỗi khi lấy danh sách sách mới nhất: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new ApiResponseDTO(false, e.getMessage()));
        } catch (Exception e) {
            log.error("Lỗi không xác định khi lấy danh sách sách mới nhất: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponseDTO(false, "Lỗi server khi lấy danh sách sách mới nhất"));
        }
    }

    @GetMapping("/low-stock")
    public ResponseEntity<ApiResponseDTO> getLowStockBooks(
            @RequestParam(defaultValue = "5") @Min(1) int threshold) {
        try {
            log.info("Lấy danh sách sách sắp hết hàng (ngưỡng: {})", threshold);
            List<BookResponseDTO> books = bookService.getLowStockBooks(threshold);
            return ResponseEntity.ok(new ApiResponseDTO(true, "Lấy danh sách sách sắp hết hàng thành công", books));
        } catch (BadRequestException e) {
            log.error("Lỗi khi lấy danh sách sách sắp hết hàng: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new ApiResponseDTO(false, e.getMessage()));
        } catch (Exception e) {
            log.error("Lỗi không xác định khi lấy danh sách sách sắp hết hàng: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponseDTO(false, "Lỗi server khi lấy danh sách sách sắp hết hàng"));
        }
    }

    @GetMapping("/search/keyword")
    public ResponseEntity<ApiResponseDTO> searchBooksByKeyword(
            @RequestParam @NotBlank String keyword,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "asc") String direction) {
        try {
            log.info("Tìm kiếm sách theo từ khóa: {}, Trang: {}, Kích thước: {}, Sắp xếp: {}, Hướng: {}",
                keyword, page, size, sort, direction);

            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(direction), sort));
            Page<BookResponseDTO> result = bookService.searchBooksByKeyword(keyword, pageable);
            return ResponseEntity.ok(new ApiResponseDTO(true, "Tìm kiếm sách thành công", new PageResponse<>(result)));
        } catch (BadRequestException e) {
            log.error("Lỗi khi tìm kiếm sách theo từ khóa: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new ApiResponseDTO(false, e.getMessage()));
        } catch (Exception e) {
            log.error("Lỗi không xác định khi tìm kiếm sách theo từ khóa: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponseDTO(false, "Lỗi server khi tìm kiếm sách"));
        }
    }
} 