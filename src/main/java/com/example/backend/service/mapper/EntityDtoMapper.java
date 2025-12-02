package com.example.backend.service.mapper;

import com.example.backend.dto.character.CharacterResponse;
import com.example.backend.dto.comment.CommentResponse;
import com.example.backend.dto.game.OptionResponse;
import com.example.backend.dto.game.PageResponse;
import com.example.backend.dto.game.StoryResponse;
import com.example.backend.dto.post.PostResponse;
import com.example.backend.entity.*;
import com.example.backend.entity.Character;
import com.example.backend.repository.CommentLikeRepository;
import com.example.backend.repository.NowRepository;
import com.example.backend.repository.OptionsRepository;
import com.example.backend.repository.PageRepository;
import com.example.backend.repository.PostStatsRepository;
import com.example.backend.security.user.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class EntityDtoMapper {

    private final OptionsRepository optionsRepository;
    private final PageRepository pageRepository;
    private final NowRepository nowRepository;
    private final PostStatsRepository postStatsRepository;
    private final CommentLikeRepository commentLikeRepository;

    public PostResponse toPostResponse(Post post) {
        if (post == null) {
            return null;
        }

        User currentUser = getCurrentUserSafely();
        boolean isOwner = isOwner(currentUser, post.getUser());

        Long viewCount = postStatsRepository.findByPostId(post.getPostId())
                .map(PostStats::getViewCount)
                .orElse(0L);

        return PostResponse.builder()
                .id(post.getPostId())
                .title(post.getPostTitle())
                .content(post.getPostContents())
                .authorName(post.getUser().getUserName())
                .authorId(post.getUser().getUserId())
                .viewCount(viewCount)
                .commentCount(post.getComments() != null ? post.getComments().size() : 0)
                .isEditable(isOwner)
                .isDeletable(isOwner)
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }

    public CommentResponse toCommentResponse(Comment comment) {
        if (comment == null) {
            return null;
        }

        User currentUser = getCurrentUserSafely();
        boolean isOwner = isOwner(currentUser, comment.getUser());

        long likeCount = commentLikeRepository.countByCommentId(comment.getCmtId());

        boolean isLiked = false;
        if (currentUser != null) {
            isLiked = commentLikeRepository.existsByCommentIdAndUserId(
                    comment.getCmtId(),
                    currentUser.getUserId()
            );
        }

        return CommentResponse.builder()
                .id(comment.getCmtId())
                .postId(comment.getPost().getPostId())
                .content(comment.getCmtContents())
                .authorName(comment.getUser().getUserName())
                .authorId(comment.getUser().getUserId())
                .isEditable(isOwner)
                .isDeletable(isOwner)
                .likeCount(likeCount)
                .isLiked(isLiked)
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }

    public CharacterResponse toCharacterResponse(com.example.backend.entity.Character character) {
        if (character == null) {
            return null;
        }

        boolean isAlive = character.getCharHealth() > 0 && character.getCharSanity() > 0;
        boolean isDying = isAlive && (character.getCharHealth() <= 20 || character.getCharSanity() <= 20);
        String statusMessage = getCharacterStatusMessage(character);

        boolean hasGameProgress = false;
        Long currentStoryId = null;

        try {
            Optional<Now> gameProgress = nowRepository.findByCharacter(character);
            if (gameProgress.isPresent()) {
                hasGameProgress = true;
                currentStoryId = gameProgress.get().getPage().getStoId();
            }
        } catch (Exception e) {
        }

        return CharacterResponse.builder()
                .charId(character.getCharId())
                .charName(character.getCharName())
                .charHealth(character.getCharHealth())
                .charSanity(character.getCharSanity())
                .isAlive(isAlive)
                .isDying(isDying)
                .statusMessage(statusMessage)
                .hasGameProgress(hasGameProgress)
                .currentStoryId(currentStoryId)
                .createdAt(character.getCreatedAt())
                .build();
    }

    public StoryResponse toStoryResponse(Story story) {
        if (story == null) {
            return null;
        }

        boolean canPlay = canPlayStory(story.getStoId());
        String playStatus = determinePlayStatus(story.getStoId(), canPlay);
        String difficulty = determineDifficulty(story.getStoLength());
        String theme = determineTheme(story.getStation().getStaLine());

        return StoryResponse.builder()
                .storyId(story.getStoId())
                .storyTitle(story.getStoTitle())
                .estimatedLength(story.getStoLength())
                .difficulty(difficulty)
                .theme(theme)
                .description(generateStoryDescription(story))
                .stationName(story.getStation().getStaName())
                .stationLine(story.getStation().getStaLine())
                .canPlay(canPlay)
                .playStatus(playStatus)
                .build();
    }

    public PageResponse toPageResponse(Page page) {
        if (page == null) {
            return null;
        }

        List<Options> options = optionsRepository.findByPageId(page.getPageId());
        List<OptionResponse> optionResponses = options.stream()
                .map(this::toOptionResponse)
                .collect(Collectors.toList());

        Long totalPages = pageRepository.countPagesByStoryId(page.getStoId());

        long nextPageNumber = page.getPageNumber() + 1;
        boolean isLastPage = !pageRepository.existsNextPage(page.getStoId(), nextPageNumber);

        return PageResponse.builder()
                .pageId(page.getPageId())
                .pageNumber(page.getPageNumber())
                .content(page.getPageContents())
                .options(optionResponses)
                .isLastPage(isLastPage)
                .totalPages(totalPages.intValue())
                .build();
    }

    public OptionResponse toOptionResponse(Options option) {
        if (option == null) {
            return null;
        }

        return OptionResponse.builder()
                .optionId(option.getOptId())
                .content(option.getOptContents())
                .effect(option.getOptEffect())
                .amount(option.getOptAmount())
                .effectPreview(createEffectPreview(option))
                .build();
    }

    private User getCurrentUserSafely() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof CustomUserDetails) {
                CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
                User user = new User();
                user.setUserId(userDetails.getId());
                user.setUserName(userDetails.getName());
                user.setUserEmail(userDetails.getEmail());
                return user;
            }
        } catch (Exception e) {
        }
        return null;
    }

    private boolean isOwner(User currentUser, User owner) {
        if (currentUser == null || owner == null) {
            return false;
        }
        return currentUser.getUserId().equals(owner.getUserId());
    }

    private String getCharacterStatusMessage(Character character) {
        if (character.getCharHealth() <= 0 || character.getCharSanity() <= 0) {
            return "사망";
        }

        if (character.getCharHealth() <= 20 || character.getCharSanity() <= 20) {
            return "주의 - 상태가 좋지 않음";
        }

        if (character.getCharHealth() >= 80 && character.getCharSanity() >= 80) {
            return "건강";
        }

        return "보통";
    }

    private boolean canPlayStory(Long storyId) {
        try {
            User currentUser = getCurrentUserSafely();
            if (currentUser == null) {
                return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String determinePlayStatus(Long storyId, boolean canPlay) {
        try {
            User currentUser = getCurrentUserSafely();
            if (currentUser == null) {
                return "로그인 필요";
            }

            if (!canPlay) {
                return "플레이 불가";
            }

            return "플레이 가능";
        } catch (Exception e) {
            return "새로운 스토리";
        }
    }

    private String determineDifficulty(Integer storyLength) {
        if (storyLength == null) {
            return "보통";
        }

        if (storyLength <= 5) {
            return "쉬움";
        } else if (storyLength >= 10) {
            return "어려움";
        } else {
            return "보통";
        }
    }

    private String determineTheme(Integer lineNumber) {
        return switch (lineNumber) {
            case 1 -> "공포";
            case 2 -> "로맨스";
            case 3 -> "미스터리";
            case 4 -> "모험";
            case 5 -> "스릴러";
            case 6 -> "코미디";
            case 7 -> "판타지";
            case 8 -> "SF";
            case 9 -> "드라마";
            default -> "일반";
        };
    }

    private String generateStoryDescription(Story story) {
        String theme = determineTheme(story.getStation().getStaLine());
        
        return String.format("%s역에서 펼쳐지는 %s 장르의 텍스트 어드벤처입니다. ",
                story.getStation().getStaName(),
                theme);
    }

    private String createEffectPreview(Options option) {
        if (option.getOptEffect() == null || option.getOptAmount() == 0) {
            return null;
        }

        String effectType = option.getOptEffect().toLowerCase();
        int amount = option.getOptAmount();

        return switch (effectType) {
            case "health" -> amount > 0 ?
                    String.format("체력 +%d", amount) :
                    String.format("체력 %d", amount);
            case "sanity" -> amount > 0 ?
                    String.format("정신력 +%d", amount) :
                    String.format("정신력 %d", amount);
            default -> null;
        };
    }
}