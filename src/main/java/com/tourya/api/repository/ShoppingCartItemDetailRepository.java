package com.tourya.api.repository;

import com.tourya.api.models.ShoppingCartItemDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ShoppingCartItemDetailRepository extends JpaRepository<ShoppingCartItemDetail, Long> {
}

