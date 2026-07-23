package com.template.auth.model.enums;

/**
 * Templates d'emails disponibles pour les notifications.
 */
public enum EmailTemplate {
    WELCOME("WELCOME", "Email de Bienvenue"),
    PASSWORD_RESET("PASSWORD_RESET", "Réinitialisation du Mot de Passe"),
    EMAIL_VERIFICATION("EMAIL_VERIFICATION", "Vérification de l'Email"),
    ORDER_CONFIRMATION("ORDER_CONFIRMATION", "Confirmation de Commande"),
    ORDER_READY("ORDER_READY", "Commande Prête"),
    DELIVERY_NOTIFICATION("DELIVERY_NOTIFICATION", "Notification de Livraison"),
    PAYMENT_CONFIRMATION("PAYMENT_CONFIRMATION", "Confirmation de Paiement"),
    INVOICE("INVOICE", "Facture"),
    REMINDER("REMINDER", "Rappel"),
    SUPPORT_TICKET("SUPPORT_TICKET", "Ticket de Support"),
    FEEDBACK_REQUEST("FEEDBACK_REQUEST", "Demande d'Avis"),
    ACCOUNT_VERIFICATION("ACCOUNT_VERIFICATION", "Vérification du Compte"),
    PROMOTIONAL("PROMOTIONAL", "Email Promotionnel");

    private final String name;
    private final String label;

    EmailTemplate(String name, String label) {
        this.name = name;
        this.label = label;
    }

    public String getName() {
        return name;
    }

    public String getLabel() {
        return label;
    }
}

