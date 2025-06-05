package com.project2.BookStore.repository;

import com.project2.BookStore.model.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BookRepository extends JpaRepository<Book, String> {
    boolean existsByMainText(String mainText);
    List<Book> findByCategoryId(String categoryId);
    Page<Book> findByCategoryId(String categoryId, Pageable pageable);
    long countByCategoryId(String categoryId);
    
    // Tìm kiếm sách theo tên hoặc tác giả
    Page<Book> findByMainTextContainingIgnoreCaseOrAuthorContainingIgnoreCase(String mainText, String author, Pageable pageable);
} 