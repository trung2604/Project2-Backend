package com.project2.BookStore.service.impl;

import com.project2.BookStore.model.CartItem;
import com.project2.BookStore.model.Book;
import com.project2.BookStore.model.User;
import com.project2.BookStore.dto.CartItemDTO;
import com.project2.BookStore.dto.CartItemDetailDTO;
import com.project2.BookStore.repository.CartItemRepository;
import com.project2.BookStore.repository.BookRepository;
import com.project2.BookStore.repository.UserRepository;
import com.project2.BookStore.service.CartService;
import com.project2.BookStore.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import java.util.ArrayList;

@Slf4j
@Service
public class CartServiceImpl implements CartService {
    private static final int MAX_TOTAL_ITEMS = 500000000;
    private static final long MAX_TOTAL_VALUE = 2000000000; 

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private UserRepository userRepository;

    private void validateCartLimits(String userId, int newQuantity, long newItemPrice) {
        try {
            // Tính tổng số lượng hiện tại trong giỏ hàng
            int currentTotalItems = cartItemRepository.findByUser_Id(userId).stream()
                .mapToInt(CartItem::getQuantity)
                .sum();

            // Tính tổng giá trị hiện tại của giỏ hàng
            long currentTotalValue = cartItemRepository.findByUser_Id(userId).stream()
                .mapToLong(CartItem::getTotalPrice)
                .sum();

            // Kiểm tra tổng số lượng
            if (currentTotalItems + newQuantity > MAX_TOTAL_ITEMS) {
                throw new BadRequestException(
                    String.format("Tổng số lượng sách trong giỏ không được vượt quá %d. Hiện tại: %d", 
                        MAX_TOTAL_ITEMS, currentTotalItems)
                );
            }

            // Kiểm tra tổng giá trị
            if (currentTotalValue + (newItemPrice * newQuantity) > MAX_TOTAL_VALUE) {
                throw new BadRequestException(
                    String.format("Tổng giá trị giỏ hàng không được vượt quá %d. Hiện tại: %d", 
                        MAX_TOTAL_VALUE, currentTotalValue)
                );
            }
        } catch (BadRequestException e) {
            log.warn("Lỗi khi kiểm tra giới hạn giỏ hàng: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Lỗi không mong muốn khi kiểm tra giới hạn giỏ hàng: {}", e.getMessage());
            throw new BadRequestException("Không thể kiểm tra giới hạn giỏ hàng: " + e.getMessage());
        }
    }

    private void validateBook(Book book) {
        if (book == null) {
            throw new BadRequestException("Không tìm thấy sách");
        }
        if (book.getQuantity() <= 0) {
            throw new BadRequestException("Sách đã hết hàng");
        }
        if (book.getPrice() <= 0) {
            throw new BadRequestException("Giá sách không hợp lệ");
        }
    }

