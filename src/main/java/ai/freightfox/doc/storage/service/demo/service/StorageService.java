package ai.freightfox.doc.storage.service.demo.service;

import ai.freightfox.doc.storage.service.demo.dto.FileMetadataResponse;
import ai.freightfox.doc.storage.service.demo.globalExceptionHandler.BadRequestException;
import ai.freightfox.doc.storage.service.demo.globalExceptionHandler.FileSearchException;
import ai.freightfox.doc.storage.service.demo.globalExceptionHandler.FileUploadException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.time.Duration;

import java.util.List;

@Service
@Slf4j
public class StorageService {

    @Autowired
    private S3Client s3Client;

    @Value("${aws.s3.bucket.name}")
    private String s3BucketName;

    @Value("${app.download.url-expiry-seconds}")
    private long urlExpirySeconds;

    public List<FileMetadataResponse> searchFiles(String userName, String searchTerm, int page, int size){
        try {
            if (userName == null || userName.trim().isEmpty()) {
                throw new BadRequestException("Username cannot be null or empty");
            }
            
            String prefix = userName + "/";

            ListObjectsV2Request listObjectsV2Request = ListObjectsV2Request.builder()
                    .bucket(s3BucketName)
                    .prefix(prefix)
                    .maxKeys(1000)
                    .build();

            ListObjectsV2Response listObjectsResponse = s3Client.listObjectsV2(listObjectsV2Request);

            List<FileMetadataResponse> allFiles = listObjectsResponse.contents().stream()
                    .filter(obj -> !obj.key().endsWith("/"))
                    .filter(obj -> searchTerm == null || searchTerm.trim().isEmpty() ||
                            obj.key().toLowerCase().contains(searchTerm.toLowerCase()))
                    .map(this::mapToFileMetadata)
                    .toList();

            // Apply pagination
            int start = page * size;
            int end = Math.min(start + size, allFiles.size());
            
            if (start >= allFiles.size()) {
                return List.of();
            }
            
            return allFiles.subList(start, end).stream()
                    .map(this::addDownloadUrl)
                    .toList();
            
        } catch (Exception e) {
            log.error("Error searching files for user {} with term {}: {}", userName, searchTerm, e.getMessage());
            throw new FileSearchException("Failed to search files");
        }
    }

    private FileMetadataResponse mapToFileMetadata(S3Object s3Object) {
        String fileName = extractFileNameFromKey(s3Object.key());

        return FileMetadataResponse.builder()
                .fileName(fileName)
                .fileKey(s3Object.key())
                .fileSize(s3Object.size())
                .lastModified(s3Object.lastModified())
                .build();
    }

    public FileMetadataResponse uploadFile(String userName, MultipartFile file)  {
        try {
            if (userName == null || userName.trim().isEmpty()) {
                throw new BadRequestException("Username cannot be null or empty");
            }
            if (file == null || file.isEmpty()) {
                throw new BadRequestException("File cannot be null or empty");
            }

            String key = buildFileKey(userName, file.getOriginalFilename());
            
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(s3BucketName)
                    .key(key)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(file.getBytes()));
            
            log.info("File uploaded successfully: {}", key);
            
            FileMetadataResponse response = FileMetadataResponse.builder()
                    .fileName(file.getOriginalFilename())
                    .fileKey(key)
                    .fileSize(file.getSize())
                    .lastModified(java.time.Instant.now())
                    .build();
                    
            return addDownloadUrl(response);
                    
        } catch (Exception e) {
            log.error("Error uploading file for user {}: {}", userName, e.getMessage());
            throw new FileUploadException("Failed to upload file , Please Try Again !!");
        }
    }

    public String generateDownloadUrl(String fileKey) {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(s3BucketName)
                    .key(fileKey)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofSeconds(urlExpirySeconds))
                    .getObjectRequest(getObjectRequest)
                    .build();

            try (S3Presigner presigner = S3Presigner.create()) {
                PresignedGetObjectRequest presignedRequest = presigner.presignGetObject(presignRequest);
                return presignedRequest.url().toString();
            }
        } catch (Exception e) {
            log.error("Error generating download URL for file {}: {}", fileKey, e.getMessage());
            throw new RuntimeException("Failed to generate download URL", e);
        }
    }

    private FileMetadataResponse addDownloadUrl(FileMetadataResponse fileMetadata) {
        try {
            String downloadUrl = generateDownloadUrl(fileMetadata.getFileKey());
            return FileMetadataResponse.builder()
                    .fileName(fileMetadata.getFileName())
                    .fileKey(fileMetadata.getFileKey())
                    .fileSize(fileMetadata.getFileSize())
                    .lastModified(fileMetadata.getLastModified())
                    .downloadUrl(downloadUrl)
                    .build();
        } catch (Exception e) {
            log.warn("Could not generate download URL for file {}: {}", fileMetadata.getFileKey(), e.getMessage());
            return fileMetadata;
        }
    }

    public void deleteFile(String userName, String fileName) {
        try {
            if (userName == null || userName.trim().isEmpty()) {
                throw new BadRequestException("Username cannot be null or empty");
            }
            if (fileName == null || fileName.trim().isEmpty()) {
                throw new BadRequestException("Filename cannot be null or empty");
            }

            String key = buildFileKey(userName, fileName);

            if (!fileExists(key)) {
                throw new BadRequestException("File not found: " + fileName);
            }
            
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(s3BucketName)
                    .key(key)
                    .build();

            s3Client.deleteObject(deleteRequest);
            log.info("File deleted successfully: {}", key);

        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error deleting file {} for user {}: {}", fileName, userName, e.getMessage());
            throw new RuntimeException("Failed to delete file", e);
        }
    }

    public boolean fileExists(String fileKey) {
        try {
            HeadObjectRequest headRequest = HeadObjectRequest.builder()
                    .bucket(s3BucketName)
                    .key(fileKey)
                    .build();

            s3Client.headObject(headRequest);
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (Exception e) {
            log.error("Error checking file existence for key {}: {}", fileKey, e.getMessage());
            return false;
        }
    }

    private String extractFileNameFromKey(String key) {
        return key.substring(key.lastIndexOf("/") + 1);
    }

    private String buildFileKey(String userName, String fileName) {
        return userName + "/" + fileName;
    }

}
