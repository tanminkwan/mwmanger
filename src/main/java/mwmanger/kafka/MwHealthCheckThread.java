package mwmanger.kafka;

import static mwmanger.common.Config.getConfig;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

import org.apache.kafka.common.TopicPartition;

public class MwHealthCheckThread extends Thread {
	
    private String brokerAddress;
    private String topic;

    public MwHealthCheckThread(String brokerAddress, String topic) {
        this.brokerAddress = brokerAddress;
        this.topic = topic;
    }
    
    @Override
    public void run() {	    	
    	
    	touchHealthTopic();
    	getConfig().getLogger().info("MwHealthCheckThread exit.");
		
    }
    
    private void touchHealthTopic(){
    	
    	Properties prop = new Properties();
    	prop.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, brokerAddress);
    	//prop.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
    	prop.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
    	//prop.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "2000");
    	prop.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());    	
    	prop.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
    	prop.put(ConsumerConfig.GROUP_ID_CONFIG, "g_h_"+getConfig().getAgent_id());
    	
    	KafkaConsumer<String, String> consumer = new KafkaConsumer<>(prop);
    	
    	//TopicPartition tp = new TopicPartition(topic, 0);
    	try{
    	
    		TopicPartition topicPartition = new TopicPartition(topic, 0);
    		List<TopicPartition> ltp = Arrays.asList(topicPartition);
    	
    		consumer.assign(ltp);
    		consumer.seekToEnd(ltp);
    		
    		do {
    			
    			
    			//long endOffset = consumer.position(topicPartition);
    			
    			//System.out.println("endOffset : "+Long.toString(endOffset));
    			
    			ConsumerRecords<String, String> recs = consumer.poll(Duration.ofSeconds(3));
    			
    			for (ConsumerRecord<String, String> rec : recs){
    				
    				System.out.println("current offset : "+Long.toString(rec.offset()));
    			}
    			
    			//consumer.poll(Duration.ofSeconds(2));
    			//Thread.sleep(30*1000);
    			//consumer.commitAsync();    			
    			
    		}while (true);
    		
    	}catch(Exception e){
    		getConfig().getLogger().log(Level.SEVERE, e.getMessage(), e);
    	}finally{
    		consumer.close();
    	}
    	
    }

}
