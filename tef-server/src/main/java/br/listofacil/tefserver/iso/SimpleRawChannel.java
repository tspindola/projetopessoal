package br.listofacil.tefserver.iso;

import java.io.IOException;

import org.jpos.iso.ISOException;
import org.jpos.iso.channel.RawChannel;

public class SimpleRawChannel extends RawChannel {
	@Override
	protected void sendMessageLength(int len) throws IOException {
		// 2 bytes - big endian
		serverOut.write(len >> 8);
		serverOut.write(len);
	}

	@Override
	protected int getMessageLength() throws IOException, ISOException {
		byte[] b = new byte[2];
		serverIn.readFully(b, 0, 2);
		return ((((int) b[0]) & 0xFF) << 8) | (((int) b[1]) & 0xFF);

		// return serverIn.available();
	}
	/*
	 * @Override protected void getMessage(byte[] b, int offset, int len) throws
	 * IOException, ISOException { super.getMessage(b, offset, len);
	 * serverIn.readFully(b, offset, len); LogEvent evt = new LogEvent (this,
	 * "getMessage"); evt.addMessage("Message received (" + len + " bytes): [" +
	 * new String(Arrays.copyOfRange(b, offset, offset + len)) + "]"); }
	 * 
	 * @Override protected void sendMessage(byte[] b, int offset, int len)
	 * throws IOException { super.sendMessage(b, offset, len); LogEvent evt =
	 * new LogEvent (this, "sendMessage"); evt.addMessage("Message sent (" + len
	 * + " bytes): [" + new String(Arrays.copyOfRange(b, offset, offset + len))
	 * + "]"); }
	 */
}
