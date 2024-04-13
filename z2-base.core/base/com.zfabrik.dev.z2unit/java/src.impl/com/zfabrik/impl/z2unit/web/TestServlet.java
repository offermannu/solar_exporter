/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.impl.z2unit.web;

import static com.zfabrik.z2unit.Z2UnitTestRunner.APPLICATION_OCTET_STREAM;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

import com.zfabrik.impl.z2unit.TestExecutor;
import com.zfabrik.z2unit.Z2UnitTestRunner;
import com.zfabrik.z2unit.impl.Protocol;

import junit.framework.AssertionFailedError;

/**
 * The servlet running the actual test execution
 * 
 * @author hb
 * 
 */
public class TestServlet extends HttpServlet {
	
	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
	}
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	protected void doPost(HttpServletRequest request,HttpServletResponse response) throws ServletException, IOException {
		String className = request.getParameter("className");
		String componentName = request.getParameter("componentName");

		// the body must hold a description
		if (!Z2UnitTestRunner.APPLICATION_OCTET_STREAM.equalsIgnoreCase(request.getContentType())) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST,"wrong content type "+request.getContentType());
			return;
		}
		
		if (className == null) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST,"className missing");
			return;
		}
		if (componentName == null) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST,"componentName missing");	
			return;
		}
		
		// setting up an executor
		TestExecutor t = new TestExecutor(componentName, className);
		
		// switch context class loader to target for deserialization context
		//
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(t.getClassLoader());
		try {
		
			// now deserialize the description. We do this in between setting up the test executor
			// because JUnit wants a class to generate toString names and the like, and these are 
			// relevant for error output and comparisons. So we play along...
			
			Description d;
			InputStreamReader reader = new InputStreamReader(request.getInputStream(),"utf-8");
			try {
				d = Protocol.deSerializeDescription(reader);
			} finally {
				reader.close();
			}
			t.setDescription(d);
			// now the executor is set up
	
			logger.info("Running test class "+className+" of component "+componentName+" ("+request.getQueryString()+") - "+d);
	
			//
			// from here on, we send one event after the other to the client
			//
			try {
				response.setStatus(HttpServletResponse.SC_OK);
				response.setContentType(APPLICATION_OCTET_STREAM);
				Writer writer = response.getWriter();
				t.addRunListener(new LoggingRunListener(writer));
				t.run();
			} catch (Exception e) {
				logger.log(Level.WARNING,"z2Unit execution error", e);
				response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,e.toString());
				response.setContentType("plain/text");
				e.printStackTrace(response.getWriter());
				return;
			}
		} finally {
			Thread.currentThread().setContextClassLoader(cl);
		}
	}

	
	/* 
	 * logs events to a writer in style serialization
	 */
	public final class LoggingRunListener extends RunListener {
		private Writer writer;

		public LoggingRunListener(Writer writer) throws IOException {
			this.writer = writer;
		}
		
		public void testAssumptionFailure(Failure failure) {
			try {
				Protocol.serialize(Protocol.EVENT_TEST_ASSUMPTION_FAILURE, failure, writer);
				writer.flush();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public void testFailure(Failure failure) throws Exception {
			try {
				Protocol.serialize(Protocol.EVENT_TEST_FAILURE, failure, writer);
				writer.flush();
				if (!(failure.getException() instanceof AssertionFailedError)) {
					logger.log(Level.WARNING,"z2unit test failure at "+failure.getTestHeader(),failure.getException());
				}
			} catch (Exception e) {
				logger.log(Level.WARNING,"Failed to handle z2unit test failure "+failure.getTestHeader(),e);
				throw new RuntimeException(e);
			}
		}

		@Override
		public void testFinished(Description description) throws Exception {
			try {
				Protocol.serialize(Protocol.EVENT_TEST_FINISHED, description, writer);
				writer.flush();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public void testIgnored(Description description) throws Exception {
			try {
				Protocol.serialize(Protocol.EVENT_TEST_IGNORED, description, writer);
				writer.flush();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public void testRunFinished(Result res) throws Exception {
			try {
				Protocol.serialize(Protocol.EVENT_TEST_RUN_FINISHED, res, writer);
				writer.flush();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public void testRunStarted(Description description) throws Exception {
			try {
				Protocol.serialize(Protocol.EVENT_TEST_RUN_STARTED, description, writer);
				writer.flush();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public void testStarted(Description description) throws Exception {
			try {
				Protocol.serialize(Protocol.EVENT_TEST_STARTED, description, writer);
				writer.flush();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

	private final static Logger logger = Logger.getLogger(TestServlet.class.getName());
}
