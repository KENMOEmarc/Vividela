-- ============================================================
-- BASE DE DONNÉES POUR UN PRESSING AU CAMEROUN (AVEC ÉNUMÉRATIONS)
-- ============================================================
-- Toutes les colonnes ENUM ci-dessous sont volontairement alignées, valeur
-- pour valeur ET, quand c'est pertinent, ordre pour ordre, sur les enums Java
-- du backend (package com.template.auth.model.enums) :
--   orders.status         <-> OrderStatus
--   orders.payment_status <-> PaymentStatus
--   payments.status       <-> PaymentStatus
--   notifications.status  <-> RequestStatus
--   articles.status       <-> ArticleStatus
--   tickets.status        <-> TicketStatus
--   *.clothing_type       <-> ClothingType
--   articles.fabric       <-> FabricType
--   articles.size         <-> SizeType
--   *.service             <-> ServiceType
--   notifications.notification_type <-> NotificationType
--   products.measurement_unit       <-> MeasurementUnit
--   product_registrations.registration_type <-> RegistrationType
--   stock_movements.movement_type   <-> MovementType
--   users.role                      <-> RoleType
-- Toute évolution d'un enum Java doit être répercutée ICI en premier lieu,
-- puis dans les données de test ci-dessous.
-- ============================================================

-- ============================================================
-- 1. TABLES DE RÉFÉRENCE (seulement email_templates, tout le reste est en ENUM)
-- ============================================================
CREATE TABLE email_templates (
                                 id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                 name VARCHAR(100) UNIQUE NOT NULL,
                                 subject VARCHAR(255) NOT NULL,
                                 html_body LONGTEXT NOT NULL
);

-- ============================================================
-- 2. TABLES MÉTIER AVEC ÉNUMÉRATIONS
-- ============================================================

-- Utilisateurs (rôle en ENUM <-> RoleType)
CREATE TABLE users (
                       id BIGINT AUTO_INCREMENT PRIMARY KEY,
                       role ENUM('ADMIN','MANAGER','EMPLOYEE','CUSTOMER') NOT NULL,
                       first_name VARCHAR(100) NOT NULL,
                       last_name VARCHAR(100) NOT NULL,
                       user_name VARCHAR(50) NOT NULL UNIQUE,
                       email VARCHAR(255) NOT NULL UNIQUE,
                       phone VARCHAR(20) UNIQUE,
                       password VARCHAR(255) NOT NULL,
                       loyalty_points INT DEFAULT 0,
                       is_active BOOLEAN DEFAULT TRUE,
                       created_by BIGINT DEFAULT NULL,
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                       FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL
);

-- Tarifs des services (type de vêtement <-> ClothingType, service <-> ServiceType)
CREATE TABLE service_prices (
                                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                clothing_type ENUM('SHIRT','T_SHIRT','PANTS','JEANS','SKIRT','DRESS','JACKET','COAT','SWEATER','BLOUSE','UNDERWEAR','SOCKS','SUIT','VEST','SHORTS','SCARF','GLOVES','BELT') NOT NULL,
                                service ENUM('DRY_CLEAN','WASH','IRON','STAIN_REMOVAL','DEYING','ALTERATION') NOT NULL,
                                price DECIMAL(10,2) NOT NULL,
                                active BOOLEAN NOT NULL DEFAULT TRUE,
                                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                UNIQUE(clothing_type, service),
                                CHECK(price >= 0)
);

-- Commandes (statut <-> OrderStatus, statut de paiement <-> PaymentStatus)
-- ENUM et DEFAULT alignés sur OrderServiceImpl.createOrder, qui fixe
-- toujours explicitement status=RECEIVED et paymentStatus=PENDING à la
-- création d'une commande.
CREATE TABLE orders (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        client_user_id BIGINT NOT NULL,
                        deposit_date DATE NOT NULL,
                        expected_delivery_date DATE,
                        delivered_at DATE,
                        status ENUM('RECEIVED','PENDING','IN_PROGRESS','READY','DELIVERED','CANCELLED') NOT NULL DEFAULT 'RECEIVED',
                        payment_status ENUM('PENDING','COMPLETED','FAILED','REFUNDED') NOT NULL DEFAULT 'PENDING',
                        shipping_address VARCHAR(255),
                        notes VARCHAR(1000),
                        total_amount DECIMAL(10,2) NOT NULL DEFAULT 0,
                        discount_amount DECIMAL(10,2) NOT NULL DEFAULT 0,
                        loyalty_points_used INT NOT NULL DEFAULT 0,
                        -- AJOUT : évite de déduire/créditer deux fois les points de
                        -- fidélité de la même commande (voir Order.loyaltyProcessed).
                        loyalty_processed BOOLEAN NOT NULL DEFAULT FALSE,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                        created_by BIGINT,
                        updated_by BIGINT,
                        FOREIGN KEY(client_user_id) REFERENCES users(id),
                        FOREIGN KEY(created_by) REFERENCES users(id) ON DELETE SET NULL,
                        FOREIGN KEY(updated_by) REFERENCES users(id) ON DELETE SET NULL
);

