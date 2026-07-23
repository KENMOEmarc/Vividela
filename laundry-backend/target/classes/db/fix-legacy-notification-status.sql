-- =============================================================================
-- fix-legacy-notification-status.sql
--
-- Migration de données ONE-SHOT à exécuter manuellement une seule fois sur la
-- base concernée, AVANT de déployer une version du backend qui n'a plus le
-- RequestStatusConverter (c-a-d qui lit `notifications.status` avec un simple
-- @Enumerated(EnumType.STRING)).
--
-- Contexte : RequestStatus (PENDING / FAILED / SUCCESS) correspond déjà
-- exactement au schéma de `notifications.status` défini dans laundry.sql.
-- Le crash "No enum constant ...RequestStatus.SENT" ne vient donc pas d'une
-- incohérence de schéma ou de formulaire, mais de lignes historiques encore
-- présentes en base réelle avec l'ancienne valeur 'SENT' (d'une version de
-- l'enum antérieure à PENDING/FAILED/SUCCESS, jamais migrée).
--
-- Cette migration corrige ces lignes une bonne fois pour toutes, au lieu de
-- tolérer indéfiniment la valeur invalide via un converter applicatif.
-- =============================================================================

-- 'SENT' (notification envoyée avec succès) -> SUCCESS
UPDATE notifications SET status = 'SUCCESS' WHERE status = 'SENT';

-- Vérification : ne doit plus rien retourner après la migration.
-- SELECT DISTINCT status FROM notifications
-- WHERE status NOT IN ('PENDING', 'FAILED', 'SUCCESS');
