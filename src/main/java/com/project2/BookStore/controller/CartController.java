package com.project2.BookStore.controller;

import com.project2.BookStore.model.CartItem;
import com.project2.BookStore.dto.CartItemDTO;
import com.project2.BookStore.dto.CartItemResponseDTO;
import com.project2.BookStore.dto.ApiResponseDTO;
import com.project2.BookStore.dto.CartItemDetailDTO;
import com.project2.BookStore.service.CartService;
import com.project2.BookStore.exception.BadRequestException;
import com.project2.BookStore.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/bookStore/cart")
public class CartController {

    @Autowired
    private CartService cartService;

    @Autowired
    private JwtUtil jwtUtil;

    private String getUserIdFromToken(HttpServletRequest request) {
        log.info("Getting user ID from token");
        String authHeader = request.getHeader("Authorization");
        log.debug("Authorization header: {}", authHeader);
        
        if (authHeader == null) {
            log.warn("Authorization header is null");
            throw new BadRequestException("Không tìm thấy token xác thực");
        }
        
        if (!authHeader.startsWith("Bearer ")) {
            log.warn("Authorization header does not start with 'Bearer '");
            throw new BadRequestException("Token không đúng định dạng");
        }
        
        String token = authHeader.substring(7);
        log.debug("Extracted token: {}", token);
        
        try {
            String userId = jwtUtil.getUserIdFromToken(token);
            log.info("Successfully extracted user ID: {}", userId);
            return userId;
        } catch (Exception e) {
            log.error("Error extracting user ID from token: {}", e.getMessage());
            throw new BadRequestException("Token không hợp lệ: " + e.getMessage());
        }
    }

    private CartItemResponseDTO convertToResponseDTO(CartItem cartItem) {
        CartItemResponseDTO dto = new CartItemResponseDTO();
        dto.setBookId(cartItem.getBookId());
        dto.setBookTitle(cartItem.getBookTitle());
        dto.setBookImage(cartItem.getBookImage());
        dto.setPrice(cartItem.getPrice());
        dto.setQuantity(cartItem.getQuantity());
        dto.setTotalPrice(cartItem.getTotalPrice());
        return dto;
    }

    @PostMapping("/add")
    public ResponseEntity<ApiResponseDTO> addToCart(
            @Valid @RequestBody CartItemDTO cartItemDTO,
            HttpServletRequest request) {
        try {
            String userId = getUserIdFromToken(request);
            CartItem cartItem = cartService.addToCart(userId, cartItemDTO);
            CartItemResponseDTO responseDTO = convertToResponseDTO(cartItem);
            return ResponseEntity.ok(new ApiResponseDTO(true, "Thêm sách vào giỏ hàng thành công", responseDTO));
        } catch (BadRequestException e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponseDTO(false, e.getMessage(), null));
        }
    }

    @PutMapping("/update")
    public ResponseEntity<ApiResponseDTO> updateCartItem(
            @Valid @RequestBody CartItemDTO cartItemDTO,
            HttpServletRequest request) {
        try {
            String userId = getUserIdFromToken(request);
            CartItem cartItem = cartService.updateCartItem(userId, cartItemDTO);
            CartItemResponseDTO responseDTO = convertToResponseDTO(cartItem);
            return ResponseEntity.ok(new ApiResponseDTO(true, "Cập nhật giỏ hàng thành công", responseDTO));
        } catch (BadRequestException e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponseDTO(false, e.getMessage(), null));
        }
    }

    @DeleteMapping("/remove/{bookId}")
    public ResponseEntity<ApiResponseDTO> removeFromCart(
            @PathVariable String bookId,
            HttpServletRequest request) {
        try {
            String userId = getUserIdFromToken(request);
            CartItem cartItem = cartService.removeFromCart(userId, bookId);
            CartItemResponseDTO responseDTO = convertToResponseDTO(cartItem);
            return ResponseEntity.ok(new ApiResponseDTO(true, "Xóa sách khỏi giỏ hàng thành công", responseDTO));
        } catch (BadRequestException e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponseDTO(false, e.getMessage(), null));
        }
    }

    @DeleteMapping("/clear")
    public ResponseEntity<ApiResponseDTO> clearCart(HttpServletRequest request) {
        try {
            String userId = getUserIdFromToken(request);
            List<CartItemDetailDTO> clearedItems = cartService.clearCart(userId);
            Map<String, Object> response = new HashMap<>();
            response.put("clearedItems", clearedItems);
            response.put("message", "Đã xóa " + clearedItems.size() + " sản phẩm khỏi giỏ hàng");
            return ResponseEntity.ok(new ApiResponseDTO(true, "Xóa giỏ hàng thành công", response));
        } catch (BadRequestException e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponseDTO(false, e.getMessage(), null));
        }
    }

    @GetMapping("/items")
    public ResponseEntity<ApiResponseDTO> getCartItems(HttpServletRequest request) {
        try {
            String userId = getUserIdFromToken(request);
            List<CartItemDetailDTO> items = cartService.getCartItems(userId);
            Map<String, Object> response = new HashMap<>();
            response.put("items", items);
            response.put("totalItems", items.size());
            return ResponseEntity.ok(new ApiResponseDTO(true, "Lấy danh sách giỏ hàng thành công", response));
        } catch (BadRequestException e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponseDTO(false, e.getMessage(), null));
        }
    }

    @GetMapping("/item/{bookId}")
    public ResponseEntity<ApiResponseDTO> getCartItem(
            @PathVariable String bookId,
            HttpServletRequest request) {
        try {
            String userId = getUserIdFromToken(request);
            CartItemDetailDTO item = cartService.getCartItem(userId, bookId);
            return ResponseEntity.ok(new ApiResponseDTO(true, "Lấy thông tin giỏ hàng thành công", item));
        } catch (BadRequestException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponseDTO(false, e.getMessage(), null));
        }
    }
} 