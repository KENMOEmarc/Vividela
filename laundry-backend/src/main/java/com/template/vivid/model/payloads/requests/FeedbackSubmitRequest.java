package com.template.vivid.model.requests;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Corps de la requête POST /orders/{orderId}/feedback (soumission client). */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class FeedbackSubmitRequest {

    @NotNull(message = "La note est obligatoire")
    @Min(value = 1, message = "La note doit être comprise entre 1 et 5")
    @Max(value = 5, message = "La note doit être comprise entre 1 et 5")
    private Integer rating;

    @Size(max = 2000, message = "Le commentaire ne peut pas dépasser 2000 caractères")
    private String comment;
}
