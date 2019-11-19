package org.abapgit.adt.ui.internal.wizards;

import org.abapgit.adt.backend.IRepositoryService;
import org.abapgit.adt.ui.test.services.AbapGitPdeTestUtil;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.PlatformUI;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import com.sap.adt.tools.core.model.adtcore.IAdtObjectReference;
import com.sap.adt.transport.IAdtTransportCheckData;
import com.sap.adt.transport.IAdtTransportService;

public class TestsPdeAbapGitLinkPullWizard {
	
	@Mock
	private IAdtTransportService mockTransportService;
	@Mock
	private IRepositoryService mockRepositoryService;
	private WizardDialog wizardDialog;	
	private IProject dummyProject;

	@Before
	public void setup() throws Exception {
		this.dummyProject = AbapGitPdeTestUtil.createDummyAbapProject();
		this.mockTransportService = Mockito.mock(IAdtTransportService.class);
	}

	@Test
	public void walkthrough() {		
		Mockito.when(mockTransportService.check(Mockito.any(IAdtObjectReference.class), Mockito.anyString(), Mockito.anyBoolean())).thenReturn(Mockito.mock(IAdtTransportCheckData.class));
		wizardDialog = new WizardDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),new AbapGitWizard(this.dummyProject, mockTransportService, mockRepositoryService));
		wizardDialog.open();
	}
	
	@After
	public void tearDown() throws Exception {
		if (this.wizardDialog != null && this.wizardDialog.getShell() != null && !this.wizardDialog.getShell().isDisposed()) {
			this.wizardDialog.close();
		}
	}
	

}
