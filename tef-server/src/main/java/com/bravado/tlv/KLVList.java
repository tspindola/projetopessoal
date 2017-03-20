package com.bravado.tlv;

import java.io.PrintStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOUtil;
import org.jpos.util.LogEvent;
import org.jpos.util.Loggeable;
import org.jpos.util.Logger;

public class KLVList implements Serializable, Loggeable {
	private static final long serialVersionUID = 1L;
	private List<KLVMsg> keyToFinds = new ArrayList<KLVMsg>();
	private String keyToFind = "";
	private int indexLastOccurrence = -1;

	public KLVList() {
		super();
	}

	/**
	 * unpack a message
	 * 
	 * @param buf
	 *            - raw message
	 */
	public void unpack(byte[] buf) throws ISOException {
		unpack(buf, 0);
	}

	/**
	 * @return a list of keyToFinds.
	 */
	public List<KLVMsg> getKeys() {
		return keyToFinds;
	}

	/**
	 * @return an enumeration of the List of keyToFinds.
	 */
	public Enumeration<KLVMsg> elements() {
		return Collections.enumeration(keyToFinds);
	}

	/**
	 * unpack a message with a starting offset
	 * 
	 * @param buf
	 *            - raw message
	 * @param offset
	 *            theoffset
	 * @throws org.jpos.iso.ISOException
	 */
	public void unpack(byte[] buf, int offset) throws ISOException {
		//System.out.println(ISOUtil.hexdump(buf));
		LogEvent logEvent = new LogEvent();
		logEvent.addMessage("unpack - " + ISOUtil.hexdump(buf));
		Logger.log(logEvent);		
		ByteBuffer buffer = ByteBuffer.wrap(buf, offset, buf.length - offset);
		KLVMsg currentNode;
		while (hasNext(buffer)) {
			currentNode = getKLVMsg(buffer); // null is returned if no keyToFind
												// found (trailing padding)
			if (currentNode != null)
				append(currentNode);
		}
	}

	/**
	 * Append KLVMsg to the KLVList
	 */
	public void append(KLVMsg tlvToAppend) {
		keyToFinds.add(tlvToAppend);
	}

	/**
	 * Append KLVMsg to the KLVList
	 * 
	 * @param keyToFind
	 *            keyToFind id
	 * @param value
	 *            keyToFind value
	 */
	public void append(String keyToFind, byte[] value) {
		append(new KLVMsg(keyToFind, value));
	}

	/**
	 * Append KLVMsg to the KLVList
	 * 
	 * @param keyToFind
	 *            id
	 * @param value
	 *            in hexadecimal character representation
	 */
	public void append(String keyToFind, String value) {
		append(new KLVMsg(keyToFind, ISOUtil.hex2byte(value)));
	}

	/**
	 * delete the specified KLV from the list using a Zero based index
	 * 
	 * @param index
	 *            number
	 */
	public void deleteByIndex(int index) {
		keyToFinds.remove(index);
	}

	/**
	 * Delete the specified KLV from the list by keyToFind value
	 * 
	 * @param keyToFind
	 *            id
	 */
	public void deleteByKey(String keyToFind) {
		List<KLVMsg> t = new ArrayList<KLVMsg>();
		for (KLVMsg tlv2 : keyToFinds) {
			if (tlv2.getKey() == keyToFind)
				t.add(tlv2);
		}
		keyToFinds.removeAll(t);
	}

	/**
	 * searches the list for a specified keyToFind and returns a KLV object
	 * 
	 * @param keyToFind
	 *            id
	 * @return KLVMsg
	 */
	public KLVMsg find(String keyToFind) {
		this.keyToFind = keyToFind;
		for (KLVMsg tlv : keyToFinds) {
			if (tlv.getKey().equals(keyToFind)) {
				indexLastOccurrence = keyToFinds.indexOf(tlv);
				return tlv;
			}
		}
		indexLastOccurrence = -1;
		return null;
	}

	/**
	 * searches the list for a specified keyToFind and returns a zero based
	 * index for that keyToFind
	 * 
	 * @return index for a given {2code keyToFind}
	 */
	public int findIndex(String keyToFind) {
		this.keyToFind = keyToFind;
		for (KLVMsg tlv : keyToFinds) {
			if (keyToFind.equals(tlv.getKey())) {
				indexLastOccurrence = keyToFinds.indexOf(tlv);
				return indexLastOccurrence;
			}
		}
		indexLastOccurrence = -1;
		return -1;
	}

	/**
	 * Return the next KLVMsg of same TAG value
	 * 
	 * @return KLVMsg (return null if not found)
	 */
	public KLVMsg findNextKLV() {

		for (int i = indexLastOccurrence + 1; i < keyToFinds.size(); i++) {
			if (keyToFinds.get(i).getKey() == keyToFind) {
				indexLastOccurrence = i;
				return keyToFinds.get(i);
			}
		}
		return null;
	}

