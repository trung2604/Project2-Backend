package com.project2.BookStore.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequestDTO {
    @NotBlank(message = "Họ tên không được trống")
    @Size(min = 2, max = 100, message = "Họ tên phải từ 2 đến 100 ký tự")
    private String fullName;

    @NotBlank(message = "Số điện thoại không được trống")
    @Pattern(regexp = "^\\d{10,11}$", message = "Số điện thoại không hợp lệ")
    private String phone;

    @NotBlank(message = "Địa chỉ không được trống")
    @Size(min = 5, max = 200, message = "Địa chỉ phải từ 5 đến 200 ký tự")
    private String address;

    @NotBlank(message = "Email không được trống")
    @Email(message = "Email không hợp lệ")
    private String email;

    @NotEmpty(message = "Danh sách sản phẩm không được trống")
    private List<OrderItemRequestDTO> items;

    @NotBlank(message = "Phương thức thanh toán không được trống")
    @Pattern(regexp = "^(COD|BANKING)$", message = "Phương thức thanh toán không hợp lệ")
    private String paymentMethod;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemRequestDTO {
        @NotBlank(message = "Mã sách không được trống")
        private String bookId;

        @NotNull(message = "Số lượng không được trống")
        @Size(min = 1, max = 100, message = "Số lượng phải từ 1 đến 100")
        private int quantity;
    }
} 