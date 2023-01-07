package se.magnus.util.http;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.time.ZonedDateTime;

@Getter
public class HttpErrorInfo {
    private final ZonedDateTime timestamp;
    private final String path;
    private final HttpStatus httpStatus;
    private final String message;

    public HttpErrorInfo() {
        this.timestamp = null;
        this.path = null;
        this.httpStatus = null;
        this.message = null;
    }

    public HttpErrorInfo(HttpStatus httpStatus, String path, String message) {
        this.path = path;
        this.httpStatus = httpStatus;
        this.message = message;
        timestamp = ZonedDateTime.now();
    }
}
