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
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.zfabrik.util.datetime.ISO8601DateFormat;

// translate parameters to setters/getters,
// look for actions for any other parameter
public class BeanDerivedAction implements IAction {

	private static final String BY_DEFAULT = "byDefault";

	// mapping from primitive to wrapper
	private final static Map<Class<?>,Class<?>> WRAPPER = new HashMap<Class<?>, Class<?>>();

	static {
		WRAPPER.put(Boolean.TYPE,Boolean.class);
		WRAPPER.put(Character.TYPE,Character.class);
		WRAPPER.put(Integer.TYPE,Integer.class);
		WRAPPER.put(Long.TYPE,Long.class);
		WRAPPER.put(Float.TYPE,Float.class);
		WRAPPER.put(Double.TYPE,Double.class);
		WRAPPER.put(Short.TYPE,Short.class);
		WRAPPER.put(Byte.TYPE,Byte.class);
	}

	private final static Class<?>[] ERROR_PARAMS = new Class<?>[]{HttpServletRequest.class,String.class,Exception.class};

	private final Class<?> beanClz;
	private Method create,init,error;
	private final Map<String, Method> setters;
	private final Map<String, Method> getters;
	private final Map<String, Method> actions;
	private Constructor<?> cons;
	private final Object[] initParams;

	/**
	 * A bean derived action is exactly that: An action derived from a simple Java Bean. In short, all Java Bean setters will receive request attributes and request
	 * parameters (in that order), while all getters translate to request attributes after invocation of the action method.
	 * <br>
	 * Action methods are identified by the prefix of "do" and the action name with a capitalized first letter. For example "doBookJourney" for an action "bookJourney" or
	 * "BookJourney". The following signatures are valid for action methods:
	 * <ul>
	 * <li>OutCome do&lt;Action&gt;(); </li>
	 * <li>OutCome do&lt;Action&gt;(? extends {@link HttpServletRequest}); </li>
	 * <li>OutCome do&lt;Action&gt;(? extends {@link HttpServletRequest},? extends {@link HttpServletResponse}); </li>
	 * <li>OutCome do&lt;Action&gt;(? extends {@link ServletContext}, ? extends {@link HttpServletRequest}); </li>
	 * <li>OutCome do&lt;Action&gt;(? extends {@link ServletContext}, ? extends {@link HttpServletRequest},? extends {@link HttpServletResponse}); </li>
	 * </ul>
	 * One or more actions may be called during a request. Every parameter classifies as an action as long as their is a matching
	 * action method.
	 * <br>
	 * Before invoking any action but after applying request parameters, an init method will be called, if existing. If that method returns a non-null outcome, no action
	 * will be called. Any valid action method signature is also a valid init method signature.
	 *
	 * @param beanClz
	 */

	public BeanDerivedAction(Class<?> beanClz, Object ... initParams) {
		this.beanClz = beanClz;
		this.initParams = initParams;

		/*
		 * Find a good constructor
		 */
		if (initParams==null || initParams.length==0) {
			try {
				this.cons = beanClz.getConstructor(new Class<?>[]{});
			} catch (Exception e) {
				throw new RuntimeException("Failed to search for constructor of "+beanClz);
			}
			if (this.cons==null) {
				throw new IllegalArgumentException("No no-args constructor found on "+beanClz);
			}
		} else {
			Constructor<?>[] cs = beanClz.getConstructors();

		main:
			for (Constructor<?> c : cs) {
				Class<?>[] types = c.getParameterTypes();
				if (types.length==initParams.length) {
					// assume it's good
					for (int i=0; i<types.length;i++) {
						if (initParams[i]!=null && !types[i].isAssignableFrom(initParams[i].getClass())) {
							// won't work - try next
							continue main;
						}
					}
					// worked!
					this.cons = c;
					break main;
				}
			}
			if (this.cons==null) {
				throw new IllegalArgumentException("No constructor matching params found on "+beanClz);
			}
		}

		/*
		 * introspect: all set&lt;Name&gt;(&lt;Type&gt;), &lt;Type&gt;
		 * get&lt;Name&gt; will be translated to parameter receivers and
		 * providers. It will push in requests attributes and parameters (in this order).
		 * It will pull out request attributes.
		 * It supports a "init" method that can intercept any action as well as prepare state before
		 * setters will be called.
		 * It supports an error(request,propertyname,exception) method that will be called upon
		 * parameter transport problems.
		 *
		 * A method do&lt;Action&gt; will be executed in case there is parameter
		 * "action" with value &lt;action&gt;.
		 */
		this.setters = new HashMap<String, Method>();
		this.getters = new HashMap<String, Method>();
		this.actions = new HashMap<String, Method>();
		Method[] ms = beanClz.getMethods();
		String name;
		for (Method m : ms) {
			name = m.getName();
			if ((name.startsWith("set")) && (m.getParameterTypes().length == 1) && (m.getReturnType() == void.class)) {
				this.setters.put(propName(name,3), m);
			} else if ((name.startsWith("is")) && (m.getParameterTypes().length == 0) && ((m.getReturnType() ==  Boolean.class) || (m.getReturnType()==boolean.class))) {
				this.getters.put(propName(name,2), m);
			} else if ((name.startsWith("get")) && (m.getParameterTypes().length == 0) && (m.getReturnType() != null)) {
				this.getters.put(propName(name,3), m);
			} else if (name.startsWith("do") && _isActionMethod(m)	) {
				this.actions.put(actionName(name), m);
			} else if (name.equals("init") && 	_isActionMethod(m)) {
				this.init = m;
			} else if (name.equals("create") && 	_isActionMethod(m)) {
				this.create = m;
			}  else if ((name.equals("error")) && (Arrays.equals(ERROR_PARAMS,m.getParameterTypes()))) {
				this.error = m;
			}
		}
	}

