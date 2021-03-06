package org.abapgit.adt.ui.internal.wizards;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.abapgit.adt.backend.ApackManifestFactory;
import org.abapgit.adt.backend.ApackServiceFactory;
import org.abapgit.adt.backend.IApackBackendManifestService;
import org.abapgit.adt.backend.IApackGitManifestService;
import org.abapgit.adt.backend.IApackManifest;
import org.abapgit.adt.backend.IApackManifest.IApackDependency;
import org.abapgit.adt.backend.IExternalRepositoryInfo.AccessMode;
import org.abapgit.adt.backend.IExternalRepositoryInfo.IBranch;
import org.abapgit.adt.ui.internal.i18n.Messages;
import org.abapgit.adt.ui.internal.wizards.AbapGitWizard.CloneData;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.sap.adt.tools.core.model.adtcore.IAdtObjectReference;
import com.sap.adt.tools.core.ui.packages.AdtPackageProposalProviderFactory;
import com.sap.adt.tools.core.ui.packages.AdtPackageServiceUIFactory;
import com.sap.adt.tools.core.ui.packages.IAdtPackageProposalProvider;
import com.sap.adt.tools.core.ui.packages.IAdtPackageServiceUI;
import com.sap.adt.util.ui.swt.AdtSWTUtilFactory;

public class AbapGitWizardPageBranchAndPackage extends WizardPage {

	private static final String PAGE_NAME = AbapGitWizardPageRepositoryAndCredentials.class.getName();

	private final IProject project;
	private final String destination;
	private final CloneData cloneData;
	private final String pullBranch;

	private Button checkbox_lnp;
	private Boolean chboxLinkAndPull;
	private TextViewer txtPackage;
	private ComboViewer comboBranches;

	private final Boolean pullAction;
	private boolean backButtonEnabled = true;
	private final ApackParameters lastApackCall;

	public AbapGitWizardPageBranchAndPackage(IProject project, String destination, CloneData cloneData, Boolean pullAction) {
		super(PAGE_NAME);
		this.project = project;
		this.destination = destination;
		this.cloneData = cloneData;
		this.pullBranch = cloneData.branch;
		this.pullAction = pullAction;
		this.lastApackCall = new ApackParameters();
		this.chboxLinkAndPull = false;

		setTitle(Messages.AbapGitWizardPageBranchAndPackage_title);
		setDescription(Messages.AbapGitWizardPageBranchAndPackage_description);
	}

	@Override
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(3, false);
		container.setLayout(layout);

		/////// BRANCH INPUT
		Label lblBranch = new Label(container, SWT.NONE);
		lblBranch.setText(Messages.AbapGitWizardPageBranchAndPackage_label_branch);
		AdtSWTUtilFactory.getOrCreateSWTUtil().setMandatory(lblBranch, true);
		GridDataFactory.swtDefaults().applyTo(lblBranch);

		this.comboBranches = new ComboViewer(container, SWT.BORDER);
		GridDataFactory.swtDefaults().span(2, 0).align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(this.comboBranches.getControl());
		this.comboBranches.setContentProvider(ArrayContentProvider.getInstance());
		this.comboBranches.setLabelProvider(new LabelProvider() {

			@Override
			public String getText(Object element) {
				if (element instanceof IBranch) {
					return ((IBranch) element).getName();
				}
				return super.getText(element);
			}

		});
		this.comboBranches.getCombo().addModifyListener(event -> {
			this.cloneData.branch = this.comboBranches.getCombo().getText();
			validateClientOnly();
			fetchApackManifest();
		});

		/////// Package INPUT
		Label lblPackage = new Label(container, SWT.NONE);
		lblPackage.setText(Messages.AbapGitWizardPageBranchAndPackage_label_package);
		AdtSWTUtilFactory.getOrCreateSWTUtil().setMandatory(lblPackage, true);
		GridDataFactory.swtDefaults().applyTo(lblPackage);

		this.txtPackage = new TextViewer(container, SWT.SINGLE | SWT.BORDER);
		GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(this.txtPackage.getTextWidget());
		this.txtPackage.getTextWidget().setText(""); //$NON-NLS-1$

		this.txtPackage.getTextWidget().addModifyListener(event -> {
			this.cloneData.packageRef = null;
			validateClientOnly();
		});

