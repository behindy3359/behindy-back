package com.example.backend.filter;

import com.example.backend.entity.OpsLogA;
import com.example.backend.entity.User;
import com.example.backend.repository.OpsLogARepository;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AccessLoggingFilter extends OncePerRequestFilter {

    private final OpsLogARepository opsLogARepository;

    private static final List<String> EXCLUDED_PATHS = Arrays.asList(
            "/actuator",
            "/health",
            "/favicon.ico",
            "/error"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {

        String requestPath = request.getRequestURI();

        boolean shouldExclude = EXCLUDED_PATHS.stream()
                .anyMatch(requestPath::startsWith);

        if (shouldExclude) {
            filterChain.doFilter(request, response);
            return;
        }

        filterChain.doFilter(request, response);

        saveAccessLogAsync(request, response);
    }

    @Async
    protected void saveAccessLogAsync(HttpServletRequest request, HttpServletResponse response) {
        try {
            String ipAddress = getClientIpAddress(request);
            String userAgent = request.getHeader("User-Agent");
            String path = request.getRequestURI();
            String method = request.getMethod();
            String statusCode = String.valueOf(response.getStatus());

            User currentUser = getCurrentUser();

            OpsLogA accessLog = OpsLogA.builder()
                    .user(currentUser)
                    .logaAddress(ipAddress)
                    .logaAgent(userAgent != null ? userAgent : "Unknown")
                    .logaPath(path)
                    .logaMethod(method)
                    .logaStatusCode(statusCode)
                    .build();

            opsLogARepository.save(accessLog);

        } catch (Exception e) {
            log.error("Access log save failed: {}", e.getMessage());
            log.debug("Access log error details", e);
        }
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");

        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_CLUSTER_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_FORWARDED");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_VIA");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        if (ip != null && ip.contains(".")) {
            String[] parts = ip.split("\\.");
            if (parts.length == 4) {
                ip = parts[0] + "." + parts[1] + "." + parts[2] + ".XXX";
            }
        }

        return ip;
    }

    private User getCurrentUser() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication != null && authentication.isAuthenticated()
                    && !"anonymousUser".equals(authentication.getPrincipal())) {

                Object principal = authentication.getPrincipal();

                if (principal instanceof User) {
                    return (User) principal;
                } else if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
                    return null;
                }
            }
        } catch (Exception e) {
        }

        return null;
    }
}
