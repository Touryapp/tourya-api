package com.tourya.api.repository;

import com.tourya.api.models.ProviderUser;
import com.tourya.api.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProviderUserRepository extends JpaRepository<ProviderUser, Integer> {

    Optional<ProviderUser> findByUser(User user);

    Optional<ProviderUser> findByUserId(Integer userId);

    List<ProviderUser> findByProviderId(Integer providerId);

    boolean existsByUserId(Integer userId);

    @Query("""
            SELECT pu FROM ProviderUser pu
            JOIN FETCH pu.provider p
            WHERE pu.user.id = :userId
            """)
    Optional<ProviderUser> findByUserIdWithProvider(@Param("userId") Integer userId);

    @Query("""
            SELECT pu FROM ProviderUser pu
            JOIN FETCH pu.user u
            JOIN FETCH pu.provider p
            WHERE pu.id = :id
            """)
    Optional<ProviderUser> findByIdWithUserAndProvider(@Param("id") Integer id);
}
