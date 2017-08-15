package br.listofacil;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.util.LogEvent;
import org.jpos.util.Logger;

import br.listofacil.acquirer.BanrisulMessage;
import br.listofacil.acquirer.GlobalpaymentsMessage;
import br.listofacil.acquirer.ListoData;

public class AcquirerLogonProcess {
	
	private static CommonFunctions common = new CommonFunctions();
	
	private static Thread thrdLogon = null;
	private static ISOMsg messageLogon = null;
	private static Map<String, Calendar> dateLogon = new HashMap<String, Calendar>();
	
	
	public static synchronized void setDateLogon(String acquirer, Calendar date) {
		dateLogon.put(acquirer, date);
	}
	
	public static synchronized Calendar getDateLogon(String acquirer) {
		if (dateLogon.containsKey(acquirer))
			return dateLogon.get(acquirer);
		return null;
	}

	public static synchronized void setValidationConnection(ISOMsg message) {
		
		Calendar c = common.getCurrentDate();
		Calendar d = getDateLogon(message.getString(ListoData.FIELD_ACQUIRER_CODE));
		
		
		//Verifica se mudou o dia para envio do logon
		if ((d != null) && (d.get(Calendar.DAY_OF_YEAR) == c.get(Calendar.DAY_OF_YEAR)))
			return;
		
		Logger.log(new LogEvent("Start logon process... Acquirer ID: " + message.getString(ListoData.FIELD_ACQUIRER_CODE)));
		
		setDateLogon(message.getString(ListoData.FIELD_ACQUIRER_CODE), c);
		
		try {
			
			startProcess(message);
			
		} catch (Exception e) {
			Logger.log(new LogEvent("Fail when starting logon process"));
			stopProcess();
		}
	}
	
	private static void startProcess(ISOMsg message){
		messageLogon = message;
		
		if (thrdLogon == null) {
			thrdLogon = new Thread ();
			thrdLogon.start();
		}
	}
	
	private static void stopProcess(){
		if (thrdLogon != null) {
			try {
				thrdLogon.join();
				thrdLogon = null;
			} catch (Exception e) {
				Logger.log(new LogEvent("Fail when stopping logon process"));
			}
		}
	}
	
    private static void run() {
	    
	    try {
	    	if (messageLogon.hasField(ListoData.FIELD_ACQUIRER_CODE))
	    	{
				switch (messageLogon.getString(ListoData.FIELD_ACQUIRER_CODE)) {
				case ListoData.GLOBAL_PAYMENTS:
					GlobalpaymentsMessage globalpaymentsMessage = new GlobalpaymentsMessage();
					globalpaymentsMessage.requestLogonProcess(messageLogon);
					break;
					
				case ListoData.BANRISUL:
					BanrisulMessage banrisulMessage = new BanrisulMessage();
					banrisulMessage.requestLogonProcess(messageLogon);
					break;
	
				default:
					break;
				}			
	    	}
		} catch (Exception e) {
			Logger.log(new LogEvent("Fail when running logon process"));
		}
	    
	    thrdLogon = null;
    } 
}
