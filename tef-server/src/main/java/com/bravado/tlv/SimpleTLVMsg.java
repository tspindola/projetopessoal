package com.bravado.tlv;

import java.io.PrintStream;
import java.math.BigInteger;

import org.jpos.iso.ISOUtil;
import org.jpos.util.Loggeable;

public class SimpleTLVMsg implements Loggeable {
	private byte tag;
	protected byte[] value;

	/**
	 * empty constructor
	 */
	public SimpleTLVMsg() {
		super();
	}

	/**
	 * constructs a TLV Message from tag and value
	 * 
	 * @param tag
	 *            id
	 * @param value
	 *            tag value
	 */
	public SimpleTLVMsg(byte tag, byte[] value) {
		this.tag = tag;
		this.value = value;
	}

	/**
	 * @return tag
	 */
	public byte getTag() {
		return tag;
	}

	/**
	 * @return tag value
	 */
	public byte[] getValue() {
		return value;
	}

	/**
	 * @param tag
	 *            of TLV Message
	 */
	public void setTag(byte tag) {
		this.tag = tag;
	}

	/**
	 * @param value
	 *            of TLV Message
	 */
	public void setValue(byte[] value) {
		this.value = value;
	}

	/**
	 * @return tag + length + value of the TLV Message
	 */
	public byte[] getTLV() {
		String hexVal = Integer.toHexString(tag);
		byte[] bTag = ISOUtil.hex2byte(hexVal);
		byte[] bLen = getL();
		if (value != null) {
			int tLength = bTag.length + bLen.length + value.length;
			byte[] out = new byte[tLength];
			System.arraycopy(bTag, 0, out, 0, bTag.length);
			System.arraycopy(bLen, 0, out, bTag.length, bLen.length);
			System.arraycopy(value, 0, out, bTag.length + bLen.length,
					value.length);
			return out;
		} else {// Length can be 0
			int tLength = bTag.length + bLen.length;
			byte[] out = new byte[tLength];
			System.arraycopy(bTag, 0, out, 0, bTag.length);
			System.arraycopy(bLen, 0, out, bTag.length, bLen.length);
			return out;
		}
	}

	/**
	 * Value up to 127 can be encoded in single byte and multiple bytes are
	 * required for length bigger than 127
	 * 
	 * @return encoded length
	 */
	public byte[] getL() {
		if (value == null)
			return new byte[1];

		BigInteger bi = BigInteger.valueOf(value.length);

		/* Value to be encoded */
		byte[] rBytes = bi.toByteArray();

		return rBytes;
	}

	/**
	 * @return value
	 */
	public String getStringValue() {
		return ISOUtil.hexString(value);
	}

	@Override
	public String toString() {
		String t = Integer.toHexString(tag);
		if (t.length() % 2 > 0)
			t = "0" + t;
		return String.format("[tag: 0x%s, %s]", t, value == null ? null
				: getStringValue());
	}

	@Override
	public void dump(PrintStream p, String indent) {
		p.print(indent);
		p.print("<tag id='");
		p.print(String.format("0x%02x", getTag()));
		p.print("' length='");
		p.print(String.format("%d", value.length));
		p.print("' value='");
		p.print(ISOUtil.hexString(getValue()));
		p.println("' />");
	}

}
