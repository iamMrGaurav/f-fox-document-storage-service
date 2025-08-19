package ai.freightfox.doc.storage.service.demo.controller;

import ai.freightfox.doc.storage.service.demo.dto.ApiSuccessResponse;
import ai.freightfox.doc.storage.service.demo.dto.FileMetadataResponse;
import ai.freightfox.doc.storage.service.demo.service.StorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/freight-fox/s3-bucket/")
public class StorageController {

    @Autowired
    private StorageService storageService;

    @GetMapping("/search")
    public ResponseEntity<List<FileMetadataResponse>> searchFiles(
            @RequestParam String userName, 
            @RequestParam(required = false) String searchTerm, 
            @RequestParam(defaultValue = "0") int page, 
            @RequestParam(defaultValue = "10") int size){

        List<FileMetadataResponse> files = storageService.searchFiles(userName, searchTerm, page, size);
        return ResponseEntity.ok(files);
    }


    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FileMetadataResponse> uploadFile(
            @RequestParam String userName, 
            @RequestParam("file") MultipartFile file){

        FileMetadataResponse response = storageService.uploadFile(userName, file);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/files")
    public ResponseEntity<List<FileMetadataResponse>> listUserFiles(
            @RequestParam String userName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        List<FileMetadataResponse> files = storageService.searchFiles(userName, null, page, size);
        return ResponseEntity.ok(files);
    }

    @DeleteMapping("/delete")
    public ResponseEntity<ApiSuccessResponse> deleteFile(
            @RequestParam String userName,
            @RequestParam String fileName) {
        
        storageService.deleteFile(userName, fileName);
        ApiSuccessResponse response = new ApiSuccessResponse("File deleted successfully: " + fileName);
        return ResponseEntity.ok(response);
    }

}
