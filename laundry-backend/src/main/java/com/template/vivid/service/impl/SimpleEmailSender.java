package com.template.vivid.service.impl;

import com.template.vivid.service.EmailSender;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/**
 * Implémentation de l'envoi d'emails via JavaMailSender (SMTP).
 * <p>
 * Configuration attendue (variables d'environnement, voir application.yml) :
 * MAIL_HOST, MAIL_PORT, MAIL_USERNAME, MAIL_PASSWORD, MAIL_FROM
 * <p>
 * Si aucune configuration SMTP n'est fournie (MAIL_USERNAME vide), l'envoi
 * est simplement loggé plutôt que réellement transmis — utile en
 * développement local sans compte SMTP.
 */
@Slf4j
@Component
public class SimpleEmailSender implements EmailSender {

    private final JavaMailSender mailSender;

    @Value("${notifications.mail-from}")
    private String mailFrom;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    public SimpleEmailSender(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public boolean sendEmail(String to, String subject, String body) {
        if (to == null || to.isBlank()) {
            log.warn("[Email] Envoi annulé : adresse destinataire manquante (subject={})", subject);
            return false;
        }

        if (mailUsername == null || mailUsername.isBlank()) {
            // Pas de compte SMTP configuré -> on se contente de logguer (mode dev).
            // BUGFIX : ce n'est PAS un envoi réussi, donc on renvoie false pour que
            // le statut de notification enregistré en base reflète la réalité.
            log.info("[Email] (SMTP non configuré, log seulement) To={} Subject={} Body={}", to, subject, body);
            return false;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(mailFrom);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, false);

            mailSender.send(message);
            log.info("[Email] Envoyé avec succès à {} — Subject={}", to, subject);
            return true;
        } catch (MessagingException | MailException e) {
            // On ne relance pas d'exception : un échec d'envoi d'email ne doit pas
            // faire échouer l'opération métier (création de commande, paiement, etc.)
            log.error("[Email] Échec de l'envoi à {} — Subject={} — Erreur: {}", to, subject, e.getMessage());
            return false;
        }
    }
}
