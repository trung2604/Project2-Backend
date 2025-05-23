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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;

@Service
public class CartServiceImpl implements CartService {
    private static final Logger logger = LoggerFactory.getLogger(CartServiceImpl.class);
    private static final int MAX_TOTAL_ITEMS = 500000000;
    private static final long MAX_TOTAL_VALUE = 2000000000; 

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private BookRepository bookRepository;

    private void validateCartLimits(String userId, int newQuantity, long newItemPrice) {
        // Tính tổng số lượng hiện tại trong giỏ hàng
        int currentTotalItems = cartItemRepository.findByUserId(userId).stream()
            .mapToInt(CartItem::getQuantity)
            .sum();

        // Tính tổng giá trị hiện tại của giỏ hàng
        long currentTotalValue = cartItemRepository.findByUserId(userId).stream()
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
        logger.info("Bắt đầu thêm sách vào giỏ hàng. UserId: {}, BookId: {}, Quantity: {}", 
            userId, cartItemDTO.getBookId(), cartItemDTO.getQuantity());

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

        // Kiểm tra giới hạn giỏ hàng
        validateCartLimits(userId, cartItemDTO.getQuantity(), book.getPrice());

        // Kiểm tra xem sách đã có trong giỏ hàng chưa
        Optional<CartItem> existingItem = cartItemRepository.findByUserIdAndBookId(userId, cartItemDTO.getBookId());
        
        try {
            if (existingItem.isPresent()) {
                // Nếu đã có, cập nhật số lượng
                CartItem item = existingItem.get();
                int newQuantity = item.getQuantity() + cartItemDTO.getQuantity();
                
                // Kiểm tra lại số lượng tổng
                if (book.getQuantity() < newQuantity) {
                    throw new BadRequestException(
                        String.format("Số lượng sách trong kho không đủ. Còn lại: %d", book.getQuantity())
                    );
                }
                
                item.setQuantity(newQuantity);
                item.setTotalPrice(item.getPrice() * newQuantity);
                CartItem savedItem = cartItemRepository.save(item);
                logger.info("Cập nhật số lượng sách trong giỏ hàng thành công. CartItemId: {}", savedItem.getId());
                return savedItem;
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
                
                CartItem savedItem = cartItemRepository.save(newItem);
                logger.info("Thêm sách mới vào giỏ hàng thành công. CartItemId: {}", savedItem.getId());
                return savedItem;
            }
        } catch (Exception e) {
            logger.error("Lỗi khi thêm sách vào giỏ hàng: {}", e.getMessage(), e);
            throw new BadRequestException("Không thể thêm sách vào giỏ hàng: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public CartItem updateCartItem(String userId, CartItemDTO cartItemDTO) {
        logger.info("Bắt đầu cập nhật giỏ hàng. UserId: {}, BookId: {}, Quantity: {}", 
            userId, cartItemDTO.getBookId(), cartItemDTO.getQuantity());

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
        CartItem item = cartItemRepository.findByUserIdAndBookId(userId, cartItemDTO.getBookId())
            .orElseThrow(() -> new BadRequestException("Không tìm thấy sách trong giỏ hàng"));

        // Kiểm tra giới hạn giỏ hàng (trừ đi số lượng cũ và cộng số lượng mới)
        validateCartLimits(userId, cartItemDTO.getQuantity() - item.getQuantity(), book.getPrice());

        try {
            // Kiểm tra giá sách có thay đổi không
            if (book.getPrice() != item.getPrice()) {
                logger.warn("Giá sách đã thay đổi. BookId: {}, OldPrice: {}, NewPrice: {}", 
                    book.getId(), item.getPrice(), book.getPrice());
                item.setPrice(book.getPrice());
            }

            // Cập nhật số lượng và tổng tiền
            item.setQuantity(cartItemDTO.getQuantity());
            item.setTotalPrice(item.getPrice() * cartItemDTO.getQuantity());
            
            CartItem savedItem = cartItemRepository.save(item);
            logger.info("Cập nhật giỏ hàng thành công. CartItemId: {}", savedItem.getId());
            return savedItem;
        } catch (Exception e) {
            logger.error("Lỗi khi cập nhật giỏ hàng: {}", e.getMessage(), e);
            throw new BadRequestException("Không thể cập nhật giỏ hàng: " + e.getMessage());
        }
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
        try {
            logger.info("Bắt đầu lấy danh sách giỏ hàng cho user: {}", userId);
            
            List<CartItem> cartItems = cartItemRepository.findByUserId(userId);
            if (cartItems.isEmpty()) {
                logger.info("Giỏ hàng trống cho user: {}", userId);
                return new ArrayList<>();
            }

            List<CartItemDetailDTO> result = new ArrayList<>();
            for (CartItem item : cartItems) {
                try {
                    Optional<Book> bookOpt = bookRepository.findById(item.getBookId());
                    if (bookOpt.isPresent()) {
                        Book book = bookOpt.get();
                        CartItemDetailDTO dto = CartItemDetailDTO.fromCartItem(
                            item,
                            book.getQuantity(),
                            book.getAuthor(),
                            book.getCategoryId()
                        );
                        result.add(dto);
                    } else {
                        logger.warn("Sách không tồn tại trong database. BookId: {}", item.getBookId());
                        // Thêm item vào kết quả với thông tin sách mặc định
                        CartItemDetailDTO dto = CartItemDetailDTO.fromCartItem(
                            item,
                            0,  // Số lượng có sẵn = 0 vì sách không tồn tại
                            "Sách không còn tồn tại",
                            "Không xác định"
                        );
                        result.add(dto);
                    }
                } catch (Exception e) {
                    logger.error("Lỗi khi xử lý item trong giỏ hàng. ItemId: {}, Error: {}", 
                        item.getId(), e.getMessage(), e);
                    // Bỏ qua item lỗi và tiếp tục với item tiếp theo
                    continue;
                }
            }

            logger.info("Lấy danh sách giỏ hàng thành công. Số lượng items: {}", result.size());
            return result;
        } catch (Exception e) {
            logger.error("Lỗi khi lấy danh sách giỏ hàng. UserId: {}, Error: {}", 
                userId, e.getMessage(), e);
            throw new BadRequestException("Không thể lấy danh sách giỏ hàng: " + e.getMessage());
        }
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
            book.getCategoryId()
        );
    }
} 