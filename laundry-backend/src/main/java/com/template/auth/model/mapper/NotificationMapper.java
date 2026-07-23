package com.template.auth.model.mapper;

import com.template.auth.model.dto.NotificationDto;
import com.template.auth.model.entity.Notification;

/**
 * Mapper centralisant la conversion Notification (entity) → NotificationDto.
 */
public final class NotificationMapper {

    private NotificationMapper() {
        // Classe utilitaire : pas d'instanciation
    }

    public static NotificationDto toDto(Notification notification) {
        if (notification == null) {
            return null;
        }

        return NotificationDto.builder()
                .id(notification.getId())
                .orderId(notification.getOrder() != null ? notification.getOrder().getId() : null)
                .subject(notification.getSubject())
                .message(notification.getMessage())
                .notificationType(notification.getNotificationType())
                .status(notification.getStatus())
                .isRead(notification.getIsRead())
                .sentAt(notification.getSentAt())
                .build();
    }
}
