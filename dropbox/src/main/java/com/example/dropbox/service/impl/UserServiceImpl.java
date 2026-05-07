package com.example.dropbox.service.impl;

import java.time.LocalDateTime;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.example.dropbox.dto.CreateUserRequest;
import com.example.dropbox.dto.CreateUserResponse;
import com.example.dropbox.dto.LoginRequest;
import com.example.dropbox.dto.LoginResponse;
import com.example.dropbox.entity.User;
import com.example.dropbox.repository.UserRepository;
import com.example.dropbox.service.UserService;
import com.github.f4b6a3.uuid.UuidCreator;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public CreateUserResponse createUser(CreateUserRequest request) {
        validateCreateRequest(request);

        String email = request.email().trim().toLowerCase();

        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Email already exists: " + email);
        }

        User user = new User();
        user.setId(UuidCreator.getTimeOrderedEpoch());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName() == null ? null : request.fullName().trim());
        user.setStorageUsed(0L);
        user.setCreatedAt(LocalDateTime.now());

        userRepository.save(user);

        return new CreateUserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getStorageUsed(),
                user.getCreatedAt());
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        validateLoginRequest(request);

        String email = request.email().trim().toLowerCase();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        return new LoginResponse(user.getId(), user.getEmail(), user.getFullName());
    }

    private static void validateCreateRequest(CreateUserRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request body is required");
        }
        if (!StringUtils.hasText(request.email())) {
            throw new IllegalArgumentException("email is required");
        }
        if (!StringUtils.hasText(request.password())) {
            throw new IllegalArgumentException("password is required");
        }
        if (request.password().length() < 8) {
            throw new IllegalArgumentException("password must be at least 8 characters");
        }
    }

    private static void validateLoginRequest(LoginRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request body is required");
        }
        if (!StringUtils.hasText(request.email())) {
            throw new IllegalArgumentException("email is required");
        }
        if (!StringUtils.hasText(request.password())) {
            throw new IllegalArgumentException("password is required");
        }
    }
}
