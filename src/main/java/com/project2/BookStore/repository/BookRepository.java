package com.project2.BookStore.repository;

import com.project2.BookStore.model.Book;
import org.springframework.data.mongodb.repository.MongoRepository;
 
public interface BookRepository extends MongoRepository<Book, String> {
    boolean existsByMainText(String mainText);
} 