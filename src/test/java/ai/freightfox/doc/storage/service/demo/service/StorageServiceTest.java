package ai.freightfox.doc.storage.service.demo.service;

import ai.freightfox.doc.storage.service.demo.dto.response.FileMetadataResponse;
import ai.freightfox.doc.storage.service.demo.globalExceptionHandler.exceptionHandlers.BadRequestException;
import ai.freightfox.doc.storage.service.demo.globalExceptionHandler.exceptionHandlers.FileSearchException;
import ai.freightfox.doc.storage.service.demo.globalExceptionHandler.exceptionHandlers.FileUploadException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StorageServiceTest {

    @Mock
    private S3Client s3Client;

    @InjectMocks
    private StorageService storageService;

    private final String bucketName = "test-bucket";
    private final long urlExpirySeconds = 900L;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(storageService, "s3BucketName", bucketName);
        ReflectionTestUtils.setField(storageService, "urlExpirySeconds", urlExpirySeconds);
    }

    // Test successful file search with valid user and search term
    @Test
    void searchFiles_WithValidUserName_ReturnsFileList() {
        String userName = "testUser";
        String searchTerm = "document";
        int page = 0;
        int size = 10;

        S3Object s3Object1 = S3Object.builder()
                .key("testUser/document1.pdf")
                .size(1024L)
                .lastModified(Instant.now())
                .build();

        S3Object s3Object2 = S3Object.builder()
                .key("testUser/document2.pdf")
                .size(2048L)
                .lastModified(Instant.now())
                .build();

        ListObjectsV2Response mockResponse = ListObjectsV2Response.builder()
                .contents(Arrays.asList(s3Object1, s3Object2))
                .build();

        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class)))
                .thenReturn(mockResponse);

        List<FileMetadataResponse> result = storageService.searchFiles(userName, searchTerm, page, size);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("document1.pdf", result.get(0).getFileName());
        assertEquals("document2.pdf", result.get(1).getFileName());
        assertEquals("testUser/document1.pdf", result.get(0).getFileKey());
        assertEquals("testUser/document2.pdf", result.get(1).getFileKey());

        verify(s3Client, times(1)).listObjectsV2(any(ListObjectsV2Request.class));
    }

    // Test file search validation with empty username
    @Test
    void searchFiles_WithEmptyUserName_ThrowsFileSearchException() {
        String userName = "";
        String searchTerm = "document";
        int page = 0;
        int size = 10;

        FileSearchException exception = assertThrows(FileSearchException.class, 
            () -> storageService.searchFiles(userName, searchTerm, page, size));
        
        assertEquals("Failed to search files", exception.getMessage());
        verify(s3Client, never()).listObjectsV2(any(ListObjectsV2Request.class));
    }

    // Test file search validation with null username
    @Test
    void searchFiles_WithNullUserName_ThrowsFileSearchException() {
        String userName = null;
        String searchTerm = "document";
        int page = 0;
        int size = 10;

        FileSearchException exception = assertThrows(FileSearchException.class, 
            () -> storageService.searchFiles(userName, searchTerm, page, size));
        
        assertEquals("Failed to search files", exception.getMessage());
        verify(s3Client, never()).listObjectsV2(any(ListObjectsV2Request.class));
    }

    // Test file search error handling with S3 exception
    @Test
    void searchFiles_WithS3Exception_ThrowsFileSearchException() {
        String userName = "testUser";
        String searchTerm = "document";
        int page = 0;
        int size = 10;

        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class)))
                .thenThrow(S3Exception.builder().message("S3 Error").build());

        FileSearchException exception = assertThrows(FileSearchException.class, 
            () -> storageService.searchFiles(userName, searchTerm, page, size));
        
        assertEquals("Failed to search files", exception.getMessage());
        verify(s3Client, times(1)).listObjectsV2(any(ListObjectsV2Request.class));
    }

    // Test pagination functionality returns correct page of results
    @Test
    void searchFiles_WithPagination_ReturnsCorrectPage() {
        String userName = "testUser";
        String searchTerm = null;
        int page = 1;
        int size = 2;

        List<S3Object> s3Objects = Arrays.asList(
            S3Object.builder().key("testUser/file1.pdf").size(1024L).lastModified(Instant.now()).build(),
            S3Object.builder().key("testUser/file2.pdf").size(1024L).lastModified(Instant.now()).build(),
            S3Object.builder().key("testUser/file3.pdf").size(1024L).lastModified(Instant.now()).build(),
            S3Object.builder().key("testUser/file4.pdf").size(1024L).lastModified(Instant.now()).build(),
            S3Object.builder().key("testUser/file5.pdf").size(1024L).lastModified(Instant.now()).build()
        );

        ListObjectsV2Response mockResponse = ListObjectsV2Response.builder()
                .contents(s3Objects)
                .build();

        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class)))
                .thenReturn(mockResponse);

        List<FileMetadataResponse> result = storageService.searchFiles(userName, searchTerm, page, size);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("file3.pdf", result.get(0).getFileName());
        assertEquals("file4.pdf", result.get(1).getFileName());
    }

    // Test search term filtering with case insensitive matching
    @Test
    void searchFiles_WithSearchTerm_FiltersCorrectly() {
        String userName = "testUser";
        String searchTerm = "invoice";
        int page = 0;
        int size = 10;

        List<S3Object> s3Objects = Arrays.asList(
            S3Object.builder().key("testUser/invoice1.pdf").size(1024L).lastModified(Instant.now()).build(),
            S3Object.builder().key("testUser/contract.pdf").size(1024L).lastModified(Instant.now()).build(),
            S3Object.builder().key("testUser/Invoice_Final.pdf").size(1024L).lastModified(Instant.now()).build()
        );

        ListObjectsV2Response mockResponse = ListObjectsV2Response.builder()
                .contents(s3Objects)
                .build();

        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class)))
                .thenReturn(mockResponse);

        List<FileMetadataResponse> result = storageService.searchFiles(userName, searchTerm, page, size);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("invoice1.pdf", result.get(0).getFileName());
        assertEquals("Invoice_Final.pdf", result.get(1).getFileName());
    }

    // Test successful file upload with valid parameters
    @Test
    void uploadFile_WithValidFile_ReturnsFileMetadata() throws Exception {
        String userName = "testUser";
        byte[] content = "test file content".getBytes();
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.pdf", "application/pdf", content);

        PutObjectResponse mockResponse = PutObjectResponse.builder().build();
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(mockResponse);

        FileMetadataResponse result = storageService.uploadFile(userName, file);

        assertNotNull(result);
        assertEquals("test.pdf", result.getFileName());
        assertEquals("testUser/test.pdf", result.getFileKey());
        assertEquals(content.length, result.getFileSize());
        assertNotNull(result.getLastModified());

        verify(s3Client, times(1)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    // Test file upload validation with empty username
    @Test
    void uploadFile_WithEmptyUserName_ThrowsFileUploadException() {
        String userName = "";
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.pdf", "application/pdf", "test content".getBytes());

        FileUploadException exception = assertThrows(FileUploadException.class, 
            () -> storageService.uploadFile(userName, file));
        
        assertEquals("Failed to upload file , Please Try Again !!", exception.getMessage());
        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    // Test file upload validation with empty file
    @Test
    void uploadFile_WithEmptyFile_ThrowsFileUploadException() {
        String userName = "testUser";
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.pdf", "application/pdf", new byte[0]);

        FileUploadException exception = assertThrows(FileUploadException.class, 
            () -> storageService.uploadFile(userName, file));
        
        assertEquals("Failed to upload file , Please Try Again !!", exception.getMessage());
        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    // Test file upload error handling with S3 exception
    @Test
    void uploadFile_WithS3Exception_ThrowsFileUploadException() throws Exception {
        String userName = "testUser";
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.pdf", "application/pdf", "test content".getBytes());

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(S3Exception.builder().message("Upload failed").build());

        FileUploadException exception = assertThrows(FileUploadException.class, 
            () -> storageService.uploadFile(userName, file));
        
        assertEquals("Failed to upload file , Please Try Again !!", exception.getMessage());
        verify(s3Client, times(1)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    // Test successful file deletion with valid parameters
    @Test
    void deleteFile_WithValidParameters_DeletesSuccessfully() {
        String userName = "testUser";
        String fileName = "test.pdf";

        HeadObjectResponse mockHeadResponse = HeadObjectResponse.builder().build();
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenReturn(mockHeadResponse);

        DeleteObjectResponse mockDeleteResponse = DeleteObjectResponse.builder().build();
        when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                .thenReturn(mockDeleteResponse);

        assertDoesNotThrow(() -> storageService.deleteFile(userName, fileName));

        verify(s3Client, times(1)).headObject(any(HeadObjectRequest.class));
        verify(s3Client, times(1)).deleteObject(any(DeleteObjectRequest.class));
    }

    // Test file deletion validation with empty username
    @Test
    void deleteFile_WithEmptyUserName_ThrowsBadRequestException() {
        String userName = "";
        String fileName = "test.pdf";

        BadRequestException exception = assertThrows(BadRequestException.class, 
            () -> storageService.deleteFile(userName, fileName));
        
        assertEquals("Username cannot be null or empty", exception.getMessage());
        verify(s3Client, never()).deleteObject(any(DeleteObjectRequest.class));
    }

    // Test file deletion validation with empty filename
    @Test
    void deleteFile_WithEmptyFileName_ThrowsBadRequestException() {
        String userName = "testUser";
        String fileName = "";

        BadRequestException exception = assertThrows(BadRequestException.class, 
            () -> storageService.deleteFile(userName, fileName));
        
        assertEquals("Filename cannot be null or empty", exception.getMessage());
        verify(s3Client, never()).deleteObject(any(DeleteObjectRequest.class));
    }

    // Test file deletion with non-existent file
    @Test
    void deleteFile_WithNonExistentFile_ThrowsBadRequestException() {
        String userName = "testUser";
        String fileName = "nonexistent.pdf";

        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenThrow(NoSuchKeyException.builder().build());

        BadRequestException exception = assertThrows(BadRequestException.class, 
            () -> storageService.deleteFile(userName, fileName));
        
        assertEquals("File not found: nonexistent.pdf", exception.getMessage());
        verify(s3Client, times(1)).headObject(any(HeadObjectRequest.class));
        verify(s3Client, never()).deleteObject(any(DeleteObjectRequest.class));
    }

    // Test file existence check returns true for existing file
    @Test
    void fileExists_WithExistingFile_ReturnsTrue() {
        String fileKey = "testUser/test.pdf";
        HeadObjectResponse mockResponse = HeadObjectResponse.builder().build();
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenReturn(mockResponse);

        boolean result = storageService.fileExists(fileKey);

        assertTrue(result);
        verify(s3Client, times(1)).headObject(any(HeadObjectRequest.class));
    }

    // Test file existence check returns false for non-existent file
    @Test
    void fileExists_WithNonExistentFile_ReturnsFalse() {
        String fileKey = "testUser/nonexistent.pdf";
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenThrow(NoSuchKeyException.builder().build());

        boolean result = storageService.fileExists(fileKey);

        assertFalse(result);
        verify(s3Client, times(1)).headObject(any(HeadObjectRequest.class));
    }

    // Test file existence check handles S3 exception gracefully
    @Test
    void fileExists_WithS3Exception_ReturnsFalse() {
        String fileKey = "testUser/test.pdf";
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenThrow(S3Exception.builder().message("S3 Error").build());

        boolean result = storageService.fileExists(fileKey);

        assertFalse(result);
        verify(s3Client, times(1)).headObject(any(HeadObjectRequest.class));
    }

    // Test download URL generation with basic validation
    @Test
    void generateDownloadUrl_WithValidFileKey_ReturnsUrl() {
        String fileKey = "testUser/test.pdf";
        
        assertDoesNotThrow(() -> {
            try {
                storageService.generateDownloadUrl(fileKey);
            } catch (RuntimeException e) {
                assertTrue(e.getMessage().contains("Failed to generate download URL"));
            }
        });
    }
}