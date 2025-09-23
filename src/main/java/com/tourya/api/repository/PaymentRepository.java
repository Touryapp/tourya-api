package com.tourya.api.repository;

import com.tourya.api.models.Payment;
import com.tourya.api.models.ShoppingCartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para la entidad Payment.
 * 
 * @author Tourya API Team
 * @version 1.0
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /**
     * Busca pagos por ID de transacción
     */
    Optional<Payment> findByTransactionId(String transactionId);

    /**
     * Busca pagos por item del carrito de compras
     */
    List<Payment> findByShoppingCartItem(ShoppingCartItem shoppingCartItem);

    /**
     * Busca pagos por ID del item del carrito de compras
     */
    @Query("SELECT p FROM Payment p WHERE p.shoppingCartItem.id = :shoppingCartItemId")
    List<Payment> findByShoppingCartItemId(@Param("shoppingCartItemId") Long shoppingCartItemId);

    /**
     * Busca pagos por estado
     */
    List<Payment> findByStatus(String status);

    /**
     * Busca pagos por método de pago
     */
    List<Payment> findByPaymentMethodType(String paymentMethodType);

    /**
     * Verifica si existe un pago exitoso para un item del carrito
     */
    @Query("SELECT COUNT(p) > 0 FROM Payment p WHERE p.shoppingCartItem.id = :shoppingCartItemId AND p.status = 'APPROVED'")
    boolean existsApprovedPaymentByShoppingCartItemId(@Param("shoppingCartItemId") Long shoppingCartItemId);

    /**
     * Busca pagos por ID del pagador
     */
    List<Payment> findByPayerId(Integer payerId);

    /**
     * Busca pagos por email del pagador
     */
    List<Payment> findByPayerEmail(String payerEmail);

    /**
     * Busca pagos por nombre del pagador
     */
    @Query("SELECT p FROM Payment p WHERE p.payerName LIKE %:payerName%")
    List<Payment> findByPayerNameContaining(@Param("payerName") String payerName);
}
