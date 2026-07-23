package com.template.auth.model.entity;

import com.template.auth.model.enums.NotificationType;
import com.template.auth.model.enums.RequestStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.groups.Default;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;

@Getter
@Setter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "notifications")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "order_id")
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "template_id")
    private EmailTemplate template;

    @Size(max = 255)
    @Column(name = "subject")
    private String subject;

    @Column(name = "message")
    private String message;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false)
    private NotificationType notificationType;

    @NotNull
    @ColumnDefault("'PENDING'")
    // La colonne `notifications.status` (ENUM('PENDING','SUCCESS','FAILED') dans
    // laundry.sql) correspond exactement à RequestStatus. Le "No enum constant
    // ...RequestStatus.SENT" venait de lignes historiques en base réelle
    // portant l'ancienne valeur 'SENT' (jamais migrée), pas d'une incohérence
    // de schéma : voir la migration de données one-shot
    // src/main/resources/db/fix-legacy-notification-status.sql, à exécuter une
    // fois sur la base concernée avant/à ce déploiement.
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private RequestStatus status;

    @ColumnDefault("false")
    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "sent_at")
    private Instant sentAt;

    // AJOUT : support de la reprise automatique des envois EMAIL/SMS en échec
    // (voir NotificationServiceImpl#retryFailedChannelNotifications) — voir
    // revue de code, "Statut des envois email/SMS non exposé/exploité côté
    // supervision". Nombre de tentatives déjà effectuées (0 = tentative
    // initiale uniquement, jamais retentée).
    @ColumnDefault("0")
    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private Integer retryCount = 0;

    // Horodatage de la dernière tentative d'envoi (initiale ou reprise).
    // Distinct de sentAt, qui reste l'horodatage de création de la ligne.
    @Column(name = "last_attempt_at")
    private Instant lastAttemptAt;

    // Indique que cette notification en échec définitif (retries épuisés) a
    // déjà été signalée au personnel (ADMIN/MANAGER), pour éviter de
    // ré-alerter à chaque exécution du job planifié d'escalade.
    @ColumnDefault("false")
    @Column(name = "escalated", nullable = false)
    @Builder.Default
    private Boolean escalated = false;

}