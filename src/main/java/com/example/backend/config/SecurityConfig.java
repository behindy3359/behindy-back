package com.example.backend.config;

import com.example.backend.security.csrf.SafeCookieCsrfTokenRepository;
import com.example.backend.security.filter.CsrfDebugLoggingFilter;
import com.example.backend.security.filter.InternalApiKeyFilter;
import com.example.backend.security.jwt.JwtAuthenticationEntryPoint;
import com.example.backend.security.jwt.JwtAuthenticationFilter;
import com.example.backend.security.service.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final CustomUserDetailsService userDetailsService;
    private final JwtAuthenticationEntryPoint unauthorizedHandler;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final InternalApiKeyFilter internalApiKeyFilter;
    private final CsrfDebugLoggingFilter csrfDebugLoggingFilter;

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOriginPatterns(Arrays.asList(
                "http://behindy.me",
                "https://behindy.me",
                "http://localhost:3000"
        ));

        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"
        ));

        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public CookieCsrfTokenRepository cookieCsrfTokenRepository() {
        CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repository.setCookieName("XSRF-TOKEN");
        repository.setHeaderName("X-XSRF-TOKEN");
        repository.setCookiePath("/");
        repository.setCookieHttpOnly(false);  // JavaScript에서 읽을 수 있도록 false로 설정
        repository.setCookieMaxAge((int) java.time.Duration.ofHours(1).toSeconds());
        return repository;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           CookieCsrfTokenRepository cookieCsrfTokenRepository) throws Exception {
        CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();
        requestHandler.setCsrfRequestAttributeName("_csrf");
        CsrfTokenRepository safeRepository = new SafeCookieCsrfTokenRepository(cookieCsrfTokenRepository);

        http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(safeRepository)
                        .csrfTokenRequestHandler(requestHandler)
                        .ignoringRequestMatchers(
                                // 공개 API - CSRF 검증 제외
                                "/api/auth/**",
                                "/api/public/**",
                                "/api/metro/**",
                                "/test/**",
                                "/error",
                                "/",
                                // Swagger UI - CSRF 검증 제외
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/swagger-resources/**",
                                "/webjars/**",
                                // 내부 API
                                "/api/ai-stories/internal/**",
                                // 조회 API - GET 요청은 일반적으로 CSRF 대상이 아님
                                "/api/posts",
                                "/api/posts/**",
                                "/api/comments/**",
                                "/api/stories/**",
                                "/api/ai-stories/health"
                        )
                )
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Swagger UI 및 API 문서
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/swagger-resources/**",
                                "/webjars/**"
                        ).permitAll()

                        // 공개 엔드포인트
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/public/**").permitAll()
                        .requestMatchers("/api/metro/**").permitAll()
                        .requestMatchers("/test/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/").permitAll()

                        // 조회 API
                        .requestMatchers(HttpMethod.GET, "/api/posts").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/posts/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/comments/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/stories/**").permitAll()

                        // 내부 API
                        .requestMatchers("/api/ai-stories/internal/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/security/csrf-token").permitAll()

                        .requestMatchers(HttpMethod.GET, "/api/ai-stories/health").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // 인증 필요한 엔드포인트들
                        .requestMatchers(HttpMethod.POST, "/api/posts").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/posts/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/posts/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/comments").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/comments/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/comments/**").authenticated()
                        .requestMatchers("/api/characters/**").authenticated()
                        .requestMatchers("/api/game/**").authenticated()
                        .requestMatchers("/api/ai-stories/**").authenticated()
                        .anyRequest().authenticated()
                );

        http.authenticationProvider(authenticationProvider());

        http.addFilterBefore(csrfDebugLoggingFilter, CsrfFilter.class);
        http.addFilterBefore(internalApiKeyFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
