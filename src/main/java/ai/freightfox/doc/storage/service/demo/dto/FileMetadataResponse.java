package ai.freightfox.doc.storage.service.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
public class FileMetadataResponse {
    private String fileName;
    private String fileKey;
    private Long fileSize;
    private Instant lastModified;
    private String downloadUrl;
}