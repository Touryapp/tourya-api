package com.tourya.api.services;

import com.tourya.api.models.User;
import com.tourya.api.models.WishlistItem;
import com.tourya.api.repository.WishlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WishlistService {

    private static final ZoneId CO_ZONE = ZoneId.of("America/Bogota");
    private final WishlistRepository wishlistRepository;

    @Transactional(readOnly = true)
    public List<Integer> getMyWishlistTourIds(Authentication connectedUser) {
        User user = (User) connectedUser.getPrincipal();
        return wishlistRepository.findByUserId(user.getId()).stream()
                .map(WishlistItem::getTourId)
                .toList();
    }

    @Transactional
    public void addToWishlist(Integer tourId, Authentication connectedUser) {
        if (tourId == null) throw new IllegalArgumentException("tourId is required");
        User user = (User) connectedUser.getPrincipal();
        if (wishlistRepository.existsByUserIdAndTourId(user.getId(), tourId)) return;
        WishlistItem item = new WishlistItem();
        item.setUserId(user.getId());
        item.setTourId(tourId);
        item.setCreatedAt(OffsetDateTime.now(CO_ZONE));
        wishlistRepository.save(item);
    }

    @Transactional
    public void removeFromWishlist(Integer tourId, Authentication connectedUser) {
        if (tourId == null) throw new IllegalArgumentException("tourId is required");
        User user = (User) connectedUser.getPrincipal();
        wishlistRepository.deleteByUserIdAndTourId(user.getId(), tourId);
    }
}

