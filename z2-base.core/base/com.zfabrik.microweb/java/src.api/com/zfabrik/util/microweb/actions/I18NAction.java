/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.util.microweb.actions;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.zfabrik.components.IComponentDescriptor;
import com.zfabrik.components.IComponentsManager;

public class I18NAction implements IAction {

	static private final long ONE_YEAR_IN_SEC = 365L * 24 * 60 * 60;

	private final String baseName;
	private final String objName;

	/**
	 * Creates an I18N Action for the bundle defined by the given <code>baseName</code>
	 * The {@link #handle(ServletContext, HttpServletRequest, HttpServletResponse)} method will create
	 * a JavaScript include which defines one object with name 'i18n'.
	 * This object contains the resource bundle name-value pairs for the current locale (see {@link HttpServletRequest#getLocale()}).
	 *
	 * @param baseName the full-qualified name of the Resource-Bundle - see {@link ResourceBundle#getBundle(String)}
	 */
	public I18NAction(String baseName) {
		this(baseName, "i18n");
	}

	/**
	 * Creates an I18N Action for the bundle defined by the given <code>baseName</code>
	 * The {@link #handle(ServletContext, HttpServletRequest, HttpServletResponse)} method will create
	 * a JavaScript include which defines one object with the given <code>objName</name>.
	 * This object contains the resource bundle name-value pairs for the current locale (see {@link HttpServletRequest#getLocale()}).
	 *
	 * @param baseName
	 * @param objName
	 */
	public I18NAction(String baseName, String objName) {
		this.baseName = baseName;
		this.objName = objName;
	}

	/**
	 * Computes a marker string out of the current locale and the revision of this web-app.
	 * This string can be used as cache key in the query string of i18n JavaScript URLs.
	 * Once the browser loads the i18n JS include it is stored in the browser cache as long as this method
	 * returns the same marker string. If the locale changes or the web-app is updated the string will change
	 * forcing the browser to reload the i18n JavaScript include the next time the web-app is requested.
	 *
	 * @param ctx	the servlet context (in JSPs just use <code>application</code>)
	 * @param request the request object (in JSPs just use <code>request</code>)
	 * @return String which should be used as query string for i18n-URLs
	 * @throws IOException
	 */
	public static String cacheMarker(ServletContext ctx, HttpServletRequest request) throws IOException {
		return cacheMarker(ctx, request.getLocale());
	}

	/**
	 * same as {@link #cacheMarker(ServletContext, HttpServletRequest)} but the locale can be set explicitly
	 */
	public static String cacheMarker(ServletContext ctx, Locale locale) throws IOException {
		long revision = IComponentsManager.INSTANCE.getRevision((String) ctx.getAttribute(IComponentDescriptor.COMPONENT_NAME));
		return "_zver_=" + locale + "_" + revision;
	}

	@Override
	public OutCome handle(ServletContext context, HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try {
			response.setContentType("/text/javascript");
			response.setCharacterEncoding("UTF-8");
			if (request.getParameter("_zver_") != null) {
				response.setHeader("Cache-Control", "max-age=" + ONE_YEAR_IN_SEC + ", must-revalidate, public");
			}

			PrintWriter out = response.getWriter();

			out.println("var " + this.objName + " = {");

			String l = request.getParameter("locale");
			Locale loc = l != null? new Locale(l) : request.getLocale();

			ResourceBundle bundle = ResourceBundle.getBundle(this.baseName, loc, Thread.currentThread().getContextClassLoader());
			Enumeration<String> loopKeys = bundle.getKeys();
			while (loopKeys.hasMoreElements()) {
				String key = loopKeys.nextElement();
				String value = bundle.getString(key);
				value = (value != null)? value.trim(): "";
				if (value.startsWith("[") && value.endsWith("]")) {
					// json array
					out.println("  '" + key + "': " + value + ",");
				} else {
					out.println("  '" + key + "': '" + value + "',");
				}
			}
			out.println("  '': ''}");
			out.flush();
			return OutCome.done();

		} catch (Exception e) {
			throw new RuntimeException("Failed to write Resource Bundle " + this.baseName, e);
		}
	}

}
