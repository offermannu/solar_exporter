/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.servletjsp.security;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.server.HttpChannel;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.HandlerWrapper;

/**
 * A simple whitelisting filter for Jetty access to grant access for
 * Web application paths for non localhost sources.
 * <p>
 * This filter can be used to increase security by restricting access to
 * Web applications that are only used for debugging and development 
 * to access from localhost.
 * </p>
 * <p>
 * To configure see the Jetty configuration file <code>z2-nonlocalhostwhitelist.xml</code>
 * that might look like this to grant access to the root path "/" and the context path
 * <code>/abc</code>:
 * <pre>
 * {@code
 * <Configure id="Server" class="org.eclipse.jetty.server.Server">
 *   <Call name="insertHandler">
 *     <Arg>
 *       <New id="NonLocalhostWhitelist" class="com.zfabrik.servletjsp.security.NonLocalhostWhitelist">
 *         <Set name="patterns">
 *           <Array type="String">
 *             <Item>^/abc($|/.*)</Item>
 *             <Item>^/$</Item>
 *         </Array>
 *       </Set>
 *     </New>
 *   </Arg>
 * </Call>
 * </Configure>
 * }
 * </pre>   
 * @author hb
 *
 */
public class NonLocalhostWhitelist extends HandlerWrapper {
	private final static Logger LOG = Logger.getLogger(NonLocalhostWhitelist.class.getName());

	private List<Pattern> patterns;
	
	
	public void setPatterns(String[] patterns) {
		this.patterns = Arrays.asList(patterns).stream().map(Pattern::compile).collect(Collectors.toList());
	}
	
	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
		
		InetSocketAddress address = getRemoteAdress(baseRequest);
        if (address != null) {
            if (isLocalhost(address) || isPatternMatch(baseRequest.getPathInfo())) {
            	// grant access by passing the request on to the wrapped handler
            	getHandler().handle(target, baseRequest, request, response);
            } else {
            	// reject
                LOG.warning("Rejecting remote access on path " + baseRequest.getPathInfo()+" from "+address);
                response.sendError(HttpStatus.FORBIDDEN_403);
                baseRequest.setHandled(true);
            }
        } else {
        	// reject
            LOG.warning("No remote address info for request " + baseRequest.getPathInfo());
            response.sendError(HttpStatus.FORBIDDEN_403);
            baseRequest.setHandled(true);
        }
	}


	private boolean isPatternMatch(String path) {
		if (path != null) {
		    for (Pattern pattern : patterns) {
		        if (pattern.matcher(path).matches()) {
		        	// matches => grant access
		            return true;
		        }
		    }
		}
		return false;
	}

	private boolean isLocalhost(InetSocketAddress address) {
		InetAddress iNetSocketAddress = address.getAddress();
		boolean localhost = (iNetSocketAddress != null && iNetSocketAddress.isLoopbackAddress());
		return localhost;
	}

	private InetSocketAddress getRemoteAdress(Request baseRequest) {
		InetSocketAddress address = null;
		// get the remote address
        HttpChannel channel = baseRequest.getHttpChannel();
        if (channel != null) {
            EndPoint endp = channel.getEndPoint();
            if (endp != null) {
            	SocketAddress remoteSocketAddress = endp.getRemoteSocketAddress();
            	if (remoteSocketAddress instanceof InetSocketAddress) {
            		address = (InetSocketAddress) remoteSocketAddress;
            	}
            }
        }
		return address;
	}
	
	@Override
	protected void doStart() throws Exception {
		LOG.info("Enforcing Non-Localhost-Whitelisting. Granting access for patterns "+this.patterns);
		super.doStart();
	}


}
