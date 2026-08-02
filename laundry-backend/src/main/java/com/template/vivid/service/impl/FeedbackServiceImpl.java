package com.template.vivid.service.impl;

import com.template.vivid.exception.ResourceNotFoundException;
import com.template.vivid.model.dto.FeedbackDto;
import com.template.vivid.model.dto.FeedbackSubmitRequest;
import com.template.vivid.model.entity.Feedback;
import com.template.vivid.model.entity.Order;
import com.template.vivid.model.enums.OrderStatus;
import com.template.vivid.model.mapper.FeedbackMapper;
import com.template.vivid.repository.FeedbackRepository;
import com.template.vivid.repository.OrderRepository;
import com.template.vivid.service.FeedbackService;
import com.template.vivid.service.NotificationService;
import com.template.vivid.common.TransactionUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class FeedbackServiceImpl implements FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final OrderRepository orderRepository;
    private final NotificationService notificationService;
    private final FeedbackAnalysisCoordinator feedbackAnalysisCoordinator;
    private final Clock clock;

    @Override
    public void requestFeedback(Order order) {
        if (order == null || order.getId() == null) {
            return;
        }
        // Idempotence : une commande ne devrait transiter qu'une seule fois
        // vers DELIVERED (voir OrderServiceImpl — statut définitif), mais on
        // se protège malgré tout d'un double envoi du formulaire.
        if (feedbackRepository.existsByOrderId(order.getId())) {
            log.debug("Formulaire d'avis déjà existant pour la commande {} — aucune action.", order.getId());
            return;
        }

        Feedback feedback = Feedback.builder().order(order).requestedAt(Instant.now(clock)).build();
        feedbackRepository.save(feedback);

        TransactionUtils.runAfterCommit(() -> notificationService.notifyFeedbackRequested(order));
        log.info("Formulaire d'avis généré et envoyé au client pour la commande {}", order.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public FeedbackDto getFeedbackForOrder(Long orderId, Long callerId, boolean isStaff) {
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new ResourceNotFoundException("Commande introuvable: " + orderId));

        ensureOwnerOrStaff(order, callerId, isStaff);

        return feedbackRepository.findByOrderId(orderId).map(FeedbackMapper::toDto).orElse(null);
    }

    @Override
    public FeedbackDto submitFeedback(Long orderId, Long customerId, FeedbackSubmitRequest request) {
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new ResourceNotFoundException("Commande introuvable: " + orderId));

        if (order.getClientUser() == null || !order.getClientUser().getId().equals(customerId)) {
            throw new AccessDeniedException("Vous ne pouvez donner votre avis que sur vos propres commandes.");
        }
        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new IllegalStateException("Vous ne pouvez donner votre avis qu'une fois la commande #" + orderId + " livrée.");
        }

        Feedback feedback = feedbackRepository.findByOrderId(orderId).orElseThrow(() -> new ResourceNotFoundException("Aucun formulaire d'avis n'a été généré pour la commande #" + orderId + "."));

        if (feedback.getSubmittedAt() != null) {
            throw new IllegalStateException("Vous avez déjà donné votre avis pour la commande #" + orderId + ".");
        }

        feedback.setRating(request.getRating());
        feedback.setComment(request.getComment());
        feedback.setSubmittedAt(Instant.now(clock));
        // sentiment / aiSummary restent null pour l'instant : l'analyse IA
        // (Gemini) est déclenchée de façon asynchrone ci-dessous et les
        // renseignera un peu plus tard (voir FeedbackAnalysisCoordinator).
        // Le client les récupérera en rechargeant le formulaire d'avis.

        Feedback saved = feedbackRepository.save(feedback);
        Long savedFeedbackId = saved.getId();

        // Déclenchement du pipeline asynchrone (analyse Gemini -> persistance
        // du sentiment -> notification manager/admin), entièrement sur le
        // pool notificationExecutor (CompletableFuture), sans jamais bloquer
        // le thread de la requête HTTP courante.
        //
        // IMPORTANT : on ne lance ce pipeline qu'APRÈS le commit de la
        // transaction en cours (TransactionSynchronizationManager.afterCommit).
        // Sans cela, le thread asynchrone pourrait tenter de relire ce
        // Feedback (par son id) avant que la ligne ne soit réellement
        // visible en base — l'appel Gemini est suffisamment rapide pour que
        // la race soit improbable mais pas impossible, et une notification
        // manquée ou une exception "Feedback introuvable" serait difficile à
        // diagnostiquer en production.
        TransactionUtils.runAfterCommit(() -> feedbackAnalysisCoordinator.processFeedbackAsync(savedFeedbackId, request.getRating(), request.getComment()));

        log.info("Avis client enregistré pour la commande {} (note={}) — analyse IA et notifications en cours (asynchrone)", orderId, saved.getRating());

        return FeedbackMapper.toDto(saved);
    }

    private void ensureOwnerOrStaff(Order order, Long callerId, boolean isStaff) {
        if (isStaff) {
            return;
        }
        if (order.getClientUser() == null || !order.getClientUser().getId().equals(callerId)) {
            throw new AccessDeniedException("Accès refusé : vous ne pouvez consulter que vos propres données");
        }
    }
}
