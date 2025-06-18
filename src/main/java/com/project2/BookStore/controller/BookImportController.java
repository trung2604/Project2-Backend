package com.project2.BookStore.controller;

import com.project2.BookStore.dto.ApiResponseDTO;
import com.project2.BookStore.service.BookImportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api/bookStore/books")
@RequiredArgsConstructor
public class BookImportController {

    private final BookImportService bookImportService;

    @PostMapping("/import")
    public ResponseEntity<ApiResponseDTO> importBooks(@RequestParam("file") MultipartFile file) {
        log.info("Importing books from CSV file: {}", file.getOriginalFilename());
        try {
            var result = bookImportService.importBooksFromCSV(file);
            String message = String.format(
                "Import sách thành công: %d/%d bản ghi. %d bản ghi thất bại.",
                result.get("successCount"),
                result.get("total"),
                result.get("failedCount")
            );
            return ResponseEntity.ok(new ApiResponseDTO(true, message, result));
        } catch (Exception e) {
            log.error("Error importing books: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(new ApiResponseDTO(false, "Lỗi khi import sách: " + e.getMessage(), null));
        }
    }
} 