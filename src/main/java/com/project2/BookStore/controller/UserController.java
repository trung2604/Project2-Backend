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
import com.project2.BookStore.service.OrderService;
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
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/bookStore/user")
public class UserController {
    @Autowired
    private UserService userService;

    @Autowired
    private ImageProcessingService imageProcessingService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private OrderService orderService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponseDTO> register(@Valid @RequestBody RegisterRequest registerRequest) {
        log.info("Received register request for email: {}", registerRequest.getEmail());
        try {
            UserResponseDTO result = userService.register(registerRequest);
            log.info("Register successful for email: {}", registerRequest.getEmail());
            return ResponseEntity.ok(new ApiResponseDTO(true, "Đăng ký thành công", result));
        } catch (BadRequestException e) {
            log.warn("Register failed for email {}: {}", registerRequest.getEmail(), e.getMessage());
            return ResponseEntity.badRequest()
                .body(new ApiResponseDTO(false, e.getMessage(), null));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponseDTO> login(@Valid @RequestBody LoginRequest loginRequest) {
        log.info("Received login request for email: {}", loginRequest.getEmail());
        try {
            LoginResponseDTO result = userService.login(loginRequest);
            log.info("Login successful for email: {}", loginRequest.getEmail());
            return ResponseEntity.ok(new ApiResponseDTO(true, "Đăng nhập thành công", result));
        } catch (BadRequestException e) {
            log.warn("Login failed for email {}: {}", loginRequest.getEmail(), e.getMessage());
            return ResponseEntity.badRequest()
                .body(new ApiResponseDTO(false, e.getMessage(), null));
        } catch (Exception e) {
            log.error("Unexpected error during login for email {}: {}", loginRequest.getEmail(), e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponseDTO(false, "Lỗi server: " + e.getMessage(), null));
        }
    }

    @GetMapping()
    public ResponseEntity<ApiResponseDTO> getAllUsers() {
        try {
            List<UserResponseDTO> users = userService.getAllUsers();
            return ResponseEntity.ok(new ApiResponseDTO(true, "Lấy danh sách người dùng thành công", users));
        } catch (BadRequestException e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponseDTO(false, e.getMessage(), null));
        }
    }

    @PutMapping("/update")
    public ResponseEntity<ApiResponseDTO> updateUser(@Valid @RequestBody UpdateUserRequest req) {
        try {
            UserResponseDTO updatedUser = userService.updateUser(req);
            return ResponseEntity.ok(new ApiResponseDTO(true, "Cập nhật thông tin người dùng thành công", updatedUser));
        } catch (BadRequestException e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponseDTO(false, e.getMessage(), null));
        }
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<ApiResponseDTO> deleteUser(@PathVariable String id) {
        log.info("Bắt đầu xóa người dùng. UserId: {}", id);
        try {
            if (orderService.hasActiveOrders(id)) {
                throw new BadRequestException("Không thể xóa người dùng có đơn hàng đang xử lý");
            }
            
            User user = userService.getUserById(id, false);
            
            if (user.getAvatar() != null) {
                imageProcessingService.deleteOldAvatar(user.getAvatar());
            }
            
            userService.deleteUser(id);
            
            log.info("Xóa người dùng thành công. UserId: {}", id);
            return ResponseEntity.ok(new ApiResponseDTO(true, "Xóa người dùng thành công", null));
        } catch (BadRequestException e) {
            log.warn("Lỗi khi xóa người dùng: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(new ApiResponseDTO(false, e.getMessage(), null));
        } catch (Exception e) {
            log.error("Lỗi không mong muốn khi xóa người dùng: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponseDTO(false, "Lỗi server: " + e.getMessage(), null));
        }
    }

    @PostMapping("/avatar/upload")
    public ResponseEntity<ApiResponseDTO> uploadAvatar(
            @RequestParam("file") MultipartFile file,
            @RequestParam("userId") String userId) {
        try {
            User user = userService.getUserById(userId);
            AvatarDTO avatarDTO = imageProcessingService.processAndUploadAvatar(file, user);
            userService.updateUserAvatar(userId, avatarDTO);
            return ResponseEntity.ok(new ApiResponseDTO(true, "Tải lên ảnh đại diện thành công", avatarDTO));
        } catch (BadRequestException e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponseDTO(false, e.getMessage(), null));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponseDTO(false, "Lỗi server khi xử lý ảnh: " + e.getMessage(), null));
        }
    }

    @GetMapping("/paged")
    public ResponseEntity<ApiResponseDTO> getUsersPaged(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int pageSize) {
        try {
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

            return ResponseEntity.ok(new ApiResponseDTO(true, "Lấy danh sách người dùng thành công", data));
        } catch (BadRequestException e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponseDTO(false, e.getMessage(), null));
        }
    }

    @GetMapping("/account")
    public ResponseEntity<ApiResponseDTO> getCurrentUser(HttpServletRequest request) {
        try {
            String token = request.getHeader("Authorization").replace("Bearer ", "");
            String email = jwtUtil.getEmailFromToken(token);
            UserResponseDTO user = userService.getUserByEmail(email);
            return ResponseEntity.ok(new ApiResponseDTO(true, "Lấy thông tin người dùng thành công", user));
        } catch (BadRequestException e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponseDTO(false, e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiResponseDTO(false, "Token không hợp lệ hoặc đã hết hạn", null));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponseDTO> logout(HttpServletRequest request) {
        try {
            String token = request.getHeader("Authorization").replace("Bearer ", "");
            if (!jwtUtil.validateToken(token)) {
                throw new BadRequestException("Token không hợp lệ");
            }
            return ResponseEntity.ok(new ApiResponseDTO(true, "Đăng xuất thành công", null));
        } catch (BadRequestException e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponseDTO(false, e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiResponseDTO(false, "Token không hợp lệ hoặc đã hết hạn", null));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDTO> getUserById(@PathVariable String id) {
        try {
            User user = userService.getUserById(id);
            UserResponseDTO userResponseDTO = new UserResponseDTO(user);
            return ResponseEntity.ok(new ApiResponseDTO(true, "Lấy thông tin người dùng thành công", userResponseDTO));
        } catch (BadRequestException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponseDTO(false, e.getMessage(), null));
        }
    }

    @PostMapping("/upload-avatar")
    public ResponseEntity<ApiResponseDTO> uploadAvatar(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request) {
        try {
            String token = request.getHeader("Authorization").replace("Bearer ", "");
            String userId = jwtUtil.getUserIdFromToken(token);
            User user = userService.getUserById(userId);
            AvatarDTO avatarDTO = imageProcessingService.processAndUploadAvatar(file, user);
            userService.updateUserAvatar(userId, avatarDTO);
            return ResponseEntity.ok(new ApiResponseDTO(true, "Tải lên ảnh đại diện thành công", avatarDTO));
        } catch (BadRequestException e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponseDTO(false, e.getMessage(), null));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponseDTO(false, "Lỗi khi tải lên ảnh đại diện: " + e.getMessage(), null));
        }
    }
}
