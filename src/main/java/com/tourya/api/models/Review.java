package com.tourya.api.models;

import com.tourya.api.common.BaseEntity;
import com.tourya.api.constans.enums.ReviewReasonTypeEnum;
import com.tourya.api.constans.enums.ReviewStatusEnum;
import com.tourya.api.config.TranslatedFieldConverter;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad que representa una reseña de un tour.
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
@Table(name = "review")
public class Review extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reservation_id", nullable = false)
    private Long reservationId;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(name = "tour_id", nullable = false)
    private Integer tourId;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "rating", nullable = false, precision = 3, scale = 2)
    private BigDecimal rating;

    @Convert(converter = TranslatedFieldConverter.class)
    @Column(name = "comment", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private TranslatedField comment;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ReviewStatusEnum status;

    @Column(name = "review_date", nullable = false)
    private LocalDate reviewDate;

    @Column(name = "likes", nullable = false)
    @Builder.Default
    private Integer likes = 0;

    @Column(name = "dislikes", nullable = false)
    @Builder.Default
    private Integer dislikes = 0;

    @Column(name = "hearts", nullable = false)
    @Builder.Default
    private Integer hearts = 0;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason_type", length = 10)
    private ReviewReasonTypeEnum reasonType;

    @Column(name = "reason_id")
    private Integer reasonId;

    // Relaciones
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", insertable = false, updatable = false)
    private Reservation reservation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", insertable = false, updatable = false)
    private ShoppingCartItem shoppingCartItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tour_id", insertable = false, updatable = false)
    private Tour tour;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ReviewAttachment> attachments = new ArrayList<>();

    @OneToOne(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true)
    private ReviewAnswer answer;
}

