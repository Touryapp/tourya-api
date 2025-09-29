package com.tourya.api.repository;

import com.tourya.api.models.ShoppingCart;
import com.tourya.api.models.ShoppingCartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio para la entidad ShoppingCartItem.
 * 
 * @author Tourya API Team
 * @version 1.0
 */
@Repository
public interface ShoppingCartItemRepository extends JpaRepository<ShoppingCartItem, Long> {

    /**
     * Busca items por carrito de compras.
     * 
     * @param shoppingCart carrito de compras
     * @return Lista de items del carrito
     */
    List<ShoppingCartItem> findByShoppingCart(ShoppingCart shoppingCart);
}
