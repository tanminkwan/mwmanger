package mwmanger;

import java.util.logging.Level;

import mwmanger.common.Config;

public class ShutdownThread extends Thread {

	@Override
	public void run() {
		
        Config config = Config.getInstance();
        config.getLogger().info("Shutdown Started");
		System.out.println("Shutdown Started");
        
        if (Thread.activeCount() > 2) {
        	
            for(Thread t : Thread.getAllStackTraces().keySet()){
                config.getLogger().fine("THREAD :"+t.getName()+ " Is Deamon : "+ t.isDaemon()+ " Is Alive : " +t.isAlive());
            }

            try {        	
                Thread.sleep(15000);
            } catch (InterruptedException e) {
                Config.getLogger().log(Level.WARNING, e.getMessage(), e);
            }
        }
            
		System.out.println("Shutdown End");
		Config.getLogger().info("Shutdown End");
	}
}
