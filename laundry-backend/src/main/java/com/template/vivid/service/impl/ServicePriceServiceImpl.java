package com.template.vivid.service.impl;

import com.template.vivid.model.payloads.requests.ServicePriceCreateRequest;
import com.template.vivid.model.dto.ServicePriceDto;
import com.template.vivid.model.payloads.requests.ServicePriceUpdateRequest;
import com.template.vivid.model.entity.ServicePrice;
import com.template.vivid.model.enums.ClothingType;
import com.template.vivid.model.enums.ServiceType;
import com.template.vivid.model.mapper.ServicePriceMapper;
import com.template.vivid.repository.ServicePriceRepository;
import com.template.vivid.service.ServicePriceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import com.template.vivid.exception.DuplicateResourceException;
import com.template.vivid.exception.InvalidRequestException;
import com.template.vivid.exception.InvalidStateTransitionException;
import com.template.vivid.exception.ResourceNotFoundException;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ServicePriceServiceImpl implements ServicePriceService {

    private final ServicePriceRepository servicePriceRepository;
    private final com.template.vivid.repository.ArticleServiceLineRepository articleServiceRepository;

    @Override
    public ServicePriceDto create(ServicePriceCreateRequest request) {
        ClothingType clothingType = parseClothingType(request.getClothingType());
        ServiceType serviceType = parseServiceType(request.getService());

        if (servicePriceRepository.findByClothingTypeAndService(clothingType, serviceType).isPresent()) {
            throw new DuplicateResourceException(
                    "Un tarif existe déjà pour " + clothingType + " / " + serviceType);
        }

        ServicePrice servicePrice = new ServicePrice();
        servicePrice.setClothingType(clothingType);
        servicePrice.setService(serviceType);
        servicePrice.setPrice(request.getPrice());
        servicePrice.setActive(request.getActive() == null || request.getActive());
        servicePrice.setCreatedAt(Instant.now());

        ServicePrice saved = servicePriceRepository.save(servicePrice);
        log.info("Tarif de service créé. ID: {}, {} / {} = {}", saved.getId(), clothingType, serviceType, saved.getPrice());
        return ServicePriceMapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ServicePriceDto getById(Long id) {
        return ServicePriceMapper.toDto(findEntity(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ServicePriceDto> getAll() {
        return servicePriceRepository.findAll().stream()
                .map(ServicePriceMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public ServicePriceDto update(Long id, ServicePriceUpdateRequest request) {
        ServicePrice servicePrice = findEntity(id);

        // AJOUT : le prix mis à jour doit être strictement positif. Voir
        // revue de code, règle manquante n°2 (section Tarifs des services).
        if (request.getPrice() == null || request.getPrice().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new InvalidRequestException("Le tarif doit être strictement positif.");
        }

        servicePrice.setPrice(request.getPrice());
        if (request.getActive() != null) {
            servicePrice.setActive(request.getActive());
        }

        ServicePrice updated = servicePriceRepository.save(servicePrice);
        log.info("Tarif de service mis à jour. ID: {}", updated.getId());
        return ServicePriceMapper.toDto(updated);
    }

    @Override
    public void delete(Long id) {
        ServicePrice servicePrice = findEntity(id);

        // AJOUT : avertit explicitement si ce tarif a déjà été appliqué à des
        // articles existants avant de le supprimer — le prix est recopié
        // dans ArticleService.appliedPrice au moment de l'ajout de l'article
        // donc rien ne casse techniquement, mais on documente/empêche la
        // suppression silencieuse d'un tarif encore en usage historique.
        // Voir revue de code, règle manquante n°1 (section Tarifs des services).
        if (articleServiceRepository.existsByClothingTypeAndService(servicePrice.getClothingType(), servicePrice.getService())) {
            throw new InvalidStateTransitionException(
                    "Impossible de supprimer ce tarif (" + servicePrice.getClothingType() + " / " + servicePrice.getService()
                            + ") : il a déjà été appliqué à au moins un article existant. Désactivez-le "
                            + "(champ 'active') plutôt que de le supprimer, afin de conserver l'historique des prix appliqués.");
        }

        servicePriceRepository.delete(servicePrice);
        log.info("Tarif de service supprimé. ID: {}", id);
    }

    private ServicePrice findEntity(Long id) {
        return servicePriceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tarif de service introuvable avec l'ID: " + id));
    }

    private ClothingType parseClothingType(String value) {
        try {
            return ClothingType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new InvalidRequestException("Type de vêtement invalide: " + value);
        }
    }

    private ServiceType parseServiceType(String value) {
        try {
            return ServiceType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new InvalidRequestException("Type de service invalide: " + value);
        }
    }
}
