/*
 * jPOS Project [http://jpos.org]
 * Copyright (C) 2000-2015 Alejandro P. Revilla
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package br.listofacil.acquirer;

import java.io.EOFException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.SocketException;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOFilter;
import org.jpos.iso.ISOFilter.VetoException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOPackager;
import org.jpos.iso.ISOUtil;
import org.jpos.iso.channel.XMLChannel;
import org.jpos.util.LogEvent;
import org.jpos.util.Logger;

public class ListoXMLChannel extends XMLChannel
{
 
	boolean expectKeepAlive = false;
    /**
     * sends an ISOMsg over the TCP/IP session
     * @param m the Message to be sent
     * @exception IOException
     * @exception ISOException
     * @exception ISOFilter.VetoException;
     */
	@Override
    public void send (ISOMsg m) 
        throws IOException, ISOException
    {
        LogEvent evt = new LogEvent (this, "send");
        try {
            if (!isConnected())
                throw new IOException ("unconnected ISOChannel");
            m.setDirection(ISOMsg.OUTGOING);
            ISOPackager p = getDynamicPackager(m);
            m.setPackager (p);
            m = applyOutgoingFilters (m, evt);
            evt.addMessage (m);
            m.setDirection(ISOMsg.OUTGOING); // filter may have dropped this info
            m.setPackager (p); // and could have dropped packager as well
            byte[] b = m.pack();
            synchronized (serverOutLock) {
                sendMessageLength(b.length + getHeaderLength(m));
                sendMessageHeader(m, b.length);
                sendMessage (b, 0, b.length);
                sendMessageTrailer(m, b);
                serverOut.flush ();
            }
            cnt[TX]++;
            setChanged();
            notifyObservers(m);
        } catch (VetoException e) {
            //if a filter vets the message it was not added to the event
            evt.addMessage (m);
            evt.addMessage (e);
            throw e;
        } catch (ISOException e) {
            evt.addMessage (e);
            throw e;
        } catch (IOException e) {
            evt.addMessage (e);
            throw e;
        } catch (Exception e) {
            evt.addMessage (e);
            throw new IOException ("unexpected exception", e);
        } finally {
            Logger.log (evt);
        }
    }
    /**
     * sends a byte[] over the TCP/IP session
     * @param b the byte array to be sent
     * @exception IOException
     * @exception ISOException
     * @exception ISOFilter.VetoException;
     */
    @Override
    public void send (byte[] b) 
        throws IOException, ISOException
    {
        LogEvent evt = new LogEvent (this, "send");
        try {
            if (!isConnected())
                throw new ISOException ("unconnected ISOChannel");
            synchronized (serverOutLock) {
                serverOut.write(b);
                serverOut.flush();
            }
            cnt[TX]++;
            setChanged();
        } catch (Exception e) {
            evt.addMessage (e);
            throw new ISOException ("unexpected exception", e);
        } finally {
            Logger.log (evt);
        }
    }
    /**
     * Waits and receive an ISOMsg over the TCP/IP session
     * @return the Message received
     * @throws IOException
     * @throws ISOException
     */
    @Override
    public ISOMsg receive() throws IOException, ISOException {
        byte[] b=null;
        byte[] header=null;
        LogEvent evt = new LogEvent (this, "receive");
        ISOMsg m = createMsg ();  // call createMsg instead of createISOMsg for 
                                  // backward compatibility
        m.setSource (this);
        try {
            if (!isConnected())
                throw new ISOException ("unconnected ISOChannel");

            synchronized (serverInLock) {
                int len  = getMessageLength();
                if (expectKeepAlive) {
                    while (len == 0) {
                        //If zero length, this is a keep alive msg
                        Logger.log(new LogEvent(this, "receive", "Zero length keep alive message received"));
                        len  = getMessageLength();
                    }
                }
                int hLen = getHeaderLength();

                if (len == -1) {
                    if (hLen > 0) {
                        header = readHeader(hLen);
                    }
                    b = streamReceive();
                }
                else if (len > 0 && len <= getMaxPacketLength()) {
                    if (hLen > 0) {
                        // ignore message header (TPDU)
                        // Note header length is not necessarily equal to hLen (see VAPChannel)
                        header = readHeader(hLen);
                        len -= header.length;
                    }
                    b = new byte[len];
                    getMessage (b, 0, len);
                    getMessageTrailer(m);
                }
                else
                    throw new ISOException(
                        "receive length " +len + " seems strange - maxPacketLength = " + getMaxPacketLength());
            }
            m.setPackager (getDynamicPackager(header, b));
            m.setHeader (getDynamicHeader(header));
            if (b.length > 0 && !shouldIgnore (header))  // Ignore NULL messages
                unpack (m, b);
            m.setDirection(ISOMsg.INCOMING);
            evt.addMessage (m);
            m = applyIncomingFilters (m, header, b, evt);
            m.setDirection(ISOMsg.INCOMING);
            cnt[RX]++;
            setChanged();
            notifyObservers(m);
        } catch (ISOException e) {
            evt.addMessage (e);
            if (header != null) {
                evt.addMessage ("--- header ---");
                evt.addMessage (ISOUtil.hexdump (header));
            }
            if (b != null) {
                evt.addMessage ("--- data ---");
                evt.addMessage (ISOUtil.hexdump (b));
            }
            throw e;
        } catch (EOFException e) {
            closeSocket();
            evt.addMessage ("<peer-disconnect/>");
            throw e;
        } catch (SocketException e) {
            closeSocket();
            if (usable)
                evt.addMessage ("<peer-disconnect>" + e.getMessage() + "</peer-disconnect>");
            throw e;
        } catch (InterruptedIOException e) {
            closeSocket();
            evt.addMessage ("<io-timeout/>");
            throw e;
        } catch (IOException e) { 
            closeSocket();
            if (usable) 
                evt.addMessage (e);
            throw e;
        } catch (Exception e) {
            closeSocket();
            evt.addMessage (m);
            evt.addMessage (e);
            throw new IOException ("unexpected exception", e);
        } finally {
            Logger.log (evt);
        }
        return m;
    }
}
