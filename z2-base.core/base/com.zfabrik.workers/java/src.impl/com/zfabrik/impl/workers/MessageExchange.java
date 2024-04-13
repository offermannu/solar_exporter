/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.impl.workers;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.zfabrik.work.IThreadPool;
import com.zfabrik.workers.IMessageHandler;

/**
 * abstracts our communication model: Messages are pushed in from a stream and
 * sent out to a stream. We use tickets to associates requests and responses.
 * 
 * 
 * We do usually end up having a construction like this:
 * 
 * <ul>
 * <li>StreamReader -(feed)-&gt;
 * <ul>
 * <li>StreamEventHandler -(feed)-&gt;
 * <ul>
 * <li>MessageExchange -(feed)-&gt;
 * <ul>
 * <li>MessageHandler</li>
 * </ul>
 * </li>
 * </ul>
 * </li>
 * </ul>
 * </li>
 * </ul>
 * 
 * @author hb
 * 
 */
public class MessageExchange {
	public static final String ERROR = MessageExchange.class.getName() + ".error";
	
	public static final String TYPE = MessageExchange.class.getName() + ".msgtype";
	public static final String TYPE_R = "r"; // reply
	public static final String TYPE_M = "m"; // message
	public static final String TICKET = MessageExchange.class.getName() + ".ticket";
	public static final String TIMEOUT = MessageExchange.class.getName() + ".timeout";
	public static final String BOUNDARY = "IKSIa7eakjsd9378eiieru4935kerwe3dfsMIKSI";
	public static final int BOUNDARY_LENGTH = BOUNDARY.length();

	private Logger logger;

	// tickets
	private long msgCount = 0;
	// reply mediation
	private Object mediationMx = new Object();

	//
	// the message buffer is limited by the number
	// of concurrent message senders.
	//
	private static class Msg {
		static final byte SENT = 0;
		static final byte REPLIED = 1;
		static final byte EXPIRED = 3;
		Map<String, Serializable> result;
		byte state = SENT;
	}

	private Map<Long, Msg> messages = new HashMap<Long, Msg>();
	private int maxSize = 0; // info

	// stream parsing state.
	private Object streamMx = new Object();
	private int state = 0; // parsing state (0=looking for boundary, 1=in
							// message)

	private OutputStream out;
	private IMessageHandler handler;
	private IThreadPool tp;
	private boolean stop = false;

	private TimeOutWatchDog watchDog;

	public MessageExchange(Logger logger, IMessageHandler handler, IThreadPool tp, OutputStream out) {
		this.logger = logger;
		this.out = out;
		this.handler = handler;
		this.tp = tp;
		this.watchDog = new TimeOutWatchDog(logger.getName());
	}

	private class MessageHandling implements Runnable {
		private Map<String, Serializable> message;

		public MessageHandling(Map<String, Serializable> message) {
			this.message = message;
		}

		public void run() {
			String ticket = (String) message.get(TICKET);
			String timeout = (String) message.get(TIMEOUT);
			long to = (timeout!=null? Long.parseLong(timeout) : 15000);
			Object h = MessageExchange.this.watchDog.addWatch(Thread.currentThread(), to, "Message no. " +ticket);
			try {
				Exception x = null;
				Map<String, Serializable> r = null;
				try {
					r = MessageExchange.this.handler.processMessage(message);
				} catch (Exception e) {
					x = e;
				}
				Map<String, Serializable> result = (r == null ? new HashMap<String, Serializable>() : new HashMap<String, Serializable>(r));
				if (x != null) {
					result.put(ERROR, x);
				}
				// send reply...
				// preserve the ticket (if any).
				try {
					result.put(TICKET, ticket);
					result.put(TYPE, TYPE_R);
					
					byte[] msg = messageToBytes(result);
//					System.err.println("["+Thread.currentThread().getId()+"] Reply ("+(msg.length-12)+") byte");
					MessageExchange.this.out.write(msg);
					MessageExchange.this.out.flush();
				} catch (IOException ioe) {
					MessageExchange.this.logger.log(Level.WARNING, "Error while sending reply to message with ticket " + message.get(TICKET), ioe);
				}
			} finally {
				MessageExchange.this.watchDog.removeWatch(h);
			}
		}
	}

