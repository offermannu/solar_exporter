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

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.zfabrik.util.microweb.MicroWebConstants;

/**
 * Representation of an OutCome of an action. OutCome's describe how to proceed
 * @author hb
 *
 */
public abstract class OutCome {
	
	// -------- continue, i.e. ignore the microweb filter
	public final static class PassThrough extends OutCome {

		private PassThrough() {
			super();
		}

		public void apply(ServletContext context, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		}		

		public String toString() {
			return "OutCome PassThrough";
		}
	}

	
	// -------- forward
	public final static class Forward extends OutCome {
		private String prefix; // an include prefix, that will be passed on
		private String target;

		private Forward(String prefix, String target) {
			super();
			this.prefix = prefix;
			this.target = target;
		}

		public void apply(ServletContext context, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
			new MicroWebDispatcher(context,request,target,prefix).forward(request,response);
		}		

		public String toString() {
			return "OutCome Forward: target="+target+", prefix="+prefix;
		}
	}
	
	// -------- include
	public final static class Include extends OutCome {
		private String prefix; // an include prefix, that will be passed on
		private String target;

		private Include(String prefix, String target) {
			super();
			this.prefix = prefix;
			this.target = target;
		}

		public void apply(ServletContext context, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
			new MicroWebDispatcher(context,request,target,prefix).include(request,response);
		}		

		public String toString() {
			return "OutCome Include: target="+target+", prefix="+prefix;
		}
	}

	// -------- decorate
	public final static class Decorate extends OutCome {
		private String decorator;
		private String prefix; // an include prefix, that will be passed on
		private String target;

		private Decorate(String decorator, String prefix, String target) {
			super();
			this.decorator = decorator;
			this.prefix = prefix;
			this.target = target;
		}

		public void apply(ServletContext context, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
			// no recursive decoration. Test if this target has been decorated.
			MicroWebDispatcher mwd = new MicroWebDispatcher(context,request,target,prefix);
			if (mwd.decoratorForward(request, response)) {
				// decoree content has been inserted as request attribute
				mwd = new MicroWebDispatcher(context,request,decorator,null);
				mwd.forward(request,response);
			}
		}		

		public String toString() {
			return "OutCome Decorate: target="+target+", prefix="+prefix+", decorator:"+this.decorator;
		}
	}

	// -------- error
	public final static class Error extends OutCome {
		private int    sc;
		private String message;

		private Error(int sc, String message) {
			super();
			this.sc = sc;
			this.message = message;
		}

		public void apply(ServletContext context, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
			if (this.message!=null) {
				response.sendError(this.sc,this.message);
			} else {
				response.sendError(this.sc);
			}
		}		

		public String toString() {
			return "OutCome Error: sc="+sc+", message="+message;
		}
	}
	
	// -------- error
	public final static class Done extends OutCome {
		private int    sc;

		private Done(int sc) {
			super();
			this.sc = sc;
		}

		public void apply(ServletContext context, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
			response.setStatus(this.sc);
		}		

		public String toString() {
			return "OutCome Done: sc="+sc;
		}
	}

	// -------- redirect
	public final static class Redirect extends OutCome {
		private String target;

		private Redirect(String target) {
			this.target = target;
		}

		public void apply(ServletContext context, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
			String t = target.toLowerCase();
			if (t.startsWith("http://") || t.startsWith("https://")) {
				response.sendRedirect(target);
			} else {
				if (target.startsWith("//")) {
					response.sendRedirect(response.encodeRedirectURL(target.substring(1)));
				} else {
					response.sendRedirect(response.encodeRedirectURL(request.getContextPath()+target));
				}
			}
		}		

		public String toString() {
			return "OutCome Redirect: target="+target;
		}
	}
	
	// -------- delegation to another action instance  
	public final static class Delegate extends OutCome {
		private IAction action;

		private Delegate(IAction action) {
			this.action = action;
		}

		public void apply(ServletContext context, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
			Object o = request.getAttribute(MicroWebConstants.MICROWEB_INTERNAL_DISPATCH);
			try	{
				request.setAttribute(MicroWebConstants.MICROWEB_INTERNAL_DISPATCH,Boolean.TRUE);
				OutCome oc = this.action.handle(context, request, response);
				if (oc!=null) {
					oc.apply(context, request, response);
				}
			} finally {
				request.setAttribute(MicroWebConstants.MICROWEB_INTERNAL_DISPATCH, o);
			}
		}		

		public String toString() {
			return "OutCome Delegate: action="+action;
		}
	}
	
	// -------- goto (like a forward without going outside - and therefore independent of the dispatch modes) 
	public final static class GoTo extends OutCome {
		private String target;

		private GoTo(String target) {
			this.target = target;
		}

		public void apply(ServletContext context, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
			// doesn't do anything. GoTo is handled in the filter loop
		}		

		public String toString() {
			return "OutCome GoTo: target="+target;
		}
		
		public String getTarget() {
			return target;
		}
	}
	
	// can be used concurrently
	private final static OutCome PASSTHROUGH = OutCome.passThrough();
	
	/**
	 * implies by-passing of the Microweb filter and continue with the filter chain
	 */
	public static OutCome passThrough() {
		return PASSTHROUGH;
	}
	
	/**
	 * implies a direct forward
	 * @param target
	 * @return
	 */
	public static OutCome forward(String target) {
		return new Forward(null,target);
	}
	
	public static OutCome forward(String prefix,String target) {
		return new Forward(prefix,target);
	}
	/**
	 * implies an include
	 * @param target
	 * @return
	 */
	public static OutCome include(String target) {
		return new Include(null,target);
	}

	public static OutCome include(String prefix, String target) {
		return new Include(prefix,target);
	}
	
	/**
	 * implies a decoration of a forward target
	 */
	public static OutCome decorate(String decorator, String target) {
		return new Decorate(decorator,null,target);
	}
	
	public static OutCome decorate(String decorator, String prefix, String target) {
		return new Decorate(decorator,prefix,target);
	}
	/**
	 * Used to return with a status code.
	 */
	public static OutCome error(int sc) {
		return new Error(sc,null);
	}
	
	public static OutCome error(int sc, String message) {
		return new Error(sc,message);
	}
	
	public static OutCome done() {
		return new Done(HttpServletResponse.SC_OK);
	}

	public static OutCome done(int sc) {
		return new Done(sc);
	}

	/**
	 * implies a direct redirect, without going through the engine again
	 * @param target
	 * @return
	 */
	public static OutCome redirect(String target) {
		return new Redirect(target);
	}

	/**
	 * implies an internal forward, going through the engine again
	 * @param target
	 * @return
	 */
	public static OutCome goTo(String target) {
		return new GoTo(target);
	}

	/**
	 * wraps another actions and makes sure there is no security
	 */
	public static OutCome delegate(IAction action) {
		return new Delegate(action);
	}

	public abstract void apply(ServletContext context, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException;

}
