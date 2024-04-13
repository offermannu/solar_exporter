/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.util.net;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * URI is a convenience class for manipulation of URLs based on the W3C RFC 2396
 * (see <a href="http://www.w3c.org">the World Wide Web Consortium</a> pages).
 * In general a URI consists of the parts
 * <ul>
 * <li>scheme, e.g. http, https, ftp,...etc</li>
 * <li>user info</li>
 * <li>host</li>
 * <li>port</li>
 * <li>path</li>
 * <li>query</li>
 * <li>fragment</li>
 * </ul>
 * concatenated as &lt;scheme&gt;://&lt;user
 * info&gt;@&lt;host&gt;:&lt;port&gt;&lt;path&gt;?&lt;query&gt;#&lt;fragment&gt;
 * Many combinations are admissable. Examples
 * <ol>
 * <li>&lt;scheme&gt;:/&lt;path&gt;#&lt;fragment&gt;</li>
 * <li>&lt;scheme&gt;://&lt;user info&gt;@&lt;host&gt;/&lt;path&gt;</li>
 * <li>&lt;scheme&gt;://&lt;user
 * info&gt;@&lt;host&gt;:&lt;port&gt;?&lt;query&gt;</li>
 * </ol>
 * 
 * @author Henning Blohm
 * @version $Revision: #3 $ <BR>
 */

public class ParsedURL {
	private static final String ISO88591 = "ISO-8859-1";

	private String scheme;
	private String fragment;
	private String userInfo;
	private String host;
	private String port;
	private String path;
	private Map<String, Set<ParameterValueHelper>> parameters = new HashMap<String, Set<ParameterValueHelper>>(1);
	private String cachedQuery;

	private class ParameterValueHelper {
		protected String value;
		protected String valueEscaped;
		protected String nameEscaped;

		public ParameterValueHelper(String name, String value) {
			try {
				if (value != null) {
					this.value = value;
					if (this.value!=null) {
						this.valueEscaped = URLEncoder.encode(value, ISO88591);
					}
				}
				if (name != null) {
					this.nameEscaped = URLEncoder.encode(name, ISO88591);
				}
			} catch (UnsupportedEncodingException e) {
				logger.log(Level.SEVERE, "error in URI helper!", e);
			}
		}

		public boolean equals(Object obj) {
			if (obj instanceof ParameterValueHelper) {
				ParameterValueHelper vh = (ParameterValueHelper) obj;
				return ((this.value == null) && (vh.value == null)) || (this.value.equals(vh.value));
			}
			if (obj instanceof String) {
				String s = (String) obj;
				return ((this.value == null) && (s == null)) || this.value.equals(s);
			}
			return false;
		}

		public int hashCode() {
			if (this.value == null)
				return 0;
			return this.value.hashCode();
		}

	}

	/**
	 * Clean!
	 */
	private void clear() {
		setScheme(null);
		setFragment(null);
		setUserInfo(null);
		setHost(null);
		setPort(null);
		setPath(null);
		removeParameters();
	}

	/**
	 * Constructor.
	 */
	public ParsedURL() {}

	/**
	 * Constructor with initial URL.
	 * 
	 * @param uri
	 *            The URI to hanlde.
	 */
	public ParsedURL(String uri) {
		this();
		setURI(uri);
	}

	/**
	 * Constructor with initial URL.
	 * 
	 * @param uri
	 *            The URI to hanlde.
	 */
	public ParsedURL(ParsedURL uri) {
		this();
		setScheme(uri.getScheme());
		setHost(uri.getHost());
		setPort(uri.getPort());
		setPath(uri.getPath());
		setFragment(uri.getFragment());
		setUserInfo(uri.getUserInfo());

		Set<String> params = uri.getParametersSet();
		if (params != null) {
			// copy
			Iterator<String> it = params.iterator();
			if (it != null) {
				String name;
				while (it.hasNext()) {
					name = it.next();
					parameters.put(name, new HashSet<ParameterValueHelper>(uri.getParameterValueSet(name)));
				}
			}
		}
	}

	/**
	 * Sets the URI handled by this instance.
	 * 
	 * @param uri
	 */
	public void setURI(String uri) {
		parse(uri);
	}

