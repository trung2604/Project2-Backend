package com.project2.BookStore.service;

import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Map;

public interface BookImportService {
    /**
     * Import books from CSV file
     * @param file CSV file containing book data
     * @return Map containing:
     *         - "success": List of successfully imported books
     *         - "failed": List of failed imports with error messages
     *         - "total": Total number of records in file
     *         - "successCount": Number of successfully imported books
     *         - "failedCount": Number of failed imports
     */
    Map<String, Object> importBooksFromCSV(MultipartFile file);
} 