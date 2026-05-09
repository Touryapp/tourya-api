package com.tourya.api.services;

import com.tourya.api.exceptions.ResourceNotFoundException;
import com.tourya.api.models.TouristProfile;
import com.tourya.api.models.User;
import com.tourya.api.models.request.TouristProfileUpsertRequest;
import com.tourya.api.models.responses.TouristProfileResponse;
import com.tourya.api.repository.TouristProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class TouristProfileService {

    private static final ZoneId CO_ZONE = ZoneId.of("America/Bogota");

    private final TouristProfileRepository touristProfileRepository;
    private final IStorageService storageService;

    @Transactional(readOnly = true)
    public TouristProfileResponse getMyProfile(Authentication connectedUser) {
        User user = (User) connectedUser.getPrincipal();
        TouristProfile profile = touristProfileRepository.findByUserId(user.getId())
                .orElseGet(() -> TouristProfile.builder()
                        .userId(user.getId())
                        .firstName(user.getFirstname())
                        .lastName(user.getLastname())
                        .email(user.getEmail())
                        .createdAt(OffsetDateTime.now(CO_ZONE))
                        .updatedAt(OffsetDateTime.now(CO_ZONE))
                        .build());

        return toResponse(profile);
    }

    @Transactional
    public TouristProfileResponse upsertMyProfile(TouristProfileUpsertRequest request, Authentication connectedUser) {
        User user = (User) connectedUser.getPrincipal();
        OffsetDateTime now = OffsetDateTime.now(CO_ZONE);

        TouristProfile profile = touristProfileRepository.findByUserId(user.getId())
                .orElseGet(() -> TouristProfile.builder()
                        .userId(user.getId())
                        .createdAt(now)
                        .build());

        profile.setFirstName(request.getFirstName());
        profile.setLastName(request.getLastName());
        profile.setDocumentNumber(request.getDocumentNumber());
        profile.setPhone(request.getPhone());
        profile.setEmail(request.getEmail());
        profile.setCity(request.getCity());
        profile.setState(request.getState());
        profile.setCountry(request.getCountry());
        profile.setUpdatedAt(now);

        TouristProfile saved = touristProfileRepository.save(profile);
        return toResponse(saved);
    }

    @Transactional
    public void deleteMyProfile(Authentication connectedUser) {
        User user = (User) connectedUser.getPrincipal();
        touristProfileRepository.findByUserId(user.getId()).ifPresent(touristProfileRepository::delete);
    }

    @Transactional(readOnly = true)
    public boolean isMyAddressComplete(Authentication connectedUser) {
        User user = (User) connectedUser.getPrincipal();
        TouristProfile profile = touristProfileRepository.findByUserId(user.getId()).orElse(null);
        if (profile == null) return false;
        return notBlank(profile.getFirstName())
                && notBlank(profile.getLastName())
                && notBlank(profile.getDocumentNumber())
                && notBlank(profile.getPhone())
                && notBlank(profile.getEmail())
                && notBlank(profile.getCity())
                && notBlank(profile.getState())
                && notBlank(profile.getCountry());
    }

    @Transactional
    public TouristProfileResponse uploadMyPhoto(MultipartFile file, Authentication connectedUser) throws IOException {
        validatePhoto(file);

        User user = (User) connectedUser.getPrincipal();
        OffsetDateTime now = OffsetDateTime.now(CO_ZONE);
        TouristProfile profile = touristProfileRepository.findByUserId(user.getId())
                .orElseGet(() -> TouristProfile.builder()
                        .userId(user.getId())
                        .createdAt(now)
                        .build());

        String url = storageService.uploadFile("tourist-profiles/" + user.getId(), file);
        profile.setPhotoUrl(url);
        profile.setUpdatedAt(now);
        TouristProfile saved = touristProfileRepository.save(profile);
        return toResponse(saved);
    }

    private TouristProfileResponse toResponse(TouristProfile profile) {
        return TouristProfileResponse.builder()
                .id(profile.getId())
                .userId(profile.getUserId())
                .firstName(profile.getFirstName())
                .lastName(profile.getLastName())
                .documentNumber(profile.getDocumentNumber())
                .phone(profile.getPhone())
                .email(profile.getEmail())
                .city(profile.getCity())
                .state(profile.getState())
                .country(profile.getCountry())
                .photoUrl(profile.getPhotoUrl())
                .build();
    }

    private static boolean notBlank(String s) {
        return s != null && !s.trim().isEmpty();
    }

    private static void validatePhoto(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required");
        }
        // 1MB max (mismo criterio que comprobantes)
        if (file.getSize() > 1_048_576) {
            throw new IllegalArgumentException("File too large. Max 1MB");
        }
        String ct = file.getContentType() != null ? file.getContentType().toLowerCase() : "";
        boolean ok = ct.equals("image/png")
                || ct.equals("image/jpeg")
                || ct.equals("image/jpg")
                || ct.equals("image/webp");
        if (!ok) {
            throw new IllegalArgumentException("Invalid file type. Only images are allowed");
        }
    }
}

