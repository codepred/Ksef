
package io.alapierre.ksef.client;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Value;

import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
@Getter
@NoArgsConstructor
public class ApiException extends Exception {

    private int code = 0;
    private Map<String, List<String>> responseHeaders = null;
    private String responseBody = null;

    private List<ExceptionDetail> exceptionDetails;

    public ApiException(Throwable throwable) {
        super(throwable);
    }

    public ApiException(String message) {
        super(message);
    }

    public ApiException(String message, Throwable throwable, int code, Map<String, List<String>> responseHeaders, String responseBody, List<ExceptionDetail> details) {
        super(message, throwable);
        this.code = code;
        this.responseHeaders = responseHeaders;
        this.responseBody = responseBody;
        this.exceptionDetails = details;
    }

    public ApiException(String message, Throwable throwable, int code, Map<String, List<String>> responseHeaders, String responseBody) {
        this(message, throwable, code, responseHeaders, responseBody, null);
    }

    public ApiException(String message, int code, Map<String, List<String>> responseHeaders, String responseBody) {
        this(message, null, code, responseHeaders, responseBody);
    }

    public ApiException(String message, Throwable throwable, int code, Map<String, List<String>> responseHeaders) {
        this(message, throwable, code, responseHeaders, null);
    }

    public ApiException(int code, Map<String, List<String>> responseHeaders, String responseBody) {
        this(null, null, code, responseHeaders, responseBody);
    }

    public ApiException(int code, String message) {
        super(message);
        this.code = code;
    }

    public ApiException(int code, String message, Map<String, List<String>> responseHeaders, String responseBody, List<ExceptionDetail> details) {
        this(code, message);
        this.responseHeaders = responseHeaders;
        this.responseBody = responseBody;
        this.exceptionDetails = details;
    }

    public ApiException(int code, String message, Map<String, List<String>> responseHeaders, String responseBody) {
        this(code, message);
        this.responseHeaders = responseHeaders;
        this.responseBody = responseBody;
    }

    public ApiException(String message, Throwable throwable) {
        super(message, throwable);
    }

    @Value
    @Builder
    public static class ExceptionDetail {
        int exceptionCode;
        String exceptionDescription;
    }

}
