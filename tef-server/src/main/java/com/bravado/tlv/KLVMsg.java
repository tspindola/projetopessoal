package com.bravado.tlv;

import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOUtil;
import org.jpos.util.Loggeable;

public class KLVMsg implements Loggeable {
	private String key;
	protected String value;

	/**
	 * empty constructor
	 */
	public KLVMsg() {
		super();
	}

	/**
	 * constructs a KLV Message from key and value
	 * 
	 * @param key
	 *            id
	 * @param value
	 *            key value
	 */
	public KLVMsg(String key, String value) {
		this.key = key;
		this.value = value;
	}
	
	/**
	 * constructs a KLV Message from key and value
	 * 
	 * @param key
	 *            id
	 * @param value
	 *            key value
	 */
	public KLVMsg(String key, byte[] value) {
		this.key = key;
		this.value = ISOUtil.hexString(value);
	}

	/**
	 * @return key
	 */
	public String getKey() {
		return key;
	}

	/**
	 * @return tag value
	 */
	public String getValue() {
		return value;
	}

	/**
	 * @param tag
	 *            of KLV Message
	 */
	public void setKey(String key) {
		this.key = key;
	}

	/**
	 * @param value
	 *            of KLV Message
	 */
	public void setValue(byte[] value) {
		this.value = ISOUtil.hexString(value);
	}

	/**
	 * @return key-length-length + key-length + key + value-length-length +
	 *         value-length + value of the KLV Message
	 * @throws ISOException
	 * @throws UnsupportedEncodingException
	 */
	public String getKLV() throws ISOException {
		String keyLength = String.format("%d", key.length());
		String keyLengthLength = String.format("%d", keyLength.length());

		String valueLength = String.format("%d", value.length());
		String valueLengthLength = String.format("%d", valueLength.length());

		String msg = keyLengthLength + keyLength + key + valueLength
				+ valueLengthLength + value;

		return msg;
	}

	@Override
	public String toString() {
		return String
				.format("[key: %s, %s]", key, value == null ? null : value);
	}

	@Override
	public void dump(PrintStream p, String indent) {
		p.print(indent);
		p.print("<key id='");
		p.print(getKey());
		p.print("' value='");
		p.print(getValue());
		p.println("' />");
	}
}