-- Articles (type <-> ClothingType, taille <-> SizeType, tissu <-> FabricType,
-- statut <-> ArticleStatus)
CREATE TABLE articles (
                          id BIGINT AUTO_INCREMENT PRIMARY KEY,
                          order_id BIGINT NOT NULL,
                          clothing_type ENUM('SHIRT','T_SHIRT','PANTS','JEANS','SKIRT','DRESS','JACKET','COAT','SWEATER','BLOUSE','UNDERWEAR','SOCKS','SUIT','VEST','SHORTS','SCARF','GLOVES','BELT') NOT NULL,
                          size ENUM('S','M','L','XL','XXL','UNIQUE') NOT NULL,
                          fabric ENUM('COTTON','POLYESTER','WOOL','SILK','LINEN','SYNTHETIC','BLENDED','ACRYLIC','NYLON') NOT NULL,
                          color VARCHAR(50),
                          distinction VARCHAR(255),
                          status ENUM('PENDING','WASHING','IRONING','QUALITY_CHECK','PACKED','COMPLETED') NOT NULL DEFAULT 'PENDING',
                          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                          updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                          FOREIGN KEY(order_id) REFERENCES orders(id) ON DELETE CASCADE
);

CREATE TABLE article_services (
                                  id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                  article_id BIGINT NOT NULL,
                                  service ENUM('DRY_CLEAN','WASH','IRON','STAIN_REMOVAL','DEYING','ALTERATION') NOT NULL,
                                  applied_price DECIMAL(10,2) NOT NULL,
                                  FOREIGN KEY(article_id) REFERENCES articles(id) ON DELETE CASCADE,
                                  UNIQUE(article_id, service)
);

-- Paiements (méthode <-> PaymentMethodType, statut <-> PaymentStatus)
CREATE TABLE payments (
                          id BIGINT AUTO_INCREMENT PRIMARY KEY,
                          order_id BIGINT NOT NULL,
                          payment_method ENUM('CASH','CHECK','MOBILE_PAYMENT') NOT NULL,
                          amount DECIMAL(10,2) NOT NULL,
                          payer_phone VARCHAR(20),
                          transaction_reference VARCHAR(255),
                          status ENUM('PENDING','COMPLETED','FAILED','REFUNDED') NOT NULL DEFAULT 'PENDING',
                          paid_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                          created_by BIGINT,
                          FOREIGN KEY(order_id) REFERENCES orders(id) ON DELETE CASCADE,
                          FOREIGN KEY(created_by) REFERENCES users(id) ON DELETE SET NULL
);

-- Notifications (type <-> NotificationType, statut <-> RequestStatus)
CREATE TABLE notifications (
                               id BIGINT AUTO_INCREMENT PRIMARY KEY,
                               user_id BIGINT NOT NULL,
                               order_id BIGINT,
                               template_id BIGINT,
                               subject VARCHAR(255),
                               message TEXT,
                               notification_type ENUM('EMAIL','SMS','PUSH','IN_APP','PHONE_CALL') NOT NULL,
                               status ENUM('PENDING','SUCCESS','FAILED') NOT NULL DEFAULT 'PENDING',
                               is_read BOOLEAN NOT NULL DEFAULT FALSE,
                               sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                               FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE,
                               FOREIGN KEY(order_id) REFERENCES orders(id) ON DELETE CASCADE,
                               FOREIGN KEY(template_id) REFERENCES email_templates(id) ON DELETE SET NULL
);

