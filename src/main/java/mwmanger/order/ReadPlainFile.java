package mwmanger.order;

import org.json.simple.JSONObject;

public class ReadPlainFile extends ReadFile {

	public ReadPlainFile(JSONObject command) {
		super(command);
	}

	protected String getFileFullName(){
		
		String file_name = commandVo.getTargetFileName();
    	
    	if(commandVo.getAdditionalParams().length() > 0){
    		file_name += "." + commandVo.getAdditionalParams();		
    	}    	
    	
		return commandVo.getTargetFilePath() + file_name;
		
	}
	
	protected String getFileName(){
		
		String file_name = commandVo.getTargetFileName();
    	
    	if(commandVo.getAdditionalParams().length() > 0){
    		file_name += "." + commandVo.getAdditionalParams();		
    	}    	
    	
		return file_name;
		
	}
	
	protected String getFilePath(){
		
		return commandVo.getTargetFilePath();
		
	}

}
