package ai.freightfox.doc.storage.service.demo.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResponse {
    private boolean success;
    private String message;
    private List<FileMetadataResponse> files;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;

    public static SearchResponse getSearchResponse(List<FileMetadataResponse> files, String userName, String searchTerm) {
        String message = "";

        if(files.isEmpty()){
            message = "No Files Found for file name: " + searchTerm;
        }else{
            message = files.size() + " files found for user " + userName;
        }

        return SearchResponse.builder()
                .success(!files.isEmpty())
                .message(message)
                .files(files)
                .timestamp(LocalDateTime.now())
                .build();
    }
}