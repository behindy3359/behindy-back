package com.example.backend.service.multiplayer;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Slf4j
@Service
public class MessageSanitizer {

    private static final int MAX_LENGTH = 100;
    private static final Pattern INJECTION_PATTERN = Pattern.compile(
        "(system|admin|prompt|ignore|instructions|forget|previous)",
        Pattern.CASE_INSENSITIVE
    );

    public String sanitize(String message) {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("메시지가 비어있습니다");
        }

        if (message.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("메시지는 " + MAX_LENGTH + "자 이하여야 합니다");
        }

        String cleaned = Jsoup.clean(message, Safelist.none());

        if (INJECTION_PATTERN.matcher(cleaned).find()) {
            log.warn("Potential prompt injection detected: {}", cleaned);
            throw new SecurityException("허용되지 않는 내용이 포함되어 있습니다");
        }

        return cleaned.trim();
    }
}
