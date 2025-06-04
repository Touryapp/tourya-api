// S3Service.java
package com.tourya.api.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;

@Service
public class S3Service {

    private final S3Client s3Client;
    private final String bucket;

    public S3Service(@Value("${aws.s3.region}") String region,
                     @Value("${aws.s3.bucket}") String bucket,
                     @Value("${aws.accessKey}") String accessKey,
                     @Value("${aws.secretKey}") String secretKey) {
        this.bucket = bucket;
        AwsBasicCredentials awsCreds = AwsBasicCredentials.create(accessKey, secretKey);
        this.s3Client = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
                .build();
    }

    public String uploadFile(String prefix, MultipartFile file) throws IOException {
        String key = prefix + "/" + Instant.now().toEpochMilli() + "_" + file.getOriginalFilename();

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(file.getContentType())
                .build();

        s3Client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        return "https://" + bucket + ".s3.amazonaws.com/" + key;
    }

    public void deleteFile(String fullUrl) {
        String key = fullUrl.replace("https://" + bucket + ".s3.amazonaws.com/", "");
        DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();
        s3Client.deleteObject(deleteRequest);
    }

    public InputStream downloadFile(String fullUrl) {
        String key = fullUrl.replace("https://" + bucket + ".s3.amazonaws.com/", "");
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();
        return s3Client.getObject(getObjectRequest);
    }

    public URL generatePresignedUrl(String fullUrl, Duration duration) {
        String key = fullUrl.replace("https://" + bucket + ".s3.amazonaws.com/", "");
        return s3Client.utilities().getUrl(builder -> builder.bucket(bucket).key(key));
    }
}
