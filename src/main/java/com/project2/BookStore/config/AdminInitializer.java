package com.project2.BookStore.config;

import com.project2.BookStore.model.User;
import com.project2.BookStore.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import java.util.Date;

@Component
public class AdminInitializer implements CommandLineRunner {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        boolean hasAdmin = userRepository.findAll().stream()
            .anyMatch(u -> "ADMIN".equalsIgnoreCase(u.getRole()));
        if (!hasAdmin) {
            User admin = new User();
            admin.setFullName("Admin");
            admin.setEmail("admin@bookstore.com");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setPhone("0123456789");
            admin.setRole("ADMIN");
            admin.setActive(true);
            admin.setCreatedAt(new Date());
            admin.setUpdatedAt(new Date());
            userRepository.save(admin);
            System.out.println("Đã tạo tài khoản admin mặc định: admin@bookstore.com / admin123");
        }
    }
} 