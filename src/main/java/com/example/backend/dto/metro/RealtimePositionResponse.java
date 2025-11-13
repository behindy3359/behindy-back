package com.example.backend.dto.metro;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RealtimePositionResponse {

    @JsonProperty("realtimePositionList")
    private List<RealtimePositionInfo> realtimePositionList;

    @JsonProperty("errorMessage")
    private MetroErrorMessage metroErrorMessage;

    @JsonProperty("status")
    private Integer directStatus;

    @JsonProperty("code")
    private String directCode;

    @JsonProperty("message")
    private String directMessage;

    @JsonProperty("link")
    private String directLink;

    @JsonProperty("developerMessage")
    private String directDeveloperMessage;

    @JsonProperty("total")
    private Integer directTotal;

    public boolean isDirectError() {
        return directStatus != null && directStatus != 200;
    }

    public boolean isWrapperError() {
        return metroErrorMessage != null &&
                (metroErrorMessage.getStatus() == null || metroErrorMessage.getStatus() != 200);
    }

    public boolean isAnyError() {
        return isDirectError() || isWrapperError();
    }

    public boolean isEmpty() {
        return realtimePositionList == null || realtimePositionList.isEmpty();
    }

    public boolean needsMockData() {
        return isAnyError() || isEmpty();
    }

    public String getUnifiedErrorMessage() {
        if (directMessage != null && !directMessage.trim().isEmpty()) {
            return directMessage;
        }

        if (metroErrorMessage != null && metroErrorMessage.getMessage() != null) {
            return metroErrorMessage.getMessage();
        }

        return "알 수 없는 오류";
    }

    public Integer getUnifiedStatus() {
        if (directStatus != null) {
            return directStatus;
        }

        if (metroErrorMessage != null && metroErrorMessage.getStatus() != null) {
            return metroErrorMessage.getStatus();
        }

        return null;
    }

    public String getDebugSummary() {
        if (isDirectError()) {
            return String.format("DirectError[%d]: %s", directStatus, directMessage);
        }

        if (isWrapperError()) {
            return String.format("WrapperError[%d]: %s",
                    metroErrorMessage.getStatus(), metroErrorMessage.getMessage());
        }

        if (isEmpty()) {
            return "Success but Empty Data";
        }

        return String.format("Success with %d trains",
                realtimePositionList != null ? realtimePositionList.size() : 0);
    }
}