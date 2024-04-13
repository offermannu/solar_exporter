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

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.discovery.ClassSelector;
import org.junit.platform.engine.discovery.ClasspathResourceSelector;
import org.junit.platform.engine.discovery.ClasspathRootSelector;
import org.junit.platform.engine.discovery.DirectorySelector;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.engine.discovery.FilePosition;
import org.junit.platform.engine.discovery.FileSelector;
import org.junit.platform.engine.discovery.MethodSelector;
import org.junit.platform.engine.discovery.ModuleSelector;
import org.junit.platform.engine.discovery.NestedClassSelector;
import org.junit.platform.engine.discovery.NestedMethodSelector;
import org.junit.platform.engine.discovery.PackageSelector;
import org.junit.platform.engine.discovery.UniqueIdSelector;
import org.junit.platform.engine.discovery.UriSelector;

/**
 * Serializable selector
 */
public class Z2JupiterSelectorDto {

	public static enum SelectorType {
		ClasspathResourceSelector, ClasspathRootSelector, ClassSelector, DirectorySelector, FileSelector,
		MethodSelector, ModuleSelector, NestedClassSelector, NestedMethodSelector, PackageSelector, UniqueIdSelector, UriSelector
	}

	private SelectorType type;
	private String classpathResourceName;
	private Integer line;
	private Integer column;
	private String  uri;
	private String className;
	private String nestedClassName;
	private String file;
	private String methodName;
	private String methodParameterTypes;
	private String moduleName;
	private List<String> enclosingClassNames;
	private String packageName;
	private String uniqueId;
	
	public Z2JupiterSelectorDto() {
	}

	public Z2JupiterSelectorDto(DiscoverySelector selector) {
		if (selector instanceof ClasspathResourceSelector) {
			this.type = SelectorType.ClasspathResourceSelector;
			ClasspathResourceSelector sel = (ClasspathResourceSelector) selector;
			this.classpathResourceName = sel.getClasspathResourceName();
			this.position(sel.getPosition());
		} else
		if (selector instanceof ClasspathRootSelector) {
			this.type = SelectorType.ClasspathRootSelector;
			ClasspathRootSelector sel = (ClasspathRootSelector) selector;
			this.uri = sel.getClasspathRoot().toString();
		} else
		if (selector instanceof ClassSelector) {
			this.type = SelectorType.ClassSelector;
			ClassSelector sel = (ClassSelector) selector;
			this.className = sel.getClassName();
		} else
		if (selector instanceof DirectorySelector) {
			this.type = SelectorType.DirectorySelector;
			DirectorySelector sel = (DirectorySelector) selector;
			file = sel.getDirectory().toString();
		} else
		if (selector instanceof FileSelector) {
			this.type = SelectorType.FileSelector;
			FileSelector sel = (FileSelector) selector;
			this.file = sel.getRawPath();
			this.position(sel.getPosition());
		} else
		if (selector instanceof MethodSelector) {
			this.type = SelectorType.MethodSelector;
			MethodSelector sel = (MethodSelector) selector;
			this.className=sel.getClassName();
			this.methodName=sel.getMethodName();
			this.methodParameterTypes=sel.getMethodParameterTypes();
		} else
		if (selector instanceof ModuleSelector) {
			this.type = SelectorType.ModuleSelector;
			ModuleSelector sel = (ModuleSelector) selector;
			this.moduleName = sel.getModuleName();
		} else
		if (selector instanceof NestedClassSelector) {
			this.type = SelectorType.NestedClassSelector;
			NestedClassSelector sel = (NestedClassSelector) selector;
			this.enclosingClassNames = sel.getEnclosingClassNames();
			this.nestedClassName = sel.getNestedClassName();
		} else
		if (selector instanceof NestedMethodSelector) {
			this.type = SelectorType.NestedMethodSelector;
			NestedMethodSelector sel = (NestedMethodSelector) selector;
			this.enclosingClassNames=sel.getEnclosingClassNames();
			this.methodName=sel.getMethodName();
			this.methodParameterTypes=sel.getMethodParameterTypes();
			this.nestedClassName=sel.getNestedClassName();
		} else
		if (selector instanceof PackageSelector) {
			this.type = SelectorType.PackageSelector;
			PackageSelector sel = (PackageSelector) selector;
			this.packageName = sel.getPackageName();
		} else
		if (selector instanceof UniqueIdSelector) {
			this.type = SelectorType.UniqueIdSelector;
			UniqueIdSelector sel = (UniqueIdSelector) selector;
			this.uniqueId = sel.getUniqueId().toString();
		} else
		if (selector instanceof UriSelector) {
			this.type = SelectorType.UriSelector;
			UriSelector sel = (UriSelector) selector;
			this.uri = sel.getUri().toString();
		} else {
			throw new IllegalArgumentException("Unsupported selector type "+selector.getClass().getName());
		}
	}

