/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.dev.z2jupiter.internal.client;

import static com.zfabrik.dev.z2jupiter.internal.engine.Z2JupiterTestEngine.LOG;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.junit.platform.launcher.TestExecutionListener;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zfabrik.dev.z2jupiter.Z2JupiterTestable;
import com.zfabrik.dev.z2jupiter.internal.transfer.Z2JupiterLauncherDiscoveryRequestDto;
import com.zfabrik.dev.z2jupiter.internal.transfer.Z2JupiterListenerEventDto;
import com.zfabrik.dev.z2jupiter.internal.transfer.Z2JupiterTestPlanDto;
import com.zfabrik.dev.z2jupiter.internal.util.Json;
import com.zfabrik.util.html.Escaper;

/**
 * Z2 Jupiter remote client. Processes requests for test discovery and test plan execution. 
 */
public class Z2JupiterClient {
	public static final String ENV_VAR_Z2_JUPITER_URL = "Z2_JUPITER_URL";
	public static final String SYSPROP_Z2JUPITER_URL = "com.zfabrik.dev.z2jupiter.url";

	public static final String ENV_VAR_Z2_JUPITER_USER = "Z2_JUPITER_USER";
	public static final String SYSPROP_Z2JUPITER_USER = "com.zfabrik.dev.z2jupiter.user";

	public static final String ENV_VAR_Z2_JUPITER_PASSWORD = "Z2_JUPITER_PASSWORD";
	public static final String SYSPROP_Z2JUPITER_PASSWORD = "com.zfabrik.dev.z2jupiter.password";

	public static final String APPLICATION_JSON_CHARSET_UTF_8 = "application/json;charset=utf-8";
	public static final String CALL_DISCOVER = "discover";
	public static final String CALL_EXECUTE = "execute";
	public static final String CALL_RUN = "run";
	public static final String HEADER_CONTENT_TYPE = "Content-Type";
	public static final String PARAM_COMPONENT_NAME = "componentName";
	public static final String PARAM_TEST_PLAN_ID = "testPlanId";
	public static final String HTTP_METHOD_POST = "POST";
	public static final String HTTP_METHOD_GET = "GET";

	private ConnectionConfig config;
	private ObjectMapper om;


	// actual connection config
	private static class ConnectionConfig {
		private String url;
		private String user;
		private String password;
		private String componentName;
		
		public ConnectionConfig(Z2JupiterTestable a) {
			this.componentName = trimToNull(a.componentName());
			if (this.componentName==null) {
				throw new IllegalArgumentException("No componentName specificed on "+Z2JupiterTestable.class.getSimpleName());
			}
			// determine config with fallbacks
			url = getConfig("url", true, a::url,SYSPROP_Z2JUPITER_URL,ENV_VAR_Z2_JUPITER_URL);
			user = getConfig("user", true, a::user,SYSPROP_Z2JUPITER_USER,ENV_VAR_Z2_JUPITER_USER);
			password = getConfig("password", false, a::password,SYSPROP_Z2JUPITER_PASSWORD,ENV_VAR_Z2_JUPITER_PASSWORD);
		}

		private static String getConfig(String name, boolean show, Supplier<String> getter, String sysProp, String envVar) {
			String v = trimToNull(System.getProperty(sysProp));
			if (v==null) {
				v = trimToNull(System.getenv(envVar));
				if (v==null) {
					v = trimToNull(getter.get());
					LOG.info("Retrieving "+name+(show? "="+v:"")+" from annotation");
				} else {
					LOG.info("Retrieving "+name+(show? "="+v:"")+" from environment variable "+envVar);
				}
			} else {
				LOG.info("Retrieving "+name+(show? "="+v:"")+" from system property "+sysProp);
			}
			return v;
		}

		@Override
		public String toString() {
			return "ConnectionConfig [url=" + url + ", user=" + user + ", componentName=" + componentName + "]";
		}
	}
	

	public Z2JupiterClient(Z2JupiterTestable annotation) {
		this.config = new ConnectionConfig(annotation);
	}


	public Z2JupiterTestPlanDto discover(Z2JupiterLauncherDiscoveryRequestDto discoverRequest) {
		try {
			HttpURLConnection conn = prepareRequest(
				HTTP_METHOD_POST, 
				CALL_DISCOVER, 
				PARAM_COMPONENT_NAME, config.componentName
			);
			try (OutputStream out = conn.getOutputStream()) {
				om().writeValue(out, discoverRequest);
			}
			sendRequest(conn);
			try (InputStream in = conn.getInputStream()) {
				return om().readValue(in,Z2JupiterTestPlanDto.class);
			}
		} catch (JsonParseException jpe) {
			throw new RuntimeException("Bad response from "+this.config,jpe);
		} catch (IOException e) {
			throw new RuntimeException("Failed to request test discovery at "+this.config,e);
		}
	}

