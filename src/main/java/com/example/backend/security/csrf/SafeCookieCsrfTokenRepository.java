package com.example.backend.security.csrf;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.util.StringUtils;

/**
 * CookieCsrfTokenRepository that keeps the issued XSRF cookie when Spring Security attempts
 * to clear the token for safe (GET/HEAD/OPTIONS/TRACE) requests.
 */
public class SafeCookieCsrfTokenRepository extends CookieCsrfTokenRepository {

    @Override
    public void saveToken(CsrfToken token, HttpServletRequest request, HttpServletResponse response) {
        if (token == null && isSafeMethod(request)) {
            return;
        }
        super.saveToken(token, request, response);
    }

    private boolean isSafeMethod(HttpServletRequest request) {
        String method = request.getMethod();
        if (!StringUtils.hasText(method)) {
            return false;
        }
        return switch (method.toUpperCase()) {
            case "GET", "HEAD", "OPTIONS", "TRACE" -> true;
            default -> false;
        };
    }
}
