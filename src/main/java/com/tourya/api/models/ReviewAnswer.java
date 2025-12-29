package com.tourya.api.models;

import com.tourya.api.common.BaseEntity;
import com.tourya.api.config.TranslatedFieldConverter;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad que representa la respuesta del proveedor a una reseña.
 * 
 * @author Tourya API Team
 * @version 1.0
 */
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "review_answer")
public class ReviewAnswer extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long answerId;

    @Column(name = "review_id", nullable = false, unique = true)
    private Long reviewId;

    @Convert(converter = TranslatedFieldConverter.class)
    @Column(name = "comment", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private TranslatedField comment;

    @Column(name = "provider_name", length = 255)
    private String providerName;

    @Column(name = "provider_image", length = 500)
    private String providerImage;

    @Column(name = "date")
    private LocalDate date;

    @Column(name = "likes", nullable = false)
    private Integer likes = 0;

    @Column(name = "dislikes", nullable = false)
    private Integer dislikes = 0;

    @Column(name = "hearts", nullable = false)
    private Integer hearts = 0;

    // Relación con Review
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", insertable = false, updatable = false)
    private Review review;

    @OneToMany(mappedBy = "answer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReviewAnswerAttachment> attachments = new ArrayList<>();
}

