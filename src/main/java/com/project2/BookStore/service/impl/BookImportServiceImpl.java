package com.project2.BookStore.service.impl;

import com.project2.BookStore.dto.BookDTO;
import com.project2.BookStore.dto.ImageDTO;
import com.project2.BookStore.exception.BadRequestException;
import com.project2.BookStore.model.Book;
import com.project2.BookStore.model.Category;
import com.project2.BookStore.repository.BookRepository;
import com.project2.BookStore.repository.CategoryRepository;
import com.project2.BookStore.service.BookImportService;
import com.project2.BookStore.service.ImageProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookImportServiceImpl implements BookImportService {

    private final BookRepository bookRepository;
    private final CategoryRepository categoryRepository;
    private final ImageProcessingService imageProcessingService;

    @Override
    @Transactional
    public Map<String, Object> importBooksFromCSV(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BadRequestException("File CSV không được để trống");
        }

        if (!file.getContentType().equals("text/csv")) {
            throw new BadRequestException("File phải có định dạng CSV");
        }

        List<BookDTO> successList = new ArrayList<>();
        List<Map<String, String>> failedList = new ArrayList<>();
        int totalRecords = 0;
        int successCount = 0;
        int failedCount = 0;

        try (Reader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            Iterable<CSVRecord> records = CSVFormat.DEFAULT
                    .builder()
                    .setHeader("mainText", "author", "price", "quantity", "categoryName", "image")
                    .setSkipHeaderRecord(true)
                    .setDelimiter(',')
                    .setQuote('"')
                    .setEscape('\\')
                    .setIgnoreEmptyLines(true)
                    .setTrim(true)
                    .build()
                    .parse(reader);

            for (CSVRecord record : records) {
                totalRecords++;
                try {
                    String mainText = record.get("mainText").trim();
                    String author = record.get("author").trim();
                    String priceStr = record.get("price").trim();
                    String quantityStr = record.get("quantity").trim();
                    String categoryName = record.get("categoryName").trim();
                    String imageUrl = record.get("image").trim();

                    // Validate required fields
                    if (mainText.isEmpty()) {
                        throw new BadRequestException("Tên sách không được để trống");
                    }
                    if (author.isEmpty()) {
                        throw new BadRequestException("Tác giả không được để trống");
                    }
                    if (categoryName.isEmpty()) {
                        throw new BadRequestException("Danh mục không được để trống");
                    }

                    // Process image if provided
                    Book.Image image = null;
                    if (imageUrl != null && !imageUrl.trim().isEmpty()) {
                        try {
                            // Validate URL
                            URL url = new URL(imageUrl.trim());
                            URLConnection conn = url.openConnection();
                            conn.connect();

                            // Download image
                            MultipartFile imageFile = new MultipartFile() {
                                private final byte[] content = conn.getInputStream().readAllBytes();
                                private final String originalFilename = imageUrl.substring(imageUrl.lastIndexOf('/') + 1);

                                @Override
                                public String getName() {
                                    return "image";
                                }

                                @Override
                                public String getOriginalFilename() {
                                    return originalFilename;
                                }

                                @Override
                                public String getContentType() {
                                    return conn.getContentType();
                                }

                                @Override
                                public boolean isEmpty() {
                                    return content.length == 0;
                                }

                                @Override
                                public long getSize() {
                                    return content.length;
                                }

                                @Override
                                public byte[] getBytes() {
                                    return content;
                                }

                                @Override
                                public java.io.InputStream getInputStream() {
                                    return new java.io.ByteArrayInputStream(content);
                                }

                                @Override
                                public void transferTo(java.io.File dest) throws IOException, IllegalStateException {
                                    try (java.io.FileOutputStream fos = new java.io.FileOutputStream(dest)) {
                                        fos.write(content);
                                    }
                                }
                            };

                            // Upload to Cloudinary and process
                            image = imageProcessingService.processAndUploadBookImage(imageFile);
                            log.info("Đã xử lý và upload ảnh sách thành công: {}", image.getOriginal());
                        } catch (Exception e) {
                            throw new BadRequestException("Lỗi khi xử lý ảnh sách: " + e.getMessage());
                        }
                    }

                    // Parse numeric values
                    Long price;
                    Integer quantity;
                    try {
                        // Parse price as double first, then convert to cents
                        double priceValue = Double.parseDouble(priceStr);
                        if (priceValue <= 0) {
                            throw new BadRequestException("Giá sách phải lớn hơn 0");
                        }
                        // Convert to cents (multiply by 100 and round)
                        price = Math.round(priceValue * 100);
                    } catch (NumberFormatException e) {
                        throw new BadRequestException("Giá sách không hợp lệ: " + priceStr);
                    }

                    try {
                        quantity = Integer.parseInt(quantityStr);
                        if (quantity < 0) {
                            throw new BadRequestException("Số lượng không được âm");
                        }
                    } catch (NumberFormatException e) {
                        throw new BadRequestException("Số lượng không hợp lệ: " + quantityStr);
                    }

                    // Find or create category with proper encoding
                    Category category = categoryRepository.findByName(categoryName)
                            .orElseGet(() -> {
                                Category newCategory = new Category();
                                newCategory.setName(categoryName.trim());
                                return categoryRepository.save(newCategory);
                            });

                    // Create and save book
                    Book book = new Book();
                    book.setMainText(mainText);
                    book.setAuthor(author);
                    book.setPrice(price);
                    book.setQuantity(quantity);
                    book.setCategoryId(category.getId());
                    book.setImage(image);
                    book.setSold(0);

                    Book savedBook = bookRepository.save(book);
                    successList.add(convertToDTO(savedBook));
                    successCount++;

                } catch (Exception e) {
                    log.error("Error importing book at row {}: {}", totalRecords, e.getMessage());
                    Map<String, String> error = new HashMap<>();
                    error.put("row", String.valueOf(totalRecords));
                    error.put("error", e.getMessage());
                    failedList.add(error);
                    failedCount++;
                }
            }

        } catch (IOException e) {
            throw new BadRequestException("Lỗi khi đọc file CSV: " + e.getMessage());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", successList);
        result.put("failed", failedList);
        result.put("total", totalRecords);
        result.put("successCount", successCount);
        result.put("failedCount", failedCount);

        return result;
    }

    private BookDTO convertToDTO(Book book) {
        ImageDTO imageDTO = null;
        if (book.getImage() != null) {
            imageDTO = new ImageDTO(
                book.getImage().getThumbnail(),
                book.getImage().getMedium(),
                book.getImage().getOriginal(),
                book.getImage().getFormat(),
                book.getImage().getSize()
            );
        }
        return new BookDTO(
            book.getId(),
            imageDTO,
            book.getMainText(),
            book.getAuthor(),
            book.getPrice(),
            book.getSold(),
            book.getQuantity(),
            book.getCategoryId()
        );
    }
} 