package com.tourya.api.repository;

import com.tourya.api.models.ShoppingCart;
import com.tourya.api.models.ShoppingCartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    /**
     * Elimina físicamente los detalles de items ACTIVE de un carrito.
     * 
     * @param cartId ID del carrito
     * @return número de detalles eliminados
     */
    @Modifying
    @Query(value = """
        DELETE FROM shopping_cart_item_detail 
        WHERE shopping_cart_item_id IN (
            SELECT id FROM shopping_cart_item 
            WHERE shopping_cart_id = :cartId AND status = 'ACTIVE'
        )
        """, nativeQuery = true)
    int deleteActiveItemDetailsByCartId(@Param("cartId") Long cartId);

    /**
     * Elimina físicamente todos los items ACTIVE de un carrito.
     * 
     * @param cartId ID del carrito
     * @return número de items eliminados
     */
    @Modifying
    @Query(value = "DELETE FROM shopping_cart_item WHERE shopping_cart_id = :cartId AND status = 'ACTIVE'", nativeQuery = true)
    int deleteActiveItemsByCartId(@Param("cartId") Long cartId);
}
