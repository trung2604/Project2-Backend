package com.project2.BookStore.repository;

import com.project2.BookStore.model.Order;
import com.project2.BookStore.model.Order.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, String> {
    Page<Order> findByStatus(Order.OrderStatus status, Pageable pageable);
    Page<Order> findByUserId(String userId, Pageable pageable);

    @Query("SELECT o FROM Order o WHERE " +
           "(:status IS NULL OR o.status = :status) AND " +
           "(:search IS NULL OR " +
           "o.fullName LIKE %:search% OR " +
           "o.email LIKE %:search% OR " +
           "o.phone LIKE %:search% OR " +
           "o.address LIKE %:search%)")
    Page<Order> findByStatusAndSearch(
        @Param("status") Order.OrderStatus status,
        @Param("search") String search,
        Pageable pageable
    );

    @Query("SELECT COUNT(o) FROM Order o WHERE o.user.id = :userId AND o.status NOT IN :statuses")
    long countByUserIdAndStatusNotIn(@Param("userId") String userId, @Param("statuses") List<Order.OrderStatus> statuses);

    List<Order> findAllByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    List<Order> findByStatus(OrderStatus status);
    
    // Tìm order gần nhất của user cho một sách cụ thể
    @Query("SELECT o FROM Order o " +
           "JOIN o.orderItems oi " +
           "WHERE o.user.id = :userId " +
           "AND oi.book.id = :bookId " +
           "AND o.status = :status " +
           "ORDER BY o.createdAt DESC")
    List<Order> findByUserIdAndOrderItemsBookIdAndStatusOrderByCreatedAtDesc(
        @Param("userId") String userId, 
        @Param("bookId") String bookId, 
        @Param("status") OrderStatus status);
} 