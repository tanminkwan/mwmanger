package mwmanger;

import java.util.logging.Level;

import org.json.simple.JSONArray;

import mwmanger.common.Common;
import mwmanger.common.Config;
import mwmanger.vo.MwResponseVO;
import mwmanger.vo.RawCommandsVO;

public class PreWork {

	public RawCommandsVO doPreWork(){
		
		long rtn = 0;
	    
		RawCommandsVO rcv = new RawCommandsVO(); 
		
		for (;;){
			
            try {
            	
	            rcv = noticeStart();
	            rtn = rcv.getReturnCode();
			   
	        	//-1 : 'Agent Not Exists'
	        	if(rtn==-1){
		        	
		        	rtn = registerMe();
		        	
		        	if(rtn<0){
		        		Config.getLogger().severe("Agent Registeration Error.");
		        		rcv.setReturnCode(-20);
		        		break;
		        	}
		        	
		        	Thread.sleep(Config.getCommand_check_cycle()*1000);
	
		        //-2 : 'Not Approved Yet'
			    }else if(rtn==-2){
		        	
			    	Config.getLogger().info("Not Approved Yet");
		        	Thread.sleep(Config.getCommand_check_cycle()*1000);
		        	
		        }else if(rtn<0){
		        	
		        	Config.getLogger().severe("noticeStart Error.["+ Long.toString(rtn)+"]");
	        		break;
	        		
		        }else{
		        	
		        	break;
		        	
		        }
	        	
	    	} catch (InterruptedException e) {
	    		Config.getLogger().log(Level.SEVERE, "shutdown by Interrupted : " + e.getMessage(), e);
	    	    rcv.setReturnCode(-1);
	    	} catch (Exception e) {
	    		Config.getLogger().log(Level.SEVERE, e.getMessage(), e);
	    	    rcv.setReturnCode(-2);
	    	}
	        
		}
		
		return rcv;

	}
	
    private long registerMe(){

		String uri =  Config.getServer_url() + Config.getPost_agent_uri();

	    StringBuilder json = new StringBuilder();
	    json.append("{");
	    json.append("\"agent_id\":\""+Config.getAgent_id()+"\",");
	    json.append("\"agent_type\":\""+ Config.getAgent_type() +"\",");
	    json.append("\"installation_path\":\""+Common.escape(System.getProperty("user.dir"))+"\",");
	    json.append("\"host_id\":\""+Config.getHostName()+"\"");
	    json.append("}");
	        
	    Config.getLogger().fine("registerMe request parms :"+json.toString());

		MwResponseVO mwrv = Common.httpPOST(uri, Config.getAccess_token(), json.toString());
			
        if (mwrv.getResponse() != null) {
            	
           	long rtn = (Long) mwrv.getResponse().get("return_code");
            if(rtn<0){
            	Config.getLogger().severe(String.format("Error :[%d] [%s]", rtn, (String)mwrv.getResponse().get("message")));
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
		
			
		String uri = "";
			
		uri =  Config.getServer_url() + Config.getGet_command_uri() + "/" + Config.getAgent_id() + "/" + Config.getAgent_version() + "/" + Config.getAgent_type() + "/" + "BOOT";
		
		Config.getLogger().fine("noticeStart : "+uri);
		MwResponseVO mrvo = Common.httpGET(uri, Config.getAccess_token());
			
	    //access token expired
	    if(mrvo.getStatusCode()>=200 && mrvo.getStatusCode()<300){

	    	long rtn = (Long)mrvo.getResponse().get("return_code");
		                    
		    if (rtn < 0){
		       	//Agent 등록 필요
		      	rcv.setReturnCode(rtn);
		    }
		                    
		    commands = (JSONArray)mrvo.getResponse().get("data");
		    rcv.setCommands(commands);	    	

        }else{
        	Config.getLogger().severe("noticeStart Error : "+Long.toString(mrvo.getStatusCode()));
        	rcv.setReturnCode(-10);
        }
	        
		return rcv;
		
	}

}
