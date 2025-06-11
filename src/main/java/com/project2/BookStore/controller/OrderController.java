package com.project2.BookStore.controller;

import com.project2.BookStore.dto.ApiResponseDTO;
import com.project2.BookStore.dto.OrderRequestDTO;
import com.project2.BookStore.dto.OrderResponseDTO;
import com.project2.BookStore.model.Order;
import com.project2.BookStore.repository.UserRepository;
import com.project2.BookStore.service.OrderService;
import com.project2.BookStore.dto.UserResponseDTO;
import com.project2.BookStore.exception.BadRequestException;
import com.project2.BookStore.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/bookStore/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;
    
    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    private String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("User not authenticated");
            throw new BadRequestException("Vui lòng đăng nhập để thực hiện thao tác này");
        }

        // Get token from request header
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            log.error("No valid token found in request header");
            throw new BadRequestException("Token không hợp lệ");
        }

        // Remove "Bearer " prefix
        token = token.substring(7);

        // Get userId from token
        String userId = jwtUtil.getUserIdFromToken(token);
        if (userId == null) {
            log.error("Could not get userId from token");
            throw new BadRequestException("Token không hợp lệ hoặc đã hết hạn");
        }

        // Validate user exists
        if (!userRepository.existsById(userId)) {
            log.error("User not found for userId: {}", userId);
            throw new BadRequestException("Không tìm thấy người dùng");
        }

        return userId;
    }

    @PostMapping
    public ResponseEntity<ApiResponseDTO> createOrder(@Valid @RequestBody OrderRequestDTO request) {
        log.info("Creating new order");
        try {
            String userId = getCurrentUserId();
            log.debug("Creating order for user: {}", userId);
            OrderResponseDTO order = orderService.createOrder(request, userId);
            log.info("Order created successfully. OrderId: {}", order.getId());
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponseDTO(true, "Tạo đơn hàng thành công", order));
        } catch (BadRequestException e) {
            log.warn("Failed to create order: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(new ApiResponseDTO(false, e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error creating order: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponseDTO(false, "Lỗi server khi tạo đơn hàng", null));
        }
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponseDTO> getOrderById(@PathVariable String orderId) {
        log.info("Getting order details. OrderId: {}", orderId);
        try {
            OrderResponseDTO order = orderService.getOrderById(orderId);
            log.debug("Order found: {}", order.getId());
            return ResponseEntity.ok(new ApiResponseDTO(true, "Lấy thông tin đơn hàng thành công", order));
        } catch (BadRequestException e) {
            log.warn("Order not found: {}", orderId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponseDTO(false, e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error getting order: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponseDTO(false, "Lỗi server khi lấy thông tin đơn hàng", null));
        }
    }

    @GetMapping("/user")
    public ResponseEntity<ApiResponseDTO> getUserOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        log.info("Getting user orders. Page: {}, Size: {}, Sort: {}", page, size, sort);
        try {
            String userId = getCurrentUserId();
            log.debug("Getting orders for user: {}", userId);
            
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
            log.debug("Found {} orders for user: {}", ordersPage.getTotalElements(), userId);

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
            log.warn("Failed to get user orders: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(new ApiResponseDTO(false, e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error getting user orders: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponseDTO(false, "Lỗi server khi lấy danh sách đơn hàng", null));
        }
    }

    @GetMapping("/admin")
    public ResponseEntity<ApiResponseDTO> getAllOrdersForAdmin(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Order.OrderStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        log.info("Getting all orders for admin. Page: {}, Size: {}, Status: {}, Search: {}, Sort: {}", 
            page, size, status, search, sort);
        try {
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
            log.debug("Found {} orders for admin", ordersPage.getTotalElements());

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
            log.warn("Failed to get orders for admin: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(new ApiResponseDTO(false, e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error getting orders for admin: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponseDTO(false, "Lỗi server khi lấy danh sách đơn hàng", null));
        }
    }

    @PutMapping("/{orderId}/status")
    public ResponseEntity<ApiResponseDTO> updateOrderStatus(
            @PathVariable String orderId,
            @RequestParam Order.OrderStatus status) {
        log.info("Updating order status. OrderId: {}, Status: {}", orderId, status);
        try {
            OrderResponseDTO updatedOrder = orderService.updateOrderStatus(orderId, status);
            log.info("Order status updated successfully. OrderId: {}, New status: {}", orderId, status);
            
            Map<String, Object> response = new HashMap<>();
            response.put("order", updatedOrder);
            response.put("message", "Đã cập nhật trạng thái đơn hàng thành " + status);
            return ResponseEntity.ok(new ApiResponseDTO(true, "Cập nhật trạng thái đơn hàng thành công", response));
        } catch (BadRequestException e) {
            log.warn("Failed to update order status: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(new ApiResponseDTO(false, e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error updating order status: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponseDTO(false, "Lỗi server khi cập nhật trạng thái đơn hàng", null));
        }
    }

    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<ApiResponseDTO> cancelOrder(@PathVariable String orderId) {
        log.info("Cancelling order. OrderId: {}", orderId);
        try {
            String userId = getCurrentUserId();
            log.debug("Cancelling order {} for user: {}", orderId, userId);
            
            OrderResponseDTO cancelledOrder = orderService.cancelOrder(orderId);
            log.info("Order cancelled successfully. OrderId: {}", orderId);
            
            return ResponseEntity.ok(new ApiResponseDTO(true, "Hủy đơn hàng thành công", cancelledOrder));
        } catch (BadRequestException e) {
            log.warn("Failed to cancel order: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(new ApiResponseDTO(false, e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error cancelling order: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponseDTO(false, "Lỗi server khi hủy đơn hàng", null));
        }
    }

    @DeleteMapping("/{orderId}")
    public ResponseEntity<ApiResponseDTO> deleteOrder(@PathVariable String orderId) {
        log.info("Deleting order. OrderId: {}", orderId);
        try {
            OrderResponseDTO deletedOrder = orderService.deleteOrder(orderId);
            log.info("Order deleted successfully. OrderId: {}", orderId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("deletedOrder", deletedOrder);
            response.put("message", "Đã xóa đơn hàng " + orderId);
            return ResponseEntity.ok(new ApiResponseDTO(true, "Xóa đơn hàng thành công", response));
        } catch (BadRequestException e) {
            log.warn("Failed to delete order: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(new ApiResponseDTO(false, e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error deleting order: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponseDTO(false, "Lỗi server khi xóa đơn hàng", null));
        }
    }

    @PostMapping("/{orderId}/confirm")
    public ResponseEntity<ApiResponseDTO> confirmOrder(
            @PathVariable String orderId) {
        try {
            // Kiểm tra quyền admin
            String token = SecurityContextHolder.getContext().getAuthentication().getCredentials().toString();
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
            @PathVariable String orderId) {
        try {
            // Kiểm tra quyền shipper
            String token = SecurityContextHolder.getContext().getAuthentication().getCredentials().toString();
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
            @PathVariable String orderId) {
        try {
            // Kiểm tra quyền shipper
            String token = SecurityContextHolder.getContext().getAuthentication().getCredentials().toString();
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