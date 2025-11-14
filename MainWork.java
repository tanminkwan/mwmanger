package mwmanger;

import static mwmanger.common.Config.getConfig;

import java.util.logging.Level;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import mwmanger.common.Common;
import mwmanger.vo.MwResponseVO;
import mwmanger.vo.RawCommandsVO;

public class MainWork {

	private final ExecutorService executorService = Executors.newCachedThreadPool();
	
	public long doAgentWork(){
		
		RawCommandsVO rcv = new RawCommandsVO();
		
        while (true) {
			   
            try {

		    	rcv = suckCommands();

		    	//Access Token Expired
	    		if(rcv.getReturnCode()==0){
	    			
	    			Common.updateToken();
	    	    	
	    		}else if(rcv.getReturnCode()>0){
	    			
		    	    for(Object commandObj : rcv.getCommands()){
	   
		    	        JSONObject command_ = (JSONObject) commandObj;
		    	        
		    	        String command_class  = (String)command_.get("command_class");
			    		
		    	        if(command_class==null){
		    	        	getConfig().getLogger().warning("Command_class not found : "+command_.toJSONString());
		    	        	continue;
		    	        }
		    	        
		    	        OrderCallerThread thread = new OrderCallerThread("mwmanger.order."+command_class, command_);
		    	        executorService.submit(thread);
		    	        //thread.setDaemon(true);
		    	        //thread.start();				    		   
			    		//thread.setName(command_class+"_");
	
		    	    }
		    	    
	    		}
		    	   
	    	    Thread.sleep(getConfig().getCommand_check_cycle()*1000);

	    	} catch (InterruptedException e) {
	    		getConfig().getLogger().log(Level.SEVERE, "shutdown by Interrupted : " + e.getMessage(), e);
	    	    return -1;
	    	} catch (Exception e) {
	    		getConfig().getLogger().log(Level.SEVERE, e.getMessage(), e);
	    	    return -1;
	    	}

        }
		
	}
	
    private RawCommandsVO suckCommands() {
    	
    	JSONArray commands = new JSONArray();
    	
    	RawCommandsVO rcv = new RawCommandsVO();
    	rcv.setReturnCode(1);
    	
        String path = getConfig().getGet_command_uri() + "/" + getConfig().getAgent_id();
			
		getConfig().getLogger().fine("getCommands : "+path);
		MwResponseVO mrvo = Common.httpGET(path, getConfig().getAccess_token());
			
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
