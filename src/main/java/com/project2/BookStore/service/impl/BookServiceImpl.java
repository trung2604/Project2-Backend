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
import java.io.IOException;
import java.util.Date;

@Service
public class BookServiceImpl implements BookService {
    private static final Logger logger = LoggerFactory.getLogger(BookServiceImpl.class);

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private ImageProcessingService imageProcessingService;

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
    public BookSimpleDTO addBookWithImage(MultipartFile file, String mainText, String author, long price, int sold, int quantity, String category) {
        // Validate các trường không được null hoặc rỗng
        if (mainText == null || mainText.trim().isEmpty()) {
            throw new RuntimeException("Trường mainText không được để trống.");
        }
        if (author == null || author.trim().isEmpty()) {
            throw new RuntimeException("Trường author không được để trống.");
        }
        if (category == null || category.trim().isEmpty()) {
            throw new RuntimeException("Trường category không được để trống.");
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
        book.setCategory(category);
        book.setCreatedAt(new java.util.Date());
        book.setUpdatedAt(new java.util.Date());
        Book saved = bookRepository.save(book);
        return new BookSimpleDTO(
            saved.getId(),
            saved.getImage(),
            saved.getMainText(),
            saved.getAuthor(),
            saved.getPrice(),
            saved.getSold(),
            saved.getQuantity(),
            saved.getCategory()
        );
    }

    @Override
    public Page<Book> getBooksPaged(Pageable pageable) {
        return bookRepository.findAll(pageable);
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
        if (book.getCategory() == null || book.getCategory().trim().isEmpty()) {
            throw new BookException("Trường category không được để trống.");
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
        existingBook.setCategory(newBook.getCategory());
        existingBook.setUpdatedAt(new Date());

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
        target.setCategory(source.getCategory());
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
} 