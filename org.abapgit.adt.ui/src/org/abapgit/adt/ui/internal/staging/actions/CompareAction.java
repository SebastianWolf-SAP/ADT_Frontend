package org.abapgit.adt.ui.internal.staging.actions;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;

import org.abapgit.adt.backend.FileServiceFactory;
import org.abapgit.adt.backend.IFileService;
import org.abapgit.adt.backend.model.abapgitstaging.IAbapGitFile;
import org.abapgit.adt.backend.model.abapgitstaging.IAbapGitObject;
import org.abapgit.adt.ui.AbapGitUIPlugin;
import org.abapgit.adt.ui.internal.i18n.Messages;
import org.abapgit.adt.ui.internal.staging.AbapGitStagingView;
import org.abapgit.adt.ui.internal.staging.compare.AbapGitCompareInput;
import org.abapgit.adt.ui.internal.staging.compare.AbapGitCompareItem;
import org.abapgit.adt.ui.internal.staging.util.AbapGitStagingService;
import org.abapgit.adt.ui.internal.staging.util.IAbapGitStagingService;
import org.eclipse.compare.CompareUI;
import org.eclipse.compare.ITypedElement;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.actions.BaseSelectionListenerAction;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import com.sap.adt.tools.abapsource.AbapSource;
import com.sap.adt.tools.abapsource.IAdtObjectLoader;
import com.sap.adt.tools.core.urimapping.AdtUriMappingServiceFactory;
import com.sap.adt.tools.core.urimapping.UriMappingContext;

@SuppressWarnings("restriction")
public class CompareAction extends BaseSelectionListenerAction {
	private final TreeViewer treeViewer;
	private final IAbapGitStagingService stagingService;
	private IFileService fileService;
	private final AbapGitStagingView view;

