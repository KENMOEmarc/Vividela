package com.template.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Identité de la boutique affichée sur les documents PDF (ticket de dépôt,
 * reçu de vente).
 *
 * Voir revue de code — "Identité boutique en dur dans le code" : ces valeurs
 * étaient dupliquées en constantes statiques dans TicketPdfGenerator et
 * ReceiptPdfGenerator, ce qui imposait une recompilation pour tout
 * changement (numéro de téléphone, nom en cas de multi-boutiques...).
 * Elles sont désormais externalisées dans application.yml sous le préfixe
 * "vividela.shop" et injectées via cette classe.
 */
@Configuration
@ConfigurationProperties(prefix = "vividela.shop")
@Getter
@Setter
public class ShopProperties {

    private String name = "Vividela";
    private String phone = "Tél : +237 000 000 000";
    private String email = "contact@vividela.com";
}
