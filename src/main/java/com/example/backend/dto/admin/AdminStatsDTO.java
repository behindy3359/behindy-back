package com.example.backend.dto.admin;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminStatsDTO {

    private Long totalUsers;
    private Long activeUsers; 
    private Long newUsersToday;
    private Long newUsersThisWeek;

    private Long totalPosts;
    private Long activePosts;
    private Long newPostsToday;
    private Long newPostsThisWeek;

    private Long totalComments;
    private Long activeComments;
    private Long newCommentsToday;
    private Long newCommentsThisWeek;

    private Long totalStories;
    private Long storiesGeneratedToday;
    private Long storiesGeneratedThisWeek;

    private Long totalStations;
    private Long totalLines;

    private LocalDateTime serverTime;
    private String serverStatus;

    public void setHealthyStatus() {
        this.serverStatus = "HEALTHY";
    }

    public void setWarningStatus() {
        this.serverStatus = "WARNING";
    }

    public void setErrorStatus() {
        this.serverStatus = "ERROR";
    }
}
