package br.listofacil;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jpos.util.LogEvent;
import org.jpos.util.Logger;

import com.bravado.bsh.InfoTransaction;
import com.bravado.bsh.TextFile;

import br.listofacil.acquirer.ListoData;

public class AcquirerSettings {
	
	private final static String GLOBAL_PAYMENTS = "01";
	private final static String BANRISUL = "02";
	
	private static Map<String, ListoData> listoDataInitGlobalpayments = new HashMap<String, ListoData>();
	private static List<String> isLoadingListoDataInitGlobalpayments = new ArrayList<String>();

	private static Map<String, ListoData> listoDataInitBanrisul = new HashMap<String, ListoData>();
	private static List<String> isLoadingListoDataInitBanrisul = new ArrayList<String>();

	private static Map<String, List<BinData>> banrisulBinsTable = new HashMap<String, List<BinData>>();
	private static Map<String, List<BinData>> globalpaymentsBinsTable = new HashMap<String, List<BinData>>();
	
	private static Map<String, Map<String, String>> banrisulFlagsTable = new HashMap<String, Map<String, String>>();
	private static Map<String, Map<String, String>> globalpaymentsFlagsTable = new HashMap<String, Map<String, String>>();

	// NSU Acquirer and NSU TEF original
	private static HashMap<String, InfoTransaction> transactions = null;
	private static CommonFunctions common = new CommonFunctions();
	
	private static long nsuBanrisul = 1;
	private static long nsuGlobalpayments = 1;
	
	private static String dateNsuLastTransaction = "0000000000000000";
	
	private static String dateDataUpdate = new String();
	private static boolean isNSUOdd;
	private static String nsuOdd;
	private static String byte_1;
	private static String byte_2;
	private static String byte_3;
	