	/**
	 * parse a given URI. We try to parse a wide variety of not necessarily "correct" URIs. 
	 * 
	 * @param uri
	 */
	protected void parse(String uri) {
		clear();
		if (uri != null) {
			// everything in front of it is the "scheme".
			int b = uri.indexOf("://"); // b = beginning
			if (b >= 0) {
				setScheme(uri.substring(0, b));
				b+=3;
			} else {
				b=0;
			}
			// Also we might check for a fragment here.
			int e = uri.lastIndexOf('#'); // e = end of resource part.
											// Possibly followed by fragment

			if (e >= 0) {
				setFragment(uri.substring(e + 1));
			} else {
				e = uri.length();
			}

			// check for the Authority definition that consists of user-info
			// host port
			int bp = -1; // bp = end of authority

			if ((b < e - 1) && (this.scheme!=null || (uri.charAt(b) != '/'  && uri.charAt(b) != '?'))) {
				//
				// this means we have a URI with host. Note that user info might precede
				// the host.
				bp = uri.indexOf('/', b);
				if (bp < 0) {
					bp = uri.indexOf('?', b);
					if (bp < 0) {
						bp = e;
					}
				}
				if ((bp > b) && (bp <= e)) {
					// user info
					int eu = uri.indexOf('@', b);

					if ((eu > b) && (eu < bp)) {
						setUserInfo(uri.substring(b, eu));
						b = eu + 1;
					}
					// host and port
					eu = uri.indexOf(':', b);
					if ((eu >= b) && (eu < bp)) {
						if (eu>b) {
							setHost(uri.substring(b, eu));
						}
						setPort(uri.substring(eu + 1, bp));
					} else {
						setHost(uri.substring(b, bp));
					}
				}
			} else {
				bp = b;
			}
			if ((bp >= 0) && (bp < e)) {
				int ep = uri.indexOf('?');

				if ((ep < 0) || (ep >= e)) {
					// no query
					ep = e;
				} else {
					// parse the query.
					StringTokenizer params = new StringTokenizer(uri.substring(ep + 1, e), "&");

					if (params != null) {
						String nameValue, name, value;
						int eq;

						while (params.hasMoreTokens()) {
							nameValue = params.nextToken();
							eq = nameValue.indexOf('=');
							try {
								if (eq < 0) {
									name = nameValue;
									value = null;
									addParameter(URLDecoder.decode(name, ISO88591), null);
								} else {
									name = nameValue.substring(0, eq);
									value = nameValue.substring(eq + 1);
									addParameter(URLDecoder.decode(name, ISO88591), URLDecoder.decode(value, ISO88591));
								}
							} catch (UnsupportedEncodingException e1) {
								// we simply ignore the single parameter.
								// this is not a cool solution, but this
								// exception
								// should never happen either
							}
						}
					}
				}
				if (bp < ep) {
					setPath(uri.substring(bp, ep));
				}
			}
			// done!
		}
	}

	/**
	 * Sets the scheme of the URI, e.g. http, https, file, ftp, ...etc.
	 * 
	 * @param scheme
	 */
	public void setScheme(String scheme) {
		this.scheme = scheme;
	}

	/**
	 * Gets the scheme of the current URI.
	 */
	public String getScheme() {
		return scheme;
	}

	/**
	 * Gets wether this URI is absolute.
	 */
	public boolean isAbsolute() {
		return (getScheme() != null);
	}

	/**
	 * Sets the fragment of the URI
	 * 
	 * @param fragment
	 */
	public void setFragment(String fragment) {
		this.fragment = fragment;
	}

	/**
	 * Gets the scheme of the current URI.
	 */
	public String getFragment() {
		return fragment;
	}

	/**
	 * Sets the User-info of the URI
	 * 
	 * @param userInfo
	 */
	public void setUserInfo(String userInfo) {
		this.userInfo = userInfo;
	}

	/**
	 * Gets the UserInfo of the current URI.
	 */
	public String getUserInfo() {
		return userInfo;
	}

	/**
	 * Sets the Host of the URI
	 * 
	 * @param host
	 */
	public void setHost(String host) {
		this.host = host;
	}

	/**
	 * Gets the Host of the current URI.
	 */
	public String getHost() {
		return host;
	}

