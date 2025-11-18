package mwmanger.agentfunction;

import java.util.ArrayList;

import mwmanger.common.Common;
import mwmanger.vo.CommandVO;
import mwmanger.vo.ResultVO;

public class HelloFunc implements AgentFunc {

	@Override
	public ArrayList<ResultVO> exeCommand(CommandVO command) {

		ResultVO rv = new ResultVO();
		rv.setOk(true);
		rv.setResult("Hello World");
    	return Common.makeOneResultArray(rv, command);
		
	}

}
