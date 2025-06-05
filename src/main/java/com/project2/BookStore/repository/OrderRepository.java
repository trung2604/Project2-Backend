package com.project2.BookStore.repository;

import com.project2.BookStore.model.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
} 