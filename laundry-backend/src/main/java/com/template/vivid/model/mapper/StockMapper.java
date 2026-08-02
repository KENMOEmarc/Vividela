package com.template.vivid.model.mapper;

import com.template.vivid.model.dto.ProductRegistrationDto;
import com.template.vivid.model.dto.StockBatchDto;
import com.template.vivid.model.dto.StockDto;
import com.template.vivid.model.dto.StockMovementDto;
import com.template.vivid.model.entity.Product;
import com.template.vivid.model.entity.ProductRegistration;
import com.template.vivid.model.entity.Stock;
import com.template.vivid.model.entity.StockMovement;
import com.template.vivid.model.entity.User;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper centralisant la conversion des entités du module Stock
 * (Stock = un LOT de stock, StockMovement, ProductRegistration) vers leurs
 * DTO respectifs.
 *
 * Un produit pouvant désormais posséder plusieurs lots de stock (Stock),
 * ce mapper distingue :
 *  - toBatchDto(Stock)                    → un lot précis
 *  - toAggregatedDto(Product, List<Stock>) → la vue agrégée par produit
 */
public final class StockMapper {

    /** Nombre de jours en dessous duquel un lot est considéré "bientôt périmé". */
    private static final long EXPIRING_SOON_DAYS = 7;

    private StockMapper() {
        // Classe utilitaire : pas d'instanciation
    }

    /** Convertit un lot de stock (Stock) en DTO détaillé. */
    public static StockBatchDto toBatchDto(Stock stock) {
        if (stock == null) {
            return null;
        }
        Product product = stock.getProduct();
        LocalDate today = LocalDate.now();
        LocalDate expiration = stock.getExpirationDate();
        boolean expired = expiration != null && expiration.isBefore(today);
        boolean expiringSoon = !expired && expiration != null
                && !expiration.isAfter(today.plusDays(EXPIRING_SOON_DAYS));

        return StockBatchDto.builder()
                .id(stock.getId())
                .productId(product != null ? product.getId() : null)
                .productName(product != null ? product.getName() : null)
                .measurementUnit(product != null ? product.getMeasurementUnit().name() : null)
                .currentQuantity(stock.getCurrentQuantity())
                .unitPrice(stock.getUnitPrice())
                .entryDate(stock.getEntryDate())
                .expirationDate(expiration)
                .expired(expired)
                .expiringSoon(expiringSoon)
                .createdAt(stock.getCreatedAt())
                .build();
    }

    public static List<StockBatchDto> toBatchDtoList(List<Stock> stocks) {
        return stocks.stream().map(StockMapper::toBatchDto).collect(Collectors.toList());
    }

    /**
     * Construit la vue agrégée (totaux) du stock d'un produit à partir de
     * l'ensemble de ses lots. {@code includeBatches} contrôle si le détail
     * des lots doit être inclus dans le DTO retourné.
     */
    public static StockDto toAggregatedDto(Product product, List<Stock> batches, boolean includeBatches) {
        if (product == null) {
            return null;
        }
        BigDecimal total = batches.stream()
                .map(Stock::getCurrentQuantity)
                .filter(q -> q != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        LocalDate nearestExpiration = batches.stream()
                .filter(s -> s.getCurrentQuantity() != null && s.getCurrentQuantity().compareTo(BigDecimal.ZERO) > 0)
                .map(Stock::getExpirationDate)
                .filter(d -> d != null)
                .min(Comparator.naturalOrder())
                .orElse(null);

        boolean belowThreshold = total.compareTo(product.getThresholdValue()) <= 0;

        StockDto.StockDtoBuilder builder = StockDto.builder()
                .productId(product.getId())
                .productName(product.getName())
                .measurementUnit(product.getMeasurementUnit().name())
                .currentQuantity(total)
                .thresholdValue(product.getThresholdValue())
                .belowThreshold(belowThreshold)
                .batchCount(batches.size())
                .nearestExpirationDate(nearestExpiration);

        if (includeBatches) {
            builder.batches(batches.stream()
                    .sorted(Comparator.comparing(Stock::getId))
                    .map(StockMapper::toBatchDto)
                    .collect(Collectors.toList()));
        }

        return builder.build();
    }

    public static StockMovementDto toDto(StockMovement movement) {
        if (movement == null) {
            return null;
        }
        Product product = movement.getStock() != null ? movement.getStock().getProduct() : null;
        User user = movement.getUser();

        return StockMovementDto.builder()
                .id(movement.getId())
                .stockId(movement.getStock() != null ? movement.getStock().getId() : null)
                .productId(product != null ? product.getId() : null)
                .productName(product != null ? product.getName() : null)
                .quantity(movement.getQuantity())
                .movementType(movement.getMovementType() != null ? movement.getMovementType().name() : null)
                .notes(movement.getNotes())
                .movementDate(movement.getMovementDate())
                .userId(user != null ? user.getId() : null)
                .userName(user != null ? user.getFirstName() + " " + user.getLastName() : null)
                .build();
    }

    public static ProductRegistrationDto toDto(ProductRegistration registration) {
        if (registration == null) {
            return null;
        }
        Product product = registration.getProduct();
        User employee = registration.getEmployeeUser();

        return ProductRegistrationDto.builder()
                .id(registration.getId())
                .productId(product != null ? product.getId() : null)
                .productName(product != null ? product.getName() : null)
                .employeeUserId(employee != null ? employee.getId() : null)
                .employeeName(employee != null ? employee.getFirstName() + " " + employee.getLastName() : null)
                .quantity(registration.getQuantity())
                .registrationType(registration.getRegistrationType() != null ? registration.getRegistrationType().name() : null)
                .notes(registration.getNotes())
                .registeredAt(registration.getRegisteredAt())
                .build();
    }
}
