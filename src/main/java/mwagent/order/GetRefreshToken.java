package mwagent.order;

import static mwagent.common.Config.getConfig;

import java.util.logging.Level;
import org.json.simple.JSONObject;
import mwagent.common.Common;

public class GetRefreshToken extends Order {

	public GetRefreshToken(JSONObject command) {
		super(command);
	}

	public int execute() {
		
		try {
						
			int rtn = Common.applyRefreshToken();
			
			if(rtn<0){
				return rtn;
			}
			
			resultVo.setOk(true);
			resultVo.setHostName(commandVo.getHostName());
			resultVo.setResult(getConfig().getRefresh_token());
			
		}catch (Exception e) {
			getConfig().getLogger().log(Level.SEVERE, e.getMessage(), e);
    		return -3;
		}
		return 1;
	}
	
}
