package com.project2.BookStore.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    private T data;
    private String message;
    private int statusCode;

    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(data, message, HttpStatus.OK.value());
    }

    public static <T> ApiResponse<T> success(T data) {
        return success(data, "Thao tác thành công");
    }

    public static <T> ApiResponse<T> error(String message, HttpStatus status) {
        return new ApiResponse<>(null, message, status.value());
    }

    public static <T> ApiResponse<T> error(String message) {
        return error(message, HttpStatus.BAD_REQUEST);
    }

    public static <T> ApiResponse<T> notFound(String message) {
        return error(message, HttpStatus.NOT_FOUND);
    }

    public static <T> ApiResponse<T> unauthorized(String message) {
        return error(message, HttpStatus.UNAUTHORIZED);
    }

    public static <T> ApiResponse<T> forbidden(String message) {
        return error(message, HttpStatus.FORBIDDEN);
    }

    public static <T> ApiResponse<T> serverError(String message) {
        return error(message, HttpStatus.INTERNAL_SERVER_ERROR);
    }
} 