package com.project2.BookStore.repository;

import com.project2.BookStore.dto.UserResponseDTO;
import com.project2.BookStore.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    User findByEmail(String email);
    User findByPhone(String phone);
    
    @Query("SELECT new com.project2.BookStore.dto.UserResponseDTO(u) FROM User u WHERE u.active = true")
    List<UserResponseDTO> getAllUsers();
}