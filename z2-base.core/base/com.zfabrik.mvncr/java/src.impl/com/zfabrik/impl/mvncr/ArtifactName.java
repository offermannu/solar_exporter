/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.impl.mvncr;

import java.io.Serializable;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.aether.util.version.GenericVersionScheme;
import org.eclipse.aether.version.InvalidVersionSpecificationException;
import org.eclipse.aether.version.VersionScheme;

/**
 * An artifact is named by its fully qualified name such as 
 * 
 * <pre>
 * groupId:artifactId:packaging:version
 * </pre>
 * or
 * 
 * <pre>
 * groupId:artifactId:version
 * </pre>
 * 
 * where packaging defaults to "jar".
 * 
 * This is the artifact's name. Other methods return elements.
 * 
 * @see https://cwiki.apache.org/confluence/display/MAVENOLD/Repository+Layout+-+Final
 * @see http://maven.apache.org/pom.html
 * @author hb
 *
 */
public class ArtifactName implements Serializable {
	public static final String JAR = "jar";
	private static final long serialVersionUID = 1L;
	private String groupId;
	private String artifactId;
	private String packaging;
	private String version;
	
	public ArtifactName() {}
	
	public ArtifactName(String groupId, String artifactId, String version, String packaging) {
		super();
		this.groupId = groupId;
		this.artifactId = artifactId;
		this.version = version;
		this.packaging = packaging;
	}
	
	public String getGroupId() {
		return groupId;
	}
	public void setGroupId(String groupId) {
		this.groupId = StringUtils.trimToNull(groupId);
	}
	public String getArtifactId() {
		return artifactId;
	}
	public void setArtifactId(String artifactId) {
		this.artifactId = StringUtils.trimToNull(artifactId);
	}
	public String getPackaging() {
		return packaging;
	}
	public void setPackaging(String packaging) {
		this.packaging = StringUtils.trimToNull(packaging);
	}
	public String getVersion() {
		return version;
	}
	public void setVersion(String version) {
		this.version = StringUtils.trimToNull(version);
	}
	
	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		if (this.groupId!=null) {
			b.append(this.groupId);
			if (this.artifactId!=null) {
				b.append(":").append(this.artifactId);
				if (this.packaging!=null) {
					b.append(":").append(this.packaging);
					if (this.version!=null) {
						b.append(":").append(this.version);
					}
				}
			}
		}
		return b.toString();
	}

	public static ArtifactName parse(String name) {
		ArtifactName an = new ArtifactName();
		String[] es = name.split(":");
		if (es.length>=1) {
			an.groupId=StringUtils.trimToNull(es[0]);
		}
		if (es.length>=2) {
			an.artifactId =StringUtils.trimToNull(es[1]);
		}
		if (es.length==3) {
			// must be version and type defaults to "jar"
			an.packaging = JAR;
			an.version= StringUtils.trimToNull(es[2]);
		} else 
		if (es.length==4) {
			an.packaging=StringUtils.trimToNull(es[2]);
			an.version=StringUtils.trimToNull(es[3]);
		}
		return an;
	}
	
	public String toComponentName(boolean versioned) {
		StringBuilder b = new StringBuilder();  
		if (this.groupId!=null) {
			b.append(this.groupId);
			if (this.artifactId!=null) {
				b.append(":").append(this.artifactId);
				if (versioned && this.version!=null) {
					b.append(":").append(this.version);
				}
			}
		}
		if (this.packaging!=null) {
			if (JAR.equals(this.packaging)) {
				b.append("/").append("java");
			} else {
				b.append("/").append(this.packaging);
			}
		}
		return b.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((artifactId == null) ? 0 : artifactId.hashCode());
		result = prime * result + ((groupId == null) ? 0 : groupId.hashCode());
		result = prime * result + ((packaging == null) ? 0 : packaging.hashCode());
		result = prime * result + ((version == null) ? 0 : version.hashCode());
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
		ArtifactName other = (ArtifactName) obj;
		if (artifactId == null) {
			if (other.artifactId != null)
				return false;
		} else if (!artifactId.equals(other.artifactId))
			return false;
		if (groupId == null) {
			if (other.groupId != null)
				return false;
		} else if (!groupId.equals(other.groupId))
			return false;
		if (packaging == null) {
			if (other.packaging != null)
				return false;
		} else if (!packaging.equals(other.packaging))
			return false;
		if (version == null) {
			if (other.version != null)
				return false;
		} else if (!version.equals(other.version))
			return false;
		return true;
	}

	public int versionCompare(ArtifactName n) {
		if (n.version==null && this.version!=null) {
			return 1;
		}
		if (n.version!=null && this.version==null) {
			return -1;
		}
		VersionScheme s = new GenericVersionScheme();
		try {
			return s.parseVersion(this.getVersion()).compareTo(s.parseVersion(n.getVersion()));
		} catch (InvalidVersionSpecificationException e) {
			throw new IllegalArgumentException("Cannot compare versions "+this.version+" and "+n.version);
		}
	}
	
	
}
