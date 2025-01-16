package mwmanger.vo;

import org.json.simple.JSONObject;

public class MwResponseVO {

	private JSONObject response;
	private int statusCode;
	private String fileName = "";
	private String fileLocation = "";
	
	
	public JSONObject getResponse() {
		return response;
	}
	public void setResponse(JSONObject response) {
		this.response = response;
	}
	public int getStatusCode() {
		return statusCode;
	}
	public void setStatusCode(int statusCode) {
		this.statusCode = statusCode;
	}
	public String getFileName() {
		return fileName;
	}
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	public String getFileLocation() {
		return fileLocation;
	}
	public void setFileLocation(String fileLocation) {
		this.fileLocation = fileLocation;
	}
	@Override
	public String toString() {
		return "MwResponseVO [response=" + response + ", statusCode=" + statusCode + ", fileName=" + fileName
				+ ", fullFileName=" + fileLocation + "]";
	}
	
}
