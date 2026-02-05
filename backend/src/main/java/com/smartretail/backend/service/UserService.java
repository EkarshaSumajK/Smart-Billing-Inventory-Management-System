package com.smartretail.backend.service;

import com.smartretail.backend.dto.SignupRequest;
import com.smartretail.backend.models.User;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.List;

public interface UserService extends UserDetailsService {
    User getUserById(String id);
    User createUser(SignupRequest request);
    List<User> getAllUsers();
    void deleteUser(String id);
    User getUserByEmail(String email);
}