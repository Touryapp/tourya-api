package com.tourya.api.services;

import com.tourya.api.constans.enums.ReviewReasonTypeEnum;
import com.tourya.api.models.ReviewReasonCatalog;
import com.tourya.api.models.TranslatedField;
import com.tourya.api.models.responses.ReviewReasonCatalogResponse;
import com.tourya.api.models.responses.ReviewReasonListResponse;
import com.tourya.api.repository.ReviewReasonCatalogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewReasonCatalogService {

    private final ReviewReasonCatalogRepository reviewReasonCatalogRepository;

    public ReviewReasonCatalogResponse getCatalog() {
        return ReviewReasonCatalogResponse.builder()
                .positive(toItems(ReviewReasonTypeEnum.POSITIVE))
                .negative(toItems(ReviewReasonTypeEnum.NEGATIVE))
                .build();
    }

    public ReviewReasonListResponse getReasonsForRating(BigDecimal rating) {
        ReviewReasonTypeEnum type = inferType(rating);
        return ReviewReasonListResponse.builder()
                .type(type)
                .reasons(toItems(type))
                .build();
    }

    private List<ReviewReasonCatalogResponse.Item> toItems(ReviewReasonTypeEnum type) {
        List<ReviewReasonCatalog> rows = reviewReasonCatalogRepository.findByReasonTypeOrderByReasonIdAsc(type);
        if (rows.isEmpty()) {
            return fallbackItems(type);
        }
        return rows.stream()
                .map(row -> ReviewReasonCatalogResponse.Item.builder()
                        .id(row.getReasonId())
                        .label(row.getLabel())
                        .type(row.getReasonType())
                        .build())
                .toList();
    }

    private ReviewReasonTypeEnum inferType(BigDecimal rating) {
        if (rating == null) return ReviewReasonTypeEnum.NEGATIVE;
        return rating.compareTo(new BigDecimal("4.0")) >= 0
                ? ReviewReasonTypeEnum.POSITIVE
                : ReviewReasonTypeEnum.NEGATIVE;
    }

    /** Fallback si la migración 063 aún no se ejecutó en la BD. */
    private List<ReviewReasonCatalogResponse.Item> fallbackItems(ReviewReasonTypeEnum type) {
        if (type == ReviewReasonTypeEnum.POSITIVE) {
            return List.of(
                    reason(1, type, "Servicio excepcional del guía", "Exceptional service from the guide", "Serviço excepcional do guia"),
                    reason(2, type, "Puntualidad del proveedor", "Provider's punctuality", "Pontualidade do prestador de serviços"),
                    reason(3, type, "Buena organización del tour", "Good tour organization", "Boa organização do passeio"),
                    reason(4, type, "Excelente relación calidad-precio", "Excellent value for money", "Excelente custo-benefício"),
                    reason(5, type, "Comodidad del transporte", "Comfort of transportation", "Conforto do transporte"),
                    reason(6, type, "Buena atención al cliente", "Good customer service", "Bom atendimento ao cliente"),
                    reason(7, type, "Recomendable para otros viajeros", "Would recommend to other travelers", "Recomendável para outros viajantes")
            );
        }
        return List.of(
                reason(1, type, "Retraso o impuntualidad", "Delays or lack of punctuality", "Atraso ou falta de pontualidade"),
                reason(2, type, "Guía poco amable o desinformado", "Unfriendly or uninformed guide", "Guia pouco simpático ou mal informado"),
                reason(3, type, "Mala comunicación con el operador turístico", "Poor communication with the tour operator", "Má comunicação com a operadora de turismo"),
                reason(4, type, "El tour no correspondía a la descripción", "Tour did not match the description", "O passeio não correspondia à descrição"),
                reason(5, type, "Problemas con el transporte", "Problems with transportation", "Problemas com o transporte"),
                reason(6, type, "Mala relación calidad-precio", "Poor value for money", "Má relação custo-benefício"),
                reason(7, type, "No cumplieron con lo que incluye el tour", "Did not deliver what the tour included", "Não cumpriram o que estava incluído no passeio")
        );
    }

    private ReviewReasonCatalogResponse.Item reason(
            int id,
            ReviewReasonTypeEnum type,
            String es,
            String en,
            String pt
    ) {
        return ReviewReasonCatalogResponse.Item.builder()
                .id(id)
                .label(TranslatedField.of(es, en, pt))
                .type(type)
                .build();
    }
}
