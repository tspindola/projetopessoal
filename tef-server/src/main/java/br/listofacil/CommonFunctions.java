package br.listofacil;

import java.math.BigInteger;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;

public class CommonFunctions {

	public Calendar getCurrentDate()
	{
		Date date = new Date();
		Calendar currentDate = new GregorianCalendar();
		currentDate.setTime(date);			
		
		return currentDate;
	}
	
	public String padRight(String str, int length, String padChar) {
		
		if(str.length() < length)
		{		    
			String pad = "";
		    for (int i = str.length(); i < length; i++) {
		        pad += padChar;
		    }
		    return str + pad;
		}
	    return str;
	}	
	
	public String padLeft(String str, int length, String padChar) {
		
		if(str.length() < length)
		{		    
			String pad = "";
		    for (int i = str.length(); i < length; i++) {
		        pad += padChar;
		    }
		    return pad + str;
		}
	    return str;
	}		
	
	public HashMap<String, String> tlvExtractData(String tlv)
	{
		HashMap<String, String> messages = new HashMap<String, String>();
		try {
			while(tlv.length() > 0)
			{
				String tag = tlv.substring(0, 3);
				String tam = tlv.substring(3, 6);
				int length = Integer.valueOf(tam);
				
				String msg = tlv.substring(6, 6 + length);
				messages.put(tag, msg);
				
				tlv = tlv.substring(length + 6, tlv.length());
			}
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			return null;
		}
		return messages;
	}
	
	public String stringValueToStringBinary(String value)
    {
	    String preBin = new BigInteger(value, 16).toString(2);
	    Integer length = preBin.length();
	    if (length < 8) {
	        for (int i = 0; i < 8 - length; i++) {
	            preBin = "0" + preBin;
	        }
	    }
	    return preBin;
    }
	
	public String convertHexToInt(String hex) {
		int value = Integer.parseInt(hex, 16);  
		return String.valueOf(value);
	}
}