	/**
	 * Sets the port of the URI
	 * 
	 * @param port
	 */
	public void setPort(String port) {
		this.port = port;
	}

	/**
	 * Gets the port of the current URI.
	 */
	public String getPort() {
		return port;
	}

	/**
	 * Sets the path of the URI
	 * 
	 * @param path
	 */
	public void setPath(String path) {
		this.path = path;
	}

	/**
	 * Gets the path of the current URI.
	 */
	public String getPath() {
		return path;
	}

	//
	// parameter handling
	//

	/**
	 * Removes all parameters from the query part.
	 */
	public void removeParameters() {
		this.parameters.clear();
		this.cachedQuery = null;
	}

	/**
	 * Removes the specified parameter from the query part.
	 * 
	 * @param name
	 *            the name of the parameter to be removed
	 */
	public void removeParameter(String name) {
		this.parameters.remove(name);
		this.cachedQuery = null;
	}

	/**
	 * Removes the specified name-value pair from the query part.
	 * 
	 * @param name
	 *            the name of the parameter to be removed
	 * @param value
	 */
	public void removeParameter(String name, String value) {
		Set<ParameterValueHelper> valueSet = parameters.get(name);
		if (valueSet != null) {
			valueSet.remove(new ParameterValueHelper(name,value));
			if (valueSet.size() == 0) {
				parameters.remove(name);
			}
		}
		this.cachedQuery = null;
	}

	/**
	 * Adds a parameter with value to the query part.
	 * 
	 * @param name
	 *            the name of the parameter to add.
	 * @param value
	 *            the value of the parameter to add.
	 */
	public void addParameter(String name, String value) {
		Set<ParameterValueHelper> valueSet = parameters.get(name);
		if (valueSet == null) {
			valueSet = new HashSet<ParameterValueHelper>(1);
			parameters.put(name, valueSet);
		}
		valueSet.add(new ParameterValueHelper(name, value));
		this.cachedQuery = null;
	}

	/**
	 * Sets a parameter with value to the query part removing all previous
	 * values
	 * 
	 * @param name
	 *            the name of the parameter to add.
	 * @param value
	 *            the value of the parameter to add.
	 */
	public void setParameter(String name, String value) {
		if (value == null) {
			this.removeParameter(name);
		} else {
			Set<ParameterValueHelper> valueSet = parameters.get(name);
			if (valueSet == null) {
				valueSet = new HashSet<ParameterValueHelper>(1);
				this.parameters.put(name, valueSet);
			} else {
				valueSet.clear();
			}
			valueSet.add(new ParameterValueHelper(name, value));
			this.cachedQuery = null;
		}
	}

	/**
	 * Gets all values assigned to a parameter from the query part.
	 * 
	 * @param name
	 *            the name of the parameter to get the values of.
	 * @return a String Array containing all the values of the specified
	 *         parameter.
	 */
	public String[] getParameterValues(String name) {
		Set<ParameterValueHelper> valueSet = parameters.get(name);
		if (valueSet != null) {
			String[] result = new String[valueSet.size()];
			Iterator<ParameterValueHelper> it = valueSet.iterator();
			int i = 0;
			while (it.hasNext()) {
				result[i++] = it.next().value;
			}
			return result;
		}
		return null;
	}

	/**
	 * Gets the lone value assigned to a parameter from the query part. If the
	 * parameter is multi-valued, the result can be any of the values.
	 * 
	 * @param name
	 *            the name of the parameter to get the values of.
	 * @return a String containing one value of the specified parameter.
	 */
	public String getParameterValue(String name) {
		Set<ParameterValueHelper> valueSet =  parameters.get(name);
		if (valueSet != null) {
			Iterator<ParameterValueHelper> it = valueSet.iterator();
			if ((it != null) && (it.hasNext())) {
				return it.next().value;
			}
		}
		return null;
	}

	private Set<ParameterValueHelper> getParameterValueSet(String name) {
		return parameters.get(name);
	}

