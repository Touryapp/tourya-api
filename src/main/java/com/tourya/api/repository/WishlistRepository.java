package com.tourya.api.repository;

import com.tourya.api.models.WishlistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WishlistRepository extends JpaRepository<WishlistItem, WishlistItem.Pk> {
    List<WishlistItem> findByUserId(Integer userId);
    boolean existsByUserIdAndTourId(Integer userId, Integer tourId);
    void deleteByUserIdAndTourId(Integer userId, Integer tourId);
}

