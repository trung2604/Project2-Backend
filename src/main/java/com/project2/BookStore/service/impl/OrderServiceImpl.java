package com.project2.BookStore.service.impl;

import com.project2.BookStore.dto.BookDTO;
import com.project2.BookStore.dto.ImageDTO;
import com.project2.BookStore.dto.OrderItemWithBookDTO;
import com.project2.BookStore.dto.OrderRequestDTO;
import com.project2.BookStore.dto.OrderResponseDTO;
import com.project2.BookStore.dto.OrderWithDetailsDTO;
import com.project2.BookStore.exception.BadRequestException;
import com.project2.BookStore.exception.OrderException;
import com.project2.BookStore.exception.ResourceNotFoundException;
import com.project2.BookStore.model.Book;
import com.project2.BookStore.model.Order;
import com.project2.BookStore.model.OrderItem;
import com.project2.BookStore.model.User;
import com.project2.BookStore.repository.BookRepository;
import com.project2.BookStore.repository.OrderItemRepository;
import com.project2.BookStore.repository.OrderRepository;
import com.project2.BookStore.repository.UserRepository;
import com.project2.BookStore.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final OrderItemRepository orderItemRepository;

    @Override
    @Transactional
    public OrderResponseDTO createOrder(OrderRequestDTO request, String userId) {
        log.info("Bắt đầu tạo đơn hàng mới. UserId: {}", userId);
        try {
            // Validate request
            if (request.getItems() == null || request.getItems().isEmpty()) {
                throw new BadRequestException("Danh sách sản phẩm không được trống");
            }

            // Get user
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy người dùng"));

            // Validate email matches user's email
            if (!user.getEmail().equals(request.getEmail())) {
                throw new BadRequestException("Email không khớp với tài khoản");
            }

            // Create order
            Order order = new Order();
            order.setUser(user);
            order.setFullName(request.getFullName().trim());
            order.setPhone(request.getPhone().trim());
            order.setAddress(request.getAddress().trim());
            order.setEmail(request.getEmail().trim());
            order.setStatus(Order.OrderStatus.PENDING);
            order.setPaymentMethod(Order.PaymentMethod.valueOf(request.getPaymentMethod()));
            order.setPaymentStatus(Order.PaymentStatus.PENDING);

            // Process order items
            List<OrderItem> orderItems = new ArrayList<>();
            double totalAmount = 0;

            for (OrderRequestDTO.OrderItemRequestDTO item : request.getItems()) {
                Book book = bookRepository.findById(item.getBookId())
                    .orElseThrow(() -> new BadRequestException("Không tìm thấy sách với ID: " + item.getBookId()));

                int quantity = Integer.parseInt(item.getQuantity());
                if (book.getQuantity() < quantity) {
                    throw new BadRequestException(
                        String.format("Số lượng sách '%s' trong kho không đủ. Còn lại: %d", 
                            book.getMainText(), book.getQuantity())
                    );
                }

                OrderItem orderItem = new OrderItem();
                orderItem.setOrder(order);
                orderItem.setBook(book);
                orderItem.setQuantity(quantity);
                orderItem.setPrice(Double.valueOf(book.getPrice()));
                orderItem.setSubtotal(Double.valueOf(book.getPrice() * quantity));

                orderItems.add(orderItem);
                totalAmount += orderItem.getSubtotal();

                // Update book quantity
                book.setQuantity(book.getQuantity() - quantity);
                book.setSold(book.getSold() + quantity);
                bookRepository.save(book);
                log.info("Đã cập nhật số lượng sách. BookId: {}, Số lượng còn lại: {}", 
                    book.getId(), book.getQuantity());
            }

            order.setOrderItems(orderItems);
            order.setTotalAmount(totalAmount);
            Order savedOrder = orderRepository.save(order);
            log.info("Đã tạo đơn hàng thành công. OrderId: {}", savedOrder.getId());

            // TODO: Send confirmation email
            // sendOrderConfirmationEmail(savedOrder);

            return convertToDTO(savedOrder, orderItems);
        } catch (NumberFormatException e) {
            log.error("Lỗi khi chuyển đổi số lượng: {}", e.getMessage());
            throw new BadRequestException("Số lượng không hợp lệ");
        } catch (BadRequestException e) {
            log.warn("Lỗi khi tạo đơn hàng: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Lỗi không mong muốn khi tạo đơn hàng: {}", e.getMessage());
            throw new BadRequestException("Không thể tạo đơn hàng: " + e.getMessage());
        }
    }

    @Override
    public OrderResponseDTO getOrderById(String orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng với ID: " + orderId));
        return convertToDTO(order, order.getOrderItems());
    }

    @Override
    public Page<OrderResponseDTO> getAllOrders(Pageable pageable) {
        Page<Order> orderPage = orderRepository.findAll(pageable);
        List<OrderResponseDTO> orderDTOs = orderPage.getContent().stream()
            .map(order -> convertToDTO(order, order.getOrderItems()))
            .collect(Collectors.toList());
        return new PageImpl<>(orderDTOs, pageable, orderPage.getTotalElements());
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
        return convertToDTO(updatedOrder, updatedOrder.getOrderItems());
    }

    @Override
    @Transactional
    public OrderResponseDTO cancelOrder(String orderId) {
        log.info("Hủy đơn hàng: {}", orderId);
        try {
            Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy đơn hàng"));

            // Cập nhật trạng thái đơn hàng
            order.setStatus(Order.OrderStatus.CANCELLED);
            order = orderRepository.save(order);

            // Hoàn trả số lượng sách vào kho
            for (OrderItem item : order.getOrderItems()) {
                Book book = item.getBook();
                if (book != null) {
                    book.setQuantity(book.getQuantity() + item.getQuantity());
                    book.setSold(book.getSold() - item.getQuantity());
                    bookRepository.save(book);
                }
            }

            log.info("Đơn hàng {} đã được hủy", orderId);
            return convertToOrderResponseDTO(order);
        } catch (Exception e) {
            log.error("Lỗi khi hủy đơn hàng {}: {}", orderId, e.getMessage());
            throw new BadRequestException("Không thể hủy đơn hàng: " + e.getMessage());
        }
    }

    @Override
    public Page<OrderResponseDTO> getAllOrdersForAdmin(Pageable pageable, Order.OrderStatus status, String search) {
        log.info("Bắt đầu tìm kiếm đơn hàng. Status: {}, Search: {}", status, search);
        try {
            Page<Order> orderPage = orderRepository.findByStatusAndSearch(status, search, pageable);
            List<OrderResponseDTO> orderDTOs = orderPage.getContent().stream()
                .map(order -> convertToDTO(order, order.getOrderItems()))
                .collect(Collectors.toList());

            log.info("Tìm kiếm đơn hàng thành công. Tổng số: {}", orderPage.getTotalElements());
            return new PageImpl<>(orderDTOs, pageable, orderPage.getTotalElements());
        } catch (Exception e) {
            log.error("Lỗi khi tìm kiếm đơn hàng: {}", e.getMessage());
            throw new BadRequestException("Không thể tìm kiếm đơn hàng: " + e.getMessage());
        }
    }

    @Override
    public Page<OrderResponseDTO> getOrdersByUser(String userId, Pageable pageable) {
        Page<Order> orderPage = orderRepository.findByUserId(userId, pageable);
        List<OrderResponseDTO> orderDTOs = orderPage.getContent().stream()
            .map(order -> convertToDTO(order, order.getOrderItems()))
            .collect(Collectors.toList());

        return new PageImpl<>(orderDTOs, pageable, orderPage.getTotalElements());
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
    public Page<OrderResponseDTO> getUserOrders(String userId, Pageable pageable) {
        Page<Order> ordersPage = orderRepository.findByUserId(userId, pageable);
        return ordersPage.map(this::convertToOrderResponseDTO);
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

    private OrderResponseDTO convertToDTO(Order order, List<OrderItem> orderItems) {
        OrderResponseDTO dto = new OrderResponseDTO();
        dto.setId(order.getId());
        dto.setUserId(order.getUser().getId());
        dto.setFullName(order.getFullName());
        dto.setPhone(order.getPhone());
        dto.setAddress(order.getAddress());
        dto.setEmail(order.getEmail());
        dto.setStatus(order.getStatus());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setPaymentMethod(order.getPaymentMethod().name());
        dto.setPaymentStatus(order.getPaymentStatus().name());
        dto.setCreatedAt(order.getCreatedAt());
        dto.setUpdatedAt(order.getUpdatedAt());

        List<OrderResponseDTO.OrderItemResponseDTO> itemDTOs = orderItems.stream()
                .map(item -> {
                    OrderResponseDTO.OrderItemResponseDTO itemDTO = new OrderResponseDTO.OrderItemResponseDTO();
                    itemDTO.setId(item.getId());
                    
                    Book book = item.getBook();
                    if (book != null) {
                        itemDTO.setBookId(book.getId());
                        itemDTO.setBookTitle(book.getMainText());
                        if (book.getImage() != null) {
                            itemDTO.setBookImage(book.getImage().getMedium());
                        }
                    }
                    
                    itemDTO.setQuantity(item.getQuantity());
                    itemDTO.setPrice(item.getPrice());
                    itemDTO.setSubtotal(item.getSubtotal());
                    return itemDTO;
                })
                .collect(Collectors.toList());

        dto.setItems(itemDTOs);
        return dto;
    }

    private OrderResponseDTO convertToOrderResponseDTO(Order order) {
        OrderResponseDTO dto = new OrderResponseDTO();
        dto.setId(order.getId());
        dto.setUserId(order.getUser().getId());
        dto.setFullName(order.getFullName());
        dto.setPhone(order.getPhone());
        dto.setAddress(order.getAddress());
        dto.setEmail(order.getEmail());
        dto.setStatus(order.getStatus());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setPaymentMethod(order.getPaymentMethod().name());
        dto.setPaymentStatus(order.getPaymentStatus().name());
        dto.setCreatedAt(order.getCreatedAt());
        dto.setUpdatedAt(order.getUpdatedAt());
        dto.setItems(order.getOrderItems().stream()
            .map(item -> {
                OrderResponseDTO.OrderItemResponseDTO itemDTO = new OrderResponseDTO.OrderItemResponseDTO();
                itemDTO.setId(item.getId());
                itemDTO.setBookId(item.getBook().getId());
                itemDTO.setBookTitle(item.getBook().getMainText());
                itemDTO.setBookImage(item.getBook().getImage().getOriginal());
                itemDTO.setQuantity(item.getQuantity());
                itemDTO.setPrice(item.getPrice());
                itemDTO.setSubtotal(item.getPrice() * item.getQuantity());
                return itemDTO;
            })
            .collect(Collectors.toList()));
        return dto;
    }
}
