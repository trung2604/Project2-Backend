package com.project2.BookStore.service.impl;

import com.project2.BookStore.model.Book;
import com.project2.BookStore.repository.BookRepository;
import com.project2.BookStore.service.BookService;
import com.project2.BookStore.exception.BookException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import com.project2.BookStore.dto.BookSimpleDTO;
import org.springframework.web.multipart.MultipartFile;
import com.project2.BookStore.service.ImageProcessingService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;
import java.io.IOException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Service
public class BookServiceImpl implements BookService {
    private static final Logger logger = LoggerFactory.getLogger(BookServiceImpl.class);

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private ImageProcessingService imageProcessingService;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public List<Book> getAllBooks() {
        return bookRepository.findAll();
    }

    @Override
    public Book addBook(Book book) {
        // Kiểm tra trùng tên sách
        if (bookRepository.existsByMainText(book.getMainText())) {
            throw new RuntimeException("Sách với tên '" + book.getMainText() + "' đã tồn tại trong hệ thống.");
        }
        return bookRepository.save(book);
    }

    @Override
    public BookSimpleDTO addBookWithImage(MultipartFile file, String mainText, String author, long price, int sold, int quantity, String categoryId) {
        // Validate các trường không được null hoặc rỗng
        if (mainText == null || mainText.trim().isEmpty()) {
            throw new RuntimeException("Trường mainText không được để trống.");
        }
        if (author == null || author.trim().isEmpty()) {
            throw new RuntimeException("Trường author không được để trống.");
        }
        if (categoryId == null || categoryId.trim().isEmpty()) {
            throw new RuntimeException("Trường categoryId không được để trống.");
        }
        // Kiểm tra trùng tên sách
        if (bookRepository.existsByMainText(mainText)) {
            throw new RuntimeException("Sách với tên '" + mainText + "' đã tồn tại trong hệ thống.");
        }
        // Validate giá (price) phải > 0, sold và quantity phải >= 0
        if (price <= 0) {
            throw new RuntimeException("Giá (price) phải lớn hơn 0.");
        }
        if (sold < 0) {
            throw new RuntimeException("Trường sold phải >= 0.");
        }
        if (quantity < 0) {
            throw new RuntimeException("Trường quantity phải >= 0.");
        }
        Book.Image image = imageProcessingService.processAndUploadBookImage(file);
        Book book = new Book();
        book.setImage(image);
        book.setMainText(mainText);
        book.setAuthor(author);
        book.setPrice(price);
        book.setSold(sold);
        book.setQuantity(quantity);
        book.setCategoryId(categoryId);
        book.setCreatedAt(LocalDateTime.now());
        book.setUpdatedAt(LocalDateTime.now());
        Book saved = bookRepository.save(book);
        return new BookSimpleDTO(
            saved.getId(),
            saved.getImage(),
            saved.getMainText(),
            saved.getAuthor(),
            saved.getPrice(),
            saved.getSold(),
            saved.getQuantity(),
            saved.getCategoryId()
        );
    }

    @Override
    @Transactional
    public Book updateBook(Book book) {
        logger.info("Bắt đầu cập nhật sách với ID: {}", book.getId());
        Book existingBook = null;
        try {
            // Kiểm tra sách tồn tại
            existingBook = bookRepository.findById(book.getId())
                .orElseThrow(() -> new BookException("Không tìm thấy sách với ID: " + book.getId()));

            // Validate các trường không được null hoặc rỗng
            validateBookFields(book);

            // Kiểm tra trùng tên sách (chỉ kiểm tra nếu tên sách thay đổi)
            if (!existingBook.getMainText().equals(book.getMainText()) && 
                bookRepository.existsByMainText(book.getMainText())) {
                throw new BookException("Sách với tên '" + book.getMainText() + "' đã tồn tại trong hệ thống.");
            }

            // Lưu trạng thái cũ để rollback nếu cần
            Book oldState = new Book();
            copyBookState(existingBook, oldState);

            // Cập nhật các trường từ book vào existingBook
            updateBookFields(existingBook, book);
            
            // Lưu thay đổi
            Book updatedBook = bookRepository.save(existingBook);
            logger.info("Cập nhật sách thành công. ID: {}, Tên sách: {}", updatedBook.getId(), updatedBook.getMainText());
            return updatedBook;

        } catch (BookException e) {
            logger.error("Lỗi khi cập nhật sách: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Lỗi không xác định khi cập nhật sách: {}", e.getMessage());
            // Rollback nếu có lỗi
            if (existingBook != null) {
                try {
                    bookRepository.save(existingBook);
                    logger.info("Đã rollback trạng thái sách về trước khi cập nhật");
                } catch (Exception rollbackEx) {
                    logger.error("Lỗi khi rollback: {}", rollbackEx.getMessage());
                }
            }
            throw new BookException("Lỗi khi cập nhật sách: " + e.getMessage(), e);
        }
    }

