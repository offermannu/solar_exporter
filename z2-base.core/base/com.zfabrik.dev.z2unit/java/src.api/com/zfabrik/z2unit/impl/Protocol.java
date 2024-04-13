package com.zfabrik.z2unit.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

import com.zfabrik.z2unit.RemoteErrorException;

/**
 * Protocol abstraction for z2 unit. Essentially a wrapper around Java (de-) serialization tasks.
 */
public class Protocol {
	private final static Logger LOG = Logger.getLogger(Protocol.class.getName());
	

	public static final String EVENT_TEST_ASSUMPTION_FAILURE = "testAssumptionFailure";
	public static final String EVENT_TEST_FAILURE = "testFailure";
	public static final String EVENT_TEST_FINISHED = "testFinished";
	public static final String EVENT_TEST_IGNORED = "testIgnored";
	public static final String EVENT_TEST_RUN_FINISHED = "testRunFinished";
	public static final String EVENT_TEST_RUN_STARTED = "testRunStarted";
	public static final String EVENT_TEST_STARTED = "testStarted";

	/**
	 * Write a JUnit description to a writer. This is sent from the runner
	 */
	public static void serialize(Description description, Writer writer) throws IOException {
		writer.write(toBase64Chunk(toBytes(purify(description))));
	}

	/**
	 * Write a JUnit result to a writer. This is sent back from the server.
	 */
	public static void serialize(Result result, Writer writer) throws IOException {
		// note that we wrap the result and serialize piecewise, as some content
		// may not be serializable, in particular exceptions.
		SResult res = new SResult(result);
		writer.write(toBase64Chunk(toBytes(res)));
	}

	/**
	 * Write a JUnit event to a writer. This is sent back from the server.
	 */
	public static void serialize(String type, Description description, Writer writer) throws IOException {
		writer.write(toBase64Chunk(toBytes(new SEvent(type,description))));
	}
	/**
	 * Write a JUnit event to a writer. This is sent back from the server.
	 */
	public static void serialize(String type, Failure failure, Writer writer) throws IOException {
		writer.write(toBase64Chunk(toBytes(new SEvent(type,new SFailure(failure)))));
	}
	/**
	 * Write a JUnit event to a writer. This is sent back from the server.
	 */
	public static void serialize(String type, Result result, Writer writer) throws IOException {
		writer.write(toBase64Chunk(toBytes(new SEvent(type,new SResult(result)))));
	}
	
	/**
	 * Read a JUnit description from a reader - used on server
	 */
	public static Description deSerializeDescription(Reader reader) throws IOException {
		return fromBytes(fromBase64Chunk(reader),Description.class);
	}

	/**
	 * Read a JUnit event from a reader - used in runner
	 */
	public static SEvent deSerializeEvent(Reader reader) throws IOException {
		return fromBytes(fromBase64Chunk(reader),SEvent.class);
	}

	
	//
	// Serialized result. Almost like a JUnit result, except that we use serialization safe failures.  
	//
	public static class SResult implements Serializable {
		private static final long serialVersionUID = 1L;
		private int failureCount, ignoreCount, runCount;
		private long runTime;
		private List<SFailure> failures=new ArrayList<>();
		
		public SResult(Result result) throws IOException {
			this.failureCount = result.getFailureCount();
			for (Failure f : result.getFailures()) {
				this.failures.add(new SFailure(f));
			}
			this.ignoreCount = result.getIgnoreCount();
			this.runCount = result.getRunCount();
			this.runTime = result.getRunTime();
		}

		public int getFailureCount() {
			return failureCount;
		}

		public int getIgnoreCount() {
			return ignoreCount;
		}

		public int getRunCount() {
			return runCount;
		}

		public long getRunTime() {
			return runTime;
		}

		public List<SFailure> getFailures() {
			return failures;
		}