	/**
	 * Execute the test plan with the given id, previously discovered via {@link #discover(Z2JupiterLauncherDiscoveryRequestDto)}.
	 * We pass all events back to a consumer of {@link Z2JupiterListenerEventDto}. It is up to the client
	 * to turn this into calls to {@link TestExecutionListener}. 
	 */
	public void execute(String id, Consumer<Z2JupiterListenerEventDto> l) {
		try {
			HttpURLConnection conn = prepareRequest(
				HTTP_METHOD_GET,
				CALL_EXECUTE,
				PARAM_COMPONENT_NAME, config.componentName,
				PARAM_TEST_PLAN_ID, id
			);
			// the test plan id is the session id!
			setSessionId(conn, id);
			// send it
			sendRequest(conn);
			// read the events that come back and send them off
			try (JsonParser parser = om().getFactory().createParser(conn.getInputStream())) {
				if (parser.nextToken()!=JsonToken.START_ARRAY) {
					throw new IllegalStateException("Excepted start of array but got "+parser.currentToken());
				}
				while (parser.nextToken()==JsonToken.START_OBJECT) {
					Z2JupiterListenerEventDto e = om().readValue(parser, Z2JupiterListenerEventDto.class);
					l.accept(e);
				}
				if (parser.currentToken()!=JsonToken.END_ARRAY) {
					throw new IllegalStateException("Excepted end of array but got "+parser.currentToken());
				}
			}
		} catch (JsonParseException jpe) {
			throw new RuntimeException("Bad response from "+this.config,jpe);
		} catch (IOException e) {
			throw new RuntimeException("Failed to request test discovery at "+this.config,e);
		}
	}


	/**
	 * Prepare a connection for sending
	 */
	private HttpURLConnection prepareRequest(String method, String call, String ... params ) throws IOException, MalformedURLException, ProtocolException {
		HttpURLConnection conn = (HttpURLConnection) new URL(buildUrl(config.url, call, params)).openConnection();
		if (config.user!=null && config.password!=null) {
			conn.setRequestProperty(
				"Authorization",
				"Basic " + Base64.getEncoder().encodeToString((config.user + ":" + config.password).getBytes(StandardCharsets.ISO_8859_1))
			);
		}
		
		HttpURLConnection.setFollowRedirects(true);
		conn.setRequestMethod(method);
		if (HTTP_METHOD_POST.equals(method)) {
			// set json
			conn.addRequestProperty(HEADER_CONTENT_TYPE, APPLICATION_JSON_CHARSET_UTF_8);
			conn.setDoOutput(true);
		}
		return conn;
	}

	/**
	 * Set the session id (that is our test plan id)
	 */
	private void setSessionId(HttpURLConnection conn, String sessionId) throws IOException {
		// set session id cookie
		conn.setRequestProperty("Cookie", "JSESSIONID="+sessionId);
	}
	
	/**
	 * send the request
	 */
	private void sendRequest(HttpURLConnection conn) throws IOException {
		if (conn.getResponseCode()!=200) {
			throw new RuntimeException(
				"Request failed ("+
				conn.getResponseCode()+ 
				(conn.getResponseMessage()!=null? "/"+conn.getResponseMessage():"")+
				")"
			);
		}
	}


	/**
	 * build a complete url with params
	 */
	private String buildUrl(String url, String call, String ... nv) {
		StringBuilder sb = new StringBuilder()
			.append(url).append("/").append(call);
		int i=0;
		int l = sb.length();
		while (i<nv.length) {
			String name = nv[i++];
			String value;
			if (i==nv.length || (value=nv[i++])==null) {
				throw new IllegalArgumentException("Missing value for param "+name);
			}
			sb.append("&").append(name).append("=").append(Escaper.urlEncode(value));
		}
		// fix start of query params
		if (sb.length()>l) {
			sb.setCharAt(l, '?');
		}
		return sb.toString();
	}
	
	private ObjectMapper om() {
		if (om==null) {
			om = Json.om();
		}
		return om;
	}
	
	private static String trimToNull(String in) {
		if (in!=null) {
			in = in.trim();
			if (in.length()==0) {
				in = null;
			}
		}
		return in;
	}
}
