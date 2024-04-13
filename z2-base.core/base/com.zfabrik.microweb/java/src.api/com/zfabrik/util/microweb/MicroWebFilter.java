/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.util.microweb;

import static com.zfabrik.util.microweb.MicroWebConstants.MICROWEB_APPLICATION_PATH;
import static com.zfabrik.util.microweb.MicroWebConstants.MICROWEB_PATH_INFO;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import com.zfabrik.util.html.Escaper;
import com.zfabrik.util.microweb.actions.IAction;
import com.zfabrik.util.microweb.actions.IActionProvider;
import com.zfabrik.util.microweb.actions.OutCome;
import com.zfabrik.work.WorkUnit;

/**
 * All this filter does is to check wether an action should be executed for this
 * request. If so, it will call the action. The outcome of that action
 * determines where to go next.
 *
 * Actions are determined by the path information of the request URL. It will be
 * cut into segments and actions will be applied according to a longest match
 * pattern.
 *
 * The relevant URL can be obtained via
 * {@link MicroWebUtil#getRelevantPath(HttpServletRequest)}. Actions that
 * require relative addressing in turn should send a redirect in case of a
 * missing trailing "/"
 *
 * @author hb
 *
 */
public class MicroWebFilter implements Filter {
	private final static Set<String> LANGUAGE_CODES = new HashSet<String>(Arrays.asList(Locale.getISOLanguages())); 
	private final static String HANDLER_PROVIDER = "actions.Provider";
	/**
	 * mode using i18n urls, &lt;context-path&gt;/&lt;lang&gt;/...
	 */
	private final static String I18NURLS = "mode.i18nURLs";
	private final static String DEF_HANDLERPROV = "Actions";

	private static ThreadLocal<HttpServletRequest> currentRequest = new ThreadLocal<HttpServletRequest>();

	private FilterConfig cfg;
	private boolean usei18nURLs = false;
	private IActionProvider actionprovider;

	@Override
	public void destroy() {
	}

	// capturing response wrapper as to avoid stream closing after a forward.
	private static class ResponseWrapper extends HttpServletResponseWrapper {
		private ServletOutputStreamWrapper out;
		private PrintWriterWrapper wri;

		public ResponseWrapper(HttpServletResponse response) {
			super(response);
		}

		@Override
		public ServletOutputStream getOutputStream() throws IOException {
			if (this.out == null) {
				this.out = new ServletOutputStreamWrapper(super
						.getOutputStream());
			}
			return this.out;
		}

		@Override
		public PrintWriter getWriter() throws IOException {
			if (this.wri == null) {
				this.wri = new PrintWriterWrapper(super.getWriter());
			}
			return this.wri;
		}
	}

	private static class PrintWriterWrapper extends PrintWriter {
		private final PrintWriter wrapped;

		public PrintWriterWrapper(PrintWriter wrapped) {
			super(wrapped);
			this.wrapped = wrapped;
		}

		@Override
		public void write(char[] buf) {
			this.wrapped.write(buf);
		}

		@Override
		public void write(char[] buf, int off, int len) {
			this.wrapped.write(buf, off, len);
		}

		@Override
		public void write(int c) {
			this.wrapped.write(c);
		}

		@Override
		public void write(String s) {
			this.wrapped.write(s);
		}

		@Override
		public void write(String s, int off, int len) {
			this.wrapped.write(s, off, len);
		}

		@Override
		public void flush() {
			this.wrapped.flush();
		}

		@Override
		public void close() {
			flush();
		}

	}

	private static class ServletOutputStreamWrapper extends ServletOutputStream {
		private final ServletOutputStream wrapped;

		public ServletOutputStreamWrapper(ServletOutputStream wrapped) {
			super();
			this.wrapped = wrapped;
		}

		@Override
		public void write(int b) throws IOException {
			this.wrapped.write(b);
		}

		@Override
		public void write(byte[] b) throws IOException {
			this.wrapped.write(b);
		}

		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			this.wrapped.write(b, off, len);
		}

		@Override
		public void flush() throws IOException {
			this.wrapped.flush();
		}

