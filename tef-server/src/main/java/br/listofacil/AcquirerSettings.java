package br.listofacil;

import java.io.BufferedWriter;
import java.io.FileWriter;
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
import org.jpos.util.LogEvent;
import org.jpos.util.Logger;
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
	
	private static long nsuBanrisul = 1;
	private static long nsuGlobalpayments = 1;
	
	private static String dateNsuLastTransaction = "0000000000000000";
	
	private static String dateDataUpdate = new String();
	private static boolean isNSUOdd = true; //impar
	private static String nsuType = "";
	
	public static synchronized void writeDataFile(){
		
		Calendar cal = common.getCurrentDate();
		
		String dateReg = common.padLeft(String.valueOf(cal.get(Calendar.DAY_OF_MONTH)), 2, "0") + 
						 common.padLeft(String.valueOf(cal.get(Calendar.MONTH) + 1), 2, "0") +
						 String.valueOf(cal.get(Calendar.YEAR));	
		
		String register = "NSU_GP=\"" + nsuGlobalpayments + "\"\n" +
						  "NSU_BA=\"" + nsuBanrisul + "\"\n" +
						  "DATE_UPDATE_NSU=\"" + dateReg + "\"\n" +
						  "NSU_TYPE=\"" + dateReg + "\"\n";
		try {

	        FileWriter fw = new FileWriter(System.getProperty("user.dir") + "\\nsuconfig.data");
	        BufferedWriter bw = new BufferedWriter(fw);
	        bw.write(register);
	        bw.close();
			
		} catch (IOException e) {
			Logger.log(new LogEvent("Fail when writing DataFile"));
		}
	}
	
	public static synchronized String getDateDataUpdate(){
		return dateDataUpdate;
	}
	
	public static synchronized void setDateDataUpdate(String date){
		dateDataUpdate = date;
	}
	
	public static synchronized long getIncrementNSUBanrisul(){
		//Incrementa o NSU
		return nsuBanrisul++;
	}
	
	public static synchronized long getIncrementNSUGlobalpayments(){
		//Incrementa o NSU
		long nsu = nsuGlobalpayments++;

		return nsu;
	}
	
	public static synchronized boolean isNSUOdd(){
		return isNSUOdd;
	}
	
	public static synchronized void loadLastNsu(){			
		int nsuGP = 1;
		int nsuBA = 1;
		//Boolean flagnsu = false;

		Calendar nsucalendaraux = common.getCurrentDate();
		
		nsuBanrisul = 1;
		nsuGlobalpayments = 1;
		
		try {		
			
			TextFile txtFile = new TextFile(System.getProperty("user.dir") + "\\nsuconfig.data");

			txtFile.openTextFile();
    		
    		while (txtFile.next()) 
    		{    			
    		    String line = txtFile.readLine();    		    
    		    String[] nsudata = line.split("[\"]");
    		    
    		    if (nsudata[0].contains("NSU_GP")){
    		    	nsuGP = Integer.parseInt(nsudata[1]);
    		    	continue;
    		    }
    		    
    		    if (nsudata[0].contains("NSU_BA")){
    		    	nsuBA = Integer.parseInt(nsudata[1]);
    		    	continue;
    		    }
    		    
    		    if (nsudata[0].contains("DATE_UPDATE_NSU")){   
    		    	//if (nsucalendaraux.get(Calendar.DAY_OF_MONTH) != Integer.parseInt(nsudata[1].substring(0,  2)) &&
    		    	//   (nsucalendaraux.get(Calendar.MONTH) + 1) != Integer.parseInt(nsudata[1].substring(2,  4))) {
	    		    //	flagnsu = false;
    		    	//}
    		    	setDateDataUpdate(nsudata[1]);
    		    	continue;
    		    }    	
    		    
    		    if (nsudata[0].contains("NSU_TYPE")){
    		    	nsuType = nsudata[1];
    		    	if (nsudata[1].toUpperCase().equals("PAR"));
    		    		isNSUOdd = false;
    		    	continue;
    		    }
    		}
    		
    		//if(flagnsu) {
    		//	nsuGlobalpayments = nsuGP;
	    	//	nsuBanrisul = nsuBA;
    		//}
    		
    		txtFile.closeTextFile();	 
    		
		} catch (Exception e) {
			Logger.log(new LogEvent("Fail when loading last NSU"));
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
	
	public static boolean loadAcquirerTables(String acquirer, String logicalNumber, String terminalNumber, boolean force) {
		boolean status = true;
		
		try {
			switch (acquirer) {
			case GLOBAL_PAYMENTS:
				if ((!getStatusLoadingGlobalpayments(logicalNumber)) || force){
					AcquirerLoadTables process = new AcquirerLoadTables();
					process.startProcess(acquirer, logicalNumber, terminalNumber, force);
				}
				break;
				
			case BANRISUL:
				if ((!getStatusLoadingBanrisul(logicalNumber)) || force){
					AcquirerLoadTables process = new AcquirerLoadTables();
					process.startProcess(acquirer, logicalNumber, terminalNumber, force);
				}
				break;

			default:
				status = false;
				break;
			}
			
		} catch (Exception e) {
			Logger.log(new LogEvent("Fail when loading acquires tables"));
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
	
	public static synchronized boolean setInitializationTables(String acquirer, 
															   String logicalNumber, 
															   ListoData dataInitialization) {
		boolean status = true;
		
		try {
			switch (acquirer) {
			case GLOBAL_PAYMENTS:
				if (listoDataInitGlobalpayments.containsKey(logicalNumber))
					listoDataInitGlobalpayments.remove(logicalNumber);
				listoDataInitGlobalpayments.put(logicalNumber, dataInitialization);
				break;
				
			case BANRISUL:
				if (listoDataInitBanrisul.containsKey(logicalNumber))
					listoDataInitBanrisul.remove(logicalNumber);
				listoDataInitBanrisul.put(logicalNumber, dataInitialization);
				break;

			default:
				break;
			}
			
		} catch (Exception e) {
			Logger.log(new LogEvent("Fail when setting initialization tables"));
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
			Logger.log(new LogEvent("Fail when getting initialization tables"));
			return null;
		}
		
		return tables;
	}	
	

	
	public static synchronized void setDateNsuLastTransactionOk(String date, String nsu){
		dateNsuLastTransaction = date + nsu;
	}
	
	public static synchronized String getDateNsuLastTransactionOk(){
		return dateNsuLastTransaction;
	}
	
}
