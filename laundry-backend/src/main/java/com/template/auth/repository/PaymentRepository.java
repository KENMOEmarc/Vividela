package com.template.auth.repository;

import com.template.auth.model.entity.Payment;
import com.template.auth.model.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByOrderId(Long orderId);

    List<Payment> findByPayerPhone(String phone);

    List<Payment> findByTransactionReference(String reference);

    /**
     * AJOUT : recherche par référence de transaction, utilisée pour le
     * garde-fou anti-doublon (voir revue de code, règle manquante n°6 —
     * "double clic → double paiement enregistré deux fois").
     */
    Optional<Payment> findFirstByTransactionReference(String reference);

    /**
     * AJOUT : somme des paiements "engagés" (en attente ou confirmés, donc
     * hors FAILED/REFUNDED) sur une commande — utilisée pour le contrôle de
     * surpaiement (voir revue de code, règle manquante n°2).
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.order.id = :orderId AND p.status IN :statuses")
    BigDecimal sumAmountByOrderIdAndStatusIn(@Param("orderId") Long orderId, @Param("statuses") Collection<PaymentStatus> statuses);
}
