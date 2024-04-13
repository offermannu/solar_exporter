/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.impl.gateway.home;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.Header;
import org.apache.http.StatusLine;
import org.apache.http.message.BasicLineParser;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;

/**
 * Write a response that is chunked into byte[] into a servlet response.
 * 
 * We wrap the response writing so we can capture the extra data (the "appendix") sent by
 * the Gateway worker node side.
 */
public class ResponseWriter  {
	private static final String HEADER_TRANSFER_ENCODING = "Transfer-Encoding";
	private static final String HEADER_CONTENT_LENGTH = "Content-Length";
	public final static String HEADER_GATEWAY_SESSIONID = "X-Z2-GATEWAY-SESSIONID";
	public final static String HEADER_GATEWAY_SESSIONEXP = "X-Z2-GATEWAY-SESSIONEXP";
	
	private final static short M_STATUS_LINE = 0;
	private final static short M_HEADERS = 1;
	private final static short M_BODY = 2;
	private final static short M_CHUNK_END = 3;
	private final static short M_CHUNK_META = 4;
	private final static short M_APPENDIX= 5;
	
	private final static String[] STATES = {"M_STATUS_LINE","M_HEADERS","M_BODY","M_CHUNK_END","M_CHUNK_META","M_APPENDIX"};
	
	private final static short P_CONTENT = 0;
	private final static short P_ONCR    = 1;
	
	private final static char CR = 13;
	private final static char LF = 10;
	
	private Response response;
	private Request  request;
	
	// where are we on the message
	private short state = M_STATUS_LINE;
	// and where in parsing
	private short pstate = P_CONTENT;
	// intermediate char buffer
	private StringBuilder sb = new StringBuilder();
	// unfortunately we need to unchunk the response
	private boolean chunked;
	// we also check for sessionid
	private String sessionId;
	// and the lease exp 
	private Long sessionExp;
	//
	private int remaining = 0;
	//
	private int bodySent;
	// 
	private boolean completed;
	private boolean lastChunk;
	
	public ResponseWriter(Request request, Response response) {
		this.response = response;
		this.request = request;
	}

	public void write(byte[] load, int off, int len) throws Exception {
		if (load==null || len==0) {
			return;
		}
		if (state==M_BODY && len<=this.remaining) {
			// simple case shortcut: Simply write into response body
			response.getOutputStream().write(load,off,len);
			wroteToBody(len);
		} else {
			// else parse...
			int i=off, l=off+len;
			while (i<l) {
				parse(load[i++]);
			}
		}
	}

	public void write(byte[] load) throws Exception {
		write(load,0,load.length);
	}

