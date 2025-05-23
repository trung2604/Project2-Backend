package com.project2.BookStore.repository;

import com.project2.BookStore.model.Category;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends MongoRepository<Category, String> {
    Optional<Category> findByName(String name);
    boolean existsByName(String name);
    
    @Query(value = "{'name': {$regex: ?0, $options: 'i'}}")
    List<Category> findByNameContainingIgnoreCase(String name);
    
    @Query(value = "{}", sort = "{'name': 1}")
    List<Category> findAllOrderByNameAsc();
} 