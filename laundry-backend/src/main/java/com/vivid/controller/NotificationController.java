package com.vivid.controller;

import com.vivid.model.payloads.responses.ApiResponse;
import com.vivid.model.dto.NotificationDto;
import com.vivid.model.entity.Notification;
import com.vivid.model.entity.Order;
import com.vivid.model.mapper.NotificationMapper;
import com.vivid.repository.ArticleRepository;
import com.vivid.repository.NotificationRepository;
import com.vivid.repository.OrderRepository;
import com.vivid.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationRepository notificationRepository;
    private final OrderRepository orderRepository;
    private final ArticleRepository articleRepository;
    private final UserService userService;

    /**
     * GET /notifications — récupère les notifications de l'utilisateur connecté
     * (les plus récentes en premier). C'est ce flux qui alimente la page
     * "Notifications" du site client (canal IN_APP).
     * <p>
     * BUGFIX : on ne renvoie plus l'entité {@code Notification} brute mais un
     * {@link NotificationDto}. L'entité embarquait, via ses associations
     * paresseuses {@code user}/{@code order} (chargées automatiquement tant
     * que spring.jpa.open-in-view est actif), le mot de passe haché du
     * client concerné ainsi que celui du client/créateur de la commande liée
     * — exposés en clair dans le JSON de réponse à chaque appel.
     * <p>
     * Chaque notification est enrichie avec quelques détails de la commande
     * liée (date de dépôt, nombre d'articles) afin que le frontend puisse
     * afficher un lien direct vers la commande sans appel supplémentaire.
     * Ces détails sont chargés en 2 requêtes groupées (une par commande
     * concernée, pas une par notification) pour éviter tout N+1.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE', 'CUSTOMER')")
    public ResponseEntity<ApiResponse<List<NotificationDto>>> getUserNotifications(
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = resolveUserId(userDetails);
        List<Notification> entities = notificationRepository.findByUserIdOrderBySentAtDesc(userId);

        List<Long> orderIds = entities.stream()
                .map(Notification::getOrder)
                .filter(Objects::nonNull)
                .map(Order::getId)
                .distinct()
                .toList();

        Map<Long, Order> orderById = orderIds.isEmpty()
                ? Map.of()
                : orderRepository.findAllById(orderIds).stream()
                .collect(Collectors.toMap(Order::getId, o -> o));

        Map<Long, Integer> articleCountByOrder = orderIds.isEmpty()
                ? Map.of()
                : articleRepository.findByOrderIdIn(orderIds).stream()
                .collect(Collectors.groupingBy(
                        a -> a.getOrder().getId(),
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)));

        List<NotificationDto> notifications = entities.stream()
                .map(entity -> {
                    NotificationDto dto = NotificationMapper.toDto(entity);
                    if (dto.orderId() != null) {
                        Order order = orderById.get(dto.orderId());
                        if (order != null) {
                            dto.toBuilder().orderDepositDate(order.getDepositDate());
                        }
                        dto.toBuilder().orderArticleCount(articleCountByOrder.getOrDefault(dto.orderId(), 0));
                    }
                    return dto;
                })
                .toList();

        return ResponseEntity.ok(
                ApiResponse.success("Notifications récupérées", notifications));
    }

    /**
     * PATCH /notifications/{notificationId}/read — marquer une notification comme lue
     * <p>
     * BUGFIX : aucune vérification de propriété n'était faite auparavant —
     * n'importe quel utilisateur authentifié pouvait marquer comme lue (et
     * donc altérer) la notification de n'importe quel autre utilisateur en
     * devinant/énumérant son ID (IDOR). On vérifie désormais que la
     * notification appartient bien à l'appelant, sauf pour ADMIN/EMPLOYEE
     * qui gèrent aussi les alertes internes (stock bas, etc.).
     */
    @PatchMapping("/{notificationId}/read")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE', 'CUSTOMER')")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @PathVariable Long notificationId,
            @AuthenticationPrincipal UserDetails userDetails) {

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification introuvable: " + notificationId));

        Long callerId = resolveUserId(userDetails);
        Long ownerId = notification.getUser() != null ? notification.getUser().getId() : null;
        boolean isStaff = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_EMPLOYEE"));

        if (!isStaff && (ownerId == null || !ownerId.equals(callerId))) {
            log.warn("Tentative de marquage d'une notification d'autrui : user={} notification={}", callerId, notificationId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Vous ne pouvez pas modifier cette notification."));
        }

        notification.setIsRead(true);
        notificationRepository.save(notification);

        log.info("Notification {} marquée comme lue", notificationId);
        return ResponseEntity.ok(ApiResponse.success("Notification marquée comme lue", null));
    }

    /**
     * Résout l'ID de l'utilisateur connecté depuis le contexte de sécurité.
     */
    private Long resolveUserId(UserDetails userDetails) {
        if (userDetails == null) {
            return null;
        }
        return userService.findByEmail(userDetails.getUsername()).id();
    }
}
