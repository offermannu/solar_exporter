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

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.engine.support.descriptor.ClasspathResourceSource;
import org.junit.platform.engine.support.descriptor.CompositeTestSource;
import org.junit.platform.engine.support.descriptor.FilePosition;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.engine.support.descriptor.PackageSource;

/**
 * Serialization DTO of TestSource
 */
public class Z2JupiterTestSourceDto {
	
	public enum TestSourceType {
		CLASSPATH_RESOURCE_SOURCE,
		CLASS_SOURCE,
		COMPOSITE_TEST_SOURCE,
		METHOD_SOURCE,
		PACKAGE_SOURCE
		;

		public static TestSourceType fromSource(TestSource testSource) {
			Class<?> clz = testSource.getClass();
			if (clz.equals(ClasspathResourceSource.class)) {
				return CLASSPATH_RESOURCE_SOURCE;
			}
			if (clz.equals(ClassSource.class)) {
				return CLASS_SOURCE;
			}
			if (clz.equals(CompositeTestSource.class)) {
				return COMPOSITE_TEST_SOURCE;
			}
			if (clz.equals(MethodSource.class)) {
				return METHOD_SOURCE;
			}
			if (clz.equals(PackageSource.class)) {
				return PACKAGE_SOURCE;
			}
			throw new IllegalArgumentException(testSource.getClass().getName());
		}
	}
	
	private TestSourceType type;
	private String className;
	private String classpathResourceName;
	private Integer line;
	private Integer column;
	private List<Z2JupiterTestSourceDto> sources;
	private String methodName;
	private String methodParameterTypes;
	private String packageName;
	
	public Z2JupiterTestSourceDto() {
	}
	
	public Z2JupiterTestSourceDto(TestSource testSource) {
		this.type = TestSourceType.fromSource(testSource);
		switch (this.type) {
			case CLASS_SOURCE: {
				ClassSource cs = (ClassSource) testSource;
				this.className = cs.getClassName();
				this.setPosition(cs.getPosition());
				break;
			}
			case CLASSPATH_RESOURCE_SOURCE: {
				ClasspathResourceSource cs = (ClasspathResourceSource) testSource;
				this.classpathResourceName = cs.getClasspathResourceName();
				this.setPosition(cs.getPosition());
				break;
			}
			case COMPOSITE_TEST_SOURCE: {
				CompositeTestSource cs = (CompositeTestSource) testSource;
				this.sources = cs.getSources().stream().map(Z2JupiterTestSourceDto::new).collect(Collectors.toList());
				break;
			}
			case METHOD_SOURCE: {
				MethodSource cs = (MethodSource) testSource;
				this.className = cs.getClassName();
				this.methodName = cs.getMethodName();
				this.methodParameterTypes = cs.getMethodParameterTypes();
				break;
			}
			case PACKAGE_SOURCE: {
				PackageSource cs = (PackageSource) testSource;
				packageName = cs.getPackageName();
				break;
			}
		}
	}
	
	

	private void setPosition(Optional<FilePosition> fpos) {
		fpos.ifPresent(fp->{
			this.line = fp.getLine();
			fp.getColumn().ifPresent(co->this.column=co);
		});
	}
	
	private FilePosition getPosition() {
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
			
	public TestSource toTestSource() {
		switch (this.type) {
			case CLASS_SOURCE: {
				if (line==null) {
					return ClassSource.from(className);
				} else {
					return ClassSource.from(className, getPosition());
				}
			}
			case CLASSPATH_RESOURCE_SOURCE: {
				if (line==null) {
					return ClasspathResourceSource.from(classpathResourceName);
				} else {
					return ClasspathResourceSource.from(classpathResourceName, getPosition());
				}
			}
			case COMPOSITE_TEST_SOURCE: {
				return CompositeTestSource.from(this.sources.stream().map(Z2JupiterTestSourceDto::toTestSource).collect(Collectors.toList()));
			}
			case METHOD_SOURCE: {
				return toMethodSource();
			}
			case PACKAGE_SOURCE: {
				return PackageSource.from(packageName);
			}
		}
		throw new IllegalStateException();
	}

	// Method source is reused for classpathresource to TestFactoryMethodTestDescriptor mapping
	public MethodSource toMethodSource() {
		if (this.methodParameterTypes!=null) {
			return MethodSource.from(className, methodName, methodParameterTypes);
		} else {
			return MethodSource.from(className, methodName);
		}
	}

	public TestSourceType getType() {
		return type;
	}

	public void setType(TestSourceType type) {
		this.type = type;
	}

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
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

	public List<Z2JupiterTestSourceDto> getSources() {
		return sources;
	}

	public void setSources(List<Z2JupiterTestSourceDto> sources) {
		this.sources = sources;
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

	public String getPackageName() {
		return packageName;
	}

	public void setPackageName(String packageName) {
		this.packageName = packageName;
	}
	
}
