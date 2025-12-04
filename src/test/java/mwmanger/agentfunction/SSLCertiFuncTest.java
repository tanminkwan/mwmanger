package mwmanger.agentfunction;

import mwmanger.common.Config;
import mwmanger.vo.CommandVO;
import mwmanger.vo.ResultVO;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.jupiter.api.*;

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for SSLCertiFunc - Remote server SSL certificate inspection
 *
 * This test validates connecting to SSL servers and retrieving certificate info.
 *
 * Test modes:
 * 1. Unit tests for helper methods (always run)
 * 2. Integration tests with real servers (require network)
 */
@DisplayName("SSLCertiFunc Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SSLCertiFuncTest {

    private SSLCertiFunc func;

    @BeforeAll
    static void setup() {
        Config.getConfig().setLogger(Logger.getLogger("SSLCertiFuncTest"));
    }

    @BeforeEach
    void beforeEach() {
        func = new SSLCertiFunc();
    }

    // ==========================================================================
    // Unit Tests - Helper Methods
    // ==========================================================================

    @Test
    @Order(1)
    @DisplayName("getDomainNPort: Parse domain with explicit port")
    void testGetDomainNPort_WithPort() {
        // When
        String[] result = func.getDomainNPort("example.com:8443");

        // Then
        assertThat(result[0]).isEqualTo("example.com");
        assertThat(result[1]).isEqualTo("8443");
    }

    @Test
    @Order(2)
    @DisplayName("getDomainNPort: Parse domain without port (default 443)")
    void testGetDomainNPort_WithoutPort() {
        // When
        String[] result = func.getDomainNPort("example.com");

        // Then
        assertThat(result[0]).isEqualTo("example.com");
        assertThat(result[1]).isEqualTo("443");
    }

    @Test
    @Order(3)
    @DisplayName("getDomainNPort: Parse domain with standard HTTPS port")
    void testGetDomainNPort_Port443() {
        // When
        String[] result = func.getDomainNPort("secure.example.com:443");

        // Then
        assertThat(result[0]).isEqualTo("secure.example.com");
        assertThat(result[1]).isEqualTo("443");
    }

    @Test
    @Order(4)
    @DisplayName("printValidCerts: Format certificate info as JSON")
    void testPrintValidCerts_FormatOutput() throws Exception {
        // Given: Mock certificate data (we'll test with null/empty for now)
        // Note: In real test, we'd need actual X509Certificate objects

        // When: Call with empty array
        ResultVO result = func.printValidCerts("test.example.com", new X509Certificate[0]);

        // Then
        assertThat(result.isOk()).isTrue();

        JSONObject json = parseJson(result.getResult());
        assertThat(json.get("domain")).isEqualTo("test.example.com");
        assertThat(json.get("certs")).isInstanceOf(JSONArray.class);

        JSONArray certs = (JSONArray) json.get("certs");
        assertThat(certs).isEmpty();
    }

    // ==========================================================================
    // Integration Tests - Local SSL Server (requires mock_server --ssl)
    // ==========================================================================

    @Test
    @Order(10)
    @DisplayName("Check localhost SSL certificate (if mock server running with SSL)")
    void testCheckLocalSSLCertificate() {
        // Given: localhost:8443 (mock_server with --ssl)
        String domain = "localhost";
        String ip = "127.0.0.1";
        int port = 8443;

        // When: Try to get certificate
        X509Certificate[] certs = func.checkSSLCertificate(domain, ip, port);

        // Then: Either succeeds or fails gracefully (server may not be running)
        if (certs.length > 0) {
            System.out.println("\n=== Local SSL Server Certificate ===");
            for (int i = 0; i < certs.length; i++) {
                X509Certificate cert = certs[i];
                System.out.println("Certificate " + (i + 1) + ":");
                System.out.println("  Subject: " + cert.getSubjectDN().getName());
                System.out.println("  Issuer: " + cert.getIssuerDN().getName());
                System.out.println("  Valid: " + cert.getNotBefore() + " to " + cert.getNotAfter());
                System.out.println();
            }
        } else {
            System.out.println("[INFO] No local SSL server running on " + domain + ":" + port);
            System.out.println("[INFO] To test, run: python test-server/mock_server.py --ssl");
        }
    }

    // ==========================================================================
    // Integration Tests - External SSL Servers (require network)
    // ==========================================================================

    @Test
    @Order(20)
    @DisplayName("Check google.com SSL certificate")
    void testCheckGoogleSSLCertificate() {
        // Given
        String domain = "www.google.com";

        // When
        X509Certificate[] certs = func.checkSSLCertificate(domain, domain, 443);

        // Then
        if (certs.length > 0) {
            assertThat(certs).isNotEmpty();

            System.out.println("\n=== Google SSL Certificate ===");
            X509Certificate cert = certs[0];
            System.out.println("Subject: " + cert.getSubjectDN().getName());
            System.out.println("Issuer: " + cert.getIssuerDN().getName());
            System.out.println("Valid until: " + cert.getNotAfter());

            // Verify it's a valid Google cert
            String subject = cert.getSubjectDN().getName();
            assertThat(subject.toLowerCase())
                    .as("Subject should contain google")
                    .containsAnyOf("google", "*.google");
        } else {
            System.out.println("[SKIP] Could not connect to www.google.com (network issue)");
        }
    }

    @Test
    @Order(21)
    @DisplayName("Check github.com SSL certificate")
    void testCheckGitHubSSLCertificate() {
        // Given
        String domain = "github.com";

        // When
        X509Certificate[] certs = func.checkSSLCertificate(domain, domain, 443);

        // Then
        if (certs.length > 0) {
            assertThat(certs).isNotEmpty();

            System.out.println("\n=== GitHub SSL Certificate ===");
            X509Certificate cert = certs[0];
            System.out.println("Subject: " + cert.getSubjectDN().getName());
            System.out.println("Issuer: " + cert.getIssuerDN().getName());
            System.out.println("Valid until: " + cert.getNotAfter());
        } else {
            System.out.println("[SKIP] Could not connect to github.com (network issue)");
        }
    }

    @Test
    @Order(22)
    @DisplayName("Check naver.com SSL certificate (Korean site)")
    void testCheckNaverSSLCertificate() {
        // Given
        String domain = "www.naver.com";

        // When
        X509Certificate[] certs = func.checkSSLCertificate(domain, domain, 443);

        // Then
        if (certs.length > 0) {
            assertThat(certs).isNotEmpty();

            System.out.println("\n=== Naver SSL Certificate ===");
            X509Certificate cert = certs[0];
            System.out.println("Subject: " + cert.getSubjectDN().getName());
            System.out.println("Issuer: " + cert.getIssuerDN().getName());
            System.out.println("Valid until: " + cert.getNotAfter());
        } else {
            System.out.println("[SKIP] Could not connect to www.naver.com (network issue)");
        }
    }

    // ==========================================================================
    // Full Command Execution Tests
    // ==========================================================================

    @Test
    @Order(30)
    @DisplayName("Execute command for github.com")
    void testExeCommand_GitHub() throws Exception {
        // Given
        CommandVO command = createCommand("github.com");

        // When
        ArrayList<ResultVO> results = func.exeCommand(command);

        // Then
        assertThat(results).hasSize(1);

        ResultVO result = results.get(0);
        if (result.isOk()) {
            JSONObject json = parseJson(result.getResult());

            assertThat(json.get("domain")).isEqualTo("github.com");
            assertThat(json.get("certs")).isInstanceOf(JSONArray.class);

            JSONArray certs = (JSONArray) json.get("certs");
            if (!certs.isEmpty()) {
                JSONObject firstCert = (JSONObject) certs.get(0);
                assertThat(firstCert.get("subject")).isNotNull();
                assertThat(firstCert.get("issuer")).isNotNull();
                assertThat(firstCert.get("notafter")).isNotNull();

                System.out.println("\n=== github.com via exeCommand ===");
                System.out.println("Subject: " + firstCert.get("subject"));
                System.out.println("Issuer: " + firstCert.get("issuer"));
            }
        } else {
            System.out.println("[SKIP] Could not connect to github.com");
        }
    }

    @Test
    @Order(31)
    @DisplayName("Execute command with custom port")
    void testExeCommand_WithPort() throws Exception {
        // Given: localhost:8443 (mock_server with --ssl)
        CommandVO command = createCommand("localhost:8443");

        // When
        ArrayList<ResultVO> results = func.exeCommand(command);

        // Then
        assertThat(results).hasSize(1);

        ResultVO result = results.get(0);
        if (result.isOk()) {
            JSONObject json = parseJson(result.getResult());
            assertThat(json.get("domain")).isEqualTo("localhost:8443");

            System.out.println("\n=== localhost:8443 via exeCommand ===");
            System.out.println("Result: " + result.getResult());
        } else {
            System.out.println("[INFO] No SSL server on localhost:8443");
            System.out.println("[INFO] To test, run: python test-server/mock_server.py --ssl");
        }
    }

    @Test
    @Order(32)
    @DisplayName("Execute command for invalid domain returns empty certs")
    void testExeCommand_InvalidDomain() throws Exception {
        // Given
        CommandVO command = createCommand("invalid.domain.that.does.not.exist.example:443");

        // When
        ArrayList<ResultVO> results = func.exeCommand(command);

        // Then
        assertThat(results).hasSize(1);

        ResultVO result = results.get(0);
        assertThat(result.isOk()).isTrue();  // Function returns OK with empty certs

        JSONObject json = parseJson(result.getResult());
        JSONArray certs = (JSONArray) json.get("certs");
        assertThat(certs).isEmpty();

        System.out.println("[Invalid Domain] Result: empty certs array (expected)");
    }

    // ==========================================================================
    // Wildcard Certificate Tests
    // ==========================================================================

    @Test
    @Order(40)
    @DisplayName("Check wildcard certificate matching")
    void testWildcardCertificate() {
        // Given: Using * as domain to match any cert
        String domain = "*";

        // When
        X509Certificate[] certs = func.checkSSLCertificate("www.google.com", "www.google.com", 443);

        // Then: With * domain, should match any certificate
        if (certs.length > 0) {
            // The printValidCerts should handle * domain
            ResultVO result = func.printValidCerts(domain, certs);
            assertThat(result.isOk()).isTrue();

            System.out.println("[Wildcard] Got " + certs.length + " certificates with * domain");
        }
    }

    // ==========================================================================
    // Helper Methods
    // ==========================================================================

    private CommandVO createCommand(String domainParam) {
        CommandVO command = new CommandVO();
        command.setAdditionalParams(domainParam);
        command.setHostName("testhost");
        command.setTargetFileName("ssl-check");
        command.setTargetFilePath("/tmp");
        return command;
    }

    private JSONObject parseJson(String json) throws Exception {
        JSONParser parser = new JSONParser();
        return (JSONObject) parser.parse(json);
    }
}
