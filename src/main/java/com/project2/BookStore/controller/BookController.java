package com.project2.BookStore.controller;

import com.project2.BookStore.dto.BookSimpleDTO;
import com.project2.BookStore.dto.ApiResponseDTO;
import com.project2.BookStore.model.Book;
import com.project2.BookStore.service.BookService;
import com.project2.BookStore.exception.BookException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.io.IOException;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/bookStore/book")
public class BookController {
    @Autowired
    private BookService bookService;

    @GetMapping("/simple")
    public ResponseEntity<ApiResponseDTO<List<BookSimpleDTO>>> getSimpleBooks() {
        List<Book> books = bookService.getAllBooks();
        List<BookSimpleDTO> result = books.stream()
            .map(book -> new BookSimpleDTO(
                book.getId(),
                book.getImage(),
                book.getMainText(),
                book.getAuthor(),
                book.getPrice(),
                book.getSold(),
                book.getQuantity(),
                book.getCategoryId()
            ))
            .collect(Collectors.toList());

        return ResponseEntity.ok(new ApiResponseDTO<>(200, "Lấy danh sách sách thành công", result));
    }

    @PostMapping("/add")
    public ResponseEntity<ApiResponseDTO<BookSimpleDTO>> addBook(
            @RequestParam("file") MultipartFile file,
            @RequestParam("mainText") String mainText,
            @RequestParam("author") String author,
            @RequestParam("price") long price,
            @RequestParam("sold") int sold,
            @RequestParam("quantity") int quantity,
            @RequestParam("categoryId") String categoryId
    ) {
        try {
            BookSimpleDTO dto = bookService.addBookWithImage(file, mainText, author, price, sold, quantity, categoryId);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponseDTO<>(201, "Thêm sách thành công!", dto));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponseDTO<>(400, e.getMessage(), null));
        }
    }

    @GetMapping("/paged")
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> getBooksPaged(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int pageSize) {
        try {
            PageRequest pageRequest = PageRequest.of(Math.max(0, current - 1), pageSize);
            Page<BookSimpleDTO> page = bookService.getBooksPaged(pageRequest);

            Map<String, Object> meta = new HashMap<>();
            meta.put("current", current);
            meta.put("pageSize", pageSize);
            meta.put("pages", page.getTotalPages());
            meta.put("total", page.getTotalElements());

            Map<String, Object> data = new HashMap<>();
            data.put("meta", meta);
            data.put("result", page.getContent());

            return ResponseEntity.ok(new ApiResponseDTO<>(200, "Lấy danh sách sách thành công", data));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponseDTO<>(500, "Lỗi khi lấy danh sách sách: " + e.getMessage(), null));
        }
    }

    @PutMapping("/update")
    public ResponseEntity<ApiResponseDTO<Book>> updateBook(@Valid @RequestBody Book book) {
        try {
            Book updatedBook = bookService.updateBook(book);
            return ResponseEntity.ok(new ApiResponseDTO<>(200, "Cập nhật sách thành công", updatedBook));
        } catch (BookException e) {
            if (e.getMessage().contains("Không tìm thấy sách")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponseDTO<>(404, e.getMessage(), null));
            }
            return ResponseEntity.badRequest()
                .body(new ApiResponseDTO<>(400, e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponseDTO<>(500, "Lỗi khi cập nhật sách: " + e.getMessage(), null));
        }
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<ApiResponseDTO<Void>> deleteBook(@PathVariable String id) {
        try {
            bookService.deleteBook(id);
            return ResponseEntity.ok(new ApiResponseDTO<>(200, "Xóa sách thành công", null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponseDTO<>(500, "Lỗi khi xóa sách: " + e.getMessage(), null));
        }
    }

    @PutMapping("/image")
    public ResponseEntity<ApiResponseDTO<Book>> updateBookImage(
            @RequestParam("id") String id,
            @RequestParam("image") MultipartFile image) {
        try {
            Book updatedBook = bookService.updateBookImage(id, image);
            return ResponseEntity.ok(new ApiResponseDTO<>(200, "Cập nhật ảnh sách thành công", updatedBook));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponseDTO<>(500, "Lỗi khi xử lý ảnh: " + e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponseDTO<>(400, e.getMessage(), null));
        }
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> getBooksByCategory(
            @PathVariable String categoryId,
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int pageSize) {
        try {
            PageRequest pageRequest = PageRequest.of(Math.max(0, current - 1), pageSize);
            Page<BookSimpleDTO> page = bookService.getBooksByCategoryPaged(categoryId, pageRequest);

            Map<String, Object> meta = new HashMap<>();
            meta.put("current", current);
            meta.put("pageSize", pageSize);
            meta.put("pages", page.getTotalPages());
            meta.put("total", page.getTotalElements());

            Map<String, Object> data = new HashMap<>();
            data.put("meta", meta);
            data.put("result", page.getContent());

            return ResponseEntity.ok(new ApiResponseDTO<>(200, "Lấy danh sách sách theo danh mục thành công", data));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponseDTO<>(500, "Lỗi khi lấy danh sách sách: " + e.getMessage(), null));
        }
    }
} 