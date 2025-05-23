package com.project2.BookStore.service;

import com.project2.BookStore.model.Book;
import com.project2.BookStore.dto.BookSimpleDTO;
import com.project2.BookStore.exception.BookException;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Map;
import java.io.IOException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BookService {
    List<Book> getAllBooks();
    Book addBook(Book book);
    BookSimpleDTO addBookWithImage(MultipartFile file, String mainText, String author, long price, int sold, int quantity, String categoryId);
    
    // Cập nhật phương thức phân trang
    Page<BookSimpleDTO> getBooksPaged(Pageable pageable);
    Page<BookSimpleDTO> getBooksByCategoryPaged(String categoryId, Pageable pageable);
    
    Book updateBook(Book book);
    void deleteBook(String id);
    Book updateBookImage(String id, MultipartFile image) throws IOException;
} 