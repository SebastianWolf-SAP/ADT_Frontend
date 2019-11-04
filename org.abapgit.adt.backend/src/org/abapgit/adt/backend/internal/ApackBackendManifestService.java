package org.abapgit.adt.backend.internal;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;

import org.abapgit.adt.backend.IApackBackendManifestService;
import org.abapgit.adt.backend.IApackManifest;
import org.eclipse.core.runtime.IProgressMonitor;

import com.sap.adt.communication.resources.AdtRestResourceFactory;
import com.sap.adt.communication.resources.IRestResource;
import com.sap.adt.communication.resources.ResourceException;
import com.sap.adt.compatibility.filter.AdtCompatibleRestResourceFilterFactory;
import com.sap.adt.compatibility.filter.IAdtCompatibleRestResourceFilter;

public class ApackBackendManifestService implements IApackBackendManifestService {

	private final String destinationId;
	private final URI uri;

	public ApackBackendManifestService(String destinationId, URI uri) {
		this.destinationId = destinationId;
		this.uri = uri;
	}

	@Override
	public IApackManifest getManifest(String groupId, String artifactId, IProgressMonitor monitor) {

		IApackManifest apackManifest = null;

		try {
			StringBuilder stringBuilder = new StringBuilder();
			stringBuilder.append(this.uri.toString());
			stringBuilder.append("/"); //$NON-NLS-1$
			stringBuilder.append(URLEncoder.encode(groupId, "UTF-8")); //$NON-NLS-1$
			stringBuilder.append("/"); //$NON-NLS-1$
			stringBuilder.append(URLEncoder.encode(artifactId, "UTF-8")); //$NON-NLS-1$
			URI uriWithPath = URI.create(stringBuilder.toString());

			IRestResource restResource = AdtRestResourceFactory.createRestResourceFactory().createResourceWithStatelessSession(uriWithPath,
					this.destinationId);

			ApackManifestResponseContentHandlerV1 responseContentHandler = new ApackManifestResponseContentHandlerV1();
			responseContentHandler.setAcceptHeaderBackendManifest();
			restResource.addContentHandler(responseContentHandler);

			IAdtCompatibleRestResourceFilter compatibilityFilter = AdtCompatibleRestResourceFilterFactory
					.createFilter(responseContentHandler);
			restResource.addRequestFilter(compatibilityFilter);
			restResource.addResponseFilter(compatibilityFilter);

			return restResource.get(monitor, IApackManifest.class);

		} catch (IOException ioe) {
			// If the parameters can't be converted, we consider the manifest to be non-existent
			return apackManifest;
		} catch (ResourceException re) {
			// Backend resource not existing -> initial manifest
			return apackManifest;
		}
	}

}
