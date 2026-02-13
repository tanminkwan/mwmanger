package mwagent.agentfunction;

import java.util.ArrayList;

import mwagent.common.Common;
import mwagent.vo.CommandVO;
import mwagent.vo.ResultVO;

public class HelloFunc implements AgentFunc {

	@Override
	public ArrayList<ResultVO> exeCommand(CommandVO command) {

		ResultVO rv = new ResultVO();
		rv.setOk(true);
		rv.setResult("Hello World");
    	return Common.makeOneResultArray(rv, command);
		
	}

}
