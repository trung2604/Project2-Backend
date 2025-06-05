package com.project2.BookStore.service;

import com.project2.BookStore.dto.*;
import com.project2.BookStore.exception.BadRequestException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface BookService {
    List<BookResponseDTO> getAllBooks() throws BadRequestException;
    
    Page<BookResponseDTO> getBooksPaged(Pageable pageable) throws BadRequestException;
    
    BookResponseDTO getBookById(String id) throws BadRequestException;
    
    /**
     * Thêm sách mới
     * @param request Thông tin sách cần thêm
     * @return Thông tin sách đã thêm
     */
    BookResponseDTO addBook(AddBookRequest request);
    
    BookResponseDTO updateBook(UpdateBookRequest request) throws BadRequestException;
    
    void deleteBook(String id) throws BadRequestException;
    
    Page<BookResponseDTO> getBooksByCategoryPaged(String categoryId, Pageable pageable) throws BadRequestException;
} 