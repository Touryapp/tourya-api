package com.tourya.api.models.responses;

import com.tourya.api.constans.enums.AddressTypeEnum;
import com.tourya.api.models.TranslatedField;
import lombok.Data;

@Data
public class TourAddressResponse {
    private Integer id;
    private String address;
    private TranslatedField location;
    private AddressTypeEnum addressType;
    private Integer countryId;
    private Integer stateId;
    private Integer cityId;
    /**
     * TC-016 (#219 Luis 2026-08-06): nombres del pais/estado/ciudad del meeting
     * point para renderizar en tour-detail y detalle de reserva sin necesitar
     * un segundo fetch al catalogo. Se pueblan desde `TourAddress.country/state/city.name`.
     */
    private String countryName;
    private String stateName;
    private String cityName;
    private Double latitude;
    private Double longitude;
    //private TourResponse tour;
}
