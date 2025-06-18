package com.project2.BookStore.service.impl;

import com.project2.BookStore.model.User;
import com.project2.BookStore.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;

@Slf4j
@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.info("Loading user by email: {}", email);
        
        User user = userRepository.findByEmail(email);
        if (user == null) {
            log.warn("User not found with email: {}", email);
            throw new UsernameNotFoundException("User not found with email: " + email);
        }

        if (!user.isActive()) {
            log.warn("User account is inactive: {}", email);
            throw new UsernameNotFoundException("User account is inactive: " + email);
        }

        log.info("User found: {}", email);
        return new org.springframework.security.core.userdetails.User(
            user.getEmail(),
            user.getPassword(),
            Collections.singletonList(new SimpleGrantedAuthority(user.getRole().name()))
        );
    }
} 