package mwmanger;

import java.util.logging.Level;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import mwmanger.common.Common;
import mwmanger.common.Config;
import mwmanger.vo.MwResponseVO;
import mwmanger.vo.RawCommandsVO;

public class MainWork {

	private final ExecutorService executorService = Executors.newCachedThreadPool();

	public long doAgentWork(){
		
		RawCommandsVO rcv = new RawCommandsVO();
		Config config = Config.getInstance();

        while (true) {
			   
            try {

		    	rcv = suckCommands();

		    	//Access Token Expired
	    		if(rcv.getReturnCode()==0){
	    			
	    			Common.updateToken();
	    	    	
	    		}else if(rcv.getReturnCode()>0){

					for (Object commandObj : rcv.getCommands()) {
	   
		    	        JSONObject command = (JSONObject) commandObj;
		    	        
		    	        String command_class  = (String)command.get("command_class");
			    		
		    	        if(command_class==null){
		    	        	config.getLogger().warning("Command_class not found : "+command_.toJSONString());
		    	        	continue;
		    	        }
		    	        
                        OrderCallerThread thread = new OrderCallerThread("mwmanger.order." + command_class, command);
                        executorService.submit(thread);
	
		    	    }
		    	    
	    		}
		    	   
	    	    Thread.sleep(config.getCommand_check_cycle() * 1000);

	    	} catch (InterruptedException e) {
	    	    config.getLogger().log(Level.SEVERE, "shutdown by Interrupted : " + e.getMessage(), e);
	    	    return -1;
	    	} catch (Exception e) {
	    	    config.getLogger().log(Level.SEVERE, e.getMessage(), e);
	    	    return -1;
	    	}

        }
		
	}
	
    private RawCommandsVO suckCommands() {
    	
    	RawCommandsVO rcv = new RawCommandsVO();
		Config config = Config.getInstance();
		rcv.setReturnCode(1);
    	
        String uri = config.getServer_url() + Config.getGet_command_uri() + "/" + Config.getAgent_id();
		config.getLogger().fine("getCommands : "+uri);

		MwResponseVO mrvo = Common.httpGET(uri, config.getAccess_token());
			
	    //Access Token Expired
	    if(mrvo.getStatusCode()==401){
	    	
	    	rcv.setReturnCode(0);
	    	
	    }else if(mrvo.getStatusCode()>=200 && mrvo.getStatusCode()<300){
	    	
		    long rtn = (Long)mrvo.getResponse().get("return_code");
		                    
		    if (rtn < 0){
		       	//Agent 등록 필요 등
		    	rcv.setReturnCode(rtn);
		    }else{
		                    
		        commands = (JSONArray)mrvo.getResponse().get("data");
		        rcv.setCommands(commands);
		        
		    }
		    
        }else if(mrvo.getStatusCode()<0){
        	rcv.setReturnCode(mrvo.getStatusCode());
        }else{
        	rcv.setReturnCode(-10);
        }
	        
		return rcv;
		
	}

}
