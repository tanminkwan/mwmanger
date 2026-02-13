package mwagent.agentfunction;

import java.util.ArrayList;

import mwagent.vo.CommandVO;
import mwagent.vo.ResultVO;

public interface AgentFunc {

	ArrayList<ResultVO> exeCommand(CommandVO command) throws Exception;
	
}
