package com.project2.BookStore.service;

import com.project2.BookStore.model.Book;
import com.project2.BookStore.dto.BookSimpleDTO;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.io.IOException;

public interface BookService {
    List<Book> getAllBooks();
    Book addBook(Book book);
    BookSimpleDTO addBookWithImage(MultipartFile file, String mainText, String author, long price, int sold, int quantity, String category);
    Page<Book> getBooksPaged(Pageable pageable);
    Book updateBook(Book book);
    void deleteBook(String id);
    Book updateBookImage(String id, MultipartFile image) throws IOException;
} 