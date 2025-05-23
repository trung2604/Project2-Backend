package com.project2.BookStore.service.impl;

import com.project2.BookStore.dto.RegisterRequest;
import com.project2.BookStore.dto.LoginRequest;
import com.project2.BookStore.dto.UserResponseDTO;
import com.project2.BookStore.dto.LoginResponseDTO;
import com.project2.BookStore.dto.UpdateUserRequest;
import com.project2.BookStore.dto.AvatarDTO;
import com.project2.BookStore.model.User;
import com.project2.BookStore.repository.UserRepository;
import com.project2.BookStore.service.UserService;
import com.project2.BookStore.exception.BadRequestException;
import com.project2.BookStore.util.JwtUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public UserResponseDTO register(RegisterRequest registerRequest) {
        // Chỉ kiểm tra email đã tồn tại
        if (userRepository.findByEmail(registerRequest.getEmail()) != null) {
            throw new BadRequestException("Email " + registerRequest.getEmail() + " đã tồn tại trong hệ thống, vui lòng sử dụng email khác");
        }
        User user = new User();
        user.setId(UUID.randomUUID().toString());
        user.setFullName(registerRequest.getFullName());
        user.setEmail(registerRequest.getEmail());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setPhone(registerRequest.getPhoneNumber());
        user.setRole("ROLE_USER");
        user.setActive(true);
        user.setCreatedAt(new Date());
        user.setUpdatedAt(new Date());
        User savedUser = userRepository.save(user);
        return new UserResponseDTO(savedUser);
    }

    @Override
    public LoginResponseDTO login(LoginRequest loginRequest) {
        User user = userRepository.findByEmail(loginRequest.getEmail());
        if (user == null || !passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            throw new BadRequestException("Email hoặc mật khẩu không đúng");
        }
        String token = jwtUtil.generateToken(user.getEmail(), user.getRole(), user.getId());
        UserResponseDTO userResponseDTO = new UserResponseDTO(user);
        return new LoginResponseDTO(token, userResponseDTO);
    }
    @Override
    public List<UserResponseDTO> getAllUsers() {
        return userRepository.getAllUsers();
    }

    @Override
    public UserResponseDTO updateUser(UpdateUserRequest req) {
        User user = userRepository.findById(req.getId())
            .orElseThrow(() -> new BadRequestException("ID không tồn tại"));
            
        if ("ROLE_ADMIN".equalsIgnoreCase(user.getRole())) {
            throw new BadRequestException("Không thể cập nhật thông tin tài khoản admin!");
        }
        
        user.setFullName(req.getFullName());
        user.setPhone(req.getPhone());
        user.setUpdatedAt(new Date());
        User saved = userRepository.save(user);
        return new UserResponseDTO(saved);
    }

    @Override
    public void deleteUser(String id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new BadRequestException("User không tồn tại"));
        if ("ROLE_ADMIN".equalsIgnoreCase(user.getRole())) {
            throw new BadRequestException("Không thể xóa tài khoản admin!");
        }
        userRepository.deleteById(id);
    }

    @Override
    public void updateUserAvatar(String userId, AvatarDTO avatarDTO) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new BadRequestException("User không tồn tại"));
        User.Avatar avatar = new User.Avatar();
        avatar.setThumbnail(avatarDTO.getThumbnail());
        avatar.setMedium(avatarDTO.getMedium());
        avatar.setOriginal(avatarDTO.getOriginal());
        avatar.setFormat(avatarDTO.getFormat());
        avatar.setSize(avatarDTO.getSize());
        user.setAvatar(avatar);
        user.setUpdatedAt(new Date());
        userRepository.save(user);
    }

    @Override
    public User getUserById(String userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new BadRequestException("User không tồn tại"));
    }

    @Override
    public Page<UserResponseDTO> getUsersPaged(Pageable pageable) {
        return userRepository.findAll(pageable).map(UserResponseDTO::new);
    }

    @Override
    public UserResponseDTO getUserByEmail(String email) {
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new BadRequestException("Không tìm thấy user với email: " + email);
        }
        return new UserResponseDTO(user);
    }
}