    @Override
    @Transactional
    public CartItem addToCart(String userId, CartItemDTO cartItemDTO) {
        log.info("Bắt đầu thêm sách vào giỏ hàng. UserId: {}, BookId: {}", userId, cartItemDTO.getBookId());
        try {
            // Validate user
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy người dùng"));

            // Validate book
            Book book = bookRepository.findById(cartItemDTO.getBookId())
                .orElseThrow(() -> new BadRequestException("Không tìm thấy sách"));

            // Validate book quantity
            if (book.getQuantity() < cartItemDTO.getQuantity()) {
                throw new BadRequestException(
                    String.format("Số lượng sách '%s' trong kho không đủ. Còn lại: %d", 
                        book.getMainText(), book.getQuantity())
                );
            }

            // Validate cart limits
            validateCartLimits(userId, cartItemDTO.getQuantity(), book.getPrice());

            // Check if book already in cart
            Optional<CartItem> existingItem = cartItemRepository.findByUser_IdAndBook_Id(userId, cartItemDTO.getBookId());
            CartItem cartItem;

            if (existingItem.isPresent()) {
                cartItem = existingItem.get();
                int newQuantity = cartItem.getQuantity() + cartItemDTO.getQuantity();
                
                // Validate total quantity
                if (newQuantity > book.getQuantity()) {
                    throw new BadRequestException(
                        String.format("Tổng số lượng sách '%s' trong giỏ vượt quá số lượng trong kho. Còn lại: %d", 
                            book.getMainText(), book.getQuantity())
                    );
                }
                
                cartItem.setQuantity(newQuantity);
                cartItem.setTotalPrice(book.getPrice() * newQuantity);
                log.info("Cập nhật số lượng sách trong giỏ hàng. BookId: {}, Số lượng mới: {}", 
                    cartItemDTO.getBookId(), newQuantity);
            } else {
                cartItem = new CartItem();
                cartItem.setUser(user);
                cartItem.setBook(book);
                cartItem.setQuantity(cartItemDTO.getQuantity());
                cartItem.setTotalPrice(book.getPrice() * cartItemDTO.getQuantity());
                log.info("Thêm sách mới vào giỏ hàng. BookId: {}, Số lượng: {}", 
                    cartItemDTO.getBookId(), cartItemDTO.getQuantity());
            }

            CartItem savedItem = cartItemRepository.save(cartItem);
            log.info("Lưu giỏ hàng thành công. CartItemId: {}", savedItem.getId());
            return savedItem;
        } catch (BadRequestException e) {
            log.warn("Lỗi khi thêm sách vào giỏ hàng: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Lỗi không mong muốn khi thêm sách vào giỏ hàng: {}", e.getMessage());
            throw new BadRequestException("Không thể thêm sách vào giỏ hàng: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public CartItem updateCartItem(String userId, CartItemDTO cartItemDTO) {
        log.info("Bắt đầu cập nhật giỏ hàng. UserId: {}, BookId: {}, Quantity: {}", 
            userId, cartItemDTO.getBookId(), cartItemDTO.getQuantity());

        try {
            // Kiểm tra sách tồn tại và hợp lệ
            Book book = bookRepository.findById(cartItemDTO.getBookId())
                .orElseThrow(() -> new BadRequestException("Không tìm thấy sách"));
            validateBook(book);

            // Kiểm tra số lượng sách có đủ không
            if (book.getQuantity() < cartItemDTO.getQuantity()) {
                throw new BadRequestException(
                    String.format("Số lượng sách trong kho không đủ. Còn lại: %d", book.getQuantity())
                );
            }

            // Lấy item từ giỏ hàng
            CartItem item = cartItemRepository.findByUser_IdAndBook_Id(userId, cartItemDTO.getBookId())
                .orElseThrow(() -> new BadRequestException("Không tìm thấy sách trong giỏ hàng"));

            // Kiểm tra giới hạn giỏ hàng (trừ đi số lượng cũ và cộng số lượng mới)
            validateCartLimits(userId, cartItemDTO.getQuantity() - item.getQuantity(), book.getPrice());

            // Cập nhật số lượng và tổng giá
            item.setQuantity(cartItemDTO.getQuantity());
            item.setTotalPrice(book.getPrice() * cartItemDTO.getQuantity());

            CartItem updatedItem = cartItemRepository.save(item);
            log.info("Cập nhật giỏ hàng thành công. CartItemId: {}", updatedItem.getId());
            return updatedItem;
        } catch (BadRequestException e) {
            log.warn("Lỗi khi cập nhật giỏ hàng: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Lỗi không mong muốn khi cập nhật giỏ hàng: {}", e.getMessage());
            throw new BadRequestException("Không thể cập nhật giỏ hàng: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public CartItem removeFromCart(String userId, String bookId) {
        log.info("Bắt đầu xóa sách khỏi giỏ hàng. UserId: {}, BookId: {}", userId, bookId);

        try {
            CartItem item = cartItemRepository.findByUser_IdAndBook_Id(userId, bookId)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy sách trong giỏ hàng"));

            cartItemRepository.delete(item);
            log.info("Xóa sách khỏi giỏ hàng thành công. CartItemId: {}", item.getId());
            return item;
        } catch (BadRequestException e) {
            log.warn("Lỗi khi xóa sách khỏi giỏ hàng: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Lỗi không mong muốn khi xóa sách khỏi giỏ hàng: {}", e.getMessage());
            throw new BadRequestException("Không thể xóa sách khỏi giỏ hàng: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public List<CartItemDetailDTO> clearCart(String userId) {
        log.info("Bắt đầu xóa toàn bộ giỏ hàng. UserId: {}", userId);

        try {
            List<CartItem> items = cartItemRepository.findByUser_Id(userId);
            if (items.isEmpty()) {
                return new ArrayList<>();
            }

            List<CartItemDetailDTO> removedItems = items.stream()
                .map(CartItemDetailDTO::new)
                .collect(Collectors.toList());

            cartItemRepository.deleteAll(items);
            log.info("Xóa toàn bộ giỏ hàng thành công. Số lượng items: {}", items.size());
            return removedItems;
        } catch (Exception e) {
            log.error("Lỗi không mong muốn khi xóa toàn bộ giỏ hàng: {}", e.getMessage());
            throw new BadRequestException("Không thể xóa toàn bộ giỏ hàng: " + e.getMessage());
        }
    }

    @Override
    public List<CartItemDetailDTO> getCartItems(String userId) {
        log.info("Bắt đầu lấy danh sách sách trong giỏ hàng. UserId: {}", userId);

        try {
            List<CartItem> items = cartItemRepository.findByUser_Id(userId);
            List<CartItemDetailDTO> cartItems = items.stream()
                .map(CartItemDetailDTO::new)
                .collect(Collectors.toList());

            log.info("Lấy danh sách sách trong giỏ hàng thành công. Số lượng: {}", cartItems.size());
            return cartItems;
        } catch (Exception e) {
            log.error("Lỗi không mong muốn khi lấy danh sách sách trong giỏ hàng: {}", e.getMessage());
            throw new BadRequestException("Không thể lấy danh sách sách trong giỏ hàng: " + e.getMessage());
        }
    }

    @Override
    public CartItemDetailDTO getCartItem(String userId, String bookId) {
        log.info("Bắt đầu lấy thông tin sách trong giỏ hàng. UserId: {}, BookId: {}", userId, bookId);

        try {
            CartItem item = cartItemRepository.findByUser_IdAndBook_Id(userId, bookId)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy sách trong giỏ hàng"));

            log.info("Lấy thông tin sách trong giỏ hàng thành công. CartItemId: {}", item.getId());
            return new CartItemDetailDTO(item);
        } catch (BadRequestException e) {
            log.warn("Lỗi khi lấy thông tin sách trong giỏ hàng: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Lỗi không mong muốn khi lấy thông tin sách trong giỏ hàng: {}", e.getMessage());
            throw new BadRequestException("Không thể lấy thông tin sách trong giỏ hàng: " + e.getMessage());
        }
    }
} 