package com.project2.BookStore.service;

import com.project2.BookStore.dto.PaymentDTO;
import com.project2.BookStore.model.Payment;
 
public interface PaymentService {
    PaymentDTO.VNPayPaymentResponse createVNPayPayment(PaymentDTO.VNPayPaymentRequest request, String userId, String ipAddress);
    boolean handleVNPayCallback(java.util.Map<String, String> params);
    PaymentDTO.PaymentStatusResponse getPaymentStatus(String orderId);
} 