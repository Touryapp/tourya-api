package com.tourya.api.models.responses;

import com.tourya.api.models.TranslatedField;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
public class SearchTourScheduleFullResponse {

    private TourInfo tour;                       // Info completa del tour (incluye address, tags, gallery)
    private List<TourScheduleResponse> schedules; // N schedules por tour

    // ===================== TOUR =====================
    @Data
    public static class TourInfo {
        private Integer id;
        private TranslatedField name;
        private TranslatedField description;
        private String duration;
        private String durationType;             // Si tienes enum en Java, cámbialo al enum correspondiente
        private Double rating;
        private String status;                   // accepted, etc.
        private List<TagResponse> tags;          // Tags del tour
        private AddressResponse address;         // Dirección del tour
        private List<GalleryItemResponse> gallery; // Imágenes del tour
    }

    @Data
    public static class TagResponse {
        private Integer id;
        private String name;
        private String category;
    }

    @Data
    public static class AddressResponse {
        private Integer country;     // country_id (según SP)
        private Integer state;       // state_id
        private Integer city;        // city_id
        private String address;
        private Double latitude;
        private Double longitude;
    }

    @Data
    public static class GalleryItemResponse {
        private Integer id;
        private String imageUrl;
        private TranslatedField description;
        private Integer order;
    }

    // ===================== SCHEDULE =====================
    @Data
    public static class TourScheduleResponse {
        private Integer id;
        private LocalDate scheduleDate;
        private LocalTime startTime;
        private LocalTime endTime;
        private Integer maxCapacity;
        private Integer reservedCapacity;
        private Boolean isUnlimitedCapacity;
        private String status; // Si tienes enum (TourScheduleStatusEnum), cámbialo aquí
        private TourScheduleConfigResponse config; // Config asociada al schedule
    }

    // ===================== CONFIG =====================
    @Data
    public static class TourScheduleConfigResponse {
        private Integer id;
        private List<TourScheduleSlotResponse> slots; // N slots por config
    }

    // ===================== SLOT =====================
    @Data
    public static class TourScheduleSlotResponse {
        private Integer slotId;
        private LocalTime startTime;
        private LocalTime endTime;
        private Integer minCapacity;
        private Integer maxCapacity;
        private List<TourSchedulePriceResponse> prices; // N prices por slot
        private HighestPriceResponse highestPrice;      // price más alto por slot
    }

    // ===================== PRICE =====================
    @Data
    public static class TourSchedulePriceResponse {
        private String ageType;     // Si tienes enum (AgePriceTypeEnum), cámbialo aquí
        private Integer minAge;
        private Integer maxAge;
        private BigDecimal price;
    }

    @Data
    public static class HighestPriceResponse {
        private String ageType;
        private BigDecimal price;
    }
}
