package org.abapgit.adt.backend.internal;

import org.abapgit.adt.backend.IApackManifest.IApackDependency;
import org.abapgit.adt.backend.IApackManifest.IApackVersionDependency;

import com.sap.adt.tools.core.model.adtcore.IAdtObjectReference;

public class ApackDependency implements IApackDependency {

	private String groupId;
	private String artifactId;
	private IApackVersionDependency version;
	private String gitUrl;
	private IAdtObjectReference targetPackage;
	private boolean requiresLink;
	private boolean requiresPull;
	private String syncMessageText;
	private int syncMessageType;

	public ApackDependency() {
		this.requiresLink = true;
		this.requiresPull = true;
	}

	@Override
	public String getGroupId() {
		return this.groupId;
	}

	@Override
	public String getArtifactId() {
		return this.artifactId;
	}

	@Override
	public String getGitUrl() {
		return this.gitUrl;
	}

	public void setGroupId(String groupId) {
		this.groupId = groupId;
	}

	public void setArtifactId(String artifactId) {
		this.artifactId = artifactId;
	}

	public void setGitUrl(String homeUrl) {
		this.gitUrl = homeUrl;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Dependency [groupId="); //$NON-NLS-1$
		builder.append(this.groupId);
		builder.append(", artifactId="); //$NON-NLS-1$
		builder.append(this.artifactId);
		builder.append(", version="); //$NON-NLS-1$
		builder.append(this.version);
		builder.append(", gitUrl="); //$NON-NLS-1$
		builder.append(this.gitUrl);
		builder.append(", targetPackage="); //$NON-NLS-1$
		builder.append(this.targetPackage);
		builder.append(", requiresSynchronization="); //$NON-NLS-1$
		builder.append(this.requiresLink);
		builder.append("]"); //$NON-NLS-1$
		return builder.toString();
	}

	@Override
	public int hashCode() { // NOPMD
		final int prime = 31;
		int result = 1;
		result = prime * result + ((this.groupId == null) ? 0 : this.groupId.hashCode());
		result = prime * result + ((this.artifactId == null) ? 0 : this.artifactId.hashCode());
		result = prime * result + ((this.version == null) ? 0 : this.version.hashCode());
		result = prime * result + ((this.gitUrl == null) ? 0 : this.gitUrl.hashCode());
		// No target package as it doesn't implement a correct equals and is also not really relevant for comparison ;)
		result = prime * result + (this.requiresLink ? 1337 : 7331);
		return result;
	}

	@Override
	public boolean equals(Object obj) { // NOPMD
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		ApackDependency other = (ApackDependency) obj;
		if (this.groupId == null) {
			if (other.groupId != null) {
				return false;
			}
		} else if (!this.groupId.equals(other.groupId)) {
			return false;
		}
		if (this.artifactId == null) {
			if (other.artifactId != null) {
				return false;
			}
		} else if (!this.artifactId.equals(other.artifactId)) {
			return false;
		}
		if (this.version == null) {
			if (other.version != null) {
				return false;
			}
		} else if (!this.version.equals(other.version)) {
			return false;
		}
		if (this.gitUrl == null) {
			if (other.gitUrl != null) {
				return false;
			}
		} else if (!this.gitUrl.equals(other.gitUrl)) {
			return false;
		}
		// No target package as it doesn't implement a correct equals and is also not really relevant for comparison ;)
		return this.requiresLink == other.requiresLink;
	}

	@Override
	public boolean isEmpty() {
		return (this.groupId == null || this.groupId.isEmpty()) && (this.artifactId == null || this.artifactId.isEmpty())
				&& (this.gitUrl == null || this.gitUrl.isEmpty() && this.targetPackage == null && !this.requiresLink);
	}

	@Override
	public void setTargetPackage(IAdtObjectReference targetPackage) {
		this.targetPackage = targetPackage;
	}

	@Override
	public IAdtObjectReference getTargetPackage() {
		return this.targetPackage;
	}

	@Override
	public boolean requiresLink() {
		return this.requiresLink;
	}

	@Override
	public void setRequiresLink(boolean requiresSynchronization) {
		this.requiresLink = requiresSynchronization;
	}

	@Override
	public IApackVersionDependency getVersion() {
		return this.version;
	}

	public void setVersion(IApackVersionDependency version) {
		this.version = version;
	}

	@Override
	public String getGlobalIdentifier() {
		return this.groupId + "/" + this.artifactId; //$NON-NLS-1$
	}

	@Override
	public void setSyncMessage(String text, int type) {
		this.syncMessageText = text;
		this.syncMessageType = type;
	}

	@Override
	public String getSyncMessageText() {
		return this.syncMessageText;
	}

	@Override
	public int getSyncMessageType() {
		return this.syncMessageType;
	}

	@Override
	public boolean requiresPull() {
		return this.requiresPull;
	}

	@Override
	public void setRequiresPull(boolean requiresPull) {
		this.requiresPull = requiresPull;
	}

}
