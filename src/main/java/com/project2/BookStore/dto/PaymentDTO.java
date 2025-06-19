package com.project2.BookStore.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PaymentDTO {
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreatePaymentRequest {
        @NotBlank(message = "Order ID không được trống")
        private String orderId;
        
        @NotNull(message = "Số tiền không được trống")
        @DecimalMin(value = "1000", message = "Số tiền phải lớn hơn 1,000 VND")
        private BigDecimal amount;
        
        private String description;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentResponse {
        private String paymentId;
        private String orderId;
        private BigDecimal amount;
        private String currency;
        private String status;
        private String paymentUrl;
        private String transactionId;
        private LocalDateTime createdAt;
        private String description;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VNPayPaymentRequest {
        @NotBlank(message = "Order ID không được trống")
        private String orderId;
        
        @NotNull(message = "Số tiền không được trống")
        @DecimalMin(value = "1000", message = "Số tiền phải lớn hơn 1,000 VND")
        private BigDecimal amount;
        
        private String description;
        private String returnUrl;
        private String ipAddress;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VNPayPaymentResponse {
        private String paymentId;
        private String orderId;
        private BigDecimal amount;
        private String paymentUrl;
        private String transactionId;
        private String checksum;
        private LocalDateTime createdAt;
        private String description;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VNPayCallbackRequest {
        private String vnp_TxnRef;
        private String vnp_Amount;
        private String vnp_ResponseCode;
        private String vnp_TransactionNo;
        private String vnp_Message;
        private String vnp_SecureHash;
        private String vnp_OrderInfo;
        private String vnp_PayDate;
        private String vnp_BankCode;
        private String vnp_CardType;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentStatusResponse {
        private String paymentId;
        private String orderId;
        private String status;
        private String message;
        private LocalDateTime updatedAt;
    }
} 