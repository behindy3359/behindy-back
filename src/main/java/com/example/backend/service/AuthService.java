package com.example.backend.service;

import com.example.backend.config.DemoAccountConfig;
import com.example.backend.dto.auth.JwtAuthResponse;
import com.example.backend.dto.auth.LoginRequest;
import com.example.backend.dto.auth.SignupRequest;
import com.example.backend.dto.auth.TokenRefreshRequest;
import com.example.backend.entity.User;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.exception.TokenRefreshException;
import com.example.backend.repository.UserRepository;
import com.example.backend.security.jwt.JwtTokenProvider;
import com.example.backend.security.user.CustomUserDetails;
import com.example.backend.util.HtmlSanitizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final RedisService redisService;
    private final HtmlSanitizer htmlSanitizer;
    private final DemoAccountConfig demoAccountConfig;

    private static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";
    private static final int REFRESH_TOKEN_COOKIE_MAX_AGE = 7 * 24 * 60 * 60;
    private static final String COOKIE_PATH = "/";

    @Transactional
    public User register(SignupRequest request) {
        if (userRepository.existsByUserEmail(request.getEmail())) {
            throw new RuntimeException("이미 사용 중인 이메일입니다.");
        }

        String sanitizedName = htmlSanitizer.sanitize(request.getName());
        String sanitizedEmail = htmlSanitizer.sanitize(request.getEmail());
        String sanitizedPassword = htmlSanitizer.sanitize(request.getPassword());

        User user = User.builder()
                .userName(sanitizedName)
                .userEmail(sanitizedEmail)
                .userPassword(passwordEncoder.encode(sanitizedPassword))
                .build();

        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        return userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userDetails.getId()));
    }

    @Transactional
    public JwtAuthResponse authenticate(LoginRequest request, HttpServletResponse response) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        redisService.deleteAllRefreshTokensForUser(String.valueOf(userDetails.getId()));

        String accessToken = tokenProvider.generateAccessToken(authentication);
        String refreshToken = tokenProvider.generateRefreshToken(userDetails.getId());
        String jti = tokenProvider.getJtiFromToken(refreshToken);

        saveRefreshTokenToRedis(userDetails.getId(), jti, refreshToken);
        setRefreshTokenCookie(response, refreshToken);

        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userDetails.getId()));

        log.info("Login successful: userId={}", userDetails.getId());

        return JwtAuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(null)
                .userId(userDetails.getId())
                .name(userDetails.getName())
                .email(userDetails.getEmail())
                .role(user.getRole().name().replace("ROLE_", ""))
                .build();
    }

    @Transactional
    public JwtAuthResponse refreshToken(HttpServletRequest request, HttpServletResponse response) {
        
        String refreshToken = getRefreshTokenFromCookie(request);

        if (refreshToken == null) {
            throw new TokenRefreshException("", "Refresh token not found in cookie");
        }

        if (!tokenProvider.validateRefreshToken(refreshToken)) {
            throw new TokenRefreshException(refreshToken, "Invalid or expired refresh token");
        }

        Long userId = tokenProvider.getUserIdFromJWT(refreshToken);
        String jti = tokenProvider.getJtiFromToken(refreshToken);

        if (!redisService.isRefreshTokenValid(userId.toString(), jti)) {
            throw new TokenRefreshException(refreshToken, "Refresh token not found in cache");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        CustomUserDetails userDetails = CustomUserDetails.build(user);

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        String newAccessToken = tokenProvider.generateAccessToken(authentication);

        String newRefreshToken = tokenProvider.generateRefreshToken(userId);
        String newJti = tokenProvider.getJtiFromToken(newRefreshToken);

        redisService.deleteRefreshToken(userId.toString(), jti);
        saveRefreshTokenToRedis(userId, newJti, newRefreshToken);

        setRefreshTokenCookie(response, newRefreshToken);

        return JwtAuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(null)
                .userId(user.getUserId())
                .name(user.getUserName())
                .email(user.getUserEmail())
                .role(user.getRole().name().replace("ROLE_", ""))
                .build();
    }

    @Transactional
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        try {
            String refreshToken = getRefreshTokenFromCookie(request);

            if (refreshToken != null) {
                Long userId = tokenProvider.getUserIdFromJWT(refreshToken);
                String jti = tokenProvider.getJtiFromToken(refreshToken);

                redisService.deleteRefreshToken(userId.toString(), jti);
            }

            clearRefreshTokenCookie(response);

        } catch (Exception e) {
            clearRefreshTokenCookie(response);
        }

        SecurityContextHolder.clearContext();
    }

    private void saveRefreshTokenToRedis(Long userId, String jti, String token) {
        try {
            Date expiryDate = tokenProvider.getExpirationDateFromToken(token);
            long ttlMillis = expiryDate.getTime() - System.currentTimeMillis();

            String redisKey = "RT:" + userId + ":" + jti;
            redisService.setWithExpiration(redisKey, token, ttlMillis);

        } catch (Exception e) {
            log.error("Refresh Token Redis 저장 실패: {}", e.getMessage());
            throw new RuntimeException("리프레시 토큰 저장 중 오류가 발생했습니다.", e);
        }
    }

    private String getRefreshTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }

        return Arrays.stream(request.getCookies())
                .filter(cookie -> REFRESH_TOKEN_COOKIE_NAME.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, refreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath(COOKIE_PATH);
        cookie.setMaxAge(REFRESH_TOKEN_COOKIE_MAX_AGE);
        cookie.setAttribute("SameSite", "Strict");

        response.addCookie(cookie);
    }

    private void clearRefreshTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath(COOKIE_PATH);
        cookie.setMaxAge(0);

        response.addCookie(cookie);
    }

    @Transactional
    public JwtAuthResponse demoLogin(HttpServletResponse response) {
        List<DemoAccountConfig.DemoAccount> demoAccounts = demoAccountConfig.getAccounts();

        if (demoAccounts == null || demoAccounts.isEmpty()) {
            throw new IllegalStateException("설정된 데모 계정이 없습니다.");
        }

        for (DemoAccountConfig.DemoAccount demoAccount : demoAccounts) {
            try {
                User user = userRepository.findByUserEmail(demoAccount.getEmail())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "User", "email", demoAccount.getEmail()));

                Set<String> activeTokens = redisService.getAllRefreshTokensForUser(String.valueOf(user.getUserId()));

                if (activeTokens.isEmpty()) {
                    LoginRequest loginRequest = new LoginRequest();
                    loginRequest.setEmail(demoAccount.getEmail());
                    loginRequest.setPassword(demoAccount.getPassword());

                    return authenticate(loginRequest, response);
                }

            } catch (ResourceNotFoundException e) {
                log.warn("Demo account user not found: {}", demoAccount.getEmail());
                continue;
            } catch (Exception e) {
                log.error("Demo account check failed: {} - {}", demoAccount.getEmail(), e.getMessage());
                continue;
            }
        }

        throw new IllegalStateException("현재 모든 데모 계정이 사용 중입니다. 잠시 후 다시 시도해주세요.");
    }
}