	private boolean _isActionMethod(Method m) {
		return ((m.getParameterTypes().length == 0) ||
		((m.getParameterTypes().length==1) &&
		 (HttpServletRequest.class.isAssignableFrom(m.getParameterTypes()[0])))
		 ||
		((m.getParameterTypes().length==2) &&
				 (ServletContext.class.isAssignableFrom(m.getParameterTypes()[0])) &&
				 (HttpServletRequest.class.isAssignableFrom(m.getParameterTypes()[1])))
		 ||
		((m.getParameterTypes().length==2) &&
				 (HttpServletRequest.class.isAssignableFrom(m.getParameterTypes()[0])) &&
				 (HttpServletResponse.class.isAssignableFrom(m.getParameterTypes()[1])))

		 ||
		((m.getParameterTypes().length==3) &&
				 (ServletContext.class.isAssignableFrom(m.getParameterTypes()[0])) &&
				 (HttpServletRequest.class.isAssignableFrom(m.getParameterTypes()[1])) &&
				 (HttpServletResponse.class.isAssignableFrom(m.getParameterTypes()[2]))) && (OutCome.class.isAssignableFrom(m.getReturnType())));
	}

	private String propName(String name, int l) {
		String p = name.substring(l);
		return Character.toLowerCase(p.charAt(0)) + p.substring(1);
	}

	private String actionName(String name) {
		return Character.toLowerCase(name.charAt(2)) + name.substring(3);
	}

