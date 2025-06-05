package com.project2.BookStore.dto;

import com.project2.BookStore.model.CartItem;
import com.project2.BookStore.model.Book;
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
    private Book.Image bookImage;   // Hình ảnh sách
    private long price;             // Giá sách
    private int quantity;           // Số lượng trong giỏ hàng
    private long totalPrice;        // Tổng tiền (price * quantity)
    private int availableQuantity;  // Số lượng có sẵn trong kho
    private String author;          // Tác giả
    private String category;        // Thể loại

    public CartItemDetailDTO(CartItem item) {
        this.id = item.getId();
        this.bookId = item.getBook().getId();
        this.bookTitle = item.getBook().getMainText();
        this.bookImage = item.getBook().getImage();
        this.price = item.getBook().getPrice();
        this.quantity = item.getQuantity();
        this.totalPrice = item.getTotalPrice();
        this.availableQuantity = item.getBook().getQuantity();
        this.author = item.getBook().getAuthor();
        this.category = item.getBook().getCategory().getName();
    }

    public static CartItemDetailDTO fromCartItem(CartItem item, int availableQuantity, String author, String category) {
        CartItemDetailDTO dto = new CartItemDetailDTO();
        dto.setId(item.getId());
        dto.setBookId(item.getBook().getId());
        dto.setBookTitle(item.getBook().getMainText());
        dto.setBookImage(item.getBook().getImage());
        dto.setPrice(item.getBook().getPrice());
        dto.setQuantity(item.getQuantity());
        dto.setTotalPrice(item.getTotalPrice());
        dto.setAvailableQuantity(availableQuantity);
        dto.setAuthor(author);
        dto.setCategory(category);
        return dto;
    }
} 