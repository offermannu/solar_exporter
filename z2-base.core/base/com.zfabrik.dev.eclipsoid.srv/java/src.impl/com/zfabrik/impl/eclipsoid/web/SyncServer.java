/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.impl.eclipsoid.web;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.ObjectName;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.zfabrik.components.IComponentDescriptor;
import com.zfabrik.components.IComponentsManager;
import com.zfabrik.management.home.IHomeJMXClient;
import com.zfabrik.management.home.IHomeMBeanServer;
import com.zfabrik.util.expression.X;

public class SyncServer extends HttpServlet {

    private static final long serialVersionUID = 2212501839578863530L;

    private final static Logger logger = Logger.getLogger(SyncServer.class.getName());

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {

            Map<String, Long> oldRevisions = computeRevisions(new HashMap<String, Long>());

            IHomeJMXClient hcl = IHomeJMXClient.INSTANCE;
            IHomeMBeanServer mbs = hcl.getRemoteMBeanServer(null);
            ObjectName on = ObjectName.getInstance("zfabrik:type=launchers.HomeLauncher");
            mbs.invoke(on, "synchronize", null, null);

            Map<String, Long> chgRevisions = compaction(computeRevisions(oldRevisions));

            response.setStatus(HttpServletResponse.SC_OK);
            for (Map.Entry<String, Long> e : chgRevisions.entrySet()) {
                response.getWriter().println(propEsc(e.getKey()) + ": " + e.getValue());
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to sync Server", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to sync Server");
        }
    }

    private String propEsc(String key) {
        return key.replace(":", "\\:");
    }

    /*
        Compact component revisions to module revisions by using the highest revision number
     */
    private Map<String,Long> compaction(Map<String,Long> revisionsMap) {
        Map<String, Long> result = new HashMap<String, Long>();
        for (Map.Entry<String, Long> e : revisionsMap.entrySet()) {

            String moduleName = module(e.getKey());
            Long   moduleRev  = e.getValue();

            Long hasRevision = result.get(moduleName);
            if (hasRevision == null || moduleRev > hasRevision) {
                result.put(moduleName, moduleRev);
            }
        }
        return result;
    }

    private Map<String, Long> computeRevisions(Map<String, Long> revisionsMap) throws IOException {

        Collection<String> allComps = IComponentsManager.INSTANCE.findComponents(X.val(true));
        for (String component : allComps) {

            IComponentDescriptor cDesc = IComponentsManager.INSTANCE.getComponent(component);

            Long curRevision = revisionsMap.get(component);
            Long newRevision = cDesc.getRevision();

            if (equal(curRevision, newRevision)) {
                revisionsMap.remove(component);

            } else {
                revisionsMap.put(component, newRevision);
            }
        }
        return revisionsMap;
    }

    private boolean equal(Long a, Long b) {
        if (a == null)
            return b == null;
        else
            return a.equals(b);

    }

    public static String module(String cn) {
        int p = cn.lastIndexOf('/');
        return p<0? cn : cn.substring(0,p);
    }

}