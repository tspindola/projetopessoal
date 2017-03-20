package br.listofacil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jpos.core.Configuration;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.MUX;
import org.jpos.util.NameRegistrar;
import org.jpos.util.NameRegistrar.NotFoundException;

import com.bravado.bsh.InfoTransaction;
import com.bravado.bsh.TextFile;

import br.listofacil.acquirer.BanrisulMessage;
import br.listofacil.acquirer.ListoData;
import br.listofacil.acquirer.ListoMessage;
import br.listofacil.tefserver.iso.ISO93EPackagerBanrisul;

public class AcquirerSettings {
	
	private final static String GLOBAL_PAYMENTS = "01";
	private final static String BANRISUL = "02";
	
	private static Map<String, ListoData> listoDataInitGlobalpayments = new HashMap<String, ListoData>();
	private static List<String> isLoadingListoDataInitGlobalpayments = new ArrayList<String>();

	private static Map<String, ListoData> listoDataInitBanrisul = new HashMap<String, ListoData>();
	private static List<String> isLoadingListoDataInitBanrisul = new ArrayList<String>();
	
	//NSU Acquirer and NSU TEF original
	private static HashMap<String, InfoTransaction> transactions = null;
	private static CommonFunctions common = new CommonFunctions();
	private static int nsu = 0;

	//COLOCAR UM NSU PARA CADA NUMERO LOGICO E ADQUIRENTE
	
	public static synchronized int getIncrementNSU(){
		//Incrementa o NSU
		return nsu++;
	}
	
	public static void loadLastNsu(){			
		int nsuaux = 1;
		Boolean flagnsu = false;

		Calendar nsucalendaraux = common.getCurrentDate();
		
		nsu = 1;
		
		try {
			
			TextFile txtFile = new TextFile(System.getProperty("user.dir") + "\\nsuconfig.data");
			
			txtFile.openTextFile();		
    		
    		while (txtFile.next()) 
    		{    			
    		    String line = txtFile.readLine();    		    
    		    String[] nsudata = line.split("[\"]");
    		    
    		    if (nsudata[0].contains("NSU")){
    		    	nsuaux = Integer.parseInt(nsudata[1]);
    		    	continue;
    		    }
    		    
    		    if (nsudata[0].contains("DATE")){   
    		    	if (nsucalendaraux.get(Calendar.DAY_OF_MONTH) == Integer.parseInt(nsudata[1].substring(0,  2)) &&
    		    	   (nsucalendaraux.get(Calendar.MONTH) + 1) == Integer.parseInt(nsudata[1].substring(2,  4)))
    		    	{
	    		    	flagnsu = true;
    		    	}
    		    	continue;
    		    }    		    
    		}
    		
    		if(flagnsu)
    		{
	    		nsu = nsuaux;
    		}
    		
    		txtFile.closeTextFile();	 
    		
		} catch (Exception e) {
			e.printStackTrace();
		}				
	}	
	
	public static synchronized boolean getStatusLoadingGlobalpayments(String logicalNumber){
		
		return isLoadingListoDataInitGlobalpayments.contains(logicalNumber);
	}
	
	public static synchronized void setStatusLoadingGlobalpayments(String logicalNumber){
		isLoadingListoDataInitGlobalpayments.add(logicalNumber);
	}
	
	public static synchronized void removeStatusLoadingGlobalpayments(String logicalNumber){
		isLoadingListoDataInitGlobalpayments.remove(logicalNumber);
	}
	
	public static synchronized boolean getStatusLoadingBanrisul(String logicalNumber){
		
		return isLoadingListoDataInitBanrisul.contains(logicalNumber);
	}
	
	public static synchronized void setStatusLoadingBanrisul(String logicalNumber){
		isLoadingListoDataInitBanrisul.add(logicalNumber);
	}
	
	public static synchronized void removeStatusLoadingBanrisul(String logicalNumber){
		isLoadingListoDataInitBanrisul.remove(logicalNumber);
	}
	
	public static boolean loadAcquirerTables(String acquirer, String logicalNumber, String terminalNumber) {
		boolean status = true;
		
		try {
			switch (acquirer) {
			case GLOBAL_PAYMENTS:
				if (!getStatusLoadingGlobalpayments(logicalNumber)){
					AcquirerProcess process = new AcquirerProcess();
					process.startProcess(acquirer, logicalNumber, terminalNumber);
				}
				break;
				
			case BANRISUL:
				if (!getStatusLoadingBanrisul(logicalNumber)){
					AcquirerProcess process = new AcquirerProcess();
					process.startProcess(acquirer, logicalNumber, terminalNumber);
				}
				break;

			default:
				status = false;
				break;
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return status;
	}
	
	/*
	public static void setResponseTransaction(ISOMsg msg) throws IOException {
		
		if (msg == null)
			return;		
	
		Calendar trsDate = common.getCurrentDate();
		
		try {
			
			switch (msg.getMTI()) {
			case "0210":
				//verifica se os bits 11, 38 e 39 estao presentes
				if (msg.hasField(11) && msg.hasField(39) && msg.hasField(127)) 
				{					
					//verifica se a transacao foi autorizada					
					if (msg.getString(39).equals("00")) 
					{
						//Limpa os registros caso seja data diferente
	    		    	if (currentDate.get(Calendar.DAY_OF_MONTH) != trsDate.get(Calendar.DAY_OF_MONTH) &&
	    	    		   (currentDate.get(Calendar.MONTH) + 1) != (trsDate.get(Calendar.MONTH) + 1))
	    		    		transactions.clear();
	    		    	
	    		    	InfoTransaction info = new InfoTransaction();
	    		    	info.mti = msg.getMTI();
	    		    	info.nsu_acquirer = msg.getString(127);
	    		    	info.nsu_listo = msg.getString(11);
	    		    	info.date_time = msg.getString(7);
	    		    	info.amount = msg.getString(4);
	    		    	
	    		    	transactions.put(info.nsu_acquirer, info);	
						
						//Grava no txt						
					}						
				}				
				break;

			default:
				break;
			}

		} catch (ISOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}
	
	public static InfoTransaction getInfoTransaction(String nsu_acquirer) {
		InfoTransaction info = null;
		if (transactions.containsKey(nsu_acquirer)) {
			info = transactions.get(nsu_acquirer);
		}
		return info;
	}
	*/
	
	public static synchronized void loadTransactions() {
		transactions = new HashMap<String, InfoTransaction>();
		
		//Carregar do arquivo TXT
		//Carregar as ultimas transacoes em memoria para que seja possivel estornar GP
	}
	
	public static synchronized boolean setInitializationTables(String acquirer, 
															   String logicalNumber, 
															   ListoData dataInitialization) {
		boolean status = true;
		
		try {
			switch (acquirer) {
			case GLOBAL_PAYMENTS:
				listoDataInitGlobalpayments.put(logicalNumber, dataInitialization);
				break;
				
			case BANRISUL:
				listoDataInitBanrisul.put(logicalNumber, dataInitialization);
				break;

			default:
				break;
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return status;
	}		
	
	public static synchronized ListoData getInitializationTables(String acquirer, 
																 String logicalNumber) {
		ListoData tables = null;
		
		try {
			switch (acquirer) {
			case GLOBAL_PAYMENTS:
				tables = listoDataInitGlobalpayments.get(logicalNumber);
				break;
				
			case BANRISUL:
				tables = listoDataInitBanrisul.get(logicalNumber);
				break;

			default:
				break;
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		
		return tables;
	}	
	
	
	
}