-- Avis clients (formulaire envoyé à la livraison, sentiment <-> FeedbackSentiment)
-- Une seule ligne par commande (order_id UNIQUE) : générée dès le passage au
-- statut DELIVERED (requested_at), complétée au plus une fois par le client
-- (rating/comment/submitted_at), puis analysée par IA (sentiment/ai_summary).
CREATE TABLE order_feedback (
                                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                order_id BIGINT NOT NULL UNIQUE,
                                rating TINYINT,
                                comment VARCHAR(2000),
                                sentiment ENUM('POSITIVE','NEUTRAL','NEGATIVE'),
                                ai_summary VARCHAR(500),
                                requested_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                submitted_at TIMESTAMP NULL,
                                FOREIGN KEY(order_id) REFERENCES orders(id) ON DELETE CASCADE
);

-- Tickets (statut <-> TicketStatus)
CREATE TABLE tickets (
                         id BIGINT AUTO_INCREMENT PRIMARY KEY,
                         order_id BIGINT NOT NULL UNIQUE,
                         barcode VARCHAR(50) UNIQUE,
                         pdf_url VARCHAR(500),
                         status ENUM('GENERATED','DOWNLOADED','EXPIRED') NOT NULL DEFAULT 'GENERATED',
                         issued_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                         -- AJOUT : expiration et suivi de consultation du ticket
                         -- (voir revue de code — règle manquante n°16).
                         expires_at TIMESTAMP NULL,
                         downloaded_at TIMESTAMP NULL,
                         FOREIGN KEY(order_id) REFERENCES orders(id) ON DELETE CASCADE
);

