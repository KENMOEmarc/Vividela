package com.vivid.model.mapper;

import com.vivid.model.dto.ArticleDto;
import com.vivid.model.dto.ArticleServiceDto;
import com.vivid.model.entity.Article;
import com.vivid.model.entity.ArticleServiceLine;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper centralisant la conversion Article (entity) ↔ ArticleDto.
 */
public final class ArticleMapper {

    private ArticleMapper() {
        // Classe utilitaire : pas d'instanciation
    }

    /** Variante sans services (services/totalPrice laissés vides). */
    public static ArticleDto toDto(Article article) {
        return toDto(article, article != null && article.getOrder() != null ? article.getOrder().getId() : null, Collections.emptyList());
    }

    /**
     * Variante utilisée lorsque l'ID de la commande est déjà connu
     * (évite un appel supplémentaire à article.getOrder()).
     */
    public static ArticleDto toDto(Article article, Long orderId) {
        return toDto(article, orderId, Collections.emptyList());
    }

    /**
     * Variante complète incluant les services appliqués (et leur prix) à l'article.
     */
    public static ArticleDto toDto(Article article, Long orderId, List<ArticleServiceLine> services) {
        if (article == null) {
            return null;
        }

        List<ArticleServiceDto> serviceDtos = services == null ? List.of() : services.stream()
                .map(s -> ArticleServiceDto.builder()
                        .service(s.getService())
                        .appliedPrice(s.getAppliedPrice())
                        .build())
                .collect(Collectors.toList());

        BigDecimal totalPrice = serviceDtos.stream()
                .map(ArticleServiceDto::getAppliedPrice)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return ArticleDto.builder()
                .id(article.getId())
                .orderId(orderId)
                .clothingType(article.getClothingType())
                .size(article.getSize())
                .sizeName(article.getSize() != null ? article.getSize().name() : null)
                .fabric(article.getFabric())
                .color(article.getColor()) // String directement
                .distinction(article.getDistinction())
                .status(article.getStatus())
                .services(serviceDtos)
                .totalPrice(totalPrice)
                .createdAt(toLocalDateTime(article.getCreatedAt()))
                .updatedAt(toLocalDateTime(article.getUpdatedAt()))
                .build();
    }

    public static List<ArticleDto> toDtoList(List<Article> articles) {
        if (articles == null) {
            return List.of();
        }
        return articles.stream()
                .map(ArticleMapper::toDto)
                .collect(Collectors.toList());
    }

    private static LocalDateTime toLocalDateTime(java.time.Instant instant) {
        return instant != null
                ? LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
                : null;
    }
}
