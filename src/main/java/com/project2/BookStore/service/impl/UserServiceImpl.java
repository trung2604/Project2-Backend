package com.project2.BookStore.service.impl;

import com.project2.BookStore.dto.LoginRequest;
import com.project2.BookStore.dto.LoginResponseDTO;
import com.project2.BookStore.dto.RegisterRequest;
import com.project2.BookStore.dto.UserRequestDTO;
import com.project2.BookStore.dto.UserResponseDTO;
import com.project2.BookStore.dto.UpdateUserRequest;
import com.project2.BookStore.dto.AvatarDTO;
import com.project2.BookStore.exception.BadRequestException;
import com.project2.BookStore.model.User;
import com.project2.BookStore.repository.UserRepository;
import com.project2.BookStore.service.UserService;
import com.project2.BookStore.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    @Transactional
    public UserResponseDTO register(RegisterRequest registerRequest) {
        log.info("Bắt đầu đăng ký tài khoản mới. Email: {}", registerRequest.getEmail());

        try {
            // Kiểm tra email đã tồn tại
            if (existsByEmail(registerRequest.getEmail())) {
                throw new BadRequestException("Email " + registerRequest.getEmail() + " đã tồn tại trong hệ thống");
            }

            // Kiểm tra số điện thoại đã tồn tại
            if (existsByPhone(registerRequest.getPhoneNumber())) {
                throw new BadRequestException("Số điện thoại " + registerRequest.getPhoneNumber() + " đã tồn tại trong hệ thống");
            }

            User user = new User();
            user.setId(UUID.randomUUID().toString());
            user.setFullName(registerRequest.getFullName());
            user.setEmail(registerRequest.getEmail());
            user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
            user.setPhone(registerRequest.getPhoneNumber());
            user.setRole(User.UserRole.ROLE_USER);
            user.setActive(true);

            User savedUser = userRepository.save(user);
            log.info("Đăng ký tài khoản thành công. UserId: {}", savedUser.getId());
            return new UserResponseDTO(savedUser);
        } catch (BadRequestException e) {
            log.warn("Lỗi khi đăng ký tài khoản: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Lỗi không mong muốn khi đăng ký tài khoản: {}", e.getMessage());
            throw new BadRequestException("Không thể đăng ký tài khoản: " + e.getMessage());
        }
    }

    @Override
    public LoginResponseDTO login(LoginRequest loginRequest) {
        log.info("Bắt đầu đăng nhập. Email: {}", loginRequest.getEmail());

        try {
            User user = userRepository.findByEmail(loginRequest.getEmail());
            if (user == null) {
                throw new BadRequestException("Email không tồn tại trong hệ thống");
            }

            if (!user.isActive()) {
                throw new BadRequestException("Tài khoản đã bị khóa");
            }

            if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
                throw new BadRequestException("Mật khẩu không đúng");
            }

            String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name(), user.getId());
            UserResponseDTO userResponseDTO = new UserResponseDTO(user);
            
            log.info("Đăng nhập thành công. UserId: {}", user.getId());
            return new LoginResponseDTO(token, userResponseDTO);
        } catch (BadRequestException e) {
            log.warn("Lỗi khi đăng nhập: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Lỗi không mong muốn khi đăng nhập: {}", e.getMessage());
            throw new BadRequestException("Không thể đăng nhập: " + e.getMessage());
        }
    }

    @Override
    public List<UserResponseDTO> getAllUsers() {
        log.info("Bắt đầu lấy danh sách tất cả người dùng");

        try {
            List<UserResponseDTO> users = userRepository.getAllUsers();
            log.info("Lấy danh sách người dùng thành công. Số lượng: {}", users.size());
            return users;
        } catch (Exception e) {
            log.error("Lỗi khi lấy danh sách người dùng: {}", e.getMessage());
            throw new BadRequestException("Không thể lấy danh sách người dùng: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public UserResponseDTO updateUser(UpdateUserRequest req) {
        log.info("Bắt đầu cập nhật thông tin người dùng. UserId: {}", req.getId());

        try {
            User user = userRepository.findById(req.getId())
                .orElseThrow(() -> new BadRequestException("Không tìm thấy người dùng với ID: " + req.getId()));
            
            if (user.getRole() == User.UserRole.ROLE_ADMIN) {
                throw new BadRequestException("Không thể cập nhật thông tin tài khoản admin");
            }

            // Kiểm tra số điện thoại mới có bị trùng không
            if (!user.getPhone().equals(req.getPhone())) {
                User existingUser = userRepository.findByPhone(req.getPhone());
                if (existingUser != null && !existingUser.getId().equals(user.getId())) {
                    throw new BadRequestException("Số điện thoại " + req.getPhone() + " đã tồn tại trong hệ thống");
                }
            }
            
            user.setFullName(req.getFullName());
            user.setPhone(req.getPhone());

            User saved = userRepository.save(user);
            log.info("Cập nhật thông tin người dùng thành công. UserId: {}", saved.getId());
            return new UserResponseDTO(saved);
        } catch (BadRequestException e) {
            log.warn("Lỗi khi cập nhật thông tin người dùng: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Lỗi không mong muốn khi cập nhật thông tin người dùng: {}", e.getMessage());
            throw new BadRequestException("Không thể cập nhật thông tin người dùng: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void deleteUser(String id) {
        log.info("Bắt đầu xóa người dùng. UserId: {}", id);

        try {
            User user = userRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy người dùng với ID: " + id));

            if (user.getRole() == User.UserRole.ROLE_ADMIN) {
                throw new BadRequestException("Không thể xóa tài khoản admin");
            }

            // Xóa hoàn toàn user khỏi database
            userRepository.delete(user);
            log.info("Xóa người dùng thành công. UserId: {}", id);
        } catch (BadRequestException e) {
            log.warn("Lỗi khi xóa người dùng: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Lỗi không mong muốn khi xóa người dùng: {}", e.getMessage());
            throw new BadRequestException("Không thể xóa người dùng: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void updateUserAvatar(String userId, AvatarDTO avatarDTO) {
        log.info("Bắt đầu cập nhật avatar người dùng. UserId: {}", userId);

        try {
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy người dùng với ID: " + userId));

            User.Avatar avatar = new User.Avatar();
            avatar.setThumbnail(avatarDTO.getThumbnail());
            avatar.setMedium(avatarDTO.getMedium());
            avatar.setOriginal(avatarDTO.getOriginal());
            avatar.setPublicId(avatarDTO.getPublicId());
            avatar.setFormat(avatarDTO.getFormat());
            avatar.setCreatedAt(avatarDTO.getCreatedAt());

            user.setAvatar(avatar);
            User saved = userRepository.save(user);
            log.info("Cập nhật avatar người dùng thành công. UserId: {}", saved.getId());
        } catch (BadRequestException e) {
            log.warn("Lỗi khi cập nhật avatar người dùng: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Lỗi không mong muốn khi cập nhật avatar người dùng: {}", e.getMessage());
            throw new BadRequestException("Không thể cập nhật avatar người dùng: " + e.getMessage());
        }
    }

    @Override
    public User getUserById(String userId) {
        return getUserById(userId, true);  // Default to checking active status
    }

    @Override
    public User getUserById(String userId, boolean checkActive) {
        log.info("Bắt đầu lấy thông tin người dùng. UserId: {}, checkActive: {}", userId, checkActive);

        try {
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy người dùng với ID: " + userId));

            if (checkActive && !user.isActive()) {
                throw new BadRequestException("Tài khoản đã bị khóa");
            }

            log.info("Lấy thông tin người dùng thành công. UserId: {}", userId);
            return user;
        } catch (BadRequestException e) {
            log.warn("Lỗi khi lấy thông tin người dùng: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Lỗi không mong muốn khi lấy thông tin người dùng: {}", e.getMessage());
            throw new BadRequestException("Không thể lấy thông tin người dùng: " + e.getMessage());
        }
    }

    @Override
    public Page<UserResponseDTO> getUsersPaged(Pageable pageable) {
        log.info("Bắt đầu lấy danh sách người dùng phân trang");

        try {
            Page<UserResponseDTO> users = userRepository.findAll(pageable).map(UserResponseDTO::new);
            log.info("Lấy danh sách người dùng phân trang thành công. Số lượng: {}", users.getTotalElements());
            return users;
        } catch (Exception e) {
            log.error("Lỗi khi lấy danh sách người dùng phân trang: {}", e.getMessage());
            throw new BadRequestException("Không thể lấy danh sách người dùng phân trang: " + e.getMessage());
        }
    }

    @Override
    public UserResponseDTO getUserByEmail(String email) {
        log.info("Bắt đầu lấy thông tin người dùng theo email. Email: {}", email);

        try {
            User user = userRepository.findByEmail(email);
            if (user == null) {
                throw new BadRequestException("Không tìm thấy người dùng với email: " + email);
            }

            if (!user.isActive()) {
                throw new BadRequestException("Tài khoản đã bị khóa");
            }

            log.info("Lấy thông tin người dùng theo email thành công. Email: {}", email);
            return new UserResponseDTO(user);
        } catch (BadRequestException e) {
            log.warn("Lỗi khi lấy thông tin người dùng theo email: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Lỗi không mong muốn khi lấy thông tin người dùng theo email: {}", e.getMessage());
            throw new BadRequestException("Không thể lấy thông tin người dùng theo email: " + e.getMessage());
        }
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.findByEmail(email) != null;
    }

    @Override
    public boolean existsByPhone(String phone) {
        return userRepository.findByPhone(phone) != null;
    }

    @Override
    @Transactional
    public UserResponseDTO createUser(UserRequestDTO request, User.UserRole role) {
        log.info("Bắt đầu tạo tài khoản mới. Email: {}", request.getEmail());

        try {
            // Kiểm tra email đã tồn tại
            if (existsByEmail(request.getEmail())) {
                throw new BadRequestException("Email " + request.getEmail() + " đã tồn tại trong hệ thống");
            }

            // Kiểm tra số điện thoại đã tồn tại
            if (existsByPhone(request.getPhone())) {
                throw new BadRequestException("Số điện thoại " + request.getPhone() + " đã tồn tại trong hệ thống");
            }

            User user = new User();
            user.setId(UUID.randomUUID().toString());
            user.setFullName(request.getFullName());
            user.setEmail(request.getEmail());
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            user.setPhone(request.getPhone());
            user.setRole(role);
            user.setActive(true);

            User savedUser = userRepository.save(user);
            log.info("Tạo tài khoản thành công. UserId: {}", savedUser.getId());
            return new UserResponseDTO(savedUser);
        } catch (BadRequestException e) {
            log.warn("Lỗi khi tạo tài khoản: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Lỗi không mong muốn khi tạo tài khoản: {}", e.getMessage());
            throw new BadRequestException("Không thể tạo tài khoản: " + e.getMessage());
        }
    }
}