		IAdtPackageProposalProvider packageProposalProvider = AdtPackageProposalProviderFactory
				.createPackageProposalProvider(this.txtPackage);
		packageProposalProvider.setProject(this.project);

		Button btnPackage = new Button(container, SWT.PUSH);
		GridDataFactory.swtDefaults().applyTo(btnPackage);
		btnPackage.setText(Messages.AbapGitWizardPageBranchAndPackage_btn_browse);

		btnPackage.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				IAdtPackageServiceUI packageServiceUI = AdtPackageServiceUIFactory.getOrCreateAdtPackageServiceUI();
				IAdtObjectReference[] selectedPackages = packageServiceUI.openPackageSelectionDialog(e.display.getActiveShell(), false,
						AbapGitWizardPageBranchAndPackage.this.destination,
						AbapGitWizardPageBranchAndPackage.this.txtPackage.getTextWidget().getText());
				if (selectedPackages != null && selectedPackages.length > 0) {
					AbapGitWizardPageBranchAndPackage.this.txtPackage.getTextWidget().setText(selectedPackages[0].getName());
					AbapGitWizardPageBranchAndPackage.this.cloneData.packageRef = selectedPackages[0];
				}
			}
		});

		//-> Show checkbox only in link wizard
		if (!this.pullAction) {
			/////// CHECKBOX Link & Pull
			Label lblLnp = new Label(container, SWT.NONE);
			lblLnp.setText(Messages.AbapGitWizardPageBranchAndPackage_chbox_activate);
			lblLnp.setToolTipText(Messages.AbapGitWizardPageBranchAndPackage_chbox_activate_tooltip);
			GridDataFactory.swtDefaults().applyTo(lblLnp);

			this.checkbox_lnp = new Button(container, SWT.CHECK);
			GridDataFactory.swtDefaults().applyTo(this.checkbox_lnp);

			this.checkbox_lnp.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent event) {
					Button chbox = (Button) event.getSource();
					setLnpSequence(chbox.getSelection());
				}
			});
		}

		setControl(container);

		if (this.cloneData.url != null) {

			this.comboBranches.getCombo().setEnabled(false);
			this.txtPackage.getTextWidget().setText(this.cloneData.packageRef.getName());
			AdtSWTUtilFactory.getOrCreateSWTUtil().setEditable(this.txtPackage.getControl(), false);

			btnPackage.setEnabled(false);

			//-> Disable back navigation if repo is public and we're in pull wizard
			if (this.cloneData.externalRepoInfo != null && this.cloneData.externalRepoInfo.getAccessMode() == AccessMode.PUBLIC) {
				setBackButtonEnabled(false);
			}
		}

		validateClientOnly();
	}

	private void setLnpSequence(boolean chboxValue) {
		this.chboxLinkAndPull = chboxValue;
	}

	public boolean getLnpSequence() {
		return this.chboxLinkAndPull;
	}

	public void setBackButtonEnabled(boolean enabled) {
		this.backButtonEnabled = enabled;
		getContainer().updateButtons();
	}

	@Override
	public IWizardPage getPreviousPage() {
		if (!this.backButtonEnabled) {
			return null;
		}
		return super.getPreviousPage();
	}

	private boolean validateClientOnly() {
		setPageComplete(true);
		setMessage(null);

		if (this.comboBranches.getCombo().getText().isEmpty()) {
			setMessage(Messages.AbapGitWizardPageBranchAndPackage_combobox_branch_message, DialogPage.INFORMATION);
			setPageComplete(false);
			return false;
		}

		if (this.txtPackage.getTextWidget().getText().isEmpty()) {
			setMessage(Messages.AbapGitWizardPageBranchAndPackage_text_package_message, DialogPage.INFORMATION);
			setPageComplete(false);
			return false;
		}
		return true;
	}

	public boolean validateAll() {
		if (!validateClientOnly()) {
			return false;
		}
		if (this.cloneData.packageRef == null) {
			try {
				String packageName = this.txtPackage.getTextWidget().getText();
				getContainer().run(true, true, new IRunnableWithProgress() {

					@Override
					public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
						monitor.beginTask(Messages.AbapGitWizardPageBranchAndPackage_task_package_validation_message,
								IProgressMonitor.UNKNOWN);
						IAdtPackageServiceUI packageServiceUI = AdtPackageServiceUIFactory.getOrCreateAdtPackageServiceUI();
						if (packageServiceUI.packageExists(AbapGitWizardPageBranchAndPackage.this.destination, packageName, monitor)) {
							List<IAdtObjectReference> packageRefs = packageServiceUI
									.find(AbapGitWizardPageBranchAndPackage.this.destination, packageName, monitor);
							AbapGitWizardPageBranchAndPackage.this.cloneData.packageRef = packageRefs.stream().findFirst().orElse(null);
						}
					}
				});
				if (this.cloneData.packageRef == null) {
					setMessage(Messages.AbapGitWizardPageBranchAndPackage_task_package_validation_error_message, DialogPage.ERROR);
					setPageComplete(false);
					return false;
				}
			} catch (InvocationTargetException e) {
				setMessage(e.getTargetException().getMessage(), DialogPage.ERROR);
				setPageComplete(false);
				return false;
			} catch (InterruptedException e) {
				return false;
			}
		}
		return true;
	}

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);

		if (visible) {
			this.comboBranches.setInput(null);
			this.comboBranches.setSelection(StructuredSelection.EMPTY);
			if (this.cloneData.externalRepoInfo != null) {
				List<IBranch> branches = this.cloneData.externalRepoInfo.getBranches();
				this.comboBranches.setInput(branches);
				if (!branches.isEmpty()) {
					IBranch selectedBranch = branches.stream().filter(b -> b.isHead()).findFirst()
							.orElse(branches.stream().findFirst().get());

					//PULL branch is pre populated
					if (this.pullBranch != null && selectedBranch.getName() != this.pullBranch) {
						selectedBranch = branches.stream().filter(b -> b.getName().equals(this.pullBranch)).findFirst().get();
					}

					this.comboBranches.setSelection(new StructuredSelection(selectedBranch));
				}
			}

			fetchApackManifest();
		}
	}

	private void fetchApackManifest() {
		if (this.cloneData.url.isEmpty() || this.cloneData.branch.isEmpty()) {
			return;
		}
		if (this.cloneData.url.equals(this.lastApackCall.url) && this.cloneData.branch.equals(this.lastApackCall.branch)) {
			return;
		}
		this.cloneData.apackManifest = null;
		try {
			getContainer().run(true, true, new IRunnableWithProgress() {

				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {

					final HashMap<String, Boolean> dependencyCoverage = new HashMap<String, Boolean>();
					final List<IApackManifest> retrievedGitManifests = new ArrayList<IApackManifest>();
					final Map<String, IApackManifest> retrievedBackendManifests = new HashMap<String, IApackManifest>();

					ApackParameters nextApackCall = new ApackParameters();
					nextApackCall.url = AbapGitWizardPageBranchAndPackage.this.cloneData.url;
					nextApackCall.branch = AbapGitWizardPageBranchAndPackage.this.cloneData.branch;

					AbapGitWizardPageBranchAndPackage.this.cloneData.apackManifest = retrieveApackManifest(monitor, dependencyCoverage,
							retrievedGitManifests, nextApackCall);

					retrieveBackendManifests(monitor, retrievedBackendManifests);

					AbapGitWizardPageBranchAndPackage.this.lastApackCall.url = AbapGitWizardPageBranchAndPackage.this.cloneData.url;
					AbapGitWizardPageBranchAndPackage.this.lastApackCall.branch = AbapGitWizardPageBranchAndPackage.this.cloneData.branch;

					evaluateManifests(retrievedGitManifests, retrievedBackendManifests);
				}

				private void retrieveBackendManifests(IProgressMonitor monitor, final Map<String, IApackManifest> installedManifests) {
					// Retrieve installed manifests and populate versions for later evaluation
					IApackBackendManifestService backendManifestService = ApackServiceFactory
							.createApackBackendManifestService(AbapGitWizardPageBranchAndPackage.this.destination, monitor);
					retrieveBackendManifest(backendManifestService, monitor, installedManifests,
							AbapGitWizardPageBranchAndPackage.this.cloneData.apackManifest.getDescriptor().getGroupId(),
							AbapGitWizardPageBranchAndPackage.this.cloneData.apackManifest.getDescriptor().getPackageId());
					for (IApackDependency dependency : AbapGitWizardPageBranchAndPackage.this.cloneData.apackManifest.getDescriptor()
							.getDependencies()) {
						retrieveBackendManifest(backendManifestService, monitor, installedManifests, dependency.getGroupId(),
								dependency.getArtifactId());
					}
				}

				private void retrieveBackendManifest(IApackBackendManifestService backendManifestService, IProgressMonitor monitor,
						final Map<String, IApackManifest> installedManifests, String groupId, String artifactId) {
					IApackManifest backendManifest = backendManifestService.getManifest(groupId, artifactId, monitor);
					if (backendManifest != null && backendManifest.getDescriptor() != null) {
						installedManifests.put(backendManifest.getDescriptor().getGlobalIdentifer(), backendManifest);
					}
				}

				private IApackManifest retrieveApackManifest(IProgressMonitor monitor, final HashMap<String, Boolean> dependencyCoverage,
						final List<IApackManifest> retrievedGitManifests, ApackParameters apackParameters) {

					monitor.beginTask(NLS.bind(Messages.AbapGitWizardPageBranchAndPackage_task_apack_manifest_message,
							AbapGitWizardPageBranchAndPackage.this.cloneData.url), IProgressMonitor.UNKNOWN);
					IApackGitManifestService manifestService = ApackServiceFactory
							.createApackGitManifestService(AbapGitWizardPageBranchAndPackage.this.destination, monitor);
					IApackManifest myManifest = null;
					if (manifestService != null) {
						myManifest = manifestService.getManifest(apackParameters.url, apackParameters.branch,
								AbapGitWizardPageBranchAndPackage.this.cloneData.user,
								AbapGitWizardPageBranchAndPackage.this.cloneData.pass, monitor);
						retrievedGitManifests.add(myManifest);
						dependencyCoverage.put(apackParameters.url, true);
						if (myManifest.hasDependencies()) {
							List<IApackDependency> retrievedDependencies = new ArrayList<IApackDependency>();
							for (IApackDependency dependency : myManifest.getDescriptor().getDependencies()) {
								retrievedDependencies.add(dependency);
								retrieveDependentManifests(ApackParameters.createFromDependency(dependency), dependencyCoverage,
										retrievedDependencies, retrievedGitManifests, manifestService, monitor);
							}
							myManifest.getDescriptor().setDependencies(retrievedDependencies);
						}
					}
					return myManifest;

				}

				private void retrieveDependentManifests(ApackParameters apackParameters, final HashMap<String, Boolean> dependencyCoverage,
						final List<IApackDependency> retrievedDependencies, final List<IApackManifest> retrievedGitManifests,
						IApackGitManifestService manifestService,
						IProgressMonitor monitor) {
					monitor.beginTask(NLS.bind(Messages.AbapGitWizardPageBranchAndPackage_task_apack_manifest_message, apackParameters.url),
							IProgressMonitor.UNKNOWN);
					IApackManifest myManifest = manifestService.getManifest(apackParameters.url, apackParameters.branch,
							AbapGitWizardPageBranchAndPackage.this.cloneData.user, AbapGitWizardPageBranchAndPackage.this.cloneData.pass,
							monitor);
					if (myManifest.isEmpty()) {
						// Remote Git repository does not yet support APACK, but should be able to be referenced anyway
						myManifest = ApackManifestFactory.createApackManifest(apackParameters.groupId, apackParameters.artifactId,
								apackParameters.url);
					}
					retrievedGitManifests.add(myManifest);
					dependencyCoverage.put(apackParameters.url, true);
					if (myManifest.hasDependencies()) {
						for (IApackDependency myDependency : myManifest.getDescriptor().getDependencies()) {
							if (!retrievedDependencies.contains(myDependency)) {
								retrievedDependencies.add(myDependency);
							}
							if (!dependencyCoverage.getOrDefault(myDependency.getGitUrl(), false)) {
								retrieveDependentManifests(ApackParameters.createFromDependency(myDependency), dependencyCoverage,
										retrievedDependencies, retrievedGitManifests, manifestService, monitor);
							}
						}
					}
				}
			});

			setPageComplete(true);
			setMessage(null);

		} catch (InvocationTargetException e) {
			setPageComplete(false);
			setMessage(e.getTargetException().getMessage(), DialogPage.ERROR);
		} catch (InterruptedException e) {
			// Call was aborted - no dependencies will be retrieved and used in the import
			setPageComplete(true);
		}
	}

	private void evaluateManifests(final List<IApackManifest> retrievedGitManifests,
			final Map<String, IApackManifest> installedManifests) {

		// We check all dependencies if they are already installed and which version is already installed
		// The evaluation of the results will be done according to the following rules:
		// - If the dependency is not yet installed, it will be installed if the remote Git repo contains a compatible one
		// - If it's installed and there is no dedicated version (range) required, it remains untouched
		// - If it's installed and the required version (range) is compatible, it remains untouched
		// - If it's installed and the required version (range) is incompatible, we try to install a compatible one
		// - If we determine that a remote repository doesn't contain a compatible version,
		//   we ask the user if the remote one should be installed anyway

		// Dependency issues will be reported as errors
		// Updated dependencies will be reported as information messages



		for (IApackDependency currentDependency : this.cloneData.apackManifest.getDescriptor().getDependencies()) {

			String currentGlobalIdentifier = currentDependency.getGlobalIdentifier();
			IApackManifest installedManifest = installedManifests.getOrDefault(currentGlobalIdentifier, null);
			if (installedManifest == null) {
				// Dependency is not installed -> We need to synchronize it
				currentDependency.setRequiresLink(true);
				currentDependency.setRequiresPull(true);
				currentDependency.setSyncMessage(Messages.AbapGitWizardPageBranchAndPackage_apack_sync_text_not_yet_installed, DialogPage.INFORMATION);
			} else if (currentDependency.getVersion().isVersionCompatible(installedManifest.getDescriptor().getVersion())
					&& currentDependency.getTargetPackage().getPackageName() != null) {
				currentDependency.setRequiresLink(false);
				currentDependency.setRequiresPull(false);
				currentDependency.setSyncMessage(Messages.AbapGitWizardPageBranchAndPackage_apack_sync_text_already_installed, DialogPage.INFORMATION);
			} else {
				IApackManifest gitManifest = retrievedGitManifests.stream()
						.filter(manifest -> manifest.getDescriptor().getGlobalIdentifer().equals(currentDependency.getGlobalIdentifier()))
						.findFirst().orElse(null);
				if (gitManifest != null
						&& currentDependency.getVersion().isVersionCompatible(gitManifest.getDescriptor().getVersion())) {
					currentDependency.setRequiresLink(false);
					currentDependency.setRequiresPull(true);
					currentDependency.setSyncMessage(Messages.AbapGitWizardPageBranchAndPackage_apack_sync_text_version_update,
							DialogPage.INFORMATION);
				} else {
					String currentVersion = "n/a"; //$NON-NLS-1$
					if (gitManifest != null && gitManifest.getDescriptor() != null
							&& gitManifest.getDescriptor().getVersion() != null) {
						currentVersion = gitManifest.getDescriptor().getVersion();
					}
					currentDependency
							.setSyncMessage(NLS.bind(Messages.AbapGitWizardPageBranchAndPackage_apack_sync_text_installation_not_possible,
									currentDependency.getVersion().toString(), currentVersion),
							DialogPage.ERROR);
					AbapGitWizardPageBranchAndPackage.this.cloneData.apackManifest
							.setSyncMessage(Messages.AbapGitWizardPageBranchAndPackage_apack_sync_text_wizard_error, DialogPage.ERROR);
				}
			}
		}


	}

	private static class ApackParameters {

		public String groupId;
		public String artifactId;
		public String url;
		public String branch;

		public static ApackParameters createFromDependency(IApackDependency dependency) {
			ApackParameters apackParameters = new ApackParameters();
			apackParameters.groupId = dependency.getGroupId();
			apackParameters.artifactId = dependency.getArtifactId();
			apackParameters.url = dependency.getGitUrl();
			apackParameters.branch = IApackManifest.MASTER_BRANCH;
			return apackParameters;
		}

	}

}
