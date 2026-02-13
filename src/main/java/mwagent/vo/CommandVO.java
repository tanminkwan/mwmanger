package mwagent.vo;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class CommandVO {

	private String commandId = "";
	private long repetitionSeq = 0;
	private String hostName = null;
	private String targetFileName = null;
	private String targetFilePath = null;
	private String resultHash = null;
	private String additionalParams = null;
	private JSONObject additionalParamsJson = null;
	private String resultReceiver = null;
	private String targetObject = null;
	
	public String getCommandId() {
		return commandId;
	}
	public void setCommandId(String commandId) {
		this.commandId = commandId;
	}
	public long getRepetitionSeq() {
		return repetitionSeq;
	}
	public void setRepetitionSeq(long repetitionSeq) {
		this.repetitionSeq = repetitionSeq;
	}
	public String getHostName() {
		return hostName;
	}
	public void setHostName(String hostName) {
		this.hostName = hostName;
	}
	public String getTargetFileName() {
		return targetFileName;
	}
	public void setTargetFileName(String targetFileName) {
		this.targetFileName = targetFileName;
	}
	public String getTargetFilePath() {
		return targetFilePath;
	}
	public void setTargetFilePath(String targetFilePath) {
		this.targetFilePath = targetFilePath;
	}
	public String getResultHash() {
		return resultHash;
	}
	public void setResultHash(String resultHash) {
		this.resultHash = resultHash;
	}
	public String getAdditionalParams() {
		return additionalParams;
	}
	public void setAdditionalParams(String additionalParams) {
		this.additionalParams = additionalParams;
	}
	public org.json.simple.JSONObject getAdditionalParamsJson() {
		if (additionalParamsJson == null && additionalParams != null && !additionalParams.isEmpty()) {
			try {
				JSONParser parser = new JSONParser();
				Object obj = parser.parse(additionalParams);
				if (obj instanceof JSONObject) {
					additionalParamsJson = (JSONObject) obj;
				}
			} catch (Exception e) {
				// Not a valid JSON or other error, leave it null
			}
		}
		return additionalParamsJson;
	}
	public void setAdditionalParamsJson(JSONObject additionalParamsJson) {
		this.additionalParamsJson = additionalParamsJson;
	}
	public String getResultReceiver() {
		return resultReceiver;
	}
	public void setResultReceiver(String resultReceiver) {
		this.resultReceiver = resultReceiver;
	}
	public String getTargetObject() {
		return targetObject;
	}
	public void setTargetObject(String targetObject) {
		this.targetObject = targetObject;
	}
	
	@Override
	public String toString() {
		return "CommandVO [commandId=" + commandId + ", repetitionSeq=" + repetitionSeq + ", hostName=" + hostName
				+ ", targetFileName=" + targetFileName + ", targetFilePath=" + targetFilePath + ", resultHash="
				+ resultHash + ", additionalParams=" + additionalParams + ", resultReceiver=" + resultReceiver
				+ ", targetObject=" + targetObject + "]";
	}
	
}
