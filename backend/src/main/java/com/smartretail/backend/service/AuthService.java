package com.smartretail.backend.service;

import com.smartretail.backend.dto.LoginRequest;
import com.smartretail.backend.dto.LoginResponse;
import com.smartretail.backend.dto.SignupRequest;
import com.smartretail.backend.models.User;

public interface AuthService {
    User signup(SignupRequest request);
    LoginResponse login(LoginRequest request);
    User getUserByEmail(String email);
    String refreshToken(String refreshToken);

    // --- ADD THIS NEW METHOD ---
    LoginResponse processOAuth2Login(String email, String name);
}