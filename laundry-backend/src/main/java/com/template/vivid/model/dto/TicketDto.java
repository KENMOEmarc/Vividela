package com.template.vivid.model.dto;

import com.template.vivid.model.enums.TicketStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Builder
public record TicketDto(
        Long id,
        String barcode,
        TicketStatus status,
        LocalDateTime issuedAt
) {
}