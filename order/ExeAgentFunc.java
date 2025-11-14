package mwmanger.order;

import static mwmanger.common.Config.getConfig;

import java.util.logging.Level;

import org.json.simple.JSONObject;

import mwmanger.agentfunction.AgentFunc;
import mwmanger.agentfunction.AgentFuncFactory;

public class ExeAgentFunc extends Order {

	public ExeAgentFunc(JSONObject command) {
		super(command);
		// TODO Auto-generated constructor stub
	}

	public int execute() {

		int rtn = 1;
		
		getConfig().getLogger().info("commandVo : "+commandVo.toString());
		AgentFunc func = AgentFuncFactory.getAgentFunc(commandVo.getTargetFileName());
		
		try{
			
			resultVos = func.exeCommand(commandVo);
			
		}catch (Exception e) {
			getConfig().getLogger().log(Level.SEVERE, e.getMessage(), e);
    		rtn = -1;
		}				

		return rtn;
	}


}
