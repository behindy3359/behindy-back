package com.example.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter@Setter@Builder @NoArgsConstructor@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Table(name = "POST",
    indexes = {
        @Index(name = "idx_post_created_at", columnList = "created_at DESC"),
        @Index(name = "idx_post_user_id", columnList = "user_id"),
        @Index(name = "idx_post_deleted_at", columnList = "deleted_at"),
        @Index(name = "idx_post_active_list", columnList = "deleted_at, created_at DESC")
    }
)
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="post_id")
    private Long postId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name="post_title")
    private String postTitle;

    @Column(name="post_contents",columnDefinition = "TEXT")
    private String postContents;

    @OneToOne(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private PostStats stats;

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

    @OneToMany(mappedBy = "post", orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Comment> comments = new ArrayList<>();
}
