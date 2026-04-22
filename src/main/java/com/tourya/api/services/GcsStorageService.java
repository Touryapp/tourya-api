package com.tourya.api.services;

import com.google.cloud.storage.Acl;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

@Service
@ConditionalOnProperty(name = "storage.provider", havingValue = "GCP")
public class GcsStorageService implements IStorageService {

    private static final Logger log = LoggerFactory.getLogger(GcsStorageService.class);

    private final Storage storage;
    private final String bucket;
    private final String urlPrefix;

    public GcsStorageService(@Value("${gcp.project-id}") String projectId,
                             @Value("${gcp.storage.bucket}") String bucket) {
        this.bucket = bucket;
        this.urlPrefix = "https://storage.googleapis.com/" + bucket + "/";
        this.storage = StorageOptions.newBuilder().setProjectId(projectId).build().getService();
    }

    @Override
    public String uploadFile(String prefix, MultipartFile file) throws IOException {
        String key = prefix + "/" + Instant.now().toEpochMilli() + "_" + file.getOriginalFilename();

        BlobInfo blobInfo = BlobInfo.newBuilder(BlobId.of(bucket, key))
                .setContentType(file.getContentType())
                .setAcl(Collections.singletonList(Acl.of(Acl.User.ofAllUsers(), Acl.Role.READER)))
                .build();

        try {
            storage.create(blobInfo, file.getBytes());
        } catch (Exception ex) {
            log.warn("Failed to set ACL on blob (bucket may use uniform access): {}", ex.getMessage());
            // Retry without ACL for uniform access buckets
            BlobInfo fallback = BlobInfo.newBuilder(BlobId.of(bucket, key))
                    .setContentType(file.getContentType())
                    .build();
            storage.create(fallback, file.getBytes());
        }

        return urlPrefix + key;
    }

    @Override
    public void deleteFile(String fullUrl) {
        String key = fullUrl.replace(urlPrefix, "");
        storage.delete(BlobId.of(bucket, key));
    }

    @Override
    public InputStream downloadFile(String fullUrl) {
        String key = fullUrl.replace(urlPrefix, "");
        Blob blob = storage.get(BlobId.of(bucket, key));
        if (blob == null) {
            return new ByteArrayInputStream(new byte[0]);
        }
        return new ByteArrayInputStream(blob.getContent());
    }

    @Override
    public URL generatePresignedUrl(String fullUrl, Duration duration) {
        String key = fullUrl.replace(urlPrefix, "");
        try {
            BlobInfo blobInfo = BlobInfo.newBuilder(BlobId.of(bucket, key)).build();
            return storage.signUrl(blobInfo, duration.getSeconds(), TimeUnit.SECONDS,
                    Storage.SignUrlOption.withV4Signature());
        } catch (Exception ex) {
            log.warn("Failed to generate signed URL, returning public URL: {}", ex.getMessage());
            try {
                return URI.create(fullUrl).toURL();
            } catch (MalformedURLException e) {
                throw new RuntimeException("Invalid URL: " + fullUrl, e);
            }
        }
    }
}
