package mwmanger;

import java.util.logging.Level;

import mwmanger.common.Config;

public class ShutdownThread extends Thread {

	@Override
	public void run() {
		
		Config.getLogger().info("Shutdown Started");
		System.out.println("Shutdown Started");
        
        if (Thread.activeCount() > 2) {
        	
            for(Thread t : Thread.getAllStackTraces().keySet()){
                Config.getLogger().fine("THREAD :"+t.getName()+ " Is Deamon : "+ t.isDaemon()+ " Is Alive : " +t.isAlive());
            }

            try {        	
                Thread.sleep(15000);
            } catch (InterruptedException e) {
                Config.getLogger().log(Level.WARNING, e.getMessage(), e);
            }
        }
        /*
        while (Thread.activeCount() > 2) {
        	System.out.println("Thread Count :"+Thread.activeCount());	
			logger.info("Thread Count :"+Thread.activeCount());
			Thread.getAllStackTraces().keySet().forEach((t)-> System.out.println("THREAD :"+t.getName()+ " Is Deamon : "+ t.isDaemon()+ " Is Alive : " +t.isAlive()));
    		   
 	       try {        	
 	    	   Thread.sleep(1000);
	       } catch (InterruptedException e) {
	           e.printStackTrace();
	           logger.log(Level.SEVERE, e.getMessage(), e);
	       }
        }
        */	            
		System.out.println("Shutdown End");
		Config.getLogger().info("Shutdown End");
	}
}
