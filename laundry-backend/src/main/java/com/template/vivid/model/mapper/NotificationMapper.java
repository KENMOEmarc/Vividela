package com.template.vivid.model.mapper;

import com.template.vivid.model.dto.NotificationDto;
import com.template.vivid.model.entity.Notification;

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
