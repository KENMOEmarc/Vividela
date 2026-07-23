-- =============================================================================
-- fix-legacy-stocks-schema.sql
--
-- Migration de schéma ONE-SHOT à exécuter manuellement une seule fois sur
-- toute base MySQL créée AVANT l'évolution du modèle de stock (passage de
-- "1 produit = 1 ligne de stock" à "1 produit = plusieurs lots (batches)").
--
-- Contexte : l'entité Stock (voir Stock.java) attend désormais les colonnes
-- entry_date, expiration_date et unit_price sur la table `stocks`, telles
-- que définies dans laundry.sql. Sur une base déjà créée avec l'ancienne
-- version du schéma (product_id UNIQUE, sans ces colonnes), Hibernate ne les
-- ajoute pas de façon fiable via ddl-auto=update (colonne NOT NULL sans
-- valeur de repli explicite pour les lignes existantes), ce qui provoque au
-- runtime :
--   "Unknown column 's1_0.entry_date' in 'field list'"
-- dès qu'un lot de stock est chargé (ex: GET /api/stock/product/{id}).
--
-- Cette migration ajoute les colonnes manquantes de façon idempotente (sans
-- effet si elles existent déjà, donc rejouable sans risque) et retire
-- l'ancienne contrainte UNIQUE sur product_id si elle est encore présente,
-- puisqu'un produit peut désormais avoir plusieurs lots.
-- =============================================================================

-- 1) Colonne entry_date (date de réception du lot), toujours NOT NULL avec
--    une valeur par défaut pour ne pas casser les lignes déjà existantes.
ALTER TABLE stocks
    ADD COLUMN IF NOT EXISTS entry_date DATE NOT NULL DEFAULT (CURRENT_DATE) AFTER unit_price;

-- 2) Colonne expiration_date (facultative : certains produits ne périment pas).
ALTER TABLE stocks
    ADD COLUMN IF NOT EXISTS expiration_date DATE NULL AFTER entry_date;

-- 3) Colonne unit_price (facultative), au cas où elle manquerait également
--    sur une base encore plus ancienne.
ALTER TABLE stocks
    ADD COLUMN IF NOT EXISTS unit_price DECIMAL(12,2) NULL AFTER current_quantity;

-- 4) Colonne created_at, au cas où une base très ancienne ne l'aurait pas non plus.
ALTER TABLE stocks
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- 5) Un produit peut désormais posséder plusieurs lots : on retire l'ancienne
--    contrainte UNIQUE sur product_id si elle existe encore (nom généré par
--    MySQL habituellement "product_id", à adapter si votre contrainte porte
--    un autre nom — vérifier au préalable avec :
--      SHOW INDEX FROM stocks WHERE Non_unique = 0 AND Key_name <> 'PRIMARY';
--    ). Ignore silencieusement l'erreur si la contrainte n'existe pas/plus.
SET @constraint_exists := (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'stocks'
      AND COLUMN_NAME = 'product_id'
      AND NON_UNIQUE = 0
      AND INDEX_NAME <> 'PRIMARY'
);
SET @idx_name := (
    SELECT INDEX_NAME FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'stocks'
      AND COLUMN_NAME = 'product_id'
      AND NON_UNIQUE = 0
      AND INDEX_NAME <> 'PRIMARY'
    LIMIT 1
);
SET @drop_sql := IF(@constraint_exists > 0,
    CONCAT('ALTER TABLE stocks DROP INDEX `', @idx_name, '`'),
    'SELECT 1');
PREPARE stmt FROM @drop_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Vérification : la requête ci-dessous doit désormais s'exécuter sans erreur.
-- SELECT id, product_id, current_quantity, unit_price, entry_date,
--        expiration_date, created_at
-- FROM stocks;
