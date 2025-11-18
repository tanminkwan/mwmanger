package mwmanger.agentfunction;

import java.lang.reflect.Constructor;
import java.util.logging.Level;

import static mwmanger.common.Config.getConfig;

public class AgentFuncFactory {

	public static AgentFunc getAgentFunc(String functionType){
		getConfig().getLogger().info("functionType : " + functionType);
		switch(functionType){
		
			case "say_hello" : return new HelloFunc(); 
			case "get_server_stat" : return new JmxStatFunc();
			case "get_ssl_certi" : return new SSLCertiFunc();
			case "get_ssl_certifile" : return new SSLCertiFileFunc();
			case "download_n_unzip" : return new  DownloadNUnzipFunc();
			case "read_all_from_syper" : return new SuckSyperFunc();
			default:
		    	try {
		    		Class<?> agentFunc = Class.forName(functionType);
		    		Constructor<?> constructor = agentFunc.getConstructor();
		    		return (AgentFunc)constructor.newInstance();
		    	}catch (Exception e) {
		    		getConfig().getLogger().log(Level.WARNING, e.getMessage(), e);
		    	}
		};
		return null;
	}
}
