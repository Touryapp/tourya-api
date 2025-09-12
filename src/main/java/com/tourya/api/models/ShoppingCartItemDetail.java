package com.tourya.api.models;

import com.tourya.api.common.BaseEntity;
import com.tourya.api.constans.enums.AgePriceType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * Entidad que representa el detalle de un item en el carrito de compras por tipo de edad.
 * 
 * @author Tourya API Team
 * @version 1.0
 */
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "shopping_cart_item_detail")
public class ShoppingCartItemDetail extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shopping_cart_item_id", nullable = false)
    private ShoppingCartItem shoppingCartItem;

    @Enumerated(EnumType.STRING)
    @Column(name = "age_type", nullable = false)
    private AgePriceType ageType;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "total_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPrice;
}

