package mwmanger.agentfunction;

import java.util.ArrayList;

import mwmanger.vo.CommandVO;
import mwmanger.vo.ResultVO;

public interface AgentFunc {

	ArrayList<ResultVO> exeCommand(CommandVO command) throws Exception;
	
}
