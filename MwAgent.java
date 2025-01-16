package mwmanger;

import mwmanger.common.Common;
import mwmanger.common.Config;
import mwmanger.vo.RawCommandsVO;

public class MwAgent {

	//static Config config = Config.getInstance();
	
    public static void main(String[] args) {

    	Config.setConfig();
    	
    	Runtime.getRuntime().addShutdownHook(new ShutdownThread());
		
		PreWork pw = new PreWork();
		RawCommandsVO rcv = pw.doPreWork();
		
		long rtn = 0;
		
		FirstWork fc = new FirstWork();
		rtn = fc.executeFirstCommands(rcv.getCommands());
		
		if(rtn<0){
			
			Config.getLogger().info("First commands execution failed.");
	        System.exit(0);
	        
		}

		MainWork c = new MainWork();
		rtn = c.doAgentWork();
		
		if(rtn<0){
			
			Config.getLogger().info("Commands execution failed.");
	        System.exit(0);
	        
		}
	   
	}

}
