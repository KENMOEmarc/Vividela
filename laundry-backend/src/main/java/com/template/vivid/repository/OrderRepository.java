package com.template.vivid.repository;

import com.template.vivid.model.entity.Order;
import com.template.vivid.model.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByClientUserId(Long clientUserId);

    // Filtrage par plusieurs statuts à la fois (checkboxs de filtre côté frontend)
    List<Order> findByStatusIn(List<OrderStatus> statuses);
}

