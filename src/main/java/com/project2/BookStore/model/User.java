package com.project2.BookStore.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@Entity
@Table(name = "users")
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private String id;

    @NotBlank(message = "Họ tên không được để trống")
    @Size(min = 2, max = 100, message = "Họ tên phải từ 2 đến 100 ký tự")
    @Column(name = "full_name", nullable = false)
    private String fullName;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Column(name = "password", nullable = false)
    private String password;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^\\d{10}$", message = "Số điện thoại phải có 10 chữ số")
    @Column(name = "phone")
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Order> orders = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<CartItem> cartItems = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false, columnDefinition = "TIMESTAMP")
    private LocalDateTime updatedAt;

    @Embedded
    private Avatar avatar;

    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Avatar {
        @Column(name = "avatar_thumbnail")
        private String thumbnail;  // URL for small size (150x150)
        
        @Column(name = "avatar_medium")
        private String medium;     // URL for medium size (300x300)
        
        @Column(name = "avatar_original")
        private String original;   // URL for original size
        
        @Column(name = "avatar_public_id")
        private String publicId;   // Cloudinary public ID
        
        @Column(name = "avatar_format")
        private String format;     // Image format (webp/jpeg/png)
        
        @Column(name = "avatar_created_at", columnDefinition = "TIMESTAMP")
        private LocalDateTime createdAt; // When the avatar was uploaded
    }

    public enum UserRole {
        ROLE_USER,
        ROLE_ADMIN,
        ROLE_SHIPPER
    }
} 