package com.project2.BookStore.dto;

import com.project2.BookStore.model.CartItem;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CartItemDetailDTO {
    private String id;              // ID của item trong giỏ hàng
    private String bookId;          // ID của sách
    private String bookTitle;       // Tên sách
    private String bookImage;       // Hình ảnh sách
    private String author;          // Tác giả
    private String category;        // Thể loại
    private long price;             // Giá sách
    private int quantity;           // Số lượng trong giỏ hàng
    private int availableQuantity;  // Số lượng có sẵn trong kho
    private long totalPrice;        // Tổng tiền (price * quantity)
    private String userId;          // ID người dùng

    public static CartItemDetailDTO fromCartItem(CartItem cartItem, int availableQuantity, String author, String category) {
        CartItemDetailDTO dto = new CartItemDetailDTO();
        dto.setId(cartItem.getId());
        dto.setBookId(cartItem.getBookId());
        dto.setBookTitle(cartItem.getBookTitle());
        dto.setBookImage(cartItem.getBookImage());
        dto.setAuthor(author);
        dto.setCategory(category);
        dto.setPrice(cartItem.getPrice());
        dto.setQuantity(cartItem.getQuantity());
        dto.setAvailableQuantity(availableQuantity);
        dto.setTotalPrice(cartItem.getTotalPrice());
        dto.setUserId(cartItem.getUserId());
        return dto;
    }
} 