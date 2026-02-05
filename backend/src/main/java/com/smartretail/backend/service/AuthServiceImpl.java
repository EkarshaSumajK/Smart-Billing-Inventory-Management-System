package com.smartretail.backend.service;

import com.smartretail.backend.dto.LoginRequest;
import com.smartretail.backend.dto.LoginResponse;
import com.smartretail.backend.dto.SignupRequest;
import com.smartretail.backend.models.RefreshToken;
import com.smartretail.backend.models.User;
import com.smartretail.backend.repository.RefreshTokenRepository;
import com.smartretail.backend.repository.ShopRepository;
import com.smartretail.backend.repository.UserRepository;
import com.smartretail.backend.models.Shop;
import com.smartretail.backend.security.JwtUtil;
// --- IMPORT THE INTERFACE ---
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Optional;

import java.util.Date;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final ShopRepository shopRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtil jwtUtil;
    // --- USE THE INTERFACE ---
    private final PasswordEncoder passwordEncoder;
    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);

    // --- UPDATE THE CONSTRUCTOR ---
    public AuthServiceImpl(UserRepository userRepository,
            ShopRepository shopRepository,
            RefreshTokenRepository refreshTokenRepository,
            JwtUtil jwtUtil,
            PasswordEncoder passwordEncoder) { // <-- INJECT IT HERE
        this.userRepository = userRepository;
        this.shopRepository = shopRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder; // <-- ASSIGN THE INJECTED BEAN
    }

    @Override
    public User signup(SignupRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("User already exists: " + request.getEmail());
        }
        User user = new User();
        user.setEmail(request.getEmail());
        user.setName(request.getName());
        user.setRole(request.getRole());
        user.setPassword(passwordEncoder.encode(request.getPassword())); // Now uses the correct bean
        user.setPassword(passwordEncoder.encode(request.getPassword())); // Now uses the correct bean

        // Create Shop
        Shop shop = new Shop(request.getShopName(), null); // Owner ID will be set after saving user? No, need user ID
                                                           // first or save shop first?
        // Actually, let's save user first to get ID, then save shop with ownerID, then
        // update user with shopID?
        // Or generate IDs manually? MongoDB generates IDs on save.
        // Let's save Shop first with null owner, then User with ShopID, then update
        // Shop with OwnerID.

        shop = shopRepository.save(shop);

        user.setShopId(shop.getId());
        user = userRepository.save(user);

        shop.setOwnerId(user.getId());
        shopRepository.save(shop);

        return user;
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found: " + request.getEmail()));

        // DEBUG: Log user details
        System.out.println("[DEBUG] User found: " + user.getEmail());
        System.out.println("[DEBUG] User shopId: " + user.getShopId());
        System.out.println("[DEBUG] User shopId is null: " + (user.getShopId() == null));
        System.out.println("[DEBUG] User shopId isEmpty: " + (user.getShopId() != null && user.getShopId().isEmpty()));

        // This check will now work
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        // Handle legacy users without shopId
        if (user.getShopId() == null || user.getShopId().isEmpty()) {
            throw new RuntimeException(
                    "User account is not associated with any shop. Please contact administrator or create a new account.");
        }

        String shopName = shopRepository.findById(user.getShopId()).map(Shop::getName).orElse("Unknown Shop");
        String accessToken = jwtUtil.generateToken(user.getEmail(), user.getRole(), user.getShopId(), shopName);
        String refreshToken = jwtUtil.generateRefreshToken(user.getEmail());

        refreshTokenRepository.save(
                new RefreshToken(user.getId(), refreshToken,
                        new Date(System.currentTimeMillis() + jwtUtil.getExpiration() * 2)));

        return new LoginResponse(accessToken, refreshToken);
    }

    @Transactional
    @Override
    public LoginResponse processOAuth2Login(String email, String name) {
        Optional<User> userOptional = userRepository.findByEmail(email);

        User user;
        if (userOptional.isPresent()) {
            // User exists
            user = userOptional.get();
            if (name != null && !name.equals(user.getName())) {
                user.setName(name);
            }
            user.setLastLogin(new Date());
            user = userRepository.save(user);
            logger.info("[OAuth] Existing user logged in: {}", email);
        } else {
            // New user, create them
            logger.info("[OAuth] Creating new user: {}", email);
            user = new User();
            user.setEmail(email);
            user.setName(name);
            user.setRole("CASHIER"); // Default role
            user.setPassword(null); // No password for OAuth-only users
            user.setCreatedAt(new Date());
            user.setLastLogin(new Date());
            user.setUserId("u" + (System.currentTimeMillis() % 10000));
            user = userRepository.save(user);
        }

        // Generate tokens
        String shopId = user.getShopId();
        String shopName = shopRepository.findById(shopId).map(Shop::getName).orElse("Unknown Shop");
        String accessToken = jwtUtil.generateToken(user.getEmail(), user.getRole(), shopId, shopName);
        String refreshToken = jwtUtil.generateRefreshToken(user.getEmail());

        // Save refresh token
        refreshTokenRepository.deleteByUserId(user.getId());
        refreshTokenRepository.save(
                new RefreshToken(user.getId(), refreshToken,
                        new Date(System.currentTimeMillis() + jwtUtil.getExpiration() * 2)));

        return new LoginResponse(accessToken, refreshToken);
    }

    @Override
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
    }

    @Override
    public String refreshToken(String refreshToken) {
        RefreshToken storedToken = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));

        if (storedToken.getExpiryDate().before(new Date())) {
            refreshTokenRepository.deleteById(storedToken.getId());
            throw new RuntimeException("Refresh token expired");
        }

        User user = userRepository.findById(storedToken.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found: " + storedToken.getUserId()));

        String shopName = shopRepository.findById(user.getShopId()).map(Shop::getName).orElse("Unknown Shop");
        return jwtUtil.generateToken(user.getEmail(), user.getRole(), user.getShopId(), shopName);
    }
}