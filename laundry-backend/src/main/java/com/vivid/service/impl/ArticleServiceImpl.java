package com.vivid.service.impl;

import com.vivid.model.entity.Article;
import com.vivid.model.entity.ArticleServiceLine;
import com.vivid.model.entity.Order;
import com.vivid.model.entity.ServicePrice;
import com.vivid.model.enums.ArticleStatus;
import com.vivid.model.enums.OrderStatus;
import com.vivid.model.enums.PaymentStatus;
import com.vivid.model.payloads.requests.ArticleCreateRequest;
import com.vivid.model.dto.ArticleDto;
import com.vivid.model.payloads.requests.ArticleUpdateRequest;
import com.vivid.model.entity.*;
import com.vivid.model.enums.ServiceType;
import com.vivid.model.mapper.ArticleMapper;
import com.vivid.repository.*;
import com.vivid.repository.ArticleRepository;
import com.vivid.repository.ArticleServiceLineRepository;
import com.vivid.repository.OrderRepository;
import com.vivid.repository.ServicePriceRepository;
import com.vivid.service.ArticleService;
import com.vivid.service.NotificationService;
import com.vivid.service.OrderService;
import com.vivid.common.TransactionUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import com.vivid.exception.ResourceNotFoundException;
import com.vivid.exception.InvalidRequestException;
import com.vivid.exception.InvalidStateTransitionException;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ArticleServiceImpl implements ArticleService {

    private final ArticleRepository articleRepository;
    private final OrderRepository orderRepository;
    private final ArticleServiceLineRepository articleServiceRepository;
    private final ServicePriceRepository servicePriceRepository;
    private final OrderService orderService;
    private final NotificationService notificationService;

    @Override
    public ArticleDto create(ArticleCreateRequest request, Long orderId) {
        log.debug("Création d'un article pour la commande ID: {}", orderId);

        if (orderId == null) {
            throw new InvalidRequestException("Un article doit obligatoirement être rattaché à une commande.");
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Commande introuvable avec l'ID: " + orderId));

        // AJOUT : porte d'entrée principale du problème "commande
        // payée/livrée modifiable" (voir revue de code, règle manquante n°1
        // section Articles) — impossible d'ajouter un vêtement sur une
        // commande déjà livrée, annulée, ou intégralement payée.
        ensureOrderIsEditable(order);

        Article article = Article.builder()
                .order(order)
                .clothingType(request.getClothingType())
                .size(request.getSize())
                .fabric(request.getFabric())
                // CORRECTION : color est maintenant un String, plus de Color.decode()
                .color(normalizeColor(request.getColor()))
                .distinction(request.getDistinction())
                .status(request.getStatus())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        article = articleRepository.save(article);

        List<ArticleServiceLine> services = attachServices(article, request.getServices());

        orderService.recalculateTotal(orderId);
        // AJOUT : synchronise le statut de la commande avec celui de ses articles
        // (un nouvel article étant PENDING par défaut, une commande RECEIVED
        // passe alors automatiquement à PENDING).
        orderService.recalculateStatus(orderId);

        log.info("Article créé avec l'ID: {} ({} service(s) appliqué(s))", article.getId(), services.size());
        return ArticleMapper.toDto(article, orderId, services);
    }

    @Override
    public ArticleDto getArticleById(Long id) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Article introuvable avec l'ID: " + id));
        List<ArticleServiceLine> services = articleServiceRepository.findByArticleId(id);
        return ArticleMapper.toDto(article, article.getOrder() != null ? article.getOrder().getId() : null, services);
    }

    @Override
    public List<ArticleDto> getAllArticles() {
        return articleRepository.findAll().stream()
                .map(a -> ArticleMapper.toDto(
                        a,
                        a.getOrder() != null ? a.getOrder().getId() : null,
                        articleServiceRepository.findByArticleId(a.getId())))
                .collect(Collectors.toList());
    }

    @Override
    public ArticleDto updateArticle(Long id, ArticleUpdateRequest request) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Article introuvable avec l'ID: " + id));

        if (article.getOrder() != null) {
            ensureOrderIsEditable(article.getOrder());
        }

        if (request.getSize() != null) {
            article.setSize(request.getSize());
        }
        if (request.getFabric() != null) {
            article.setFabric(request.getFabric());
        }
        if (request.getColor() != null) {
            article.setColor(normalizeColor(request.getColor()));
        }
        if (request.getDistinction() != null) {
            article.setDistinction(request.getDistinction());
        }
        if (request.getStatus() != null) {
            article.setStatus(request.getStatus());
        }

        article.setUpdatedAt(Instant.now());
        article = articleRepository.save(article);

        List<ArticleServiceLine> services;
        if (request.getServices() != null) {
            // Remplace intégralement les services (et donc le prix) de l'article :
            // on supprime d'abord toutes les anciennes relations ArticleServiceLine,
            // puis on insère les nouvelles. La suppression (deleteByArticleId)
            // est désormais une requête de suppression en masse exécutée
            // IMMÉDIATEMENT en base (voir ArticleServiceLineRepository), afin que
            // les anciennes lignes aient bien disparu avant l'insertion des
            // nouvelles — sans quoi un service conservé d'une modification à
            // l'autre (ex : WASH gardé) violait la contrainte
            // UNIQUE(article_id, service) et rendait toute modification des
            // services impossible.
            articleServiceRepository.deleteByArticleId(article.getId());
            services = attachServices(article, request.getServices());
        } else {
            services = articleServiceRepository.findByArticleId(article.getId());
        }

        Long orderId = article.getOrder() != null ? article.getOrder().getId() : null;
        if (orderId != null) {
            orderService.recalculateTotal(orderId);

            // AJOUT : synchronise le statut de la commande avec celui de ses
            // articles (COMPLETED partout → READY, PENDING partout → PENDING,
            // sinon IN_PROGRESS pendant que le traitement est en cours).
            orderService.recalculateStatus(orderId);

            // Check if all articles in the order have the same status
            Order order = article.getOrder();
            List<Article> allArticles = articleRepository.findByOrderId(orderId);
            checkAndNotifyArticleStatusChanges(order, allArticles);
        }

        log.info("Article mis à jour ID: {}", article.getId());
        return ArticleMapper.toDto(article, orderId, services);
    }

    @Override
    public void deleteArticle(Long id) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Article introuvable avec l'ID: " + id));
        Long orderId = article.getOrder() != null ? article.getOrder().getId() : null;

        if (article.getOrder() != null) {
            ensureOrderIsEditable(article.getOrder());
        }

        articleRepository.delete(article); // cascade DB supprime les article_services associés

        if (orderId != null) {
            orderService.recalculateTotal(orderId);
            // AJOUT : la suppression d'un article peut faire basculer les
            // articles restants vers un statut homogène (ex : ne restent que
            // des articles COMPLETED) → on resynchronise le statut de la commande.
            orderService.recalculateStatus(orderId);
        }
        log.info("Article supprimé ID: {}", id);
    }

    @Override
    public Article findEntityById(Long id) {
        return articleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Article introuvable avec l'ID: " + id));
    }

    @Override
    public Article findByName(String name) {
        throw new UnsupportedOperationException("La recherche par nom n'est pas supportée pour les articles.");
    }

    /**
     * Résout le tarif de chaque service demandé (via ServicePrice, en fonction du
     * type de vêtement de l'article), puis enregistre les ArticleServiceLine correspondants.
     * C'est ce prix appliqué qui sert ensuite au recalcul du total de la commande.
     */
    private List<ArticleServiceLine> attachServices(Article article, List<ServiceType> requestedServices) {
        if (requestedServices == null || requestedServices.isEmpty()) {
            throw new InvalidRequestException("Sélectionnez au moins un service pour l'article.");
        }

        List<ArticleServiceLine> toCreate = requestedServices.stream()
                .distinct()
                .map(serviceType -> {
                    ServicePrice servicePrice = servicePriceRepository
                            .findByClothingTypeAndServiceAndActiveTrue(article.getClothingType(), serviceType)
                            .orElseThrow(() -> new ResourceNotFoundException(
                                    "Aucun tarif actif configuré pour le service " + serviceType
                                            + " sur le type de vêtement " + article.getClothingType()));

                    return ArticleServiceLine.builder()
                            .article(article)
                            .service(serviceType)
                            .appliedPrice(servicePrice.getPrice())
                            .build();
                })
                .collect(Collectors.toList());

        return articleServiceRepository.saveAll(toCreate);
    }

    /**
     * AJOUT : garde-fou empêchant d'ajouter, modifier ou supprimer un
     * article sur une commande déjà livrée, annulée, ou intégralement
     * payée — voir revue de code, règle manquante n°1 (section Articles) :
     * c'était la porte d'entrée principale permettant de faire varier
     * totalAmount (via recalculateTotal()) sur une commande déjà réglée.
     */
    private void ensureOrderIsEditable(Order order) {
        if (order.getStatus() == OrderStatus.DELIVERED
                || order.getStatus() == OrderStatus.CANCELLED
                || order.getPaymentStatus() == PaymentStatus.COMPLETED) {
            throw new InvalidStateTransitionException(
                    "Impossible de modifier les articles de la commande #" + order.getId()
                            + " : elle est " + (order.getStatus() == OrderStatus.DELIVERED
                            ? "livrée" : order.getStatus() == OrderStatus.CANCELLED
                                         ? "annulée" : "déjà payée intégralement") + ".");
        }
    }

    /**
     * Normalise la couleur : s'assure que le code hex est préfixé par '#'.
     * Retourne null si la couleur est vide.
     */
    private String normalizeColor(String color) {
        if (color == null || color.isBlank()) {
            return null;
        }
        return color.startsWith("#") ? color : "#" + color;
    }

    /**
     * Vérifie si tous les articles d'une commande ont le même statut.
     * Si c'est le cas, envoie une notification.
     * Si le statut est COMPLETED, envoie une notification spéciale.
     */
    private void checkAndNotifyArticleStatusChanges(Order order, List<Article> articles) {
        if (articles == null || articles.isEmpty()) {
            return;
        }

        // Check if all articles have the same status
        ArticleStatus firstStatus = articles.getFirst().getStatus();
        boolean allSameStatus = articles.stream()
                .allMatch(a -> a.getStatus() == firstStatus);

        if (allSameStatus) {
            log.info("Tous les articles de la commande {} ont le statut: {}", order.getId(), firstStatus);

            if (firstStatus != ArticleStatus.COMPLETED) {
                // All articles have changed to the same state - notify only in-app
                TransactionUtils.runAfterCommit(() -> notificationService.notifyAllArticlesSameState(order, firstStatus.toString()));
            }
            // NOTE : quand tous les articles passent à COMPLETED, la commande
            // elle-même bascule vers OrderStatus.READY ("terminée") via
            // orderService.recalculateStatus(orderId), appelé juste avant dans
            // updateArticle(). Ce changement de statut de commande déclenche déjà,
            // de façon asynchrone (CompletableFuture), la notification IN_APP +
            // email correspondante (voir NotificationServiceImpl
            // #notifyOrderStatusChangedAsync). On ne rappelle donc plus
            // notifyAllArticlesReady() ici, pour éviter d'envoyer la même
            // notification deux fois.
        }
    }
}