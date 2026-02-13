package mwagent.infrastructure.http;

import java.util.Map;

/**
 * HTTP client interface for dependency injection.
 * Abstracts HTTP communication to enable testing and flexibility.
 */
public interface HttpClient {

    /**
     * HTTP response wrapper.
     */
    class HttpResponse {
        private final int statusCode;
        private final String body;
        private final Map<String, String> headers;

        public HttpResponse(int statusCode, String body, Map<String, String> headers) {
            this.statusCode = statusCode;
            this.body = body;
            this.headers = headers;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public String getBody() {
            return body;
        }

        public Map<String, String> getHeaders() {
            return headers;
        }

        public boolean isSuccess() {
            return statusCode >= 200 && statusCode < 300;
        }
    }

    /**
     * Perform an HTTP GET request.
     *
     * @param url full URL to request
     * @param headers request headers
     * @return HTTP response
     * @throws HttpClientException if request fails
     */
    HttpResponse get(String url, Map<String, String> headers) throws HttpClientException;

    /**
     * Perform an HTTP POST request.
     *
     * @param url full URL to request
     * @param headers request headers
     * @param body request body
     * @return HTTP response
     * @throws HttpClientException if request fails
     */
    HttpResponse post(String url, Map<String, String> headers, String body) throws HttpClientException;

    /**
     * Perform an HTTP POST request with form data.
     *
     * @param url full URL to request
     * @param headers request headers
     * @param formData form data as key-value pairs
     * @return HTTP response
     * @throws HttpClientException if request fails
     */
    HttpResponse postForm(String url, Map<String, String> headers, Map<String, String> formData) throws HttpClientException;

    /**
     * Download a file from URL.
     *
     * @param url full URL to download from
     * @param headers request headers
     * @param destinationPath local path to save file
     * @return downloaded file name
     * @throws HttpClientException if download fails
     */
    String downloadFile(String url, Map<String, String> headers, String destinationPath) throws HttpClientException;

    /**
     * Close the client and release resources.
     */
    void close();
}
