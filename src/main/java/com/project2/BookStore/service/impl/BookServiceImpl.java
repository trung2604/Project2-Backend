package com.project2.BookStore.service.impl;

import com.project2.BookStore.model.Book;
import com.project2.BookStore.repository.BookRepository;
import com.project2.BookStore.service.BookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import com.project2.BookStore.dto.BookSimpleDTO;
import org.springframework.web.multipart.MultipartFile;
import com.project2.BookStore.service.ImageProcessingService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
public class BookServiceImpl implements BookService {
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
        return bookRepository.save(book);
    }

    @Override
    public BookSimpleDTO addBookWithImage(MultipartFile file, String mainText, String author, long price, int sold, int quantity, String category) {
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
} 