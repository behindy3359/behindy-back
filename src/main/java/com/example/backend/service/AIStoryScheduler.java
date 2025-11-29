package com.example.backend.service;

import com.example.backend.entity.Station;
import com.example.backend.entity.Story;
import com.example.backend.entity.Page;
import com.example.backend.entity.Options;
import com.example.backend.repository.StationRepository;
import com.example.backend.repository.StoryRepository;
import com.example.backend.repository.PageRepository;
import com.example.backend.repository.OptionsRepository;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIStoryScheduler {

    private final StationRepository stationRepository;
    private final StoryRepository storyRepository;
    private final PageRepository pageRepository;
    private final OptionsRepository optionsRepository;

    @Qualifier("llmWebClient")
    private final WebClient llmWebClient;

    @Value("${ai.story.generation.enabled:true}")
    private Boolean storyGenerationEnabled;

    @Value("${ai.story.generation.daily-limit:5}")
    private Integer dailyGenerationLimit;

    @Value("${ai.server.timeout:900000}")
    private int aiServerTimeout;

    private final AtomicInteger dailyGeneratedCount = new AtomicInteger(0);
    private LocalDateTime lastSuccessfulGeneration = null;

    @Scheduled(fixedRateString = "${ai.story.generation.test-interval:86400000}")
    public void generateStoryBatch() {
        if (storyGenerationEnabled == null || !storyGenerationEnabled) {
            return;
        }

        if (dailyGenerationLimit == null || dailyGeneratedCount.get() >= dailyGenerationLimit) {
            return;
        }

        try {
            Station selectedStation = selectStationForGeneration();
            if (selectedStation == null) {
                return;
            }

            requestFromLLMServer(selectedStation)
                    .doOnNext(llmResponse -> {
                        if (validateLLMResponse(llmResponse)) {
                            boolean saved = saveStoryToDB(selectedStation, llmResponse);
                            if (saved) {
                                dailyGeneratedCount.incrementAndGet();
                                lastSuccessfulGeneration = LocalDateTime.now();
                                log.info("Story created: '{}' (total: {})",
                                        llmResponse.getStoryTitle(), dailyGeneratedCount.get());
                            } else {
                                log.error("DB save failed");
                            }
                        } else {
                            log.warn("LLM response validation failed");
                        }
                    })
                    .doOnError(e -> {
                        log.error("Story generation async error: {} - {}", e.getClass().getSimpleName(), e.getMessage());
                    })
                    .subscribe();

        } catch (Exception e) {
            log.error("Story generation failed: {} - {}", e.getClass().getSimpleName(), e.getMessage());
        }
    }

    private Station selectStationForGeneration() {
        try {
            List<Station> allStations = stationRepository.findAll();
            List<Station> needyStations = new ArrayList<>();

            for (Station station : allStations) {
                List<Story> stories = storyRepository.findByStation(station);
                if (stories.size() < 2) {
                    needyStations.add(station);
                }
            }

            if (needyStations.isEmpty()) {
                return null;
            }

            return needyStations.get(new Random().nextInt(needyStations.size()));
        } catch (Exception e) {
            log.error("Station selection failed: {}", e.getMessage());
            return null;
        }
    }
    private Mono<CompleteStoryResponse> requestFromLLMServer(Station station) {
        if (station == null) {
            log.error("Station is null");
            return Mono.empty();
        }

        CompleteStoryRequest request = CompleteStoryRequest.builder()
                .station_name(station.getStaName())
                .line_number(station.getStaLine())
                .character_health(80)
                .character_sanity(80)
                .build();

        return llmWebClient.post()
                .uri("/generate-complete-story")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofMillis(aiServerTimeout))
                .map(this::parseJsonManually)
                .doOnSuccess(response -> {
                    if (response == null) {
                        log.error("JSON parsing failed - null returned");
                    }
                })
                .doOnError(e -> {
                    log.error("LLM server communication failed: {} - {}",
                        e.getClass().getSimpleName(), e.getMessage());
                });
    }

    private CompleteStoryResponse parseJsonManually(String jsonString) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(jsonString);

            String storyTitle = root.has("story_title") ? root.get("story_title").asText() : null;
            String description = root.has("description") ? root.get("description").asText() : null;
            String theme = root.has("theme") ? root.get("theme").asText() : null;
            Integer estimatedLength = root.has("estimated_length") ? root.get("estimated_length").asInt() : null;
            String difficulty = root.has("difficulty") ? root.get("difficulty").asText() : null;
            String stationName = root.has("station_name") ? root.get("station_name").asText() : null;
            Integer lineNumber = root.has("line_number") ? root.get("line_number").asInt() : null;

            List<String> keywords = new ArrayList<>();
            if (root.has("keywords") && root.get("keywords").isArray()) {
                for (com.fasterxml.jackson.databind.JsonNode keyword : root.get("keywords")) {
                    keywords.add(keyword.asText());
                }
            }

            List<LLMPageData> pages = new ArrayList<>();
            if (root.has("pages") && root.get("pages").isArray()) {
                for (com.fasterxml.jackson.databind.JsonNode pageNode : root.get("pages")) {
                    String content = pageNode.has("content") ? pageNode.get("content").asText() : "";

                    List<LLMOptionData> options = new ArrayList<>();
                    if (pageNode.has("options") && pageNode.get("options").isArray()) {
                        for (com.fasterxml.jackson.databind.JsonNode optionNode : pageNode.get("options")) {
                            LLMOptionData option = LLMOptionData.builder()
                                    .content(optionNode.has("content") ? optionNode.get("content").asText() : "")
                                    .effect(optionNode.has("effect") ? optionNode.get("effect").asText() : "none")
                                    .amount(optionNode.has("amount") ? optionNode.get("amount").asInt() : 0)
                                    .effect_preview(optionNode.has("effect_preview") ? optionNode.get("effect_preview").asText() : "")
                                    .build();
                            options.add(option);
                        }
                    }

                    LLMPageData page = LLMPageData.builder()
                            .content(content)
                            .options(options)
                            .build();
                    pages.add(page);
                }
            }

            CompleteStoryResponse result = CompleteStoryResponse.builder()
                    .story_title(storyTitle)
                    .description(description)
                    .theme(theme)
                    .keywords(keywords)
                    .pages(pages)
                    .estimated_length(estimatedLength)
                    .difficulty(difficulty)
                    .station_name(stationName)
                    .line_number(lineNumber)
                    .build();

            return result;

        } catch (Exception e) {
            log.error("수동 JSON 파싱 실패: {}", e.getMessage());
            return null;
        }
    }
    private boolean validateLLMResponse(CompleteStoryResponse response) {
        if (response == null) {
            log.warn("응답이 null입니다");
            return false;
        }

        if (response.getStoryTitle() == null || response.getStoryTitle().trim().isEmpty()) {
            log.warn("제목이 없는 응답");
            return false;
        }

        if (response.getPages() == null || response.getPages().isEmpty()) {
            log.warn("페이지가 없는 응답");
            return false;
        }

        for (LLMPageData page : response.getPages()) {
            if (page == null || page.getContent() == null || page.getContent().trim().isEmpty()) {
                log.warn("빈 페이지 내용 발견");
                return false;
            }
            if (page.getOptions() == null || page.getOptions().isEmpty()) {
                log.warn("선택지가 없는 페이지 발견");
                return false;
            }
        }

        return true;
    }

    @Transactional
    public boolean saveStoryToDB(Station station, CompleteStoryResponse llmResponse) {
        if (station == null || llmResponse == null) {
            log.error("Data is null - station={}, llmResponse={}", station != null, llmResponse != null);
            return false;
        }

        try {
            Story story = Story.builder()
                    .station(station)
                    .stoTitle(llmResponse.getStoryTitle())
                    .stoLength(llmResponse.getPages() != null ? llmResponse.getPages().size() : 0)
                    .stoDescription(llmResponse.getDescription())
                    .stoTheme(llmResponse.getTheme())
                    .stoKeywords(llmResponse.getKeywords() != null ?
                            String.join(",", llmResponse.getKeywords()) : "")
                    .build();
            Story savedStory = storyRepository.save(story);

            List<Page> savedPages = new ArrayList<>();
            List<LLMPageData> pages = llmResponse.getPages();
            if (pages != null) {
                for (int i = 0; i < pages.size(); i++) {
                    LLMPageData pageData = pages.get(i);
                    if (pageData == null) {
                        log.warn("Page {} is null - skipped", i+1);
                        continue;
                    }

                    Page page = Page.builder()
                            .stoId(savedStory.getStoId())
                            .pageNumber((long)(i + 1))
                            .pageContents(pageData.getContent() != null ? pageData.getContent() : "")
                            .build();

                    savedPages.add(pageRepository.save(page));
                }
            }

            if (pages != null) {
                for (int i = 0; i < pages.size() && i < savedPages.size(); i++) {
                    LLMPageData pageData = pages.get(i);
                    Page savedPage = savedPages.get(i);

                    if (pageData == null || pageData.getOptions() == null) {
                        log.warn("Page {} options are null - skipped", i+1);
                        continue;
                    }

                    for (LLMOptionData optionData : pageData.getOptions()) {
                        if (optionData == null) continue;

                        Long nextPageId = (i < savedPages.size() - 1) ?
                                savedPages.get(i + 1).getPageId() : null;

                        Options option = Options.builder()
                                .pageId(savedPage.getPageId())
                                .optContents(optionData.getContent() != null ? optionData.getContent() : "")
                                .optEffect(optionData.getEffect() != null ? optionData.getEffect() : "none")
                                .optAmount(optionData.getAmount() != null ? optionData.getAmount() : 0)
                                .nextPageId(nextPageId)
                                .build();

                        optionsRepository.save(option);
                    }
                }
            }

            return true;

        } catch (Exception e) {
            log.error("DB save failed: {} - {}", e.getClass().getSimpleName(), e.getMessage());
            return false;
        }
    }

    @Scheduled(cron = "0 0 0 * * *")
    public void resetDailyCount() {
        int previousCount = dailyGeneratedCount.getAndSet(0);
        log.info("=== 일일 통계 초기화: 어제 생성 {}개 ===", previousCount);
    }

    public void requestStoryFromLLM() {
        log.info("수동 스토리 생성 요청");
        generateStoryBatch();
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class CompleteStoryRequest {
        @JsonProperty("station_name")
        private String station_name;

        @JsonProperty("line_number")
        private Integer line_number;

        @JsonProperty("character_health")
        private Integer character_health;

        @JsonProperty("character_sanity")
        private Integer character_sanity;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class CompleteStoryResponse {
        @JsonProperty("story_title")
        private String story_title;

        @JsonProperty("description")
        private String description;

        @JsonProperty("theme")
        private String theme;

        @JsonProperty("keywords")
        private List<String> keywords;

        @JsonProperty("pages")
        private List<LLMPageData> pages;

        @JsonProperty("estimated_length")
        private Integer estimated_length;

        @JsonProperty("difficulty")
        private String difficulty;

        @JsonProperty("station_name")
        private String station_name;

        @JsonProperty("line_number")
        private Integer line_number;

        public String getStoryTitle() { return story_title; }
        public String getDescription() { return description; }
        public String getTheme() { return theme; }
        public List<String> getKeywords() { return keywords; }
        public List<LLMPageData> getPages() { return pages; }
        public Integer getEstimatedLength() { return estimated_length; }
        public String getDifficulty() { return difficulty; }
        public String getStationName() { return station_name; }
        public Integer getLineNumber() { return line_number; }
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class LLMPageData {
        @JsonProperty("content")
        private String content;

        @JsonProperty("options")
        private List<LLMOptionData> options;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class LLMOptionData {
        @JsonProperty("content")
        private String content;

        @JsonProperty("effect")
        private String effect;

        @JsonProperty("amount")
        private Integer amount;

        @JsonProperty("effect_preview")
        private String effect_preview;

        public String getContent() { return content; }
        public String getEffect() { return effect; }
        public Integer getAmount() { return amount; }
        public String getEffectPreview() { return effect_preview; }
    }
}