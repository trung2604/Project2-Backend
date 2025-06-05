package com.project2.BookStore.config;

import com.project2.BookStore.model.User;
import com.project2.BookStore.repository.UserRepository;
import com.project2.BookStore.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInitializer implements CommandLineRunner {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        try {
            User existingAdmin = userRepository.findByEmail("admin@bookstore.com");
            if (existingAdmin == null) {
                User admin = new User();
                admin.setFullName("Admin");
                admin.setEmail("admin@bookstore.com");
                admin.setPassword(passwordEncoder.encode("admin123"));
                admin.setPhone("0123456789");
                admin.setRole(User.UserRole.ROLE_ADMIN);
                admin.setActive(true);
                admin.setCreatedAt(LocalDateTime.now());
                admin.setUpdatedAt(LocalDateTime.now());
                userRepository.save(admin);
                log.info("Đã tạo tài khoản admin mặc định: admin@bookstore.com / admin123");
            } else {
                log.info("Tài khoản admin đã tồn tại");
            }
        } catch (Exception e) {
            log.error("Lỗi khi khởi tạo tài khoản admin: {}", e.getMessage());
            throw new BadRequestException("Không thể khởi tạo tài khoản admin: " + e.getMessage());
        }
    }
} 