	// parse on char stream.
	private synchronized void parse(byte c) throws IOException {
		switch (pstate) {
			case P_ONCR: {
				// read pending LF and 
				// go back to CONTENT
				pstate = P_CONTENT;
				if (c==LF) {
					// ignore this char
					break;
				}
				// else fall through
			}
			case P_CONTENT: {
				if (state==M_BODY) {
					if (this.remaining>0) {
						// valid body byte. 
						this.response.getOutputStream().write(c);
						wroteToBody(1);
					} // else ignore. must be done
				} else {
					// we are building a line
					if (c==CR || c==LF) {
						// on a line break
						if (c==CR) {
							pstate = P_ONCR;
						}
						// line is complete
						String line = sb.toString();
						if (LOG.isLoggable(Level.FINEST)) {
							LOG.finest("Read (state="+STATES[state]+") line \""+line+"\"");
						}
						// reset line buffer
						sb.setLength(0);

						switch (state) {
							case M_STATUS_LINE: {
								StatusLine sl = BasicLineParser.parseStatusLine(line, null);
								if (sl.getReasonPhrase()!=null) {
									this.response.setStatusWithReason(sl.getStatusCode(),sl.getReasonPhrase());
								} else {
									this.response.setStatus(sl.getStatusCode());
								}
								// was the status line: Move on to headers
								state = M_HEADERS;
							}
							break;
							case M_HEADERS: {
								if (line.length()==0) {
									// headers ended
									if ("HEAD".equalsIgnoreCase(this.request.getMethod())) {
										// move on to appendix for no-body request
										state=M_APPENDIX;
									} else {
										// move on to body
										if (this.chunked) {
											// reading chunk line
											state = M_CHUNK_META;
										} else {
											if (this.remaining==0) {
												// already done
												state=M_APPENDIX;
											} else {
												state = M_BODY;
											}
										}
									}
								} else {
									// it is a header
									Header h = BasicLineParser.parseHeader(line, null);
									if (HEADER_CONTENT_LENGTH.equals(h.getName())) {
										this.remaining = Integer.parseInt(h.getValue());
										if (LOG.isLoggable(Level.FINER)) {
											LOG.finer("Detected "+HEADER_CONTENT_LENGTH+": "+this.remaining);
										}
									} else
									if (HEADER_TRANSFER_ENCODING.equals(h.getName())) {
										if ("chunked".equals(h.getValue())) {
											this.chunked=true;
											if (LOG.isLoggable(Level.FINER)) {
												LOG.finer("Detected chunked response");
											}
										}
									} else {
										this.response.addHeader(h.getName(), h.getValue());
	
										if (LOG.isLoggable(Level.FINEST)) {
											LOG.finest("Setting header "+h.getName()+"="+h.getValue());
										}
									}
								}
							}
							break;
							case M_CHUNK_END:
								// skip chunk end line
								if (this.lastChunk) {
									// switch to appendix
									state = M_APPENDIX;
								} else {
									// await chunk meta
									state = M_CHUNK_META;
								}
								break;
							case M_CHUNK_META: {
								// cut off chunk extension
								line = line.trim();
								int p = line.indexOf(';');
								if (p>=0) {
									line = line.substring(0,p);
								}
								// read a chunk line
								this.remaining = Integer.parseInt(line,16);
								if (LOG.isLoggable(Level.FINEST)) {
									LOG.finest("Read response body chunk size "+this.remaining);
								}
								if (this.remaining==0) {
									this.lastChunk=true;
									state=M_CHUNK_END;
									if (LOG.isLoggable(Level.FINER)) {
										LOG.finer("Detected chunked response terminator");
									}
								} else {
									state=M_BODY;
								}
							}
							break;
							case M_APPENDIX: {
								LOG.finer("Read appendix "+line);
								// read the appendix line
								int p = line.lastIndexOf('/');
								if (p>=0) {
									this.sessionId = line.substring(0,p);
									this.sessionExp = Long.parseLong(line.substring(p+1));
									if (LOG.isLoggable(Level.FINER)) {
										LOG.finer("Detected session id "+this.sessionId);
										LOG.finer("Detected lease exp "+this.sessionExp);
									}
								}
								this.completed=true;
								break;
							}
						}
					} else {
						// just another character. Append.
						sb.append((char)c);
					}
				}
			}
		}
	}

	private void wroteToBody(int len) {
		this.bodySent+=len;
		this.remaining-=len;
		if (this.remaining == 0) {
			if (this.chunked) {
				if (LOG.isLoggable(Level.FINEST)) {
					LOG.finest("End of chunk - checking for next");
				}
				// chunk end
				state=M_CHUNK_END;
			} else {
				if (LOG.isLoggable(Level.FINEST)) {
					LOG.finest("End of official response - waiting for appendix");
				}
				// response end
				state=M_APPENDIX;
			}
		}
	}


	public synchronized String getSessionId() {
		return sessionId;
	}
	
	public synchronized Long getSessionExpiration() {
		return this.sessionExp;
	}
	
	public synchronized boolean isCompleted() {
		return this.completed;
//		return (state==M_BODY) && (
//			// we have writte the last chunk
//			(this.chunked && this.lastChunkWritten) ||
//			// we are not chunked, no content length set or written 
//			(!this.chunked && this.remaining==0) ||
//			// not a method that requires a body
//			("HEAD".equalsIgnoreCase(this.request.getMethod()))
//		);
	}
	
	public synchronized boolean isChunked() {
		return chunked;
	}
	
	public int getBodyBytesSent() {
		return bodySent;
	}
	
	private final static Logger LOG = Logger.getLogger(ResponseWriter.class.getName());
}
