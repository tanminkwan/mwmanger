package mwagent.lifecycle;

/**
 * Agent의 생명주기 상태를 나타내는 enum
 *
 * CREATED  - 인스턴스 생성됨
 * STARTING - 시작 중 (초기화, 연결 등)
 * RUNNING  - 정상 실행 중
 * STOPPING - 종료 중 (자원 정리)
 * STOPPED  - 종료 완료
 * FAILED   - 에러로 인한 실패 상태
 */
public enum LifecycleState {
    CREATED("Created - not yet started"),
    STARTING("Starting - initialization in progress"),
    RUNNING("Running - fully operational"),
    STOPPING("Stopping - graceful shutdown in progress"),
    STOPPED("Stopped - fully terminated"),
    FAILED("Failed - error occurred");

    private final String description;

    LifecycleState(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean isStarted() {
        return this == STARTING || this == RUNNING;
    }

    public boolean isRunning() {
        return this == RUNNING;
    }

    public boolean isStopped() {
        return this == STOPPED || this == FAILED;
    }

    public boolean canTransitionTo(LifecycleState newState) {
        switch (this) {
            case CREATED:
                return newState == STARTING || newState == FAILED;
            case STARTING:
                return newState == RUNNING || newState == FAILED;
            case RUNNING:
                return newState == STOPPING || newState == FAILED;
            case STOPPING:
                return newState == STOPPED || newState == FAILED;
            case STOPPED:
                return false; // Terminal state
            case FAILED:
                return false; // Terminal state
            default:
                return false;
        }
    }
}
