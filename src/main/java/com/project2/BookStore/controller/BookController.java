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
import java.util.HashMap;
import java.util.Map;
import java.io.IOException;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/bookStore/book")
public class BookController {
    @Autowired
    private BookService bookService;

    @GetMapping("/simple")
    public ResponseEntity<ApiResponseDTO<List<BookSimpleDTO>>> getSimpleBooks() {
        List<Book> books = bookService.getAllBooks();
        List<BookSimpleDTO> result = books.stream().map(book -> new BookSimpleDTO(
            book.getId(),
            book.getImage(),
            book.getMainText(),
            book.getAuthor(),
            book.getPrice(),
            book.getSold(),
            book.getQuantity(),
            book.getCategory()
        )).collect(Collectors.toList());

        ApiResponseDTO<List<BookSimpleDTO>> response = new ApiResponseDTO<>(200, "", result);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/add")
    public ResponseEntity<ApiResponseDTO<BookSimpleDTO>> addBook(
            @RequestParam("file") MultipartFile file,
            @RequestParam("mainText") String mainText,
            @RequestParam("author") String author,
            @RequestParam("price") long price,
            @RequestParam("sold") int sold,
            @RequestParam("quantity") int quantity,
            @RequestParam("category") String category
    ) {
        BookSimpleDTO dto = bookService.addBookWithImage(file, mainText, author, price, sold, quantity, category);
        ApiResponseDTO<BookSimpleDTO> response = new ApiResponseDTO<>(201, "Thêm sách thành công!", dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/paged")
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> getBooksPaged(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int pageSize) {
        PageRequest pageRequest = PageRequest.of(Math.max(0, current - 1), pageSize);
        Page<Book> page = bookService.getBooksPaged(pageRequest);
        // Chuyển sang BookSimpleDTO
        List<BookSimpleDTO> result = page.getContent().stream().map(book -> new BookSimpleDTO(
            book.getId(),
            book.getImage(),
            book.getMainText(),
            book.getAuthor(),
            book.getPrice(),
            book.getSold(),
            book.getQuantity(),
            book.getCategory()
        )).toList();
        Map<String, Object> meta = new HashMap<>();
        meta.put("current", current);
        meta.put("pageSize", pageSize);
        meta.put("pages", page.getTotalPages());
        meta.put("total", page.getTotalElements());
        Map<String, Object> data = new HashMap<>();
        data.put("meta", meta);
        data.put("result", result);
        ApiResponseDTO<Map<String, Object>> response = new ApiResponseDTO<>(200, "", data);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/update")
    public ResponseEntity<ApiResponseDTO<Book>> updateBook(@RequestBody Book book) {
        try {
            Book updatedBook = bookService.updateBook(book);
            ApiResponseDTO<Book> response = new ApiResponseDTO<>(
                200,
                "Cập nhật sách thành công",
                updatedBook
            );
            return ResponseEntity.ok(response);
        } catch (BookException e) {
            ApiResponseDTO<Book> response;
            if (e.getMessage().contains("Không tìm thấy sách")) {
                response = new ApiResponseDTO<>(404, e.getMessage(), null);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            } else {
                response = new ApiResponseDTO<>(400, e.getMessage(), null);
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            ApiResponseDTO<Book> response = new ApiResponseDTO<>(
                500,
                "Lỗi khi cập nhật sách: " + e.getMessage(),
                null
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> deleteBook(@PathVariable String id) {
        bookService.deleteBook(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/image")
    public ResponseEntity<Book> updateBookImage(@RequestParam("id") String id, @RequestParam("image") MultipartFile image) throws IOException {
        Book updatedBook = bookService.updateBookImage(id, image);
        return ResponseEntity.ok(updatedBook);
    }
} 