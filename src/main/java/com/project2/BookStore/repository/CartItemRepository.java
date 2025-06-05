package com.project2.BookStore.repository;

import com.project2.BookStore.model.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, String> {
    List<CartItem> findByUser_Id(String userId);
    Optional<CartItem> findByUser_IdAndBook_Id(String userId, String bookId);
    void deleteByUser_IdAndBook_Id(String userId, String bookId);
} 