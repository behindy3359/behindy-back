package com.example.backend.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.WebUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Component
public class CsrfDebugLoggingFilter extends OncePerRequestFilter {

    private static final Set<String> METHODS = new HashSet<>(Arrays.asList("POST", "PUT", "PATCH", "DELETE"));

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        if (METHODS.contains(request.getMethod())) {
            String headerToken = request.getHeader("X-XSRF-TOKEN");
            Cookie cookie = WebUtils.getCookie(request, "XSRF-TOKEN");
            String cookieToken = cookie != null ? cookie.getValue() : null;

            log.info("[CSRF] {} {} headerPresent={} cookiePresent={} cookieValueSnippet={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    headerToken != null,
                    cookieToken != null,
                    cookieToken != null && cookieToken.length() > 8 ? cookieToken.substring(0, 8) + "..." : cookieToken);
        }

        filterChain.doFilter(request, response);
    }
}
