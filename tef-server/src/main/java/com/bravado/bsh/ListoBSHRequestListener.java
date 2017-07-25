package com.bravado.bsh;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.jpos.bsh.BSHRequestListener;
import org.jpos.core.Configuration;
import org.jpos.core.ConfigurationException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOSource;
import org.jpos.iso.MUX;
import org.jpos.util.NameRegistrar.NotFoundException;

import com.bravado.util.RabbitMQ;

import br.listofacil.AcquirerSettings;
import br.listofacil.acquirer.ListoMessage;

public class ListoBSHRequestListener extends BSHRequestListener {
	
	Configuration cfg;
	MUX mux;	

	private void initContextBsh() throws ConfigurationException, NotFoundException {		
		
		// contextBsh = new Interpreter();
		// String contextFile = cfg.get("context");
		// if (contextFile.isEmpty()) {
		// warn("No context file defined");
		// return;
		// }
		//
		// try {
		// contextBsh.source(contextFile);
		// } catch (IOException | EvalError e) {
		// throw new ConfigurationException("failed to interpret the context
		// file " + contextFile);
		// }
	}
	
	@Override
	public void setConfiguration(Configuration cfg) throws ConfigurationException {
		super.setConfiguration(cfg);
		// bshDefault = cfg.get("default");
		this.cfg = cfg;
		
		//Load data and NSU
		AcquirerSettings.loadLastNsu();
		
		//Colocar tratamento caso nao consiga conectar no host do BD
		try {
			RabbitMQ.Connect();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TimeoutException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// try {
		// mux = (MUX) NameRegistrar.get("mux." + cfg.get("mux"));
		// packager = new ISO87APackagerGP();
		// } catch (NotFoundException e1) {
		// // TODO Auto-generated catch block
		// e1.printStackTrace();
		// }
		//
		// try {
		// initContextBsh();
		// } catch (NotFoundException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }		
	}		

	// private String findScritpByMit(String mti) {
	// boolean scriptFound = false;
	// String script = null;
	// for (String aBshSource : bshSource) {
	// try {
	// int idx = aBshSource.indexOf(MTI_MACRO);
	// if (idx >= 0) {
	// script = aBshSource.substring(0, idx) + mti + aBshSource.substring(idx +
	// MTI_MACRO.length());
	// File file = new File(script);
	// if (file.exists()) {
	// scriptFound = true;
	// break;
	// }
	// }
	// } catch (Exception e) {
	// warn(e);
	// }
	// }
	// if (scriptFound) {
	// return script;
	// } else {
	// return null;
	// }
	// }

	@Override
	public boolean process(ISOSource source, ISOMsg m) {
		
		ListoMessage listoMessage = new ListoMessage();
		ISOMsg response = null; 
				
		try {			
			
			response = listoMessage.getResponseMessage(m);
			/*
			if (isomsg == null)
				response = controlMessage.getListoResponseMessage(m);
			else
			{
				mux = (MUX) NameRegistrar.get("mux." + cfg.get("mux"));
				response = mux.request(m, 30 * 1000);
				controlMessage.setResponseTransaction(response);
			}
			*/
			
			if (response != null)
				source.send(response);
			
			//Registra os NSUs
			AcquirerSettings.writeDataFile();

		} catch (Exception e) {
			listoMessage.SendUnmakingMessage(m, response);
			return false;
		}
		return true;
	}
		
}