	public CompareAction(AbapGitStagingView view, TreeViewer treeViewer) {
		super(Messages.AbapGitStaging_action_compare);

		this.treeViewer = treeViewer;
		this.stagingService = AbapGitStagingService.getInstance();
		this.fileService = getFileService();
		this.view = view;

		setToolTipText(Messages.AbapGitStaging_action_compare);
		setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(AbapGitUIPlugin.PLUGIN_ID, "icons/etool/compare_view.png")); //$NON-NLS-1$
		setEnabled(true);
	}

	@Override
	public void run() {
		//compare files
		compareAbapGitFiles();
	}

	private void compareAbapGitFiles() {
		Set<String> compareInputFiles = new HashSet<>(); //maintain a list for handling duplicate file compare inputs
		IStructuredSelection selection = (IStructuredSelection) this.treeViewer.getSelection();
		for (Object object : selection.toList()) {
			if (object instanceof IAbapGitObject) { // if selection is on an abap object, create compare inputs for all the files inside the selected object
				IAbapGitObject abapObject = (IAbapGitObject) object;
				for (IAbapGitFile file : abapObject.getFiles()) {
					if (!compareInputFiles.contains(file.getName())) { //check if the file is already added to the comparison list
						compareInputFiles.add(file.getName());
						openFileCompareEditor(file, abapObject); //open compare editor
					}
				}
			} else if (object instanceof IAbapGitFile) { // create compare input for the selected file
				IAbapGitFile file = (IAbapGitFile) object;
				if (!compareInputFiles.contains(file.getName())) { //check if the file is already added to the comparison list
					compareInputFiles.add(file.getName());
					openFileCompareEditor(file, (IAbapGitObject) file.eContainer()); //open compare editor
				}
			}
		}
	}

	private void openFileCompareEditor(IAbapGitFile file, IAbapGitObject abapObject) {
		if (!checkCompareActionSupported(file, abapObject)) {
			MessageDialog.openInformation(Display.getDefault().getActiveShell(),
					Messages.AbapGitStaging_compare_objects_dialog_title,
					NLS.bind(Messages.AbapGitStaging_compare_not_supported_xmg, file.getName()));
			return;
		}
		Job openFileComparisonEditor = new Job(Messages.AbapGitStaging_compare_job_title) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					String leftFileContents = CompareAction.this.fileService.readLocalFileContents(file,
							CompareAction.this.stagingService.getDestination(CompareAction.this.view.getProject()));
					String rightFileContents = CompareAction.this.fileService.readRemoteFileContents(file,
							CompareAction.this.stagingService.getDestination(CompareAction.this.view.getProject()));
					AbapGitCompareItem left = new AbapGitCompareItem(getFileNameLocal(file), getFileExtension(file, abapObject),
							leftFileContents);
					AbapGitCompareItem right = new AbapGitCompareItem(getFileNameRemote(file), getFileExtension(file, abapObject),
							rightFileContents);
					if (isContentIdentical(left.getContents(), right.getContents())) {
						Display.getDefault().syncExec(() -> {
							MessageDialog.openInformation(Display.getDefault().getActiveShell(),
									Messages.AbapGitStaging_compare_objects_dialog_title,
									NLS.bind(Messages.AbapGitStaging_compare_objects_identical_xmsg, file.getName()));
						});
						return Status.OK_STATUS;
					}
					AbapGitCompareInput compareInput = new AbapGitCompareInput(left, right, abapObject.getName(),
							CompareAction.this.view.getProject());
					Display.getDefault().asyncExec(() -> CompareUI.openCompareEditor(compareInput));
					return Status.OK_STATUS;
				} catch (IOException | CoreException e) {
					return Status.CANCEL_STATUS;
				}
			}
		};
		openFileComparisonEditor.schedule();
	}

	private String getFileNameLocal(IAbapGitFile file) {
		return (file.getName() + " (Local)"); //$NON-NLS-1$
	}

	private String getFileNameRemote(IAbapGitFile file) {
		return (file.getName() + " (Remote)"); //$NON-NLS-1$
	}

	private boolean checkCompareActionSupported(IAbapGitFile file, IAbapGitObject abapObject) {
		//check if parent node is non-code meta files
		if (abapObject.getType() == null) {
			if (!checkIfPropertiesFile(file)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Returns the file extension registered in SFS for the given abap object
	 * for opening the right compare editor. For properties file the default
	 * extension (txt) is returned for opening the text compare editor. </br>
	 * </br>
	 * eg: If the selection is on an abap class file, the method returns the the
	 * SFS file extension registered for the abap class in ADT to open the abap
	 * compare editor. If the selection is on an xml file, the methods returns
	 * the text editor extension (txt) to open a text compare editor.
	 *
	 * @param file
	 *            File to compare
	 * @param abapObject
	 *            Parent abap object of the selected file
	 * @return File extension for the compare editor
	 */
	private String getFileExtension(IAbapGitFile file, IAbapGitObject abapObject) {
		if (isSourceCodeFile(file)) {
			IFile objectFile = (IFile) AdtUriMappingServiceFactory.createUriMappingService().getPlatformResource(
					new UriMappingContext(this.view.getProject()), URI.create(abapObject.getUri()));
			if (objectFile != null) {
				IAdtObjectLoader adtObjectLoader = AbapSource.getInstance().getAdtObjectLoader(objectFile);
				if (adtObjectLoader != null) {
					final IFile propsFile = adtObjectLoader.getLifecycleInfoFile(objectFile);
					if (propsFile != null) {
						final IFile sourceFile = adtObjectLoader.getSourceFile(propsFile);
						if (sourceFile != null) {
							return sourceFile.getFileExtension();
						}
					}
				}
			}
		}
		return ITypedElement.TEXT_TYPE;
	}

	/**
	 * Checks if the selection is on a source file and not a properties file
	 * Source file will have the extension <i>.abap</i> and the properties file
	 * will have the extension <i>.xml</i>
	 *
	 * @return true if the selection is a source file
	 */
	private boolean isSourceCodeFile(IAbapGitFile file) {
		if (checkIfPropertiesFile(file)) {
			return false;
		}
		return true;
	}

	private boolean checkIfPropertiesFile(IAbapGitFile file) {
		String[] tokens = file.getName().split("\\."); //$NON-NLS-1$
		String fileType = tokens[tokens.length - 1];
		if (fileType.equals("xml")) { //$NON-NLS-1$
			return true;
		}
		return false;
	}

	/**
	 * check whether two source objects are identical
	 *
	 * @param isLeft
	 *            InputStream object of content
	 * @param isRight
	 *            InputStream object of content
	 * @return true, when their contents are identical
	 * @throws IOException
	 */
	private boolean isContentIdentical(InputStream isLeft, InputStream isRight) throws IOException {
		int sizeLeft = isLeft.available();
		int sizeRight = isRight.available();

		if (sizeLeft != sizeRight) {
			return false;
		}

		/*
		 *  FileUtil provides the toString method, but an ABAP source file could be very big,
		 *  direct using String could cause memory problem.
		 */
		BufferedReader rdLeft = new BufferedReader(new InputStreamReader(isLeft));
		BufferedReader rdRight = new BufferedReader(new InputStreamReader(isRight));

		String textLineLeft, textLineRight;
		while ((textLineLeft = rdLeft.readLine()) != null) {
			textLineRight = rdRight.readLine();
			if (textLineRight == null || textLineLeft.compareTo(textLineRight) != 0) {
				return false;
			}
		}
		return true;
	}

	/**
	 * @return the fileService
	 */
	private IFileService getFileService() {
		if (this.fileService == null) {
			return FileServiceFactory.createFileService();
		}
		return this.fileService;
	}

	//For Testing
	public void setFileService(IFileService fileService) {
		this.fileService = fileService;
	}
}