	/**
	 * process a line of input. Returns parts of the line that is not message
	 * (or null if none at all).
	 * 
	 */
	public String processLine(String line) throws IOException {
		synchronized (this.streamMx) {
			if (state == 0) {
				// we are waiting for a boundary
				if (line.endsWith(BOUNDARY)) {
//					if (!Foundation.isWorker()) {
//						logger.info("*line="+line.substring(0, Math.min(200,line.length())));
//					}
					// ahh... a boundary. Go into state 1
					state = 1;
					if (line.length() == BOUNDARY_LENGTH)
						return null;
					else
						return line.substring(0, line.length() - BOUNDARY_LENGTH);
				} else {
					return line;
				}
			} else {
				try {
//					if (!Foundation.isWorker()) {
//						logger.info("line ("+line.length()+" chars)="+line.substring(0, Math.min(200,line.length())));
//					}
					// this must be a message
					if (this.logger != null) {
						if (this.logger.isLoggable(Level.FINEST)) {
							logger.finest("Received: " + line);
						} else if (this.logger.isLoggable(Level.FINER)) {
							logger.finer("Received: " + line.length() + " bytes (payload)");
						}
					}
					Map<String, Serializable> message = decodeMessage(line.getBytes());
					// what is that message?
					String t = (String) message.get(TYPE);
					// could be a reply:
					if (TYPE_R.equals(t)) {
						// it's a reply
						String ticketstring = (String) message.get(TICKET);
						if (logger.isLoggable(Level.FINER)) {
							logger.finer("Received reply for ticket: " + ticketstring);
						}
						_dispatchReply(Long.parseLong(ticketstring), message);
					} else if (TYPE_M.equals(t)) {
						if (logger.isLoggable(Level.FINER)) {
							logger.finer("Received a message with ticket " + message.get(TICKET));
						}
						// it's a message.
						// hand it over to a Runnable that will be executed in some
						// application thread
						// and send the reponse when done.
						this.tp.execute(false, new MessageHandling(message));
					} else {
						if (this.logger != null)
							this.logger.severe("Received message of unknown type \"" + t + "\": " + message);
					}
				} finally {
					state = 0;
				}
			}
		}
		return null;
	}

	/**
	 * a reply has been received. Handle it by checking if somebody is waiting
	 * for it already or by putting it on hold.
	 * 
	 */
	private void _dispatchReply(long ticket, Map<String, Serializable> message) {
		synchronized (this.mediationMx) {
			if (logger.isLoggable(Level.FINER)) {
				logger.finer("Dispatching reply for ticket: " + ticket);
			}
			Long lticket = new Long(ticket);
			Msg m = (Msg) this.messages.get(lticket);
			if (m != null) {
				if (m.state == Msg.SENT) {
					// some thread is waiting for this message.
					m.result = message;
					m.state = Msg.REPLIED;
					this.mediationMx.notifyAll();
				} else if (m.state == Msg.EXPIRED) {
					// we can remove it... nobody's waiting anymore.
					this.messages.remove(lticket);
					if (this.logger != null)
						this.logger.fine("Discarding message ticket " + ticket + " because it is outdated");
				} else {
					// invalid state: must have been replied previously! This
					// should not be possible
					this.messages.remove(lticket);
					if (this.logger != null)
						this.logger.severe("Message ticket " + ticket + " has duplicate (replied but not picked up before)!");
				}
			} else {
				// it has been sent and replied, but nobody is waiting yet. We
				// create the message queue entry
				// here. But we do not need to notify (will be picked up)
				m = new Msg();
				m.state = Msg.REPLIED;
				m.result = message;
				this.messages.put(lticket, m);
				if (this.messages.size() > maxSize)
					maxSize = this.messages.size();
			}
		}
	}

	private synchronized long _pullTicket() {
		return this.msgCount++;
	}

