/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.dev.z2jupiter.internal.transfer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.TestExecutionResult.Status;

/**
 * Test result as conveyed via {@link Z2JupiterListenerEventDto}
 */
public class Z2JupiterTestResultDto {
	private final static Logger LOG = Logger.getLogger(Z2JupiterTestResultDto.class.getName());

	private Status status;
	private byte[] causeBytes;
	private String causeString;

	public Z2JupiterTestResultDto(TestExecutionResult result) {
		status = result.getStatus();
		setCause(result.getThrowable());
	}

	public Z2JupiterTestResultDto() {
	}

	public Status getStatus() {
		return status;
	}

	public byte[] getCauseBytes() {
		return causeBytes;
	}

	public void setCauseBytes(byte[] causeBytes) {
		this.causeBytes = causeBytes;
	}

	public String getCauseString() {
		return causeString;
	}

	public void setCauseString(String causeString) {
		this.causeString = causeString;
	}

	public void setCause(Optional<Throwable> cause) {
		Throwable t = cause.orElse(null);
		if (t!=null) {
			StringWriter w = new StringWriter();
			try (PrintWriter s = new PrintWriter(w);) {t.printStackTrace(s);}
			this.causeString = w.toString();
			try {
				ByteArrayOutputStream b = new ByteArrayOutputStream();
				try (ObjectOutputStream out = new ObjectOutputStream(b)) { out.writeObject(t); }
				this.causeBytes = b.toByteArray();
			} catch (IOException e) {
				LOG.log(Level.WARNING,"Failed to serialize throwable of type "+t.getClass().getName(),e);
			}
		} else {
			this.causeBytes = null;
			this.causeString=null;
		}
	}
	
	private Throwable buildCause() {
		if (this.causeString!=null) {
			Throwable t;
			if (this.causeBytes!=null) {
				// try de-serialize
				try {
					try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(this.causeBytes))) {
						t = (Throwable) in.readObject();
					}
				} catch (Exception e) {
					// failed
					t = new IllegalStateException("Test Failed with non-deserializable cause (included)\n"+this.causeString);
				}
			} else {
				t = new IllegalStateException("Test Failed with non-serializable cause (included)\n"+this.causeString);
			}
			return t;
		}
		return null;
	}

	
	public TestExecutionResult toTestExecutionResult() {
		switch (this.status) {
		case SUCCESSFUL:
			return TestExecutionResult.successful();
		case ABORTED:
			return TestExecutionResult.aborted(buildCause());
		case FAILED:
			return TestExecutionResult.failed(buildCause());
		default:
			throw new IllegalStateException(this.status.name());
		}
	}

}
