package com.tourya.api.models;

import com.tourya.api.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "service")
public class Service extends BaseEntity {

    private String name;

    @Column(name = "service_type")
    private String serviceType;

    @Column(name = "description")
    private String description;

    @Column(name = "cancellation_policy")
    private String cancellationPolicy;

    @Column(name = "status")
    private String status;
}
