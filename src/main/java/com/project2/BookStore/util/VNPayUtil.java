package com.project2.BookStore.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.*;

@Slf4j
@Component
public class VNPayUtil {
    
    @Value("${vnpay.tmn-code}")
    private String tmnCode;
    
    @Value("${vnpay.hash-secret}")
    private String hashSecret;
    
    @Value("${vnpay.url}")
    private String url;
    
    @Value("${vnpay.return-url}")
    private String returnUrl;
    
    @Value("${vnpay.ipn-url}")
    private String ipnUrl;
    
    @Value("${vnpay.locale}")
    private String locale;
    
    @Value("${vnpay.currency}")
    private String currency;
    
    @Value("${vnpay.command}")
    private String command;
    
    @Value("${vnpay.order-type}")
    private String orderType;
    
    @Value("${vnpay.version}")
    private String version;
    
    /**
     * Tạo URL thanh toán VNPay
     */
    public String createPaymentUrl(String orderId, String amount, String description, String ipAddress) {
        try {
            // Tạo các tham số cần thiết
            String vnp_Version = version;
            String vnp_Command = command;
            String vnp_TmnCode = tmnCode;
            String vnp_Amount = String.valueOf(Long.parseLong(amount) * 100); // VNPay yêu cầu số tiền * 100
            String vnp_CurrCode = currency;
            String vnp_BankCode = "";
            String vnp_TxnRef = orderId;
            String vnp_OrderInfo = description != null ? description : "Thanh toan don hang " + orderId;
            String vnp_OrderType = orderType;
            String vnp_Locale = locale;
            String vnp_ReturnUrl = returnUrl;
            String vnp_IpnUrl = ipnUrl;
            
            // Tạo timestamp
            Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
            SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
            String vnp_CreateDate = formatter.format(cld.getTime());
            
            // Tạo danh sách tham số
            Map<String, String> vnp_Params = new HashMap<>();
            vnp_Params.put("vnp_Version", vnp_Version);
            vnp_Params.put("vnp_Command", vnp_Command);
            vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
            vnp_Params.put("vnp_Amount", vnp_Amount);
            vnp_Params.put("vnp_CurrCode", vnp_CurrCode);
            vnp_Params.put("vnp_BankCode", vnp_BankCode);
            vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
            vnp_Params.put("vnp_OrderInfo", vnp_OrderInfo);
            vnp_Params.put("vnp_OrderType", vnp_OrderType);
            vnp_Params.put("vnp_Locale", vnp_Locale);
            vnp_Params.put("vnp_ReturnUrl", vnp_ReturnUrl);
            vnp_Params.put("vnp_IpnUrl", vnp_IpnUrl);
            vnp_Params.put("vnp_CreateDate", vnp_CreateDate);
            vnp_Params.put("vnp_IpAddr", ipAddress);
            
            // Sắp xếp tham số theo thứ tự alphabet
            List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
            Collections.sort(fieldNames);
            
            // Tạo chuỗi hash data
            StringBuilder hashData = new StringBuilder();
            StringBuilder query = new StringBuilder();
            Iterator<String> itr = fieldNames.iterator();
            while (itr.hasNext()) {
                String fieldName = itr.next();
                String fieldValue = vnp_Params.get(fieldName);
                if ((fieldValue != null) && (fieldValue.length() > 0)) {
                    // Build hash data
                    hashData.append(fieldName);
                    hashData.append('=');
                    hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                    // Build query
                    query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII));
                    query.append('=');
                    query.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                    if (itr.hasNext()) {
                        query.append('&');
                        hashData.append('&');
                    }
                }
            }
            
            // Tạo checksum
            String queryUrl = query.toString();
            String vnp_SecureHash = hmacSHA512(hashSecret, hashData.toString());
            queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;
            
            String paymentUrl = url + "?" + queryUrl;
            log.info("Created VNPay payment URL: {}", paymentUrl);
            
            return paymentUrl;
            
        } catch (Exception e) {
            log.error("Error creating VNPay payment URL: {}", e.getMessage(), e);
            throw new RuntimeException("Không thể tạo URL thanh toán VNPay", e);
        }
    }
    
    /**
     * Tạo QR code từ URL thanh toán
     */
    public String createQRCode(String paymentUrl) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
            hints.put(EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name());
            hints.put(EncodeHintType.MARGIN, 2);
            
            BitMatrix bitMatrix = qrCodeWriter.encode(paymentUrl, BarcodeFormat.QR_CODE, 300, 300, hints);
            
            // Convert to base64
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            for (int y = 0; y < bitMatrix.getHeight(); y++) {
                for (int x = 0; x < bitMatrix.getWidth(); x++) {
                    outputStream.write(bitMatrix.get(x, y) ? 0xFF : 0x00);
                }
            }
            
            byte[] qrCodeBytes = outputStream.toByteArray();
            String base64QRCode = Base64.getEncoder().encodeToString(qrCodeBytes);
            
            return "data:image/png;base64," + base64QRCode;
            
        } catch (WriterException e) {
            log.error("Error creating QR code: {}", e.getMessage(), e);
            throw new RuntimeException("Không thể tạo QR code", e);
        }
    }
    
    /**
     * Tạo HMAC-SHA512 checksum
     */
    public String hmacSHA512(String key, String data) {
        try {
            Mac sha512_HMAC = Mac.getInstance("HmacSHA512");
            SecretKeySpec secret_key = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            sha512_HMAC.init(secret_key);
            byte[] hash = sha512_HMAC.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Error creating HMAC-SHA512: {}", e.getMessage(), e);
            throw new RuntimeException("Không thể tạo checksum", e);
        }
    }
    
    /**
     * Chuyển đổi byte array thành hex string
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
    
    /**
     * Xác thực checksum từ callback
     */
    public boolean verifyChecksum(Map<String, String> params, String secureHash) {
        try {
            // Loại bỏ vnp_SecureHash khỏi params
            params.remove("vnp_SecureHash");
            params.remove("vnp_SecureHashType");
            
            // Sắp xếp tham số theo thứ tự alphabet
            List<String> fieldNames = new ArrayList<>(params.keySet());
            Collections.sort(fieldNames);
            
            // Tạo chuỗi hash data
            StringBuilder hashData = new StringBuilder();
            Iterator<String> itr = fieldNames.iterator();
            while (itr.hasNext()) {
                String fieldName = itr.next();
                String fieldValue = params.get(fieldName);
                if ((fieldValue != null) && (fieldValue.length() > 0)) {
                    hashData.append(fieldName);
                    hashData.append('=');
                    hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                    if (itr.hasNext()) {
                        hashData.append('&');
                    }
                }
            }
            
            // Tạo checksum
            String calculatedHash = hmacSHA512(hashSecret, hashData.toString());
            
            return calculatedHash.equals(secureHash);
            
        } catch (Exception e) {
            log.error("Error verifying checksum: {}", e.getMessage(), e);
            return false;
        }
    }
} 