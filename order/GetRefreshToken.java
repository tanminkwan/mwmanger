package mwmanger.order;

import java.util.logging.Level;

//import java.util.logging.Level;

import org.json.simple.JSONObject;

import mwmanger.common.Common;
import mwmanger.common.Config;

public class GetRefreshToken extends Order {

	public GetRefreshToken(JSONObject command) {
		super(command);
		// TODO Auto-generated constructor stub
	}

	public int execute() {
		
		try {
						
			int rtn = Common.applyRefreshToken();
			
			if(rtn<0){
				return rtn;
			}
			
			resultVo.setOk(true);
			resultVo.setHostName(commandVo.getHostName());
			resultVo.setResult(Config.getRefresh_token());
			
		}catch (Exception e) {
			Config.getLogger().log(Level.SEVERE, e.getMessage(), e);
    		return -3;
		}
		return 1;
	}
	
}
