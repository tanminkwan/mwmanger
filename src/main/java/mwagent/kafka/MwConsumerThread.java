package mwagent.kafka;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.logging.Level;

import org.apache.commons.codec.binary.StringUtils;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import mwagent.OrderCallerThread;
import static mwagent.common.Config.getConfig;

public class MwConsumerThread extends Thread {
	
    private String brokerAddress;
    private String topic;
    private static final String FIN_MESSAGE = "FIN";
    private boolean stopRequested = false;

    public MwConsumerThread(String brokerAddress, String topic) {
        this.brokerAddress = brokerAddress;
        this.topic = topic;
    }
    
    @Override
    public void run() {	    	
    	
    	executeOrderFromKafka();
    	getConfig().getLogger().info("MwConsumerThread exit.");
		
    }
    
    public void stop_thread() {
    	stopRequested = true;
    }
    
    private void executeOrderFromKafka(){
    	
    	Properties prop = new Properties();
    	prop.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, brokerAddress);
    	//prop.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    	prop.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
    	prop.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());    	
    	prop.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
    	prop.put(ConsumerConfig.GROUP_ID_CONFIG, "g_"+getConfig().getAgent_id());
    	
    	KafkaConsumer<String, String> consumer = new KafkaConsumer<>(prop);
    	consumer.subscribe(Collections.singletonList(topic));
    	
    	String message = null;
    	
    	try{
    		
    		do {
    			
    			ConsumerRecords<String, String> recs = consumer.poll(Duration.ofSeconds(3));
    			
    			for (ConsumerRecord<String, String> rec : recs){
    				
    				message = rec.value();
    				consumer.commitAsync();
    				
    				JSONObject command_ =  (JSONObject) new JSONParser().parse(message);
 	    		   	String command_class  = (String)command_.get("command_class");
 	    		    
 	    		    OrderCallerThread thread = new OrderCallerThread("mwagent.order."+command_class, command_);

 	    		    thread.setDaemon(true);
 	    		    thread.start();	    		   
 	    		   
 	    		   getConfig().getLogger().info("Order called by Kafka :"+ Long.toString(rec.offset()) + " key : "+ rec.key() + "_" + message);
    				
    			}
    			
    		}while (!StringUtils.equals(message, FIN_MESSAGE) && !stopRequested);
    		
    	}catch(ParseException e){
    		getConfig().getLogger().log(Level.SEVERE, e.getMessage(), e);
    	}catch(Exception e){
    		getConfig().getLogger().log(Level.SEVERE, e.getMessage(), e);
    	}finally{
    		consumer.close();
    	}
    	
    }

}