		public Result toResult() {
			final List<Failure> failures = this.failures.stream().map(SFailure::toFailure).collect(Collectors.toList());
			
			Result r = new Result() {
				private static final long serialVersionUID = 1L;
				@Override
				public int getFailureCount() {
					return getFailureCount();
				}			
				@Override
				public int getIgnoreCount() {
					return getIgnoreCount();
				}
				@Override
				public List<Failure> getFailures() {
					return failures;
				}
				@Override
				public int getRunCount() {
					return getRunCount();
				}
				@Override
				public long getRunTime() {
					return getRunTime();
				}
			};
			return r;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + failureCount;
			result = prime * result + ((failures == null) ? 0 : failures.hashCode());
			result = prime * result + ignoreCount;
			result = prime * result + runCount;
			result = prime * result + (int) (runTime ^ (runTime >>> 32));
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			SResult other = (SResult) obj;
			if (failureCount != other.failureCount)
				return false;
			if (failures == null) {
				if (other.failures != null)
					return false;
			} else if (!failures.equals(other.failures))
				return false;
			if (ignoreCount != other.ignoreCount)
				return false;
			if (runCount != other.runCount)
				return false;
			if (runTime != other.runTime)
				return false;
			return true;
		}
	}
	
	//
	// Serialized failure, almost like JUnit failure, except that we can handle serialization problems more safely
	public static class SFailure implements Serializable {
		private static final long serialVersionUID = 1L;
		private byte[] exception;
		private String exceptionMessage;
		private Description description;
		
		public SFailure(Failure failure) throws IOException {
			try {
				// try serialize
				this.exception = toBytes(failure.getException());
			} catch (Exception nse) {
				// that's too bad. Ignore it
				LOG.log(Level.WARNING,"Failed to serialize instance of "+failure.getException().getClass().getName() +". Falling back to stack trace copy",nse);
			}
			this.exceptionMessage=getStackTrace(failure.getException());
			this.description=purify(failure.getDescription());
		}
		
		public Throwable getException() {
			if (exception!=null) {
				try {
					return fromBytes(exception, Throwable.class);
				} catch (Exception e) {
					LOG.log(Level.WARNING,"Failed to deserialize remotely serialized failure throwable.",e);
				}
			}
			return null;
		}

		public String getExceptionMessage() {
			return exceptionMessage;
		}

		public Description getDescription() {
			return description;
		}
		
		// turn into JUnit failure
		public Failure toFailure() {
			Throwable t=getException();
			if (t==null) {
				// the failure was not serializable server-side already. We just go with the message
				t = new RemoteErrorException(getExceptionMessage());
			}
			Failure f = new Failure(this.description, t);
			return f;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((description == null) ? 0 : description.hashCode());
			result = prime * result + Arrays.hashCode(exception);
			result = prime * result + ((exceptionMessage == null) ? 0 : exceptionMessage.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			SFailure other = (SFailure) obj;
			if (description == null) {
				if (other.description != null)
					return false;
			} else if (!description.equals(other.description))
				return false;
			if (!Arrays.equals(exception, other.exception))
				return false;
			if (exceptionMessage == null) {
				if (other.exceptionMessage != null)
					return false;
			} else if (!exceptionMessage.equals(other.exceptionMessage))
				return false;
			return true;
		}
		
		
	}
	
	//
	// Serialized event 
	//
	public static class SEvent implements Serializable {
		private static final long serialVersionUID = 1L;
		private String type;
		private Description description;
		private SFailure failure;
		private SResult result;
		
		public SEvent(String type, Description description) {
			super();
			this.type = type;
			this.description = description;
		}
		
		public SEvent(String type, SFailure failure) {
			super();
			this.type = type;
			this.failure = failure;
		}
		
		public SEvent(String type, SResult result) {
			super();
			this.type = type;
			this.result = result;
		}

		public String getType() {
			return type;
		}

		public Description getDescription() {
			return description;
		}

		public SFailure getFailure() {
			return failure;
		}

