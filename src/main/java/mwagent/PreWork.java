package mwagent;

import mwagent.service.registration.BootstrapService;
import mwagent.vo.RawCommandsVO;

/**
 * @deprecated Use {@link AgentRegistrationPhase} instead.
 * This class is kept for backward compatibility.
 */
@Deprecated
public class PreWork {

	private final AgentRegistrationPhase delegate;

	public PreWork() {
		this.delegate = new AgentRegistrationPhase();
	}

	public PreWork(BootstrapService bootstrapService) {
		this.delegate = new AgentRegistrationPhase(bootstrapService);
	}

	/**
	 * @deprecated Use {@link AgentRegistrationPhase#execute()} instead.
	 */
	@Deprecated
	public RawCommandsVO doPreWork() {
		return delegate.execute();
	}
}
