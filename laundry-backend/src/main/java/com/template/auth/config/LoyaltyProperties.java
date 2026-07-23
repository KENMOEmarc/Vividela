package com.template.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

/**
 * AJOUT : paramètres du programme de fidélité, jusqu'ici totalement
 * "décoratif" (voir revue de code — règles métier manquantes, point 12) :
 * les points utilisés n'étaient jamais comparés au solde réel, jamais
 * déduits, jamais convertis en réduction, et aucun point n'était jamais
 * crédité au client.
 * <p>
 * Ces deux taux définissent le fonctionnement du programme et sont
 * externalisés (comme {@link ShopProperties}) plutôt que codés en dur, afin
 * de pouvoir être ajustés sans recompilation :
 * <ul>
 *   <li><b>pointValue</b> : valeur en devise (XAF) d'UN point de fidélité
 *   utilisé, appliquée en réduction supplémentaire sur le montant net à
 *   payer d'une commande (en plus de {@code discountAmount}).</li>
 *   <li><b>earnRatePerCurrencyUnit</b> : nombre de points crédités au client
 *   par unité de devise réellement dépensée (net payé), lorsqu'une commande
 *   est livrée. Ex : 0.01 → 1 point gagné tous les 100 XAF dépensés.</li>
 * </ul>
 * Valeurs par défaut choisies à titre d'hypothèse de travail documentée ;
 * à ajuster selon la politique commerciale réelle de la boutique.
 */
@Configuration
@ConfigurationProperties(prefix = "vividela.loyalty")
@Getter
@Setter
public class LoyaltyProperties {

    /** Valeur en XAF d'un point de fidélité utilisé comme réduction. */
    private BigDecimal pointValue = new BigDecimal("50");

    /** Points gagnés par unité de devise (XAF) nette dépensée. */
    private BigDecimal earnRatePerCurrencyUnit = new BigDecimal("0.01");
}
