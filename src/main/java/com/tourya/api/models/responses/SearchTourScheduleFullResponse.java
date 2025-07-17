package com.tourya.api.models.responses;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
public class SearchTourScheduleFullResponse {
    private Schedule schedule;
    private Tour tour;
    private Address address;
    private List<Gallery> gallery;
    private List<Slot> slots;

    @Data
    public static class Schedule {
        private Integer id;
        private LocalDate scheduleDate;
        private LocalTime startTime;
        private LocalTime endTime;
        private Integer maxCapacity;
        private Integer reservedCapacity;
        private Boolean isUnlimitedCapacity;
        private String status;
    }

    @Data
    public static class Tour {
        private Integer id;
        private String name;
        private String description;
        private String duration;
        private Double rating;
    }

    @Data
    public static class Address {
        private String city;
        private String state;
        private String country;
        private String address;
    }

    @Data
    public static class Gallery {
        private String imageUrl;
        private String description;
        private Integer order;
    }

    @Data
    public static class Slot {
        private Integer slotId;
        private LocalTime startTime;
        private LocalTime endTime;
        private Integer minCapacity;
        private Integer maxCapacity;
        private List<Price> prices;
    }

    @Data
    public static class Price {
        private String ageType;
        private Integer minAge;
        private Integer maxAge;
        private Double price;
    }
}
