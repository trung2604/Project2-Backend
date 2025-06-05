package com.project2.BookStore.service;

import com.project2.BookStore.model.CartItem;
import com.project2.BookStore.dto.CartItemDTO;
import com.project2.BookStore.dto.CartItemDetailDTO;
import java.util.List;

public interface CartService {
    CartItem addToCart(String userId, CartItemDTO cartItemDTO);
    CartItem updateCartItem(String userId, CartItemDTO cartItemDTO);
    CartItem removeFromCart(String userId, String bookId);
    List<CartItemDetailDTO> clearCart(String userId);
    List<CartItemDetailDTO> getCartItems(String userId);
    CartItemDetailDTO getCartItem(String userId, String bookId);
} 