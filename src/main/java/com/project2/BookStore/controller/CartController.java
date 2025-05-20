package com.project2.BookStore.controller;

import com.project2.BookStore.model.CartItem;
import com.project2.BookStore.dto.CartItemDTO;
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

@RestController
@RequestMapping("/api/bookStore/cart")
public class CartController {

    @Autowired
    private CartService cartService;

    @Autowired
    private JwtUtil jwtUtil;

    private String getUserIdFromToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new BadRequestException("Không tìm thấy token xác thực");
        }
        String token = authHeader.substring(7);
        return jwtUtil.getEmailFromToken(token);
    }

    @PostMapping("/add")
    public ResponseEntity<ApiResponseDTO<CartItem>> addToCart(
            @Valid @RequestBody CartItemDTO cartItemDTO,
            HttpServletRequest request) {
        try {
            String userId = getUserIdFromToken(request);
            CartItem cartItem = cartService.addToCart(userId, cartItemDTO);
            ApiResponseDTO<CartItem> response = new ApiResponseDTO<>(
                200,
                "Thêm sách vào giỏ hàng thành công!",
                cartItem
            );
            return ResponseEntity.ok(response);
        } catch (BadRequestException e) {
            ApiResponseDTO<CartItem> response = new ApiResponseDTO<>(400, e.getMessage(), null);
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/update")
    public ResponseEntity<ApiResponseDTO<CartItem>> updateCartItem(
            @Valid @RequestBody CartItemDTO cartItemDTO,
            HttpServletRequest request) {
        try {
            String userId = getUserIdFromToken(request);
            CartItem cartItem = cartService.updateCartItem(userId, cartItemDTO);
            ApiResponseDTO<CartItem> response = new ApiResponseDTO<>(
                200,
                "Cập nhật giỏ hàng thành công!",
                cartItem
            );
            return ResponseEntity.ok(response);
        } catch (BadRequestException e) {
            ApiResponseDTO<CartItem> response = new ApiResponseDTO<>(400, e.getMessage(), null);
            return ResponseEntity.badRequest().body(response);
        }
    }

    @DeleteMapping("/remove/{bookId}")
    public ResponseEntity<ApiResponseDTO<Void>> removeFromCart(
            @PathVariable String bookId,
            HttpServletRequest request) {
        try {
            String userId = getUserIdFromToken(request);
            cartService.removeFromCart(userId, bookId);
            ApiResponseDTO<Void> response = new ApiResponseDTO<>(
                200,
                "Xóa sách khỏi giỏ hàng thành công!",
                null
            );
            return ResponseEntity.ok(response);
        } catch (BadRequestException e) {
            ApiResponseDTO<Void> response = new ApiResponseDTO<>(400, e.getMessage(), null);
            return ResponseEntity.badRequest().body(response);
        }
    }

    @DeleteMapping("/clear")
    public ResponseEntity<ApiResponseDTO<Void>> clearCart(HttpServletRequest request) {
        try {
            String userId = getUserIdFromToken(request);
            cartService.clearCart(userId);
            ApiResponseDTO<Void> response = new ApiResponseDTO<>(
                200,
                "Xóa giỏ hàng thành công!",
                null
            );
            return ResponseEntity.ok(response);
        } catch (BadRequestException e) {
            ApiResponseDTO<Void> response = new ApiResponseDTO<>(400, e.getMessage(), null);
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/items")
    public ResponseEntity<ApiResponseDTO<List<CartItemDetailDTO>>> getCartItems(HttpServletRequest request) {
        try {
            String userId = getUserIdFromToken(request);
            List<CartItemDetailDTO> items = cartService.getCartItems(userId);
            ApiResponseDTO<List<CartItemDetailDTO>> response = new ApiResponseDTO<>(
                200,
                "",
                items
            );
            return ResponseEntity.ok(response);
        } catch (BadRequestException e) {
            ApiResponseDTO<List<CartItemDetailDTO>> response = new ApiResponseDTO<>(400, e.getMessage(), null);
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/item/{bookId}")
    public ResponseEntity<ApiResponseDTO<CartItemDetailDTO>> getCartItem(
            @PathVariable String bookId,
            HttpServletRequest request) {
        try {
            String userId = getUserIdFromToken(request);
            CartItemDetailDTO item = cartService.getCartItem(userId, bookId);
            ApiResponseDTO<CartItemDetailDTO> response = new ApiResponseDTO<>(
                200,
                "",
                item
            );
            return ResponseEntity.ok(response);
        } catch (BadRequestException e) {
            ApiResponseDTO<CartItemDetailDTO> response = new ApiResponseDTO<>(404, e.getMessage(), null);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }
} 