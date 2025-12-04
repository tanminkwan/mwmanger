package mwmanger;

import org.json.simple.JSONArray;

/**
 * @deprecated Use {@link InitializationPhase} instead.
 * This class is kept for backward compatibility.
 */
@Deprecated
public class FirstWork {

	private final InitializationPhase delegate = new InitializationPhase();

	/**
	 * @deprecated Use {@link InitializationPhase#execute(JSONArray)} instead.
	 */
	@Deprecated
	public long executeFirstCommands(JSONArray commands) {
		return delegate.execute(commands);
	}

}
