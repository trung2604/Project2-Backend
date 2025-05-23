package com.project2.BookStore.controller;

import com.project2.BookStore.model.OrderItem;
import com.project2.BookStore.service.OrderItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/order-items")
@RequiredArgsConstructor
public class OrderItemController {
    private final OrderItemService orderItemService;

    @GetMapping("/{orderItemId}")
    public OrderItem getOrderItemById(@PathVariable String orderItemId) {
        return orderItemService.getOrderItemById(orderItemId);
    }

    @GetMapping
    public Page<OrderItem> getAllOrderItems(Pageable pageable) {
        return orderItemService.getAllOrderItems(pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderItem createOrderItem(@Valid @RequestBody OrderItem orderItem) {
        return orderItemService.createOrderItem(orderItem);
    }

    @PutMapping("/{orderItemId}")
    public OrderItem updateOrderItem(
            @PathVariable String orderItemId,
            @Valid @RequestBody OrderItem orderItem) {
        return orderItemService.updateOrderItem(orderItemId, orderItem);
    }

    @DeleteMapping("/{orderItemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteOrderItem(@PathVariable String orderItemId) {
        orderItemService.deleteOrderItem(orderItemId);
    }
} 