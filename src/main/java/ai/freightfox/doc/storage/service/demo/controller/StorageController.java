package ai.freightfox.doc.storage.service.demo.controller;

import ai.freightfox.doc.storage.service.demo.dto.ApiSuccessResponse;
import ai.freightfox.doc.storage.service.demo.dto.FileMetadataResponse;
import ai.freightfox.doc.storage.service.demo.dto.SearchResponse;
import ai.freightfox.doc.storage.service.demo.service.StorageService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

@RestController
@RequestMapping("/api/freight-fox/s3-bucket/")
@Validated
public class StorageController {

    @Autowired
    private StorageService storageService;

    @Operation(summary = "Search By File Name")
    @GetMapping("/search")
    public ResponseEntity<SearchResponse> searchFiles(
            @RequestParam @NotBlank(message = "Username is required") String userName, 
            @RequestParam(required = false) String searchTerm, 
            @RequestParam(defaultValue = "0") int page, 
            @RequestParam(defaultValue = "10") int size){

        List<FileMetadataResponse> files = storageService.searchFiles(userName, searchTerm, page, size);
        SearchResponse response = SearchResponse.getSearchResponse(files, userName, searchTerm);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Upload a File")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FileMetadataResponse> uploadFile(
            @RequestParam @NotBlank(message = "Username is required") String userName, 
            @RequestParam("file") MultipartFile file){

        FileMetadataResponse response = storageService.uploadFile(userName, file);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get All Files From User Storage")
    @GetMapping("/search/files")
    public ResponseEntity<SearchResponse> listUserFiles(
            @RequestParam @NotBlank(message = "Username is required") String userName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        List<FileMetadataResponse> files = storageService.searchFiles(userName, null, page, size);
        SearchResponse response = SearchResponse.getSearchResponse(files, userName, null);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Delete File By Name")
    @DeleteMapping("/delete")
    public ResponseEntity<ApiSuccessResponse> deleteFile(
            @RequestParam @NotBlank(message = "Username is required") String userName,
            @RequestParam @NotBlank(message = "Filename is required") String fileName) {
        
        storageService.deleteFile(userName, fileName);
        ApiSuccessResponse response = new ApiSuccessResponse("File deleted successfully: " + fileName);
        return ResponseEntity.ok(response);
    }

}
