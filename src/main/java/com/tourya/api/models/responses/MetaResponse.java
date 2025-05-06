package com.tourya.api.models.responses;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class MetaResponse {
    private String messageUid = UUID.randomUUID().toString();
    private String requestDt = LocalDateTime.now().toString();
}
