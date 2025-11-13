package com.example.backend.dto.admin;

import com.example.backend.entity.Role;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserDTO {

    private Long userId;
    private String userName; 
    private String userEmail;
    private Role role;
    private Long postCount;
    private Long commentCount;
    private Long characterCount;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt; 
    private Boolean isDeleted;

    public static String maskEmail(String email) {
        if (email == null || email.length() < 5) {
            return "***";
        }

        int atIndex = email.indexOf('@');
        if (atIndex < 3) {
            return "***" + email.substring(atIndex);
        }

        String localPart = email.substring(0, atIndex);
        String domain = email.substring(atIndex);

        String masked = localPart.substring(0, Math.min(3, localPart.length())) + "***";
        return masked + domain;
    }

    public static String maskName(String name) {
        if (name == null || name.length() < 2) {
            return "*";
        }

        if (name.length() == 2) {
            return name.charAt(0) + "*";
        }

        return name.charAt(0) + "*" + name.charAt(name.length() - 1);
    }
}
