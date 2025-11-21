package mwmanger.lifecycle;

import static mwmanger.common.Config.getConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Agent의 Graceful Shutdown을 담당하는 핸들러
 *
 * 등록된 모든 서비스를 역순으로 정상 종료하고
 * 자원을 정리합니다.
 */
public class GracefulShutdownHandler {

    private final Logger logger;
    private final List<AgentLifecycle> services;
    private final int shutdownTimeoutSeconds;

    public GracefulShutdownHandler() {
        this(60); // Default 60초 timeout
    }

    public GracefulShutdownHandler(int shutdownTimeoutSeconds) {
        this.logger = getConfig().getLogger();
        this.services = new ArrayList<>();
        this.shutdownTimeoutSeconds = shutdownTimeoutSeconds;
    }

    /**
     * 종료 시 정리할 서비스 등록
     * 등록된 순서의 역순으로 종료됨 (LIFO)
     */
    public void registerService(AgentLifecycle service) {
        if (service != null) {
            services.add(service);
            logger.fine("Registered service for shutdown: " + service.getClass().getSimpleName());
        }
    }

    /**
     * 모든 서비스를 Graceful하게 종료
     * 등록된 역순으로 종료 (나중에 시작된 것을 먼저 종료)
     */
    public void shutdown() {
        logger.info("========================================");
        logger.info("Graceful shutdown initiated");
        logger.info("========================================");

        if (services.isEmpty()) {
            logger.info("No services to shutdown");
            return;
        }

        // Reverse order shutdown (LIFO)
        List<AgentLifecycle> reversedServices = new ArrayList<>(services);
        Collections.reverse(reversedServices);

        int successCount = 0;
        int failureCount = 0;

        for (AgentLifecycle service : reversedServices) {
            String serviceName = service.getClass().getSimpleName();

            try {
                logger.info("Stopping service: " + serviceName + " (current state: " + service.getState() + ")");

                long startTime = System.currentTimeMillis();
                service.stop();
                long elapsedTime = System.currentTimeMillis() - startTime;

                logger.info("Service stopped successfully: " + serviceName + " (" + elapsedTime + "ms)");
                successCount++;

            } catch (Exception e) {
                logger.log(Level.SEVERE, "Failed to stop service: " + serviceName, e);
                failureCount++;
            }
        }

        logger.info("========================================");
        logger.info("Shutdown completed: " + successCount + " succeeded, " + failureCount + " failed");
        logger.info("========================================");

        // Flush logs
        flushLogs();
    }

    /**
     * 로그를 flush하여 모든 로그가 기록되도록 함
     */
    private void flushLogs() {
        try {
            if (logger != null && logger.getHandlers() != null) {
                for (java.util.logging.Handler handler : logger.getHandlers()) {
                    handler.flush();
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to flush logs: " + e.getMessage());
        }
    }

    /**
     * 등록된 서비스 개수 반환
     */
    public int getServiceCount() {
        return services.size();
    }

    /**
     * 모든 서비스의 현재 상태 반환
     */
    public List<ServiceStatus> getServiceStatuses() {
        List<ServiceStatus> statuses = new ArrayList<>();
        for (AgentLifecycle service : services) {
            statuses.add(new ServiceStatus(
                service.getClass().getSimpleName(),
                service.getState()
            ));
        }
        return statuses;
    }

    /**
     * 서비스 상태 정보
     */
    public static class ServiceStatus {
        private final String serviceName;
        private final LifecycleState state;

        public ServiceStatus(String serviceName, LifecycleState state) {
            this.serviceName = serviceName;
            this.state = state;
        }

        public String getServiceName() {
            return serviceName;
        }

        public LifecycleState getState() {
            return state;
        }

        @Override
        public String toString() {
            return serviceName + ": " + state;
        }
    }
}