		@Override
		public void close() throws IOException {
			flush();
		}
		@Override
		public boolean isReady() {
			return wrapped.isReady();
		}
		@Override
		public void setWriteListener(WriteListener list) {
			this.wrapped.setWriteListener(list);
		}
	}

	// -------------------------

	private static String getDecodedApplicationPath(HttpServletRequest sreq, int prefix) {
		String uri = getApplicationPath(sreq,prefix);
		int s = uri.indexOf(';');
		if (s >= 0) {
			// strip off jsessionid and the like
			uri = uri.substring(0, s);
		}
		return uri;
	}

	private static String getApplicationPath(HttpServletRequest sreq, int prefix) {
		// get the url for the case of an include
		String uri = (String) sreq.getAttribute("javax.servlet.include.request_uri");
		if (uri == null) {
			// no include, then it must be a good request
			uri = sreq.getRequestURI().substring(prefix);
			// make sure to remove the locale part, if any
		} else {
			// fix the include uri
			String cp = (String) sreq.getAttribute("javax.servlet.include.context_path");
			if (cp != null) {
				uri = uri.substring(cp.length());
			} else {
				logger.warning("Ran into empty context path during include for "+ uri);
			}
		}
		if (!uri.startsWith("/"))
			uri = "/" + uri;
		return uri;
	}

	/**
	 * get the current request
	 */
	public static HttpServletRequest getCurrentRequest() {
		return currentRequest.get();
	}
	
	/**
	 * construct a new locale specific url
	 */
	public static String getLanguageContextPath(HttpServletRequest request, String lang) {
		String cp = (String) request.getAttribute(MicroWebConstants.MICROWEB_CONTEXT_PATH);
		Locale l = (Locale) request.getAttribute(MicroWebConstants.MICROWEB_LOCALE);
		if (l!=null) {
			cp = cp.substring(1,cp.length()-l.getLanguage().length()-1);
		}
		cp += "/"+Escaper.urlEncode(lang);
		return cp;
	}
	
	@Override
	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {

		if (this.actionprovider == null) {
			chain.doFilter(req, res);
			return;
		}

		long start = System.currentTimeMillis();
		boolean rollback = true;
		HttpServletRequest or = currentRequest.get();
		try {
			HttpServletRequest sreq = (HttpServletRequest) req;
			currentRequest.set(sreq);
			HttpServletResponse sres = (HttpServletResponse) res;

			int prefix = sreq.getContextPath().length();
			if (req.getAttribute(MicroWebConstants.MICROWEB_CONTEXT_PATH) == null) {
				String cp = sreq.getHeader(MicroWebConstants.PROXY_CONTEXT_PATH);
				if (cp == null) {
					cp = sreq.getContextPath();
				}
				if ("/".equals(cp)) {
					cp = "";
				}
				
				if (this.usei18nURLs) {
					/*
					 * 1. If there is no further path segment, then redirect to "" locale's language url
					 * 2. If there is an action, try recognizing it as a language. If it is indeed
					 *    a language code, set the request locale and include the path segment in the context path
					 *    
					 * Everything works as if the language is part of the external context path!
					 */
					String path = sreq.getRequestURI().substring(sreq.getContextPath().length());
					if (path!=null && path.length()>0 && path.charAt(0)=='/') {
						int q = path.indexOf('/',1);
						String lc = (q<0? path.substring(1) : path.substring(1,q));
						// is it a language code?
						if (LANGUAGE_CODES.contains(lc) || lc.length()==0) {
							// ok, assume it's a language code
							// add it to the context path
							cp += "/"+lc;
							prefix+=lc.length()+1;
							req.setAttribute(MicroWebConstants.MICROWEB_LOCALE, new Locale(lc));
						}
					}
				}
				req.setAttribute(MicroWebConstants.MICROWEB_CONTEXT_PATH, cp);
			}
			String mwa = (String) req.getAttribute(MICROWEB_APPLICATION_PATH);

			int p = -1;
			ResponseWrapper rw = new ResponseWrapper(sres);
			OutCome o = null;
			try {
				String uri = getDecodedApplicationPath(sreq,prefix);
				req.setAttribute(MICROWEB_APPLICATION_PATH, uri);
				/*
				 * Rules of the game: 1. longest match 2. null OutCome means to
				 * continue with next (shorter) match 3. GOTO OutCome starts
				 * over with new uri 4. Other OutComes apply and we are done
				 */
				if (logger.isLoggable(Level.FINE)) {
					boolean isInclude = sreq.getAttribute("javax.servlet.include.request_uri") != null;
					boolean isForward = sreq.getAttribute("javax.servlet.forward.request_uri") != null;
					StringBuilder b = new StringBuilder(200).append("MicroWeb.Actions:");
					if (isForward)
						b.append(" FORWARD");
					if (isInclude)
						b.append(" INCLUDE");
					b.append(" uri:").append(sreq.getRequestURI());
					b.append(" ctx:").append(sreq.getContextPath());
					b.append(" mwuri:" + uri);
					logger.fine(b.toString());
				}

				String u;
				IAction h;
				o = OutCome.goTo(uri);
				while (o != null && (o instanceof OutCome.GoTo)) {
					uri = ((OutCome.GoTo) o).getTarget();
					o = null;
					p = uri.length();
					do {
						while ((p > 0) && (uri.charAt(p - 1) == '/'))
							p--;
						u = uri.substring(0, (p == 0 ? 1 : p));
						h = this.actionprovider.getAction(u);
						if (h != null) {
							req.setAttribute(MICROWEB_PATH_INFO, uri.substring(p));
							o = h.handle(this.cfg.getServletContext(), sreq, rw);
							if (o != null) {
								o.apply(this.cfg.getServletContext(), sreq, rw);
							}
						}
					} while ((o == null) && ((p = uri.lastIndexOf("/", p - 1)) >= 0));
				}
				rollback = false;

			} catch (Exception e) {
				logger.warning("###############################################################");
				logger.warning("===============================================================");
				logger.warning("---------------------------------------------------------------");
				logger.log(Level.WARNING, "Exception occured in MicroWebFilter", e);
				logger.warning("---------------------------------------------------------------");
				logger.warning("===============================================================");
				logger.warning("###############################################################");
				throw new ServletException(e.getMessage(), e.getCause());

			} finally {
				req.setAttribute(MICROWEB_APPLICATION_PATH, mwa);
				if (rollback) {
					logger.warning("Rolling back current work unit as the request could not be completed");
					WorkUnit.setRollbackOnlyCurrent();
				}
				if (logger.isLoggable(Level.FINE)) {
					long duration = System.currentTimeMillis() - start;
					logger.fine("Request duration: " + duration + "ms for " + sreq.getRequestURI());
				}
			}
			if (p < 0 || o instanceof OutCome.PassThrough) {
				// not handled or pass through ... keep going as default
				chain.doFilter(req, rw);
			}
		} finally {
			currentRequest.set(or);
		}
	}

	@Override
	public void init(FilterConfig cfg) throws ServletException {
		this.cfg = cfg;
		String clzn = this.cfg.getInitParameter(HANDLER_PROVIDER);
		if (clzn == null) {
			clzn = DEF_HANDLERPROV;
		}
		this.usei18nURLs = Boolean.valueOf(I18NURLS);
		try {
			Class<?> clz = Class.forName(clzn, false, Thread.currentThread().getContextClassLoader());
			try {
				Constructor<?> cons = clz.getConstructor(ServletContext.class);
				this.actionprovider = (IActionProvider) cons.newInstance(this.cfg.getServletContext());
			} catch (NoSuchMethodException e) {
				this.actionprovider = (IActionProvider) clz.newInstance();
			}
		} catch (ClassNotFoundException cnfe) {
			logger.warning("Failed to find action provider (" + clzn+ ") for context "+ cfg.getServletContext().getContextPath());
		} catch (Exception e) {
			throw new ServletException("Failed to load action provider (" + clzn+ ")", e);
		}

	}

	private final static Logger logger = Logger.getLogger(MicroWebFilter.class
			.getName());
}
