package com.project2.BookStore.repository;

import com.project2.BookStore.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, String> {
    Optional<Category> findByName(String name);
    boolean existsByName(String name);
    
    @Query("SELECT c FROM Category c WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Category> findByNameContainingIgnoreCase(String name);
    
    @Query("SELECT c FROM Category c ORDER BY c.name ASC")
    List<Category> findAllOrderByNameAsc();
} 