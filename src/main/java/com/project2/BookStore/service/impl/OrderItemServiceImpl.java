package com.project2.BookStore.service.impl;

import com.project2.BookStore.exception.ResourceNotFoundException;
import com.project2.BookStore.model.OrderItem;
import com.project2.BookStore.repository.OrderItemRepository;
import com.project2.BookStore.service.OrderItemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderItemServiceImpl implements OrderItemService {
    private final OrderItemRepository orderItemRepository;

    @Override
    public OrderItem getOrderItemById(String orderItemId) {
        return orderItemRepository.findById(orderItemId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chi tiết đơn hàng với ID: " + orderItemId));
    }

    @Override
    public Page<OrderItem> getAllOrderItems(Pageable pageable) {
        return orderItemRepository.findAll(pageable);
    }

    @Override
    public Page<OrderItem> getOrderItemsByOrderId(String orderId, Pageable pageable) {
        return orderItemRepository.findByOrderId(orderId, pageable);
    }

    @Override
    @Transactional
    public OrderItem createOrderItem(OrderItem orderItem) {
        log.info("Tạo chi tiết đơn hàng mới cho đơn hàng: {}", orderItem.getOrder().getId());
        return orderItemRepository.save(orderItem);
    }

    @Override
    @Transactional
    public OrderItem updateOrderItem(String orderItemId, OrderItem orderItem) {
        log.info("Cập nhật chi tiết đơn hàng: {}", orderItemId);
        OrderItem existingOrderItem = getOrderItemById(orderItemId);
        
        // Cập nhật các trường
        existingOrderItem.setQuantity(orderItem.getQuantity());
        existingOrderItem.setPrice(orderItem.getPrice());
        existingOrderItem.setSubtotal(orderItem.getSubtotal());
        
        return orderItemRepository.save(existingOrderItem);
    }

    @Override
    @Transactional
    public void deleteOrderItem(String orderItemId) {
        log.info("Xóa chi tiết đơn hàng: {}", orderItemId);
        if (!orderItemRepository.existsById(orderItemId)) {
            throw new ResourceNotFoundException("Không tìm thấy chi tiết đơn hàng với ID: " + orderItemId);
        }
        orderItemRepository.deleteById(orderItemId);
    }

    @Override
    @Transactional
    public void deleteOrderItemsByOrderId(String orderId) {
        log.info("Xóa tất cả chi tiết đơn hàng của đơn hàng: {}", orderId);
        orderItemRepository.deleteByOrderId(orderId);
    }
} 