package mwmanger.vo;

public class ResultVO {
	
	private String targetFileName = "";
	private String hostName = "";
	private String targetFilePath = "";
	private String result = "";
	private String objectAggregationKey = "";
	private String resultHash = "";
	private boolean isOk = false;

	public String getResult() {
		return result;
	}
	
	public void setResult(String result) {
		this.result = result;
	}
	public String getObjectAggregationKey() {
		return objectAggregationKey;
	}
	public void setObjectAggregationKey(String objectAggregationKey) {
		this.objectAggregationKey = objectAggregationKey;
	}
	public boolean isOk() {
		return isOk;
	}
	public void setOk(boolean isOk) {
		this.isOk = isOk;
	}
	public String getResultHash() {
		return resultHash;
	}
	public void setResultHash(String resultHash) {
		this.resultHash = resultHash;
	}
	public String getTargetFileName() {
		return targetFileName;
	}
	public void setTargetFileName(String targetFileName) {
		this.targetFileName = targetFileName;
	}
	public String getHostName() {
		return hostName;
	}
	public void setHostName(String hostName) {
		this.hostName = hostName;
	}
	public String getTargetFilePath() {
		return targetFilePath;
	}
	public void setTargetFilePath(String targetFilePath) {
		this.targetFilePath = targetFilePath;
	}

	@Override
	public String toString() {
		return "ResultVO [targetFileName=" + targetFileName + ", hostName=" + hostName + ", targetFilePath="
				+ targetFilePath + ", result=" + result + ", objectAggregationKey=" + objectAggregationKey
				+ ", resultHash=" + resultHash + ", isOk=" + isOk + "]";
	}

}
