package com.project2.BookStore.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponseDTO {
    private String message;
    private String token;
    private UserResponseDTO user;

    public LoginResponseDTO(String token, UserResponseDTO user) {
        this.message = "Đăng nhập thành công";
        this.token = token;
        this.user = user;
    }
}
