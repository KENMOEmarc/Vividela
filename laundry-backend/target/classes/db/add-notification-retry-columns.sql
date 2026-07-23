-- =============================================================================
-- add-notification-retry-columns.sql
--
-- Migration de schéma à exécuter manuellement sur tout environnement où
-- `spring.jpa.hibernate.ddl-auto` est réglé sur `validate` (typiquement le
-- profil `prod` — voir application-prod.yml), AVANT de déployer la version
-- du backend qui introduit la reprise automatique des notifications
-- EMAIL/SMS en échec (voir NotificationServiceImpl —
-- retryFailedChannelNotifications / escalateExhaustedNotificationFailures).
--
-- En profil `dev` (ddl-auto: update), Hibernate ajoute déjà ces colonnes
-- automatiquement au démarrage — ce script n'y est pas nécessaire.
-- =============================================================================

ALTER TABLE notifications
    ADD COLUMN retry_count INT NOT NULL DEFAULT 0,
    ADD COLUMN last_attempt_at TIMESTAMP NULL,
    ADD COLUMN escalated BOOLEAN NOT NULL DEFAULT FALSE;

-- Vérification : les notifications déjà en échec avant la migration doivent
-- rester éligibles à une reprise (retry_count = 0 par défaut, escalated =
-- false par défaut) — aucune action supplémentaire n'est nécessaire ici.
