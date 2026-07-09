package com.tourya.api.repository;

import com.tourya.api.models.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByJti(String jti);

    @Modifying
    @Query("UPDATE RefreshToken rt " +
           "SET rt.revokedAt = :revokedAt, rt.revokedReason = :reason " +
           "WHERE rt.familyId = :familyId AND rt.revokedAt IS NULL")
    int revokeFamily(@Param("familyId") String familyId,
                     @Param("revokedAt") OffsetDateTime revokedAt,
                     @Param("reason") String reason);
}
