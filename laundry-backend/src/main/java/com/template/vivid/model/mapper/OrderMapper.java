package com.template.vivid.model.mapper;

import com.template.vivid.model.dto.ArticleDto;
import com.template.vivid.model.dto.OrderDto;
import com.template.vivid.model.entity.Article;
import com.template.vivid.model.entity.ArticleServiceLine;
import com.template.vivid.model.entity.Order;

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
        return toDto(order, articles, includeArticles, Map.of(), null);
    }

    /**
     * @param servicesByArticle services (avec prix appliqué) indexés par ID d'article,
     *                          utilisé pour peupler ArticleDto.services / totalPrice.
     */
    public static OrderDto toDto(Order order, List<Article> articles, boolean includeArticles,
                                  Map<Long, List<ArticleServiceLine>> servicesByArticle) {
        return toDto(order, articles, includeArticles, servicesByArticle, null);
    }

    /**
     * @param ticketNumber référence du ticket (code-barre) associé à la commande, si déjà généré ;
     *                     permet la recherche d'une commande par numéro de ticket côté frontend.
     */
    public static OrderDto toDto(Order order, List<Article> articles, boolean includeArticles,
                                 Map<Long, List<ArticleServiceLine>> servicesByArticle, String ticketNumber) {
        if (order == null) {
            return null;
        }

        int itemCount = articles != null ? articles.size() : 0;

        OrderDto dto = OrderDto.builder()
                .id(order.getId())
                .clientUserId(order.getClientUser() != null ? order.getClientUser().getId() : null)
                .userName(order.getClientUser() != null ? order.getClientUser().getUserName() : null)
                .ticketNumber(ticketNumber)
                .customerName(order.getClientUser() != null ? order.getClientUser().getFirstName() : null)
                .customerLastName(order.getClientUser() != null ? order.getClientUser().getLastName() : null)
                .customerEmail(order.getClientUser() != null ? order.getClientUser().getEmail() : null)
                .customerPhone(order.getClientUser() != null ? order.getClientUser().getPhone() : null)
                .depositDate(order.getDepositDate())
                .expectedDeliveryDate(order.getExpectedDeliveryDate())
                .deliveredAt(order.getDeliveredAt())
                .status(order.getStatus())
                .paymentStatus(order.getPaymentStatus())
                .shippingAddress(order.getShippingAddress())
                .notes(order.getNotes())
                .totalAmount(order.getTotalAmount())
                .discountAmount(order.getDiscountAmount())
                .loyaltyPointsUsed(order.getLoyaltyPointsUsed())
                .itemCount(itemCount)
                .orderDate(order.getCreatedAt())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .createdBy(order.getCreatedBy() != null ? order.getCreatedBy().getId() : null)
                .updatedBy(order.getUpdatedBy() != null ? order.getUpdatedBy().getId() : null)
                .build();

        if (includeArticles && articles != null) {
            List<ArticleDto> articleDtos = articles.stream()
                    .map(a -> ArticleMapper.toDto(a, order.getId(),
                            servicesByArticle.getOrDefault(a.getId(), List.of())))
                    .collect(Collectors.toList());
            dto.setArticles(articleDtos);
        }

        return dto;
    }
}
