package br.listofacil;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.MUX;
import org.jpos.util.LogEvent;
import org.jpos.util.Logger;
import org.jpos.util.NameRegistrar;
import org.jpos.util.NameRegistrar.NotFoundException;

import com.bravado.util.RabbitMQ;

import br.listofacil.acquirer.BanrisulMessage;
import br.listofacil.acquirer.GlobalpaymentsMessage;
import br.listofacil.acquirer.ListoData;

public class AcquirerLoadTables implements Runnable {
	
	private final static String GLOBAL_PAYMENTS = "01";
	private final static String BANRISUL = "02";
	
	private Thread thrd_settings;
	private String acquirerSetted;
	private String logicalNumberSetted;
	private String terminalNumberSetted;
	private boolean forceInitialization;
	
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
			} catch (Exception e) {
				Logger.log(new LogEvent("Fail when stopping AcquirerLoadTables process"));
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			Logger.log(new LogEvent("Fail to run AdquirerLoadTables"));
		}
	    
	    thrd_settings = null;
    } 
    
}