	/**
	 * Gets all parameters of the query part.
	 */
	public Set<String> getParametersSet() {
		return parameters.keySet();
	}


	
	/**
	 * Gets the query String.
	 * 
	 * @return String representation of the query part of the URI
	 */
	public String getQuery() {
		if (this.cachedQuery == null) {
			Iterator<Set<ParameterValueHelper>> it = this.parameters.values().iterator();
			if ((it != null) && (it.hasNext())) {
				StringBuffer result = new StringBuffer(200);
				boolean first = true;
				Iterator<ParameterValueHelper> vi;
				ParameterValueHelper helper;
				while (it.hasNext()) {
					vi = it.next().iterator();
					while (vi.hasNext()) {
						helper = vi.next();
						if (!first)
							result.append("&");
						first = false;
						result.append(helper.nameEscaped);
						if (helper.valueEscaped!=null) {
							result.append('=');
							result.append(helper.valueEscaped);
						}
					}
				}
				this.cachedQuery = result.toString();
			}
		}
		return this.cachedQuery;
	}

	/**
	 * constructs a regular expression that matches all valid url that would result into the same 
	 * properties for all properties that are currently set - up to the last piece that has been set.
	 * The address part of the URL vs. the resource path of the URL requires special handling. If there is no property 
	 * relating to the address part, it will be completely omitted from the URL.
	 * So, if you have a URL with a host specified, all urls for that the host will be matched. If you only have a path, no host must be set and the regex stops
	 * at that path. If you have host and path, host must be set, scheme doesn't matter, path must be present, regex stops at path... and so on.
	 *  
	 * @return
	 */
	public String toRegEx() {
		StringBuilder sb = new StringBuilder("^");
		boolean address = false;
		if (this.scheme==null) {
			// no scheme: everything up to resource path is open
			sb.append("(https://|http://){1}");
		} else {
			// given a scheme, we expect everything up to the resource path
			sb.append(Pattern.quote(this.scheme+"://"));
			address=true;
		}
		String ui = this.getUserInfo();
		if (ui == null) {
			sb.append("(\\S*@)?");
		} else {
			sb.append(Pattern.quote(ui + "@"));
			address=true;
		}
		if (host == null) {
			sb.append("[\\p{Alnum}\\.]+");
		} else {
			sb.append(Pattern.quote(host));
			address=true;
		}
		if (port == null) {
			sb.append("(:\\d+)?");
		} else {
			sb.append(Pattern.quote(":" + port));
			address=true;
		}
		if (address) {
			// there was something in the address. So we want to capture it!
		} else {
			// drop the address part again
			sb.setLength(1);
		}
		int l = sb.length();
		String path = this.getPath();
		if (path == null) {
			sb.append("[\\p{Alnum}\\+%\\-/_]*");
		} else {
			sb.append(Pattern.quote(path));
			l = sb.length();
		}
		String query = this.getQuery();
		if (query == null) {
			sb.append("(\\?[^#]*)?");
		} else {
			sb.append(Pattern.quote("\\?" + query));
			l = sb.length();
		}
		String fragment = this.getFragment();
		if (fragment == null) {
			sb.append("(#\\S*)?");
		} else {
			sb.append(Pattern.quote("#" + fragment));
			l = sb.length();
		}
		sb.setLength(l);
		return sb.toString();
	}

	/**
	 * Gets a string representation of the URI.
	 * 
	 * @param absolute
	 *            if <code>true</code> the URI will be returned as absolute
	 *            URL including protocol, host and port. If set to
	 *            <code>false</code> only the path will be taken into account
	 * @return the URI as a String.
	 */
	public String toString(boolean absolute) {
		StringBuffer result = new StringBuffer(300);
		String scheme = this.getScheme();
		if (absolute) {
			if (scheme != null) {
				result.append(scheme + "://");
				String ui = this.getUserInfo();

				if (ui != null) {
					result.append(ui + "@");
				}
				String host = this.getHost();

				if (host != null) {
					result.append(host);
				}
				String port = this.getPort();

				if (port != null) {
					result.append(":" + port);
				}
			}
		}
		String path = this.getPath();

		if (path != null) {
			result.append(path);
		}
		String query = this.getQuery();

		if (query != null) {
			result.append("?" + query);
		}
		String fragment = this.getFragment();

		if (fragment != null) {
			result.append("#" + fragment);
		}
		if (result.length() > 0)
			return result.toString();
		else
			return null;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return toString(false);
	}

	private final static Logger logger = Logger.getLogger("utilities.net");

}
