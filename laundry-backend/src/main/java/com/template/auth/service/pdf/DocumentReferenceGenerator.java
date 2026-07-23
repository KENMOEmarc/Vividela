package com.template.auth.service.pdf;

import java.util.UUID;

/**
 * Génère les références (numéros de facture / de ticket) affichées sur les
 * documents PDF (reçus, tickets de dépôt). Format : PREFIX-XXXXXXXX
 * (8 caractères hexadécimaux majuscules), ex : REC-9F3A21B0.
 *
 * Cette référence est systématiquement réutilisée comme nom de fichier du PDF
 * généré, afin que le numéro imprimé sur le document corresponde toujours au
 * nom du fichier livré au client.
 */
public final class DocumentReferenceGenerator {

    private DocumentReferenceGenerator() {
        // Classe utilitaire : pas d'instanciation
    }

    public static String generateReceiptReference(Long orderId) {
        return generate("REC");
    }

    public static String generateTicketReference(Long orderId) {
        return generate("TCK");
    }

    private static String generate(String prefix) {
        String hex = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return prefix + "-" + hex;
    }
}
