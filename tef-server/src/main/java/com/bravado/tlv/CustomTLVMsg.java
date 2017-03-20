package com.bravado.tlv;

import java.io.PrintStream;

import org.jpos.util.Loggeable;

public class CustomTLVMsg implements Loggeable {
	private int tag;
	protected byte[] value;

	/**
	 * empty constructor
	 */
	public CustomTLVMsg() {
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
	public CustomTLVMsg(int tag, byte[] value) {
		this.tag = tag;
		this.value = value;
	}

	/**
	 * @return tag
	 */
	public int getTag() {
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
	public void setTag(int tag) {
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
		String strTag = String.format("%03d", tag);
		byte[] bTag = strTag.getBytes();
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

		String strL = String.format("%03d", value.length);

		/* Value to be encoded */
		byte[] rBytes = strL.getBytes();

		return rBytes;
	}

	/**
	 * @return value
	 */
	public String getStringValue() {
		return new String(value);
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
		p.print(String.format("%03d", getTag()));
		p.print("' length='");
		p.print(String.format("%d", value.length));
		p.print("' value='");
		p.print(getStringValue());
		p.println("' />");
	}
}
