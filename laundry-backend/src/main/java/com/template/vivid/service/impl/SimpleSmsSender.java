package com.template.vivid.service.impl;

import com.template.vivid.service.SmsSender;
import com.twilio.Twilio;
import com.twilio.exception.ApiException;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Implémentation de l'envoi de SMS via Twilio.
 * <p>
 * Twilio propose un compte d'essai (trial) gratuit : https://www.twilio.com/try-twilio
 * En mode trial :
 * - un numéro Twilio gratuit est fourni (TWILIO_FROM_NUMBER)
 * - les SMS ne peuvent être envoyés qu'à des numéros vérifiés dans la console Twilio
 * - chaque SMS est préfixé automatiquement par "Sent from your Twilio trial account"
 * <p>
 * Configuration attendue (voir application.yml) :
 * TWILIO_ENABLED=true, TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN, TWILIO_FROM_NUMBER
 * <p>
 * Si TWILIO_ENABLED=false (ou credentials manquants), l'envoi est simplement
 * loggué — utile en développement local sans compte Twilio.
 */
@Slf4j
@Component
public class SimpleSmsSender implements SmsSender {

    @Value("${twilio.enabled:false}")
    private boolean enabled;

    @Value("${twilio.account-sid:}")
    private String accountSid;

    @Value("${twilio.auth-token:}")
    private String authToken;

    @Value("${twilio.from-number:}")
    private String fromNumber;

    private boolean initialized = false;

    @PostConstruct
    public void init() {
        if (enabled && !accountSid.isBlank() && !authToken.isBlank() && !fromNumber.isBlank()) {
            Twilio.init(accountSid, authToken);
            initialized = true;
            log.info("[SMS] Client Twilio initialisé (from={})", fromNumber);
        } else if (enabled) {
            log.warn("[SMS] TWILIO_ENABLED=true mais des credentials Twilio sont manquants "
                    + "(TWILIO_ACCOUNT_SID / TWILIO_AUTH_TOKEN / TWILIO_FROM_NUMBER) — envoi désactivé.");
        }
    }

    @Override
    public boolean sendSms(String phoneNumber, String message) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            log.warn("[SMS] Envoi annulé : numéro de téléphone manquant.");
            return false;
        }

        if (!initialized) {
            // Twilio non configuré -> on se contente de logguer (mode dev).
            // BUGFIX : ce n'est PAS un envoi réussi, donc on renvoie false pour que
            // le statut de notification enregistré en base reflète la réalité.
            log.info("[SMS] (Twilio non configuré, log seulement) To={} Message={}", phoneNumber, message);
            return false;
        }

        try {
            Message sms = Message.creator(
                    new PhoneNumber(phoneNumber),
                    new PhoneNumber(fromNumber),
                    message
            ).create();
            log.info("[SMS] Envoyé avec succès à {} — Sid={} Status={}", phoneNumber, sms.getSid(), sms.getStatus());
            return true;
        } catch (ApiException e) {
            // Un échec d'envoi de SMS ne doit pas faire échouer l'opération métier.
            log.error("[SMS] Échec de l'envoi à {} — Erreur Twilio: {}", phoneNumber, e.getMessage());
            return false;
        }
    }
}
