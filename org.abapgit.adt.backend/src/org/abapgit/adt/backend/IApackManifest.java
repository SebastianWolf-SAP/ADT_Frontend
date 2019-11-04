package org.abapgit.adt.backend;

import java.util.List;

import com.sap.adt.tools.core.model.adtcore.IAdtObjectReference;

public interface IApackManifest {

	public static final String MASTER_BRANCH = "refs/heads/master"; //$NON-NLS-1$

	public enum EApackVersionDependencyClassifier {
		inclusive, exclusive;
	}

	public interface IApackVersionDependency {
		String getMinimum();

		EApackVersionDependencyClassifier getMinimumClassifier();

		String getMaximum();

		EApackVersionDependencyClassifier getMaximumClassifier();

		boolean isVersionCompatible(String version);

		int getMinimumMajor();

		int getMinimumMinor();

		int getMinimumPatch();

		int getMaximumMajor();

		int getMaximumMinor();

		int getMaximumPatch();

		boolean hasRange();

		boolean isValid();
	}

	public interface IApackDependency {

		String getGroupId();

		String getArtifactId();

		String getGlobalIdentifier();

		IApackVersionDependency getVersion();

		String getGitUrl();

		boolean isEmpty();

		void setTargetPackage(IAdtObjectReference targetPackage);

		IAdtObjectReference getTargetPackage();

		boolean requiresLink();

		void setRequiresLink(boolean requiresLink);

		boolean requiresPull();

		void setRequiresPull(boolean requiresPull);

		void setSyncMessage(String text, int type);

		String getSyncMessageText();

		int getSyncMessageType();

	}

	interface IApackManifestDescriptor {

		String getGroupId();

		String getPackageId();

		String getGlobalIdentifer();

		String getVersion();

		EApackRepositoryType getRepositoryType();

		String getGitUrl();

		List<IApackDependency> getDependencies();

		void setDependencies(List<IApackDependency> dependencies);

		void addDependency(IApackDependency dependency);

	}

	IApackManifestDescriptor getDescriptor();

	boolean hasDependencies();

	void setSyncMessage(String text, int type);

	String getSyncMessageText();

	int getSyncMessageType();

}
