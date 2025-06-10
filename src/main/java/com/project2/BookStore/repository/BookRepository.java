package com.project2.BookStore.repository;

import com.project2.BookStore.model.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.math.BigDecimal;

@Repository
public interface BookRepository extends JpaRepository<Book, String>, JpaSpecificationExecutor<Book> {
    boolean existsByMainText(String mainText);
    List<Book> findByCategoryId(String categoryId);
    Page<Book> findByCategoryId(String categoryId, Pageable pageable);
    long countByCategoryId(String categoryId);
    
    // Tìm kiếm sách theo tên hoặc tác giả
    Page<Book> findByMainTextContainingIgnoreCaseOrAuthorContainingIgnoreCase(String mainText, String author, Pageable pageable);

    // Lấy sách bán chạy nhất
    List<Book> findAllByOrderBySoldDesc(Pageable pageable);

    // Lấy sách mới nhất
    List<Book> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // Lấy sách sắp hết hàng
    List<Book> findByQuantityLessThanEqualOrderByQuantityAsc(int threshold, Pageable pageable);
} 