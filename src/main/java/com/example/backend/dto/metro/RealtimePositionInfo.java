package com.example.backend.dto.metro;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class RealtimePositionInfo {

    @JsonProperty("subwayId")
    private String subwayId;

    @JsonProperty("subwayNm")
    private String subwayNm;

    @JsonProperty("statnId")
    private String statnId;

    @JsonProperty("statnNm")
    private String statnNm;

    @JsonProperty("trainNo")
    private String trainNo;

    @JsonProperty("lastRecptnDt")
    private String lastRecptnDt;

    @JsonProperty("recptnDt")
    private String recptnDt;

    @JsonProperty("updnLine")
    private String updnLine;

    @JsonProperty("statnTid")
    private String statnTid;

    @JsonProperty("directAt")
    private String directAt;

    @JsonProperty("lstcarAt")
    private String lstcarAt;
    }