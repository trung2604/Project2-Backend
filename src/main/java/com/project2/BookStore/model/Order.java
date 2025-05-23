package com.project2.BookStore.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Document(collection = "orders")
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    @Id
    private String id;
    private String userId;
    private String fullName;
    private String phone;
    private String address;
    private String email;
    private OrderStatus status;

    private double totalAmount;

    private PaymentMethod paymentMethod;

    private PaymentStatus paymentStatus;

    private List<String> itemIds;  // Thay thế @DBRef bằng danh sách ID

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public enum OrderStatus {
        PENDING,    // Chờ xác nhận
        CONFIRMED,  // Đã xác nhận
        SHIPPING,   // Đang giao hàng
        DELIVERED,  // Đã giao hàng
        CANCELLED   // Đã hủy
    }

    public enum PaymentMethod {
        COD,        // Thanh toán khi nhận hàng
        BANKING     // Chuyển khoản ngân hàng
    }

    public enum PaymentStatus {
        PENDING,    // Chờ thanh toán
        PAID,       // Đã thanh toán
        FAILED      // Thanh toán thất bại
    }
}
