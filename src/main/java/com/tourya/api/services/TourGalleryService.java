package com.tourya.api.services;

import com.tourya.api._utils.Utils;
import com.tourya.api.exceptions.InsufficientPrivilegesException;
import com.tourya.api.exceptions.ResourceNotFoundException;
import com.tourya.api.models.*;
import com.tourya.api.models.mapper.TourGalleryMapper;
import com.tourya.api.models.request.TourGalleryRequest;
import com.tourya.api.models.responses.TourGalleryResponse;
import com.tourya.api.repository.TourGalleryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class TourGalleryService {

    private final TourGalleryRepository repository;
    private final S3Service s3Service;
    private final TourGalleryMapper mapper;
    private final ProviderService providerService;
    private final TourService tourService;

    public List<TourGalleryResponse> create(List<MultipartFile> files,
                                            List<TourGalleryRequest> requests,
                                            Integer tourId,
                                            Authentication connectedUser) throws IOException {

        validateFilesAndMetadata(files, requests);

        User user = getAuthenticatedUser(connectedUser);
        Provider provider = providerService.findByUserAndStatusActive(user);
        Tour tour = getTourOrThrow(tourId, provider.getId());

        List<TourGallery> savedGalleries = IntStream.range(0, files.size())
                .mapToObj(i -> buildGalleryEntity(files.get(i), requests.get(i), user, tour))
                .map(repository::save)
                .toList();

        return savedGalleries.stream()
                .map(mapper::toTourGalleryResponse)
                .toList();
    }

    public List<TourGalleryResponse> getAllByTour(Integer tourId, Authentication connectedUser) {
        User user = (User) connectedUser.getPrincipal();
        if (!Utils.isProvider(user.getRoles())) {
            throw new InsufficientPrivilegesException("You have no privileges to perform this action.");
        }

        Provider provider = providerService.findByUserAndStatusActive(user);
        Tour tour = getTour(tourId, provider.getId());

        return repository.findByTourIdOrderByOrderIndexAsc(tour.getId())
                .stream()
                .map(mapper::toTourGalleryResponse)
                .toList();
    }

    @Transactional
    public List<TourGalleryResponse> replaceAllForTour(List<MultipartFile> files,
                                                       List<TourGalleryRequest> requests,
                                                       Integer tourId,
                                                       Authentication connectedUser) throws IOException {
        User user = (User) connectedUser.getPrincipal();
        if (!Utils.isProvider(user.getRoles())) {
            throw new InsufficientPrivilegesException("You have no privileges to perform this action.");
        }

        Provider provider = providerService.findByUserAndStatusActive(user);
        Tour tour = getTour(tourId, provider.getId());

        repository.findByTourIdOrderByOrderIndexAsc(tourId).forEach(gallery -> {
            s3Service.deleteFile(gallery.getImageUrl());
            repository.delete(gallery);
        });

        return create(files, requests, tourId, connectedUser);
    }

    private Tour getTour(Integer tourId, Integer providerId) {
        Tour tour = tourService.getTourByIdAndProviderId(tourId, providerId);
        if (tour != null) {
            return tour;
        } else {
            throw new ResourceNotFoundException("No tour with this id was found for this provider.");
        }
    }
    private void validateFilesAndMetadata(List<MultipartFile> files, List<TourGalleryRequest> requests) {
        if (files.size() != requests.size()) {
            throw new IllegalArgumentException("Each uploaded file must match a metadata entry.");
        }
    }

    private User getAuthenticatedUser(Authentication connectedUser) {
        User user = (User) connectedUser.getPrincipal();
        if (!Utils.isProvider(user.getRoles())) {
            throw new InsufficientPrivilegesException("You have no privileges to perform this action.");
        }
        return user;
    }

    private Tour getTourOrThrow(Integer tourId, Integer providerId) {
        Tour tour = tourService.getTourByIdAndProviderId(tourId, providerId);
        if (tour == null) {
            throw new ResourceNotFoundException("No tour with this ID was found for this provider.");
        }
        return tour;
    }

    private TourGallery buildGalleryEntity(MultipartFile file, TourGalleryRequest request, User user, Tour tour) {
        try {
            TourGallery entity = mapper.toTourGallery(request);
            entity.setTour(tour);
            entity.setCreatedBy(user.getId());
            entity.setCreatedDate(LocalDateTime.now());
            entity.setImageUrl(s3Service.uploadFile("tours/" + tour.getId(), file));
            return entity;
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload file to S3", e);
        }
    }
}
