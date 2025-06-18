package com.project2.BookStore.service;

import com.project2.BookStore.dto.ReportDTO;
import java.time.LocalDateTime;
import java.util.List;

public interface ReportService {
    // Báo cáo doanh thu theo khoảng thời gian
    ReportDTO.RevenueReport getRevenueReport(LocalDateTime startDate, LocalDateTime endDate);
    
    // Báo cáo trạng thái đơn hàng
    List<ReportDTO.OrderStatusReport> getOrderStatusReport();
    
    // Top sách bán chạy
    List<ReportDTO.TopSellingBook> getTopSellingBooks(LocalDateTime startDate, LocalDateTime endDate, int limit);
    
    // Báo cáo theo danh mục
    List<ReportDTO.CategoryReport> getCategoryReport(LocalDateTime startDate, LocalDateTime endDate);
    
    // Báo cáo tồn kho
    List<ReportDTO.InventoryReport> getInventoryReport();
} 