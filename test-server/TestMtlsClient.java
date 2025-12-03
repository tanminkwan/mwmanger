import javax.net.ssl.*;
import java.io.*;
import java.net.*;
import java.security.*;
import java.security.cert.CertificateException;

/**
 * Simple mTLS test client for OAuth2 token endpoint
 * Compile: javac TestMtlsClient.java
 * Run: java TestMtlsClient
 */
public class TestMtlsClient {

    private static final String SERVER_URL = "https://localhost:8443";
    private static final String KEYSTORE_PATH = "./certs/agent-test001.p12";
    private static final String KEYSTORE_PASSWORD = "agent-password";
    private static final String TRUSTSTORE_PATH = "./certs/truststore.jks";
    private static final String TRUSTSTORE_PASSWORD = "truststore-password";

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("mTLS Client Test for OAuth2 Server");
        System.out.println("========================================");
        System.out.println();

        try {
            // Test 1: Health check (no mTLS)
            System.out.println("[Test 1] Health Check...");
            testHealthCheck();

            // Test 2: Refresh token grant
            System.out.println("\n[Test 2] Refresh Token Grant...");
            testRefreshTokenGrant();

            // Test 3: mTLS client credentials grant
            System.out.println("\n[Test 3] mTLS Client Credentials Grant...");
            testMtlsClientCredentialsGrant();

            System.out.println("\n========================================");
            System.out.println("All tests completed!");
            System.out.println("========================================");

        } catch (Exception e) {
            System.err.println("Test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void testHealthCheck() throws Exception {
        SSLContext sslContext = createSSLContext(null, null, TRUSTSTORE_PATH, TRUSTSTORE_PASSWORD);
        HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
        HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);

        URL url = new URL(SERVER_URL + "/health");
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        int responseCode = conn.getResponseCode();
        String response = readResponse(conn);

        System.out.println("  Response Code: " + responseCode);
        System.out.println("  Response: " + response);

        if (responseCode == 200) {
            System.out.println("  [PASS] Health check successful");
        } else {
            System.out.println("  [FAIL] Health check failed");
        }
    }

    private static void testRefreshTokenGrant() throws Exception {
        SSLContext sslContext = createSSLContext(null, null, TRUSTSTORE_PATH, TRUSTSTORE_PASSWORD);
        HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
        HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);

        URL url = new URL(SERVER_URL + "/oauth2/token");
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

        String body = "grant_type=refresh_token&refresh_token=refresh-token-test001";
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes());
        }

        int responseCode = conn.getResponseCode();
        String response = readResponse(conn);

        System.out.println("  Response Code: " + responseCode);
        System.out.println("  Response: " + response.substring(0, Math.min(200, response.length())) + "...");

        if (responseCode == 200 && response.contains("access_token")) {
            System.out.println("  [PASS] Refresh token grant successful");
        } else {
            System.out.println("  [FAIL] Refresh token grant failed");
        }
    }

    private static void testMtlsClientCredentialsGrant() throws Exception {
        // Create SSL context with client certificate (mTLS)
        SSLContext sslContext = createSSLContext(
            KEYSTORE_PATH, KEYSTORE_PASSWORD,
            TRUSTSTORE_PATH, TRUSTSTORE_PASSWORD
        );

        HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
        HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);

        URL url = new URL(SERVER_URL + "/oauth2/token");
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

        String body = "grant_type=client_credentials";
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes());
        }

        int responseCode = conn.getResponseCode();
        String response;
        if (responseCode >= 400) {
            response = readErrorResponse(conn);
        } else {
            response = readResponse(conn);
        }

        System.out.println("  Response Code: " + responseCode);
        System.out.println("  Response: " + response.substring(0, Math.min(200, response.length())) + "...");

        if (responseCode == 200 && response.contains("access_token")) {
            System.out.println("  [PASS] mTLS client credentials grant successful");

            // Parse and show token claims
            if (response.contains("\"sub\"")) {
                System.out.println("  Token contains 'sub' claim as expected");
            }
        } else {
            System.out.println("  [FAIL] mTLS client credentials grant failed");
        }
    }

    private static SSLContext createSSLContext(
            String keystorePath, String keystorePassword,
            String truststorePath, String truststorePassword) throws Exception {

        KeyManager[] keyManagers = null;
        TrustManager[] trustManagers = null;

        // Load client keystore (for mTLS)
        if (keystorePath != null) {
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            try (FileInputStream fis = new FileInputStream(keystorePath)) {
                keyStore.load(fis, keystorePassword.toCharArray());
            }

            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(keyStore, keystorePassword.toCharArray());
            keyManagers = kmf.getKeyManagers();

            System.out.println("  Client keystore loaded: " + keystorePath);
        }

        // Load truststore
        if (truststorePath != null) {
            KeyStore trustStore = KeyStore.getInstance("JKS");
            try (FileInputStream fis = new FileInputStream(truststorePath)) {
                trustStore.load(fis, truststorePassword.toCharArray());
            }

            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(trustStore);
            trustManagers = tmf.getTrustManagers();

            System.out.println("  Truststore loaded: " + truststorePath);
        }

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagers, trustManagers, new SecureRandom());

        return sslContext;
    }

    private static String readResponse(HttpsURLConnection conn) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        }
    }

    private static String readErrorResponse(HttpsURLConnection conn) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream()))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        }
    }
}