	@Override
	public OutCome handle(ServletContext context, HttpServletRequest req, HttpServletResponse res) throws ServletException,
			IOException {
		Object bean;
		try {
			bean = this.cons.newInstance(this.initParams);

		} catch (InvocationTargetException ite) {
			Throwable cause = ite.getTargetException();
			if (cause == null) {
				cause = ite.getCause();
			}
			throw new ServletException("Failed to instantiate action bean of type " + this.beanClz, cause);

		} catch (Exception e) {
			throw new ServletException("Failed to instantiate action bean of type " + this.beanClz, e);
		}

		// call create if any
		if (this.create!=null) {
			try {
				OutCome o = invokeActionMethod(context, req, res, bean, this.create);
				if (o!=null) {
					return o;
				}
			} catch (InvocationTargetException ite) {
				Throwable cause = ite.getTargetException();
				if (cause == null) {
					cause = ite.getCause();
				}
				throw new ServletException("Failed to invoke create method "+bean, cause);
			} catch (Exception e) {
				throw new ServletException("Failed to invoke create method" + bean, e);
			}
		}

		try {
			for (Map.Entry<String, Method> s : this.setters.entrySet()) {

				Class<?> t = s.getValue().getParameterTypes()[0];

				Object o = req.getAttribute(s.getKey());
				if (o != null) {
					if (t.isArray() && !o.getClass().isArray()) {
						// convert single value attribute to array if convenient
						Object[] a = (Object[]) Array.newInstance(o.getClass(), 1);
						a[0]=o;
						o = a;
					}
				} else {
					if (t.isArray()) {
						// read appropriately from the parameter set
						o = req.getParameterValues(s.getKey());
					} else {
						o = req.getParameter(s.getKey());
					}
				}

				if (o != null) {
					if (t.isPrimitive()) {
						// normalize to the wrapper type
						t = WRAPPER.get(t);
						if (t==null) {
							throw new IllegalArgumentException("Unsupported type "+s.getValue().getParameterTypes()[0]);
						}
					}
					if (t.isAssignableFrom(o.getClass())) {
						s.getValue().invoke(bean, o);
					} else if (o.getClass()==String.class) {
						String p = ((String) o).trim();
						if (p.length()>0) {
							// support String, Integer, Long, Float, Double, Short, boolean, Date
							try {
								if (t == Integer.class) {
									s.getValue().invoke(bean, Integer.parseInt(p));
								} else if (t == Long.class) {
									s.getValue().invoke(bean, Long.parseLong(p));
								} else if (t == Float.class) {
									s.getValue().invoke(bean, Float.parseFloat(p));
								} else if (t == Double.class) {
									s.getValue().invoke(bean, Double.parseDouble(p));
								} else if (t == Short.class) {
									s.getValue().invoke(bean, Short.parseShort(p));
								} else if (t == Boolean.class) {
									s.getValue().invoke(bean, Boolean.parseBoolean(p));
								} else if (t == Date.class) {
									s.getValue().invoke(bean, ISO8601DateFormat.parse(p));
								}
							} catch (Exception e) {
								if (this.error!=null) {
									this.error.invoke(bean, req, s.getKey(), e);
								} else {
									throw e;
								}
							}
						}
					} else if (o.getClass()==String[].class && t.isArray()) {
						// support Integer[], Long[], Float[], Double[], Short[], Boolean[], Date[]
						try {
							_invokeWithConvertedArray(bean, s.getValue(), (String[]) o, t.getComponentType());
						} catch (Exception e) {
							if (this.error!=null) {
								this.error.invoke(bean, req, s.getKey(), e);
							} else {
								throw e;
							}
						}
					}
				}
			}
		} catch (InvocationTargetException ite) {
			Throwable cause = ite.getTargetException();
			if (cause == null) {
				cause = ite.getCause();
			}
			throw new ServletException("Failed to pass request data to bean " + bean, cause);

		} catch (Exception e) {
			throw new ServletException("Failed to pass request data to bean " + bean, e);
		}
		OutCome o = null;
		// call init if any
		if (this.init!=null) {
			try {
				o = invokeActionMethod(context, req, res, bean, this.init);
			} catch (InvocationTargetException ite) {
				Throwable cause = ite.getTargetException();
				if (cause == null) {
					cause = ite.getCause();
				}
				throw new ServletException("Failed to invoke init method "+bean, cause);
			} catch (Exception e) {
				throw new ServletException("Failed to invoke init method" + bean, e);
			}
		}
		// check for actions
		if (o==null) {
			// no result yet
			for (Map.Entry<String, Method> a : this.actions.entrySet()) {
				if (req.getParameter(a.getKey()) != null  || req.getAttribute(a.getKey())!=null) {
					Method m = a.getValue();
					if (m != null) {
						try {
							o = invokeActionMethod(context, req, res, bean, m);
							if (o != null)
								break;
						} catch (InvocationTargetException ite) {
							Throwable cause = ite.getTargetException();
							if (cause == null) {
								cause = ite.getCause();
							}
							throw new ServletException("Failed to invoke action method " + m, cause);
						} catch (Exception e) {
							throw new ServletException("Failed to invoke action method " + m, e);
						}
					}
				}
			}
		}
		// run byDefault if nothing else matches
		if (o==null && req.getParameter(BY_DEFAULT)==null && req.getAttribute(BY_DEFAULT)==null) {
			Method m = this.actions.get(BY_DEFAULT);
			if (m != null) {
				try {
					o = invokeActionMethod(context, req, res, bean, m);
				} catch (InvocationTargetException ite) {
					Throwable cause = ite.getTargetException();
					if (cause == null) {
						cause = ite.getCause();
					}
					throw new ServletException("Failed to invoke action method " + m, cause);
				} catch (Exception e) {
					throw new ServletException("Failed to invoke action method " + m, e);
				}
			}
		}

		// get all params back out if OutCome is not redirect or done.
		if ( !(o instanceof OutCome.Redirect || o instanceof OutCome.Done || o instanceof OutCome.Error)) {
			try {
				for (Map.Entry<String, Method> s : this.getters.entrySet()) {
					req.setAttribute(s.getKey(), s.getValue().invoke(bean));
				}
			} catch (InvocationTargetException ite) {
				Throwable cause = ite.getTargetException();
				if (cause == null) {
					cause = ite.getCause();
				}
				throw new RuntimeException("Failed to copy action bean state into request for action bean " + bean, cause);

			} catch (Exception e) {
				throw new ServletException("Failed to copy action bean state into request for action bean " + bean, e);
			}
		}
		return o;
	}

