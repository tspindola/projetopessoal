package br.listofacil;

import org.jpos.iso.ISOException;
import org.jpos.util.LogEvent;
import org.jpos.util.Logger;

import br.listofacil.acquirer.BanrisulMessage;
import br.listofacil.acquirer.GlobalpaymentsMessage;

public class AcquirerLoadTables implements Runnable {
	
	private final static String GLOBAL_PAYMENTS = "01";
	private final static String BANRISUL = "02";
	
	private static Thread thrd_settings;
	private static String acquirerSetted;
	private static String logicalNumberSetted;
	private static String terminalNumberSetted;
	private static boolean forceInitialization;
	
	public AcquirerLoadTables() {
		// TODO Auto-generated method stub
		
	}

	public void startProcess(String acquirer, String logicalNumber, String terminalNumber, boolean force){
		acquirerSetted = acquirer;
		logicalNumberSetted = logicalNumber;
		terminalNumberSetted = terminalNumber;
		forceInitialization = force;
		
		if (thrd_settings == null) {
			thrd_settings = new Thread (this);
			thrd_settings.start();
		}
	}
	
	public void stopProcess(){
		if (thrd_settings != null) {
			try {
				thrd_settings.join();
				thrd_settings = null;
			} catch (InterruptedException e) {
				Logger.log(new LogEvent(
						"Error: br.listofacil.acquirer.AcquirerLoadTables.stopProcess \n " + e.getMessage()));
				e.printStackTrace();
			}
		}
	}
	
    public void run() {
	    System.out.println("Start settings process...");
	    try {
			switch (acquirerSetted) {
			case GLOBAL_PAYMENTS:
				GlobalpaymentsMessage globalpaymentsMessage = new GlobalpaymentsMessage();
				globalpaymentsMessage.loadTablesInitialization(logicalNumberSetted, terminalNumberSetted, forceInitialization);
				break;
				
			case BANRISUL:
				BanrisulMessage banrisulMessage = new BanrisulMessage();
				banrisulMessage.loadTablesInitialization(logicalNumberSetted, terminalNumberSetted, forceInitialization);
				break;

			default:
				break;
			}			
		} catch (ISOException e) {
			Logger.log(new LogEvent(
					"Error: br.listofacil.acquirer.AcquirerLoadTables.run \n " + e.getMessage()));
			e.printStackTrace();
		} catch (Exception e) {
			Logger.log(new LogEvent(
					"Error: br.listofacil.acquirer.AcquirerLoadTables.run \n " + e.getMessage()));
			e.printStackTrace();
		}
	    
	    thrd_settings = null;
    } 
    
}
