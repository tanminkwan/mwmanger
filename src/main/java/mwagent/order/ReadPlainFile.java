package mwagent.order;

import org.json.simple.JSONObject;

/**
 * Reads a file within the target path, appending an extension from additional_params.
 * 
 * Supported additional_params (String):
 * - File extension (without the dot).
 * 
 * Example:
 * If target_file_name="test" and additional_params="txt", reads "test.txt".
 */
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
