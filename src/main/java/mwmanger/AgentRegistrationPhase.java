package mwmanger;

import mwmanger.service.registration.BootstrapService;
import mwmanger.vo.RawCommandsVO;

/**
 * Agent 시작 전 준비 작업 담당 (등록 단계)
 * - Agent 등록
 * - 승인 대기
 * - 초기 명령 조회
 *
 * 리팩토링: 등록 로직을 BootstrapService로 모듈화
 *
 * @deprecated Use {@link AgentRegistrationPhase} instead of PreWork
 */
public class AgentRegistrationPhase {

	private final BootstrapService bootstrapService;

	public AgentRegistrationPhase() {
		this.bootstrapService = new BootstrapService();
	}

	/**
	 * Constructor for dependency injection (테스트 용이성)
	 */
	public AgentRegistrationPhase(BootstrapService bootstrapService) {
		this.bootstrapService = bootstrapService;
	}

	/**
	 * Agent 부트스트랩 프로세스 실행
	 * - 등록되지 않은 경우 자동 등록
	 * - 승인 대기
	 * - 승인 완료 시 초기 명령 반환
	 *
	 * @return RawCommandsVO (초기 명령 + 상태 코드)
	 */
	public RawCommandsVO execute() {
		return bootstrapService.executeBootstrapProcess();
	}
}
