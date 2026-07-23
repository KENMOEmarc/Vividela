package com.template.auth.service;

import com.template.auth.model.dto.ServicePriceCreateRequest;
import com.template.auth.model.dto.ServicePriceDto;
import com.template.auth.model.dto.ServicePriceUpdateRequest;

import java.util.List;

/**
 * Service de gestion des tarifs de service (prix par type de vêtement / type
 * de service, utilisés pour calculer automatiquement le montant des commandes).
 *
 * Règles métier :
 * - ADMIN et MANAGER peuvent créer et modifier les tarifs.
 * - Seul ADMIN peut supprimer un tarif (comme toute suppression de l'application).
 * - Tout le personnel (ADMIN, MANAGER, EMPLOYEE) peut consulter les tarifs.
 */
public interface ServicePriceService {

    ServicePriceDto create(ServicePriceCreateRequest request);

    ServicePriceDto getById(Long id);

    List<ServicePriceDto> getAll();

    ServicePriceDto update(Long id, ServicePriceUpdateRequest request);

    void delete(Long id);
}
