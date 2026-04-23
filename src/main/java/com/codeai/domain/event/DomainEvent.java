package com.codeai.domain.event;

import java.time.LocalDateTime;

public record DomainEvent(LocalDateTime occurredAt) {

    public DomainEvent() {
        this(LocalDateTime.now());
    }
}