	private void _invokeWithConvertedArray(Object o, Method m, String[] inArr, Class<?> t) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		if (t == int.class) {
			int[] arr = new int[inArr.length];
			for (int idx = 0; idx < inArr.length; idx++) {
				arr[idx] = Integer.parseInt(inArr[idx]);
			}
			m.invoke(o, new Object[]{arr});

		} else if (t == Integer.class) {
			Integer[] arr = new Integer[inArr.length];
			for (int idx = 0; idx < inArr.length; idx++) {
				arr[idx] = Integer.parseInt(inArr[idx]);
			}
			m.invoke(o, new Object[]{arr});

		} else if (t == long.class) {
			long[] arr = new long[inArr.length];
			for (int idx = 0; idx < inArr.length; idx++) {
				arr[idx] = Long.parseLong(inArr[idx]);
			}
			m.invoke(o, new Object[]{arr});

		} else  if (t == Long.class) {
			Long[] arr = new Long[inArr.length];
			for (int idx = 0; idx < inArr.length; idx++) {
				arr[idx] = Long.parseLong(inArr[idx]);
			}
			m.invoke(o, new Object[]{arr});

		} else  if (t == float.class) {
			float[] arr = new float[inArr.length];
			for (int idx = 0; idx < inArr.length; idx++) {
				arr[idx] = Float.parseFloat(inArr[idx]);
			}
			m.invoke(o, new Object[]{arr});

		} else  if (t == Float.class) {
			Float[] arr = new Float[inArr.length];
			for (int idx = 0; idx < inArr.length; idx++) {
				arr[idx] = Float.parseFloat(inArr[idx]);
			}
			m.invoke(o, new Object[]{arr});

		} else  if (t == double.class) {
			double[] arr = new double[inArr.length];
			for (int idx = 0; idx < inArr.length; idx++) {
				arr[idx] = Double.parseDouble(inArr[idx]);
			}
			m.invoke(o, new Object[]{arr});

		} else  if (t == Double.class) {
			Double[] arr = new Double[inArr.length];
			for (int idx = 0; idx < inArr.length; idx++) {
				arr[idx] = Double.parseDouble(inArr[idx]);
			}
			m.invoke(o, new Object[]{arr});

		} else  if (t == boolean.class) {
			boolean[] arr = new boolean[inArr.length];
			for (int idx = 0; idx < inArr.length; idx++) {
				arr[idx] = Boolean.parseBoolean(inArr[idx]);
			}
			m.invoke(o, new Object[]{arr});

		} else  if (t == Boolean.class) {
			Boolean[] arr = new Boolean[inArr.length];
			for (int idx = 0; idx < inArr.length; idx++) {
				arr[idx] = Boolean.parseBoolean(inArr[idx]);
			}
			m.invoke(o, new Object[]{arr});

		} else  if (t == Date.class) {
			Date[] arr = new Date[inArr.length];
			for (int idx = 0; idx < inArr.length; idx++) {
				arr[idx] = ISO8601DateFormat.parse(inArr[idx]);
			}
			m.invoke(o, new Object[]{arr});
		}
	}

	private OutCome invokeActionMethod(ServletContext context, HttpServletRequest req, HttpServletResponse res, Object bean, Method m)
			throws IllegalAccessException, InvocationTargetException {
		OutCome o;
		if (m.getParameterTypes().length == 0) {
			o = (OutCome) m.invoke(bean);
		} else
		if (m.getParameterTypes().length == 1) {
			o = (OutCome) m.invoke(bean, req);
		} else
		if (m.getParameterTypes().length == 2) {
			if (ServletContext.class.isAssignableFrom(m.getParameterTypes()[0])) {
				o = (OutCome) m.invoke(bean, context,req);
			} else {
				o = (OutCome) m.invoke(bean, req,res);
			}
		} else
		if (m.getParameterTypes().length == 3) {
			o = (OutCome) m.invoke(bean, context, req,res);
		} else {
			o = (OutCome) m.invoke(bean);
		}
		return o;
	}

}
