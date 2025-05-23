package com.project2.BookStore.service.impl;

import com.project2.BookStore.exception.ResourceNotFoundException;
import com.project2.BookStore.model.OrderItem;
import com.project2.BookStore.repository.OrderItemRepository;
import com.project2.BookStore.service.OrderItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderItemServiceImpl implements OrderItemService {
    private final OrderItemRepository orderItemRepository;

    @Override
    public OrderItem getOrderItemById(String orderItemId) {
        return orderItemRepository.findById(orderItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chi tiết đơn hàng với id: " + orderItemId));
    }

    @Override
    public Page<OrderItem> getAllOrderItems(Pageable pageable) {
        return orderItemRepository.findAll(pageable);
    }

    @Override
    @Transactional
    public OrderItem createOrderItem(OrderItem orderItem) {
        return orderItemRepository.save(orderItem);
    }

    @Override
    @Transactional
    public OrderItem updateOrderItem(String orderItemId, OrderItem orderItem) {
        OrderItem existingOrderItem = getOrderItemById(orderItemId);
        existingOrderItem.setQuantity(orderItem.getQuantity());
        existingOrderItem.setPrice(orderItem.getPrice());
        existingOrderItem.setSubtotal(orderItem.getSubtotal());
        return orderItemRepository.save(existingOrderItem);
    }

    @Override
    @Transactional
    public void deleteOrderItem(String orderItemId) {
        OrderItem orderItem = getOrderItemById(orderItemId);
        orderItemRepository.delete(orderItem);
    }
} 