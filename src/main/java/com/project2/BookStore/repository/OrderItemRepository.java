package com.project2.BookStore.repository;

import com.project2.BookStore.model.OrderItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, String> {
    Page<OrderItem> findByOrderId(String orderId, Pageable pageable);
    
    @Modifying
    @Query("DELETE FROM OrderItem oi WHERE oi.order.id = ?1")
    void deleteByOrderId(String orderId);
} 