	/**
	 * Returns a KLV object which represents the KLVMsg stored within the
	 * KLVList at the given index
	 * 
	 * @param index
	 *            number
	 * @return KLVMsg
	 */
	public KLVMsg index(int index) {
		return keyToFinds.get(index);
	}

	/**
	 * pack the KLV message (BER-KLV Encoding)
	 * 
	 * @return the packed message
	 * @throws ISOException
	 */
	public byte[] pack() throws ISOException {
		StringBuilder buffer = new StringBuilder();

		for (KLVMsg klv : keyToFinds)
			buffer.append(klv.getKLV());

		byte[] b = null;
		try {
			b = buffer.toString().getBytes("ASCII-US");
		} catch (UnsupportedEncodingException e) {
			throw new ISOException(e);
		}
		return b;
	}

	/**
	 * Read next KLV Message from stream and return it
	 * 
	 * @param buffer
	 *            the buffer
	 * @return KLVMsg
	 */
	private KLVMsg getKLVMsg(ByteBuffer buffer) throws ISOException {
		String messageError = new String();
		String keyToFind = readKey(buffer); // keyToFind = 0 if keyToFind not
											// found
		if (keyToFind == null || keyToFind.isEmpty())
			return null;

		// Get Length if buffer remains!
		if (!buffer.hasRemaining()){
			messageError = String.format("BAD KLV FORMAT - keyToFind (%x) without length or value", keyToFind);
			throw new ISOException(messageError);
		}

		String valueFound = readValue(buffer);

		if (valueFound == null || valueFound.isEmpty()) {
			messageError = String.format("BAD KLV FORMAT - keyToFind (%s) invalid value.", keyToFind);
			throw new ISOException(messageError);			
		}

		return getKLVMsg(keyToFind, valueFound);
	}

	protected KLVMsg getKLVMsg(String keyToFind, String valueFound) {
		return new KLVMsg(keyToFind, valueFound);
	}
	
	static final Charset CHARSET  = Charset.forName("ISO8859_1");

	/**
	 * Check Existence of next KLV Field
	 * 
	 * @param buffer
	 *            ByteBuffer containing KLV data
	 */
	private boolean hasNext(ByteBuffer buffer) {
		return buffer.hasRemaining();
	}
	
	private String readBytesToString(ByteBuffer buffer, int length) {
		byte[] b = new byte[length];
		buffer.get(b);
		String s = new String(b, CHARSET);
		return s;
	}

	private String readElement(ByteBuffer buffer) {
		int elemLengthLength = Integer.parseInt(readBytesToString(buffer, 1));
		int elemLength = Integer.parseInt(readBytesToString(buffer, elemLengthLength));

		String elem = String.valueOf(readBytesToString(buffer, elemLength));
		
		System.out.println(String.format("%d - %d - %s", elemLengthLength, elemLength, elem));

		return elem;
	}

	/**
	 * Return the next key
	 * 
	 * @return keyToFind
	 */
	private String readKey(ByteBuffer buffer) {
		String keyToFind = readElement(buffer);

		return keyToFind;
	}

	/**
	 * Read the value
	 * 
	 * @param buffer
	 *            buffer
	 * @return valueFound
	 */
	protected String readValue(ByteBuffer buffer) {
		String valueFound = readElement(buffer);

		return valueFound;
	}

	/**
	 * searches the list for a specified keyToFind and returns a hex String
	 * 
	 * @param keyToFind
	 *            id
	 * @return hexString
	 */
	public String getString(String keyToFind) {
		KLVMsg msg = find(keyToFind);
		if (msg != null) {
			return msg.getValue();
		} else {
			return null;
		}
	}

	/**
	 * searches the list for a specified keyToFind and returns it raw
	 * 
	 * @param keyToFind
	 *            id
	 * @return byte[]
	 */
	public byte[] getValue(String keyToFind) {
		KLVMsg msg = find(keyToFind);
		if (msg != null) {
			return ISOUtil.hex2byte(msg.getValue());
		} else {
			return null;
		}
	}

	/**
	 * searches the list for a specified keyToFind and returns a boolean
	 * indicating presence
	 * 
	 * @return boolean
	 */
	public boolean hasKey(String keyToFind) {
		return (findIndex(keyToFind) > -1);
	}

	@Override
	public void dump(PrintStream p, String indent) {
		String inner = indent + "   ";
		p.println(indent + "<klvlist>");
		for (KLVMsg msg : getKeys())
			msg.dump(p, inner);
		p.println(indent + "</klvlist>");
	}
}
