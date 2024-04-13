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

import com.zfabrik.components.IComponentsLookup;
import com.zfabrik.components.IComponentsManager;
import com.zfabrik.components.java.IJavaComponent;
import com.zfabrik.components.java.JavaComponentUtil;
import com.zfabrik.util.runtime.Foundation;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ApiImplJarsResolver extends HttpServlet {

	private static final long serialVersionUID = 8722135449331567958L;

    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
        new Resolver() {{
            setProjectName(request.getParameter("projectName"));
            setDebug(request.getParameter("debug"));
            setOutDir(StringUtils.trimToNull(request.getParameter("dir")));
        }}.resolve(request, response);
    }

    private static class Resolver {
        private List<String> projectNames;
        private boolean debug=false;
        private File outDir;
        private Set<String> inuse = new HashSet<>();

        public void setProjectName(String projectName) {
            this.projectNames = splitString(projectName);
        }

        public void setDebug(String debug) {
            this.debug = (debug != null);
        }

        public void setOutDir(String outDir) {
            if (outDir != null) {
                this.outDir = new File(outDir);
                this.outDir.mkdirs();
            }
        }

        protected void resolve(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {

            ZipOutputStream z_out = null;
            PrintWriter w_out = null;
            if (! this.debug && this.outDir == null) {
                response.setContentType("application/zip");
                z_out = new ZipOutputStream(response.getOutputStream());

                ZipEntry ze = new ZipEntry("info.txt");
                z_out.putNextEntry(ze);
                String info = "projectNames: "+projectNames;
                z_out.write(info.getBytes(), 0, info.getBytes().length);

            } else {
                response.setContentType("text/plain");
                w_out = response.getWriter();
            }
            try {
                if (this.projectNames != null && this.projectNames.size() > 0) {
                    try {
                        for (String project : this.projectNames) {

                            project = project.trim();
                            String jpro = JavaComponentUtil.fixJavaComponentName(project);

                            File javaf = null;
                            // make sure it's built and we get access to the actual
                            // runtime resources
                            try {
                                IJavaComponent jc = IComponentsLookup.INSTANCE.lookup(jpro, IJavaComponent.class);
                                if (jc!=null) {
                                	javaf = jc.getRuntimeResources();
                                }
                            } catch (Exception e) {
                                logger.log(Level.WARNING,"Cannot provide api jar of "+jpro+" due to: "+e.toString(),e);
                                // ok, we can't run the build. But at least we can provide
                                // the other jars.
                            }

                            if (javaf==null) {
                            	// fall back to original resources
                            	javaf = IComponentsManager.INSTANCE.retrieve(jpro);
                            }

                            // for backward compatibility also use bin/lib [to be deprecated]
                            File[] libFiles = {new File(javaf, "bin/lib"), new File(javaf,"bin.api/lib")};

                            for (File libf : libFiles) {
                                if (libf.exists()) {
                                    File[] jarfiles = libf.listFiles(new FileFilter() {
                                        public boolean accept(File pathname) {
                                            return pathname.getName().toLowerCase().endsWith(".jar");
                                        }
                                    });

                                    if (jarfiles!=null && jarfiles.length > 0) {
                                        writeFiles(z_out, w_out, project, null, jarfiles);
                                    }
                                }
                            }

                            // add impl-jars
                            File[] implLibFiles = {new File(javaf,"bin.impl/lib")};

                            for (File libf : implLibFiles) {
                                if (libf.exists()) {
                                    File[] jarfiles = libf.listFiles(new FileFilter() {
                                        public boolean accept(File pathname) {
                                            return pathname.getName().toLowerCase().endsWith(".jar");
                                        }
                                    });

                                    if (jarfiles!=null && jarfiles.length > 0) {
                                        writeFiles(z_out, w_out, project + "/impl", null, jarfiles);
                                    }
                                }
                            }

                            // add z.properties
                            File zDotProperties = new File(javaf, "z.properties");
                            if (zDotProperties.isFile()) {
                                writeFiles(z_out, w_out, project, null, zDotProperties);
                            }

                        }

                        // add z.jar
                        File f_zjar = new File(Foundation.getProperties().getProperty(Foundation.HOME), "bin/z.jar");
                        writeFiles(z_out, w_out, "com.zfabrik.core/java/impl", null, f_zjar);

                    } catch (Exception e)  {
                        logger.log(Level.SEVERE, "Failed to deliver Libs for " + this.projectNames, e);

                        response.sendError(
                                HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                                "Failed to deliver Libs for " + this.projectNames);

                    }
                }
            } finally {
                if (z_out != null) {
                    z_out.close();

                } else if (outDir != null) {
                    logger.fine("in-use " + inuse);

                    // delete obsolete files and empty dirs
                    Files.walkFileTree(outDir.toPath(), new SimpleFileVisitor<>() {
                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                            return super.visitFile(file, attrs);
                        }

                        @Override
                        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                            if (Files.list(dir).findAny().isEmpty()) {
                                logger.fine("deleting empty dir " + dir);
                                Files.delete(dir);
                            }
                            return super.postVisitDirectory(dir, exc);
                        }
                    });
                }
            }
        }

        private void writeFiles(ZipOutputStream z_out, PrintWriter w_out, String prefix, File rootPath, File... files) throws FileNotFoundException, IOException {

            if (debug) {
                debugFiles(w_out, prefix, rootPath, files);

            } else if (outDir != null) {
                writeFiles(w_out, prefix, rootPath, files);

            } else {

                byte[] buf = new byte[4096];

                for (File f : files) {
                    try (BufferedInputStream fin = new BufferedInputStream(new FileInputStream(f))) {
                        int l;
                        ZipEntry ze = new ZipEntry(prefix + getRelPath(rootPath, f));
                        z_out.putNextEntry(ze);
                        while ((l = fin.read(buf)) >= 0) {
                            z_out.write(buf, 0, l);
                        }
                    }
                }
                z_out.flush();
            }
        }

        private void writeFiles(PrintWriter wOut, String prefix, File rootPath, File... files) throws IOException {

            for (File srcFile : files) {

                String path = prefix + getRelPath(rootPath, srcFile);
                File targetFile = new File(outDir, path);
                inuse.add(path);

                if (! FileUtils.contentEquals(srcFile, targetFile)) {
                    wOut.println("Copy " + path);
                    targetFile.getParentFile().mkdirs();
                    Files.copy(srcFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } else {
                    wOut.println("Skip " + path);
                }
            }
        }

        private void debugFiles(PrintWriter wOut, String prefix, File rootPath, File... files) throws IOException {
            for (File srcFile : files) {
                String path = prefix + getRelPath(rootPath, srcFile);
                wOut.println(path);
            }
        }

        private String getRelPath(File root, File f) {
            if (root == null) {
                return "/" + f.getName();
            }

            String rootPath = root.getAbsolutePath();
            String absPath = f.getAbsolutePath();

            if (absPath.startsWith(rootPath)) {
                return absPath.substring(rootPath.length());
            } else {
                return absPath;
            }
        }

        private List<String> splitString(String s) {

            if (s == null) return Collections.emptyList();

            List<String> result = new ArrayList<>();
            StringTokenizer loop = new StringTokenizer(s, "{([ \t,])}");
            while (loop.hasMoreTokens()) {
                result.add(loop.nextToken());
            }

            return result;
        }

    }

	private final static Logger logger = Logger.getLogger(ApiImplJarsResolver.class.getName());
}
