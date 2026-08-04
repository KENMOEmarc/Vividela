package com.template.vivid.controller;

import com.template.vivid.model.payloads.responses.ApiResponse;
import com.template.vivid.model.dto.NotificationDto;
import com.template.vivid.model.entity.Notification;
import com.template.vivid.model.entity.Order;
import com.template.vivid.model.mapper.NotificationMapper;
import com.template.vivid.repository.ArticleRepository;
import com.template.vivid.repository.NotificationRepository;
import com.template.vivid.repository.OrderRepository;
import com.template.vivid.service.UserService;
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
                    if (dto.getOrderId() != null) {
                        Order order = orderById.get(dto.getOrderId());
                        if (order != null) {
                            dto.setOrderDepositDate(order.getDepositDate());
                        }
                        dto.setOrderArticleCount(articleCountByOrder.getOrDefault(dto.getOrderId(), 0));
                    }
                    return dto;
                })
                .toList();

        return ResponseEntity.ok(
                ApiResponse.success("Notifications récupérées", notifications));
    }

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

    private Long resolveUserId(UserDetails userDetails) {
        if (userDetails == null) {
            return null;
        }
        return userService.findByEmail(userDetails.getUsername()).getId();
    }
}
