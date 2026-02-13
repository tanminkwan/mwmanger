package mwagent;

/**
 * @deprecated Use {@link CommandProcessingLoop} instead.
 * This class is kept for backward compatibility.
 */
@Deprecated
public class MainWork {

	private final CommandProcessingLoop delegate = new CommandProcessingLoop();

	/**
	 * @deprecated Use {@link CommandProcessingLoop#execute()} instead.
	 */
	@Deprecated
	public long doAgentWork(){
		return delegate.execute();
	}

}
