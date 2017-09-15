package br.listofacil;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

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

import org.jdom.Element;
import org.jpos.core.ConfigurationException;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISORequestListener;
import org.jpos.iso.ISOResponseListener;
import org.jpos.iso.ISOSource;
import org.jpos.iso.ISOUtil;
import org.jpos.iso.MUX;
import org.jpos.q2.QBeanSupport;
import org.jpos.q2.QFactory;
import org.jpos.q2.iso.QMUXMBean;
import org.jpos.space.LocalSpace;
import org.jpos.space.Space;
import org.jpos.space.SpaceFactory;
import org.jpos.space.SpaceListener;
import org.jpos.space.SpaceUtil;
import org.jpos.space.TSpace;
import org.jpos.util.LogEvent;
import org.jpos.util.Loggeable;
import org.jpos.util.Logger;
import org.jpos.util.NameRegistrar;

/**
 * @author Alejandro Revilla
 */
@SuppressWarnings("unchecked")
public class QMUXCustom extends QBeanSupport implements SpaceListener, MUX, QMUXMBean, Loggeable {
	static final String nomap = "0123456789";
	static final String DEFAULT_KEY = "41, 11";
	protected LocalSpace sp;
	protected String in, out, unhandled;
	protected String[] ready;
	protected String[] key;
	protected String ignorerc;
	protected String[] mtiMapping;
	private boolean headerIsKey;
	private LocalSpace isp; // internal space

	List<ISORequestListener> listeners;
	int rx, tx, rxExpired, txExpired, rxPending, rxUnhandled, rxForwarded;
	long lastTxn = 0L;
	boolean listenerRegistered;

	public QMUXCustom() {
		super();
		listeners = new ArrayList<ISORequestListener>();
	}

	public void initService() throws ConfigurationException {
		Element e = getPersist();
		this.sp = grabSpace(e.getChild("space"));
		this.isp = cfg.getBoolean("reuse-space", false) ? sp : new TSpace();
		this.in = e.getChildTextTrim("in");
		this.out = e.getChildTextTrim("out");
		this.ignorerc = e.getChildTextTrim("ignore-rc");
		this.key = toStringArray(e.getChildTextTrim("key"), ", ", DEFAULT_KEY);
		this.ready = toStringArray(e.getChildTextTrim("ready"));
		this.mtiMapping = toStringArray(e.getChildTextTrim("mtimapping"));
		if (this.mtiMapping == null || this.mtiMapping.length != 3)
			this.mtiMapping = new String[] { nomap, nomap, "0022446789" };
		addListeners();
		this.unhandled = e.getChildTextTrim("unhandled");
		NameRegistrar.register("mux." + getName(), this);
	}

	public void startService() {
		if (!this.listenerRegistered) {
			this.listenerRegistered = true;
			// Handle messages that could be in the in queue at start time
			synchronized (this.sp) {
				Object[] pending = SpaceUtil.inpAll(this.sp, this.in);
				this.sp.addListener(this.in, this);
				for (Object o : pending)
					this.sp.out(this.in, o);
			}
		}
	}

	public void stopService() {
		this.listenerRegistered = false;
		this.sp.removeListener(this.in, this);
	}

	public void destroyService() {
		NameRegistrar.unregister("mux." + getName());
	}

	/**
	 * @return MUX with name using NameRegistrar
	 * @throws NameRegistrar.NotFoundException
	 * @see NameRegistrar
	 */
	public static MUX getMUX(String name) throws NameRegistrar.NotFoundException {
		return (MUX) NameRegistrar.get("mux." + name);
	}

	/**
	 * @param m
	 *            message to send
	 * @param timeout
	 *            amount of time in millis to wait for a response
	 * @return response or null
	 */
	public ISOMsg request(ISOMsg m, long timeout) throws ISOException {
		
		String key = getKey(m);
		String req = key + ".req";
		if (this.isp.rdp(req) != null)
			throw new ISOException("Duplicate key '" + req + "' detected");
		this.isp.out(req, m);
		m.setDirection(0);
		if (timeout > 0)
			this.sp.out(out, m, timeout);
		else
			this.sp.out(out, m);

		ISOMsg resp = null;
		try {
			synchronized (this) {
				this.tx++;
				this.rxPending++;
			}

			for (;;) {
				resp = (ISOMsg) this.isp.rd(key, timeout);
				if (shouldIgnore(resp))
					continue;
				this.isp.inp(key);
				break;
			}
			if (resp == null && this.isp.inp(req) == null) {
				// possible race condition, retry for a few extra seconds
				resp = (ISOMsg) this.isp.in(key, 10000);
			}
			synchronized (this) {
				if (resp != null) {
					this.rx++;
					this.lastTxn = System.currentTimeMillis();
				} else {
					rxExpired++;
					if (m.getDirection() != ISOMsg.OUTGOING)
						this.txExpired++;
				}
			}
		} catch (Exception e) {
			Logger.log(new LogEvent(
					"Erro: br.listofacil.acquirer.QMUXCustom.request \n " + e.getMessage()));
			throw e;
		} finally {
			synchronized (this) {
				this.rxPending--;
			}
		}
		return resp;
	}

