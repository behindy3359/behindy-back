package com.example.backend.service.admin;

import com.example.backend.dto.admin.AdminStatsDTO;
import com.example.backend.dto.admin.AdminUserDTO;
import com.example.backend.entity.User;
import com.example.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class AdminService {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final StoryRepository storyRepository;
    private final StationRepository stationRepository;
    private final CharacterRepository characterRepository;

    public AdminStatsDTO getStats() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime todayStart = now.toLocalDate().atStartOfDay();
        LocalDateTime weekStart = now.minusDays(7);

        AdminStatsDTO stats = AdminStatsDTO.builder()
                .totalUsers(userRepository.count())
                .activeUsers(userRepository.countByDeletedAtIsNull())
                .newUsersToday(userRepository.countByCreatedAtAfter(todayStart))
                .newUsersThisWeek(userRepository.countByCreatedAtAfter(weekStart))

                .totalPosts(postRepository.count())
                .activePosts(postRepository.countByDeletedAtIsNull())
                .newPostsToday(postRepository.countByCreatedAtAfter(todayStart))
                .newPostsThisWeek(postRepository.countByCreatedAtAfter(weekStart))

                .totalComments(commentRepository.count())
                .activeComments(commentRepository.countByDeletedAtIsNull())
                .newCommentsToday(commentRepository.countByCreatedAtAfter(todayStart))
                .newCommentsThisWeek(commentRepository.countByCreatedAtAfter(weekStart))

                .totalStories(storyRepository.count())
                .storiesGeneratedToday(0L)
                .storiesGeneratedThisWeek(0L)

                .totalStations(stationRepository.count())
                .totalLines(stationRepository.countDistinctStaLine())

                .serverTime(now)
                .build();

        if (stats.getActiveUsers() > 0 && stats.getActivePosts() > 0) {
            stats.setHealthyStatus();
        } else {
            stats.setWarningStatus();
        }

        log.info("Admin stats retrieved: {} active users, {} active posts",
                stats.getActiveUsers(), stats.getActivePosts());

        return stats;
    }

    public Page<AdminUserDTO> getUsers(Pageable pageable) {
        Page<User> users = userRepository.findAll(pageable);

        return users.map(user -> AdminUserDTO.builder()
                .userId(user.getUserId())
                .userName(AdminUserDTO.maskName(user.getUserName()))
                .userEmail(AdminUserDTO.maskEmail(user.getUserEmail()))
                .role(user.getRole())
                .postCount((long) user.getPosts().size())
                .commentCount((long) user.getComments().size())
                .characterCount((long) user.getCharacters().size())
                .createdAt(user.getCreatedAt())
                .isDeleted(user.getDeletedAt() != null)
                .build());
    }

    public List<AdminUserDTO> getRecentActiveUsers() {
        LocalDateTime weekStart = LocalDateTime.now().minusDays(7);
        List<User> activeUsers = userRepository.findTop10ByDeletedAtIsNullAndCreatedAtAfterOrderByCreatedAtDesc(weekStart);

        return activeUsers.stream()
                .map(user -> AdminUserDTO.builder()
                        .userId(user.getUserId())
                        .userName(AdminUserDTO.maskName(user.getUserName()))
                        .userEmail(AdminUserDTO.maskEmail(user.getUserEmail()))
                        .role(user.getRole())
                        .postCount((long) user.getPosts().size())
                        .commentCount((long) user.getComments().size())
                        .createdAt(user.getCreatedAt())
                        .isDeleted(false)
                        .build())
                .collect(Collectors.toList());
    }

    public boolean isAdmin(User user) {
        return user != null && user.getRole() != null
                && user.getRole().name().equals("ROLE_ADMIN");
    }
}
