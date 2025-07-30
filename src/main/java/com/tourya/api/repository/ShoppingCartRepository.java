package com.tourya.api.repository;

import com.tourya.api.constans.enums.ShoppingCartStatusEnum;
import com.tourya.api.models.ShoppingCart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ShoppingCartRepository extends JpaRepository<ShoppingCart, Long> {

    Optional<ShoppingCart> findByUserIdAndStatus(Integer userId, ShoppingCartStatusEnum status);
}
