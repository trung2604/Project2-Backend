package com.project2.BookStore.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderWithDetailsDTO {
    private String id;
    private String userId;
    private String fullName;
    private String email;
    private String phone;
    private String address;
    private List<OrderItemWithBookDTO> orderItems;
    private double totalAmount;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
} 