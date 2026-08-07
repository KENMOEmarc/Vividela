package com.vivid.model.dto;

import com.vivid.model.enums.NotificationType;
import com.vivid.model.enums.RequestStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

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
 * <p>
 * NOTE : record immuable — {@code toBuilder()} (généré par {@code @Builder})
 * permet de produire une copie enrichie (ex. avec les détails de commande)
 * plutôt que de muter l'instance, comme le faisait l'ancien DTO à setters.
 */
@Builder(toBuilder = true)
public record NotificationDto(
        Long id,
        Long orderId,
        String subject,
        String message,
        NotificationType notificationType,
        RequestStatus status,
        Boolean isRead,
        Instant sentAt,

        // Détails de la commande liée, pour affichage direct dans la liste de
        // notifications sans appel supplémentaire côté frontend (numéro de
        // commande = orderId ci-dessus).
        LocalDate orderDepositDate,
        Integer orderArticleCount
) {
}
