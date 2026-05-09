package com.tourya.api.services;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.Duration;

/**
 * Abstracción del almacenamiento de archivos (AWS S3 o GCP Cloud Storage).
 * La implementación concreta se selecciona via flag {@code storage.provider}.
 */
public interface IStorageService {
    String uploadFile(String prefix, MultipartFile file) throws IOException;

    void deleteFile(String fullUrl);

    InputStream downloadFile(String fullUrl);

    URL generatePresignedUrl(String fullUrl, Duration duration);
}
