package mwmanger.order;

import static mwmanger.common.Config.getConfig;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.logging.Level;

import org.json.simple.JSONObject;
import mwmanger.vo.ResultVO;


public class ExeShell extends Order {

	public ExeShell(JSONObject command) {
		super(command);
		// TODO Auto-generated constructor stub
	}

	public int execute() {
		
		try {

			resultVo = runShell();
			
		}catch (Exception e) {
			getConfig().getLogger().log(Level.WARNING, e.getMessage(), e);    		
		}
		return 1;
	}

    private ResultVO runShell() {

    	String result = "";
    	String currentPath = System.getProperty("user.dir") + File.separator + commandVo.getTargetFilePath();
    	
    	ResultVO rv = new ResultVO();

    	rv.setOk(false);
		rv.setHostName(commandVo.getHostName());
		rv.setTargetFilePath(currentPath);
		rv.setTargetFileName(commandVo.getTargetFileName());

    	Runtime rt = Runtime.getRuntime();
    	String command_t = "";
    	String command_s = "";
		
    	command_t = currentPath + commandVo.getTargetFileName();
    	
    	if (getConfig().getOs().equals("WIN")){
    		command_s = "cmd /c start";
    	}else if (getConfig().getOs().equals("LINUX")){
    		command_s = "bash -c command";
    	}else if (getConfig().getOs().equals("AIX")){
    		command_s = "ksh -c command";
    	}else if (getConfig().getOs().equals("HPUX")){
    		command_s = "ksh -c command";
    	}
    	
    	String[] command = null;
    	
    	if (getConfig().getOs().equals("WIN")){
        	
    		command_s += " "+command_t;
    		
        	if(commandVo.getAdditionalParams().length() > 0){
        		command_s += " " + commandVo.getAdditionalParams();		
        	}
        	
        	command = command_s.split("\\s+");

    	}else{
    		
        	command = command_s.split("\\s+");
        	
	    	if(commandVo.getAdditionalParams().length() > 0){
	    		command_t += " " + commandVo.getAdditionalParams();		
	    	}    	
	
	    	command[command.length - 1] = command_t;

    	}
    	
    	for(String s : command){
    		getConfig().getLogger().fine("commandline: "+s);
    		System.out.println("commandline: "+s);
    	}
    	
    	try {
        	
    		rt.exec(command);
    		    		
    		result = "Command is executed.";
    		rv.setOk(true);
    		
    	}catch(UnsupportedEncodingException e){
    		getConfig().getLogger().log(Level.SEVERE, e.getMessage(), e);    		
    		result = "Error:UnsupportedEncodingException";
    	}catch(FileNotFoundException e){
    		getConfig().getLogger().log(Level.SEVERE, e.getMessage(), e);    		
    		result = "Error:FileNotFoundException";
    	}catch(IOException e){
    		getConfig().getLogger().log(Level.SEVERE, e.getMessage(), e);    		
    		result = "Error:IOException";
    	}
    	
    	rv.setResult(result);
    	return rv;
    }
}
