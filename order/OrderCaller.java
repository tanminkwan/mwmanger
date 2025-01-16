package mwmanger.order;

import java.lang.reflect.Constructor;
import java.util.logging.Level;

import org.json.simple.JSONObject;

import mwmanger.common.Config;

public final class OrderCaller {
	
	public static int executeOrder(String orderedClass, JSONObject command){
    	
    	try {

    		Config.getLogger().info("orderedClass : "+orderedClass);
    		//- Order order = (Order) Class.forName(orderedClass).getDeclaredConstructor().newInstance(command);
    		Class<?> order = Class.forName(orderedClass);
    		Constructor<?> orderConstructor = order.getConstructor(JSONObject.class);
    		
    		//Create Order Object & deliver a command 
    		Order orderObj = (Order)orderConstructor.newInstance(command);
    		
    		//Execute command & Make Results
    		int rtn = orderObj.execute();
    		
    		//Send results to Server(MW Server or KAFKA)
    		if(rtn>0){
    			orderObj.sendResults();
    		}else{
    			Config.getLogger().warning("executeOrder failed. orderedClass : "+orderedClass);
    		}
    		
        	Config.getLogger().info("finished OrderCaller.");
        	
    	}catch (ClassNotFoundException e) {
    		Config.getLogger().log(Level.WARNING, e.getMessage(), e);
    		return -2;
    	}catch (Exception e) {
    		Config.getLogger().log(Level.WARNING, e.getMessage(), e);    		
    		return -3;
    	}    	
    	
    	return 1;
    }

}
