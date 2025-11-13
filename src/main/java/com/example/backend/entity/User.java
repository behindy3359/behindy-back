package com.example.backend.entity;

import com.example.backend.security.crypto.TableCryptoConverter;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity(name = "User")
@Getter@Setter @NoArgsConstructor @AllArgsConstructor @Builder @EntityListeners(AuditingEntityListener.class)
@Table(name = "users",
    indexes = {
        @Index(name = "idx_user_email", columnList = "user_email"),
        @Index(name = "idx_user_role", columnList = "role"),
        @Index(name = "idx_user_deleted_at", columnList = "deleted_at"),
        @Index(name = "idx_user_active", columnList = "deleted_at, created_at DESC")
    }
)
public class User {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "user_name", nullable = false)
    @Convert(converter = TableCryptoConverter.class)
    private String userName;

    @Column(name = "user_password", nullable = false)
    private String userPassword;

    @Column(name = "user_email")
    @Convert(converter = TableCryptoConverter.class)
    private String userEmail;

    @CreatedDate
    @Column(name="created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    @Builder.Default
    private Role role = Role.ROLE_USER;

    @OneToMany(mappedBy = "user")
    @Builder.Default
    private List<Post> posts = new ArrayList<>();

    @OneToMany(mappedBy = "user")
    @Builder.Default
    private List<Comment> comments = new ArrayList<>();

    @OneToMany(mappedBy = "user")
    @Builder.Default
    private List<Character> characters = new ArrayList<>();

    @OneToMany(mappedBy = "user")
    @Builder.Default
    private List<OpsLogA> opsLogAS = new ArrayList<>();

    @OneToMany(mappedBy = "user")
    @Builder.Default
    private List<RefreshToken> refreshTokens = new ArrayList<>();
}