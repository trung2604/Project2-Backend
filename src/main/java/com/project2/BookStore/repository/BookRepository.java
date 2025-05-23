package com.project2.BookStore.repository;

import com.project2.BookStore.model.Book;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BookRepository extends MongoRepository<Book, String> {
    boolean existsByMainText(String mainText);
    List<Book> findByCategoryId(String categoryId);
    Page<Book> findByCategoryId(String categoryId, Pageable pageable);
} 