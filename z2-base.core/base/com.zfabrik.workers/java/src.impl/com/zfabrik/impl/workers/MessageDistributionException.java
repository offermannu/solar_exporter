/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.impl.workers;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;

public class MessageDistributionException extends IOException {
	private Map<String,Throwable> causes;
	
	private static final long serialVersionUID = 7670789922635108205L;

	public MessageDistributionException(String s)   {
        super(s);
    }

    public MessageDistributionException(String s,Throwable t)   {
        super(s);
        super.initCause(t);
    }

    public MessageDistributionException(String s,Map<String,Throwable> causes)   {
        super(s);
        this.causes=causes;
    }
    
    public Map<String,Throwable> getCausesByNode() {
    	return this.causes;
    }
    
    public String toString() {
    	StringBuilder b = new StringBuilder(super.toString());
    	if ((this.causes!=null) && (this.causes.size()>0)) {
        	b.append("- Aggregated causes:\n");
        	for (Map.Entry<String,Throwable> e:this.causes.entrySet()) {
        		b.append("Reported during communication with  "+e.getKey()+":\n");
        		StringWriter w = new StringWriter();
        		e.getValue().printStackTrace(new PrintWriter(w));
        		b.append(w.toString());
        		b.append("\n");
        	}
    	}
    	return b.toString();
    }  
    
}
