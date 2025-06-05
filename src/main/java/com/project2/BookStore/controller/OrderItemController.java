package com.project2.BookStore.controller;

import com.project2.BookStore.dto.ApiResponseDTO;
import com.project2.BookStore.model.OrderItem;
import com.project2.BookStore.service.OrderItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/order-items")
@RequiredArgsConstructor
public class OrderItemController {
    private final OrderItemService orderItemService;

    @GetMapping("/{orderItemId}")
    public ResponseEntity<OrderItem> getOrderItemById(@PathVariable String orderItemId) {
        log.info("Lấy chi tiết đơn hàng với ID: {}", orderItemId);
        return ResponseEntity.ok(orderItemService.getOrderItemById(orderItemId));
    }

    @GetMapping
    public ResponseEntity<Page<OrderItem>> getAllOrderItems(Pageable pageable) {
        log.info("Lấy danh sách chi tiết đơn hàng");
        return ResponseEntity.ok(orderItemService.getAllOrderItems(pageable));
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<Page<OrderItem>> getOrderItemsByOrderId(
            @PathVariable String orderId,
            Pageable pageable) {
        log.info("Lấy danh sách chi tiết đơn hàng của đơn hàng: {}", orderId);
        return ResponseEntity.ok(orderItemService.getOrderItemsByOrderId(orderId, pageable));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<OrderItem> createOrderItem(@Valid @RequestBody OrderItem orderItem) {
        log.info("Tạo chi tiết đơn hàng mới");
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(orderItemService.createOrderItem(orderItem));
    }

    @PutMapping("/{orderItemId}")
    public ResponseEntity<OrderItem> updateOrderItem(
            @PathVariable String orderItemId,
            @Valid @RequestBody OrderItem orderItem) {
        log.info("Cập nhật chi tiết đơn hàng: {}", orderItemId);
        return ResponseEntity.ok(orderItemService.updateOrderItem(orderItemId, orderItem));
    }

    @DeleteMapping("/{orderItemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<ApiResponseDTO> deleteOrderItem(@PathVariable String orderItemId) {
        log.info("Xóa chi tiết đơn hàng: {}", orderItemId);
        orderItemService.deleteOrderItem(orderItemId);
        return ResponseEntity.ok(new ApiResponseDTO(true, "Xóa chi tiết đơn hàng thành công"));
    }

    @DeleteMapping("/order/{orderId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<ApiResponseDTO> deleteOrderItemsByOrderId(@PathVariable String orderId) {
        log.info("Xóa tất cả chi tiết đơn hàng của đơn hàng: {}", orderId);
        orderItemService.deleteOrderItemsByOrderId(orderId);
        return ResponseEntity.ok(new ApiResponseDTO(true, "Xóa tất cả chi tiết đơn hàng thành công"));
    }
} 