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


/**
 * The MicroWeb web utility stores several pieces of processing information on the current request object.
 * These can be accessed as attributes using the constants below.
 * @author hb
 *
 * */
public final class MicroWebConstants {
	private MicroWebConstants() {}
	
	/**
	 * The actual context path that was used on the client request. This will not be overwritten in case of internal forwarding.
	 * It may be different than the request's context path also in reverse proxy situations. In order to generate client urls, this
	 * context path is the choice.
	 * <br/>
	 * Note: Reverse proxies should take care of paths in headers and cookies (body content is a different story). Use the 
	 * request's context path for headers and cookis. Use the microweb context path for body urls. 
	 * <br/>
	 * Note, this path is constructed so that it can always be concatenated with a following path that has a leading slash. In particular,
	 * this path may be "" for the server's root path.
	 * <br>
	 * Note furthermore that forwarding with namespace translation extends this path as fits, so that it can always 
	 * be used to consistently compute paths to local resources that will follow the same forwarding scheme. 
	 */
    public static final String MICROWEB_CONTEXT_PATH   = "microweb_context_path";
	/**
	 * The application path is the path used within the microweb application (regardless of includes) used to determine actions. It is close in meaning
	 * to the request URI.
	 * <br> 
	 * Note: This path always has a leading slash.
	 */
	public static final String MICROWEB_APPLICATION_PATH = "microweb_application_path";
	/**
	 * The requested Locale as far as Microweb is concerned. May be null
	 */
	public static final String MICROWEB_LOCALE = "microweb_locale";
	/**
	 * The microweb path info, is the equivalent of a servlet's path info for microweb actions. It is the path trailing the the part of the URI that 
	 * was used to determine the current action. 
	 */
	public static final String MICROWEB_PATH_INFO = "microweb_path_info";

	/**
	 * Used to indicate internal wrapping of actions. If this attribute is set other than <code>null</code>, the action will be handled
	 * liks an internal dispatch, in particular there will be no security checking. 
	 */
	public static final String MICROWEB_INTERNAL_DISPATCH = "com.zfabrik.microweb.internaldispatch";
	/**
	 * header field to specify context path used by the client request. This is to be set 
	 * by reverse proxies to help the backend translate good urls.
	 */
	public final static String PROXY_CONTEXT_PATH = "X-Z2-CONTEXT-PATH";
	

}
