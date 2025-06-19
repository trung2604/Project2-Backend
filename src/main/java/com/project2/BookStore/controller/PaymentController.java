package com.project2.BookStore.controller;

import com.project2.BookStore.dto.ApiResponseDTO;
import com.project2.BookStore.dto.PaymentDTO;
import com.project2.BookStore.service.PaymentService;
import com.project2.BookStore.util.JwtUtil;
import com.project2.BookStore.util.VNPayUtil;
import com.project2.BookStore.repository.PaymentRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/bookStore/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private VNPayUtil vnPayUtil;
    @Autowired
    private PaymentRepository paymentRepository;

    private String getCurrentUserId(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException("Token không hợp lệ");
        }
        token = token.substring(7);
        String userId = jwtUtil.getUserIdFromToken(token);
        if (userId == null) throw new RuntimeException("Token không hợp lệ hoặc đã hết hạn");
        return userId;
    }

    @PostMapping("/vnpay")
    public ResponseEntity<ApiResponseDTO> createVNPayPayment(@RequestBody PaymentDTO.VNPayPaymentRequest request, HttpServletRequest httpRequest) {
        try {
            String userId = getCurrentUserId(httpRequest);
            String ipAddress = httpRequest.getRemoteAddr();
            PaymentDTO.VNPayPaymentResponse response = paymentService.createVNPayPayment(request, userId, ipAddress);
            Map<String, Object> data = new HashMap<>();
            data.put("paymentId", response.getPaymentId());
            data.put("orderId", response.getOrderId());
            data.put("amount", response.getAmount());
            data.put("paymentUrl", response.getPaymentUrl());
            data.put("transactionId", response.getTransactionId());
            data.put("checksum", response.getChecksum());
            data.put("createdAt", response.getCreatedAt());
            data.put("description", response.getDescription());
            return ResponseEntity.ok(new ApiResponseDTO(true, "Tạo thanh toán thành công", data));
        } catch (Exception e) {
            log.error("Lỗi tạo thanh toán VNPay: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(new ApiResponseDTO(false, e.getMessage(), null));
        }
    }

    @PostMapping("/ipn")
    public ResponseEntity<String> vnpayIpn(@RequestParam Map<String, String> params) {
        boolean result = paymentService.handleVNPayCallback(new HashMap<>(params));
        if (result) {
            return ResponseEntity.ok("OK");
        } else {
            return ResponseEntity.badRequest().body("INVALID");
        }
    }

    @GetMapping("/status/{orderId}")
    public ResponseEntity<ApiResponseDTO> getPaymentStatus(@PathVariable String orderId) {
        PaymentDTO.PaymentStatusResponse status = paymentService.getPaymentStatus(orderId);
        return ResponseEntity.ok(new ApiResponseDTO(true, "Trạng thái thanh toán", status));
    }

    @GetMapping("/qr-code/{paymentId}")
    public ResponseEntity<ApiResponseDTO> getQRCode(@PathVariable String paymentId, HttpServletRequest httpRequest) {
        try {
            String userId = getCurrentUserId(httpRequest);
            
            // Tìm payment và kiểm tra quyền
            var payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thanh toán"));
            
            if (!payment.getOrder().getUser().getId().equals(userId)) {
                throw new RuntimeException("Bạn không có quyền truy cập thanh toán này");
            }
            
            // Tạo QR code từ payment URL
            String qrCodeUrl = vnPayUtil.createQRCode(payment.getPaymentUrl());
            
            Map<String, String> response = new HashMap<>();
            response.put("qrCodeUrl", qrCodeUrl);
            response.put("paymentUrl", payment.getPaymentUrl());
            
            return ResponseEntity.ok(new ApiResponseDTO(true, "QR code thanh toán", response));
        } catch (Exception e) {
            log.error("Lỗi lấy QR code: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(new ApiResponseDTO(false, e.getMessage(), null));
        }
    }
} 