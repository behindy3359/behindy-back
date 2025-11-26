package com.example.backend.event;

import lombok.Getter;

@Getter
public class RoomCreatedEvent {
    private final Long roomId;

    public RoomCreatedEvent(Long roomId) {
        this.roomId = roomId;
    }
}
