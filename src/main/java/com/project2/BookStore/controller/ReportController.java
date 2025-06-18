package com.project2.BookStore.controller;

import com.project2.BookStore.dto.ApiResponseDTO;
import com.project2.BookStore.dto.ReportDTO;
import com.project2.BookStore.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/bookStore/reports")
@RequiredArgsConstructor
public class ReportController {
    private final ReportService reportService;

    @GetMapping("/revenue")
    public ResponseEntity<ApiResponseDTO> getRevenueReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        log.info("Getting revenue report from {} to {}", startDate, endDate);
        try {
            ReportDTO.RevenueReport report = reportService.getRevenueReport(startDate, endDate);
            return ResponseEntity.ok(new ApiResponseDTO(true, "Lấy báo cáo doanh thu thành công", report));
        } catch (Exception e) {
            log.error("Error getting revenue report: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(new ApiResponseDTO(false, "Lỗi khi lấy báo cáo doanh thu: " + e.getMessage(), null));
        }
    }

    @GetMapping("/top-selling-books")
    public ResponseEntity<ApiResponseDTO> getTopSellingBooks(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "10") int limit) {
        log.info("Getting top selling books from {} to {}, limit: {}", startDate, endDate, limit);
        try {
            List<ReportDTO.TopSellingBook> books = reportService.getTopSellingBooks(startDate, endDate, limit);
            return ResponseEntity.ok(new ApiResponseDTO(true, "Lấy danh sách sách bán chạy thành công", books));
        } catch (Exception e) {
            log.error("Error getting top selling books: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(new ApiResponseDTO(false, "Lỗi khi lấy danh sách sách bán chạy: " + e.getMessage(), null));
        }
    }

    @GetMapping("/inventory")
    public ResponseEntity<ApiResponseDTO> getInventoryReport() {
        log.info("Getting inventory report");
        try {
            List<ReportDTO.InventoryReport> report = reportService.getInventoryReport();
            return ResponseEntity.ok(new ApiResponseDTO(true, "Lấy báo cáo tồn kho thành công", report));
        } catch (Exception e) {
            log.error("Error getting inventory report: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(new ApiResponseDTO(false, "Lỗi khi lấy báo cáo tồn kho: " + e.getMessage(), null));
        }
    }

    @GetMapping("/categories")
    public ResponseEntity<ApiResponseDTO> getCategoryReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        log.info("Getting category report from {} to {}", startDate, endDate);
        try {
            List<ReportDTO.CategoryReport> report = reportService.getCategoryReport(startDate, endDate);
            return ResponseEntity.ok(new ApiResponseDTO(true, "Lấy báo cáo theo danh mục thành công", report));
        } catch (Exception e) {
            log.error("Error getting category report: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(new ApiResponseDTO(false, "Lỗi khi lấy báo cáo danh mục: " + e.getMessage(), null));
        }
    }

    @GetMapping("/order-status")
    public ResponseEntity<ApiResponseDTO> getOrderStatusReport() {
        log.info("Getting order status report");
        try {
            List<ReportDTO.OrderStatusReport> report = reportService.getOrderStatusReport();
            return ResponseEntity.ok(new ApiResponseDTO(true, "Lấy báo cáo trạng thái đơn hàng thành công", report));
        } catch (Exception e) {
            log.error("Error getting order status report: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(new ApiResponseDTO(false, "Lỗi khi lấy báo cáo trạng thái đơn hàng: " + e.getMessage(), null));
        }
    }
} 