	public DiscoverySelector toSelector() {
		switch (this.type) {
		case ClasspathResourceSelector:
			return DiscoverySelectors.selectClasspathResource(this.classpathResourceName, position());
		case ClasspathRootSelector:
			try {
				return DiscoverySelectors.selectClasspathRoots(Collections.singleton(Paths.get(new URI(this.uri)))).stream().findAny().orElse(null);
			} catch (URISyntaxException e) {
				throw new IllegalArgumentException(e);
			}
		case ClassSelector:
			return DiscoverySelectors.selectClass(this.className);
		case DirectorySelector:
			return DiscoverySelectors.selectDirectory(this.file);
		case FileSelector:
			return DiscoverySelectors.selectFile(this.file,position());
		case MethodSelector:
			return DiscoverySelectors.selectMethod(this.className,  this.methodName,  this.methodParameterTypes);
		case ModuleSelector:
			return DiscoverySelectors.selectModule(this.moduleName);
		case NestedClassSelector:
			return DiscoverySelectors.selectNestedClass(this.enclosingClassNames, this.nestedClassName);
		case NestedMethodSelector:
			return DiscoverySelectors.selectNestedMethod(this.enclosingClassNames, this.nestedClassName, this.methodName, this.methodParameterTypes);
		case PackageSelector:
			return DiscoverySelectors.selectPackage(this.packageName);
		case UniqueIdSelector:
			return DiscoverySelectors.selectUniqueId(this.uniqueId);
		case UriSelector:
			return DiscoverySelectors.selectUri(this.uri);
		default:
			throw new IllegalStateException();
		}
	}
		
	private void position(Optional<FilePosition> fpos) {
		fpos.ifPresent(fp->{
			this.line = fp.getLine();
			fp.getColumn().ifPresent(co->this.column=co);
		});
	}
	
	private FilePosition position() {
		if (line!=null) {
			if (column!=null) {
				return FilePosition.from(line, column);
			} else {
				return FilePosition.from(line);
			}
		} else {
			return null;
		}
	}


	public SelectorType getType() {
		return type;
	}


	public void setType(SelectorType type) {
		this.type = type;
	}


	public String getClasspathResourceName() {
		return classpathResourceName;
	}


	public void setClasspathResourceName(String classpathResourceName) {
		this.classpathResourceName = classpathResourceName;
	}


	public Integer getLine() {
		return line;
	}


	public void setLine(Integer line) {
		this.line = line;
	}


	public Integer getColumn() {
		return column;
	}


	public void setColumn(Integer column) {
		this.column = column;
	}


	public String getUri() {
		return uri;
	}


	public void setUri(String uri) {
		this.uri = uri;
	}


	public String getClassName() {
		return className;
	}


	public void setClassName(String className) {
		this.className = className;
	}


	public String getNestedClassName() {
		return nestedClassName;
	}


	public void setNestedClassName(String nestedClassName) {
		this.nestedClassName = nestedClassName;
	}


	public String getFile() {
		return file;
	}


	public void setFile(String file) {
		this.file = file;
	}


	public String getMethodName() {
		return methodName;
	}


	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}


	public String getMethodParameterTypes() {
		return methodParameterTypes;
	}


	public void setMethodParameterTypes(String methodParameterTypes) {
		this.methodParameterTypes = methodParameterTypes;
	}


	public String getModuleName() {
		return moduleName;
	}


	public void setModuleName(String moduleName) {
		this.moduleName = moduleName;
	}


	public List<String> getEnclosingClassNames() {
		return enclosingClassNames;
	}


	public void setEnclosingClassNames(List<String> enclosingClassNames) {
		this.enclosingClassNames = enclosingClassNames;
	}


	public String getPackageName() {
		return packageName;
	}


	public void setPackageName(String packageName) {
		this.packageName = packageName;
	}


	public String getUniqueId() {
		return uniqueId;
	}


	public void setUniqueId(String uniqueId) {
		this.uniqueId = uniqueId;
	}
	
	
	
}
