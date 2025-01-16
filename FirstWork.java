package mwmanger;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import mwmanger.common.Common;
import mwmanger.common.Config;
import mwmanger.kafka.MwConsumerThread;
import mwmanger.kafka.MwHealthCheckThread;
import mwmanger.kafka.MwProducer;

public class FirstWork {

	public long executeFirstCommands(JSONArray commands) {
		
		for(Object c : commands){
			
			JSONObject command = (JSONObject)c;
			Config.getLogger().info(command.toJSONString());
			
			String command_class  = (String)command.get("command_class");
			
			if(command_class.equals("BOOT")){

				String kafka_broker_address = (String)command.get("kafka_broker_address");
				
				if(kafka_broker_address != null && !kafka_broker_address.isEmpty()){
		    		   
					Config.getLogger().info("Kafka broker : "+ kafka_broker_address);
		            Config.setKafka_broker_address(kafka_broker_address);
		        	   
		            //Command consuming
		            MwConsumerThread consumerThread = new MwConsumerThread(Config.getKafka_broker_address(), "t_"+Config.getAgent_id());

		            consumerThread.setDaemon(true);
		            consumerThread.start();
		            consumerThread.setName("MwConsumer");
		   	
		            //Health checking
		            MwHealthCheckThread healthcheckThread = new MwHealthCheckThread(Config.getKafka_broker_address(), "t_agent_health");

		            healthcheckThread.setDaemon(true);
		            healthcheckThread.start();
		            healthcheckThread.setName("MwHealthCheck");
		               
		            //Producer Activate
		            MwProducer.getInstance();
		       		
		        }

			}
			
			Common.applyRefreshToken();
			
		}
        
		return 1;
		
	}
	
}
