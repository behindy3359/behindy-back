package com.example.backend.controller;

import com.example.backend.dto.auth.ApiResponse;
import com.example.backend.dto.auth.JwtAuthResponse;
import com.example.backend.dto.auth.LoginRequest;
import com.example.backend.dto.auth.SignupRequest;
import com.example.backend.entity.User;
import com.example.backend.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Map;

@Tag(name = "인증 API", description = "회원가입, 로그인, 토큰 관리 등 인증 관련 API")
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(
            summary = "회원가입",
            description = """
                새로운 사용자를 등록합니다.

                **요청 조건:**
                - 이메일: 유효한 이메일 형식, 중복 불가
                - 비밀번호: 8자 이상
                - 이름: 1자 이상

                **처리 과정:**
                1. 이메일 중복 확인
                2. 비밀번호 암호화 (BCrypt)
                3. 민감 정보 암호화 (AES256)
                4. 데이터베이스 저장
                5. USER 권한 자동 부여
                """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "회원가입 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    name = "회원가입 성공",
                                    value = """
                                        {
                                          "success": true,
                                          "message": "사용자 등록이 완료되었습니다.",
                                          "data": 1
                                        }
                                        """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 - 중복된 이메일 또는 유효성 검증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    name = "중복된 이메일",
                                    value = """
                                        {
                                          "success": false,
                                          "message": "이미 사용중인 이메일입니다.",
                                          "data": null
                                        }
                                        """
                            )
                    )
            )
    })
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse> registerUser(
            @Valid @RequestBody SignupRequest signupRequest) {

        log.info("회원가입 요청: {}", signupRequest.getEmail());

        User user = authService.register(signupRequest);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.builder()
                        .success(true)
                        .message("사용자 등록이 완료되었습니다.")
                        .data(user.getUserId())
                        .build());
    }

    @Operation(
            summary = "로그인",
            description = """
                사용자 인증 후 Access Token과 Refresh Token을 발급합니다.

                **토큰 정책:**
                - **Access Token**: 15분 유효, 응답 본문에 포함
                - **Refresh Token**: 7일 유효, HttpOnly Cookie에 저장 (XSS 방지)

                **사용 방법:**
                1. 이메일과 비밀번호로 로그인
                2. 응답으로 받은 `accessToken`을 저장
                3. 이후 모든 API 요청 시 헤더에 포함:
                   ```
                   Authorization: Bearer {accessToken}
                   ```
                4. Access Token 만료 시 `/api/auth/refresh`로 갱신

                **보안 기능:**
                - Refresh Token Rotation (재사용 공격 방지)
                - JTI (JWT ID)로 토큰 고유성 보장
                - Redis에 Refresh Token 저장 (즉시 무효화 가능)
                - Rate Limiting: IP당 분당 5회 (Brute Force 방지)
                """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "로그인 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = JwtAuthResponse.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    name = "로그인 성공 예시",
                                    value = """
                                        {
                                          "accessToken": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiIxIiwiaWF0IjoxNzM...",
                                          "tokenType": "Bearer",
                                          "userId": 1,
                                          "userName": "홍길동",
                                          "email": "user@example.com"
                                        }
                                        """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패 - 이메일 또는 비밀번호 불일치",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    name = "인증 실패",
                                    value = """
                                        {
                                          "success": false,
                                          "message": "이메일 또는 비밀번호가 올바르지 않습니다.",
                                          "data": null
                                        }
                                        """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "429",
                    description = "Rate Limit 초과 - 너무 많은 로그인 시도 (IP당 분당 5회 제한)",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    name = "Rate Limit 초과",
                                    value = """
                                        {
                                          "timestamp": "2025-10-14T12:30:00",
                                          "status": 429,
                                          "error": "Too Many Requests",
                                          "message": "요청 한도를 초과했습니다. 잠시 후 다시 시도해주세요.",
                                          "path": "/api/auth/login"
                                        }
                                        """
                            )
                    )
            )
    })
    @PostMapping("/login")
    public ResponseEntity<JwtAuthResponse> authenticateUser(
            @Valid @RequestBody LoginRequest loginRequest,
            HttpServletResponse response) {

        log.info("로그인 요청: {}", loginRequest.getEmail());

        JwtAuthResponse authResponse = authService.authenticate(loginRequest, response);

        log.info("로그인 성공: userId={}", authResponse.getUserId());

        return ResponseEntity.ok(authResponse);
    }

    @Operation(
            summary = "토큰 갱신",
            description = """
                Cookie의 Refresh Token을 사용하여 새로운 Access Token과 Refresh Token을 발급받습니다.

                **Refresh Token Rotation:**
                - 기존 Refresh Token은 무효화됨 (Redis에서 삭제)
                - 새로운 Refresh Token이 Cookie에 재저장됨
                - 재사용 공격 방지 (Replay Attack Prevention)

                **사용 시점:**
                - Access Token 만료 시 (15분 후)
                - 401 Unauthorized 응답 받은 경우
                - 프론트엔드에서 자동으로 갱신 처리 권장

                **주의사항:**
                - Refresh Token도 만료된 경우 (7일) 재로그인 필요
                - Cookie가 없거나 유효하지 않으면 401 반환
                """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "토큰 갱신 성공 - 새로운 Access Token과 Refresh Token 발급",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = JwtAuthResponse.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    name = "토큰 갱신 성공",
                                    value = """
                                        {
                                          "accessToken": "eyJhbGciOiJIUzUxMiJ9.NEW_TOKEN...",
                                          "tokenType": "Bearer",
                                          "userId": 1,
                                          "userName": "홍길동",
                                          "email": "user@example.com"
                                        }
                                        """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Refresh Token이 없거나 유효하지 않음 - 재로그인 필요",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    name = "유효하지 않은 Refresh Token",
                                    value = """
                                        {
                                          "success": false,
                                          "message": "유효하지 않은 Refresh Token입니다. 다시 로그인해주세요.",
                                          "data": null
                                        }
                                        """
                            )
                    )
            )
    })
    @PostMapping("/refresh")
    public ResponseEntity<JwtAuthResponse> refreshToken(
            HttpServletRequest request,
            HttpServletResponse response) {

        log.info("토큰 갱신 요청");

        JwtAuthResponse authResponse = authService.refreshToken(request, response);

        log.info("토큰 갱신 성공: userId={}", authResponse.getUserId());

        return ResponseEntity.ok(authResponse);
    }

    @Operation(summary = "로그아웃", description = "Cookie와 Redis에서 Refresh Token을 제거하여 로그아웃합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그아웃 성공")
    })
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse> logoutUser(
            HttpServletRequest request,
            HttpServletResponse response) {

        log.info("로그아웃 요청");

        authService.logout(request, response);

        return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .message("로그아웃 되었습니다.")
                .build());
    }

    @Operation(summary = "현재 사용자 정보 조회", description = "인증된 사용자의 정보를 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "사용자 정보 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @GetMapping("/me")
    public ResponseEntity<ApiResponse> getCurrentUser() {
        try {
            User currentUser = authService.getCurrentUser();

            return ResponseEntity.ok(ApiResponse.builder()
                    .success(true)
                    .message("사용자 정보 조회 성공")
                    .data(Map.of(
                            "id", currentUser.getUserId(),
                            "name", currentUser.getUserName(),
                            "email", currentUser.getUserEmail(),
                            "role", currentUser.getRole().name().replace("ROLE_", "")
                    ))
                    .build());
        } catch (Exception e) {
            log.error("사용자 정보 조회 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.builder()
                            .success(false)
                            .message("인증이 필요합니다.")
                            .build());
        }
    }

    @Operation(summary = "토큰 상태 확인", description = "Access Token과 Refresh Token의 존재 여부를 확인합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "토큰 상태 확인 완료")
    })
    @GetMapping("/status")
    public ResponseEntity<ApiResponse> checkAuthStatus(HttpServletRequest request) {
        try {
            String authHeader = request.getHeader("Authorization");
            boolean hasAccessToken = authHeader != null && authHeader.startsWith("Bearer ");

            boolean hasRefreshToken = request.getCookies() != null &&
                    Arrays.stream(request.getCookies())
                            .anyMatch(cookie -> "refreshToken".equals(cookie.getName()));

            return ResponseEntity.ok(ApiResponse.builder()
                    .success(true)
                    .message("토큰 상태 확인 완료")
                    .data(Map.of(
                            "hasAccessToken", hasAccessToken,
                            "hasRefreshToken", hasRefreshToken,
                            "timestamp", System.currentTimeMillis()
                    ))
                    .build());
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.builder()
                    .success(false)
                    .message("토큰 상태 확인 실패")
                    .build());
        }
    }

    @Operation(
            summary = "데모 로그인",
            description = """
                세션에 연결되지 않은 사용 가능한 데모 계정으로 자동 로그인합니다.

                **동작 방식:**
                1. 설정된 데모 계정들 중 현재 활성 세션이 없는 계정을 찾음
                2. 사용 가능한 계정으로 자동 로그인 처리
                3. 일반 로그인과 동일하게 토큰 발급

                **특징:**
                - 요청 본문 없이 호출 가능
                - 사용자가 계정을 선택할 필요 없음
                - 여러 사용자가 동시에 체험 가능
                - 모든 데모 계정이 사용 중이면 503 반환
                """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "데모 로그인 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = JwtAuthResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "503",
                    description = "사용 가능한 데모 계정 없음 - 모든 계정이 사용 중",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class)
                    )
            )
    })
    
    @PostMapping("/demo-login")
    public ResponseEntity<?> demoLogin(HttpServletResponse response) {
        try {
            JwtAuthResponse authResponse = authService.demoLogin(response);
            log.info("데모 로그인 성공: userId={}", authResponse.getUserId());
            return ResponseEntity.ok(authResponse);
        } catch (IllegalStateException e) {
            log.warn("데모 로그인 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ApiResponse.builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        }
    }
}