	public void notify(Object k, Object value) {
		Object obj = this.sp.inp(k);
		if (obj instanceof ISOMsg) {
			ISOMsg m = (ISOMsg) obj;
			try {
				String key = getKey(m);
				String req = key + ".req";
				Object r = this.isp.inp(req);
				if (r != null) {
					if (r instanceof AsyncRequest) {
						((AsyncRequest) r).responseReceived(m);
					} else {
						this.isp.out(key, m);
					}
					return;
				}
			} catch (ISOException e) {
				Logger.log(new LogEvent(
						"Erro: br.listofacil.acquirer.QMUXCustom.notify \n " + e.getMessage()));
				getLog().warn("notify", e);
				e.printStackTrace();
			}
			processUnhandled(m);
		}
	}

	public String getKey(ISOMsg m) throws ISOException {
		StringBuilder sb = new StringBuilder(this.out);
		sb.append('.');
		sb.append(mapMTI(m.getMTI()));
		if (this.headerIsKey && m.getHeader() != null) {
			sb.append('.');
			sb.append(ISOUtil.hexString(m.getHeader()));
			sb.append('.');
		}
		boolean hasFields = false;
		for (String f : this.key) {
			String v = m.getString(f);
			if (v != null) {
				if ("11".equals(f)) {
					String vt = v.trim();
					int l = m.getMTI().charAt(0) == '2' ? 12 : 6;
					if (vt.length() < l)
						v = ISOUtil.zeropad(vt, l);
				}
				if ("41".equals(f)) {
					v = ISOUtil.zeropad(v.trim(), 16); // BIC ANSI to ISO hack
				}
				hasFields = true;
				sb.append(v);
			}
		}
		if (!hasFields)
			throw new ISOException("Key fields not found - not sending " + sb.toString());
		return sb.toString();
	}

	private String mapMTI(String mti) throws ISOException {
		StringBuilder sb = new StringBuilder();		
		if (mti != null) {
			String mti_formatted = mti;
			if (mti_formatted.length() < 4)
				mti_formatted = ISOUtil.zeropad(mti_formatted, 4); // #jPOS-55
			if (mti_formatted.length() == 4) {
				for (int i = 0; i < mtiMapping.length; i++) {
					int c = mti_formatted.charAt(i) - '0';
					if (c >= 0 && c < 10)
						sb.append(mtiMapping[i].charAt(c));
				}
			}
		}
		return sb.toString();
	}

	public synchronized void setInQueue(String in) {
		this.in = in;
		getPersist().getChild("in").setText(in);
		setModified(true);
	}

	public synchronized String getInQueue() {
		return this.in;
	}

	public synchronized void setOutQueue(String out) {
		this.out = out;
		getPersist().getChild("out").setText(out);
		setModified(true);
	}

	public synchronized String getOutQueue() {
		return this.out;
	}

	public Space getSpace() {
		return this.sp;
	}

	public synchronized void setUnhandledQueue(String unhandled) {
		this.unhandled = unhandled;
		getPersist().getChild("unhandled").setText(unhandled);
		setModified(true);
	}

	public synchronized String getUnhandledQueue() {
		return this.unhandled;
	}

	public void request(ISOMsg m, long timeout, ISOResponseListener rl, Object handBack) throws ISOException {
		String key = getKey(m);
		String req = key + ".req";
		if (this.isp.rdp(req) != null)
			throw new ISOException("Duplicate key '" + req + "' detected.");
		m.setDirection(0);
		AsyncRequest ar = new AsyncRequest(rl, handBack);
		synchronized (ar) {
			if (timeout > 0)
				ar.setFuture(getScheduledThreadPoolExecutor().schedule(ar, timeout, TimeUnit.MILLISECONDS));
		}
		this.isp.out(req, ar, timeout);
		this.sp.out(this.out, m, timeout);
	}

	public String[] getReadyIndicatorNames() {
		return this.ready;
	}

	private void addListeners() throws ConfigurationException {
		QFactory factory = getFactory();
		Iterator iter = getPersist().getChildren("request-listener").iterator();
		while (iter.hasNext()) {
			Element l = (Element) iter.next();
			ISORequestListener listener = (ISORequestListener) factory.newInstance(l.getAttributeValue("class"));
			factory.setLogger(listener, l);
			factory.setConfiguration(listener, l);
			addISORequestListener(listener);
		}
	}

	public void addISORequestListener(ISORequestListener l) {
		this.listeners.add(l);
	}

	public boolean removeISORequestListener(ISORequestListener l) {
		return this.listeners.remove(l);
	}

