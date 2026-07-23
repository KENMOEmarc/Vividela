package com.template.auth.repository;

import com.template.auth.model.entity.Order;
import com.template.auth.model.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByClientUserId(Long clientUserId);

    List<Order> findByStatus(OrderStatus status);

    // Filtrage par plusieurs statuts à la fois (checkboxs de filtre côté frontend)
    List<Order> findByStatusIn(List<OrderStatus> statuses);

    List<Order> findByDepositDateBetween(LocalDate start, LocalDate end);

    List<Order> findByExpectedDeliveryDateBeforeAndStatus(LocalDate date, OrderStatus status);

}
