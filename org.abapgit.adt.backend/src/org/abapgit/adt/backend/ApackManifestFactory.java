package org.abapgit.adt.backend;

import org.abapgit.adt.backend.internal.ApackManifest;
import org.abapgit.adt.backend.internal.ApackManifestDescriptor;

public class ApackManifestFactory {

	private ApackManifestFactory() {
		// Service class with static methods only...
	}

	public static IApackManifest createApackManifest(String groupId, String artifactId, String gitUrl) {

		ApackManifest apackManifest = new ApackManifest();
		ApackManifestDescriptor manifestDescriptor = new ApackManifestDescriptor();
		manifestDescriptor.setGroupId(groupId);
		manifestDescriptor.setPackageId(artifactId);
		manifestDescriptor.setGitUrl(gitUrl);
		manifestDescriptor.setRepositoryType(EApackRepositoryType.abapGit);
		manifestDescriptor.setVersion("1"); //$NON-NLS-1$
		apackManifest.setDescriptor(manifestDescriptor);

		return apackManifest;
	}

}
