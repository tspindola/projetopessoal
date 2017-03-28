package com.bravado.bsh;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;

import org.jpos.bsh.BSHRequestListener;
import org.jpos.core.Configuration;
import org.jpos.core.ConfigurationException;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOSource;
import org.jpos.iso.MUX;
import org.jpos.iso.packager.ISO93APackager;
import org.jpos.util.NameRegistrar;
import org.jpos.util.NameRegistrar.NotFoundException;

import br.listofacil.AcquirerSettings;
import br.listofacil.acquirer.ListoMessage;
import br.listofacil.tefserver.iso.ISO87APackagerGP;
import bsh.EvalError;
import bsh.Interpreter;

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
		try {			
			
			ListoMessage listoMessage = new ListoMessage();
			ISOMsg response = listoMessage.getResponseMessage(m);
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
			source.send(response);
			
			//Registra os NSUs
			AcquirerSettings.writeDataFile();

		} catch (Exception e) {
			warn(e);
			return false;
		}
		return true;
	}
		
}
