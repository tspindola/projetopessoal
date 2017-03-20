package com.bravado.tlv;

import java.io.PrintStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOUtil;
import org.jpos.util.Loggeable;

public class CustomTLVList implements Serializable, Loggeable {
	private static final long serialVersionUID = 1L;

	private List<CustomTLVMsg> tags = new ArrayList<CustomTLVMsg>();
	private int tbravadooFind = 0;
	private int indexLastOccurrence = -1;

	public CustomTLVList() {
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
	 * @return a list of tags.
	 */
	public List<CustomTLVMsg> getTags() {
		return tags;
	}

	/**
	 * @return an enumeration of the List of tags.
	 */
	public Enumeration<CustomTLVMsg> elements() {
		return Collections.enumeration(tags);
	}

	/**
	 * unpack a message with a starting offset
	 * 
	 * @param buf
	 *            - raw message
	 * @param offset
	 *            the offset
	 * @throws org.jpos.iso.ISOException
	 */
	public void unpack(byte[] buf, int offset) throws ISOException {
		System.out.println(ISOUtil.hexdump(buf));
		ByteBuffer buffer = ByteBuffer.wrap(buf, offset, buf.length - offset);
		CustomTLVMsg currentNode;
		while (hasNext(buffer)) {
			currentNode = getCustomTLVMsg(buffer); // null is returned if no tag
													// found (trailing padding)
			if (currentNode != null)
				append(currentNode);
		}
	}

	/**
	 * Append CustomTLVMsg to the CustomTLVList
	 */
	public void append(CustomTLVMsg tlvToAppend) {
		tags.add(tlvToAppend);
	}

	/**
	 * Append CustomTLVMsg to the CustomTLVList
	 * 
	 * @param tag
	 *            tag id
	 * @param value
	 *            tag value
	 */
	public void append(int tag, byte[] value) {
		append(new CustomTLVMsg(tag, value));
	}

	/**
	 * Append CustomTLVMsg to the CustomTLVList
	 * 
	 * @param tag
	 *            id
	 * @param value
	 *            in hexadecimal character representation
	 */
	public void append(int tag, String value) {
		append(new CustomTLVMsg(tag, ISOUtil.hex2byte(value)));
	}

	/**
	 * delete the specified TLV from the list using a Zero based index
	 * 
	 * @param index
	 *            number
	 */
	public void deleteByIndex(int index) {
		tags.remove(index);
	}

	/**
	 * Delete the specified TLV from the list by tag value
	 * 
	 * @param tag
	 *            id
	 */
	public void deleteByTag(int tag) {
		List<CustomTLVMsg> t = new ArrayList<CustomTLVMsg>();
		for (CustomTLVMsg tlv2 : tags) {
			if (tlv2.getTag() == tag)
				t.add(tlv2);
		}
		tags.removeAll(t);
	}

	/**
	 * searches the list for a specified tag and returns a TLV object
	 * 
	 * @param tag
	 *            id
	 * @return CustomTLVMsg
	 */
	public CustomTLVMsg find(int tag) {
		tbravadooFind = tag;
		for (CustomTLVMsg tlv : tags) {
			if (tlv.getTag() == tag) {
				indexLastOccurrence = tags.indexOf(tlv);
				return tlv;
			}
		}
		indexLastOccurrence = -1;
		return null;
	}

	/**
	 * searches the list for a specified tag and returns a zero based index for
	 * that tag
	 * 
	 * @return index for a given {2code tag}
	 */
	public int findIndex(int tag) {
		tbravadooFind = tag;
		for (CustomTLVMsg tlv : tags) {
			if (tlv.getTag() == tag) {
				indexLastOccurrence = tags.indexOf(tlv);
				return indexLastOccurrence;
			}
		}
		indexLastOccurrence = -1;
		return -1;
	}

	/**
	 * Return the next CustomTLVMsg of same TAG value
	 * 
	 * @return CustomTLVMsg (return null if not found)
	 */
	public CustomTLVMsg findNextTLV() {

		for (int i = indexLastOccurrence + 1; i < tags.size(); i++) {
			if (tags.get(i).getTag() == tbravadooFind) {
				indexLastOccurrence = i;
				return tags.get(i);
			}
		}
		return null;
	}

	/**
	 * Returns a TLV object which represents the CustomTLVMsg stored within the
	 * CustomTLVList at the given index
	 * 
	 * @param index
	 *            number
	 * @return CustomTLVMsg
	 */
	public CustomTLVMsg index(int index) {
		return tags.get(index);
	}

	/**
	 * pack the TLV message (BER-TLV Encoding)
	 * 
	 * @return the packed message
	 */
	public byte[] pack() {
    	final int maxBufferLengthGuess = 999;
		ByteBuffer buffer = ByteBuffer.allocate(maxBufferLengthGuess);
		for (CustomTLVMsg tlv : tags)
			buffer.put(tlv.getTLV());
		byte[] b = new byte[buffer.position()];
		buffer.flip();
		buffer.get(b);
		return b;

	}

	/**
	 * Read next TLV Message from stream and return it
	 * 
	 * @param buffer
	 *            the buffer
	 * @return CustomTLVMsg
	 */
	private CustomTLVMsg getCustomTLVMsg(ByteBuffer buffer) throws ISOException {
		int tag = getTAG(buffer); // tag = 0 if tag not found
		if (tag == 0)
			return null;

		// Get Length if buffer remains!
		if (!buffer.hasRemaining())
			throw new ISOException(String.format("BAD TLV FORMAT - tag (%x)"
					+ " without length or value", tag));

		int length = getValueLength(buffer);
		if (length > buffer.remaining())
			throw new ISOException(String.format("BAD TLV FORMAT - tag (%x)"
					+ " length (%d) exceeds available data.", tag, length));

		byte[] arrValue = new byte[length];
		buffer.get(arrValue);

		return getCustomTLVMsg(tag, arrValue);
	}

	protected CustomTLVMsg getCustomTLVMsg(int tag, byte[] arrValue) {
		return new CustomTLVMsg(tag, arrValue);
	}

	/**
	 * Check Existence of next TLV Field
	 * 
	 * @param buffer
	 *            ByteBuffer containing TLV data
	 */
	private boolean hasNext(ByteBuffer buffer) {
		return buffer.hasRemaining();
	}

	/**
	 * Return the next TAG
	 * 
	 * @return tag
	 */
	private int getTAG(ByteBuffer buffer) {
		final int count = 3;
		byte[] tagBytes = new byte[count];
		buffer.get(tagBytes);
		int tag = Integer.parseInt(new String(tagBytes));
		return tag;
	}

	/**
	 * Read length bytes and return the int value
	 * 
	 * @param buffer
	 *            buffer
	 * @return value length
	 */
	protected int getValueLength(ByteBuffer buffer) {
		final int count = 3;
		byte[] lenBytes = new byte[count];
		buffer.get(lenBytes);
		int len = Integer.parseInt(new String(lenBytes));
		return len;
	}

	/**
	 * searches the list for a specified tag and returns a hex String
	 * 
	 * @param tag
	 *            id
	 * @return hexString
	 */
	public String getString(int tag) {
		CustomTLVMsg msg = find(tag);
		if (msg != null) {
			return msg.getStringValue();
		} else {
			return null;
		}
	}

	/**
	 * searches the list for a specified tag and returns it raw
	 * 
	 * @param tag
	 *            id
	 * @return byte[]
	 */
	public byte[] getValue(int tag) {
		CustomTLVMsg msg = find(tag);
		if (msg != null) {
			return msg.getValue();
		} else {
			return null;
		}
	}

	/**
	 * searches the list for a specified tag and returns a boolean indicating
	 * presence
	 * 
	 * @return boolean
	 */
	public boolean hasTag(int tag) {
		return (findIndex(tag) > -1);
	}

	@Override
	public void dump(PrintStream p, String indent) {
		String inner = indent + "   ";
		p.println(indent + "<tlvlist>");
		for (CustomTLVMsg msg : getTags())
			msg.dump(p, inner);
		p.println(indent + "</tlvlist>");
	}
}
