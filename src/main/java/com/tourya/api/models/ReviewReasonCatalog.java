package com.tourya.api.models;

import com.tourya.api.config.TranslatedFieldConverter;
import com.tourya.api.constans.enums.ReviewReasonTypeEnum;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "review_reason_catalog")
public class ReviewReasonCatalog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason_type", nullable = false, length = 20)
    private ReviewReasonTypeEnum reasonType;

    @Column(name = "reason_id", nullable = false)
    private Integer reasonId;

    @Convert(converter = TranslatedFieldConverter.class)
    @Column(name = "label", columnDefinition = "jsonb", nullable = false)
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    private TranslatedField label;
}
