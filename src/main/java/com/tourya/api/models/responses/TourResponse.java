package com.tourya.api.models.responses;

import lombok.Data;

@Data
public class TourResponse {
    private Integer id;
    private String name;
    private String description;
    private String duration;
    private Integer maxPeople;
    private Integer highlight;
    private TourCategoryResponse tourCategory;
    private ProveedorResponse proveedor;

}
