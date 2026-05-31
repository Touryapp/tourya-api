package com.tourya.api.repository;

import com.tourya.api.models.Token;
import com.tourya.api.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TokenRepository extends JpaRepository<Token, Integer> {
    Optional<Token> findByToken(String token);

    @Modifying
    @Query("DELETE FROM Token t WHERE t.user = :user AND t.validatedAt IS NULL")
    void deletePendingByUser(@Param("user") User user);
}
