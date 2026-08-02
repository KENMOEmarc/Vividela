package com.template.vivid.repository;

import com.template.vivid.model.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    /**
     * Le formulaire d'avis (demandé et/ou rempli) d'une commande donnée.
     */
    Optional<Feedback> findByOrderId(Long orderId);

    /**
     * Variante de {@code findById} qui charge en plus (JOIN FETCH) la
     * commande et son client associés, en une seule requête.
     * <p>
     * Utilisée par {@code FeedbackSentimentUpdater}, appelée depuis le
     * pipeline asynchrone d'analyse IA (voir {@code FeedbackAnalysisCoordinator}) :
     * ce pipeline s'exécute sur un thread du pool {@code notificationExecutor},
     * en dehors de toute session Hibernate ouverte par la requête HTTP
     * d'origine. Sans ce JOIN FETCH, {@code feedback.getOrder()} et
     * {@code order.getClientUser()} (tous deux {@code FetchType.LAZY})
     * lèveraient une {@code LazyInitializationException} dès qu'on tente de
     * construire le corps de la notification manager/admin.
     */
    @Query("select f from Feedback f join fetch f.order o join fetch o.clientUser where f.id = :id")
    Optional<Feedback> findByIdWithOrderAndClient(@Param("id") Long id);

    /**
     * Vérifie qu'un formulaire a déjà été généré pour la commande — utilisé
     * pour rendre {@code requestFeedback} idempotent (une commande ne peut
     * transiter qu'une fois vers DELIVERED en temps normal, mais on se
     * protège d'un double appel).
     */
    boolean existsByOrderId(Long orderId);

    /**
     * Récupération groupée (1 requête) pour plusieurs commandes à la fois —
     * utilisée par OrderServiceImpl pour peupler OrderDto.feedbackStatus
     * sans provoquer de N+1.
     */
    List<Feedback> findByOrderIdIn(Collection<Long> orderIds);
}
