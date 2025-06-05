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
@RequestMapping("/api/bookStore/orders")
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
    public ResponseEntity<ApiResponseDTO> createOrder(
            @Valid @RequestBody OrderRequestDTO request,
            HttpServletRequest httpRequest) {
        try {
            String userId = getUserIdFromToken(httpRequest);
            OrderResponseDTO order = orderService.createOrder(request, userId);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponseDTO(true, "Tạo đơn hàng thành công", order));
        } catch (BadRequestException e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponseDTO(false, e.getMessage(), null));
        }
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponseDTO> getOrderById(@PathVariable String orderId) {
        try {
            OrderResponseDTO order = orderService.getOrderById(orderId);
            return ResponseEntity.ok(new ApiResponseDTO(true, "Lấy thông tin đơn hàng thành công", order));
        } catch (BadRequestException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponseDTO(false, e.getMessage(), null));
        }
    }

    @GetMapping("/user")
    public ResponseEntity<ApiResponseDTO> getUserOrders(
            HttpServletRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        try {
            String userId = getUserIdFromToken(request);
            
            // Parse sort parameter
            String[] sortParams = sort.split(",");
            String sortField = sortParams[0];
            String sortDirection = sortParams.length > 1 ? sortParams[1] : "desc";
            
            PageRequest pageRequest = PageRequest.of(page, size, 
                sortDirection.equalsIgnoreCase("desc") ? 
                    org.springframework.data.domain.Sort.Direction.DESC : 
                    org.springframework.data.domain.Sort.Direction.ASC, 
                sortField);

            Page<OrderResponseDTO> ordersPage = orderService.getUserOrders(userId, pageRequest);

            Map<String, Object> meta = new HashMap<>();
            meta.put("current", page);
            meta.put("pageSize", size);
            meta.put("pages", ordersPage.getTotalPages());
            meta.put("total", ordersPage.getTotalElements());

            Map<String, Object> data = new HashMap<>();
            data.put("meta", meta);
            data.put("result", ordersPage.getContent());

            return ResponseEntity.ok(new ApiResponseDTO(true, "Lấy danh sách đơn hàng thành công", data));
        } catch (BadRequestException e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponseDTO(false, e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponseDTO(false, "Lỗi server: " + e.getMessage(), null));
        }
    }

    @GetMapping("/admin")
    public ResponseEntity<ApiResponseDTO> getAllOrdersForAdmin(
            HttpServletRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Order.OrderStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        try {
            // Validate admin role
            String token = request.getHeader("Authorization").substring(7);
            if (!jwtUtil.hasRole(token, "ROLE_ADMIN")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponseDTO(false, "Không có quyền truy cập", null));
            }

            // Parse sort parameter
            String[] sortParams = sort.split(",");
            String sortField = sortParams[0];
            String sortDirection = sortParams.length > 1 ? sortParams[1] : "desc";
            
            PageRequest pageRequest = PageRequest.of(page, size, 
                sortDirection.equalsIgnoreCase("desc") ? 
                    org.springframework.data.domain.Sort.Direction.DESC : 
                    org.springframework.data.domain.Sort.Direction.ASC, 
                sortField);

            Page<OrderResponseDTO> ordersPage = orderService.getAllOrdersForAdmin(pageRequest, status, search);

            Map<String, Object> meta = new HashMap<>();
            meta.put("current", page);
            meta.put("pageSize", size);
            meta.put("pages", ordersPage.getTotalPages());
            meta.put("total", ordersPage.getTotalElements());

            Map<String, Object> data = new HashMap<>();
            data.put("meta", meta);
            data.put("result", ordersPage.getContent());

            return ResponseEntity.ok(new ApiResponseDTO(true, "Lấy danh sách đơn hàng thành công", data));
        } catch (BadRequestException e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponseDTO(false, e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponseDTO(false, "Lỗi server: " + e.getMessage(), null));
        }
    }

    @PutMapping("/{orderId}/status")
    public ResponseEntity<ApiResponseDTO> updateOrderStatus(
            @PathVariable String orderId,
            @RequestParam Order.OrderStatus status) {
        try {
            OrderResponseDTO updatedOrder = orderService.updateOrderStatus(orderId, status);
            Map<String, Object> response = new HashMap<>();
            response.put("order", updatedOrder);
            response.put("message", "Đã cập nhật trạng thái đơn hàng thành " + status);
            return ResponseEntity.ok(new ApiResponseDTO(true, "Cập nhật trạng thái đơn hàng thành công", response));
        } catch (BadRequestException e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponseDTO(false, e.getMessage(), null));
        }
    }

    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<ApiResponseDTO> cancelOrder(
            @PathVariable String orderId,
            HttpServletRequest request) {
        try {
            String userId = getUserIdFromToken(request);
            String token = request.getHeader("Authorization").substring(7);
            boolean isAdmin = jwtUtil.hasRole(token, "ROLE_ADMIN");
            
            // Kiểm tra xem đơn hàng có thuộc về user không (nếu không phải admin)
            OrderResponseDTO order = orderService.getOrderById(orderId);
            if (!isAdmin && !order.getUserId().equals(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponseDTO(false, "Không có quyền hủy đơn hàng này", null));
            }

            // Kiểm tra trạng thái đơn hàng
            if (!isAdmin && order.getStatus() != Order.OrderStatus.PENDING) {
                return ResponseEntity.badRequest()
                    .body(new ApiResponseDTO(false, "Chỉ có thể hủy đơn hàng ở trạng thái chờ xác nhận", null));
            }

            OrderResponseDTO cancelledOrder = orderService.cancelOrder(orderId);
            return ResponseEntity.ok(new ApiResponseDTO(true, "Hủy đơn hàng thành công", cancelledOrder));
        } catch (BadRequestException e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponseDTO(false, e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponseDTO(false, "Lỗi server: " + e.getMessage(), null));
        }
    }

    @DeleteMapping("/{orderId}")
    public ResponseEntity<ApiResponseDTO> deleteOrder(@PathVariable String orderId) {
        try {
            OrderResponseDTO deletedOrder = orderService.deleteOrder(orderId);
            Map<String, Object> response = new HashMap<>();
            response.put("deletedOrder", deletedOrder);
            response.put("message", "Đã xóa đơn hàng " + orderId);
            return ResponseEntity.ok(new ApiResponseDTO(true, "Xóa đơn hàng thành công", response));
        } catch (BadRequestException e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponseDTO(false, e.getMessage(), null));
        }
    }

    @PostMapping("/{orderId}/confirm")
    public ResponseEntity<ApiResponseDTO> confirmOrder(
            @PathVariable String orderId,
            HttpServletRequest request) {
        try {
            // Kiểm tra quyền admin
            String token = request.getHeader("Authorization").substring(7);
            if (!jwtUtil.hasRole(token, "ROLE_ADMIN")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponseDTO(false, "Không có quyền xác nhận đơn hàng", null));
            }

            // Kiểm tra đơn hàng
            OrderResponseDTO order = orderService.getOrderById(orderId);
            if (order.getStatus() != Order.OrderStatus.PENDING) {
                return ResponseEntity.badRequest()
                    .body(new ApiResponseDTO(false, "Chỉ có thể xác nhận đơn hàng ở trạng thái chờ xác nhận", null));
            }

            OrderResponseDTO confirmedOrder = orderService.updateOrderStatus(orderId, Order.OrderStatus.CONFIRMED);
            return ResponseEntity.ok(new ApiResponseDTO(true, "Xác nhận đơn hàng thành công", confirmedOrder));
        } catch (BadRequestException e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponseDTO(false, e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponseDTO(false, "Lỗi server: " + e.getMessage(), null));
        }
    }

    @PostMapping("/{orderId}/shipping")
    public ResponseEntity<ApiResponseDTO> updateOrderToShipping(
            @PathVariable String orderId,
            HttpServletRequest request) {
        try {
            // Kiểm tra quyền shipper
            String token = request.getHeader("Authorization").substring(7);
            if (!jwtUtil.hasRole(token, "ROLE_SHIPPER")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponseDTO(false, "Không có quyền cập nhật trạng thái giao hàng", null));
            }

            // Kiểm tra đơn hàng
            OrderResponseDTO order = orderService.getOrderById(orderId);
            if (order.getStatus() != Order.OrderStatus.CONFIRMED) {
                return ResponseEntity.badRequest()
                    .body(new ApiResponseDTO(false, "Chỉ có thể cập nhật trạng thái giao hàng cho đơn hàng đã xác nhận", null));
            }

            OrderResponseDTO updatedOrder = orderService.updateOrderStatus(orderId, Order.OrderStatus.SHIPPING);
            return ResponseEntity.ok(new ApiResponseDTO(true, "Cập nhật trạng thái giao hàng thành công", updatedOrder));
        } catch (BadRequestException e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponseDTO(false, e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponseDTO(false, "Lỗi server: " + e.getMessage(), null));
        }
    }

    @PostMapping("/{orderId}/delivered")
    public ResponseEntity<ApiResponseDTO> updateOrderToDelivered(
            @PathVariable String orderId,
            HttpServletRequest request) {
        try {
            // Kiểm tra quyền shipper
            String token = request.getHeader("Authorization").substring(7);
            if (!jwtUtil.hasRole(token, "ROLE_SHIPPER")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponseDTO(false, "Không có quyền cập nhật trạng thái đã giao hàng", null));
            }

            // Kiểm tra đơn hàng
            OrderResponseDTO order = orderService.getOrderById(orderId);
            if (order.getStatus() != Order.OrderStatus.SHIPPING) {
                return ResponseEntity.badRequest()
                    .body(new ApiResponseDTO(false, "Chỉ có thể cập nhật trạng thái đã giao hàng cho đơn hàng đang giao", null));
            }

            OrderResponseDTO updatedOrder = orderService.updateOrderStatus(orderId, Order.OrderStatus.DELIVERED);
            return ResponseEntity.ok(new ApiResponseDTO(true, "Cập nhật trạng thái đã giao hàng thành công", updatedOrder));
        } catch (BadRequestException e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponseDTO(false, e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponseDTO(false, "Lỗi server: " + e.getMessage(), null));
        }
    }
} 