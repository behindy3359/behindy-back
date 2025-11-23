package com.example.backend.security.csrf;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.util.StringUtils;

/**
 * Wrapper around {@link CookieCsrfTokenRepository} that avoids deleting the CSRF cookie
 * when Spring Security tries to save a {@code null} token for safe (e.g. GET) requests.
 * This prevents accidental cookie deletion when a GET request happens to include a stale header.
 */
public class SafeCookieCsrfTokenRepository implements CsrfTokenRepository {

    private final CookieCsrfTokenRepository delegate;

    public SafeCookieCsrfTokenRepository() {
        this.delegate = CookieCsrfTokenRepository.withHttpOnlyFalse();
    }

    @Override
    public CsrfToken generateToken(HttpServletRequest request) {
        return delegate.generateToken(request);
    }

    @Override
    public void saveToken(CsrfToken token, HttpServletRequest request, HttpServletResponse response) {
        if (token == null && isSafeMethod(request)) {
            // Don't clear cookie on GET/HEAD/etc. to keep issued token alive
            return;
        }
        delegate.saveToken(token, request, response);
    }

    @Override
    public CsrfToken loadToken(HttpServletRequest request) {
        return delegate.loadToken(request);
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

    public void setCookieName(String cookieName) {
        delegate.setCookieName(cookieName);
    }

    public void setHeaderName(String headerName) {
        delegate.setHeaderName(headerName);
    }

    public void setCookiePath(String path) {
        delegate.setCookiePath(path);
    }

    public void setCookieHttpOnly(boolean httpOnly) {
        delegate.setCookieHttpOnly(httpOnly);
    }

    public void setCookieMaxAge(int cookieMaxAge) {
        delegate.setCookieMaxAge(cookieMaxAge);
    }
}
