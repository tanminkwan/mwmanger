package mwmanger;

import mwmanger.service.registration.BootstrapService;
import mwmanger.vo.RawCommandsVO;

/**
 * Agent 시작 전 준비 작업 담당
 * - Agent 등록
 * - 승인 대기
 * - 초기 명령 조회
 *
 * 리팩토링: 등록 로직을 BootstrapService로 모듈화
 */
public class PreWork {

	private final BootstrapService bootstrapService;

	public PreWork() {
		this.bootstrapService = new BootstrapService();
	}

	/**
	 * Constructor for dependency injection (테스트 용이성)
	 */
	public PreWork(BootstrapService bootstrapService) {
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
	public RawCommandsVO doPreWork() {
		return bootstrapService.executeBootstrapProcess();
	}
}
