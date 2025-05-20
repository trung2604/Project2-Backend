package com.project2.BookStore.service.impl;

import com.project2.BookStore.model.CartItem;
import com.project2.BookStore.model.Book;
import com.project2.BookStore.dto.CartItemDTO;
import com.project2.BookStore.dto.CartItemDetailDTO;
import com.project2.BookStore.repository.CartItemRepository;
import com.project2.BookStore.repository.BookRepository;
import com.project2.BookStore.service.CartService;
import com.project2.BookStore.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CartServiceImpl implements CartService {

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private BookRepository bookRepository;

    @Override
    @Transactional
    public CartItem addToCart(String userId, CartItemDTO cartItemDTO) {
        // Kiểm tra sách tồn tại
        Book book = bookRepository.findById(cartItemDTO.getBookId())
            .orElseThrow(() -> new BadRequestException("Không tìm thấy sách"));

        // Kiểm tra số lượng sách có đủ không
        if (book.getQuantity() < cartItemDTO.getQuantity()) {
            throw new BadRequestException("Số lượng sách trong kho không đủ");
        }

        // Kiểm tra xem sách đã có trong giỏ hàng chưa
        Optional<CartItem> existingItem = cartItemRepository.findByUserIdAndBookId(userId, cartItemDTO.getBookId());
        
        if (existingItem.isPresent()) {
            // Nếu đã có, cập nhật số lượng
            CartItem item = existingItem.get();
            int newQuantity = item.getQuantity() + cartItemDTO.getQuantity();
            
            // Kiểm tra lại số lượng tổng
            if (book.getQuantity() < newQuantity) {
                throw new BadRequestException("Số lượng sách trong kho không đủ");
            }
            
            item.setQuantity(newQuantity);
            item.setTotalPrice(item.getPrice() * newQuantity);
            return cartItemRepository.save(item);
        } else {
            // Nếu chưa có, tạo mới
            CartItem newItem = new CartItem();
            newItem.setId(UUID.randomUUID().toString());
            newItem.setUserId(userId);
            newItem.setBookId(book.getId());
            newItem.setBookTitle(book.getMainText());
            newItem.setBookImage(book.getImage() != null ? book.getImage().getOriginal() : null);
            newItem.setPrice(book.getPrice());
            newItem.setQuantity(cartItemDTO.getQuantity());
            newItem.setTotalPrice(book.getPrice() * cartItemDTO.getQuantity());
            
            return cartItemRepository.save(newItem);
        }
    }

    @Override
    @Transactional
    public CartItem updateCartItem(String userId, CartItemDTO cartItemDTO) {
        // Kiểm tra sách tồn tại
        Book book = bookRepository.findById(cartItemDTO.getBookId())
            .orElseThrow(() -> new BadRequestException("Không tìm thấy sách"));

        // Kiểm tra số lượng sách có đủ không
        if (book.getQuantity() < cartItemDTO.getQuantity()) {
            throw new BadRequestException("Số lượng sách trong kho không đủ");
        }

        // Lấy item từ giỏ hàng
        CartItem item = cartItemRepository.findByUserIdAndBookId(userId, cartItemDTO.getBookId())
            .orElseThrow(() -> new BadRequestException("Không tìm thấy sách trong giỏ hàng"));

        // Cập nhật số lượng và tổng tiền
        item.setQuantity(cartItemDTO.getQuantity());
        item.setTotalPrice(item.getPrice() * cartItemDTO.getQuantity());
        
        return cartItemRepository.save(item);
    }

    @Override
    @Transactional
    public void removeFromCart(String userId, String bookId) {
        cartItemRepository.deleteByUserIdAndBookId(userId, bookId);
    }

    @Override
    @Transactional
    public void clearCart(String userId) {
        cartItemRepository.deleteByUserId(userId);
    }

    @Override
    public List<CartItemDetailDTO> getCartItems(String userId) {
        List<CartItem> cartItems = cartItemRepository.findByUserId(userId);
        return cartItems.stream()
            .map(item -> {
                Book book = bookRepository.findById(item.getBookId())
                    .orElseThrow(() -> new BadRequestException("Không tìm thấy sách"));
                return CartItemDetailDTO.fromCartItem(
                    item,
                    book.getQuantity(),
                    book.getAuthor(),
                    book.getCategory()
                );
            })
            .collect(Collectors.toList());
    }

    @Override
    public CartItemDetailDTO getCartItem(String userId, String bookId) {
        CartItem item = cartItemRepository.findByUserIdAndBookId(userId, bookId)
            .orElseThrow(() -> new BadRequestException("Không tìm thấy sách trong giỏ hàng"));
        
        Book book = bookRepository.findById(bookId)
            .orElseThrow(() -> new BadRequestException("Không tìm thấy sách"));
            
        return CartItemDetailDTO.fromCartItem(
            item,
            book.getQuantity(),
            book.getAuthor(),
            book.getCategory()
        );
    }
} 