package com.template.vivid.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Résultat de la génération d'un PDF (ticket ou reçu) : le contenu binaire,
 * accompagné de sa référence (= numéro de facture affiché sur le document).
 * Cette référence sert aussi de nom de fichier, afin que le numéro visible
 * sur le document corresponde toujours exactement au nom du fichier livré.
 */
@Getter
@AllArgsConstructor
public class GeneratedPdfDto {
    private final byte[] content;
    private final String reference;

    /** Nom de fichier complet (référence + extension .pdf). */
    public String fileName() {
        return reference + ".pdf";
    }
}
