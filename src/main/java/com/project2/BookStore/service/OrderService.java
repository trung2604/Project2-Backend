package com.project2.BookStore.service;

import com.project2.BookStore.dto.OrderRequestDTO;
import com.project2.BookStore.dto.OrderResponseDTO;
import com.project2.BookStore.model.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderService {
    OrderResponseDTO createOrder(OrderRequestDTO request, String userId);
    OrderResponseDTO getOrderById(String orderId);
    Page<OrderResponseDTO> getAllOrders(Pageable pageable);
    Page<OrderResponseDTO> getAllOrdersForAdmin(Pageable pageable, Order.OrderStatus status, String search);
    Page<OrderResponseDTO> getOrdersByUser(String userId, Pageable pageable);
    OrderResponseDTO updateOrderStatus(String orderId, Order.OrderStatus status);
    void cancelOrder(String orderId);
}
