package br.listofacil;

import java.io.IOException;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISORequestListener;
import org.jpos.iso.ISOSource;
import org.jpos.iso.MUX;
import org.jpos.q2.Q2;
import org.jpos.util.Log;
import org.jpos.util.LogEvent;
import org.jpos.util.Logger;
import org.jpos.util.NameRegistrar;

public class ServerListener implements ISORequestListener {

	public static void main(String[] args) {
		Q2 q2 = new Q2();
		q2.start();
	}

	@Override
	public boolean process(ISOSource isoSrc, ISOMsg isoMsg) {

		
		try {
			// send request to server B
			MUX mux = (MUX) NameRegistrar.getIfExists("mux.jpos-client-mux");
			ISOMsg reply = mux.request(isoMsg, 30 * 1000);
			if (reply != null) {
				System.out.println(new String(reply.pack()));
				reply.set(125, "RESPONSE FROM SERVER A");
				isoSrc.send(reply);
			}
		} catch (ISOException | IOException e) {			
			LogEvent logEvent = new LogEvent();
			logEvent.addMessage("process - " + e);
			Logger.log(logEvent);
		}

		return false;
	}

}