		public SResult getResult() {
			return result;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((description == null) ? 0 : description.hashCode());
			result = prime * result + ((failure == null) ? 0 : failure.hashCode());
			result = prime * result + ((this.result == null) ? 0 : this.result.hashCode());
			result = prime * result + ((type == null) ? 0 : type.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			SEvent other = (SEvent) obj;
			if (description == null) {
				if (other.description != null)
					return false;
			} else if (!description.equals(other.description))
				return false;
			if (failure == null) {
				if (other.failure != null)
					return false;
			} else if (!failure.equals(other.failure))
				return false;
			if (result == null) {
				if (other.result != null)
					return false;
			} else if (!result.equals(other.result))
				return false;
			if (type == null) {
				if (other.type != null)
					return false;
			} else if (!type.equals(other.type))
				return false;
			return true;
		}
		
	}
	
	
	//
	// Unfortunately a runner provided description is not necessarily cleanly serializable or de-serializable.
	// We do not need the full description anyway to run the tests. Only sorting, naming, filtering is really important.
	// That is why we clean out stuff that may be harming (at least with Spock, annotations for example were problematic)
	// 
	private static Description purify(Description description) {
		for (Description c : description.getChildren()) {
			purify(c);
		}
		try {
			Field f = Description.class.getDeclaredField("fAnnotations");
			f.setAccessible(true);
			Annotation[] anns = (Annotation[]) f.get(description);
			if (anns!=null && anns.length>0) {
				f.set(description, new Annotation[0]);
			}
		} catch (NoSuchFieldException e) {
			// it's ok. Ignore this.
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return description;
	}
	
	// serialize
	private static byte[] toBytes(Serializable object) throws IOException {
        try (ByteArrayOutputStream ba = new ByteArrayOutputStream()) {
            try (ObjectOutputStream oo = new ObjectOutputStream(ba)) {
                oo.writeObject(object);
            }
            return ba.toByteArray();
        }
    }

	// de-serialize
	private static <T> T fromBytes(byte[] bytes, Class<T> clz) throws IOException {
		if (bytes==null) {
			// EOF!
			return null;
		}
    	ClassLoader cl = Thread.currentThread().getContextClassLoader()!=null? 
				Thread.currentThread().getContextClassLoader():
				Protocol.class.getClassLoader();
        try {
    		try (ByteArrayInputStream bi = new ByteArrayInputStream(bytes)) {
                try (ObjectInputStream oi = new ObjectInputStream(bi) {
                    protected java.lang.Class<?> resolveClass(java.io.ObjectStreamClass desc) throws IOException ,ClassNotFoundException {
                        return Class.forName(desc.getName(),false,cl);
                    };
                }) {
                    return clz.cast(oi.readObject());
                }
            }
        } catch (ClassNotFoundException e) {
			throw new IllegalStateException(e);
        }
	}

	// convert to base 64 string prepended with a length number followed by a " ".
	public static String toBase64Chunk(byte[] bytes) throws IOException {
		String payload = Base64.getEncoder().encodeToString(bytes);
		return payload.length()+" "+payload;
	}

	// convert from base 64 string prepended with a length number followed by a " ".
	public static byte[] fromBase64Chunk(Reader reader) throws IOException {
		// advance to blank
		StringBuilder sb=new StringBuilder();
		int c;
		while ((c=reader.read())>=0 && ((char) c)!=' ') {
			sb.append((char)c);
		}
		if (c<0) {
			if (sb.length()==0) {
				// EOF!
				return null;
			}
			// hit end of stream
			throw new IllegalStateException("Ran into end of stream while parsing for chunk header separator");
		}
		int length = Integer.parseInt(sb.toString());
		// read as many
		sb.setLength(0);
		char[] buf = new char[1024];
		int l;
		while (length>0 && (l=reader.read(buf,0,Math.min(length,buf.length)))>=0) {
			length-=l;
			sb.append(buf,0,l);
		}
		// decode
		return Base64.getDecoder().decode(sb.toString());
	}
	
	// in case we fail to serialize or to de-serialize, we can still show some 
	// text information on a failure. We use the stacktrace
	private static String getStackTrace(Throwable t) throws IOException {
		try (StringWriter w = new StringWriter()) {
			try (PrintWriter pw = new PrintWriter(w)) {
				t.printStackTrace(pw);
			}
			return w.toString();
		}
	}

}
