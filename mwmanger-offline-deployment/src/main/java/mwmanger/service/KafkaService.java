package mwmanger.service;

import static mwmanger.common.Config.getConfig;

import java.util.logging.Level;
import java.util.logging.Logger;

import mwmanger.kafka.MwConsumerThread;
import mwmanger.kafka.MwHealthCheckThread;
import mwmanger.kafka.MwProducer;
import mwmanger.lifecycle.AgentLifecycle;
import mwmanger.lifecycle.LifecycleState;

/**
 * Kafka 통신을 관리하는 서비스
 *
 * - Consumer Thread: 명령 수신
 * - HealthCheck Thread: Agent 상태 보고
 * - Producer: 결과 전송
 */
public class KafkaService implements AgentLifecycle {

    private final Logger logger;
    private LifecycleState state;

    private String brokerAddress;
    private MwConsumerThread consumerThread;
    private MwHealthCheckThread healthCheckThread;
    private boolean producerInitialized = false;

    public KafkaService() {
        this.logger = getConfig().getLogger();
        this.state = LifecycleState.CREATED;
    }

    /**
     * Kafka 브로커 주소 설정
     * start() 호출 전에 설정되어야 함
     */
    public void setBrokerAddress(String brokerAddress) {
        if (state != LifecycleState.CREATED) {
            throw new IllegalStateException("Cannot set broker address after service has started");
        }
        this.brokerAddress = brokerAddress;
        getConfig().setKafka_broker_address(brokerAddress);
    }

    @Override
    public void start() throws Exception {
        if (!state.canTransitionTo(LifecycleState.STARTING)) {
            throw new IllegalStateException("Cannot start from state: " + state);
        }

        if (brokerAddress == null || brokerAddress.isEmpty()) {
            logger.info("Kafka broker address not configured. Skipping Kafka initialization.");
            state = LifecycleState.RUNNING;
            return;
        }

        logger.info("Starting Kafka service with broker: " + brokerAddress);
        state = LifecycleState.STARTING;

        try {
            // 1. Start Consumer Thread
            consumerThread = new MwConsumerThread(brokerAddress, "t_" + getConfig().getAgent_id());
            consumerThread.setDaemon(true);
            consumerThread.setName("MwConsumer");
            consumerThread.start();
            logger.info("Kafka consumer thread started");

            // 2. Start HealthCheck Thread
            healthCheckThread = new MwHealthCheckThread(brokerAddress, "t_agent_health");
            healthCheckThread.setDaemon(true);
            healthCheckThread.setName("MwHealthCheck");
            healthCheckThread.start();
            logger.info("Kafka health check thread started");

            // 3. Initialize Producer
            MwProducer.getInstance();
            producerInitialized = true;
            logger.info("Kafka producer initialized");

            state = LifecycleState.RUNNING;
            logger.info("Kafka service started successfully");

        } catch (Exception e) {
            state = LifecycleState.FAILED;
            logger.log(Level.SEVERE, "Failed to start Kafka service", e);
            throw e;
        }
    }

    @Override
    public void stop() throws Exception {
        if (!state.canTransitionTo(LifecycleState.STOPPING)) {
            logger.warning("Cannot stop from state: " + state);
            return;
        }

        logger.info("Stopping Kafka service...");
        state = LifecycleState.STOPPING;

        try {
            // Stop consumer thread
            if (consumerThread != null && consumerThread.isAlive()) {
                consumerThread.interrupt();
                consumerThread.join(5000); // Wait max 5 seconds
                logger.info("Kafka consumer thread stopped");
            }

            // Stop health check thread
            if (healthCheckThread != null && healthCheckThread.isAlive()) {
                healthCheckThread.interrupt();
                healthCheckThread.join(5000);
                logger.info("Kafka health check thread stopped");
            }

            // Close producer
            if (producerInitialized) {
                MwProducer.getInstance().close();
                logger.info("Kafka producer closed");
            }

            state = LifecycleState.STOPPED;
            logger.info("Kafka service stopped successfully");

        } catch (Exception e) {
            state = LifecycleState.FAILED;
            logger.log(Level.SEVERE, "Error stopping Kafka service", e);
            throw e;
        }
    }

    @Override
    public LifecycleState getState() {
        return state;
    }

    /**
     * Kafka가 설정되었는지 확인
     */
    public boolean isConfigured() {
        return brokerAddress != null && !brokerAddress.isEmpty();
    }

    /**
     * Consumer Thread 반환 (테스트용)
     */
    MwConsumerThread getConsumerThread() {
        return consumerThread;
    }

    /**
     * HealthCheck Thread 반환 (테스트용)
     */
    MwHealthCheckThread getHealthCheckThread() {
        return healthCheckThread;
    }
}
