package com.tourya.api.models.mapper;

import com.tourya.api.models.ShoppingCartItem;
import com.tourya.api.models.ShoppingCartItemDetail;
import com.tourya.api.models.responses.ReservationPriceBreakdownResponse;
import com.tourya.api.models.responses.ReservationResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ReservationPriceBreakdownMapper {

    public void applyToReservationResponse(ReservationResponse response, ShoppingCartItem item) {
        if (response == null || item == null || item.getDetails() == null || item.getDetails().isEmpty()) {
            return;
        }
        List<ReservationPriceBreakdownResponse> breakdown = fromCartDetails(item.getDetails());
        response.setPriceBreakdown(breakdown);
        response.setProviderTotalAmount(sumProviderTotal(breakdown));
    }

    public List<ReservationPriceBreakdownResponse> fromCartDetails(Collection<ShoppingCartItemDetail> details) {
        if (details == null || details.isEmpty()) {
            return Collections.emptyList();
        }
        return details.stream()
                .filter(d -> d.getQuantity() != null && d.getQuantity() > 0)
                .map(this::toBreakdown)
                .collect(Collectors.toList());
    }

    public BigDecimal sumProviderTotal(List<ReservationPriceBreakdownResponse> breakdown) {
        if (breakdown == null || breakdown.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return breakdown.stream()
                .map(ReservationPriceBreakdownResponse::getProviderSubtotal)
                .filter(p -> p != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal sumSaleTotal(List<ReservationPriceBreakdownResponse> breakdown) {
        if (breakdown == null || breakdown.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return breakdown.stream()
                .map(ReservationPriceBreakdownResponse::getSubtotal)
                .filter(p -> p != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private ReservationPriceBreakdownResponse toBreakdown(ShoppingCartItemDetail detail) {
        BigDecimal providerSubtotal = null;
        if (detail.getProviderUnitPrice() != null && detail.getQuantity() != null) {
            providerSubtotal = detail.getProviderUnitPrice()
                    .multiply(BigDecimal.valueOf(detail.getQuantity()));
        }
        return ReservationPriceBreakdownResponse.builder()
                .ageType(detail.getAgeType())
                .quantity(detail.getQuantity())
                .unitPrice(detail.getUnitPrice())
                .providerUnitPrice(detail.getProviderUnitPrice())
                .subtotal(detail.getTotalPrice())
                .providerSubtotal(providerSubtotal)
                .build();
    }
}
