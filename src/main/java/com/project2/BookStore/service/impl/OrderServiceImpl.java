package com.project2.BookStore.service.impl;

import com.project2.BookStore.dto.BookDTO;
import com.project2.BookStore.dto.ImageDTO;
import com.project2.BookStore.dto.OrderItemWithBookDTO;
import com.project2.BookStore.dto.OrderRequestDTO;
import com.project2.BookStore.dto.OrderResponseDTO;
import com.project2.BookStore.dto.OrderWithDetailsDTO;
import com.project2.BookStore.dto.BuyNowRequestDTO;
import com.project2.BookStore.exception.BadRequestException;
import com.project2.BookStore.exception.OrderException;
import com.project2.BookStore.exception.ResourceNotFoundException;
import com.project2.BookStore.model.Book;
import com.project2.BookStore.model.CartItem;
import com.project2.BookStore.model.Order;
import com.project2.BookStore.model.OrderItem;
import com.project2.BookStore.model.User;
import com.project2.BookStore.repository.BookRepository;
import com.project2.BookStore.repository.CartItemRepository;
import com.project2.BookStore.repository.OrderItemRepository;
import com.project2.BookStore.repository.OrderRepository;
import com.project2.BookStore.repository.UserRepository;
import com.project2.BookStore.service.CartService;
import com.project2.BookStore.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartItemRepository cartItemRepository;
    @Autowired
    private CartService cartService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void updateCartItems(String userId, List<OrderItem> orderItems) {
        log.info("Bắt đầu cập nhật giỏ hàng sau khi tạo đơn hàng. UserId: {}", userId);
        try {
            // Get all cart items for this user
            List<CartItem> userCartItems = cartItemRepository.findByUser_Id(userId);
            log.info("Tìm thấy {} sản phẩm trong giỏ hàng của user {}", userCartItems.size(), userId);

            // Create a map of book IDs to cart items for quick lookup
            Map<String, CartItem> cartItemMap = userCartItems.stream()
                .collect(Collectors.toMap(item -> item.getBook().getId(), item -> item));

            // Process each order item
            for (OrderItem orderItem : orderItems) {
                String bookId = orderItem.getBook().getId();
                int purchasedQuantity = orderItem.getQuantity();

                CartItem cartItem = cartItemMap.get(bookId);
                if (cartItem == null) {
                    log.warn("Không tìm thấy sách {} trong giỏ hàng của user {}", bookId, userId);
                    continue;
                }

                int remainingQuantity = cartItem.getQuantity() - purchasedQuantity;
                if (remainingQuantity <= 0) {
                    cartItemRepository.delete(cartItem);
                    log.info("Đã xóa sách khỏi giỏ hàng vì đã mua hết. UserId: {}, BookId: {}", userId, bookId);
                } else {
                    cartItem.setQuantity(remainingQuantity);
                    cartItem.setTotalPrice(orderItem.getBook().getPrice() * remainingQuantity);
                    cartItemRepository.save(cartItem);
                    log.info("Đã cập nhật số lượng sách trong giỏ hàng. UserId: {}, BookId: {}, Số lượng còn lại: {}", 
                        userId, bookId, remainingQuantity);
                }
            }
            log.info("Cập nhật giỏ hàng thành công");
        } catch (Exception e) {
            log.error("Lỗi khi cập nhật giỏ hàng: {}", e.getMessage());
            throw new BadRequestException("Không thể cập nhật giỏ hàng: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public OrderResponseDTO createOrder(OrderRequestDTO request, String userId) {
        log.info("Bắt đầu tạo đơn hàng mới. UserId: {}", userId);
        Order order = null;
        try {
            // Validate request
            if (request == null) {
                throw new BadRequestException("Dữ liệu đơn hàng không được trống");
            }
            
            if (request.getItems() == null || request.getItems().isEmpty()) {
                throw new BadRequestException("Danh sách sản phẩm không được trống");
            }

            // Validate user info
            if (request.getFullName() == null || request.getFullName().trim().isEmpty()) {
                throw new BadRequestException("Họ tên người nhận không được trống");
            }
            if (request.getPhone() == null || request.getPhone().trim().isEmpty()) {
                throw new BadRequestException("Số điện thoại không được trống");
            }
            if (request.getAddress() == null || request.getAddress().trim().isEmpty()) {
                throw new BadRequestException("Địa chỉ không được trống");
            }
            if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                throw new BadRequestException("Email không được trống");
            }
            if (request.getPaymentMethod() == null || request.getPaymentMethod().trim().isEmpty()) {
                throw new BadRequestException("Phương thức thanh toán không được trống");
            }

            // Get user
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy người dùng"));

            // Validate email matches user's email
            if (!user.getEmail().equals(request.getEmail().trim())) {
                throw new BadRequestException("Email không khớp với tài khoản");
            }

            // Create order
            order = new Order();
            order.setUser(user);
            order.setFullName(request.getFullName().trim());
            order.setPhone(request.getPhone().trim());
            order.setAddress(request.getAddress().trim());
            order.setEmail(request.getEmail().trim());
            order.setStatus(Order.OrderStatus.PENDING);
            
            try {
                order.setPaymentMethod(Order.PaymentMethod.valueOf(request.getPaymentMethod().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Phương thức thanh toán không hợp lệ");
            }
            
            order.setPaymentStatus(Order.PaymentStatus.PENDING);

            // Process order items
            List<OrderItem> orderItems = new ArrayList<>();
            double totalAmount = 0;

            for (OrderRequestDTO.OrderItemRequestDTO itemRequest : request.getItems()) {
                if (itemRequest.getBookId() == null || itemRequest.getBookId().trim().isEmpty()) {
                    throw new BadRequestException("ID sách không được trống");
                }

                if (itemRequest.getQuantity() == null || itemRequest.getQuantity().trim().isEmpty()) {
                    throw new BadRequestException("Số lượng sách không được trống");
                }

                int quantity;
                try {
                    quantity = Integer.parseInt(itemRequest.getQuantity().trim());
                    if (quantity <= 0) {
                        throw new BadRequestException("Số lượng sách phải lớn hơn 0");
                    }
                } catch (NumberFormatException e) {
                    throw new BadRequestException("Số lượng sách không hợp lệ");
                }

                String bookId = itemRequest.getBookId().trim();
                Book book = bookRepository.findById(bookId)
                    .orElseThrow(() -> new BadRequestException("Không tìm thấy sách với ID: " + bookId));

                if (book.getQuantity() < quantity) {
                    throw new BadRequestException("Số lượng sách " + book.getMainText() + " trong kho không đủ");
                }

                // Kiểm tra xem sách có trong giỏ hàng không (không bắt buộc)
                CartItem cartItem = cartItemRepository.findByUser_IdAndBook_Id(userId, bookId).orElse(null);
                if (cartItem != null) {
                    // Nếu có trong giỏ hàng, kiểm tra số lượng
                    if (cartItem.getQuantity() < quantity) {
                        throw new BadRequestException("Số lượng sách " + book.getMainText() + " trong giỏ hàng không đủ");
                    }
                }

                OrderItem orderItem = new OrderItem();
                orderItem.setOrder(order);
                orderItem.setBook(book);
                orderItem.setQuantity(quantity);
                orderItem.setPrice(book.getPrice());
                orderItem.setSubtotal(book.getPrice() * quantity);

                // Update book quantity
                book.setQuantity(book.getQuantity() - quantity);
                book.setSold(book.getSold() + quantity);
                bookRepository.save(book);

                orderItems.add(orderItem);
                totalAmount += orderItem.getSubtotal();
            }

            order.setTotalAmount(totalAmount);
            order.setOrderItems(orderItems);
            
            // Save order
            order = orderRepository.save(order);
            log.info("Đơn hàng được tạo thành công. OrderId: {}", order.getId());

            // Update cart items in a separate transaction
            try {
                updateCartItems(userId, orderItems);
            } catch (Exception e) {
                log.error("Lỗi khi cập nhật giỏ hàng, nhưng đơn hàng đã được tạo thành công: {}", e.getMessage());
                // Don't throw the error since the order was created successfully
            }
            
            return convertToOrderResponseDTO(order);
        } catch (BadRequestException e) {
            log.warn("Lỗi khi tạo đơn hàng: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Lỗi không mong muốn khi tạo đơn hàng: {}", e.getMessage(), e);
            throw new BadRequestException("Không thể tạo đơn hàng: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public OrderResponseDTO buyNow(BuyNowRequestDTO request, String userId) {
        log.info("Bắt đầu mua ngay sách. UserId: {}, BookId: {}", userId, request.getBookId());
        Order order = null;
        try {
            // Validate request
            if (request == null) {
                throw new BadRequestException("Dữ liệu mua ngay không được trống");
            }

            // Validate user info
            if (request.getFullName() == null || request.getFullName().trim().isEmpty()) {
                throw new BadRequestException("Họ tên người nhận không được trống");
            }
            if (request.getPhone() == null || request.getPhone().trim().isEmpty()) {
                throw new BadRequestException("Số điện thoại không được trống");
            }
            if (request.getAddress() == null || request.getAddress().trim().isEmpty()) {
                throw new BadRequestException("Địa chỉ không được trống");
            }
            if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                throw new BadRequestException("Email không được trống");
            }
            if (request.getPaymentMethod() == null || request.getPaymentMethod().trim().isEmpty()) {
                throw new BadRequestException("Phương thức thanh toán không được trống");
            }

            // Get user
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy người dùng"));

            // Validate email matches user's email
            if (!user.getEmail().equals(request.getEmail().trim())) {
                throw new BadRequestException("Email không khớp với tài khoản");
            }

            // Validate book
            if (request.getBookId() == null || request.getBookId().trim().isEmpty()) {
                throw new BadRequestException("ID sách không được trống");
            }

            if (request.getQuantity() == null || request.getQuantity() <= 0) {
                throw new BadRequestException("Số lượng sách phải lớn hơn 0");
            }

            String bookId = request.getBookId().trim();
            Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy sách với ID: " + bookId));

            if (book.getQuantity() < request.getQuantity()) {
                throw new BadRequestException("Số lượng sách " + book.getMainText() + " trong kho không đủ");
            }

            // Create order
            order = new Order();
            order.setUser(user);
            order.setFullName(request.getFullName().trim());
            order.setPhone(request.getPhone().trim());
            order.setAddress(request.getAddress().trim());
            order.setEmail(request.getEmail().trim());
            order.setStatus(Order.OrderStatus.PENDING);
            
            try {
                order.setPaymentMethod(Order.PaymentMethod.valueOf(request.getPaymentMethod().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Phương thức thanh toán không hợp lệ");
            }
            
            order.setPaymentStatus(Order.PaymentStatus.PENDING);

            // Create order item
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setBook(book);
            orderItem.setQuantity(request.getQuantity());
            orderItem.setPrice(book.getPrice());
            orderItem.setSubtotal(book.getPrice() * request.getQuantity());

            // Update book quantity
            book.setQuantity(book.getQuantity() - request.getQuantity());
            book.setSold(book.getSold() + request.getQuantity());
            bookRepository.save(book);

            List<OrderItem> orderItems = new ArrayList<>();
            orderItems.add(orderItem);
            double totalAmount = orderItem.getSubtotal();

            order.setTotalAmount(totalAmount);
            order.setOrderItems(orderItems);
            
            // Save order
            order = orderRepository.save(order);
            log.info("Đơn hàng mua ngay được tạo thành công. OrderId: {}", order.getId());
            
            return convertToOrderResponseDTO(order);
        } catch (BadRequestException e) {
            log.warn("Lỗi khi mua ngay: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Lỗi không mong muốn khi mua ngay: {}", e.getMessage(), e);
            throw new BadRequestException("Không thể mua ngay: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponseDTO getOrderById(String orderId) {
        log.info("Lấy thông tin đơn hàng: {}", orderId);
        try {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng với ID: " + orderId));
            return convertToOrderResponseDTO(order);
        } catch (ResourceNotFoundException e) {
            log.warn("Không tìm thấy đơn hàng: {}", orderId);
            throw e;
        } catch (Exception e) {
            log.error("Lỗi khi lấy thông tin đơn hàng {}: {}", orderId, e.getMessage(), e);
            throw new BadRequestException("Không thể lấy thông tin đơn hàng: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponseDTO> getAllOrders(Pageable pageable) {
        log.info("Lấy danh sách tất cả đơn hàng");
        try {
        Page<Order> orderPage = orderRepository.findAll(pageable);
            return orderPage.map(this::convertToOrderResponseDTO);
        } catch (Exception e) {
            log.error("Lỗi khi lấy danh sách đơn hàng: {}", e.getMessage(), e);
            throw new BadRequestException("Không thể lấy danh sách đơn hàng: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public OrderResponseDTO updateOrderStatus(String orderId, Order.OrderStatus status) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng với ID: " + orderId));

        if (order.getStatus() == Order.OrderStatus.CANCELLED) {
            throw new BadRequestException("Không thể cập nhật đơn hàng đã hủy");
        }

        order.setStatus(status);
        order.setUpdatedAt(LocalDateTime.now());
        if (status == Order.OrderStatus.DELIVERED) {
            order.setPaymentStatus(Order.PaymentStatus.PAID);
        }

        Order updatedOrder = orderRepository.save(order);
        return convertToOrderResponseDTO(updatedOrder);
    }

    @Override
    @Transactional
    public OrderResponseDTO cancelOrder(String orderId) {
        log.info("Bắt đầu hủy đơn hàng: {}", orderId);
        try {
            Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy đơn hàng"));

            // Kiểm tra trạng thái đơn hàng
            if (order.getStatus() == Order.OrderStatus.CANCELLED) {
                throw new BadRequestException("Đơn hàng đã được hủy trước đó");
            }
            if (order.getStatus() == Order.OrderStatus.DELIVERED) {
                throw new BadRequestException("Không thể hủy đơn hàng đã giao thành công");
            }

            // Cập nhật trạng thái đơn hàng
            order.setStatus(Order.OrderStatus.CANCELLED);
            order.setPaymentStatus(Order.PaymentStatus.FAILED);
            order.setUpdatedAt(LocalDateTime.now());
            order = orderRepository.save(order);

            // Hoàn trả số lượng sách vào kho
            for (OrderItem item : order.getOrderItems()) {
                Book book = item.getBook();
                if (book != null) {
                    book.setQuantity(book.getQuantity() + item.getQuantity());
                    book.setSold(book.getSold() - item.getQuantity());
                    bookRepository.save(book);
                    log.info("Đã hoàn trả số lượng sách. BookId: {}, Số lượng: {}", 
                        book.getId(), item.getQuantity());
                }
            }

            log.info("Đơn hàng {} đã được hủy thành công", orderId);
            return convertToOrderResponseDTO(order);
        } catch (BadRequestException e) {
            log.warn("Lỗi khi hủy đơn hàng {}: {}", orderId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Lỗi không mong muốn khi hủy đơn hàng {}: {}", orderId, e.getMessage(), e);
            throw new BadRequestException("Không thể hủy đơn hàng: " + e.getMessage());
        }
    }

    @Override
    public Page<OrderResponseDTO> getAllOrdersForAdmin(Pageable pageable, Order.OrderStatus status, String search) {
        log.info("Bắt đầu tìm kiếm đơn hàng. Status: {}, Search: {}", status, search);
        try {
            Page<Order> orderPage = orderRepository.findByStatusAndSearch(status, search, pageable);
            List<OrderResponseDTO> orderDTOs = orderPage.getContent().stream()
                .map(order -> convertToOrderResponseDTO(order))
                .collect(Collectors.toList());

            log.info("Tìm kiếm đơn hàng thành công. Tổng số: {}", orderPage.getTotalElements());
            return new PageImpl<>(orderDTOs, pageable, orderPage.getTotalElements());
        } catch (Exception e) {
            log.error("Lỗi khi tìm kiếm đơn hàng: {}", e.getMessage());
            throw new BadRequestException("Không thể tìm kiếm đơn hàng: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponseDTO> getOrdersByUser(String userId, Pageable pageable) {
        log.info("Lấy danh sách đơn hàng của user: {}", userId);
        try {
        Page<Order> orderPage = orderRepository.findByUserId(userId, pageable);
            return orderPage.map(this::convertToOrderResponseDTO);
        } catch (Exception e) {
            log.error("Lỗi khi lấy danh sách đơn hàng của user {}: {}", userId, e.getMessage(), e);
            throw new BadRequestException("Không thể lấy danh sách đơn hàng: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponseDTO> getUserOrders(String userId, Pageable pageable) {
        log.info("Lấy danh sách đơn hàng của user: {}", userId);
        try {
            if (userId == null || userId.trim().isEmpty()) {
                throw new BadRequestException("ID người dùng không được trống");
            }

            // Validate user exists
            if (!userRepository.existsById(userId)) {
                throw new BadRequestException("Không tìm thấy người dùng");
            }

            Page<Order> ordersPage = orderRepository.findByUserId(userId, pageable);
            if (ordersPage == null || ordersPage.getContent() == null) {
                log.warn("Không tìm thấy đơn hàng nào cho user: {}", userId);
                return Page.empty(pageable);
            }

            List<OrderResponseDTO> orderDTOs = ordersPage.getContent().stream()
                .map(order -> {
                    try {
                        return convertToOrderResponseDTO(order);
                    } catch (Exception e) {
                        log.error("Lỗi khi chuyển đổi đơn hàng {}: {}", order.getId(), e.getMessage());
                        return null;
                    }
                })
                .filter(dto -> dto != null)
            .collect(Collectors.toList());

            return new PageImpl<>(orderDTOs, pageable, ordersPage.getTotalElements());
        } catch (BadRequestException e) {
            log.warn("Lỗi khi lấy danh sách đơn hàng: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Lỗi không mong muốn khi lấy danh sách đơn hàng: {}", e.getMessage(), e);
            throw new BadRequestException("Không thể lấy danh sách đơn hàng: " + e.getMessage());
        }
    }

    @Override
    public Page<OrderWithDetailsDTO> getOrdersPaged(Pageable pageable) {
        try {
            Page<Order> orderPage = orderRepository.findAll(pageable);
            List<OrderWithDetailsDTO> orderDTOs = orderPage.getContent().stream()
                .map(order -> {
                    List<OrderItemWithBookDTO> orderItemDTOs = order.getOrderItems().stream()
                        .map(item -> {
                            Book book = item.getBook();
                            BookDTO bookDTO = null;
                            if (book != null) {
                                Book.Image bookImage = book.getImage();
                                ImageDTO imageDTO = null;
                                if (bookImage != null) {
                                    imageDTO = new ImageDTO(
                                        bookImage.getThumbnail(),
                                        bookImage.getMedium(),
                                        bookImage.getOriginal(),
                                        bookImage.getFormat(),
                                        bookImage.getSize()
                                    );
                                }
                                
                                bookDTO = new BookDTO(
                                    book.getId(),
                                    imageDTO,
                                    book.getMainText(),
                                    book.getAuthor(),
                                    book.getPrice(),
                                    book.getSold(),
                                    book.getQuantity(),
                                    book.getCategoryId()
                                );
                            }
                            return new OrderItemWithBookDTO(
                                item.getId(),
                                bookDTO,
                                item.getQuantity(),
                                item.getPrice()
                            );
                        })
                        .collect(Collectors.toList());

                    return new OrderWithDetailsDTO(
                        order.getId(),
                        order.getUser().getId(),
                        order.getFullName(),
                        order.getEmail(),
                        order.getPhone(),
                        order.getAddress(),
                        orderItemDTOs,
                        order.getTotalAmount(),
                        order.getStatus().name(),
                        order.getCreatedAt(),
                        order.getUpdatedAt()
                    );
                })
                .collect(Collectors.toList());

            return new PageImpl<>(orderDTOs, pageable, orderPage.getTotalElements());
        } catch (Exception e) {
            log.error("Lỗi khi lấy danh sách đơn hàng: {}", e.getMessage());
            throw new OrderException("Lỗi khi lấy danh sách đơn hàng: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public OrderResponseDTO deleteOrder(String orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new BadRequestException("Không tìm thấy đơn hàng"));
        
        // Kiểm tra trạng thái đơn hàng
        if (order.getStatus() != Order.OrderStatus.PENDING) {
            throw new BadRequestException("Chỉ có thể xóa đơn hàng ở trạng thái chờ xử lý");
        }

        // Lưu thông tin đơn hàng trước khi xóa
        OrderResponseDTO deletedOrder = convertToOrderResponseDTO(order);

        // Xóa đơn hàng
        orderRepository.delete(order);

        return deletedOrder;
    }

    @Override
    public boolean hasActiveOrders(String userId) {
        log.info("Kiểm tra đơn hàng đang xử lý của user: {}", userId);
        try {
            // Kiểm tra các đơn hàng có trạng thái khác PENDING và CANCELLED
            long activeOrdersCount = orderRepository.countByUserIdAndStatusNotIn(
                userId, 
                Arrays.asList(Order.OrderStatus.PENDING, Order.OrderStatus.CANCELLED)
            );
            boolean hasActive = activeOrdersCount > 0;
            log.info("User {} {} đơn hàng đang xử lý", userId, hasActive ? "có" : "không có");
            return hasActive;
        } catch (Exception e) {
            log.error("Lỗi khi kiểm tra đơn hàng của user {}: {}", userId, e.getMessage());
            throw new BadRequestException("Không thể kiểm tra đơn hàng của người dùng: " + e.getMessage());
        }
    }

    private OrderResponseDTO convertToOrderResponseDTO(Order order) {
        try {
            if (order == null) {
                throw new BadRequestException("Đơn hàng không được null");
            }

        OrderResponseDTO dto = new OrderResponseDTO();
        dto.setId(order.getId());
            dto.setUserId(order.getUser() != null ? order.getUser().getId() : null);
        dto.setFullName(order.getFullName());
        dto.setPhone(order.getPhone());
        dto.setAddress(order.getAddress());
        dto.setEmail(order.getEmail());
        dto.setStatus(order.getStatus());
        dto.setTotalAmount(order.getTotalAmount());
            dto.setPaymentMethod(order.getPaymentMethod() != null ? order.getPaymentMethod().name() : null);
            dto.setPaymentStatus(order.getPaymentStatus() != null ? order.getPaymentStatus().name() : null);
        dto.setCreatedAt(order.getCreatedAt());
        dto.setUpdatedAt(order.getUpdatedAt());

            if (order.getOrderItems() != null) {
                dto.setItems(order.getOrderItems().stream()
                .map(item -> {
                        try {
                            if (item == null) {
                                return null;
                            }

                    OrderResponseDTO.OrderItemResponseDTO itemDTO = new OrderResponseDTO.OrderItemResponseDTO();
                    itemDTO.setId(item.getId());
                    
                            if (item.getBook() != null) {
                                itemDTO.setBookId(item.getBook().getId());
                                itemDTO.setBookTitle(item.getBook().getMainText());
                                if (item.getBook().getImage() != null) {
                                    itemDTO.setBookImage(item.getBook().getImage().getMedium());
                        }
                    }
                    
                    itemDTO.setQuantity(item.getQuantity());
                    itemDTO.setPrice(item.getPrice());
                            itemDTO.setSubtotal(item.getPrice() * item.getQuantity());
                    return itemDTO;
                        } catch (Exception e) {
                            log.error("Lỗi khi chuyển đổi chi tiết đơn hàng: {}", e.getMessage(), e);
                            return null;
                        }
                    })
                    .filter(item -> item != null)
                    .collect(Collectors.toList()));
            } else {
                dto.setItems(new ArrayList<>());
            }
            
        return dto;
        } catch (Exception e) {
            log.error("Lỗi khi chuyển đổi đơn hàng sang DTO: {}", e.getMessage(), e);
            throw new OrderException("Lỗi khi chuyển đổi đơn hàng: " + e.getMessage());
        }
    }
}
