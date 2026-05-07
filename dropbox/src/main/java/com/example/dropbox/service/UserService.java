package com.example.dropbox.service;

import com.example.dropbox.dto.CreateUserRequest;
import com.example.dropbox.dto.CreateUserResponse;
import com.example.dropbox.dto.LoginRequest;
import com.example.dropbox.dto.LoginResponse;

public interface UserService {
    CreateUserResponse createUser(CreateUserRequest request);

    LoginResponse login(LoginRequest request);
}
