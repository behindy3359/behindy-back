package com.example.backend.dto.multiplayer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LlmStoryRequest {
    private Long roomId;
    private Integer phase;
    private String stationName;
    private String storyOutline;
    private List<ParticipantContext> participants;
    private List<MessageContext> messageStack;
    private Boolean isIntro;
}
