package com.template.vivid.service;

public interface EmailSender {
    /**
     * Envoie un email.
     * @return true si l'envoi a réellement réussi (ou a été journalisé en
     *         mode dev sans SMTP configuré est considéré comme NON envoyé —
     *         voir implémentation), false en cas d'échec ou de destinataire manquant.
     */
    boolean sendEmail(String to, String subject, String body);
}

