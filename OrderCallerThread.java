package mwmanger;

import java.util.logging.Level;

import org.json.simple.JSONObject;

import mwmanger.common.Config;
import mwmanger.order.OrderCaller;

public class OrderCallerThread extends Thread {
	
    private String command_class;
    private JSONObject command;

    public OrderCallerThread(String command_class, JSONObject command) {
        this.command_class = command_class;
        this.command = command;
    }
    
    @Override
    public void run() {	    	
    	
    	OrderCaller.executeOrder(command_class, command);

    	try {
    		Thread.sleep(500);
    	} catch (InterruptedException e) {
    	    Config.getInstance().getLogger().log(Level.WARNING, e.getMessage(), e);
    	}

    	Config.getInstance().getLogger().info(command_class + " Thread exit.");

    }

}
