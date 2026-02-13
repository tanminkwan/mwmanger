package mwagent.infrastructure.http;

/**
 * Exception thrown by HttpClient operations.
 */
public class HttpClientException extends Exception {

    private final int statusCode;

    public HttpClientException(String message) {
        super(message);
        this.statusCode = -1;
    }

    public HttpClientException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = -1;
    }

    public HttpClientException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public HttpClientException(String message, int statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    /**
     * Get the HTTP status code if available.
     * @return status code or -1 if not applicable
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * Check if this exception was caused by an HTTP error response.
     */
    public boolean isHttpError() {
        return statusCode > 0;
    }
}
