package com.tourya.api.models;


import com.tourya.api.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "tour")
public class Tour extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    private String name;
    private String description;

    // Many-to-One relationship with TourCategory
    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false)
    private TourCategory tourCategory;

    private String duration;

    @Column(name = "max_people")
    private Integer maxPeople;

    @Column(name = "highlight")
    private Integer highlight;

    // Many-to-One relationship with Proveedor
    @ManyToOne
    @JoinColumn(name = "proveedor_id")
    private Proveedor proveedor;


}