	/**
	 * Send a message and wait for a response with a timeout. In case an fault is returned via the payload, it will be de-seriarlized, wrapped by a remote exception
	 * and thrown again by this method. Any other exception indicates a potentially critical communication error.  
	 * 
	 * @param args
	 * @return
	 */
	public Map<String, Serializable> sendMessage(Map<String, Serializable> msg, long timeout) throws IOException, RemoteException {
		// tunnel a message ticket and the type
		long ticket = _pullTicket();
		// clone the message args (so we can tunnel extra data w/o modifying the original message object)
		Map<String, Serializable> args = new HashMap<String, Serializable>(msg);
		args.put(TICKET, Long.toString(ticket));
		args.put(TIMEOUT, Long.toString(timeout));
		args.put(TYPE, TYPE_M);
		byte[] smsg = messageToBytes(args);
		// write the whole message
		this.out.write(smsg);
		this.out.flush();
		synchronized (this.mediationMx) {
			Long lTicket = new Long(ticket);
			// check if there is a reply already
			Msg m = (Msg) this.messages.remove(lTicket);
			if (m != null) {
				if (m.state != Msg.REPLIED)
					throw new IllegalStateException("pulled illegal message");
				// a reply has been received already
				if (this.logger != null)
					this.logger.finer("picked up pending reply for ticket " + ticket);
				Throwable e = (Throwable) m.result.get(ERROR);
				if (e != null)
					throw new RemoteException("Message target exception", e);
				return m.result;
			}
			m = new Msg();
			this.messages.put(lTicket, m); // indicate that it was sent
			if (this.messages.size() > maxSize)
				maxSize = this.messages.size();
			long start = System.currentTimeMillis();
			long stopTime = (timeout > 0 ? start + timeout : Long.MAX_VALUE);
			try {
				while ((timeout > 0) && (!stop)) {
					try {
						if (this.logger != null)
							this.logger.finer("awaiting reply for ticket " + ticket);
						this.mediationMx.wait(timeout);
					} catch (InterruptedException e) {
						IOException ioe = new IOException("message reception interrupted");
						ioe.initCause(e);
						throw ioe;
					}
					if (m.state == Msg.REPLIED) {
						this.messages.remove(lTicket);
						if (this.logger != null)
							this.logger.finer("Received reply for ticket " + ticket + " while waiting for it");
						Throwable e = (Throwable) m.result.get(ERROR);
						if (e != null)
							throw new RemoteException("Message target exception", e);
						return m.result;
					}
					timeout = stopTime - System.currentTimeMillis();
				}
			} finally {
				// set it to expired
				m.state = Msg.EXPIRED;
			}
			if (stop) {
				throw new MessageExchangeClosed();
			} else {
				// must have timed out!
				throw new MessageTimeOutException(ticket, System.currentTimeMillis() - start);
			}
		}
	}

	private byte[] messageToBytes(Map<String, Serializable> args)	throws IOException {
		ByteArrayOutputStream baout = new ByteArrayOutputStream(10024); 
		// insert message boundary
		baout.write(BOUNDARY.getBytes());
		baout.write(10);
		encodeMessage(baout, args);
		baout.write(10);
		
		if (MessageExchange.this.logger != null) {
			if (MessageExchange.this.logger.isLoggable(Level.FINER)) {
				logger.finer((args.get(TYPE)==TYPE_R? "Replying: ":"Sending: ") + (baout.size()) + " bytes to ticket " + args.get(TICKET));
			}
		}

		return baout.toByteArray();
	}

	// private final static String ENC = "ISO-8859-1"; //"UTF-8";

	public static void encodeMessage(OutputStream out, Map<String, Serializable> args) throws IOException {
		// serialize the map
		ByteArrayOutputStream baout = new ByteArrayOutputStream(4096);
		ObjectOutputStream oout = new ObjectOutputStream(baout);
		oout.writeObject(args);
		oout.close();
		for (int b : baout.toByteArray()) {
			if (b<0) {
				b+=256;
			}
			out.write((int) ((b >> 4) + 'a'));
			out.write((int) ((b & 0xF) + 'a'));
		}
	}

	@SuppressWarnings("unchecked")
	public static Map<String, Serializable> decodeMessage(byte[] in) throws IOException {
		ByteArrayOutputStream baout = new ByteArrayOutputStream(in.length>>1);
		int i=0,c,d;
		while (i<in.length) {
			c = (in[i++]-'a') << 4;
			d = (in[i++]-'a');
			baout.write(c+d);
		}
		ObjectInputStream oin = new ObjectInputStream(new ByteArrayInputStream(baout.toByteArray()));
		try {
			return (Map<String, Serializable>) oin.readObject();
		} catch (ClassNotFoundException e) {
			throw new MessageException("decoding error", e);
		} finally {
			oin.close();
		}
	}

	public int getMaxQueueSize() {
		return this.maxSize;
	}

	public void close() {
		// all reply waiting to be ended
		synchronized (this.mediationMx) {
			this.stop = true;
			this.mediationMx.notifyAll();
		}
		// finish own reply
		synchronized (this.streamMx) {	}
		this.watchDog.stop();
	}
}