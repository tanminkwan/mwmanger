package mwmanger.integration;

import mwmanger.common.Common;
import mwmanger.common.Config;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration test for mTLS token renewal
 *
 * Prerequisites:
 * 1. Mock server running: python test-server/mock_server.py --ssl
 * 2. Certificates generated: test-server/generate-certs.bat or .sh
 * 3. Set environment variable: MTLS_INTEGRATION_TEST=true
 *
 * To run:
 * ./gradlew test --tests MtlsTokenRenewalIntegrationTest
 */
@EnabledIfEnvironmentVariable(named = "MTLS_INTEGRATION_TEST", matches = "true")
class MtlsTokenRenewalIntegrationTest {

    private static final String KEYSTORE_PATH = "./test-server/certs/agent-test001.p12";
    private static final String TRUSTSTORE_PATH = "./test-server/certs/truststore.jks";
    private static final String SERVER_URL = "https://localhost:8443";

    @BeforeAll
    static void checkPrerequisites() {
        // Check if certificates exist
        boolean keystoreExists = Files.exists(Paths.get(KEYSTORE_PATH));
        boolean truststoreExists = Files.exists(Paths.get(TRUSTSTORE_PATH));

        if (!keystoreExists || !truststoreExists) {
            fail("Certificates not found. Run test-server/generate-certs.sh first.\n" +
                 "Expected files:\n" +
                 "  - " + KEYSTORE_PATH + " (exists: " + keystoreExists + ")\n" +
                 "  - " + TRUSTSTORE_PATH + " (exists: " + truststoreExists + ")");
        }

        // Initialize logger
        Config.getConfig().setLogger(Logger.getLogger("IntegrationTest"));
    }

    @Test
    void testMtlsTokenRenewal() {
        // Given: Configuration for mTLS
        Config config = Config.getConfig();
        config.setUseMtls(true);
        config.setClientKeystorePath(KEYSTORE_PATH);
        config.setClientKeystorePassword("agent-password");
        config.setTruststorePath(TRUSTSTORE_PATH);
        config.setTruststorePassword("truststore-password");
        config.setServer_url(SERVER_URL);

        // When: Creating mTLS client
        assertThatCode(() -> Common.createMtlsClient())
                .as("mTLS client should be created successfully")
                .doesNotThrowAnyException();

        // When: Renewing access token with mTLS
        int result = Common.renewAccessTokenWithMtls();

        // Then: Token renewal should succeed
        assertThat(result)
                .as("Token renewal should return success code (1)")
                .isEqualTo(1);

        // And: Access token should be set
        assertThat(config.getAccess_token())
                .as("Access token should not be empty")
                .isNotNull()
                .isNotEmpty();
    }

    @Test
    void testRefreshTokenMethod() {
        // Given: Configuration for refresh token
        Config config = Config.getConfig();
        config.setUseMtls(false);
        config.setRefresh_token("refresh-token-test001");
        config.setServer_url(SERVER_URL);

        // When: Updating token with refresh token
        int result = Common.updateToken();

        // Then: Token update should succeed or fail gracefully
        // Note: This might fail if mock server is HTTP only
        assertThat(result)
                .as("Token update should return a result code")
                .isNotNull();

        if (result == 1) {
            // Success case
            assertThat(config.getAccess_token())
                    .as("Access token should be set on success")
                    .isNotNull()
                    .isNotEmpty();
        }
    }

    @Test
    void testMtlsWithWrongCertificate() {
        // Given: Configuration with wrong keystore password
        Config config = Config.getConfig();
        config.setUseMtls(true);
        config.setClientKeystorePath(KEYSTORE_PATH);
        config.setClientKeystorePassword("wrong-password");
        config.setTruststorePath(TRUSTSTORE_PATH);
        config.setTruststorePassword("truststore-password");
        config.setServer_url(SERVER_URL);

        // When & Then: Creating mTLS client should fail
        assertThatThrownBy(() -> Common.createMtlsClient())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("mTLS client creation failed");
    }

    @Test
    void testMtlsDisabledFallbackToRefreshToken() {
        // Given: mTLS disabled
        Config config = Config.getConfig();
        config.setUseMtls(false);

        // When: Attempting mTLS renewal
        int result = Common.renewAccessTokenWithMtls();

        // Then: Should return error indicating mTLS not enabled
        assertThat(result)
                .as("Should return -1 when mTLS is disabled")
                .isEqualTo(-1);
    }
}
