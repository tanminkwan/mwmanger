package mwagent.kafka;

import static mwagent.common.Config.getConfig;

import java.util.Properties;
import java.util.logging.Level;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

public final class MwProducer {

	private static MwProducer instance;
	private static KafkaProducer<String, String> producer = null;

	private MwProducer() {
		
		System.out.println("MwProducer !!" + getConfig().getKafka_broker_address());
		
    	Properties prop = new Properties();
    	prop.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, getConfig().getKafka_broker_address());
    	prop.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());    	
    	prop.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
    	
    	producer = new KafkaProducer<>(prop);
    	
	}

	public static int sendMessage(String topic, String key, String message){

		ProducerRecord<String, String> rec = new ProducerRecord<>(topic, key, message);
		try{

			producer.send(rec, (metadata, exception) -> {
				if (exception != null){
					
				}
			});
		}catch (Exception e){
			getConfig().getLogger().warning(String.format("Kafka producing error topic:%s key:%s message:%s",topic,key,message));
			getConfig().getLogger().log(Level.WARNING, e.getMessage(), e);
		}
		return 1;
	}
	
	public static KafkaProducer<String, String> getProducer() {
		return producer;
	}

	public static MwProducer getInstance(){
		if(instance == null){
			synchronized(MwProducer.class){
				if(instance == null){
					instance = new MwProducer();
				}
			}
		}
		return instance;
	}

	/**
	 * Producer를 정상적으로 종료합니다.
	 * Graceful shutdown을 위해 추가됨.
	 */
	public void close() {
		if (producer != null) {
			try {
				producer.flush();
				producer.close();
				getConfig().getLogger().info("Kafka producer closed successfully");
			} catch (Exception e) {
				getConfig().getLogger().log(Level.WARNING, "Error closing Kafka producer", e);
			}
		}
	}

}
