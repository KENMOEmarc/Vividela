-- ============================================================
-- Migration : Suppression de la table email_templates
-- ============================================================
-- La table email_templates n'est plus utilisée depuis la refonte
-- du système de notifications qui génère les messages dynamiquement
-- au lieu d'utiliser des templates prédéfinis.
--
-- Cette migration :
-- 1. Supprime la contrainte de clé étrangère template_id dans notifications
-- 2. Supprime la colonne template_id de la table notifications
-- 3. Supprime la table email_templates elle-même
-- ============================================================

-- Supprimer la contrainte de clé étrangère sur notifications.template_id
ALTER TABLE notifications DROP FOREIGN KEY notifications_ibfk_3;

-- Supprimer la colonne template_id de la table notifications
ALTER TABLE notifications DROP COLUMN template_id;

-- Supprimer la table email_templates
DROP TABLE IF EXISTS email_templates;

-- ============================================================
-- Migration appliquée avec succès
-- ============================================================

