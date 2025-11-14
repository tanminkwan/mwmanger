package mwmanger;

import static mwmanger.common.Config.getConfig;

import java.util.logging.Level;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import mwmanger.common.Common;
import mwmanger.vo.MwResponseVO;
import mwmanger.vo.RawCommandsVO;

public class PreWork {

	public RawCommandsVO doPreWork(){
		
		long rtn = 0;
	    
		RawCommandsVO rcv = new RawCommandsVO(); 
		
		while (true){
			
            try {
            	
	            rcv = noticeStart();
	            rtn = rcv.getReturnCode();
			   
	        	//-1 : 'Agent Not Exists'
	        	if(rtn==-1){
		        	
		        	rtn = registerMe();
		        	
		        	if(rtn<0){
		        		getConfig().getLogger().severe("Agent Registeration Error.");
		        		rcv.setReturnCode(-20);
		        		break;
		        	}
		        	
		        	Thread.sleep(getConfig().getCommand_check_cycle() * 1000);
	
		        //-2 : 'Not Approved Yet'
			    }else if(rtn==-2){
		        	
			    	getConfig().getLogger().info("Agent is not Approved yet.");
		        	Thread.sleep(getConfig().getCommand_check_cycle() * 1000);
		        	
		        }else if(rtn<0){
		        	
		        	getConfig().getLogger().severe("noticeStart Error.["+ Long.toString(rtn)+"]");
	        		break;
	        		
		        }else{
		        	
		        	break;
		        	
		        }
	        	
	    	} catch (InterruptedException e) {
	    		getConfig().getLogger().log(Level.SEVERE, "Shutdown by Interrupted : " + e.getMessage(), e);
	    	    rcv.setReturnCode(-1);
	    	    break;
	    	} catch (Exception e) {
	    		getConfig().getLogger().log(Level.SEVERE, e.getMessage(), e);
	    	    rcv.setReturnCode(-2);
	    	    break;
	    	}
	        
		}
		
		return rcv;

	}
	
	/**
	 * Agent 등록 Method
	 * @return Success 1, Failure negative integer 
	 */
	@SuppressWarnings("unchecked")
    private long registerMe(){

		String path =  getConfig().getPost_agent_uri();

		JSONObject jsonObj = new JSONObject();
		jsonObj.put("agent_id", getConfig().getAgent_id());
		jsonObj.put("agent_type", getConfig().getAgent_type());
		jsonObj.put("installation_path", System.getProperty("user.dir"));
		jsonObj.put("host_id", getConfig().getHostName());

		getConfig().getLogger().fine("registerMe request parms :"+jsonObj.toString());
		
		MwResponseVO mwrv = Common.httpPOST(path, getConfig().getAccess_token(), jsonObj.toString());
			
        if (mwrv.getResponse() != null) {
            	
           	long rtn = (Long) mwrv.getResponse().get("return_code");
            if(rtn<0){
            	getConfig().getLogger().severe(String.format("Error :[%d] [%s]", rtn, (String)mwrv.getResponse().get("message")));
               	return -1;
            }
                
        }else{
        	return -2;
        }
        
        return 1;
    	
    }

    private RawCommandsVO noticeStart() {
    	
    	JSONArray commands = new JSONArray();
    	
    	RawCommandsVO rcv = new RawCommandsVO();
    	rcv.setReturnCode(1);
		
			
		String path = getConfig().getGet_command_uri()
				+ "/" + getConfig().getAgent_id()
				+ "/" + getConfig().getAgent_version()
				+ "/" + getConfig().getAgent_type()
				+ "/BOOT";
		
		getConfig().getLogger().fine("noticeStart : "+path);
		
		MwResponseVO mrvo = Common.httpGET(path, getConfig().getAccess_token());
			
	    //access token expired
	    if(mrvo.getStatusCode() >= 200 && mrvo.getStatusCode() < 300){

	    	long rtn = (Long)mrvo.getResponse().get("return_code");
		                    
		    if (rtn < 0){
		       	//Agent 등록 필요
		      	rcv.setReturnCode(rtn);
		    }
		                    
		    commands = (JSONArray)mrvo.getResponse().get("data");
		    rcv.setCommands(commands);	    	

        }else{
        	getConfig().getLogger().severe("noticeStart Error : "+Long.toString(mrvo.getStatusCode()));
        	rcv.setReturnCode(-10);
        }
	        
		return rcv;
		
	}

}
