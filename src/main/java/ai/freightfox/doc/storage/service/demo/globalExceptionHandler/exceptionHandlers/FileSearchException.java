package ai.freightfox.doc.storage.service.demo.globalExceptionHandler.exceptionHandlers;

public class FileSearchException extends RuntimeException {
    public FileSearchException(String message) {
        super(message);
    }
}
