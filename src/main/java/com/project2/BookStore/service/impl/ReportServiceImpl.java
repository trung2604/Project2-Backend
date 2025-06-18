package com.project2.BookStore.service.impl;

import com.project2.BookStore.dto.ReportDTO;
import com.project2.BookStore.model.*;
import com.project2.BookStore.repository.*;
import com.project2.BookStore.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final BookRepository bookRepository;
    private final ReviewRepository reviewRepository;

    @Override
    @Transactional(readOnly = true)
    public ReportDTO.RevenueReport getRevenueReport(LocalDateTime startDate, LocalDateTime endDate) {
        List<Order> orders = orderRepository.findAllByCreatedAtBetween(startDate, endDate).stream()
                .filter(order -> order.getStatus() == Order.OrderStatus.DELIVERED)
                .collect(Collectors.toList());
        
        double totalRevenue = orders.stream()
                .mapToDouble(Order::getTotalAmount)
                .sum();
        
        int totalOrders = orders.size();
        double averageOrderValue = totalOrders > 0 ? totalRevenue / totalOrders : 0;
        
        Map<LocalDate, List<Order>> ordersByDate = orders.stream()
                .collect(Collectors.groupingBy(order -> order.getCreatedAt().toLocalDate()));
        
        List<ReportDTO.DailyRevenue> dailyStats = ordersByDate.entrySet().stream()
                .map(entry -> {
                    LocalDate date = entry.getKey();
                    List<Order> dailyOrders = entry.getValue();
                    double dailyRevenue = dailyOrders.stream()
                            .mapToDouble(Order::getTotalAmount)
                            .sum();
                    int dailyOrderCount = dailyOrders.size();
                    double dailyAverage = dailyOrderCount > 0 ? dailyRevenue / dailyOrderCount : 0;
                    
                    return new ReportDTO.DailyRevenue(
                        date,
                        BigDecimal.valueOf(dailyRevenue),
                        dailyOrderCount,
                        BigDecimal.valueOf(dailyAverage)
                    );
                })
                .sorted(Comparator.comparing(ReportDTO.DailyRevenue::getDate))
                .collect(Collectors.toList());
        
        return new ReportDTO.RevenueReport(
            BigDecimal.valueOf(totalRevenue),
            totalOrders,
            BigDecimal.valueOf(averageOrderValue),
            dailyStats
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReportDTO.OrderStatusReport> getOrderStatusReport() {
        List<Order> orders = orderRepository.findAll();
        
        Map<Order.OrderStatus, List<Order>> ordersByStatus = orders.stream()
                .collect(Collectors.groupingBy(Order::getStatus));
        
        long totalOrders = orders.size();
        
        return ordersByStatus.entrySet().stream()
                .map(entry -> {
                    Order.OrderStatus status = entry.getKey();
                    List<Order> statusOrders = entry.getValue();
                    double totalAmount = statusOrders.stream()
                            .mapToDouble(Order::getTotalAmount)
                            .sum();
                    double percentage = totalOrders > 0 
                        ? (statusOrders.size() * 100.0) / totalOrders 
                        : 0;
                    
                    return new ReportDTO.OrderStatusReport(
                        status,
                        statusOrders.size(),
                        BigDecimal.valueOf(totalAmount),
                        percentage
                    );
                })
                .sorted(Comparator.comparing(ReportDTO.OrderStatusReport::getOrderCount).reversed())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReportDTO.TopSellingBook> getTopSellingBooks(LocalDateTime startDate, LocalDateTime endDate, int limit) {
        List<OrderItem> orderItems = orderItemRepository.findAllByOrderCreatedAtBetween(startDate, endDate).stream()
                .filter(item -> item.getOrder().getStatus() == Order.OrderStatus.DELIVERED)
                .collect(Collectors.toList());
        
        Map<Book, LongSummaryStatistics> bookStats = orderItems.stream()
                .collect(Collectors.groupingBy(
                    OrderItem::getBook,
                    Collectors.summarizingLong(OrderItem::getQuantity)
                ));
        
        return bookStats.entrySet().stream()
                .map(entry -> {
                    Book book = entry.getKey();
                    LongSummaryStatistics stats = entry.getValue();
                    long totalSold = stats.getSum();
                    double totalRevenue = totalSold * book.getPrice();
                    
                    // Tính toán số sao đánh giá trung bình
                    Double averageRating = reviewRepository.getAverageRatingByBookId(book.getId());
                    double rating = averageRating != null ? averageRating : 0.0;
                    
                    return new ReportDTO.TopSellingBook(
                        book.getId(),
                        book.getMainText(),
                        book.getAuthor(),
                        book.getCategory().getName(),
                        totalSold,
                        BigDecimal.valueOf(totalRevenue),
                        rating
                    );
                })
                .sorted(Comparator.comparing(ReportDTO.TopSellingBook::getTotalSold).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReportDTO.CategoryReport> getCategoryReport(LocalDateTime startDate, LocalDateTime endDate) {
        List<OrderItem> orderItems = orderItemRepository.findAllByOrderCreatedAtBetween(startDate, endDate).stream()
                .filter(item -> item.getOrder().getStatus() == Order.OrderStatus.DELIVERED)
                .collect(Collectors.toList());
        
        Map<Category, LongSummaryStatistics> categoryStats = orderItems.stream()
                .collect(Collectors.groupingBy(
                    item -> item.getBook().getCategory(),
                    Collectors.summarizingLong(OrderItem::getQuantity)
                ));
        
        long totalSold = categoryStats.values().stream()
                .mapToLong(LongSummaryStatistics::getSum)
                .sum();
        
        return categoryStats.entrySet().stream()
                .map(entry -> {
                    Category category = entry.getKey();
                    LongSummaryStatistics stats = entry.getValue();
                    double totalRevenue = orderItems.stream()
                            .filter(item -> item.getBook().getCategory().equals(category))
                            .mapToDouble(item -> item.getQuantity() * item.getBook().getPrice())
                            .sum();
                    double percentage = totalSold > 0 ? (stats.getSum() * 100.0) / totalSold : 0;
                    
                    return new ReportDTO.CategoryReport(
                        category.getId(),
                        category.getName(),
                        category.getBooks().size(),
                        stats.getSum(),
                        BigDecimal.valueOf(totalRevenue),
                        percentage
                    );
                })
                .sorted(Comparator.comparing(ReportDTO.CategoryReport::getTotalSold).reversed())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReportDTO.InventoryReport> getInventoryReport() {
        List<Book> books = bookRepository.findAll();
        List<OrderItem> deliveredOrderItems = orderItemRepository.findAll().stream()
                .filter(item -> item.getOrder().getStatus() == Order.OrderStatus.DELIVERED)
                .collect(Collectors.toList());
        
        Map<Book, LongSummaryStatistics> bookStats = deliveredOrderItems.stream()
                .collect(Collectors.groupingBy(
                    OrderItem::getBook,
                    Collectors.summarizingLong(OrderItem::getQuantity)
                ));
        
        return books.stream()
                .map(book -> {
                    LongSummaryStatistics stats = bookStats.getOrDefault(book, new LongSummaryStatistics());
                    long totalSold = stats.getSum();
                    double totalRevenue = totalSold * book.getPrice();
                    ReportDTO.StockStatus stockStatus = getStockStatus(book.getQuantity());
                    
                    return new ReportDTO.InventoryReport(
                        book.getId(),
                        book.getMainText(),
                        book.getAuthor(),
                        book.getCategory().getName(),
                        book.getQuantity(),
                        totalSold,
                        BigDecimal.valueOf(totalRevenue),
                        stockStatus
                    );
                })
                .sorted(Comparator.comparing(ReportDTO.InventoryReport::getTotalSold).reversed())
                .collect(Collectors.toList());
    }

    private ReportDTO.StockStatus getStockStatus(int quantity) {
        if (quantity <= 0) return ReportDTO.StockStatus.OUT_OF_STOCK;
        if (quantity <= 10) return ReportDTO.StockStatus.LOW;
        return ReportDTO.StockStatus.NORMAL;
    }
} 