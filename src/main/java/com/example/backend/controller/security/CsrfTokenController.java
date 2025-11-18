package com.example.backend.controller.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/security")
@RequiredArgsConstructor
public class CsrfTokenController {

    private final CookieCsrfTokenRepository csrfTokenRepository;

    @GetMapping("/csrf-token")
    public ResponseEntity<Map<String, String>> getCsrfToken(HttpServletRequest request,
                                                           HttpServletResponse response) {
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrfToken == null) {
            csrfToken = csrfTokenRepository.generateToken(request);
            csrfTokenRepository.saveToken(csrfToken, request, response);
        }

        return ResponseEntity.ok(Map.of(
                "token", csrfToken.getToken()
        ));
    }
}
