package mwmanger;

import static mwmanger.common.Config.getConfig;

import java.util.logging.Level;
import java.util.logging.Logger;

import mwmanger.lifecycle.AgentLifecycleManager;

/**
 * MwAgent - Remote Management Agent
 *
 * Phase 1 Refactoring: Lifecycle Management
 * - 기존: PreWork → FirstWork → MainWork (절차적)
 * - 개선: AgentLifecycleManager (생명주기 기반)
 *
 * 아키텍처:
 * - AgentLifecycleManager: 전체 생명주기 관리
 * - BootstrapService: 등록 및 승인
 * - KafkaService: Kafka 연결 관리
 * - CommandExecutorService: 명령 실행 관리
 * - GracefulShutdownHandler: 정상 종료 처리
 */
public class MwAgent {

    private static final Logger logger = getConfig().getLogger();

    public static void main(String[] args) {

        try {
            // Initialize configuration
            getConfig().setConfig();

            // Create lifecycle manager
            AgentLifecycleManager lifecycleManager = new AgentLifecycleManager();

            // Register shutdown hook for graceful termination
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Shutdown signal received");
                try {
                    lifecycleManager.stop();
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Error during shutdown", e);
                }
            }, "ShutdownHook"));

            // Start agent
            lifecycleManager.start();

            // Wait for termination
            lifecycleManager.awaitTermination();

            logger.info("Agent terminated normally");
            System.exit(0);

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Agent failed to start", e);
            System.exit(1);
        }
    }
}
