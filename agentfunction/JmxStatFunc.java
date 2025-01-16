package mwmanger.agentfunction;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Level;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.naming.Context;
import javax.naming.InitialContext;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import jeus.management.j2ee.J2EEDomainMBean;
import mwmanger.common.Common;
import mwmanger.common.Config;
import mwmanger.vo.CommandVO;
import mwmanger.vo.ResultVO;

public class JmxStatFunc implements AgentFunc {
	
	@Override
	public ArrayList<ResultVO> exeCommand(CommandVO command) throws IOException{

		String params = command.getTargetFilePath();
    	
    	String jmx_url = "";
    	String jmx_username = "";
    	String jmx_password = "";
    	String jmx_target = "";
    	String jmx_domain = "";
    	JMXConnector connector = null;

    	ResultVO rv = new ResultVO();
    	rv.setOk(false);
    	
		try{
			
			JSONParser jsonPar = new JSONParser();
			JSONObject jsonObj = (JSONObject) jsonPar.parse(params);
		
    		jmx_url = (String)jsonObj.get("jmx_url");
    		jmx_username = (String)jsonObj.get("jmx_username");
    		jmx_password = (String)jsonObj.get("jmx_password");
    		jmx_target = (String)jsonObj.get("jmx_target");
    		jmx_domain = (String)jsonObj.get("jmx_domain");
    		
    	}catch(Exception e){
    		e.printStackTrace();
    		Config.getLogger().severe(e.getMessage());    		
    		rv.setResult("params parsing error");
		}
    	
		try{
    	
			Hashtable<String, String> env = new Hashtable<String, String>();
    		env.put(Context.INITIAL_CONTEXT_FACTORY, "jeus.jndi.JNSContextFactory");
            env.put(Context.PROVIDER_URL, jmx_url);
            env.put(Context.SECURITY_PRINCIPAL, jmx_username);
            env.put(Context.SECURITY_CREDENTIALS, jmx_password);

            InitialContext ctx = new InitialContext(env);

            for(Entry<?, ?> a : ctx.getEnvironment().entrySet()){
            	Config.getLogger().fine("ctx 2 : "+a.getKey()+":"+a.getValue());
            	//System.out.println("ctx 2 : "+a.getKey()+":"+a.getValue());
            }
            
            connector = (JMXConnector)ctx.lookup("mgmt/rmbs/" + jmx_target);
            MBeanServerConnection mbeanServer = connector.getMBeanServerConnection();
            ObjectName objectName = new ObjectName("JEUS:j2eeType=J2EEDomain,JMXManager=" + jmx_target + ",name=" + jmx_domain);
            J2EEDomainMBean domainMBean = MBeanServerInvocationHandler.newProxyInstance(mbeanServer, objectName, J2EEDomainMBean.class, false);

            StringBuilder json = new StringBuilder();
            
            //json.append("{\"servers\":[");
            json.append("[");
            List<String> servers = domainMBean.getServerListFromDescriptor();
            for(String s : servers){
            	//System.out.println("Server Name :" + s + ":" + domainMBean.getServerState(s));
        		json.append("{\"server_name\":\""+s+"\",");
                json.append("\"status\":\""+domainMBean.getServerState(s)+"\"}");
        		//json.append("\""+ s + "__" + domainMBean.getServerState(s)+"\"");
                if(s!=servers.get(servers.size()-1)){
                	json.append(",");
                }
			}
            //json.append("]}");
            json.append("]");
            rv.setResult(json.toString());
            rv.setObjectAggregationKey(Config.getAgent_id()+"__"+jmx_domain);
            rv.setOk(true);
            connector.close();
            
    	}catch(javax.naming.ServiceUnavailableException e){
    		Config.getLogger().log(Level.WARNING, e.getMessage(), e);
    		rv.setResult("jmx connection error : " + jmx_url);
    	}catch(Exception e){
    		Config.getLogger().log(Level.WARNING, e.getMessage(), e);
    		rv.setResult("jmx error");
    		if(connector != null){ 
    			connector.close();
    		}
		}
		
    	return Common.makeOneResultArray(rv, command);
    	
	}

}
