package mwmanger.order;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.logging.Level;

import org.json.simple.JSONObject;

import mwmanger.common.Config;
import mwmanger.vo.ResultVO;

public class ExeScript extends Order {

	public ExeScript(JSONObject command) {
		super(command);
		// TODO Auto-generated constructor stub
	}

	public int execute() {
		
		try {
			
			resultVo = runScript();

		}catch (Exception e) {
			e.printStackTrace();
			Config.getLogger().log(Level.SEVERE, e.getMessage(), e);    		
		}
		return 1;
	}

    private ResultVO runScript() {

    	String result = "";
    	String currentPath = System.getProperty("user.dir") + File.separator + commandVo.getTargetFilePath();
    	
    	ResultVO rv = new ResultVO();

    	rv.setOk(false);
		rv.setHostName(commandVo.getHostName());
		rv.setTargetFilePath(currentPath);
		rv.setTargetFileName(commandVo.getTargetFileName());

    	String command = currentPath + commandVo.getTargetFileName() + " " + commandVo.getAdditionalParams();
    	String[] commands = command.split("\\s+");    	
    	
    	try {
        	
    		ProcessBuilder pb = new ProcessBuilder(commands);
    		Process proc = pb.start();
    		//Process proc = rt.exec(commands);
    		
    		BufferedReader in = new BufferedReader(new InputStreamReader(proc.getInputStream(), "UTF8"));
    	
    		String line = null;
    		String ls = System.getProperty("line.separator");
    		StringBuilder sb = new StringBuilder();
    		
    		while((line=in.readLine())!=null){
    			sb.append(line);
    			sb.append(ls);
    		}
    		
    		result = sb.toString();
        	rv.setOk(true);
    		Config.getLogger().info("ExeScript result : "+result);
    		in.close();
    		
    	}catch(UnsupportedEncodingException e){
    		
    		Config.getLogger().log(Level.WARNING, e.getMessage(), e);    		
    		result = "Error:UnsupportedEncodingException";
    		
    	}catch(FileNotFoundException e){
    		
    		Config.getLogger().log(Level.WARNING, e.getMessage(), e);    		
    		result = "Error:FileNotFoundException " + currentPath + commandVo.getTargetFileName();
    		
    	}catch(IOException e){
    		
    		Config.getLogger().log(Level.WARNING, e.getMessage(), e);    		
    		result = "Error:IOException";
    		
    	}
    	
    	rv.setResult(result);
    	
    	return rv;
    	
    }
    
}
