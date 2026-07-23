package com.template.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * AJOUT : durée de validité (en jours) d'un ticket de dépôt avant expiration
 * automatique (voir revue de code — règle manquante n°16 : "Un ticket ne
 * peut jamais expirer"). Passé ce délai depuis l'émission, un ticket bascule
 * en {@code TicketStatus.EXPIRED} et ne peut plus être présenté au guichet
 * pour retirer les vêtements sans validation manuelle du personnel.
 */
@Configuration
@ConfigurationProperties(prefix = "vividela.ticket")
@Getter
@Setter
public class TicketProperties {

    private int validityDays = 90;
}
