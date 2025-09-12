package com.tourya.api.models;

import com.tourya.api.common.BaseEntity;
import com.tourya.api.constans.enums.ShoppingCartStatusEnum;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad que representa un item en el carrito de compras.
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
@Table(name = "shopping_cart_item")
public class ShoppingCartItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shopping_cart_id", nullable = false)
    private ShoppingCart shoppingCart;

    @Column(name = "product_id", nullable = false)
    private Integer productId;

    @Column(name = "product_type", nullable = false)
    private String productType; // TOUR, SERVICE, etc.

    // service_id no existe en la base de datos, se maneja a través de product_id y product_type

    @Column(name = "schedule_date")
    private LocalDate scheduleDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tour_schedule_id")
    private TourSchedule tourSchedule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "slot_id")
    private TourScheduleConfigSlot slot;

    @Column(name = "total_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ShoppingCartStatusEnum status;

    @OneToMany(mappedBy = "shoppingCartItem", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ShoppingCartItemDetail> details = new ArrayList<>();

    /**
     * Calcula el precio total sumando todos los detalles
     */
    public BigDecimal calculateTotalPrice() {
        if (details == null || details.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return details.stream()
                .map(ShoppingCartItemDetail::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
