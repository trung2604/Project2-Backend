package com.project2.BookStore.repository;

import com.project2.BookStore.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, String> {
    
    Optional<Payment> findByOrderId(String orderId);
    
    List<Payment> findByOrder_User_Id(String userId);
    
    Optional<Payment> findByTransactionId(String transactionId);
    
    Optional<Payment> findByVnpayTransactionId(String vnpayTransactionId);
    
    @Query("SELECT p FROM Payment p WHERE p.order.id = :orderId AND p.status = 'COMPLETED'")
    Optional<Payment> findCompletedPaymentByOrderId(@Param("orderId") String orderId);
    
    @Query("SELECT p FROM Payment p WHERE p.status = :status")
    List<Payment> findByStatus(@Param("status") Payment.PaymentStatus status);
    
    @Query("SELECT COUNT(p) FROM Payment p WHERE p.order.user.id = :userId AND p.status = 'COMPLETED'")
    long countCompletedPaymentsByUserId(@Param("userId") String userId);
} 