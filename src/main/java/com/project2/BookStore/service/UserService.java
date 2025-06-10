package com.project2.BookStore.service;

import com.project2.BookStore.dto.RegisterRequest;
import com.project2.BookStore.dto.LoginRequest;
import com.project2.BookStore.dto.UserRequestDTO;
import com.project2.BookStore.dto.UserResponseDTO;
import com.project2.BookStore.dto.LoginResponseDTO;
import com.project2.BookStore.dto.UpdateUserRequest;
import com.project2.BookStore.dto.AvatarDTO;
import com.project2.BookStore.model.User;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {
    UserResponseDTO register(RegisterRequest registerRequest);
    LoginResponseDTO login(LoginRequest loginRequest);
    UserResponseDTO createUser(UserRequestDTO request, User.UserRole role);
    List<UserResponseDTO> getAllUsers();
    UserResponseDTO updateUser(UpdateUserRequest updateUserRequest);
    void deleteUser(String id);
    void updateUserAvatar(String userId, AvatarDTO avatarDTO);
    UserResponseDTO getUserById(String userId);
    UserResponseDTO getUserById(String userId, boolean checkActive);
    Page<UserResponseDTO> getUsersPaged(Pageable pageable);
    UserResponseDTO getUserByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);
}
