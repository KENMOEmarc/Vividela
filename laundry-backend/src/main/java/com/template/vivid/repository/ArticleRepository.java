package com.template.vivid.repository;

import com.template.vivid.model.entity.Article;
import com.template.vivid.model.enums.ArticleStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ArticleRepository extends JpaRepository<Article, Long> {
    List<Article> findByOrderId(Long orderId);

    /**
     * Charge en une seule requête les articles de plusieurs commandes à la
     * fois. Utilisée par OrderServiceImpl pour éviter les requêtes N+1
     * lorsqu'on mappe une liste de commandes en DTO (getAllOrders,
     * getOrdersByClient, getOrdersByStatuses...).
     */
    List<Article> findByOrderIdIn(List<Long> orderIds);

    // Si vous souhaitez chercher par statut
    List<Article> findByStatus(ArticleStatus status);
}
