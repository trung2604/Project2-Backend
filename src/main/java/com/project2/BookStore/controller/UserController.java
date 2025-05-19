package com.project2.BookStore.controller;

import com.project2.BookStore.dto.RegisterRequest;
import com.project2.BookStore.dto.LoginRequest;
import com.project2.BookStore.dto.UserResponseDTO;
import com.project2.BookStore.dto.LoginResponseDTO;
import com.project2.BookStore.dto.UpdateUserRequest;
import com.project2.BookStore.dto.ApiResponseDTO;
import com.project2.BookStore.dto.AvatarDTO;
import com.project2.BookStore.dto.LoginResultDTO;
import com.project2.BookStore.exception.BadRequestException;
import com.project2.BookStore.model.User;
import com.project2.BookStore.service.UserService;
import com.project2.BookStore.service.ImageProcessingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import java.util.HashMap;
import java.util.Map;

import java.util.List;

import com.project2.BookStore.util.JwtUtil;

@RestController
@RequestMapping("/api/bookStore/user")
public class UserController {
    @Autowired
    private UserService userService;

    @Autowired
    private ImageProcessingService imageProcessingService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/register")
    public ResponseEntity<UserResponseDTO> register(@Valid @RequestBody RegisterRequest req) {
        UserResponseDTO user = userService.register(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponseDTO<LoginResultDTO>> login(@Valid @RequestBody LoginRequest req) {
        LoginResponseDTO loginResponse = userService.login(req);
        LoginResultDTO result = new LoginResultDTO(
            "Đăng nhập thành công!",
            loginResponse.getToken(),
            loginResponse.getUser()
        );
        ApiResponseDTO<LoginResultDTO> response = new ApiResponseDTO<>(200, "", result);
        return ResponseEntity.ok(response);
    }

    @GetMapping()
    public List<UserResponseDTO> getAllUsers() {
        return userService.getAllUsers();
    }

    @PutMapping("/update")
    public ResponseEntity<UserResponseDTO> updateUser(@Valid @RequestBody UpdateUserRequest req) {
        UserResponseDTO updatedUser = userService.updateUser(req);
        return ResponseEntity.ok(updatedUser);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<ApiResponseDTO<Void>> deleteUser(@PathVariable String id) {
        userService.deleteUser(id);
        ApiResponseDTO<Void> response = new ApiResponseDTO<>(200, "Xóa user thành công!", null);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/avatar/upload")
    public ResponseEntity<ApiResponseDTO<AvatarDTO>> uploadAvatar(
            @RequestParam("file") MultipartFile file,
            @RequestParam("userId") String userId) {
        try {
            // Get user to access old avatar
            User user = userService.getUserById(userId);
            AvatarDTO avatarDTO = imageProcessingService.processAndUploadAvatar(file, user);
            userService.updateUserAvatar(userId, avatarDTO);
            
            ApiResponseDTO<AvatarDTO> response = new ApiResponseDTO<>(
                200,
                "Upload avatar thành công!",
                avatarDTO
            );
            return ResponseEntity.ok(response);
        } catch (BadRequestException e) {
            ApiResponseDTO<AvatarDTO> response = new ApiResponseDTO<>(
                400,
                e.getMessage(),
                null
            );
            return ResponseEntity.badRequest().body(response);
        } catch (IOException e) {
            ApiResponseDTO<AvatarDTO> response = new ApiResponseDTO<>(
                500,
                "Lỗi server khi xử lý ảnh: " + e.getMessage(),
                null
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/paged")
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> getUsersPaged(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int pageSize) {
        PageRequest pageRequest = PageRequest.of(Math.max(0, current - 1), pageSize);
        Page<UserResponseDTO> page = userService.getUsersPaged(pageRequest);

        Map<String, Object> meta = new HashMap<>();
        meta.put("current", current);
        meta.put("pageSize", pageSize);
        meta.put("pages", page.getTotalPages());
        meta.put("total", page.getTotalElements());

        Map<String, Object> data = new HashMap<>();
        data.put("meta", meta);
        data.put("result", page.getContent());

        ApiResponseDTO<Map<String, Object>> response = new ApiResponseDTO<>(200, "", data);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/account")
    public ResponseEntity<ApiResponseDTO<UserResponseDTO>> getCurrentUser(HttpServletRequest request) {
        try {
            String authHeader = request.getHeader("Authorization");
            System.out.println("Auth header: " + authHeader); // Debug log

            if (authHeader == null || authHeader.isEmpty()) {
                ApiResponseDTO<UserResponseDTO> response = new ApiResponseDTO<>(401, "Không tìm thấy token xác thực", null);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            if (!authHeader.startsWith("Bearer ")) {
                ApiResponseDTO<UserResponseDTO> response = new ApiResponseDTO<>(401, "Token không đúng định dạng", null);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            String token = authHeader.substring(7).trim();
            if (token.isEmpty()) {
                ApiResponseDTO<UserResponseDTO> response = new ApiResponseDTO<>(401, "Token không được để trống", null);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            System.out.println("Token: " + token); // Debug log
            String email = jwtUtil.getEmailFromToken(token);
            System.out.println("Email from token: " + email); // Debug log

            UserResponseDTO user = userService.getUserByEmail(email);
            ApiResponseDTO<UserResponseDTO> response = new ApiResponseDTO<>(200, "Lấy thông tin user thành công!", user);
            return ResponseEntity.ok(response);
        } catch (BadRequestException e) {
            System.out.println("BadRequestException: " + e.getMessage()); // Debug log
            ApiResponseDTO<UserResponseDTO> response = new ApiResponseDTO<>(400, e.getMessage(), null);
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            System.out.println("Exception: " + e.getMessage()); // Debug log
            ApiResponseDTO<UserResponseDTO> response = new ApiResponseDTO<>(401, "Token không hợp lệ hoặc đã hết hạn", null);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponseDTO<Void>> logout(HttpServletRequest request) {
        try {
            String authHeader = request.getHeader("Authorization");
            System.out.println("Logout - Auth header: " + authHeader); // Debug log

            if (authHeader == null || authHeader.isEmpty()) {
                ApiResponseDTO<Void> response = new ApiResponseDTO<>(401, "Không tìm thấy token xác thực", null);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            if (!authHeader.startsWith("Bearer ")) {
                ApiResponseDTO<Void> response = new ApiResponseDTO<>(401, "Token không đúng định dạng", null);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            String token = authHeader.substring(7).trim();
            if (token.isEmpty()) {
                ApiResponseDTO<Void> response = new ApiResponseDTO<>(401, "Token không được để trống", null);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            // Kiểm tra token có hợp lệ không
            try {
                String email = jwtUtil.getEmailFromToken(token);
                System.out.println("Logout - Email from token: " + email); // Debug log
                
                // Token hợp lệ, trả về thành công
                ApiResponseDTO<Void> response = new ApiResponseDTO<>(200, "Đăng xuất thành công!", null);
                return ResponseEntity.ok(response);
            } catch (Exception e) {
                // Token không hợp lệ hoặc đã hết hạn
                ApiResponseDTO<Void> response = new ApiResponseDTO<>(401, "Token không hợp lệ hoặc đã hết hạn", null);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
        } catch (Exception e) {
            System.out.println("Logout - Exception: " + e.getMessage()); // Debug log
            ApiResponseDTO<Void> response = new ApiResponseDTO<>(500, "Đã xảy ra lỗi khi đăng xuất", null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
