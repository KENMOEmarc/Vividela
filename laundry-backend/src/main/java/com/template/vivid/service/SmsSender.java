package com.template.vivid.service;

public interface SmsSender {
    /**
     * Envoie un SMS.
     * @return true si l'envoi a réellement réussi, false sinon (échec,
     *         numéro manquant, ou Twilio non configuré).
     */
    boolean sendSms(String phoneNumber, String message);
}