	public synchronized void resetCounters() {
		this.rx = this.tx = this.rxExpired = this.txExpired = this.rxPending = this.rxUnhandled = this.rxForwarded = 0;
		this.lastTxn = 0l;
	}

	public String getCountersAsString() {
		StringBuilder sb = new StringBuilder();
		append(sb, "tx=", tx);
		append(sb, ", rx=", rx);
		append(sb, ", tx_expired=", txExpired);
		append(sb, ", tx_pending=", sp.size(out));
		append(sb, ", rx_expired=", rxExpired);
		append(sb, ", rx_pending=", rxPending);
		append(sb, ", rx_unhandled=", rxUnhandled);
		append(sb, ", rx_forwarded=", rxForwarded);
		sb.append(", connected=");
		sb.append(Boolean.toString(isConnected()));
		sb.append(", last=");
		sb.append(this.lastTxn);
		if (this.lastTxn > 0) {
			sb.append(", idle=");
			sb.append(System.currentTimeMillis() - this.lastTxn);
			sb.append("ms");
		}
		return sb.toString();
	}

	public int getTXCounter() {
		return this.tx;
	}

	public int getRXCounter() {
		return this.rx;
	}

	public long getLastTxnTimestampInMillis() {
		return this.lastTxn;
	}

	public long getIdleTimeInMillis() {
		return this.lastTxn > 0L ? System.currentTimeMillis() - this.lastTxn : -1L;
	}

	protected void processUnhandled(ISOMsg m) {
		ISOSource source = m.getSource() != null ? m.getSource() : this;
		Iterator iter = this.listeners.iterator();
		if (iter.hasNext())
			synchronized (this) {
				this.rxForwarded++;
			}
		while (iter.hasNext())
			if (((ISORequestListener) iter.next()).process(source, m))
				return;
		if (this.unhandled != null) {
			synchronized (this) {
				this.rxUnhandled++;
			}
			this.sp.out(this.unhandled, m, 120000);
		}
	}

	private LocalSpace grabSpace(Element e) throws ConfigurationException {
		String uri = e != null ? e.getText() : "";
		Space sp = SpaceFactory.getSpace(uri);
		if (sp instanceof LocalSpace) {
			return (LocalSpace) sp;
		}
		throw new ConfigurationException("Invalid space " + uri);
	}

	/**
	 * sends (or hands back) an ISOMsg
	 *
	 * @param m
	 *            the Message to be sent
	 * @throws java.io.IOException
	 * @throws org.jpos.iso.ISOException
	 * @throws org.jpos.iso.ISOFilter.VetoException;
	 */
	public void send(ISOMsg m) throws IOException, ISOException {
		if (!isConnected())
			throw new ISOException("MUX is not connected");
		this.sp.out(this.out, m);
	}

	public boolean isConnected() {
		if (running() && this.ready != null && this.ready.length > 0) {
			for (String aReady : this.ready)
				if (this.sp.rdp(aReady) != null)
					return true;
			return false;
		}
		return running();
	}

	public void dump(PrintStream p, String indent) {
		p.println(indent + getCountersAsString());
	}

	private String[] toStringArray(String s, String delimiter, String def) {
		String sa = s;
		if (s == null)
			sa = def;
		String[] arr = null;
		if (sa != null && sa.length() > 0) {
			StringTokenizer st;
			if (delimiter != null)
				st = new StringTokenizer(sa, delimiter);
			else
				st = new StringTokenizer(sa);

			List<String> l = new ArrayList<String>();
			while (st.hasMoreTokens()) {
				String t = st.nextToken();
				if ("header".equalsIgnoreCase(t)) {
					this.headerIsKey = true;
				} else {
					l.add(t);
				}
			}
			arr = l.toArray(new String[l.size()]);
		}
		return arr;
	}

	private String[] toStringArray(String s) {
		return toStringArray(s, null, null);
	}

	private boolean shouldIgnore(ISOMsg m) {
		if (m != null && this.ignorerc != null && this.ignorerc.length() > 0 && m.hasField(39)) {
			return this.ignorerc.contains(m.getString(39));
		}
		return false;
	}

	private void append(StringBuilder sb, String name, int value) {
		sb.append(name);
		sb.append(value);
	}

	public static class AsyncRequest implements Runnable {
		ISOResponseListener rl;
		Object handBack;
		ScheduledFuture future;

		public AsyncRequest(ISOResponseListener rl, Object handBack) {
			super();
			this.rl = rl;
			this.handBack = handBack;
		}

		public void setFuture(ScheduledFuture future) {
			this.future = future;
		}

		public void responseReceived(ISOMsg response) {
			if (future == null || future.cancel(false))
				this.rl.responseReceived(response, this.handBack);
		}

		public void run() {
			this.rl.expired(this.handBack);
		}
	}
}
