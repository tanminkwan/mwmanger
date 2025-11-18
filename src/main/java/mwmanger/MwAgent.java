package mwmanger;

import static mwmanger.common.Config.getConfig;

import mwmanger.vo.RawCommandsVO;

public class MwAgent {

	//static Config config = Config.getInstance();
	
    public static void main(String[] args) {

    	getConfig().setConfig();
    	
    	Runtime.getRuntime().addShutdownHook(new ShutdownThread());
		
		PreWork pw = new PreWork();
		RawCommandsVO rcv = pw.doPreWork();
		
		long rtn = 0;
		
		FirstWork fc = new FirstWork();
		rtn = fc.executeFirstCommands(rcv.getCommands());
		
		if(rtn<0){
			
			getConfig().getLogger().info("First commands execution failed.");
	        System.exit(0);
	        
		}

		MainWork c = new MainWork();
		rtn = c.doAgentWork();
		
		if(rtn<0){
			
			getConfig().getLogger().info("Commands execution failed.");
	        System.exit(0);
	        
		}
	   
	}

}
