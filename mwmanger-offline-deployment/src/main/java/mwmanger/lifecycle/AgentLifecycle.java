package mwmanger.lifecycle;

/**
 * Agent의 생명주기를 관리하는 인터페이스
 *
 * 모든 Agent 서비스 컴포넌트는 이 인터페이스를 구현하여
 * 일관된 생명주기 관리를 가능하게 함
 */
public interface AgentLifecycle {

    /**
     * 서비스를 시작합니다.
     * 이 메서드는 블로킹되지 않으며, 백그라운드 스레드를 시작할 수 있습니다.
     *
     * @throws Exception 시작 중 에러 발생 시
     */
    void start() throws Exception;

    /**
     * 서비스를 정상적으로 종료합니다.
     * 실행 중인 작업을 완료하고 자원을 정리합니다.
     *
     * @throws Exception 종료 중 에러 발생 시
     */
    void stop() throws Exception;

    /**
     * 현재 생명주기 상태를 반환합니다.
     *
     * @return 현재 상태
     */
    LifecycleState getState();

    /**
     * 서비스가 실행 중인지 확인합니다.
     *
     * @return 실행 중이면 true
     */
    default boolean isRunning() {
        return getState().isRunning();
    }

    /**
     * 서비스가 종료되었는지 확인합니다.
     *
     * @return 종료되었으면 true
     */
    default boolean isStopped() {
        return getState().isStopped();
    }
}
