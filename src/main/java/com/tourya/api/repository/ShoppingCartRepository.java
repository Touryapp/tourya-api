package com.tourya.api.repository;

import com.tourya.api.constans.enums.ShoppingCartStatusEnum;
import com.tourya.api.models.ShoppingCart;
import com.tourya.api.models.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    /**
     * Busca carritos del usuario con paginación.
     * 
     * @param userId ID del usuario
     * @param pageable parámetros de paginación
     * @return Page con los carritos del usuario
     */
    Page<ShoppingCart> findByUser_Id(Integer userId, Pageable pageable);

    /**
     * Busca carritos por status con paginación.
     * 
     * @param status estado del carrito
     * @param pageable parámetros de paginación
     * @return Page con los carritos del status especificado
     */
    Page<ShoppingCart> findByStatus(ShoppingCartStatusEnum status, Pageable pageable);

    /**
     * Busca carritos del usuario y status con paginación.
     * 
     * @param userId ID del usuario
     * @param status estado del carrito
     * @param pageable parámetros de paginación
     * @return Page con los carritos del usuario y status especificado
     */
    Page<ShoppingCart> findByUser_IdAndStatus(Integer userId, ShoppingCartStatusEnum status, Pageable pageable);

    @Query(value = "SELECT public.sp_clear_shopping_cart(:cartId)", nativeQuery = true)
    Integer clearCart(@Param("cartId") Long cartId);
}
