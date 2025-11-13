package com.example.backend.entity.multiplayer;

public enum MessageType {
    USER,       // 사용자 메시지
    LLM,        // LLM 응답
    SYSTEM,     // 시스템 메시지 (입장/퇴장/사망 등)
    PHASE,      // Phase 변경 메시지
    VOTE        // 투표 메시지
}
