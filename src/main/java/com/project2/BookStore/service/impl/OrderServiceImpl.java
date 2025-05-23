package com.project2.BookStore.service.impl;

import com.project2.BookStore.dto.OrderRequestDTO;
import com.project2.BookStore.dto.OrderResponseDTO;
import com.project2.BookStore.exception.BadRequestException;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final OrderItemRepository orderItemRepository;
    private final MongoTemplate mongoTemplate;

    @Override
    @Transactional
    public OrderResponseDTO createOrder(OrderRequestDTO request, String userId) {
        // Validate request
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BadRequestException("Danh sách sản phẩm không được trống");
        }

        // Create order
        Order order = new Order();
        order.setUserId(userId);
        order.setFullName(request.getFullName());
        order.setPhone(request.getPhone());
        order.setAddress(request.getAddress());
        order.setEmail(request.getEmail());
        order.setStatus(Order.OrderStatus.PENDING);
        order.setPaymentMethod(Order.PaymentMethod.valueOf(request.getPaymentMethod()));
        order.setPaymentStatus(Order.PaymentStatus.PENDING);
        
        // Set timestamps
        LocalDateTime now = LocalDateTime.now();
        order.setCreatedAt(now);
        order.setUpdatedAt(now);

        // Process order items
        List<OrderItem> orderItems = new ArrayList<>();
        List<String> itemIds = new ArrayList<>();
        double totalAmount = 0;

        for (OrderRequestDTO.OrderItemRequestDTO itemRequest : request.getItems()) {
            Book book = bookRepository.findById(itemRequest.getBookId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sách với id: " + itemRequest.getBookId()));

            // Check stock
            if (book.getQuantity() < itemRequest.getQuantity()) {
                throw new BadRequestException("Số lượng sách trong kho không đủ");
            }

            // Create order item
            OrderItem orderItem = new OrderItem();
            orderItem.setBook(book);
            orderItem.setQuantity(itemRequest.getQuantity());
            orderItem.setPrice(Double.valueOf(book.getPrice()));
            orderItem.setSubtotal(Double.valueOf(book.getPrice() * itemRequest.getQuantity()));
            orderItem = orderItemRepository.save(orderItem);
            orderItems.add(orderItem);
            itemIds.add(orderItem.getId());

            // Update book stock
            book.setQuantity(book.getQuantity() - itemRequest.getQuantity());
            bookRepository.save(book);

            totalAmount += orderItem.getSubtotal();
        }

        order.setItemIds(itemIds);
        order.setTotalAmount(totalAmount);
        order = orderRepository.save(order);

        return convertToDTO(order, orderItems);
    }

    @Override
    public OrderResponseDTO getOrderById(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng với id: " + orderId));
        
        List<OrderItem> orderItems = orderItemRepository.findAllById(order.getItemIds());
        return convertToDTO(order, orderItems);
    }

    @Override
    public Page<OrderResponseDTO> getAllOrders(Pageable pageable) {
        return orderRepository.findAll(pageable)
                .map(order -> {
                    List<OrderItem> orderItems = orderItemRepository.findAllById(order.getItemIds());
                    return convertToDTO(order, orderItems);
                });
    }

    @Override
    @Transactional
    public OrderResponseDTO updateOrderStatus(String orderId, Order.OrderStatus status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng với id: " + orderId));

        if (order.getStatus() == Order.OrderStatus.CANCELLED) {
            throw new BadRequestException("Không thể cập nhật đơn hàng đã hủy");
        }

        order.setStatus(status);
        order.setUpdatedAt(LocalDateTime.now());
        if (status == Order.OrderStatus.DELIVERED) {
            order.setPaymentStatus(Order.PaymentStatus.PAID);
        }

        order = orderRepository.save(order);
        List<OrderItem> orderItems = orderItemRepository.findAllById(order.getItemIds());
        return convertToDTO(order, orderItems);
    }

    @Override
    @Transactional
    public void cancelOrder(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng với id: " + orderId));

        if (order.getStatus() != Order.OrderStatus.PENDING) {
            throw new BadRequestException("Chỉ có thể hủy đơn hàng đang ở trạng thái chờ xác nhận");
        }

        order.setStatus(Order.OrderStatus.CANCELLED);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);

        // Hoàn trả số lượng sách vào kho
        List<OrderItem> orderItems = orderItemRepository.findAllById(order.getItemIds());
        for (OrderItem item : orderItems) {
            Book book = item.getBook();
            book.setQuantity(book.getQuantity() + item.getQuantity());
            bookRepository.save(book);
        }
    }

    @Override
    public Page<OrderResponseDTO> getAllOrdersForAdmin(Pageable pageable, Order.OrderStatus status, String search) {
        // Tạo query để tìm kiếm đơn hàng
        Query query = new Query();
        
        // Thêm điều kiện tìm kiếm theo trạng thái nếu có
        if (status != null) {
            query.addCriteria(Criteria.where("status").is(status));
        }
        
        // Thêm điều kiện tìm kiếm theo từ khóa nếu có
        if (search != null && !search.trim().isEmpty()) {
            String searchRegex = ".*" + search.trim() + ".*";
            query.addCriteria(new Criteria().orOperator(
                Criteria.where("fullName").regex(searchRegex, "i"),
                Criteria.where("email").regex(searchRegex, "i"),
                Criteria.where("phone").regex(searchRegex, "i"),
                Criteria.where("address").regex(searchRegex, "i")
            ));
        }
        
        // Thêm phân trang
        query.with(pageable);
        
        // Thực hiện truy vấn
        List<Order> orders = mongoTemplate.find(query, Order.class);
        long total = mongoTemplate.count(query, Order.class);
        
        // Chuyển đổi kết quả sang DTO
        List<OrderResponseDTO> orderDTOs = orders.stream()
            .map(order -> {
                List<OrderItem> orderItems = orderItemRepository.findAllById(order.getItemIds());
                return convertToDTO(order, orderItems);
            })
            .collect(Collectors.toList());
        
        // Tạo Page object với kết quả
        return new PageImpl<>(orderDTOs, pageable, total);
    }

    @Override
    public Page<OrderResponseDTO> getOrdersByUser(String userId, Pageable pageable) {
        // Tạo query để tìm kiếm đơn hàng của user
        Query query = new Query(Criteria.where("userId").is(userId));
        query.with(pageable);
        
        // Thực hiện truy vấn
        List<Order> orders = mongoTemplate.find(query, Order.class);
        long total = mongoTemplate.count(query, Order.class);
        
        // Chuyển đổi kết quả sang DTO
        List<OrderResponseDTO> orderDTOs = orders.stream()
            .map(order -> {
                List<OrderItem> orderItems = orderItemRepository.findAllById(order.getItemIds());
                return convertToDTO(order, orderItems);
            })
            .collect(Collectors.toList());
        
        // Tạo Page object với kết quả
        return new PageImpl<>(orderDTOs, pageable, total);
    }

    private OrderResponseDTO convertToDTO(Order order, List<OrderItem> orderItems) {
        OrderResponseDTO dto = new OrderResponseDTO();
        dto.setId(order.getId());
        dto.setUserId(order.getUserId());
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
                    itemDTO.setBookId(item.getBook().getId());
                    itemDTO.setBookTitle(item.getBook().getName());
                    itemDTO.setBookImage(item.getBook().getImage().getMedium());
                    itemDTO.setQuantity(item.getQuantity());
                    itemDTO.setPrice(item.getPrice());
                    itemDTO.setSubtotal(item.getSubtotal());
                    return itemDTO;
                })
                .collect(Collectors.toList());

        dto.setItems(itemDTOs);
        return dto;
    }
}
