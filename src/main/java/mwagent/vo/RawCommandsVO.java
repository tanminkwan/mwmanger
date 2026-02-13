package mwagent.vo;

import org.json.simple.JSONArray;

public class RawCommandsVO {
	
	JSONArray commands;
	long returnCode;
	
	public JSONArray getCommands() {
		return commands;
	}
	public void setCommands(JSONArray commands) {
		this.commands = commands;
	}
	public long getReturnCode() {
		return returnCode;
	}
	public void setReturnCode(long returnCode) {
		this.returnCode = returnCode;
	}
}
