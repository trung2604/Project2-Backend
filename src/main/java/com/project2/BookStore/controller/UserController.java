package com.project2.BookStore.controller;

import com.project2.BookStore.dto.*;
import com.project2.BookStore.exception.BadRequestException;
import com.project2.BookStore.model.User;
import com.project2.BookStore.repository.UserRepository;
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
import java.util.Optional;

import com.project2.BookStore.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

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
    @Autowired
    private UserRepository userRepository;

    @PostMapping("/register")
    public ResponseEntity<ApiResponseDTO> register(@Valid @RequestBody RegisterRequest registerRequest) {
        log.info("Bắt đầu đăng ký tài khoản mới. Email: {}", registerRequest.getEmail());
        try {
            UserResponseDTO result = userService.register(registerRequest);
            log.info("Đăng ký thành công. Email: {}", registerRequest.getEmail());
            return ResponseEntity.ok(new ApiResponseDTO(true, "Đăng ký thành công", result));
        } catch (BadRequestException e) {
            log.warn("Đăng ký thất bại. Email: {}, Lỗi: {}", registerRequest.getEmail(), e.getMessage());
            return ResponseEntity.badRequest()
                .body(new ApiResponseDTO(false, e.getMessage(), null));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponseDTO> login(@Valid @RequestBody LoginRequest loginRequest) {
        log.info("Bắt đầu đăng nhập. Email: {}", loginRequest.getEmail());
        try {
            LoginResponseDTO result = userService.login(loginRequest);
            log.info("Đăng nhập thành công. Email: {}", loginRequest.getEmail());
            return ResponseEntity.ok(new ApiResponseDTO(true, "Đăng nhập thành công", result));
        } catch (BadRequestException e) {
            log.warn("Đăng nhập thất bại. Email: {}, Lỗi: {}", loginRequest.getEmail(), e.getMessage());
            return ResponseEntity.badRequest()
                .body(new ApiResponseDTO(false, e.getMessage(), null));
        } catch (Exception e) {
            log.error("Lỗi không mong muốn khi đăng nhập. Email: {}, Lỗi: {}", loginRequest.getEmail(), e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponseDTO(false, "Lỗi server: " + e.getMessage(), null));
        }
    }

    @GetMapping()
    public ResponseEntity<ApiResponseDTO> getAllUsers() {
        log.info("Bắt đầu lấy danh sách tất cả người dùng");
        try {
            List<UserResponseDTO> users = userService.getAllUsers();
            log.info("Lấy danh sách người dùng thành công. Số lượng: {}", users.size());
            return ResponseEntity.ok(new ApiResponseDTO(true, "Lấy danh sách người dùng thành công", users));
        } catch (BadRequestException e) {
            log.warn("Lỗi khi lấy danh sách người dùng: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(new ApiResponseDTO(false, e.getMessage(), null));
        }
    }

    @PutMapping("/update")
    public ResponseEntity<ApiResponseDTO> updateUser(@Valid @RequestBody UpdateUserRequest req) {
        log.info("Bắt đầu cập nhật thông tin người dùng. UserId: {}", req.getId());
        try {
            UserResponseDTO updatedUser = userService.updateUser(req);
            log.info("Cập nhật thông tin người dùng thành công. UserId: {}", req.getId());
            return ResponseEntity.ok(new ApiResponseDTO(true, "Cập nhật thông tin người dùng thành công", updatedUser));
        } catch (BadRequestException e) {
            log.warn("Lỗi khi cập nhật thông tin người dùng: {}", e.getMessage());
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
            
            UserResponseDTO user = userService.getUserById(id);
            
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
            HttpServletRequest request) {
        log.info("Bắt đầu tải lên ảnh đại diện");
        try {
            String token = request.getHeader("Authorization").replace("Bearer ", "");
            String userId = jwtUtil.getUserIdFromToken(token);
            UserResponseDTO user = userService.getUserById(userId);
            
            AvatarDTO avatarDTO = imageProcessingService.processAndUploadAvatar(file, user);
            userService.updateUserAvatar(userId, avatarDTO);
            
            log.info("Tải lên ảnh đại diện thành công. UserId: {}", userId);
            return ResponseEntity.ok(new ApiResponseDTO(true, "Tải lên ảnh đại diện thành công", avatarDTO));
        } catch (BadRequestException e) {
            log.warn("Lỗi khi tải lên ảnh đại diện: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(new ApiResponseDTO(false, e.getMessage(), null));
        } catch (IOException e) {
            log.error("Lỗi khi xử lý ảnh: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponseDTO(false, "Lỗi khi xử lý ảnh: " + e.getMessage(), null));
        }
    }

    @GetMapping("/paged")
    public ResponseEntity<ApiResponseDTO> getUsersPaged(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int pageSize) {
        log.info("Bắt đầu lấy danh sách người dùng phân trang. Trang: {}, Kích thước: {}", current, pageSize);
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

            log.info("Lấy danh sách người dùng phân trang thành công. Tổng số: {}", page.getTotalElements());
            return ResponseEntity.ok(new ApiResponseDTO(true, "Lấy danh sách người dùng thành công", data));
        } catch (BadRequestException e) {
            log.warn("Lỗi khi lấy danh sách người dùng phân trang: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(new ApiResponseDTO(false, e.getMessage(), null));
        }
    }

    @GetMapping("/account")
    public ResponseEntity<ApiResponseDTO> getCurrentUser() {
        log.info("Bắt đầu lấy thông tin người dùng hiện tại");
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                throw new BadRequestException("User not authenticated");
            }
            String email = authentication.getName();
            UserResponseDTO user = userService.getUserByEmail(email);
            
            log.info("Lấy thông tin người dùng hiện tại thành công. Email: {}", email);
            return ResponseEntity.ok(new ApiResponseDTO(true, "Lấy thông tin người dùng thành công", user));
        } catch (BadRequestException e) {
            log.warn("Lỗi khi lấy thông tin người dùng hiện tại: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(new ApiResponseDTO(false, e.getMessage(), null));
        } catch (Exception e) {
            log.error("Lỗi xác thực token: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiResponseDTO(false, "Token không hợp lệ hoặc đã hết hạn", null));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponseDTO> logout() {
        log.info("Bắt đầu đăng xuất");
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                throw new BadRequestException("User not authenticated");
            }
            log.info("Đăng xuất thành công");
            return ResponseEntity.ok(new ApiResponseDTO(true, "Đăng xuất thành công", null));
        } catch (BadRequestException e) {
            log.warn("Lỗi khi đăng xuất: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(new ApiResponseDTO(false, e.getMessage(), null));
        } catch (Exception e) {
            log.error("Lỗi xác thực token khi đăng xuất: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiResponseDTO(false, "Token không hợp lệ hoặc đã hết hạn", null));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDTO> getUserById(@PathVariable String id) {
        log.info("Bắt đầu lấy thông tin người dùng. UserId: {}", id);
        try {
            UserResponseDTO userResponse = userService.getUserById(id);
            log.info("Lấy thông tin người dùng thành công. UserId: {}", id);
            return ResponseEntity.ok(new ApiResponseDTO(true, "Lấy thông tin người dùng thành công", userResponse));
        } catch (BadRequestException e) {
            log.warn("Lỗi khi lấy thông tin người dùng: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponseDTO(false, e.getMessage(), null));
        }
    }

    @PostMapping("/create")
    public ResponseEntity<ApiResponseDTO> createUser(@Valid @RequestBody CreateUserRequest request) {
        log.info("Bắt đầu tạo tài khoản mới. Email: {}", request.getEmail());
        try {
            // Validate role
            User.UserRole role;
            try {
                role = User.UserRole.valueOf(request.getRole());
            } catch (IllegalArgumentException e) {
                log.warn("Vai trò không hợp lệ: {}", request.getRole());
                return ResponseEntity.badRequest()
                    .body(new ApiResponseDTO(false, "Vai trò không hợp lệ", null));
            }

            // Convert request to DTO
            UserRequestDTO userRequest = new UserRequestDTO();
            userRequest.setFullName(request.getFullName());
            userRequest.setEmail(request.getEmail());
            userRequest.setPassword(request.getPassword());
            userRequest.setPhone(request.getPhoneNumber());

            // Create user
            UserResponseDTO result = userService.createUser(userRequest, role);
            log.info("Tạo tài khoản thành công. UserId: {}", result.getId());
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponseDTO(true, "Tạo tài khoản thành công", result));
        } catch (BadRequestException e) {
            log.warn("Lỗi khi tạo tài khoản: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(new ApiResponseDTO(false, e.getMessage(), null));
        } catch (Exception e) {
            log.error("Lỗi không mong muốn khi tạo tài khoản: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponseDTO(false, "Lỗi server khi tạo tài khoản", null));
        }
    }
} 