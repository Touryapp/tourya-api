package com.tourya.api.repository;

import com.tourya.api.constans.enums.ShoppingCartStatusEnum;
import com.tourya.api.models.ShoppingCart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para la entidad ShoppingCart.
 * 
 * @author Tourya API Team
 * @version 1.0
 */
@Repository
public interface ShoppingCartRepository extends JpaRepository<ShoppingCart, Long> {

    /**
     * Busca un carrito por usuario y estado.
     * 
     * @param userId ID del usuario
     * @param status estado del carrito
     * @return Optional con el carrito encontrado
     */
    Optional<ShoppingCart> findByUserIdAndStatus(Integer userId, ShoppingCartStatusEnum status);

    /**
     * Busca carritos por usuario ordenados por fecha de creación descendente.
     * 
     * @param userId ID del usuario
     * @return Lista de carritos del usuario
     */
    List<ShoppingCart> findByUserIdOrderByCreatedDateDesc(Integer userId);
}
