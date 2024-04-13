/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.impl.javadoc;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.zfabrik.components.IComponentDescriptor;
import com.zfabrik.components.IComponentsManager;
import com.zfabrik.components.java.IJavaComponent;
import com.zfabrik.components.java.JavaComponentUtil;
import com.zfabrik.impl.javadoc.ComponentJavaDoc.JavaDocType;
import com.zfabrik.util.expression.X;
import com.zfabrik.util.html.Escaper;

/**
 * Java Doc filter - serves and triggers on-demand creation of java component
 * java docs
 * 
 * @author hb
 * 
 */
public class JDocFilter implements Filter {
	private final static Logger LOG = Logger.getLogger(JDocFilter.class.getName());
	private static final String LAST_MODIFIED = "Last-Modified";
	private static final String IF_MODIFIED_SINCE = "If-Modified-Since";
	private FilterConfig cfg;

	public void init(FilterConfig cfg) throws ServletException {
		this.cfg = cfg;
	}

	public void destroy() {
		this.cfg = null;
	}

	public void doFilter(ServletRequest req, ServletResponse res,FilterChain chain) throws IOException, ServletException {
		HttpServletRequest sreq = (HttpServletRequest) req;
		HttpServletResponse resp = (HttpServletResponse) res;

		// paths are /<component>/<api|impl|any>/<jdoc path>
		String path = sreq.getRequestURI().substring(sreq.getContextPath().length());
		if (path.length() > 1) {
			int p = path.indexOf('/', 1);
			if (p >= 0) {
				String component = Escaper.urlDecode(path.substring(1, p),'!');
				component = JavaComponentUtil.fixJavaComponentName(component);
				path = path.substring(p);
				if (path.length() > 1) {
					p = path.indexOf('/', 1);
					if (p >= 0) {
						JavaDocType type = JavaDocType.parseType(Escaper.urlDecode(path.substring(1, p),'!'));
						List<File> roots = checkDocs(component, type, true);
						// try all roots
						for (File f:roots) {
							path = path.substring(p);
							if (path.length()>0) {
								f = new File(f, path);
								if (f.exists()) {
									// check last modified request
									long ifModifiedSince = sreq	.getDateHeader(IF_MODIFIED_SINCE);
									if (ifModifiedSince >= 0) {
										// make sure to reduce to seconds precision!
										long cr = f.lastModified();
										if (((cr / 1000) * 1000) <= ifModifiedSince) {
											// nothing to do...
											resp.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
											return;
										}
									}
									// ok, write it out
									// headers
									// Important: set Last-Modified!
									resp.setDateHeader(LAST_MODIFIED, f.lastModified());
									resp.setContentType(this.cfg.getServletContext().getMimeType(f.getName()));
									resp.setContentLength((int) f.length());
	
									// stream out
									OutputStream out = resp.getOutputStream();
									InputStream in = new FileInputStream(f);
									try {
										byte[] buffer = new byte[16384];
										int l;
										while ((l = in.read(buffer)) >= 0) {
											out.write(buffer, 0, l);
										}
									} finally {
										in.close();
									}
									return;
								}
							} else {
								// url ends after type string. redirect
								redirect2Index(sreq,resp);
								return;
							}
						}
						if (roots.isEmpty()) {
							// not found
							gotoList(sreq,resp,true);
							return;
						}
					} else {
						// url ends at type string. redirect
						redirect2Index(sreq,resp);
						return;
					}
				} else {
					// url ends at component
					redirect2Index(sreq,resp);
					return;
				}
			} else {
				// url ends at component
				redirect2Index(sreq,resp);
				return;
			}
		} else {
			// index
			gotoList(sreq,resp,false);
			return;
		}
		resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
		chain.doFilter(req,res);
	}

	private void gotoList(HttpServletRequest sreq, HttpServletResponse resp,boolean notfound) throws IOException, ServletException {
		// present a choice of components
		sreq.setAttribute("notfound", notfound);
		Collection<String> cs = IComponentsManager.INSTANCE.findComponents(X.var(IComponentDescriptor.COMPONENT_TYPE).eq(X.val(IJavaComponent.TYPE)));
		Set<String> apis  = new HashSet<>();
		Set<String> impls = new HashSet<>();
		for (String cn : cs) {
			if (hasDocs(cn, JavaDocType.API)) {
				apis.add(cn);
			}
			if (hasDocs(cn, JavaDocType.IMPL)) {
				impls.add(cn);
			}
		}
		TreeSet<String> scs = new TreeSet<String>(cs);
		sreq.setAttribute("components", scs);
		sreq.setAttribute("apis", apis);
		sreq.setAttribute("impls", impls);
		sreq.getRequestDispatcher("/WEB-INF/all.jsp").forward(sreq, resp);		
	}

	private void redirect2Index(HttpServletRequest sreq,HttpServletResponse resp) throws IOException, ServletException {
		String s = sreq.getRequestURI();
		if (!s.endsWith("/")) {
			s += "/";
		}
		if (s.endsWith("/api/") || s.endsWith("/impl/")) {
			resp.sendRedirect(s+"index.html");
		} else {
			resp.sendRedirect(s+"api/index.html");
		}
	}

	private boolean hasDocs(String component, JavaDocType type) {
		boolean hasAny = false;
		if (type!=null) {
			try {
				ComponentJavaDoc componentJavaDoc = new ComponentJavaDoc(component,type);
				hasAny = componentJavaDoc.getSrcFolders().stream().filter(File::exists).findAny().isPresent();
				if (!hasAny) {
					// check for existing (can happen for core components)
					hasAny = Optional.ofNullable(componentJavaDoc.getRootFolder(false)).filter(File::exists).isPresent();
				}
			} catch (Exception e) {
				LOG.info("Accessing Java "+component+" for javadocs failed ("+e.getMessage()+")");
			}
		}
		return hasAny;
	}

	
	/*
	 * Check for javadocs of the given type from the given component.
	 * If not found, trigger creation
	 */
	private List<File> checkDocs(String component, JavaDocType type, boolean create) {
		LinkedList<File> roots = new LinkedList<File>();
		if (type!=null) {
			roots.add(new ComponentJavaDoc(component,type).getRootFolder(create));
		}
		return roots;
	}
}
