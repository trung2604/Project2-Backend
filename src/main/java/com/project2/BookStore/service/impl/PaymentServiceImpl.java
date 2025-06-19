package com.project2.BookStore.service.impl;

import com.project2.BookStore.dto.PaymentDTO;
import com.project2.BookStore.model.Order;
import com.project2.BookStore.model.Payment;
import com.project2.BookStore.repository.OrderRepository;
import com.project2.BookStore.repository.PaymentRepository;
import com.project2.BookStore.service.PaymentService;
import com.project2.BookStore.util.VNPayUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final VNPayUtil vnPayUtil;
    
    @Value("${vnpay.currency}")
    private String currency;

    @Override
    @Transactional
    public PaymentDTO.VNPayPaymentResponse createVNPayPayment(PaymentDTO.VNPayPaymentRequest request, String userId, String ipAddress) {
        // Kiểm tra đơn hàng
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng"));
        if (!order.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Bạn không có quyền thanh toán đơn hàng này");
        }
        if (order.getPaymentStatus() == Order.PaymentStatus.PAID) {
            throw new IllegalArgumentException("Đơn hàng đã được thanh toán");
        }
        // Tạo URL thanh toán
        String paymentUrl = vnPayUtil.createPaymentUrl(order.getId(), request.getAmount().toString(), request.getDescription(), ipAddress);
        // Không tạo QR code ở backend nữa
        // Lưu thông tin thanh toán
        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setAmount(request.getAmount());
        payment.setCurrency(currency);
        payment.setPaymentMethod(Payment.PaymentMethod.BANKING);
        payment.setStatus(Payment.PaymentStatus.PENDING);
        payment.setPaymentUrl(paymentUrl);
        payment.setDescription(request.getDescription());
        payment.setCreatedAt(LocalDateTime.now());
        paymentRepository.save(payment);
        // Trả về response
        return new PaymentDTO.VNPayPaymentResponse(
                payment.getId(),
                order.getId(),
                payment.getAmount(),
                payment.getPaymentUrl(),
                payment.getTransactionId(),
                payment.getChecksum(),
                payment.getCreatedAt(),
                payment.getDescription()
        );
    }

    @Override
    @Transactional
    public boolean handleVNPayCallback(Map<String, String> params) {
        // Xác thực checksum
        String secureHash = params.get("vnp_SecureHash");
        if (!vnPayUtil.verifyChecksum(params, secureHash)) {
            log.warn("VNPay callback: Checksum không hợp lệ");
            return false;
        }
        String vnp_TxnRef = params.get("vnp_TxnRef");
        String vnp_Amount = params.get("vnp_Amount");
        String vnp_ResponseCode = params.get("vnp_ResponseCode");
        String vnp_TransactionNo = params.get("vnp_TransactionNo");
        String vnp_Message = params.get("vnp_Message");
        // Tìm payment
        Optional<Payment> paymentOpt = paymentRepository.findByOrderId(vnp_TxnRef);
        if (paymentOpt.isEmpty()) {
            log.warn("VNPay callback: Không tìm thấy payment cho orderId {}", vnp_TxnRef);
            return false;
        }
        Payment payment = paymentOpt.get();
        // Cập nhật trạng thái
        if ("00".equals(vnp_ResponseCode)) {
            payment.setStatus(Payment.PaymentStatus.COMPLETED);
            payment.setPaidAt(LocalDateTime.now());
        } else {
            payment.setStatus(Payment.PaymentStatus.FAILED);
        }
        payment.setVnpayTransactionId(vnp_TransactionNo);
        payment.setVnpayResponseCode(vnp_ResponseCode);
        payment.setVnpayMessage(vnp_Message);
        paymentRepository.save(payment);
        // Cập nhật trạng thái đơn hàng nếu thanh toán thành công
        if ("00".equals(vnp_ResponseCode)) {
            Order order = payment.getOrder();
            order.setPaymentStatus(Order.PaymentStatus.PAID);
            orderRepository.save(order);
        }
        return true;
    }

    @Override
    public PaymentDTO.PaymentStatusResponse getPaymentStatus(String orderId) {
        Optional<Payment> paymentOpt = paymentRepository.findByOrderId(orderId);
        if (paymentOpt.isEmpty()) {
            return new PaymentDTO.PaymentStatusResponse(null, orderId, "NOT_FOUND", "Không tìm thấy thanh toán", null);
        }
        Payment payment = paymentOpt.get();
        return new PaymentDTO.PaymentStatusResponse(
                payment.getId(),
                orderId,
                payment.getStatus().name(),
                payment.getVnpayMessage(),
                payment.getUpdatedAt()
        );
    }
} 