package com.template.vivid.repository;

import com.template.vivid.model.entity.ServicePrice;
import com.template.vivid.model.enums.ClothingType;
import com.template.vivid.model.enums.ServiceType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ServicePriceRepository extends JpaRepository<ServicePrice, Long> {

    Optional<ServicePrice> findByClothingTypeAndServiceAndActiveTrue(ClothingType clothingType, ServiceType service);

    Optional<ServicePrice> findByClothingTypeAndService(ClothingType clothingType, ServiceType service);
}
