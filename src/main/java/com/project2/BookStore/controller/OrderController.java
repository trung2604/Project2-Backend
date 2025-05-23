package com.project2.BookStore.controller;

import com.project2.BookStore.dto.ApiResponseDTO;
import com.project2.BookStore.dto.OrderRequestDTO;
import com.project2.BookStore.dto.OrderResponseDTO;
import com.project2.BookStore.model.Order;
import com.project2.BookStore.service.OrderService;
import com.project2.BookStore.util.JwtUtil;
import com.project2.BookStore.exception.BadRequestException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;
    private final JwtUtil jwtUtil;

    private String getUserIdFromToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new BadRequestException("Không tìm thấy token xác thực");
        }
        String token = authHeader.substring(7);
        return jwtUtil.getUserIdFromToken(token);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponseDTO createOrder(
            @Valid @RequestBody OrderRequestDTO request,
            HttpServletRequest httpRequest) {
        String userId = getUserIdFromToken(httpRequest);
        return orderService.createOrder(request, userId);
    }

    @GetMapping("/{orderId}")
    public OrderResponseDTO getOrderById(@PathVariable String orderId) {
        return orderService.getOrderById(orderId);
    }

    @GetMapping("/user")
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> getOrdersByUser(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {
        try {
            String userId = getUserIdFromToken(request);
            PageRequest pageRequest = PageRequest.of(page, size);
            Page<OrderResponseDTO> ordersPage = orderService.getOrdersByUser(userId, pageRequest);

            Map<String, Object> meta = new HashMap<>();
            meta.put("current", page);
            meta.put("pageSize", size);
            meta.put("pages", ordersPage.getTotalPages());
            meta.put("total", ordersPage.getTotalElements());

            Map<String, Object> data = new HashMap<>();
            data.put("meta", meta);
            data.put("result", ordersPage.getContent());

            return ResponseEntity.ok(new ApiResponseDTO<>(200, "Lấy danh sách đơn hàng thành công", data));
        } catch (BadRequestException e) {
            return ResponseEntity.badRequest().body(new ApiResponseDTO<>(400, e.getMessage(), null));
        }
    }

    @GetMapping
    public Page<OrderResponseDTO> getAllOrders(Pageable pageable) {
        return orderService.getAllOrders(pageable);
    }

    @PatchMapping("/{orderId}/status")
    public OrderResponseDTO updateOrderStatus(
            @PathVariable String orderId,
            @RequestParam Order.OrderStatus status) {
        return orderService.updateOrderStatus(orderId, status);
    }

    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<ApiResponseDTO<Void>> cancelOrder(@PathVariable String orderId) {
        orderService.cancelOrder(orderId);
        return ResponseEntity.ok(new ApiResponseDTO<>(200, "Hủy đơn hàng thành công", null));
    }

    @GetMapping("/admin/all")
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> getAllOrdersForAdmin(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Order.OrderStatus status,
            @RequestParam(required = false) String search) {
        try {
            PageRequest pageRequest = PageRequest.of(page, size);
            Page<OrderResponseDTO> ordersPage = orderService.getAllOrdersForAdmin(pageRequest, status, search);

            Map<String, Object> meta = new HashMap<>();
            meta.put("current", page);
            meta.put("pageSize", size);
            meta.put("pages", ordersPage.getTotalPages());
            meta.put("total", ordersPage.getTotalElements());

            Map<String, Object> data = new HashMap<>();
            data.put("meta", meta);
            data.put("result", ordersPage.getContent());

            return ResponseEntity.ok(new ApiResponseDTO<>(200, "Lấy danh sách đơn hàng thành công", data));
        } catch (BadRequestException e) {
            return ResponseEntity.badRequest().body(new ApiResponseDTO<>(400, e.getMessage(), null));
        }
    }
} 