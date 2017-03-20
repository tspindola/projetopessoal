package com.bravado.tlv;

import java.io.PrintStream;
import java.io.Serializable;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOUtil;
import org.jpos.util.Loggeable;

public class SimpleTLVList implements Serializable, Loggeable {
	private static final long serialVersionUID = 1L;
	
	private List<SimpleTLVMsg> tags = new ArrayList<SimpleTLVMsg>();
    private byte tbravadooFind = 0;
    private int indexLastOccurrence = -1;

    public SimpleTLVList() {
        super();
    }

    /**
     * unpack a message
     * @param buf - raw message
     */
    public void unpack(byte[] buf) throws ISOException {
        unpack(buf, 0);
    }

    /**
     * @return a list of tags.
     */
    public List<SimpleTLVMsg> getTags() {
        return tags;
    }

    /**
     * @return an enumeration of the List of tags.
     */
    public Enumeration<SimpleTLVMsg> elements() {
        return Collections.enumeration(tags);
    }

    /**
     * unpack a message with a starting offset
     * @param buf - raw message
     * @param offset the offset
     * @throws org.jpos.iso.ISOException
     */
    public void unpack(byte[] buf, int offset) throws ISOException {
        System.out.println (ISOUtil.hexdump(buf));
        ByteBuffer buffer=ByteBuffer.wrap(buf,offset,buf.length-offset);
        SimpleTLVMsg currentNode;
        while (hasNext(buffer)) {    
            currentNode = getSimpleTLVMsg(buffer);    // null is returned if no tag found (trailing padding)
            if (currentNode != null)
                append(currentNode);
        }
    }

    /**
     * Append SimpleTLVMsg to the TLVList
     */
    public void append(SimpleTLVMsg tlvToAppend) {
        tags.add(tlvToAppend);
    }
    
    /**
     * Append SimpleTLVMsg to the TLVList
     * @param tag tag id
     * @param value tag value
     */
    public void append(byte tag, byte[] value) {
        append(new SimpleTLVMsg(tag, value));
    }
    
    /**
     * Append SimpleTLVMsg to the TLVList
     * @param tag id
     * @param value in hexadecimal character representation
     */
    public void append(byte tag, String value) {
        append(new SimpleTLVMsg(tag, ISOUtil.hex2byte(value)));
    }

    /**
     * delete the specified TLV from the list using a Zero based index
     * @param index number
     */
    public void deleteByIndex(int index) {
        tags.remove(index);
    }

    /**
     * Delete the specified TLV from the list by tag value
     * @param tag id
     */
    public void deleteByTag(byte tag) {
        List<SimpleTLVMsg> t = new ArrayList<SimpleTLVMsg>();
        for (SimpleTLVMsg tlv2 :tags ) {
            if (tlv2.getTag() == tag)
                t.add(tlv2);
        }
        tags.removeAll(t);
    }

    /**
     * searches the list for a specified tag and returns a TLV object
     * @param tag id
     * @return SimpleTLVMsg
     */
    public SimpleTLVMsg find(byte tag) {
        tbravadooFind = tag;
        for (SimpleTLVMsg tlv :tags ) {
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
     * @return index for a given {2code tag}
     */
    public int findIndex(byte tag) {
        tbravadooFind = tag;
        for (SimpleTLVMsg tlv :tags ) {
            if (tlv.getTag() == tag) {
                indexLastOccurrence = tags.indexOf(tlv);
                return indexLastOccurrence;
            }
        }
        indexLastOccurrence = -1;
        return -1;
    }
    
    /**
     * Return the next SimpleTLVMsg of same TAG value
     * @return SimpleTLVMsg (return null if not found)
     */
    public SimpleTLVMsg findNextTLV() {

        for ( int i=indexLastOccurrence + 1 ; i < tags.size(); i++) {
            if (tags.get(i).getTag() == tbravadooFind) {
                indexLastOccurrence = i;
                return tags.get(i);
            }
        }
        return null;
    }

    /**
     * Returns a TLV object which represents the SimpleTLVMsg stored within the TLVList
     * at the given index
     * @param index number
     * @return SimpleTLVMsg
     */
    public SimpleTLVMsg index(int index) {
        return tags.get(index);
    }

    /**
     * pack the TLV message (BER-TLV Encoding)
     * @return the packed message
     */
    public byte[] pack() {
    	final int maxBufferLengthGuess = 999;
        ByteBuffer buffer=ByteBuffer.allocate(maxBufferLengthGuess);
        for ( SimpleTLVMsg tlv : tags)
          buffer.put(tlv.getTLV());
        byte[] b=new byte[buffer.position()];
        buffer.flip();
        buffer.get(b);
        return b;
 
    }

    /**
     * Read next TLV Message from stream and return it 
     * @param buffer the buffer
     * @return SimpleTLVMsg
     */
    private SimpleTLVMsg getSimpleTLVMsg(ByteBuffer buffer) throws ISOException {
    	String messageError = new String();
        byte tag = getTAG(buffer);  // tag = 0 if tag not found
        if (tag ==0)
            return null;

        // Get Length if buffer remains!
        if (!buffer.hasRemaining()) {        	
			messageError = String.format("BAD TLV FORMAT - tag (%x) without length or value", tag);
			throw new ISOException(messageError);            
        }

        int length = getValueLength(buffer);
        if(length >buffer.remaining()){
			messageError = String.format("BAD TLV FORMAT - tag (%x) length (%d) exceeds available data.", tag, length);
			throw new ISOException(messageError);    
        }
        byte[] arrValue= new byte[length];
        buffer.get(arrValue);

        return getSimpleTLVMsg(tag, arrValue);
    }
   
    protected SimpleTLVMsg getSimpleTLVMsg(byte tag, byte[] arrValue) {
        return new SimpleTLVMsg(tag,arrValue);
    }

    /**
     * Check Existence of next TLV Field
     * @param buffer  ByteBuffer containing TLV data
     */
    private  boolean hasNext(ByteBuffer buffer) {
        return buffer.hasRemaining();
    }
    
    /**
     * Return the next TAG
     * @return tag
     */
    private byte getTAG(ByteBuffer buffer) {
        byte tag = (byte) (buffer.get() & 0xff);
        return tag;
    }
    
    /**
     * Read length bytes and return the int value
     * @param buffer buffer
     * @return value length
     */
    protected int getValueLength(ByteBuffer buffer) {
        final int count = 2;
        byte[] bb = new byte[count];
        buffer.get(bb);
        return new BigInteger(bb).intValue();
    }
    
    /**
     * searches the list for a specified tag and returns a hex String
     * @param tag id
     * @return hexString  
     */
    public String getString(byte tag) {
        SimpleTLVMsg msg = find(tag);
        if (msg != null) {
            return msg.getStringValue();
        }
        else {
            return null;
        }
    }
    
    /**
     * searches the list for a specified tag and returns it raw
     * @param tag id
     * @return byte[]  
     */
    public byte[] getValue(byte tag) {
        SimpleTLVMsg msg = find(tag);
        if (msg != null) {
            return msg.getValue();
        }
        else {
            return null;
        }
    }
    
    /**
     * searches the list for a specified tag and returns a boolean indicating presence
     * @return boolean
     */
    public boolean hasTag(byte tag) {
        return (findIndex(tag) > -1);
    }

    @Override
    public void dump(PrintStream p, String indent) {
        String inner = indent + "   ";
        p.println (indent + "<tlvlist>");
        for (SimpleTLVMsg msg : getTags())
            msg.dump (p, inner);
        p.println (indent + "</tlvlist>");
    }
}
