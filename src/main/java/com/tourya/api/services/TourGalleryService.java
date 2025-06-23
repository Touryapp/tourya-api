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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TourGalleryService {

    private final TourGalleryRepository repository;
    private final S3Service s3Service;
    private final TourGalleryMapper mapper;
    private final ProviderService providerService;
    private final TourService tourService;

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



    private Tour getTour(Integer tourId, Integer providerId) {
        Tour tour = tourService.getTourByIdAndProviderId(tourId, providerId);
        if (tour != null) {
            return tour;
        } else {
            throw new ResourceNotFoundException("No tour with this id was found for this provider.");
        }
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

    @Transactional
    public List<TourGalleryResponse> syncTourGallery(
            Integer tourId,
            List<MultipartFile> newFiles, // Contiene solo los archivos de las nuevas imágenes
            List<TourGalleryRequest> galleryRequests, // Contiene la metadata para TODAS las imágenes (existentes y nuevas)
            Authentication connectedUser
    ) throws IOException {
        // 1. Validaciones iniciales
        User user = (User) connectedUser.getPrincipal();
        if (!Utils.isProvider(user.getRoles())) {
            throw new InsufficientPrivilegesException("You have no privileges to perform this action.");
        }
        Provider provider = providerService.findByUserAndStatusActive(user);
        Tour tour = getTourOrThrow(tourId, provider.getId());

        // 2. Obtener el estado actual de la galería
        List<TourGallery> currentGalleries = repository.findByTourIdOrderByOrderIndexAsc(tourId);
        Map<Integer, TourGallery> currentGalleryMap = currentGalleries.stream()
                .collect(Collectors.toMap(TourGallery::getId, Function.identity()));

        Set<Integer> incomingGalleryIds = galleryRequests.stream()
                .map(TourGalleryRequest::getId)
                .filter(Objects::nonNull) // Filtrar solo los IDs no nulos (existentes)
                .collect(Collectors.toSet());

        // 3. Identificar y eliminar imágenes que ya no están en el estado deseado (eliminadas)
        List<TourGallery> toDelete = currentGalleries.stream()
                .filter(gallery -> !incomingGalleryIds.contains(gallery.getId()))
                .toList();

        for (TourGallery gallery : toDelete) {
            s3Service.deleteFile(gallery.getImageUrl());
            repository.delete(gallery);
        }

        // 4. Procesar las solicitudes del frontend (actualizar existentes y añadir nuevas)
        List<TourGallery> updatedAndNewGalleries = new ArrayList<>();
        int newFileIndex = 0; // Para asociar newFiles con TourGalleryRequest sin ID

        for (TourGalleryRequest request : galleryRequests) {
            if (request.getId() != null) {
                // Es una imagen existente: actualizar metadata si ha cambiado
                TourGallery existingGallery = currentGalleryMap.get(request.getId());
                if (existingGallery != null) {
                    // Solo actualiza si pertenece a este tour (medida de seguridad extra)
                    if (existingGallery.getTour().getId().equals(tourId)) {
                        boolean changed = updateGalleryEntityIfChanged(existingGallery, request, user);
                        if (changed) {
                            updatedAndNewGalleries.add(repository.save(existingGallery));
                        } else {
                            updatedAndNewGalleries.add(existingGallery); // Mantener sin guardar si no hay cambios
                        }
                    }
                } else {
                    // Caso donde el frontend envía un ID que no existe en el backend
                    // Esto puede ocurrir si el frontend está desincronizado o si el ID fue eliminado por otra operación
                    // Podrías lanzar una excepción o simplemente ignorarlo.
                    System.err.println("Warning: TourGalleryRequest with ID " + request.getId() + " not found in current gallery. Skipping update.");
                }
            } else {
                // Es una imagen nueva: crear nueva entrada
                if (newFiles != null && newFileIndex < newFiles.size()) {
                    MultipartFile file = newFiles.get(newFileIndex);
                    TourGallery newGallery = buildGalleryEntity(file, request, user, tour);
                    updatedAndNewGalleries.add(repository.save(newGallery));
                    newFileIndex++;
                } else {
                    // Error: metadata de nueva imagen sin archivo correspondiente
                    throw new IllegalArgumentException("Metadata for new image provided without corresponding file or file count mismatch.");
                }
            }
        }

        // 5. Validar que todos los archivos nuevos tengan su metadata correspondiente
        if (newFiles != null && newFileIndex != newFiles.size()) {
            throw new IllegalArgumentException("Not all new files provided have corresponding metadata entries in galleryData.");
        }

        // 6. Asegurar que el orderIndex = 1 sea la foto principal
        // Después de todas las operaciones (crear/actualizar/eliminar), reordenamos si es necesario
        // y definimos la "foto principal".
        List<TourGallery> finalGalleries = repository.findByTourIdOrderByOrderIndexAsc(tourId);

        // Lógica para el orderIndex = 1
        ensurePrincipalImage(finalGalleries);

        // 7. Retornar la galería actualizada (obtenida de la DB para reflejar todos los cambios)
            return repository.findByTourIdOrderByOrderIndexAsc(tourId).stream()
                .map(mapper::toTourGalleryResponse)
                .toList();
    }

    /**
     * Actualiza la entidad si hay cambios en la descripción o el orderIndex.
     * Retorna true si hubo cambios, false en caso contrario.
     */
    private boolean updateGalleryEntityIfChanged(TourGallery entity, TourGalleryRequest request, User user) {
        boolean changed = false;
        if (!Objects.equals(entity.getDescription(), request.getDescription())) {
            entity.setDescription(request.getDescription());
            changed = true;
        }
        if (!Objects.equals(entity.getOrderIndex(), request.getOrderIndex())) {
            entity.setOrderIndex(request.getOrderIndex());
            changed = true;
        }
        if (changed) {
            entity.setLastModifiedBy(user.getId());
            entity.setLastModifiedDate(LocalDateTime.now());
        }
        return changed;
    }

    private void ensurePrincipalImage(List<TourGallery> galleries) {
        // Encontrar imágenes con orderIndex = 1
        List<TourGallery> principalCandidates = galleries.stream()
                .filter(g -> g.getOrderIndex() != null && g.getOrderIndex() == 1)
                .collect(Collectors.toList());

        if (principalCandidates.isEmpty() && !galleries.isEmpty()) {
            // Si no hay ninguna principal y hay imágenes, la primera imagen se convierte en principal
            TourGallery firstGallery = galleries.get(0);
            firstGallery.setOrderIndex(1);
            repository.save(firstGallery);
        } else if (principalCandidates.size() > 1) {
            // Si hay múltiples principales, elige la primera y desmarca las demás
            TourGallery actualPrincipal = principalCandidates.get(0); // Podrías tener una lógica más sofisticada aquí
            for (int i = 1; i < principalCandidates.size(); i++) {
                TourGallery otherPrincipal = principalCandidates.get(i);
                if (otherPrincipal.getId() != null && !otherPrincipal.getId().equals(actualPrincipal.getId())) {
                    otherPrincipal.setOrderIndex(null); // O un número diferente
                    repository.save(otherPrincipal);
                }
            }
        }
        // Si hay exactamente una principal, no se hace nada.
    }

    @Transactional
    public List<TourGalleryResponse> syncTourGalleryWithUpdate(
            Integer tourId,
            List<MultipartFile> filesList, // ¡Ahora es una lista!
            List<TourGalleryRequest> galleryRequests,
            Authentication connectedUser
    ) throws IOException {
        // 1. Validaciones iniciales (sin cambios)
        User user = (User) connectedUser.getPrincipal();
        if (!Utils.isProvider(user.getRoles())) {
            throw new InsufficientPrivilegesException("You have no privileges to perform this action.");
        }
        Provider provider = providerService.findByUserAndStatusActive(user);
        Tour tour = getTourOrThrow(tourId, provider.getId());

        // ** VALIDACIONES DE DUPLICIDAD DE KEYS (NUEVA LÓGICA) **

        // Valida que no haya 'fileKey' duplicados en las solicitudes de galleryData
        Set<String> uniqueFileKeysInRequests = new HashSet<>();
        for (TourGalleryRequest request : galleryRequests) {
            if (request.getFileKey() != null && !request.getFileKey().isEmpty()) {
                if (!uniqueFileKeysInRequests.add(request.getFileKey())) {
                    throw new IllegalArgumentException("Duplicate 'fileKey' found in galleryData requests: " + request.getFileKey() + ". Each fileKey must be unique within a single sync request.");
                }
            }
        }

        // Valida que no haya 'originalFilename' duplicados en los archivos subidos
        // Esto es importante porque tu servicio los usa como clave para el mapa temporal.
        Set<String> uniqueOriginalFilenamesInFiles = new HashSet<>();
        if (filesList != null) {
            for (MultipartFile file : filesList) {
                if (file.getOriginalFilename() != null && !file.getOriginalFilename().isEmpty()) {
                    if (!uniqueOriginalFilenamesInFiles.add(file.getOriginalFilename())) {
                        throw new IllegalArgumentException("Duplicate original filename found in uploaded files: " + file.getOriginalFilename() + ". Each uploaded file must have a unique original filename.");
                    }
                }
            }
        }
        // ******************************************************


        // 2. Obtener el estado actual de la galería (sin cambios)
        List<TourGallery> currentGalleries = repository.findByTourIdOrderByOrderIndexAsc(tourId);
        Map<Integer, TourGallery> currentGalleryMap = currentGalleries.stream()
                .collect(Collectors.toMap(TourGallery::getId, Function.identity()));

        // 3. Identificar IDs de imágenes que deben permanecer (sin cambios)
        Set<Integer> incomingGalleryIds = galleryRequests.stream()
                .map(TourGalleryRequest::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 4. Identificar y eliminar imágenes que ya no están en el estado deseado (sin cambios)
        List<TourGallery> toDelete = currentGalleries.stream()
                .filter(gallery -> !incomingGalleryIds.contains(gallery.getId()))
                .toList();

        for (TourGallery gallery : toDelete) {
            s3Service.deleteFile(gallery.getImageUrl());
            repository.delete(gallery);
        }

        Map<String, MultipartFile> filesByOriginalFilename = new HashMap<>();
        if (filesList != null) {
            for (MultipartFile file : filesList) {
                if (file.getOriginalFilename() != null && !file.getOriginalFilename().isEmpty()) {
                    // Si hay nombres duplicados, el último en la lista sobrescribe.
                    // Esto es un riesgo si el frontend envía archivos con nombres duplicados.
                    filesByOriginalFilename.put(file.getOriginalFilename(), file);
                }
            }
        }
        // ***********************************************************************************

        // 5. Procesar las solicitudes del frontend (crear, actualizar metadata, o reemplazar archivo)
        List<TourGallery> savedOrUpdatedGalleries = new ArrayList<>();
        Set<String> processedFileKeys = new HashSet<>(); // Para la validación final

        for (TourGalleryRequest request : galleryRequests) {
            MultipartFile associatedFile = null;
            // Si el request tiene un fileKey, buscamos el archivo por su nombre original
            if (request.getFileKey() != null) {
                associatedFile = filesByOriginalFilename.get(request.getFileKey());
                if (associatedFile != null) {
                    processedFileKeys.add(request.getFileKey()); // Marcar como usado
                }
            }

            if (request.getId() != null) {
                // Es una imagen existente
                TourGallery existingGallery = currentGalleryMap.get(request.getId());
                if (existingGallery != null) {
                    if (!existingGallery.getTour().getId().equals(tourId)) {
                        throw new IllegalArgumentException("Gallery image with ID " + request.getId() + " does not belong to tour " + tourId);
                    }

                    boolean changed = updateGalleryEntityIfChanged(existingGallery, request, user);

                    if (request.isReplaceFile()) {
                        if (associatedFile == null) {
                            throw new IllegalArgumentException("TourGalleryRequest with ID " + request.getId() + " marked for file replacement but no corresponding file was provided with fileKey (originalFilename): " + request.getFileKey());
                        }
                        // log.info("Replacing file for existing gallery ID: {} with new file (originalFilename: {}).", existingGallery.getId(), request.getFileKey());
                        s3Service.deleteFile(existingGallery.getImageUrl());
                        String newImageUrl = s3Service.uploadFile("tours/" + tour.getId(), associatedFile);
                        existingGallery.setImageUrl(newImageUrl);
                        changed = true;
                    } else if (associatedFile != null) {
                        // Error: Archivo proporcionado para imagen existente pero 'replaceFile' no es true
                        throw new IllegalArgumentException("File provided for existing gallery ID " + request.getId() + " (originalFilename: " + request.getFileKey() + ") but 'replaceFile' is not true. If you intend to replace the file, set 'replaceFile' to true.");
                    }

                    if (changed) {
                        savedOrUpdatedGalleries.add(repository.save(existingGallery));
                    } else {
                        savedOrUpdatedGalleries.add(existingGallery);
                    }
                } else {
                    // log.warn("TourGalleryRequest with ID {} not found in current gallery. Skipping update.", request.getId());
                }
            } else {
                // Es una imagen nueva
                if (request.getFileKey() == null || associatedFile == null) {
                    throw new IllegalArgumentException("New image request (without ID) requires a 'fileKey' (originalFilename) and a corresponding file in the 'files' part.");
                }

                // log.info("Creating new gallery entry with file (originalFilename: {}).", request.getFileKey());
                TourGallery newGallery = buildGalleryEntity(associatedFile, request, user, tour);
                savedOrUpdatedGalleries.add(repository.save(newGallery));
            }
        }

        // 6. Validar que todos los archivos enviados en 'files' fueron procesados
        // Comparamos los nombres de archivo que llegaron vs los que se usaron en el JSON.
        Set<String> originalFilenamesReceived = new HashSet<>();
        if (filesList != null) {
            filesList.forEach(file -> {
                if (file.getOriginalFilename() != null && !file.getOriginalFilename().isEmpty()) {
                    originalFilenamesReceived.add(file.getOriginalFilename());
                }
            });
        }

        if (originalFilenamesReceived.size() != processedFileKeys.size() || !originalFilenamesReceived.containsAll(processedFileKeys)) {
            Set<String> unmappedFilesFromRequest = new HashSet<>(originalFilenamesReceived);
            unmappedFilesFromRequest.removeAll(processedFileKeys);
            Set<String> missingFilesFromRequest = new HashSet<>(processedFileKeys);
            missingFilesFromRequest.removeAll(originalFilenamesReceived);

            String errorMessage = "Mismatch in file processing: ";
            if (!unmappedFilesFromRequest.isEmpty()) {
                errorMessage += "Some uploaded files were not mapped to any galleryData entry (original filenames: " + unmappedFilesFromRequest + "). ";
            }
            if (!missingFilesFromRequest.isEmpty()) {
                errorMessage += "Some galleryData entries expected a file (by original filename) that was not provided (missing original filenames: " + missingFilesFromRequest + ").";
            }
            throw new IllegalArgumentException(errorMessage.trim());
        }

        // 7. Asegurar que el orderIndex = 1 sea la foto principal (sin cambios)
        List<TourGallery> finalGalleries = repository.findByTourIdOrderByOrderIndexAsc(tourId);
        ensurePrincipalImage(finalGalleries);

        // 8. Retornar la galería actualizada (sin cambios)
        return repository.findByTourIdOrderByOrderIndexAsc(tourId).stream()
                .map(mapper::toTourGalleryResponse)
                .toList();
    }

}
