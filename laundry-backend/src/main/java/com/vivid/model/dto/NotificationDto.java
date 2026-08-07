package com.vivid.model.dto;

import com.vivid.model.enums.NotificationType;
import com.vivid.model.enums.RequestStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Représentation publique d'une notification.
 * <p>
 * BUGFIX : {@code NotificationController} renvoyait auparavant directement
 * l'entité JPA {@code Notification}. Avec {@code spring.jpa.open-in-view}
 * actif par défaut, Jackson sérialisait alors les associations paresseuses
 * {@code user} et {@code order} au complet — exposant, à chaque notification
 * retournée par GET /notifications, le mot de passe (haché) du client, ainsi
 * que ceux de {@code order.clientUser}, {@code order.createdBy} et
 * {@code order.updatedBy}. Ce DTO ne contient que ce dont le frontend a
 * réellement besoin.
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NotificationDto {
    private Long id;
    private Long orderId;
    private String subject;
    private String message;
    private NotificationType notificationType;
    private RequestStatus status;
    private Boolean isRead;
    private Instant sentAt;

    // Détails de la commande liée, pour affichage direct dans la liste de
    // notifications sans appel supplémentaire côté frontend (numéro de
    // commande = orderId ci-dessus).
    private LocalDate orderDepositDate;
    private Integer orderArticleCount;
}