-- AJOUT : reçu de vente d'une commande. Le numéro de facture (reference)
-- doit être stable et unique par commande — voir revue de code, règle
-- manquante : "Aucun reçu n'est jamais réellement généré après un paiement,
-- et le numéro de reçu n'est pas stable d'un téléchargement à l'autre". Une
-- ligne est créée automatiquement dès que la commande passe à
-- paymentStatus=COMPLETED (voir PaymentServiceImpl#reconcileOrderPaymentStatus).
CREATE TABLE receipts (
                          id BIGINT AUTO_INCREMENT PRIMARY KEY,
                          order_id BIGINT NOT NULL UNIQUE,
                          reference VARCHAR(50) NOT NULL UNIQUE,
                          issued_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                          issued_by BIGINT,
                          FOREIGN KEY(order_id) REFERENCES orders(id) ON DELETE CASCADE,
                          FOREIGN KEY(issued_by) REFERENCES users(id) ON DELETE SET NULL
);

-- Produits (unité de mesure <-> MeasurementUnit)
CREATE TABLE products (
                          id BIGINT AUTO_INCREMENT PRIMARY KEY,
                          name VARCHAR(100) NOT NULL UNIQUE,
                          threshold_value DECIMAL(12,2) NOT NULL DEFAULT 0,
                          measurement_unit ENUM('LITER','UNIT','KG','ML','PACKET') NOT NULL,
                          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                          updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Stocks
-- ÉVOLUTION : un produit peut désormais posséder PLUSIEURS lots de stock
-- (product_id n'est plus UNIQUE), chacun avec sa propre quantité, son prix
-- d'achat, sa date d'entrée en stock et sa date d'expiration.
CREATE TABLE stocks (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        product_id BIGINT NOT NULL,
                        current_quantity DECIMAL(12,2) NOT NULL DEFAULT 0,
                        unit_price DECIMAL(12,2),
                        entry_date DATE NOT NULL DEFAULT (CURRENT_DATE),
                        expiration_date DATE,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY(product_id) REFERENCES products(id) ON DELETE CASCADE
);

-- Enregistrements de produits (type <-> RegistrationType)
CREATE TABLE product_registrations (
                                       id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                       product_id BIGINT NOT NULL,
                                       employee_user_id BIGINT NOT NULL,
                                       quantity DECIMAL(12,2) NOT NULL,
                                       registration_type ENUM('IN','ADJUSTMENT','SACHET') NOT NULL,
                                       notes VARCHAR(255),
                                       registered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                       FOREIGN KEY(product_id) REFERENCES products(id),
                                       FOREIGN KEY(employee_user_id) REFERENCES users(id)
);

-- Traitements (service <-> ServiceType)
CREATE TABLE treatments (
                            id BIGINT AUTO_INCREMENT PRIMARY KEY,
                            article_id BIGINT NOT NULL,
                            employee_user_id BIGINT NOT NULL,
                            service ENUM('DRY_CLEAN','WASH','IRON','STAIN_REMOVAL','DEYING','ALTERATION') NOT NULL,
                            started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            completed_at TIMESTAMP NULL,
                            notes VARCHAR(255),
                            FOREIGN KEY(article_id) REFERENCES articles(id) ON DELETE CASCADE,
                            FOREIGN KEY(employee_user_id) REFERENCES users(id)
);

-- Mouvements de stock (type <-> MovementType)
CREATE TABLE stock_movements (
                                 id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                 stock_id BIGINT NOT NULL,
                                 treatment_id BIGINT,
                                 user_id BIGINT NOT NULL,
                                 quantity DECIMAL(12,2) NOT NULL,
                                 movement_type ENUM('CONSUMPTION','RESTOCK','ADJUSTMENT') NOT NULL,
                                 notes VARCHAR(255),
                                 movement_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                 FOREIGN KEY(stock_id) REFERENCES stocks(id) ON DELETE CASCADE,
                                 FOREIGN KEY(treatment_id) REFERENCES treatments(id) ON DELETE CASCADE,
                                 FOREIGN KEY(user_id) REFERENCES users(id)
);

-- ============================================================
-- 2 bis. RÉPARATION DE DONNÉES HISTORIQUES (idempotent)
-- ------------------------------------------------------------
-- Sans effet sur une base fraîchement créée par ce script (aucune ligne ne
-- peut encore exister). Protège en revanche toute ré-exécution de ce script
-- sur une base déjà peuplée par une ancienne version de l'application, dont
-- les enums auraient depuis été renommés :
--   - notifications.status portait autrefois la valeur 'SENT' (ancien
--     libellé de RequestStatus, renommé depuis en 'SUCCESS') ;
--   - payments.status a pu porter d'anciens libellés non alignés sur
--     PaymentStatus actuel ('PAID'/'PAYE' au lieu de 'COMPLETED',
--     'REFUND' au lieu de 'REFUNDED').
-- Ces UPDATE sont sans danger : ils ne touchent que ces valeurs précises et
-- ne modifient rien d'autre.
-- ============================================================
UPDATE notifications SET status = 'SUCCESS' WHERE status = 'SENT';
UPDATE payments SET status = 'COMPLETED' WHERE status IN ('PAID', 'PAYE', 'VALIDE');
UPDATE payments SET status = 'REFUNDED'  WHERE status = 'REFUND';

-- ============================================================
-- 3. INSERTIONS DE DONNÉES (RÉALITÉS CAMEROUNAISES)
-- ============================================================

-- Utilisateurs (inchangés)
INSERT INTO users (role, first_name, last_name, user_name, email, phone, password, loyalty_points, is_active) VALUES
                                                                                                                  ('ADMIN', 'Jean-Pierre', 'Mbarga', 'jpmbarga', 'jp.mbarga@pressingplus.cm', '237699887766', SHA2('Admin@2025', 256), 0, TRUE),
                                                                                                                  ('MANAGER', 'Sophie', 'Ngo Mbock', 'sngombock', 's.ngombock@pressingplus.cm', '237677112233', SHA2('Manager@2025', 256), 0, TRUE),
                                                                                                                  ('EMPLOYEE', 'Alain', 'Etoundi', 'aetoundi', 'alain.etoundi@pressingplus.cm', '237655443322', SHA2('Employe@2025', 256), 0, TRUE),
                                                                                                                  ('EMPLOYEE', 'Christelle', 'Bikanda', 'cbikanda', 'christelle.bikanda@pressingplus.cm', '237690123456', SHA2('Employe@2025', 256), 0, TRUE),
                                                                                                                  ('CUSTOMER', 'Luc', 'Owona', 'lowona', 'luc.owona@gmail.com', '237698765432', SHA2('Client@2025', 256), 150, TRUE),
                                                                                                                  ('CUSTOMER', 'Marie', 'Essimi', 'messimi', 'marie.essimi@yahoo.fr', '237670998877', SHA2('Client@2025', 256), 80, TRUE);

-- Tarifs en francs CFA
INSERT INTO service_prices (clothing_type, service, price, active) VALUES
                                                                       ('SHIRT', 'DRY_CLEAN', 1500, TRUE),
                                                                       ('SHIRT', 'WASH', 500, TRUE),
                                                                       ('SHIRT', 'IRON', 300, TRUE),
                                                                       ('PANTS', 'DRY_CLEAN', 2000, TRUE),
                                                                       ('PANTS', 'WASH', 800, TRUE),
                                                                       ('PANTS', 'IRON', 500, TRUE),
                                                                       ('JACKET', 'DRY_CLEAN', 3000, TRUE),
                                                                       ('JACKET', 'WASH', 1500, TRUE),
                                                                       ('DRESS', 'DRY_CLEAN', 2500, TRUE),
                                                                       ('DRESS', 'WASH', 1000, TRUE),
                                                                       ('DRESS', 'IRON', 700, TRUE),
                                                                       ('COAT', 'DRY_CLEAN', 4000, TRUE),
                                                                       ('SKIRT', 'DRY_CLEAN', 1800, TRUE),
                                                                       ('SKIRT', 'WASH', 900, TRUE),
                                                                       ('SKIRT', 'IRON', 600, TRUE),
                                                                       ('SHIRT', 'STAIN_REMOVAL', 1000, TRUE),
                                                                       ('SHIRT', 'ALTERATION', 1200, TRUE),
                                                                       ('PANTS', 'STAIN_REMOVAL', 1500, TRUE),
                                                                       ('PANTS', 'ALTERATION', 2000, TRUE);

-- Modèles d'e-mails (français)
INSERT INTO email_templates (name, subject, html_body) VALUES
                                                           ('welcome', 'Bienvenue chez Pressing+', '<h1>Bienvenue !</h1><p>Merci de vous être inscrit.</p>'),
                                                           ('order_confirmation', 'Confirmation de votre commande', '<h1>Commande confirmée</h1><p>Votre commande #{{order_id}} a bien été enregistrée.</p>'),
                                                           ('password_reset', 'Réinitialisation de votre mot de passe', '<h1>Mot de passe oublié</h1><p>Cliquez sur le lien pour le réinitialiser.</p>'),
                                                           ('order_received', 'Votre commande {{order_id}} a bien été réceptionnée',
                                                            '<h1>Merci {{customer_name}} !</h1>
                                                             <p>Nous avons bien enregistré votre commande <strong>#{{order_id}}</strong>.</p>
                                                             <p>Date de dépôt : {{deposit_date}}<br>
                                                             Retrait estimé : {{estimated_pickup_date}}</p>
                                                             <p>Vous recevrez une notification dès que vos articles seront prêts.</p>'),
                                                           ('order_in_progress', 'Votre commande {{order_id}} est en cours de traitement',
                                                            '<h1>Bonjour {{customer_name}},</h1>
                                                             <p>Votre commande <strong>#{{order_id}}</strong> est actuellement en cours de traitement dans notre atelier.</p>
                                                             <p>Articles concernés : {{items_list}}</p>
                                                             <p>Nous mettons tout en œuvre pour vous la rendre dans les délais annoncés.</p>'),
                                                           ('order_ready', 'Votre commande {{order_id}} est prête !',
                                                            '<h1>Bonne nouvelle {{customer_name}} !</h1>
                                                             <p>Votre commande <strong>#{{order_id}}</strong> est prête et vous attend en boutique.</p>
                                                             <p>Date de disponibilité : {{ready_date}}<br>
                                                             Boutique : {{shop_address}}</p>
                                                             <p>Merci de venir la récupérer avant le {{expiry_date}}.</p>'),
                                                           ('order_completed', 'Votre commande {{order_id}} a été retirée',
                                                            '<h1>Merci {{customer_name}} !</h1>
                                                             <p>Nous confirmons que vous avez bien retiré votre commande <strong>#{{order_id}}</strong> le {{pickup_date}}.</p>
                                                             <p>Nous espérons que le résultat vous donne satisfaction. À bientôt !</p>'),
                                                           ('order_delayed', 'Information concernant votre commande {{order_id}}',
                                                            '<h1>Bonjour {{customer_name}},</h1>
                                                             <p>Nous tenons à vous informer que le traitement de votre commande <strong>#{{order_id}}</strong> prend un peu plus de temps que prévu.</p>
                                                             <p>Nouvelle estimation de disponibilité : {{new_estimated_date}}</p>
                                                             <p>Nous vous prions de nous excuser pour ce désagrément.</p>'),
                                                           ('pickup_reminder', 'Rappel : votre commande {{order_id}} vous attend',
                                                            '<h1>Bonjour {{customer_name}},</h1>
                                                             <p>Votre commande <strong>#{{order_id}}</strong> est prête depuis le {{ready_date}} et n’a pas encore été récupérée.</p>
                                                             <p>Dernier délai de retrait : {{expiry_date}}.</p>
                                                             <p>Passé ce délai, des frais de garde pourraient s’appliquer.</p>'),
                                                           ('all_items_washed', 'Votre commande {{order_id}} : tous les articles ont été lavés',
                                                            '<h1>Bonjour {{customer_name}},</h1>
                                                             <p>Bonne nouvelle : tous les vêtements de votre commande <strong>#{{order_id}}</strong> ont terminé l’étape de <strong>lavage</strong>.</p>
                                                             <p>Ils passent maintenant aux étapes de séchage et de finition.</p>
                                                             <p>Articles concernés : {{items_list}}</p>
                                                             <p>Nous vous tiendrons informé(e) de la suite.</p>'),
                                                           ('all_items_ironed', 'Commande {{order_id}} : repassage terminé pour tous vos articles',
                                                            '<h1>Bonjour {{customer_name}},</h1>
                                                             <p>Le repassage de l’ensemble des articles de votre commande <strong>#{{order_id}}</strong> vient de s’achever.</p>
                                                             <p>{{items_list}}</p>
                                                             <p>Prochaine étape : contrôle qualité.</p>
                                                             <p>À bientôt !</p>'),
                                                           ('all_items_quality_checked', 'Commande {{order_id}} : tous vos articles passent le contrôle qualité',
                                                            '<h1>Bonjour {{customer_name}},</h1>
                                                             <p>Un petit mot pour vous dire que chaque vêtement de votre commande <strong>#{{order_id}}</strong> est actuellement vérifié par notre équipe qualité.</p>
                                                             <p>Nous nous assurons que tout soit impeccable avant la mise à disposition.</p>
                                                             <p>Vous serez prévenu(e) dès que le colis sera prêt.</p>'),
                                                           ('all_items_packed', 'Commande {{order_id}} : vos vêtements sont emballés',
                                                            '<h1>Bonjour {{customer_name}},</h1>
                                                             <p>Nous venons de finaliser l’emballage de votre commande <strong>#{{order_id}}</strong>.</p>
                                                             <p>{{items_list}} sont maintenant protégés et sur le point d’être acheminés vers notre point de retrait.</p>
                                                             <p>Un dernier contrôle et vous recevrez la confirmation de mise à disposition.</p>');

-- Produits (consommables)
INSERT INTO products (name, threshold_value, measurement_unit) VALUES
                                                                   ('Lessive Omo', 5.00, 'KG'),
                                                                   ('Détachant local', 2.00, 'LITER'),
                                                                   ('Assouplissant', 3.00, 'LITER'),
                                                                   ('Carton de protection', 50.00, 'UNIT'),
                                                                   ('Cintre', 100.00, 'UNIT'),
                                                                   ('Sachet plastique', 200.00, 'PACKET');

-- Stocks initiaux
INSERT INTO stocks (product_id, current_quantity) VALUES
                                                      (1, 20.50),
                                                      (2, 8.00),
                                                      (3, 12.00),
                                                      (4, 200.00),
                                                      (5, 500.00),
                                                      (6, 300.00);

-- Commande n°1 de Luc Owona (id=5) : terminée, payée
INSERT INTO orders (client_user_id, deposit_date, expected_delivery_date, status, payment_status, total_amount, created_by) VALUES
    (5, '2026-06-15', '2026-06-18', 'READY', 'COMPLETED', 3800, 1);

-- Articles avec taille directement en ENUM
INSERT INTO articles (order_id, clothing_type, size, fabric, color, distinction, status) VALUES
                                                                                             (1, 'SHIRT', 'M', 'COTTON', 'Blanc', 'Boutons nacrés', 'COMPLETED'),
                                                                                             (1, 'PANTS', 'L', 'COTTON', 'Bleu', 'Coupe droite', 'COMPLETED');

-- Services associés
INSERT INTO article_services (article_id, service, applied_price) VALUES
                                                                      (1, 'DRY_CLEAN', 1500),
                                                                      (1, 'IRON', 300),
                                                                      (2, 'DRY_CLEAN', 2000);

-- Ticket
INSERT INTO tickets (order_id, barcode, status) VALUES (1, 'PRS-001-CMR', 'GENERATED');

-- Paiement Mobile Money (règle intégralement la commande n°1)
INSERT INTO payments (order_id, payment_method, amount, payer_phone, transaction_reference, status, created_by) VALUES
    (1, 'MOBILE_PAYMENT', 3800, '237698765432', 'OM-20260615-001', 'COMPLETED', 1);

-- Commande n°2 de Marie Essimi (id=6) : en cours de traitement, paiement en attente
INSERT INTO orders (client_user_id, deposit_date, expected_delivery_date, status, payment_status, total_amount, created_by) VALUES
    (6, '2026-06-20', '2026-06-23', 'IN_PROGRESS', 'PENDING', 4000, 1);

INSERT INTO articles (order_id, clothing_type, size, fabric, color, distinction, status) VALUES
                                                                                             (2, 'SKIRT', 'S', 'COTTON', 'Multicolore', 'Motif wax', 'WASHING'),
                                                                                             (2, 'DRESS', 'M', 'COTTON', 'Rouge', 'Fleurs brodées', 'PENDING');

INSERT INTO article_services (article_id, service, applied_price) VALUES
                                                                      (3, 'WASH', 900),
                                                                      (3, 'IRON', 600),
                                                                      (4, 'DRY_CLEAN', 2500);

-- Traitement en cours
INSERT INTO treatments (article_id, employee_user_id, service, started_at, notes) VALUES
    (3, 4, 'WASH', '2026-06-21 08:15:00', 'Lavage délicat à la main');

-- Mouvement de stock
INSERT INTO stock_movements (stock_id, treatment_id, user_id, quantity, movement_type, notes) VALUES
    (1, 1, 4, 0.5, 'CONSUMPTION', 'Utilisé pour lavage article #3');

-- Réapprovisionnement
INSERT INTO product_registrations (product_id, employee_user_id, quantity, registration_type, notes) VALUES
    (1, 3, 5.00, 'IN', 'Approvisionnement hebdomadaire');

-- Mise à jour manuelle des stocks (pas de trigger dans ce script)
UPDATE stocks SET current_quantity = current_quantity - 0.5 WHERE product_id = 1;
UPDATE stocks SET current_quantity = current_quantity + 5 WHERE product_id = 1;

-- Commande n°3 de Luc Owona : annulée après dépôt, paiement remboursé
-- (couvre les statuts CANCELLED / REFUNDED, absents des jeux de données
-- précédents, pour des tests et démonstrations plus complets)
INSERT INTO orders (client_user_id, deposit_date, expected_delivery_date, status, payment_status, total_amount, created_by) VALUES
    (5, '2026-06-25', '2026-06-28', 'CANCELLED', 'REFUNDED', 1500, 1);

INSERT INTO articles (order_id, clothing_type, size, fabric, color, distinction, status) VALUES
    (3, 'SHIRT', 'L', 'COTTON', 'Noir', NULL, 'PENDING');

INSERT INTO article_services (article_id, service, applied_price) VALUES
    (5, 'DRY_CLEAN', 1500);

INSERT INTO payments (order_id, payment_method, amount, payer_phone, transaction_reference, status, created_by) VALUES
    (3, 'MOBILE_PAYMENT', 1500, '237698765432', 'OM-20260625-002', 'REFUNDED', 1);

-- Notifications (une par commande, statut cohérent avec l'issue de l'envoi)
INSERT INTO notifications (user_id, order_id, template_id, subject, message, notification_type, status, is_read) VALUES
    (5, 1, (SELECT id FROM email_templates WHERE name='order_ready'),   'Votre commande est prête',   'Commande #1 prête à Douala - Bonaberi.', 'EMAIL', 'SUCCESS', TRUE),
    (6, 2, (SELECT id FROM email_templates WHERE name='order_in_progress'), 'Commande en cours', 'Commande #2 en cours de traitement.', 'EMAIL', 'SUCCESS', FALSE),
    (5, 3, (SELECT id FROM email_templates WHERE name='order_delayed'), 'Commande annulée', 'Commande #3 annulée, remboursement effectué.', 'SMS', 'SUCCESS', FALSE);

-- ============================================================
-- FIN DU SCRIPT
-- ============================================================
