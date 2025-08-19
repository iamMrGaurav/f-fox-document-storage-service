package ai.freightfox.doc.storage.service.demo.globalExceptionHandler;

public class FileUploadException extends RuntimeException {
    public FileUploadException(String message) {
        super(message);
    }
}