    private void validateBookFields(Book book) {
        if (book.getMainText() == null || book.getMainText().trim().isEmpty()) {
            throw new BookException("Trường mainText không được để trống.");
        }
        if (book.getAuthor() == null || book.getAuthor().trim().isEmpty()) {
            throw new BookException("Trường author không được để trống.");
        }
        if (book.getCategoryId() == null || book.getCategoryId().trim().isEmpty()) {
            throw new BookException("Trường categoryId không được để trống.");
        }
        if (book.getPrice() <= 0) {
            throw new BookException("Giá (price) phải lớn hơn 0.");
        }
        if (book.getSold() < 0) {
            throw new BookException("Trường sold phải >= 0.");
        }
        if (book.getQuantity() < 0) {
            throw new BookException("Trường quantity phải >= 0.");
        }
    }

    private void updateBookFields(Book existingBook, Book newBook) {
        existingBook.setMainText(newBook.getMainText());
        existingBook.setAuthor(newBook.getAuthor());
        existingBook.setPrice(newBook.getPrice());
        existingBook.setSold(newBook.getSold());
        existingBook.setQuantity(newBook.getQuantity());
        existingBook.setCategoryId(newBook.getCategoryId());
        existingBook.setUpdatedAt(LocalDateTime.now());

        // Chỉ cập nhật ảnh nếu có ảnh mới
        if (newBook.getImage() != null) {
            existingBook.setImage(newBook.getImage());
        }
    }

    private void copyBookState(Book source, Book target) {
        target.setId(source.getId());
        target.setMainText(source.getMainText());
        target.setAuthor(source.getAuthor());
        target.setPrice(source.getPrice());
        target.setSold(source.getSold());
        target.setQuantity(source.getQuantity());
        target.setCategoryId(source.getCategoryId());
        target.setImage(source.getImage());
        target.setCreatedAt(source.getCreatedAt());
        target.setUpdatedAt(source.getUpdatedAt());
    }

    @Override
    public void deleteBook(String id) {
        Book book = bookRepository.findById(id).orElseThrow(() -> new RuntimeException("Book not found"));
        bookRepository.delete(book);
    }

    @Override
    public Book updateBookImage(String id, MultipartFile image) throws IOException {
        Book book = bookRepository.findById(id).orElseThrow(() -> new RuntimeException("Book not found"));
        Book.Image newImage = imageProcessingService.processAndUploadBookImage(image);
        book.setImage(newImage);
        return bookRepository.save(book);
    }

    @Override
    public Page<BookSimpleDTO> getBooksPaged(Pageable pageable) {
        return bookRepository.findAll(pageable)
            .map(book -> new BookSimpleDTO(
                book.getId(),
                book.getImage(),
                book.getMainText(),
                book.getAuthor(),
                book.getPrice(),
                book.getSold(),
                book.getQuantity(),
                book.getCategoryId()
            ));
    }

    @Override
    public Page<BookSimpleDTO> getBooksByCategoryPaged(String categoryId, Pageable pageable) {
        try {
            // Tạo query để tìm kiếm theo categoryId
            Query query = new Query();
            query.addCriteria(Criteria.where("categoryId").is(categoryId));
            query.with(pageable);
            
            // Thực hiện tìm kiếm
            List<Book> books = mongoTemplate.find(query, Book.class);
            long total = mongoTemplate.count(Query.of(query).limit(-1).skip(-1), Book.class);
            
            // Chuyển đổi sang DTO
            List<BookSimpleDTO> bookDTOs = books.stream()
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
                
            return new PageImpl<>(bookDTOs, pageable, total);
        } catch (Exception e) {
            logger.error("Lỗi khi lấy danh sách sách theo danh mục: {}", e.getMessage());
            throw new RuntimeException("Lỗi khi lấy danh sách sách theo danh mục: " + e.getMessage());
        }
    }
} 