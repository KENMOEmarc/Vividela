package com.vivid.model.mapper;

import com.vivid.model.dto.ArticleDto;
import com.vivid.model.dto.OrderDto;
import com.vivid.model.entity.Article;
import com.vivid.model.entity.ArticleServiceLine;
import com.vivid.model.entity.Order;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Mapper centralisant la conversion Order (entity) ↔ OrderDto.
 */
public final class OrderMapper {

    private OrderMapper() {
        // Classe utilitaire : pas d'instanciation
    }

    /**
     * @param order           l'entité commande
     * @param articles        les articles rattachés à la commande (pour itemCount et, si demandé, le détail)
     * @param includeArticles si true, la liste détaillée des ArticleDto est incluse dans le DTO
     */
    public static OrderDto toDto(Order order, List<Article> articles, boolean includeArticles) {
        return toDto(order, articles, includeArticles, Map.of(), null, null, null);
    }

    /**
     * @param servicesByArticle services (avec prix appliqué) indexés par ID d'article,
     *                          utilisé pour peupler ArticleDto.services / totalPrice.
     */
    public static OrderDto toDto(Order order, List<Article> articles, boolean includeArticles,
                                  Map<Long, List<ArticleServiceLine>> servicesByArticle) {
        return toDto(order, articles, includeArticles, servicesByArticle, null, null, null);
    }

    /**
     * @param ticketNumber référence du ticket (code-barre) associé à la commande, si déjà généré ;
     *                     permet la recherche d'une commande par numéro de ticket côté frontend.
     */
    public static OrderDto toDto(Order order, List<Article> articles, boolean includeArticles,
                                 Map<Long, List<ArticleServiceLine>> servicesByArticle, String ticketNumber) {
        return toDto(order, articles, includeArticles, servicesByArticle, ticketNumber, null, null);
    }

    /**
     * Surcharge complète avec les paramètres calculés.
     * @param netAmountDue montant net après remise et points de fidélité
     * @param feedbackStatus état du formulaire d'avis (REQUESTED/SUBMITTED)
     */
    public static OrderDto toDto(Order order, List<Article> articles, boolean includeArticles,
                                 Map<Long, List<ArticleServiceLine>> servicesByArticle, String ticketNumber,
                                 BigDecimal netAmountDue, String feedbackStatus) {
        if (order == null) {
            return null;
        }

        int itemCount = articles != null ? articles.size() : 0;

        List<ArticleDto> articleDtos = null;
        if (includeArticles && articles != null) {
            articleDtos = articles.stream()
                    .map(a -> ArticleMapper.toDto(a, order.getId(),
                            servicesByArticle.getOrDefault(a.getId(), List.of())))
                    .collect(Collectors.toList());
        }

        return new OrderDto(
                order.getId(),
                order.getClientUser() != null ? order.getClientUser().getId() : null,
                order.getClientUser() != null ? order.getClientUser().getUserName() : null,
                ticketNumber,
                order.getClientUser() != null ? order.getClientUser().getFirstName() : null,
                order.getClientUser() != null ? order.getClientUser().getLastName() : null,
                order.getClientUser() != null ? order.getClientUser().getEmail() : null,
                order.getClientUser() != null ? order.getClientUser().getPhone() : null,
                order.getDepositDate(),
                order.getExpectedDeliveryDate(),
                order.getDeliveredAt(),
                order.getStatus(),
                order.getPaymentStatus(),
                order.getShippingAddress(),
                order.getNotes(),
                order.getTotalAmount(),
                order.getDiscountAmount(),
                netAmountDue,
                order.getLoyaltyPointsUsed(),
                feedbackStatus,
                itemCount,
                order.getCreatedAt(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                order.getCreatedBy() != null ? order.getCreatedBy().getId() : null,
                order.getUpdatedBy() != null ? order.getUpdatedBy().getId() : null,
                articleDtos
        );
    }
}
