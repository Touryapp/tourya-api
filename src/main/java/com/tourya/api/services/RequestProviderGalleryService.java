package com.tourya.api.services;

import com.tourya.api._utils.Utils;
import com.tourya.api.exceptions.InsufficientPrivilegesException;
import com.tourya.api.exceptions.ResourceNotFoundException;
import com.tourya.api.models.*;
import com.tourya.api.models.mapper.RequestProviderGalleryMapper;

import com.tourya.api.models.request.RequestProviderGalleryRequest;
import com.tourya.api.models.responses.RequestProviderGalleryResponse;
import com.tourya.api.repository.RequestProviderGalleryRepository;
import com.tourya.api.repository.RequestProviderRepository;
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
public class RequestProviderGalleryService {

    private final RequestProviderGalleryRepository repository;
    private final RequestProviderRepository requestProviderRepository;
    private final S3Service s3Service;
    private final RequestProviderGalleryMapper mapper;
    private final ProviderService providerService;

    @Transactional
    public List<RequestProviderGalleryResponse> syncGalleryMetadata(Integer requestId,
                                                                    List<RequestProviderGalleryRequest> addedGalleries,
                                                                    List<RequestProviderGalleryRequest> deletedGalleries,
                                                                    List<MultipartFile> files,
                                                                    Authentication connectedUser) throws IOException {


        User user = getAuthenticatedUser(connectedUser);
        Provider provider = providerService.findByUserAndStatusActive(user);
        RequestProvider requestProvider = validateAndGetRequestProvider(requestId, provider.getId());

        List<RequestProviderGallery> toDeleteEntities = deletedGalleries.stream()
                .map(id -> repository.findById(id.getId())
                        .orElseThrow(() -> new ResourceNotFoundException("Gallery item not found for deletion.")))
                .toList();

        toDeleteEntities.forEach(entity -> s3Service.deleteFile(entity.getImageUrl()));
        repository.deleteAll(toDeleteEntities);


        if (files == null || files.isEmpty()) return List.of();

        if (files.size() != addedGalleries.size()) {
            throw new IllegalArgumentException("Number of uploaded files must match number of metadata entries.");
        }

        List<RequestProviderGallery> saved = IntStream.range(0, files.size())
                .mapToObj(i -> {
                    MultipartFile file = files.get(i);
                    RequestProviderGalleryRequest meta = addedGalleries.get(i);
                    return buildEntity(file, meta, user, requestProvider);
                })
                .map(repository::save)
                .toList();

        return saved.stream()
                .map(mapper::toRequestProviderGalleryResponse)
                .toList();
    }


    public List<RequestProviderGalleryResponse> create(List<MultipartFile> files,
                                                       List<RequestProviderGalleryRequest> requests,
                                                       Integer requestId,
                                                       Authentication connectedUser) throws IOException {

        validateFilesAndMetadata(files, requests);

        User user = getAuthenticatedUser(connectedUser);
        Provider provider = providerService.findByUserAndStatusActive(user);
        RequestProvider requestProvider = validateAndGetRequestProvider(requestId, provider.getId());

        List<RequestProviderGallery> savedGalleries = IntStream.range(0, files.size())
                .mapToObj(i -> buildGalleryEntity(files.get(i), requests.get(i), user, requestProvider))
                .map(repository::save)
                .toList();

        return savedGalleries.stream()
                .map(mapper::toRequestProviderGalleryResponse)
                .toList();
    }


    public List<RequestProviderGalleryResponse> getAllByRequest(Integer requestId, Authentication connectedUser) {
        User user = (User) connectedUser.getPrincipal();
        if (!Utils.isProvider(user.getRoles())) {
            throw new InsufficientPrivilegesException("You have no privileges to perform this action.");
        }

        Provider provider = providerService.findByUserAndStatusActive(user);
        RequestProvider requestProvider = validateAndGetRequestProvider(requestId, provider.getId());

        return repository.findByRequestProviderIdOrderByOrderIndexAsc(requestProvider.getId())
                .stream()
                .map(mapper::toRequestProviderGalleryResponse)
                .toList();
    }

    @Transactional
    public List<RequestProviderGalleryResponse> replaceAll(List<MultipartFile> files,
                                                           List<RequestProviderGalleryRequest> requests,
                                                           Integer requestId,
                                                           Authentication connectedUser) throws IOException {

        User user = getAuthenticatedUser(connectedUser);
        Provider provider = providerService.findByUserAndStatusActive(user);
        RequestProvider requestProvider = validateAndGetRequestProvider(requestId, provider.getId());

        repository.findByRequestProviderIdOrderByOrderIndexAsc(requestId).forEach(gallery -> {
            s3Service.deleteFile(gallery.getImageUrl());
            repository.delete(gallery);
        });

        return create(files, requests, requestId, connectedUser);
    }

    private RequestProviderGallery buildEntity(MultipartFile file,
                                               RequestProviderGalleryRequest request,
                                               User user,
                                               RequestProvider requestProvider) {
        try {
            RequestProviderGallery entity = mapper.toRequestProviderGallery(request);
            entity.setRequestProvider(requestProvider);
            entity.setCreatedBy(user.getId());
            entity.setCreatedDate(LocalDateTime.now());
            entity.setImageUrl(s3Service.uploadFile("requests/" + requestProvider.getId(), file));
            return entity;
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload file to S3", e);
        }
    }
    private void validateFilesAndMetadata(List<MultipartFile> files, List<RequestProviderGalleryRequest> requests) {
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
    private RequestProvider validateAndGetRequestProvider(Integer requestId, Integer providerId) {
        RequestProvider requestProvider = requestProviderRepository.findByIdAndProviderId(requestId, providerId);
        if (requestProvider == null) {
            throw new ResourceNotFoundException("No request with this ID was found for this provider.");
        }
        return requestProvider;
    }



    private RequestProviderGallery buildGalleryEntity(MultipartFile file, RequestProviderGalleryRequest request, User user, RequestProvider requestProvider) {
        try {
            RequestProviderGallery entity = mapper.toRequestProviderGallery(request);
            entity.setRequestProvider(requestProvider);
            entity.setCreatedBy(user.getId());
            entity.setCreatedDate(LocalDateTime.now());
            entity.setImageUrl(s3Service.uploadFile("requests/" + requestProvider.getId(), file));
            return entity;
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload file to S3", e);
        }
    }
}
