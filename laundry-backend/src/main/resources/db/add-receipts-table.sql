-- =============================================================================
-- add-receipts-table.sql
--
-- Migration ONE-SHOT à exécuter manuellement une seule fois sur une base déjà
-- déployée, AVANT de déployer une version du backend qui persiste désormais
-- la référence du reçu de chaque commande (voir Receipt / ReceiptRepository /
-- PaymentServiceImpl#reconcileOrderPaymentStatus).
--
-- Contexte : jusqu'ici, le numéro de reçu (référence de facture) était
-- re-tiré aléatoirement à CHAQUE téléchargement du PDF (voir
-- DocumentReferenceGenerator#generateReceiptReference), sans jamais être
-- persisté nulle part — deux téléchargements du même reçu produisaient donc
-- deux numéros de facture différents, et aucun reçu n'était jamais
-- réellement "généré" tant qu'un utilisateur n'avait pas cliqué sur le
-- bouton de téléchargement. Cette table corrige ce problème en fixant, une
-- bonne fois pour toutes, le numéro de reçu de chaque commande dès qu'elle
-- passe à paymentStatus=COMPLETED.
--
-- Sur une installation neuve, cette table est déjà créée par laundry.sql :
-- ce script ne concerne que les bases déjà en production avant ce correctif.
-- =============================================================================

CREATE TABLE IF NOT EXISTS receipts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL UNIQUE,
    reference VARCHAR(50) NOT NULL UNIQUE,
    issued_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    issued_by BIGINT,
    FOREIGN KEY(order_id) REFERENCES orders(id) ON DELETE CASCADE,
    FOREIGN KEY(issued_by) REFERENCES users(id) ON DELETE SET NULL
);

-- AJOUT (rattrapage) : génère une référence de reçu pour toutes les
-- commandes DÉJÀ payées intégralement (paymentStatus=COMPLETED) avant ce
-- correctif, afin qu'elles disposent elles aussi d'un numéro de facture
-- stable dès la mise à jour, sans attendre un nouvel événement de paiement.
INSERT INTO receipts (order_id, reference, issued_at)
SELECT o.id,
       CONCAT('REC-', UPPER(SUBSTRING(MD5(CONCAT(o.id, '-', RAND())), 1, 8))),
       COALESCE(o.updated_at, CURRENT_TIMESTAMP)
FROM orders o
WHERE o.payment_status = 'COMPLETED'
  AND NOT EXISTS (SELECT 1 FROM receipts r WHERE r.order_id = o.id);
