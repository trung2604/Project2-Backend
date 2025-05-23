package com.project2.BookStore.service;

import com.project2.BookStore.model.OrderItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderItemService {
    OrderItem getOrderItemById(String orderItemId);

    Page<OrderItem> getAllOrderItems(Pageable pageable);

    OrderItem createOrderItem(OrderItem orderItem);

    OrderItem updateOrderItem(String orderItemId, OrderItem orderItem);

    void deleteOrderItem(String orderItemId);
} 