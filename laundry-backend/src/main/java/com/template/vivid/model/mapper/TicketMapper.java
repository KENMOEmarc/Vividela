package com.template.vivid.model.mapper;

import com.template.vivid.model.dto.TicketDto;
import com.template.vivid.model.entity.Ticket;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Mapper centralisant la conversion Ticket (entity) ↔ TicketDto.
 */
public final class TicketMapper {

    private TicketMapper() {
        // Classe utilitaire : pas d'instanciation
    }

    public static TicketDto toDto(Ticket ticket) {
        if (ticket == null) {
            return null;
        }

        return TicketDto.builder()
                .id(ticket.getId())
                .barcode(ticket.getBarcode())
                .status(ticket.getStatus())
                .issuedAt(ticket.getIssuedAt() != null
                        ? LocalDateTime.ofInstant(ticket.getIssuedAt(), ZoneId.systemDefault())
                        : null)
                .build();
    }
}
