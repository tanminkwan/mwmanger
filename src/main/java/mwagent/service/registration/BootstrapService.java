package mwagent.service.registration;

import static mwagent.common.Config.getConfig;

import java.util.logging.Level;
import java.util.logging.Logger;

import mwagent.vo.AgentStatus;
import mwagent.vo.RawCommandsVO;
import mwagent.vo.RegistrationRequest;
import mwagent.vo.RegistrationResponse;

/**
 * Agent 부트스트랩 프로세스 전체를 관리하는 서비스
 * PreWork.doPreWork() 로직을 모듈화
 *
 * 1. Agent 상태 확인
 * 2. 미등록 시 등록 시도
 * 3. 승인 대기
 * 4. 승인 완료 시 초기 명령 반환
 */
public class BootstrapService {

    private final Logger logger;
    private final RegistrationService registrationService;
    private final AgentStatusService statusService;

    public BootstrapService() {
        this.logger = getConfig().getLogger();
        this.registrationService = new RegistrationService();
        this.statusService = new AgentStatusService();
    }

    /**
     * Constructor for dependency injection (테스트 용이성)
     */
    public BootstrapService(RegistrationService registrationService,
                           AgentStatusService statusService) {
        Logger configLogger = getConfig() != null ? getConfig().getLogger() : null;
        this.logger = configLogger != null ? configLogger : Logger.getLogger(BootstrapService.class.getName());
        this.registrationService = registrationService;
        this.statusService = statusService;
    }

    /**
     * Agent 부트스트랩 프로세스 실행
     * 등록 -> 승인 대기 -> 초기 명령 반환
     *
     * @return RawCommandsVO (초기 명령 + 상태 코드)
     */
    public RawCommandsVO executeBootstrapProcess() {

        RawCommandsVO rcv = new RawCommandsVO();

        logger.info("Starting agent bootstrap process...");

        while (true) {

            try {

                // 1. Agent 상태 확인 및 BOOT 명령 조회
                rcv = statusService.noticeStartAndGetStatus();
                AgentStatus status = statusService.getAgentStatus(rcv);

                // 2. 상태에 따른 처리
                if (status.needsRegistration()) {
                    // Agent 미등록 -> 등록 시도
                    logger.info("Agent not registered. Attempting registration...");

                    if (!handleRegistration(rcv)) {
                        break; // 등록 실패 시 종료
                    }

                    // 등록 후 재시도 대기
                    sleepWithLogging(getConfig().getCommand_check_cycle() * 1000);

                } else if (status.isPending()) {
                    // 승인 대기 중
                    logger.info("Agent registration pending approval. Waiting...");
                    sleepWithLogging(getConfig().getCommand_check_cycle() * 1000);

                } else if (status.isApproved()) {
                    // 승인 완료 -> 정상 시작
                    logger.info("Agent approved. Bootstrap completed successfully.");
                    break;

                } else {
                    // 에러 발생
                    logger.severe("Bootstrap failed with status: " + status.getDescription());
                    break;
                }

            } catch (InterruptedException e) {
                logger.log(Level.SEVERE, "Bootstrap interrupted: " + e.getMessage(), e);
                rcv.setReturnCode(-1);
                break;

            } catch (Exception e) {
                logger.log(Level.SEVERE, "Bootstrap error: " + e.getMessage(), e);
                rcv.setReturnCode(-2);
                break;
            }
        }

        return rcv;
    }

    /**
     * Agent 등록 처리
     *
     * @param rcv 현재 상태를 담은 RawCommandsVO (에러 코드 업데이트용)
     * @return 등록 성공 여부
     */
    private boolean handleRegistration(RawCommandsVO rcv) {

        RegistrationRequest request = registrationService.createRegistrationRequest();
        RegistrationResponse response = registrationService.register(request);

        if (!response.isSuccess()) {
            logger.severe("Agent registration error.");
            rcv.setReturnCode(-20);
            return false;
        }

        logger.info("Agent registration request submitted successfully.");
        return true;
    }

    /**
     * Thread sleep with logging
     *
     * @param millis sleep 시간 (밀리초)
     * @throws InterruptedException
     */
    private void sleepWithLogging(long millis) throws InterruptedException {
        logger.fine("Waiting " + (millis / 1000) + " seconds before retry...");
        Thread.sleep(millis);
    }
}
