package com.project2.BookStore.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequestDTO {
    @NotBlank(message = "Họ tên không được trống")
    @Size(min = 2, max = 100, message = "Họ tên phải từ 2 đến 100 ký tự")
    private String fullName;

    @NotBlank(message = "Số điện thoại không được trống")
    @Pattern(regexp = "^\\d{10,11}$", message = "Số điện thoại phải có 10-11 chữ số")
    private String phone;

    @NotBlank(message = "Địa chỉ không được trống")
    @Size(min = 10, max = 200, message = "Địa chỉ phải từ 10 đến 200 ký tự")
    private String address;

    @NotBlank(message = "Email không được trống")
    @Email(message = "Email không đúng định dạng")
    @Size(max = 100, message = "Email không được vượt quá 100 ký tự")
    private String email;

    @NotEmpty(message = "Danh sách sản phẩm không được trống")
    @Valid
    private List<OrderItemRequestDTO> items;

    @NotBlank(message = "Phương thức thanh toán không được để trống")
    @Pattern(regexp = "^(COD|BANKING)$", message = "Phương thức thanh toán không hợp lệ. Chấp nhận: COD (Thanh toán khi nhận hàng) hoặc BANKING (Chuyển khoản ngân hàng)")
    private String paymentMethod;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemRequestDTO {
        @NotBlank(message = "ID sách không được trống")
        private String bookId;

        @Pattern(regexp = "^[1-9]\\d*$", message = "Số lượng phải là số nguyên dương")
        private String quantity;
    }
} 