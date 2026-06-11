package com.tourya.api.models;

import com.tourya.api.common.BaseEntity;
import com.tourya.api.constans.enums.ShoppingCartStatusEnum;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "shopping_cart")
public class ShoppingCart extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ShoppingCartStatusEnum status;

    @Column(name = "accommodation_name", length = 255)
    private String accommodationName;

    @Column(name = "accommodation_latitude")
    private Double accommodationLatitude;

    @Column(name = "accommodation_longitude")
    private Double accommodationLongitude;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "origin_country_id")
    private Country originCountry;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "origin_state_id")
    private State originState;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "origin_city_id")
    private City originCity;

    @Column(name = "electronic_billing", nullable = false)
    private Boolean electronicBilling = false;

    @Column(name = "billing_document_type", length = 50)
    private String billingDocumentType;

    @Column(name = "billing_document_number", length = 50)
    private String billingDocumentNumber;

    @Column(name = "billing_email", length = 255)
    private String billingEmail;

    @Column(name = "billing_customer_name", length = 255)
    private String billingCustomerName;

    @Column(name = "billing_phone", length = 30)
    private String billingPhone;

    @OneToMany(mappedBy = "shoppingCart", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ShoppingCartItem> items;

    @PrePersist
    void ensureDefaults() {
        if (electronicBilling == null) {
            electronicBilling = Boolean.FALSE;
        }
    }
}
