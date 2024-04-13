/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.impl.hubcr.web;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.zfabrik.hubcr.RemoteComponentRepositoryDB;
import com.zfabrik.impl.hubcr.store.HubCRResource;

public class RemoteRepositoryServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
		
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		
		// first pathInfo segment is the command
		String cmd = req.getPathInfo();
		while (cmd.startsWith("/")) cmd=cmd.substring(1);
		int p = cmd.indexOf('/');
		if (p>0) {
			cmd = cmd.substring(0,p);
		}

		try {
			if ("download".equals(cmd)) {
				doDownload(req,resp);
			} else
			if ("getDB".equals(cmd)) {
				doGetDB(req,resp);
			} else {
				resp.sendError(HttpServletResponse.SC_BAD_REQUEST,"Unknown command "+cmd);
			}
		} catch (Exception e) {
			resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,e.toString());
		}
	}

	/**
	 * Send a db over the wire. If none found yet, return NOT_FOUND. If not modified, return NOT_MODIFIED. Otherwise stream
	 * the DB.
	 * 
	 * @param req
	 * @param resp
	 * @throws Exception
	 */
	private void doGetDB(HttpServletRequest req, HttpServletResponse resp) throws Exception {
		String srev = req.getParameter("rev");
		Long rev = null;
		if (srev!=null) {
			try {
				rev = Long.parseLong(srev.trim()); 
			} catch (Exception e) {
				resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Bad revision "+srev+" requested");
				return;
			}
		}
		
		RemoteComponentRepositoryDB db = HubCRResource.getManager().getDB();
		if (db==null) {
			resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Remote repository not initialized");
		} else {
			if (rev!=null && rev==db.getRevision()) {
				// no change!
				resp.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
			} else {
				// serialize to the db
				ObjectOutputStream oOut = new ObjectOutputStream(resp.getOutputStream());
				try {
					oOut.writeObject(db);
				} finally {
					oOut.close();
				}
			}
		}
	}

	/**
	 * Send a complete component's resources as ZIP file.  
	 * @param req
	 * @param resp
	 * @throws IOException
	 */
	private void doDownload(HttpServletRequest req, HttpServletResponse resp)  throws Exception {
		String srev = req.getParameter("rev");
		long rev = 0;
		if (srev!=null && ((srev=srev.trim()).length()>0)) {
			try {
				rev = Long.parseLong(srev.trim()); 
			} catch (Exception e) {
				resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Bad revision "+srev+" requested");
				return;
			}
		} else {
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing required rev parameter");
			return;
		}
		
		String componentName = req.getParameter("componentName");
		if (componentName==null || ((componentName=componentName.trim()).length()==0)) {
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing required componentName parameter");
			return;
		}
		
		File folder = HubCRResource.getManager().getComponentFolder(componentName, rev);
		
		if (folder==null || !folder.exists()) {
			resp.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		
		resp.setContentType("application/zip");
		
		ZipOutputStream zOut = new ZipOutputStream(resp.getOutputStream());
		try {
			_zipIt(folder, null, zOut);
		} finally {
			zOut.close();
		}
		
	}

	private static void _zipIt(File base, String current, ZipOutputStream zo) throws Exception {
		if (current==null) {
			current="";
		}
		if (base.isDirectory()) {
			if (current.length()>0) {
				_addFolderToZIP(base, current, zo);
			}
			String path;
			for (File f : base.listFiles()) {	
				path = current + "/" + f.getName();
				_zipIt(f, path, zo);
			}

		} else {
			_addFileToZIP(base, current, zo);
		}
	}

	private static void _addFolderToZIP(File f, String path, ZipOutputStream zo) throws IOException {
		path = path + "/"; // append to make it appear as a folder
		ZipEntry ze = new ZipEntry(path);
		if (f != null)
			ze.setTime(f.lastModified());
		ze.setSize(0);
		zo.putNextEntry(ze);
		zo.closeEntry();
	}

	private static void _addFileToZIP(File f, String path, ZipOutputStream zo) throws IOException {
		ZipEntry ze = new ZipEntry(path);
		ze.setTime(f.lastModified());
		int len = (int) f.length();
		byte[] buffer = new byte[len];
		// read that file completely
		InputStream fin = new FileInputStream(f);
		int l, s = 0;
		while ((l = fin.read(buffer, s, len - s)) > 0)
			s += l;
		fin.close();
		zo.putNextEntry(ze);
		zo.write(buffer);
		zo.closeEntry();
	}

}
