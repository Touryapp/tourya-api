package com.tourya.api.models.responses;

import com.tourya.api.constans.enums.ShoppingCartStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * DTO de respuesta para un item del carrito de compras.
 * 
 * @author Tourya API Team
 * @version 1.0
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ShoppingCartItemResponse {

    private Long id;
    private Integer productId;
    private String productType;
    private String productName;
    private LocalDate scheduleDate;
    private Integer tourScheduleId;
    private String tourName;
    private Integer slotId;
    private LocalTime slotStartTime;
    private LocalTime slotEndTime;
    private String city;
    private String department;
    private TourGalleryResponse profilePicture;
    private BigDecimal totalPrice;
    private BigDecimal providerTotalPrice;
    private ShoppingCartStatusEnum status;
    private List<ShoppingCartItemDetailResponse> details;
    /**
     * Fix #189 (TC-006): expone el priceType del tour ("grupo" | "individual")
     * para que el frontend pueda renderizar correctamente el resumen del carrito
     * ("1 Grupo(s) (2 personas)" vs "2 personas"). Sin este campo el resumen caía
     * al render de individual aunque el tour fuera de grupo.
     */
    private String priceType;
    /**
     * Fix #189 (TC-006): capacidad máxima por grupo. El frontend ya lo consume
     * en {@code cart.service.ts} para validaciones de disponibilidad.
     */
    private Integer maxPeople;

    /**
     * Sprint 2 mobile — deuda 2B: expone el {@code address_type} del meeting point
     * del tour (FIXED_LOCATION / HOTEL_PICKUP) para que el mobile pueda distinguir
     * casos Hotel Pickup en el checkout sin la deteccion heuristica de TC-017
     * (empty-check de city+address). Poblado desde el primer {@code tour_address}
     * asociado al tour del cart item; null para items de tipo SERVICE.
     */
    private String addressType;
}