	public static synchronized void writeDataFile(){
		
		Calendar cal = common.getCurrentDate();

		String dateReg = common.padLeft(String.valueOf(cal.get(Calendar.DAY_OF_MONTH)), 2, "0")
				+ common.padLeft(String.valueOf(cal.get(Calendar.MONTH) + 1), 2, "0")
				+ String.valueOf(cal.get(Calendar.YEAR));

		String register = "NSU_GP=\"" + nsuGlobalpayments + "\"\n" + "NSU_BA=\"" + nsuBanrisul + "\"\n"
				+ "DATE_UPDATE_NSU=\"" + dateReg + "\"\n" + "NSU_ODD=\"" + nsuOdd + "\"\n" + "CONFIG_BYTE_1=\"" + byte_1
				+ "\"\n" + "CONFIG_BYTE_2=\"" + byte_2 + "\"\n" + "CONFIG_BYTE_3=\"" + byte_3 + "\"\n";

		try {

	        FileWriter fw = new FileWriter(System.getProperty("user.dir") + "/nsuconfig.data");
	        BufferedWriter bw = new BufferedWriter(fw);
	        bw.write(register);
	        bw.close();
			
		} catch (IOException e) {
			Logger.log(new LogEvent(
					"Error: br.listofacil.AcquirerSettings.writeDataFile \n " + e.getMessage()));
			e.printStackTrace();
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
		
		nsuBanrisul++;
		
		if(isNSUOdd){
			if (nsuBanrisul % 2 == 0){
				nsuBanrisul++;
			}
		} else {
			if (nsuBanrisul % 2 != 0){
				nsuBanrisul++;
			}
		}
		return nsuBanrisul;
	}
	
	public static synchronized long getIncrementNSUGlobalpayments(){
		//Incrementa o NSU
		nsuGlobalpayments++;

		if(isNSUOdd){
			if (nsuGlobalpayments % 2 == 0){
				nsuGlobalpayments++;
			}
		} else {
			if (nsuGlobalpayments % 2 != 0){
				nsuGlobalpayments++;
			}
		}
		return nsuGlobalpayments;
	}
	
	public static synchronized boolean isNSUOdd(){
		return isNSUOdd;
	}
	
	public static synchronized void loadLastNsu(){			
		int nsuGP = 1;
		int nsuBA = 1;
		Boolean flagnsu = false;

		Calendar nsucalendaraux = common.getCurrentDate();
		
		nsuBanrisul = 1;
		nsuGlobalpayments = 1;
		
		try {		
			
			TextFile txtFile = new TextFile(System.getProperty("user.dir") + "/nsuconfig.data");
			
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
    		    	if (nsucalendaraux.get(Calendar.DAY_OF_MONTH) != Integer.parseInt(nsudata[1].substring(0,  2)) &&
    		    	   (nsucalendaraux.get(Calendar.MONTH) + 1) != Integer.parseInt(nsudata[1].substring(2,  4))) {
	    		    	flagnsu = false;
    		    	}
    		    	setDateDataUpdate(nsudata[1]);
    		    	continue;
    		    }    	
    		    
    		    if (nsudata[0].contains("NSU_ODD")){
    		    	setNsuOdd(nsudata[1]);
    		    	if (getNsuOdd().equals("0")){
    		    		isNSUOdd = false;
    		    	} else {
    		    		isNSUOdd = true;
    		    	}
    		    	continue;
    		    }
    		    if (nsudata[0].contains("CONFIG_BYTE_1")){
    		    	setByte_1(nsudata[1]);
    		    	continue;
    		    }
    		    if (nsudata[0].contains("CONFIG_BYTE_2")){
    		    	setByte_2(nsudata[1]);
    		    	continue;
    		    }
    		    if (nsudata[0].contains("CONFIG_BYTE_3")){
    		    	setByte_3(nsudata[1]);
    		    	continue;
    		    }
    		}
    		
    		if(flagnsu) {
    			nsuGlobalpayments = nsuGP;
	    		nsuBanrisul = nsuBA;
    		}
    		
    		txtFile.closeTextFile();	 
    		
		} catch (Exception e) {
			Logger.log(new LogEvent(
					"Error: br.listofacil.AcquirerSettings.loadLastNsu \n " + e.getMessage()));
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
			Logger.log(new LogEvent(
					"Error: br.listofacil.AcquirerSettings.loadAcquirerTables return false \n " + e.getMessage()));
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
			Logger.log(new LogEvent(
					"Error: br.listofacil.AcquirerSettings.setInitializationTables return false \n " + e.getMessage()));
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
			Logger.log(new LogEvent(
					"Error: br.listofacil.AcquirerSettings.getInitializationTables return null \n " + e.getMessage()));
			e.printStackTrace();
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

	public static synchronized String getNsuType() {
		return nsuOdd;
	}

	public static synchronized void setNsuType(String nsuType) {
		AcquirerSettings.nsuOdd = nsuType;
	}

	public static HashMap<String, InfoTransaction> getTransactions() {
		return transactions;
	}

	public static void setTransactions(HashMap<String, InfoTransaction> transactions) {
		AcquirerSettings.transactions = transactions;
	}

	public static synchronized String getByte_1() {
		return byte_1;
	}

	public static synchronized void setByte_1(String byte_1) {
		AcquirerSettings.byte_1 = byte_1;
	}

	public static synchronized String getByte_2() {
		return byte_2;
	}

	public static synchronized void setByte_2(String byte_2) {
		AcquirerSettings.byte_2 = byte_2;
	}

	public static synchronized String getByte_3() {
		return byte_3;
	}

	public static synchronized void setByte_3(String byte_3) {
		AcquirerSettings.byte_3 = byte_3;
	}

	public static synchronized String getNsuOdd() {
		return nsuOdd;
	}

	public static synchronized void setNsuOdd(String nsuOdd) {
		AcquirerSettings.nsuOdd = nsuOdd;
	}

	public static synchronized BinData getBanrisulBinsTable(String logicalNumber, String bin) {

		try {
			if (AcquirerSettings.banrisulBinsTable.containsKey(logicalNumber)) {
				List<BinData> bins = AcquirerSettings.banrisulBinsTable.get(logicalNumber);
				
				for (BinData binData : bins) {
					
					long lngBin = Long.parseLong(bin);

					if (lngBin >= binData.getInitial() && lngBin <= binData.getEnd())
						return binData;
				}
			}
		} catch (Exception e) {
			// TODO: handle exception
			Logger.log(new LogEvent(
					"Error: br.listofacil.AcquirerSettings.getBanrisulBinsTable return null \n " + e.getMessage()));
		}

		return null;
	}

	public static synchronized void setBanrisulBinsTable(String logicalNumber, BinData binData) {

		try {
			List<BinData> bins = new ArrayList<BinData>();

			if (AcquirerSettings.banrisulBinsTable.containsKey(logicalNumber)) {

				bins = AcquirerSettings.banrisulBinsTable.get(logicalNumber);

				if (!bins.contains(binData)) {
					bins.add(binData);
					AcquirerSettings.banrisulBinsTable.put(logicalNumber, bins);
				}				

			} else {

				bins.add(binData);

				AcquirerSettings.banrisulBinsTable.put(logicalNumber, bins);
			}

		} catch (Exception e) {
			// TODO: handle exception
			Logger.log(new LogEvent(
					"Error: br.listofacil.AcquirerSettings.setBanrisulBinsTable \n " + e.getMessage()));
		}

	}

	public static synchronized Map<String, List<BinData>> getGlobalpaymentsBinsTable() {
		return globalpaymentsBinsTable;
	}

	public static synchronized void setGlobalpaymentsBinsTable(
			Map<String,List<BinData>> globalpaymentsBinsTable) {
		AcquirerSettings.globalpaymentsBinsTable = globalpaymentsBinsTable;
	}

	public static String getBanrisulFlagName(String logicalNumber, String flagCode) {
		String flag = "";
		
		if (banrisulFlagsTable.containsKey(logicalNumber)) {
			Map<String, String> tables = banrisulFlagsTable.get(logicalNumber);
			if (tables.containsKey(flagCode))
				flag = tables.get(flagCode);
		}
					
		return flag;		
	}

	public static void setBanrisulFlagsTable(String logicalNumber, String flagCode, String flag) {
		Map<String, String> table = new HashMap<String, String>();		
		
		if (banrisulFlagsTable.containsKey(logicalNumber)) {
			table = banrisulFlagsTable.get(logicalNumber);
			table.put(flagCode, flag);
		} else { 
			table.put(flagCode, flag);
		}
			
		banrisulFlagsTable.put(logicalNumber, table);						
	}

	public static Map<String, Map<String, String>> getGlobalpaymentsFlagName() {
		return globalpaymentsFlagsTable;
	}

	public static void setGlobalpaymentsFlagsTable(Map<String, Map<String, String>> globalpaymentsFlagsTable) {
		AcquirerSettings.globalpaymentsFlagsTable = globalpaymentsFlagsTable;
	}

}
