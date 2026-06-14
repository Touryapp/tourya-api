package com.tourya.api.services;

import com.tourya.api.models.Provider;
import com.tourya.api.models.ProviderUserTour;
import com.tourya.api.models.Tour;
import com.tourya.api.models.User;
import com.tourya.api.models.responses.TourOperatorResponse;
import com.tourya.api.repository.ProviderUserTourRepository;
import com.tourya.api.repository.TourRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TourPrincipalOperatorService {

    private final ProviderUserTourRepository providerUserTourRepository;
    private final TourRepository tourRepository;

    public TourOperatorResponse resolveForTourId(Integer tourId) {
        if (tourId == null) {
            return null;
        }
        return tourRepository.findById(tourId)
                .map(this::resolveForTour)
                .orElse(null);
    }

    public TourOperatorResponse resolveForTour(Tour tour) {
        if (tour == null || tour.getId() == null) {
            return null;
        }
        return providerUserTourRepository.findPrincipalByTourId(tour.getId())
                .map(this::fromOperatorLink)
                .orElseGet(() -> fromProvider(tour.getProvider()));
    }

    private TourOperatorResponse fromOperatorLink(ProviderUserTour link) {
        User user = link.getProviderUser().getUser();
        Provider provider = link.getProviderUser().getProvider();
        return TourOperatorResponse.builder()
                .providerUserId(link.getProviderUser().getId())
                .name(user != null ? user.fullName() : null)
                .email(user != null ? user.getEmail() : null)
                .phone(parsePhoneDigits(provider != null ? provider.getPhone() : null))
                .build();
    }

    private TourOperatorResponse fromProvider(Provider provider) {
        if (provider == null) {
            return null;
        }
        return TourOperatorResponse.builder()
                .name(provider.getName())
                .email(provider.getUser() != null ? provider.getUser().getEmail() : null)
                .phone(parsePhoneDigits(provider.getPhone()))
                .build();
    }

    private Long parsePhoneDigits(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }
        String digits = phone.replaceAll("[^0-9]", "");
        return digits.isEmpty() ? null : Long.parseLong(digits);
    }
}
