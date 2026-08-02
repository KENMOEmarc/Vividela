package com.template.vivid.repository;

import com.template.vivid.model.entity.Notification;
import com.template.vivid.model.enums.NotificationType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserIdOrderBySentAtDesc(Long userId);

    /**
     * Notifications EMAIL/SMS en échec dont le nombre de tentatives n'a pas
     * encore atteint {@code maxRetries}, les plus anciennes en premier.
     * Utilisée par {@code NotificationServiceImpl#retryFailedChannelNotifications}.
     * {@code pageable} borne le nombre de lignes traitées par exécution du
     * job (voir {@code PageRequest.of(0, batchSize)}), pour éviter qu'un pic
     * d'échecs ne fasse traiter un volume incontrôlé en une seule passe.
     */
    @Query("SELECT n FROM Notification n WHERE n.status = com.template.vivid.model.enums.RequestStatus.FAILED "
            + "AND n.notificationType IN :types AND n.retryCount < :maxRetries "
            + "ORDER BY n.sentAt ASC")
    List<Notification> findFailedForRetry(@Param("types") Collection<NotificationType> types,
                                           @Param("maxRetries") int maxRetries,
                                           Pageable pageable);

    /**
     * Notifications EMAIL/SMS ayant épuisé leurs tentatives de reprise
     * (retryCount >= maxRetries) et pas encore signalées au personnel.
     * Utilisée par {@code NotificationServiceImpl#escalateExhaustedNotificationFailures}.
     */
    @Query("SELECT n FROM Notification n WHERE n.status = com.template.vivid.model.enums.RequestStatus.FAILED "
            + "AND n.notificationType IN :types AND n.retryCount >= :maxRetries "
            + "AND n.escalated = false ORDER BY n.sentAt ASC")
    List<Notification> findExhaustedNotEscalated(@Param("types") Collection<NotificationType> types,
                                                   @Param("maxRetries") int maxRetries);
}



