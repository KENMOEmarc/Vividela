package com.vivid.model.dto;

import com.vivid.model.enums.TicketStatus;
import lombok.Builder;

import java.time.LocalDateTime;


@Builder
public record TicketDto(
        Long id,
        String barcode,
        TicketStatus status,
        LocalDateTime issuedAt
) {
}