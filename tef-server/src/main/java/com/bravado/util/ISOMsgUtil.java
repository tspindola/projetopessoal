package com.bravado.util;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.space.Space;
import org.jpos.util.LogEvent;
import org.jpos.util.Logger;

//import net.hairi.Thales.HThalesCore;

public class ISOMsgUtil {

	Space sp;
    //HThalesCore channel;
    String in, out, ready, reconnect;
    long delay;
    long keepAliveInterval=30000;
    String basePath;
    String host;
    int port;
    boolean keepAlive = false;
    boolean ignoreISOExceptions = false;
    int rx, tx, connects;
    long lastTxn = 0l;
	
	
	private static int absents[];
	static String str_absents="";

	public static void copyFields(ISOMsg srcMsg, ISOMsg dstMsg, int fields[]) {
        for (int field : fields) {
            if (srcMsg.hasField(field)) {
                try {
                    dstMsg.set(srcMsg.getComponent(field));
                } catch (ISOException e) {
        			LogEvent logEvent = new LogEvent();
        			logEvent.addMessage("copyFields - " + e);
        			Logger.log(logEvent);
                }
            }
        }
	}
	
	//------------------------------------------------------------------------------------------------------
	public static String verifyMandatoryFields(ISOMsg srcMsg, int fields[]) throws ISOException {
		
		str_absents="";
		LogEvent evt = new LogEvent(); 
		
		for (int field : fields) {			
            if ( ! srcMsg.hasField(field)) {
          /*  	LogEvent evt = new LogEvent(); 
    			evt.addMessage("Nao contem field " + field);
    			Logger.log(evt);
    	*/		
                str_absents = str_absents + " " + field;
            }
		}
			
        if (str_absents != "") {
        	evt.addMessage("Nao contem campo(s) " + str_absents);
        	Logger.log(evt);
        }
	
        return str_absents;
	}

	//------------------------------------------------------------------------------------------------------
	public static int[] verifyMandatoryFieldsOrig(ISOMsg srcMsg, int fields[]) throws ISOException {
		
		int ia = 1;
		
//		try {
			for (int field : fields) {
                if ( ! srcMsg.hasField(field)) {
	            	LogEvent evt = new LogEvent(); 
	    			evt.addMessage("Nao contem field " + field);
                	Logger.log(evt);
                	Logger getLog = Logger.getLogger("info");
                    getLog.log(evt);
                    
	                absents[ia] = field;
					ia=ia+1;
	            }
			}
//       } catch (Exception e) {
//        	LogEvent evt = new LogEvent(); 
//       	evt.addMessage("Erro VerifyMandatoryFields - " + e);
//			Logger.log(evt);
//        	Logger getLog = Logger.getLogger("warn");
//          getLog.log(evt);
//        }
	
        return absents;
	}

}
