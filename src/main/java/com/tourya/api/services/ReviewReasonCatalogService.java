package com.tourya.api.services;

import com.tourya.api.constans.enums.ReviewReasonTypeEnum;
import com.tourya.api.models.responses.ReviewReasonCatalogResponse;
import com.tourya.api.models.responses.ReviewReasonListResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ReviewReasonCatalogService {

    public ReviewReasonCatalogResponse getCatalog() {
        return ReviewReasonCatalogResponse.builder()
                .positive(positiveReasons())
                .negative(negativeReasons())
                .build();
    }

    public ReviewReasonListResponse getReasonsForRating(BigDecimal rating) {
        ReviewReasonTypeEnum type = inferType(rating);
        List<ReviewReasonCatalogResponse.Item> reasons = (type == ReviewReasonTypeEnum.POSITIVE)
                ? positiveReasons()
                : negativeReasons();
        return ReviewReasonListResponse.builder()
                .type(type)
                .reasons(reasons)
                .build();
    }

    private ReviewReasonTypeEnum inferType(BigDecimal rating) {
        if (rating == null) return ReviewReasonTypeEnum.NEGATIVE;
        return rating.compareTo(new BigDecimal("4.0")) >= 0
                ? ReviewReasonTypeEnum.POSITIVE
                : ReviewReasonTypeEnum.NEGATIVE;
    }

    private List<ReviewReasonCatalogResponse.Item> positiveReasons() {
        return List.of(
                ReviewReasonCatalogResponse.Item.builder().id(1).label("Servicio excepcional del guía").type(ReviewReasonTypeEnum.POSITIVE).build(),
                ReviewReasonCatalogResponse.Item.builder().id(2).label("Puntualidad del proveedor").type(ReviewReasonTypeEnum.POSITIVE).build(),
                ReviewReasonCatalogResponse.Item.builder().id(3).label("Buena organización del tour").type(ReviewReasonTypeEnum.POSITIVE).build(),
                ReviewReasonCatalogResponse.Item.builder().id(4).label("Excelente relación calidad-precio").type(ReviewReasonTypeEnum.POSITIVE).build(),
                ReviewReasonCatalogResponse.Item.builder().id(5).label("Comodidad del transporte").type(ReviewReasonTypeEnum.POSITIVE).build(),
                ReviewReasonCatalogResponse.Item.builder().id(6).label("Buena atención al cliente").type(ReviewReasonTypeEnum.POSITIVE).build(),
                ReviewReasonCatalogResponse.Item.builder().id(7).label("Recomendable para otros viajeros").type(ReviewReasonTypeEnum.POSITIVE).build()
        );
    }

    private List<ReviewReasonCatalogResponse.Item> negativeReasons() {
        return List.of(
                ReviewReasonCatalogResponse.Item.builder().id(1).label("Retraso o impuntualidad").type(ReviewReasonTypeEnum.NEGATIVE).build(),
                ReviewReasonCatalogResponse.Item.builder().id(2).label("Guía poco amable o desinformado").type(ReviewReasonTypeEnum.NEGATIVE).build(),
                ReviewReasonCatalogResponse.Item.builder().id(3).label("Mala comunicación con el operador turístico").type(ReviewReasonTypeEnum.NEGATIVE).build(),
                ReviewReasonCatalogResponse.Item.builder().id(4).label("El tour no correspondía a la descripción").type(ReviewReasonTypeEnum.NEGATIVE).build(),
                ReviewReasonCatalogResponse.Item.builder().id(5).label("Problemas con el transporte").type(ReviewReasonTypeEnum.NEGATIVE).build(),
                ReviewReasonCatalogResponse.Item.builder().id(6).label("Mala relación calidad-precio").type(ReviewReasonTypeEnum.NEGATIVE).build(),
                ReviewReasonCatalogResponse.Item.builder().id(7).label("No cumplieron con lo que incluye el tour").type(ReviewReasonTypeEnum.NEGATIVE).build()
        );
    }
}

