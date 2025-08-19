package ai.freightfox.doc.storage.service.demo.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ApiSuccessResponse {
    private boolean success;
    private String message;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;

    public ApiSuccessResponse() {
        this.timestamp = LocalDateTime.now();
        this.success = true;
    }

    public ApiSuccessResponse(String message) {
        this();
        this.message = message;
    }
}