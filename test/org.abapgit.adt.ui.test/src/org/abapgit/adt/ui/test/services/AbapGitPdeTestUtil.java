package org.abapgit.adt.ui.test.services;

import org.abapgit.adt.ui.internal.views.AbapGitView;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.sap.adt.destinations.model.AdtDestinationDataFactory;
import com.sap.adt.destinations.model.IDestinationData;
import com.sap.adt.destinations.model.IDestinationDataWritable;
import com.sap.adt.tools.core.project.AdtProjectServiceFactory;
import com.sap.adt.tools.core.project.IAbapProjectService;

public class AbapGitPdeTestUtil {
	
	public static final String DUMMY_PROJECT_NAME = "ABAPGIT_TEST_PROJECT";
	public static final String DUMMY_DESTINATION_ID = "ABAPGIT_TEST_PROJECT";

	private AbapGitPdeTestUtil() {
		// Static test helper class, uses static methods only
	}

	public static IProject createDummyAbapProject() throws CoreException {

		IDestinationDataWritable data = AdtDestinationDataFactory.newDestinationData(DUMMY_DESTINATION_ID);
		data.setUser("TEST_DUMMY_USER");
		data.setClient("666");
		data.setLanguage("DE");
		data.setPassword("TEST_DUMMY_PW");

		String projectDestinationId = AdtProjectServiceFactory.createProjectService().createDestinationId(DUMMY_PROJECT_NAME);
		final IDestinationData destinationData = data.getReadOnlyClone(projectDestinationId);

		final IAbapProjectService abapProjectService = AdtProjectServiceFactory.createProjectService();
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(DUMMY_PROJECT_NAME);
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		workspace.run(new IWorkspaceRunnable() {
			@Override
			public void run(IProgressMonitor monitor) throws CoreException {
				abapProjectService.createAbapProject(DUMMY_PROJECT_NAME, destinationData, monitor);
			}
		}, new NullProgressMonitor());
		return project;
	}
	
	public static AbapGitView initializeView() throws PartInitException{
		AbapGitView view;
		IWorkbenchPage activePage = PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow().getActivePage();
		IPerspectiveDescriptor abapPerspective = PlatformUI.getWorkbench()
				.getPerspectiveRegistry()
				.findPerspectiveWithId("com.sap.adt.ui.AbapPerspective"); //$NON-NLS-1$
		activePage.setPerspective(abapPerspective);
		view = ((AbapGitView) PlatformUI.getWorkbench().getActiveWorkbenchWindow()
				.getActivePage().showView(AbapGitView.ID));
		view.init(view.getViewSite());
		Shell shell = new Shell(Display.getDefault().getActiveShell());
		Composite parent = new Composite(shell, SWT.NONE);
		view.createPartControl(parent);				
		return view;
	}
	
	public static void waitInUI(long timeout){
		Display display = Display.getCurrent();
		final long start = System.currentTimeMillis();
		while (true) {
			if (System.currentTimeMillis() - start > timeout) {
				return;
			}
			try {
				if (!display.readAndDispatch()) {
					display.sleep();
				}
			} catch (Exception e) {
			}
		}
	}

}
