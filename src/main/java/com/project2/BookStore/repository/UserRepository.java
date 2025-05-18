package com.project2.BookStore.repository;

import com.project2.BookStore.dto.UserResponseDTO;
import org.springframework.data.mongodb.repository.MongoRepository;
import com.project2.BookStore.model.User;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface UserRepository extends MongoRepository<User, String> {
    User findByEmail(String email);
    @Query (value = "{ 'isActive' : true }")
    List<UserResponseDTO> getAllUsers();
}