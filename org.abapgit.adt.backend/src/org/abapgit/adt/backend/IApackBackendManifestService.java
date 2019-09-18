package org.abapgit.adt.backend;

import org.eclipse.core.runtime.IProgressMonitor;

public interface IApackBackendManifestService {

	IApackManifest getManifest(String groupId, String artifactId, IProgressMonitor monitor);

}
