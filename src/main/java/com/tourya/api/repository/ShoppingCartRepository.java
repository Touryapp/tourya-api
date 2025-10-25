package com.tourya.api.repository;

import com.tourya.api.constans.enums.ShoppingCartStatusEnum;
import com.tourya.api.models.ShoppingCart;
import com.tourya.api.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio para la entidad ShoppingCart.
 * 
 * @author Tourya API Team
 * @version 1.0
 */
@Repository
public interface ShoppingCartRepository extends JpaRepository<ShoppingCart, Long> {

    /**
     * Busca carritos activos del usuario.
     * 
     * @param user usuario
     * @param status estado del carrito (ACTIVE)
     * @return Lista de carritos activos del usuario
     */
    List<ShoppingCart> findByUserAndStatus(User user, ShoppingCartStatusEnum status);

    @Query(value = "SELECT public.sp_clear_shopping_cart(:cartId)", nativeQuery = true)
    Integer clearCart(@Param("cartId") Long cartId);
}
