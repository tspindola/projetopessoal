package br.listofacil;

import java.util.HashMap;
import java.util.Map;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.MUX;
import org.jpos.util.NameRegistrar;
import org.jpos.util.NameRegistrar.NotFoundException;

import br.listofacil.acquirer.BanrisulMessage;
import br.listofacil.acquirer.GlobalpaymentsMessage;
import br.listofacil.acquirer.ListoData;

public class AcquirerProcess implements Runnable {
	
	private final static String GLOBAL_PAYMENTS = "01";
	private final static String BANRISUL = "02";
	
	private static Thread thrd_settings;
	private static String acquirerSetted;
	private static String logicalNumberSetted;
	private static String terminalNumberSetted;
	
	public AcquirerProcess() {
		// TODO Auto-generated method stub
		
	}

	public void startProcess(String acquirer, String logicalNumber, String terminalNumber){
		acquirerSetted = acquirer;
		logicalNumberSetted = logicalNumber;
		terminalNumberSetted = terminalNumber;
		
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
				// TODO Auto-generated catch block
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
				globalpaymentsMessage.loadTablesInitialization(logicalNumberSetted, terminalNumberSetted);
				break;
				
			case BANRISUL:
				BanrisulMessage banrisulMessage = new BanrisulMessage();
				banrisulMessage.loadTablesInitialization(logicalNumberSetted, terminalNumberSetted);
				break;

			default:
				break;
			}			
		} catch (ISOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    
	    thrd_settings = null;
    } 
    
}
