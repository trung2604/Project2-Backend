package com.project2.BookStore.service;

import com.project2.BookStore.dto.BuyNowRequestDTO;
import com.project2.BookStore.dto.OrderRequestDTO;
import com.project2.BookStore.dto.OrderResponseDTO;
import com.project2.BookStore.dto.OrderWithDetailsDTO;
import com.project2.BookStore.model.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderService {
    OrderResponseDTO createOrder(OrderRequestDTO request, String userId);
    OrderResponseDTO buyNow(BuyNowRequestDTO request, String userId);
    OrderResponseDTO getOrderById(String orderId);
    Page<OrderResponseDTO> getUserOrders(String userId, Pageable pageable);
    Page<OrderResponseDTO> getAllOrders(Pageable pageable);
    Page<OrderResponseDTO> getAllOrdersForAdmin(Pageable pageable, Order.OrderStatus status, String search);
    Page<OrderResponseDTO> getOrdersByUser(String userId, Pageable pageable);
    OrderResponseDTO updateOrderStatus(String orderId, Order.OrderStatus status);
    OrderResponseDTO cancelOrder(String orderId);
    Page<OrderWithDetailsDTO> getOrdersPaged(Pageable pageable);
    OrderResponseDTO deleteOrder(String orderId);
    boolean hasActiveOrders(String userId);
}
