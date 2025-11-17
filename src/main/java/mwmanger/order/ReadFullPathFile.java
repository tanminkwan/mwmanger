package mwmanger.order;

import org.json.simple.JSONObject;

public class ReadFullPathFile extends ReadFile {

	public ReadFullPathFile(JSONObject command) {
		super(command);
		// TODO Auto-generated constructor stub
	}

	protected String getFileFullName(){
		
		return commandVo.getAdditionalParams();
		
	}
	
	protected String getFileName(){
		
		return commandVo.getTargetFileName();
		
	}

	protected String getFilePath(){
	
		return commandVo.getAdditionalParams();
		
	}
	
}
