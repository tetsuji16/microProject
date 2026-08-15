/*******************************************************************************
 * The contents of this file are subject to the Common Public Attribution License 
 * Version 1.0 (the "License"); you may not use this file except in compliance with 
 * the License. You may obtain a copy of the License at 
 * http://www.projectlibre.com/license . The License is based on the Mozilla Public 
 * License Version 1.1 but Sections 14 and 15 have been added to cover use of 
 * software over a computer network and provide for limited attribution for the 
 * Original Developer. In addition, Exhibit A has been modified to be consistent 
 * with Exhibit B. 
 *
 * Software distributed under the License is distributed on an "AS IS" basis, 
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for the 
 * specific language governing rights and limitations under the License. The 
 * Original Code is ProjectLibre. The Original Developer is the Initial Developer 
 * and is ProjectLibre Inc. All portions of the code written by ProjectLibre are 
 * Copyright (c) 2012-2019. All Rights Reserved. All portions of the code written by 
 * ProjectLibre are Copyright (c) 2012-2019. All Rights Reserved. Contributor 
 * ProjectLibre, Inc.
 *
 * Alternatively, the contents of this file may be used under the terms of the 
 * ProjectLibre End-User License Agreement (the ProjectLibre License) in which case 
 * the provisions of the ProjectLibre License are applicable instead of those above. 
 * If you wish to allow use of your version of this file only under the terms of the 
 * ProjectLibre License and not to allow others to use your version of this file 
 * under the CPAL, indicate your decision by deleting the provisions above and 
 * replace them with the notice and other provisions required by the ProjectLibre 
 * License. If you do not delete the provisions above, a recipient may use your 
 * version of this file under either the CPAL or the ProjectLibre Licenses. 
 *
 *
 * [NOTE: The text of this Exhibit A may differ slightly from the text of the notices 
 * in the Source Code files of the Original Code. You should use the text of this 
 * Exhibit A rather than the text found in the Original Code Source Code for Your 
 * Modifications.] 
 *
 * EXHIBIT B. Attribution Information for ProjectLibre required
 *
 * Attribution Copyright Notice: Copyright (c) 2012-2019, ProjectLibre, Inc.
 * Attribution Phrase (not exceeding 10 words): 
 * ProjectLibre, open source project management software.
 * Attribution URL: http://www.projectlibre.com
 * Graphic Image as provided in the Covered Code as file: projectlibre-logo.png with 
 * alternatives listed on http://www.projectlibre.com/logo 
 *
 * Display of Attribution Information is required in Larger Works which are defined 
 * in the CPAL as a work which combines Covered Code or portions thereof with code 
 * not governed by the terms of the CPAL. However, in addition to the other notice 
 * obligations, all copies of the Covered Code in Executable and Source Code form 
 * distributed must, as a form of attribution of the original author, include on 
 * each user interface screen the "ProjectLibre" logo visible to all users. 
 * The ProjectLibre logo should be located horizontally aligned with the menu bar 
 * The 
 * logo must be at least 144 x 31 pixels. When users click on the "ProjectLibre" 
 * logo it must direct them back to http://www.projectlibre.com. 
 *******************************************************************************/
package com.microproject.pm.graphic.frames;
import java.awt.*;
import java.awt.desktop.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowStateListener;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.io.File;
import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.prefs.Preferences;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.LookAndFeel;
import javax.swing.JOptionPane;
import javax.swing.RootPaneContainer;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import com.microproject.menu.resource.MissingListenerException;
import org.projectlibre.strings.Strings;
import com.projectlibre.ui.shell.ProjectLibreShell;
import com.microproject.configuration.Configuration;
import com.microproject.configuration.FieldDictionary;
import com.microproject.configuration.Settings;
import com.microproject.application.ProjectDocumentWorkflow;
import com.microproject.application.ProjectLoadWorkflow;
import com.microproject.application.RecentProjectStore;
import com.microproject.collaboration.CollaborationMetadataStore;
import com.microproject.collaboration.CollaborationSession;
import com.microproject.collaboration.ProjectMergeService;
import com.microproject.contrib.ClassLoaderUtils;
import com.microproject.dialog.AboutDialog;
import com.microproject.dialog.AbstractDialog;
import com.microproject.dialog.BaselineDialog;
import com.microproject.dialog.FindDialog;
import com.microproject.dialog.HelpDialog;
import com.microproject.dialog.LocaleDialog;
import com.microproject.dialog.OpenProjectDialog;
import com.microproject.dialog.ProjectDialog;
import com.microproject.dialog.ProjectInformationDialog;
import com.microproject.dialog.RenameProjectDialog;
import com.microproject.dialog.ResourceInformationDialog;
import com.microproject.dialog.ResourceMappingDialog;
import com.microproject.dialog.TaskInformationDialog;
import com.microproject.dialog.WelcomeDialog;
import com.microproject.dialog.UsabilityStrings;
import com.microproject.dialog.assignment.AssignmentDialog;
import com.microproject.dialog.assignment.TimesheetDialog;
import com.microproject.dialog.options.CalendarDialogBox;
import com.microproject.document.Document;
import com.microproject.document.ObjectEvent;
import com.microproject.exchange.ResourceMappingForm;
import com.microproject.field.Field;
import com.microproject.graphic.configuration.SpreadSheetFieldArray;
import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.VoidNodeImpl;
import com.microproject.grouping.core.model.NodeModel;
import com.microproject.grouping.core.transform.ViewTransformer;
import com.microproject.grouping.core.transform.filtering.NodeFilter;
import com.microproject.grouping.core.transform.filtering.ResourceInTeamFilter;
import com.microproject.job.Job;
import com.microproject.job.JobQueue;
import com.microproject.job.JobRunnable;
import com.microproject.job.Mutex;
import com.microproject.menu.MenuActionConstants;
import com.microproject.menu.MenuActionsMap;
import com.microproject.menu.MenuManager;
import com.microproject.menu.ProjectMenuActionMap;
import com.microproject.options.CalendarOption;
import com.microproject.pm.assignment.Assignment;
import com.microproject.pm.dependency.DependencyService;
import com.microproject.pm.dependency.DependencyType;
import com.microproject.pm.graphic.IconManager;
import com.microproject.pm.graphic.TabbedNavigation;
import com.microproject.pm.graphic.collaboration.CollaborationHelper;
import com.microproject.pm.graphic.frames.workspace.DefaultFrameManager;
import com.microproject.pm.graphic.frames.workspace.FrameHolder;
import com.microproject.pm.graphic.frames.workspace.FrameManager;
import com.microproject.pm.graphic.frames.workspace.NamedFrame;
import com.microproject.pm.graphic.frames.workspace.NamedFrameEvent;
import com.microproject.pm.graphic.frames.workspace.NamedFrameListener;
import com.microproject.pm.graphic.laf.LafManager;
import com.microproject.pm.graphic.spreadsheet.SpreadSheet;
import com.microproject.pm.graphic.spreadsheet.common.CommonSpreadSheet;
import com.microproject.pm.graphic.spreadsheet.selection.event.SelectionNodeEvent;
import com.microproject.pm.graphic.spreadsheet.selection.event.SelectionNodeListener;
import com.microproject.pm.graphic.views.BaseView;
import com.microproject.pm.graphic.views.GanttView;
import com.microproject.pm.graphic.views.ProjectsDialog;
import com.microproject.pm.graphic.views.Searchable;
import com.microproject.pm.resource.Resource;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.ProjectFactory;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.SubProj;
import com.microproject.pm.task.Task;
import com.microproject.pm.time.HasStartAndEnd;
import com.microproject.preference.ConfigurationFile;
import com.microproject.preference.GlobalPreferences;
import com.microproject.print.GraphPageable;
import com.microproject.print.PrintDocumentFactory;
import com.microproject.server.data.DocumentData;
import com.microproject.session.CreateOptions;
import com.microproject.session.LoadOptions;
import com.microproject.session.LocalSession;
import com.microproject.session.FileHelper;
import com.microproject.session.SaveOptions;
import com.microproject.session.Session;
import com.microproject.session.SessionFactory;
import com.microproject.strings.Messages;
import com.microproject.toolbar.FilterToolBarManager;
import com.microproject.toolbar.TransformComboBox;
import com.microproject.toolbar.TransformComboBoxModel;
import com.microproject.undo.CommandInfo;
import com.microproject.undo.UndoController;
import com.microproject.util.Alert;
import com.microproject.util.BrowserControl;
import com.microproject.util.ClassUtils;
import com.microproject.util.Environment;
import com.microproject.util.FlatUiSupport;
import com.microproject.util.SafeObjectInput;
import com.microproject.util.PopupDialogSupport;
import com.microproject.util.UiLinkTargets;
import com.microproject.workspace.SavableToWorkspace;
import com.microproject.workspace.WorkspaceSetting;



/**
 *
 */
public class GraphicManager implements  FrameHolder, NamedFrameListener, WindowStateListener,  SelectionNodeListener, ObjectEvent.Listener, ProjectMenuActionMap, MenuActionConstants, SavableToWorkspace {
	private static final Logger logger = Logger.getLogger(GraphicManager.class.getName());
	private static final boolean BINARY_WORKSPACE = true;
	private static GraphicManager lastGraphicManager = null; // used when displaying a popup but the frame isn't known
    private DocumentFrame currentFrame = null;
    private List frameList=new ArrayList();
    private HashMap<Project,NamedFrame> frameMap = new HashMap<Project, NamedFrame>();
//    private JFileChooser fileChooser = null;

	private NamedFrame viewBarFrame;
	private FrameManager frameManager;

	private MenuManager menuManager;
	MenuActionsMap actionsMap = null;
	//private String[] projectUrl;
	private static String server = null;

    private AssignmentDialog assignResourcesDialog = null;
    private FindDialog findDialog = null;
	private ProjectInformationDialog projectInformationDialog = null;
	private TaskInformationDialog taskInformationDialog = null;
	private ResourceInformationDialog resourceInformationDialog = null;
    private AboutDialog aboutDialog = null;
    private HelpDialog helpDialog = null;
    private BaselineDialog baselineDialog = null;
    private ResourceMappingDialog resourceMappingDialog=null;
	ProjectFactory projectFactory = null;
	private final AutoRecoveryManager autoRecoveryManager;
	private final RecentProjectStore recentProjectStore = new RecentProjectStore();
	private volatile boolean quitting;
	protected Container container;
	protected Frame frame;
	TabbedNavigation topTabs = null;

	private static Object lastWorkspace = null; // static required - used for copying current workspace to new instance
	static LinkedList graphicManagers = new LinkedList();
    private static LafManager lafManager;
	public static boolean badLAF = false;
	private StartupFactory startupFactory = null;
	protected JobQueue jobQueue=null;

	protected GlobalPreferences preferences=null;
	private FilterToolBarManager filterToolBarManager;
	private JMenu projectListMenu = null;

	private ArrayList<CommandInfo> history=new ArrayList<CommandInfo>();

	/** determines the parent graphic manager for a component
	 *
	 * @param component
	 * @return
	 */
	public static GraphicManager getInstance(Component component){
		Component c = component;
		for (c = component; c != null; c = c.getParent()) {
			if (c instanceof FrameHolder)
				return ((FrameHolder)c).getGraphicManager();
			else if (c.getName() != null && c.getName().endsWith("BootstrapApplet") && c.getClass().getName().endsWith("BootstrapApplet")){
				logger.fine("applet: " + c.getClass().getName());
				try {
					FrameHolder holder=(FrameHolder)Class.forName("com.microproject.bootstrap.BootstrapApplet.class").getMethod("getObject", new Class[0]).invoke(c, new Object[0]);
					return holder.getGraphicManager();
				} catch (Exception e) {
					return null;
				}
			}
		}
		return lastGraphicManager; // if none found, use last used one
	}
	public static GraphicManager getInstance(){
//System.out.println("Graphic manager getInstance = " + lastGraphicManager.hashCode());
		return lastGraphicManager;
	}

	public static Frame getFrameInstance(){
		return lastGraphicManager.getFrame();
	}

	public static DocumentFrame getDocumentFrameInstance(){
		return lastGraphicManager==null?null:lastGraphicManager.getCurrentFrame();
	}

	void setMeAsLastGraphicManager() { // makes this the current graphic manager for job queue and dialogs
		lastGraphicManager = this;
		if (jobQueue != null)
			SessionFactory.getInstance().setJobQueue(getJobQueue());

	}



	public static LinkedList getGraphicManagers() {
		return graphicManagers;
	}

	/**
	 * @param projectUrl project URL
	 * @param server server name
	 * @throws java.awt.HeadlessException
	 */
	public GraphicManager(/*String[] projectUrl,*/ String server,Container container) throws HeadlessException {
		graphicManagers.add(this);
		lastGraphicManager = this;
		container.addFocusListener(new FocusListener() {

			public void focusGained(FocusEvent e) {
//				System.out.println("GainFocus " + GraphicManager.this.hashCode());
				setMeAsLastGraphicManager();
			}

			public void focusLost(FocusEvent e) {
//				System.out.println("LostFocus " + GraphicManager.this.hashCode());
			}});

		projectFactory = ProjectFactory.getInstance();
		autoRecoveryManager = new AutoRecoveryManager(projectFactory, this);
		projectFactory.getPortfolio().addObjectListener(this);

		//this.projectUrl = projectUrl;
		GraphicManager.server = server;
		this.container=container;
		if (container instanceof Frame)
			frame=(Frame)container;
//		else if (container instanceof JApplet)
//			frame = JOptionPane.getFrameForComponent(container);
		if (container instanceof FrameHolder)
			((FrameHolder)container).setGraphicManager(this);
//		else if (container instanceof BootstrapApplet){
		else{
			try {
				FrameHolder holder=(FrameHolder)Class.forName("com.microproject.bootstrap.BootstrapApplet").getMethod("getObject", new Class[0]).invoke(container, new Object[0]);
				holder.setGraphicManager(this);
			} catch (Exception e) {
			}
		}
		registerForMacOSXEvents();
	}
	public GraphicManager(Container container) {
		this(/*null,*/ server,container);
	}

	public void cleanUp() {
		autoRecoveryManager.stop();

//		On quitting, a sleep interrupted exception (below) is thrown by Substance. Without changing the source
//		java.lang.InterruptedException: sleep interrupted
//		at java.lang.Thread.sleep(Native Method)
//		at org.jvnet.substance.utils.FadeTracker$FadeTrackerThread.run(FadeTracker.java:210)
//		I have submitted a bug report: https://substance.dev.java.net/issues/show_bug.cgi?id=155 with a proposed fix

		projectFactory.getPortfolio().removeObjectListener(this);
		((DefaultFrameManager)frameManager).cleanUp();
		graphicManagers.remove(this);
		if (graphicManagers.isEmpty())
			getLafManager().clean();

		if (jobQueue != null)
			jobQueue.cancel();
		jobQueue = null;
	}

	public LafManager getLafManager(){
		if (lafManager==null){
			try {
				String lafName=Messages.getMetaString("LafManager");
				lafManager=(LafManager)Class.forName(lafName).getConstructor(new Class[]{GraphicManager.class}).newInstance(new Object[]{this});
			} catch (IllegalArgumentException e) {
				logger.log(Level.WARNING, "Failed to instantiate LAF manager", e);
			} catch (SecurityException e) {
				logger.log(Level.WARNING, "Failed to instantiate LAF manager", e);
			} catch (InstantiationException e) {
				logger.log(Level.WARNING, "Failed to instantiate LAF manager", e);
			} catch (IllegalAccessException e) {
				logger.log(Level.WARNING, "Failed to instantiate LAF manager", e);
			} catch (InvocationTargetException e) {
				logger.log(Level.WARNING, "Failed to instantiate LAF manager", e);
			} catch (NoSuchMethodException e) {
				logger.log(Level.WARNING, "Failed to instantiate LAF manager", e);
			} catch (ClassNotFoundException e) {
				logger.log(Level.WARNING, "Failed to instantiate LAF manager", e);
			}
			//lafManager=new LafManager(this);
		}
		return lafManager;
	}



	private String fileName = "../projectlibre_exchange/testdata/New Product.mpp"; //$NON-NLS-1$
	private ViewAction resourceAction;

    private static String getTabIdForProject(Project project) {
    	if (project == null)
    		return null;
    	return "" + project.getUniqueId(); //see later //$NON-NLS-1$
    }

    void setTabNameAndTitle(DocumentFrame frame, Project project) {
    	frame.setTabNameAndTitle(project);
    }


    public void switchToProject(long projectId) {
    	Project project = ProjectFactory.getInstance().findFromId(projectId);
    	if (project == null)
    		return;
    	DocumentFrame f = (DocumentFrame) frameMap.get(project);
    	if (f == null)
    		return;
    	setCurrentFrame(f);

    }
	public DocumentFrame getFrameForProject(Project project) {
		return (DocumentFrame) frameMap.get(project);
	}
	protected void setCurrentFrame(DocumentFrame frame){
		if (frame instanceof DocumentFrame) {
			if (currentFrame != null && projectListMenu != null&&!Environment.isPlugin()) {
				currentFrame.getMenuItem().setSelected(false);
			}

			if (currentFrame != null&&!Environment.isPlugin())
				currentFrame.refreshViewButtons(false); // disable buttons for old view

			currentFrame = (DocumentFrame)frame;
			if (projectListMenu != null&&!Environment.isPlugin()) {
				currentFrame.getMenuItem().setSelected(true);
			}
			if (topTabs != null&&!Environment.isPlugin()) {
				topTabs.setCurrentFrame(currentFrame);
			}
			DocumentSelectedEvent.fire(this,currentFrame);
			if (projectInformationDialog != null)
				projectInformationDialog.documentSelected(new DocumentSelectedEvent(this,currentFrame));
			if (taskInformationDialog != null)
				taskInformationDialog.documentSelected(new DocumentSelectedEvent(this,currentFrame));
			if (resourceInformationDialog != null)
				resourceInformationDialog.documentSelected(new DocumentSelectedEvent(this,currentFrame));

			setTitle(false);
			if (currentFrame != null)
				currentFrame.refreshViewButtons(true);

			getFrameManager().activateFrame(currentFrame); // need to force activation in case being activated by closing another
			if(!Environment.isPlugin()){
				setEnabledDocumentMenuActions(currentFrame!=null);
				setButtonState(null,currentFrame.getProject());
			}
			if (currentFrame != null && currentFrame.getProject() != null) {
				if (!Environment.isPlugin()) currentFrame.getFilterToolBarManager().transformBasedOnValue();
				CalendarOption calendarOption = currentFrame.getProject().getCalendarOption();

				if (calendarOption != null) {
					CalendarOption.setInstance(calendarOption);
				}
			} else {
				CalendarOption.setInstance(CalendarOption.getDefaultInstance());
			}
		}
	}

	void setTitle(boolean isSaving) {
		DocumentFrame dframe = getCurrentFrame();
		String title=Messages.getContextString("Text.ApplicationTitle"); //$NON-NLS-1$
		if (dframe != null && dframe.getProject() != null) {
			if (Environment.getStandAlone()) title=dframe.getProject().getTitle();
			else title += " - " + dframe.getProject().getName(); //$NON-NLS-1$
			if (!isSaving && dframe.getProject().needsSaving())
				title += " *"; // modified; //$NON-NLS-1$
		}
		Frame f=getFrame();
		if (frame!=null) frame.setTitle(title);

	}
    /**
	 * Adds a new document frame and shows it
	 * @param project
	 * @return
	 */

	public DocumentFrame addProjectFrame(final Project project) {
		String tabId = getTabIdForProject(project);
		if (project == null) // in case of out of memory error
			return null;
		final DocumentFrame frame = new DocumentFrame(this,project,tabId);
		if (frame == null) // in case of out memory error
			return null;
		getFrameManager().addFrame(frame);
//		DocumentFrame newDocumentFrame = (DocumentFrame)getFrameManager().getFrame(tabId);

		setTabNameAndTitle(frame,project);
		frame.setShowTitleBar(false);
		getFrameManager().showFrame(frame); // show the frame
		frame.addNamedFrameListener(this); // main frame listens to changes in selection

		project.addProjectListener(frame);


		if (projectListMenu != null) {
			JRadioButtonMenuItem mi = new JRadioButtonMenuItem(new SelectDocumentAction(frame));
			mi.setSelected(true);
			frame.setMenuItem(mi);
			projectListMenu.add(mi);
		}
		setCurrentFrame(frame);

		frameList.add(frame);
		frameMap.put(project, frame);
		if (project.getFileName() != null) recentProjectStore.recordOpened(project.getFileName());
		updateStoredSession();

		// clear filter/grouping/sort for newly opened or created project
		if (!Environment.isPlugin()) SwingUtilities.invokeLater( new Runnable() {
			public void run() {
			 	frame.getFilterToolBarManager().clear();
			}});
		getMenuManager().setActionEnabled(ACTION_OPEN_PROJECT,frame==null || !frame.isEditingResourcePool()); //resource pool can not be opened at same time as another proj
		getMenuManager().setActionEnabled(ACTION_RECENT_PROJECTS,frame==null || !frame.isEditingResourcePool());
		return frame;
	}

	private String getCollaborationUserKey() {
		String user = System.getProperty("user.name");
		if (user == null || user.trim().length() == 0) {
			return "unknown";
		}
		return user.trim();
	}

	private void initializeCollaboration(Project project) {
		if (project == null) {
			return;
		}
		String fileName = project.getFileName();
		if (!CollaborationMetadataStore.isCollaborationCandidate(fileName)) {
			return;
		}
		if (project.getCollaborationSession() != null) {
			DocumentFrame frame = getFrameForProject(project);
			if (frame != null && project.getCollaborationWorkspace() != null) {
				frame.restoreWorkspace(project.getCollaborationWorkspace(), SavableToWorkspace.VIEW);
				project.setCollaborationWorkspace(null);
			}
			return;
		}
		CollaborationSession session = CollaborationSession.create(project, fileName, getCollaborationUserKey());
		if (session == null) {
			return;
		}
		try {
			session.setExternalReloadHandler(new CollaborationSession.ExternalProjectReloadHandler() {
				public void reload(Project changedProject) {
					refreshProjectFromExternalFile(changedProject);
				}
			});
			project.setCollaborationSession(session);
			project.setCollaborationWorkspace(session.loadWorkspace());
			session.start();
		} catch (RuntimeException e) {
			logger.log(java.util.logging.Level.WARNING, "Collaboration could not be initialized for " + fileName, e);
			project.setCollaborationSession(null);
			project.setCollaborationWorkspace(null);
			return;
		}
		DocumentFrame frame = getFrameForProject(project);
		if (frame != null && project.getCollaborationWorkspace() != null) {
			frame.restoreWorkspace(project.getCollaborationWorkspace(), SavableToWorkspace.VIEW);
			project.setCollaborationWorkspace(null);
		}
	}

	private void persistCollaborationWorkspace(Project project) {
		if (project == null) {
			return;
		}
		CollaborationSession session = project.getCollaborationSession();
		if (session == null) {
			return;
		}
		DocumentFrame frame = getFrameForProject(project);
		if (frame != null) {
			WorkspaceSetting workspace = frame.createWorkspace(SavableToWorkspace.VIEW);
			project.setCollaborationWorkspace(workspace);
			session.saveWorkspace(workspace);
		}
	}

	private void refreshProjectFromExternalFile(Project project) {
		if (project == null || project.getFileName() == null) {
			return;
		}
		CollaborationSession session = project.getCollaborationSession();
		if (!isActiveProject(project)) {
			return;
		}
		String fileName = project.getFileName();
		ProjectMergeService.ApplyResult result = new ProjectMergeService().applyExternalTaskUpdates(project, fileName,
			session == null ? null : session.getLocalLocks());
		if (session != null) {
			session.afterExternalProjectRefresh();
		}
		if (result.hasChanges()) {
			repaintProject(project);
		}
	}

	private boolean isActiveProject(Project project) {
		DocumentFrame current = getCurrentFrame();
		return current != null && project.equals(current.getProject());
	}

	private void repaintProject(Project project) {
		DocumentFrame frame = getFrameForProject(project);
		if (frame != null) {
			frame.repaint();
		}
	}

	private void closeProjectFrame(Project project) {
		String tabId = getTabIdForProject(project);
		DocumentFrame frame = (DocumentFrame) frameMap.get(project);
		if (frame!=null){

			if (currentFrame == frame){
		    	frame.setVisible(false);
				JMenuItem mi = frame.getMenuItem();
				if (mi != null && projectListMenu != null)
					projectListMenu.remove(mi);

			    if (frameList.size()<=1) {
			    	frame.refreshViewButtons(false); // disable old buttons
			    	currentFrame=null;
			    	setTitle(false);
			        setEnabledDocumentMenuActions(false);
			    } else{
			        DocumentFrame current;
			        int index=0;
			        for (Iterator i=frameList.iterator();i.hasNext();index++){
			            current=(DocumentFrame)i.next();
			            if (tabId.equals(getTabIdForProject(current.getProject())))
			                break;
			        }
					setCurrentFrame((DocumentFrame)frameList.get((index==0)?1:index-1));
			    }
			}
			project.removeProjectListener(frame);
			frame.removeNamedFrameListener(this); // main frame listens to changes in selection

			getFrameManager().removeFrame(frame);
			frame.onClose();
			frameList.remove(frame);
			frameMap.remove(project);


		}
		if (!quitting) updateStoredSession();
		setAllButResourceDisabled(false);
		getMenuManager().setActionEnabled(ACTION_OPEN_PROJECT,true); // no matter what, you can open a project after closing, since if you closed resource pool you can open after
		getMenuManager().setActionEnabled(ACTION_RECENT_PROJECTS,true);
	}
	public String doRenameProjectDialog(String name,Set projectNames,boolean saveAs) {
		finishAnyOperations();
		RenameProjectDialog renameProjectDialog = RenameProjectDialog.getInstance(getFrame(),null);
		renameProjectDialog.getForm().setName(name);
		renameProjectDialog.getForm().setProjectNames(projectNames);
		renameProjectDialog.getForm().setSaveAs(saveAs);
		if (renameProjectDialog.doModal())
			return renameProjectDialog.getForm().getName();
		return null;
	}

	public void doWelcomeDialog() {
		//claur, for test purpose to preload a project
		String preloadProject=ConfigurationFile.getProperty("preLoadProject"); 
		if (preloadProject!=null){
			loadLocalDocument(preloadProject,false);
			return;
		}

		showWelcomeDialog(false);
	}

	public void doRecentProjectsDialog() {
		showWelcomeDialog(true);
	}

	private void showWelcomeDialog(boolean focusRecentProjects) {
		WelcomeDialog instance = focusRecentProjects
			? WelcomeDialog.getRecentProjectsInstance(getFrame(),getMenuManager())
			: WelcomeDialog.getInstance(getFrame(),getMenuManager());
		if (instance.doModal()) {
			waitInitialization();
			if (instance.getForm().isCreateProject())
				doNewProjectDialog();
			else if (instance.getForm().isOpenProject()){
				if(Environment.getStandAlone()) openLocalProject();
				else doOpenProjectDialog();
			}else if (instance.getForm().isManageResources()) {
				loadMasterProject();
			}else if (instance.getForm().getRecentPath() != null) {
				loadLocalDocument(instance.getForm().getRecentPath(), false);
			}else if (instance.getForm().getTemplateId() != null) {
				createProjectFromTemplate(instance.getForm().getTemplateId());
			}
		}
	}

	private void createProjectFromTemplate(String templateId) {
		Project project = projectFactory.createProject();
		if (project == null) return;
		String[][] definitions = switch (templateId) {
			case "software" -> new String[][] { {"Plan backlog", "2"}, {"Architecture", "3"}, {"Implementation", "10"}, {"Test", "5"}, {"Release", "0"} };
			case "construction" -> new String[][] { {"Site preparation", "5"}, {"Foundation", "8"}, {"Structure", "15"}, {"Utilities", "8"}, {"Inspection", "0"} };
			default -> new String[][] { {"Planning", "2"}, {"Execution", "5"}, {"Review", "2"}, {"Complete", "0"} };
		};
		project.setName(switch (templateId) { case "software" -> "Software Delivery"; case "construction" -> "Construction Project"; default -> "New Project"; });
		long day = CalendarOption.getInstance().getMillisPerDay();
		NormalTask previous = null;
		for (String[] definition : definitions) {
			NormalTask task = (NormalTask) project.createLocalTaskNode(null).getImpl();
			task.setName(definition[0]);
			task.getCurrentSchedule().setStart(project.getStart()); task.setDuration(day * Long.parseLong(definition[1]));
			if ("0".equals(definition[1])) task.setMarkTaskAsMilestone(true);
			if (previous != null) try { DependencyService.getInstance().newDependency(previous, task, DependencyType.FS, 0L, this); } catch (Exception ignored) { }
			previous = task;
		}
		project.setDirty(true); project.recalculate();
	}

	public boolean restorePreviousSessionAtStartup() {
		if (!recentProjectStore.isRestoreSessionEnabled()) return false;
		List<java.nio.file.Path> files = recentProjectStore.session();
		if (files.isEmpty()) return false;
		int choice = PopupDialogSupport.showConfirmDialog(getFrame(), java.text.MessageFormat.format(UsabilityStrings.text("session.prompt"), files.size()), UsabilityStrings.text("session.title"), JOptionPane.YES_NO_OPTION);
		if (choice != JOptionPane.YES_OPTION) return false;
		boolean opened = false;
		for (java.nio.file.Path file : files) opened |= loadLocalDocument(file.toString(), false);
		return opened;
	}

	private void updateStoredSession() {
		List<String> files = new ArrayList<>();
		for (Object value : frameList) {
			if (value instanceof DocumentFrame documentFrame && documentFrame.getProject().getFileName() != null)
				files.add(documentFrame.getProject().getFileName());
		}
		recentProjectStore.saveSession(files);
	}

	public boolean doNewProjectDialog() {
		ProjectDialog.Form form=doNewProjectDialog1();
		if (form==null) return false;
		else return doNewProjectDialog2(form);
	}
	public boolean doNewProjectNoDialog(HashMap opts) {
		ProjectDialog.Form form=doNewProjectNoDialog1();
		if (form==null) return false;
		if (opts!=null){
			Consumer<Object> updateViewClosure=(Consumer<Object>)opts.get("updateViewClosure");
			if (updateViewClosure!=null) updateViewClosure.accept(form);
		}
		return doNewProjectDialog2(form);
	}
	public ProjectDialog.Form doNewProjectDialog1() {
		addHistory("doNewProjectDialog");
		finishAnyOperations();
		ProjectDialog projectDialog = ProjectDialog.getInstance(getFrame(),null);
		projectDialog.getForm().setManager(Environment.getUser().getName());
		if (!projectDialog.doModal())
			return null; // if cancelled
		return projectDialog.getForm();
	}
	protected static int project_suffix_count=1;
//	protected static ProjectDialog.Form lastNewProjectForm;
//	public ProjectDialog.Form getLastNewProjectForm() {
//		return lastNewProjectForm;
//	}
	public ProjectDialog.Form doNewProjectNoDialog1() {
		logger.fine("doNewProjectNoDialog1 begin");
		addHistory("doNewProjectNoDialog");
		finishAnyOperations();
		ProjectDialog.Form form=new ProjectDialog.Form();
		form.setName("Project"+(project_suffix_count++));
//		lastNewProjectForm=form;
		logger.fine("doNewProjectNoDialog1 end");
		return form;
	}
	public boolean doNewProjectDialog2(ProjectDialog.Form form) {
		showWaitCursor(true);
		ResourcePool resourcePool=form.getResourcePool();
		boolean local=form.isLocal();
		if (resourcePool!=null) resourcePool.setLocal(local);
		CreateOptions opt=new CreateOptions();
		opt.setResourcePool(form.getResourcePool());
		opt.setLocal(local);
		opt.setName(form.getName());
		opt.setAddResources(!local);
		Project project = projectFactory.createProject(opt);
		try {
			//createProject above might make a new resource pool, so make sur it is used when copying properties
			//projectDialog.getForm().setResourcePool(project.getResourcePool());

			project.setManager(form.getManager());
			project.setName(form.getName());
			project.setNotes(form.getNotes());
			project.setForward(form.isForward());
			project.setGroup(form.getGroup());
			project.setDivision(form.getDivision());
			project.setProjectType(form.getProjectType());
			project.setProjectStatus(form.getProjectStatus());
			project.setExpenseType(form.getExpenseType());

			if (!form.isLocal()){
				project.setAccessControlPolicy(form.getAccessControlType());
				project.resetRoles(form.getAccessControlType()==0);
			}


			if (form.isLocal())
				project.setLocal(true);
			else project.setTemporaryLocal(true);
			if (form.isForward())
				project.setStartDate(form.getStartDate());
			else
				project.setFinishDate(form.getStartDate());
			// copy any extra fields to the project
			project.getExtraFields().putAll(form.getExtra().getExtraFields());

//			PropertyUtils.copyProperties(project, projectDialog.getForm());
		} catch (Exception propertyException) {
			logger.log(Level.WARNING, "Failed to populate project from form", propertyException);
		}
		showWaitCursor(false);

		return true;
	}

	boolean doingOpenDialog = false;
	private void doOpenProjectDialog() {
		if (doingOpenDialog)
			return;
		doingOpenDialog = true;
		finishAnyOperations();

		final ArrayList descriptors = new ArrayList();
		final boolean localDescriptorSession = Environment.getStandAlone() || Environment.getUser() == null;
		final boolean allowMasterProjects = localDescriptorSession || (getCurrentFrame() == null && Environment.isAdministrator());
		final OpenProjectDialog dialog = OpenProjectDialog.getInstance(getFrame(),descriptors,Messages.getString("Text.openProject"),allowMasterProjects,true,null); //$NON-NLS-1$

    	Session session=SessionFactory.getInstance().getSession(localDescriptorSession);
		Job job=(Job)SessionFactory.callNoEx(session,"getLoadProjectDescriptorsJob",new Class[]{boolean.class,java.util.List.class,boolean.class},new Object[]{true,descriptors,Environment.getUser() != null && !Environment.isAdministrator()});
		if (job == null) {
			dialog.refreshProjects();
			doingOpenDialog = false;
			return;
		}
    	job.addSwingRunnable(new JobRunnable("Local: loadDocument"){ //$NON-NLS-1$
    		public Object run() throws Exception{
			   		dialog.refreshProjects();
	    		    	return null;
    		}
    	});
    	session.schedule(job);
		final Consumer<Object> setter=new Consumer<Object>() { public void accept(Object obj) {

		    }
		};
		final Consumer<Object> getter=new Consumer<Object>() { public void accept(Object obj) {
		    	final Object[] r=(Object[])obj;
		    	if (r!=null){
		    		DocumentData data=(DocumentData)r[0];
		    		boolean openAs=(Boolean)r[1];
		    		loadDocument(data.getUniqueId(),false,openAs,data.isLocal());
		    	}

		    }
		};
		try {
			dialog.execute(setter,getter); //$NON-NLS-1$
		} finally {
			doingOpenDialog = false;
		}
	}
	private void doInsertProjectDialog() {
		if (doingOpenDialog)
			return;
		doingOpenDialog = true;

		finishAnyOperations();

		final Project project;
		project= getCurrentFrame().getProject();

//		List nodes=getCurrentFrame().getSelectedNodes();
//		if (nodes==null||nodes.size()==0) return;
//		Node node=(Node)nodes.get(0);
//		if (!node.isInSubproject()) project= getCurrentFrame().getProject();
//		else{
//			while (!(node==null) && !(node.getImpl().getClass().getName().equals("com.microproject.pm.task.Subproject"))){
//				node=(Node)node.getParent();
//			}
//			if (node==null) return; //shouldn't happen
//			try {
//				project=(Project)node.getImpl().getClass().getMethod("getSubproject", null).invoke(node.getImpl(), null);
//			} catch (Exception e) {
//				return;
//			}
//		}

		final ArrayList descriptors = new ArrayList();
		final boolean localDescriptorSession = Environment.getStandAlone() || Environment.getUser() == null;
    	Session session=SessionFactory.getInstance().getSession(localDescriptorSession);
		Job job=(Job)SessionFactory.callNoEx(session,"getLoadProjectDescriptorsJob",new Class[]{boolean.class,java.util.List.class,boolean.class},new Object[]{true,descriptors,true});
		if (job == null) {
			doingOpenDialog = false;
			return;
		}
    	job.addSwingRunnable(new JobRunnable("Local: add"){ //$NON-NLS-1$
    		public Object run() throws Exception{
	    	    Consumer<Object> setter=new Consumer<Object>() { public void accept(Object obj) {

	    	        }
	    	    };
	    	    Consumer<Object> getter=new Consumer<Object>() { public void accept(Object obj) {
	    		        final Object[] r=(Object[])obj;
	    		        if (r!=null){
   		        			final DocumentData data=(DocumentData)r[0];
	    	        		if (data.isMaster())
	    	        			return;
	    	        		insertSubproject(project, data.getUniqueId(), true);
//	    	        		Project openedAlready = ProjectFactory.getInstance().findFromId(data.getUniqueId());
//
//							if (!project.canInsertProject(data.getUniqueId())) {
//								Alert.error("The selected project is already a subproject in this consolidated project.");
//								return;
//							}
//							if (openedAlready != null && openedAlready.isOpenedAsSubproject()) {
//								Alert.error("The selected project is already opened as a subproject in another consolidated project.");
//								return;
//							}
//							Subproject subprojectTask = new Subproject(project,data.getUniqueId());
//							Node subprojectNode = getCurrentFrame().addNodeForImpl(subprojectTask,NodeModel.EVENT);
//							ProjectFactory.getInstance().openSubproject(project, subprojectNode, true);
	    	        	}
	    	        }
	    	    };

	    		try {
	    		    OpenProjectDialog dlg = OpenProjectDialog.getInstance(getFrame(),descriptors,Messages.getString("Text.insertProject"),false, false, project); //$NON-NLS-1$
	    		    dlg.execute(setter,getter);
	    		} catch (Exception e) {
	    			Alert.error(Messages.getString("Message.serverUnreachable"),getContainer()); //$NON-NLS-1$
	    			logger.log(Level.WARNING, "Failed to open project dialog", e);
	    		} finally {
		    		doingOpenDialog = false;
	    		}
	    		return null;
    		}
		});
		session.schedule(job);
	}



	public void insertSubproject(final Project project, final long subprojectUniqueId,final boolean undo) {
		addHistory("insertSubproject", new Object[]{project.getName(),project.getUniqueId(),subprojectUniqueId});
		Project openedAlready = ProjectFactory.getInstance().findFromId(subprojectUniqueId);

		if (!project.getSubprojectHandler().canInsertProject(subprojectUniqueId)) {
			Alert.error(Messages.getString("GraphicManager.SelectedProjectAlreadySubproject")); //$NON-NLS-1$
			return;
		}
		if (openedAlready != null && openedAlready.isOpenedAsSubproject()) {
			Alert.error(Messages.getString("GraphicManager.SelectedProjectAlreadyOpenedAsSubproject")); //$NON-NLS-1$
			return;
		}
		SubProj subprojectTask = project.getSubprojectHandler().createSubProj(subprojectUniqueId);
		Node subprojectNode = getCurrentFrame().addNodeForImpl(subprojectTask,NodeModel.EVENT);
		ProjectFactory.getInstance().openSubproject(project, subprojectNode, true);

		//Undo
		if (undo){
			UndoController undoContoller=project.getUndoController();
			if (undoContoller.getEditSupport()!=null){
				undoContoller.clear();
				//undoContoller.getEditSupport().postEdit(new CreateSubprojectEdit(project,subprojectNode,subprojectUniqueId));
			}
		}

	}



	protected class CreateSubprojectEdit extends AbstractUndoableEdit{
		protected Project project;
		protected final Node subprojectNode;
		protected long subprojectUniqueId;


		public CreateSubprojectEdit(Project project, final Node subprojectNode, long subprojectUniqueId) {
			super();
			this.project = project;
			this.subprojectNode = subprojectNode;
			this.subprojectUniqueId = subprojectUniqueId;
		}
		public void redo() throws CannotRedoException {
			super.redo();
			insertSubproject(project, subprojectUniqueId, false);
		}
		public void undo() throws CannotUndoException {
			super.undo();
			project.getTaskOutline().remove(subprojectNode,NodeModel.EVENT);

//			UndoController undoContoller=project.getUndoController();
//			if (undoContoller.getEditSupport()!=null){
//				undoContoller.clear();
//			}
		}
	}



	private void doProjectInformationDialog() {
		if (!getCurrentFrame().isActive())
			return;
		if (!beforeProjectInformationRoute(getCurrentFrame().getProject()))
			return;
		finishAnyOperations();

		if (projectInformationDialog == null) {
			projectInformationDialog = ProjectInformationDialog.getInstance(getFrame(),getCurrentFrame().getProject());
			projectInformationDialog.pack();
			projectInformationDialog.setModal(false);
		} else {
			projectInformationDialog.setObject(getCurrentFrame().getProject());
		}
		projectInformationDialog.setLocationRelativeTo(getCurrentFrame());//to center on screen
		projectInformationDialog.setVisible(true);

	}

	public void doInformationDialog(boolean notes) {

		if (!isDocumentActive())
			return;

		finishAnyOperations();
	    List nodes=getCurrentFrame().getSelectedNodes(false);
	    if (nodes == null)
	    	return;
		if (nodes.size() > 1) {
			Alert.warn(Messages.getString("Message.onlySelectOneElement"),getContainer()); //$NON-NLS-1$
			return;
		}
		final Node node=(Node)nodes.get(0);
		Object impl=node.getImpl();
		if (impl instanceof Task||(impl instanceof Assignment&&taskType)){
			Task task=(Task)((impl instanceof Assignment)?(((Assignment)impl).getTask()):impl);
			boolean resourcesTab = impl instanceof Assignment;
			if (!beforeTaskInformationRoute(task, notes, resourcesTab))
				return;
			if (!CollaborationHelper.tryLockObject(task.getProject(), task, getCurrentFrame(), "open task details"))
				return;
			showTaskInformationDialog(task, notes, resourcesTab);
		} else if (impl instanceof Resource||(impl instanceof Assignment&&resourceType)) {
			Resource resource=(Resource)((impl instanceof Assignment)?(((Assignment)impl).getResource()):impl);;
			if (!beforeResourceInformationRoute(resource, notes))
				return;
			if (resourceInformationDialog == null) {
				resourceInformationDialog = ResourceInformationDialog.getInstance(getFrame(),resource);
				resourceInformationDialog.pack();
				resourceInformationDialog.setModal(false);
			} else {
				resourceInformationDialog.setObject(resource);
				resourceInformationDialog.updateAll();
			}
			resourceInformationDialog.setLocationRelativeTo(getCurrentFrame());//to center on screen
			if (notes)
				resourceInformationDialog.showNotes();
			resourceInformationDialog.setVisible(true);

		} else if (impl instanceof Project) {
			doProjectInformationDialog();
		}


	}

	public void doInformationDialog(Task task, boolean notes) {
		if (!isDocumentActive())
			return;
		finishAnyOperations();
		if (!beforeTaskInformationRoute(task, notes, false))
			return;
		if (!CollaborationHelper.tryLockObject(task.getProject(), task, getCurrentFrame(), "open task details"))
			return;
		showTaskInformationDialog(task, notes, false);
	}

	/**
	 * Builds the task dialog before publishing it to the reusable dialog cache.
	 * A layout failure during {@code pack()} must not leave a title-only dialog
	 * cached: the next Information command would otherwise display that partial
	 * instance instead of retrying construction.
	 */
	private void showTaskInformationDialog(Task task, boolean notes, boolean resourcesTab) {
		if (taskInformationDialog == null) {
			TaskInformationDialog dialog = TaskInformationDialog.getInstance(getFrame(), task, notes);
			try {
				dialog.pack();
				dialog.setModal(false);
				taskInformationDialog = dialog;
			} catch (RuntimeException | Error e) {
				dialog.dispose();
				throw e;
			}
		} else {
			taskInformationDialog.setObject(task);
			taskInformationDialog.updateAll();
		}
		taskInformationDialog.setLocationRelativeTo(getCurrentFrame());
		if (notes)
			taskInformationDialog.showNotes();
		else if (resourcesTab)
			taskInformationDialog.showResources();
		taskInformationDialog.setVisible(true);
	}

	private Object getSingleSelectedImpl() {
		if (!isDocumentActive())
			return null;

		finishAnyOperations();
		List nodes = getCurrentFrame().getSelectedNodes(false);
		if (nodes == null || nodes.isEmpty())
			return null;
		if (nodes.size() > 1) {
			Alert.warn(Messages.getString("Message.onlySelectOneElement"), getContainer()); //$NON-NLS-1$
			return null;
		}
		return ((Node) nodes.get(0)).getImpl();
	}

	private void showTaskInformationForSelection(boolean notes) {
		Object impl = getSingleSelectedImpl();
		if (impl == null)
			return;
		if (impl instanceof Assignment)
			impl = ((Assignment) impl).getTask();
		if (!(impl instanceof Task))
			return;

		Task task = (Task) impl;
		if (!beforeTaskInformationRoute(task, notes, false))
			return;
		if (!CollaborationHelper.tryLockObject(task.getProject(), task, getCurrentFrame(), "open task details"))
			return;
		showTaskInformationDialog(task, notes, false);
	}

	private void showResourceInformationForSelection(boolean notes) {
		Object impl = getSingleSelectedImpl();
		if (impl == null)
			return;
		if (impl instanceof Assignment)
			impl = ((Assignment) impl).getResource();
		if (!(impl instanceof Resource))
			return;

		Resource resource = (Resource) impl;
		if (!beforeResourceInformationRoute(resource, notes))
			return;
		if (resourceInformationDialog == null) {
			resourceInformationDialog = ResourceInformationDialog.getInstance(getFrame(), resource);
			resourceInformationDialog.pack();
			resourceInformationDialog.setModal(false);
		} else {
			resourceInformationDialog.setObject(resource);
			resourceInformationDialog.updateAll();
		}
		resourceInformationDialog.setLocationRelativeTo(getCurrentFrame());
		if (notes)
			resourceInformationDialog.showNotes();
		resourceInformationDialog.setVisible(true);
	}

	protected boolean beforeActionRoute(String actionId) {
		return false;
	}

	protected boolean beforeExternalRoute(String routeId) {
		return true;
	}

	protected boolean beforeViewSwitchRoute(String viewId) {
		return true;
	}

	protected boolean beforeChooserRoute(String chooserId) {
		return true;
	}

	protected boolean beforeFindRoute(Searchable searchable, Field field) {
		return true;
	}

	protected boolean beforeToggleRoute(String actionId) {
		return true;
	}

	protected boolean beforeProjectInformationRoute(Project project) {
		return true;
	}

	protected boolean beforeProjectsDialogRoute(Project project) {
		return true;
	}

	protected boolean beforeTaskInformationRoute(Task task, boolean notes, boolean resourcesTab) {
		return true;
	}

	protected boolean beforeResourceInformationRoute(Resource resource, boolean notes) {
		return true;
	}

	protected boolean beforeAssignResourcesRoute(DocumentFrame documentFrame) {
		return true;
	}

	protected boolean beforeTimesheetRoute(DocumentFrame documentFrame) {
		return true;
	}

	protected boolean beforeChangeWorkingTimeRoute(Project project, boolean restrict) {
		return true;
	}

	protected boolean beforeCalendarOptionsRoute() {
		return true;
	}

	protected boolean beforeUpdateTasksRoute(DocumentFrame documentFrame) {
		return true;
	}

	protected boolean beforeUpdateProjectRoute(DocumentFrame documentFrame) {
		return true;
	}

	protected boolean beforeSaveBaselineRoute(DocumentFrame documentFrame) {
		return true;
	}

	protected boolean beforeClearBaselineRoute(DocumentFrame documentFrame) {
		return true;
	}


	public Action getAction(String key) throws MissingListenerException {
		if (actionsMap == null)
			addHandlers();

		Action action = actionsMap.getConcreteAction(key);
		if (action == null)
			throw new MissingListenerException("no listener for mainFrame", getClass().getName(),key); //$NON-NLS-1$

		return action;
	}

	public String getStringFromAction(Action action) throws MissingListenerException {
		return actionsMap.getStringFromAction(action);
	}

	public void addHandlers() {
		actionsMap = new MenuActionsMap(menuManager);
		actionsMap.addHandler(ACTION_NEW_PROJECT, new NewProjectAction());
		actionsMap.addHandler(ACTION_OPEN_PROJECT, new OpenProjectAction());
		actionsMap.addHandler(ACTION_RECENT_PROJECTS, new RecentProjectsAction());
		actionsMap.addHandler(ACTION_INSERT_PROJECT, new InsertProjectAction());
		actionsMap.addHandler(ACTION_EXIT, new ExitAction());
		actionsMap.addHandler(ACTION_IMPORT_MSPROJECT, new ImportMSProjectAction());
		actionsMap.addHandler(ACTION_EXPORT_MSPROJECT, new ExportMSProjectAction());
		actionsMap.addHandler(ACTION_ABOUT_PROJECTLIBRE, new AboutAction());
		actionsMap.addHandler(ACTION_PROJECTLIBRE, new ProjectLibreAction());
		actionsMap.addHandler(ACTION_PROJECTLIBRE_DOCUMENTATION, new HelpAction());
		actionsMap.addHandler(ACTION_PROJECT_INFORMATION, new ProjectInformationAction());
		actionsMap.addHandler(ACTION_DEFINE_CODE, new DefineCodeAction());
		actionsMap.addHandler(ACTION_PROJECTS_DIALOG, new ProjectsDialogAction());
		actionsMap.addHandler(ACTION_TEAM_FILTER, new TeamFilterAction());
		actionsMap.addHandler(ACTION_DOCUMENTS, new DocumentsAction());
		actionsMap.addHandler(ACTION_INFORMATION, new InformationAction());
		actionsMap.addHandler("RibbonTaskInformationAction", new RibbonTaskInformationAction());
		actionsMap.addHandler("RibbonResourceInformationAction", new RibbonResourceInformationAction());
		actionsMap.addHandler(ACTION_NOTES, new NotesAction());
		actionsMap.addHandler(ACTION_ASSIGN_RESOURCES, new AssignResourcesAction());
		actionsMap.addHandler(ACTION_TIMESHEET, new TimesheetAction());

		actionsMap.addHandler(ACTION_FIND, new FindAction());
		actionsMap.addHandler(ACTION_GOTO, new GoToAction());
		actionsMap.addHandler(ACTION_INSERT_TASK, new InsertTaskAction());
		actionsMap.addHandler(ACTION_INSERT_RESOURCE, new InsertTaskAction()); // will do resource
		actionsMap.addHandler(ACTION_SAVE_PROJECT, new SaveProjectAction());
		actionsMap.addHandler(ACTION_SAVE_PROJECT_AS, new SaveProjectAsAction());
		actionsMap.addHandler(ACTION_PRINT, new PrintAction());
		actionsMap.addHandler(ACTION_PRINT_PREVIEW, new PrintPreviewAction());
		actionsMap.addHandler(ACTION_PDF, new PDFAction());
		actionsMap.addHandler(ACTION_CLOSE_PROJECT, new CloseProjectAction());
		actionsMap.addHandler(ACTION_UNDO, new UndoAction());
		actionsMap.addHandler(ACTION_REDO, new RedoAction());
//		actionsMap.addHandler(ACTION_ENTERPRISE_RESOURCES, new EnterpriseResourcesAction());
		actionsMap.addHandler(ACTION_CHANGE_WORKING_TIME, new ChangeWorkingTimeAction());
		actionsMap.addHandler(ACTION_LEVEL_RESOURCES, new LevelResourcesAction());
		actionsMap.addHandler(ACTION_DELEGATE_TASKS, new DelegateTasksAction());
		actionsMap.addHandler(ACTION_TIMELINE, new TimelineAction());
		actionsMap.addHandler(ACTION_CALENDAR_VIEW, new CalendarViewAction());
		actionsMap.addHandler(ACTION_CUSTOM_REPORT, new CustomReportAction());
		actionsMap.addHandler(ACTION_UPDATE_TASKS, new UpdateTasksAction());
		actionsMap.addHandler(ACTION_UPDATE_PROJECT, new UpdateProjectAction());
		actionsMap.addHandler(ACTION_RECALCULATE, new RecalculateAction());
		actionsMap.addHandler(ACTION_BAR, new BarAction());
		actionsMap.addHandler(ACTION_TIMESCALE, new TimescaleAction());
		actionsMap.addHandler(ACTION_GRIDLINES, new GridlinesAction());
		actionsMap.addHandler(ACTION_TEXT_STYLES, new TextStylesAction());
		actionsMap.addHandler(ACTION_BAR_STYLES, new BarStylesAction());
		actionsMap.addHandler(ACTION_LAYOUT, new LayoutAction());
		actionsMap.addHandler(ACTION_INSERT_RECURRING, new RecurringTaskAction());
		actionsMap.addHandler(ACTION_SORT, new SortAction());
		actionsMap.addHandler(ACTION_GROUP, new GroupAction());
		actionsMap.addHandler(ACTION_CALENDAR_OPTIONS, new CalendarOptionsAction());
		actionsMap.addHandler(ACTION_SAVE_BASELINE, new SaveBaselineAction());
		actionsMap.addHandler(ACTION_CLEAR_BASELINE, new ClearBaselineAction());
		actionsMap.addHandler(ACTION_LOCALE, new LocaleAction());
		actionsMap.addHandler(ACTION_LINK, new LinkAction());
		actionsMap.addHandler(ACTION_UNLINK, new UnlinkAction());
		actionsMap.addHandler(ACTION_ZOOM_IN, new ZoomInAction());
		actionsMap.addHandler(ACTION_ZOOM_OUT, new ZoomOutAction());
		actionsMap.addHandler(ACTION_SCROLL_TO_TASK, new ScrollToTaskAction());
		actionsMap.addHandler(ACTION_TOGGLE_PROGRESS_LINE, new ToggleProgressLineAction());
		actionsMap.addHandler(ACTION_LABEL_RESOURCE_NAMES, new LabelResourceNamesAction());
		actionsMap.addHandler(ACTION_LABEL_TASK_NAME, new LabelTaskNameAction());
		actionsMap.addHandler(ACTION_INDENT, new IndentAction());
		actionsMap.addHandler(ACTION_OUTDENT, new OutdentAction());
		actionsMap.addHandler(ACTION_MOVE_TASK_UP, new MoveTaskUpAction());
		actionsMap.addHandler(ACTION_MOVE_TASK_DOWN, new MoveTaskDownAction());
		actionsMap.addHandler(ACTION_COLLAPSE, new CollapseAction());
		actionsMap.addHandler(ACTION_EXPAND, new ExpandAction());


		actionsMap.addHandler(ACTION_CUT, new CutAction());
		actionsMap.addHandler(ACTION_COPY, new CopyAction());
		actionsMap.addHandler(ACTION_PASTE, new PasteAction());
		actionsMap.addHandler(ACTION_PASTE_INSERT, new PasteInsertAction());
		actionsMap.addHandler(ACTION_DELETE, new DeleteAction());

		actionsMap.addHandler(ACTION_GANTT, new ViewAction(ACTION_GANTT));
		actionsMap.addHandler(ACTION_TRACKING_GANTT, new ViewAction(ACTION_TRACKING_GANTT));
		actionsMap.addHandler(ACTION_TASK_USAGE_DETAIL, new ViewAction(ACTION_TASK_USAGE_DETAIL));
		actionsMap.addHandler(ACTION_RESOURCE_USAGE_DETAIL, new ViewAction(ACTION_RESOURCE_USAGE_DETAIL));
		actionsMap.addHandler(ACTION_NETWORK, new ViewAction(ACTION_NETWORK));
		actionsMap.addHandler(ACTION_WBS, new ViewAction(ACTION_WBS));
		actionsMap.addHandler(ACTION_RBS, new ViewAction(ACTION_RBS));
		actionsMap.addHandler(ACTION_REPORT, new ViewAction(ACTION_REPORT));
		actionsMap.addHandler(ACTION_PROJECTS, new ViewAction(ACTION_PROJECTS));
		actionsMap.addHandler(ACTION_RESOURCES, resourceAction = new ViewAction(ACTION_RESOURCES));
		actionsMap.addHandler(ACTION_HISTOGRAM, new ViewAction(ACTION_HISTOGRAM));
		actionsMap.addHandler(ACTION_CHARTS, new ViewAction(ACTION_CHARTS));
		actionsMap.addHandler(ACTION_TASK_USAGE, new ViewAction(ACTION_TASK_USAGE));
		actionsMap.addHandler(ACTION_RESOURCE_USAGE, new ViewAction(ACTION_RESOURCE_USAGE));
		actionsMap.addHandler(ACTION_NO_SUB_WINDOW, new ViewAction(ACTION_NO_SUB_WINDOW));

		actionsMap.addHandler(ACTION_CHOOSE_FILTER, new TransformAction(TransformComboBoxModel.FILTER));
		actionsMap.addHandler(ACTION_CHOOSE_SORT, new TransformAction(TransformComboBoxModel.SORTER));
		actionsMap.addHandler(ACTION_CHOOSE_GROUP, new TransformAction(TransformComboBoxModel.GROUPER));

		actionsMap.addHandler(ACTION_PALETTE, new PaletteAction());
		actionsMap.addHandler(ACTION_LOOK_AND_FEEL, new LookAndFeelAction());
		actionsMap.addHandler(ACTION_FULL_SCREEN, new FullScreenAction());
		actionsMap.addHandler(ACTION_REFRESH, new RefreshAction());


	}

	public class NewProjectAction extends MenuActionsMap.GlobalMenuAction {
		private static final long serialVersionUID = 1L;
		public void actionPerformed(ActionEvent arg0) {
			setMeAsLastGraphicManager();
			if (!beforeExternalRoute("newProject")) return;
			doNewProjectDialog();
		}
		protected boolean allowed(boolean enable){
			DocumentFrame dframe = getCurrentFrame();
			return dframe == null || !dframe.isEditingResourcePool();
		}
	}

	public class OpenProjectAction extends MenuActionsMap.GlobalMenuAction {
		private static final long serialVersionUID = 1L;
		public void actionPerformed(ActionEvent arg0) {

			setMeAsLastGraphicManager();
			if (!beforeExternalRoute("openProject")) return;
			if (Environment.getStandAlone()) openLocalProject();
			else doOpenProjectDialog();
		}
		protected boolean allowed(boolean enable){
			DocumentFrame dframe = getCurrentFrame();
			return dframe == null || !dframe.isEditingResourcePool();
		}
		protected boolean needsDocument() {
			return  !allowed(true); // force it to be called iff the resource pool is open
		}


	}

	public class RecentProjectsAction extends MenuActionsMap.GlobalMenuAction {
		private static final long serialVersionUID = 1L;
		public void actionPerformed(ActionEvent arg0) {
			setMeAsLastGraphicManager();
			if (!beforeExternalRoute("openProject")) return;
			doRecentProjectsDialog();
		}
		protected boolean allowed(boolean enable){
			DocumentFrame dframe = getCurrentFrame();
			return dframe == null || !dframe.isEditingResourcePool();
		}
		protected boolean needsDocument() {
			return !allowed(true);
		}
	}

	public class InsertProjectAction extends MenuActionsMap.DocumentMenuAction {
		private static final long serialVersionUID = 1L;
		public void actionPerformed(ActionEvent arg0) {
			setMeAsLastGraphicManager();
			if (!beforeExternalRoute("insertProject")) return;
			doInsertProjectDialog();
		}
		protected boolean allowed(boolean enable) {
			if (enable==false) return true;
			return isDocumentWritable();
		}
	}

	public class ExitAction extends MenuActionsMap.GlobalMenuAction {
		private static final long serialVersionUID = 1L;
		public void actionPerformed(ActionEvent arg0) {
			setMeAsLastGraphicManager();
		    closeApplication();
		}
	}

	public class ImportMSProjectAction extends MenuActionsMap.GlobalMenuAction {
		private static final long serialVersionUID = 1L;
		public void actionPerformed(ActionEvent arg0) {
			setMeAsLastGraphicManager();
			if (!beforeExternalRoute("openProject")) return;
			openLocalProject();		}
	}

	public class ExportMSProjectAction extends MenuActionsMap.DocumentMenuAction {
		private static final long serialVersionUID = 1L;
		public void actionPerformed(ActionEvent arg0) {
			setMeAsLastGraphicManager();
			if (!beforeExternalRoute("saveAsProject")) return;
			saveLocalProject(true);		}
	}

	public class AboutAction extends MenuActionsMap.GlobalMenuAction {
		private static final long serialVersionUID = 1L;
		public void actionPerformed(ActionEvent arg0) {
			setMeAsLastGraphicManager();
			if (!beforeExternalRoute("about")) return;
			showAboutDialog();		}
	}

	public class ProjectLibreAction extends MenuActionsMap.GlobalMenuAction {
		private static final long serialVersionUID = 1L;
		public void actionPerformed(ActionEvent arg0) {
			setMeAsLastGraphicManager();
			if (!beforeExternalRoute("projectLibre")) return;
			BrowserControl.displayURL(UiLinkTargets.PROJECT_HOME);
		}
	}

	public class HelpAction extends MenuActionsMap.GlobalMenuAction {
		private static final long serialVersionUID = 1L;
		public void actionPerformed(ActionEvent arg0) {
			setMeAsLastGraphicManager();
			if (!beforeExternalRoute("help")) return;
			showHelpDialog();		}
	}

	public class ProjectInformationAction extends MenuActionsMap.DocumentMenuAction {
		private static final long serialVersionUID = 1L;
		public void actionPerformed(ActionEvent arg0) {
			setMeAsLastGraphicManager();
			doProjectInformationDialog();
		}
	}

	public class ProjectsDialogAction extends MenuActionsMap.DocumentMenuAction {
		private static final long serialVersionUID = 1L;
		public void actionPerformed(ActionEvent arg0) {
			setMeAsLastGraphicManager();
			if (!beforeProjectsDialogRoute(getCurrentFrame().getProject())) return;
			ProjectsDialog.show(GraphicManager.this);
		}
	}

	public class TeamFilterAction extends MenuActionsMap.DocumentMenuAction {
		private static final long serialVersionUID = 1L;
		public void actionPerformed(ActionEvent arg0) {
			setMeAsLastGraphicManager();
			GlobalPreferences preferences=getPreferences();
			//Field field = Configuration.getFieldFromId("Field.showProjectResourcesOnly");
			boolean teamOnly=!preferences.isShowProjectResourcesOnly();
			//field.setValue(preferences,this,teamOnly);
			preferences.setShowProjectResourcesOnly(teamOnly);
			List<AbstractButton> buttons = getMenuManager().getToolBarFactory().getButtonsFromId("TeamFilter"); //$NON-NLS-1$
			if (buttons!=null){
				for (AbstractButton button : buttons) {
					if (Environment.isNewLook())
						button.setIcon(IconManager.getIcon(teamOnly?"menu24.showTeamResources":"menu24.showAllResources")); //$NON-NLS-1$ //$NON-NLS-2$
					else
						button.setIcon(IconManager.getIcon(teamOnly?"menu.showTeamResourcesSmall":"menu.showAllResourcesSmall")); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
			menuManager.setActionSelected(ACTION_TEAM_FILTER,teamOnly);


		}
	}
	public class DocumentsAction extends MenuActionsMap.DocumentMenuAction {
		private static final long serialVersionUID = 1L;
		public void actionPerformed(ActionEvent arg0) {
			setMeAsLastGraphicManager();
			if (!isDocumentActive())
				return;
			invokeFieldAction(ACTION_DOCUMENTS,getCurrentFrame().getProject());
		}
	}

	private boolean isEnabledFieldAction(String action, Object obj) {
		Field f = FieldDictionary.getInstance().getActionField(ACTION_DOCUMENTS);
		return (obj != null && f != null && f.getValue(obj,null) != null);

	}
	private void invokeFieldAction(String action, Object obj) {
		Field f = FieldDictionary.getInstance().getActionField(ACTION_DOCUMENTS);
		if (f != null)
			f.invokeAction(obj);

	}
	public class CalendarOptionsAction extends MenuActionsMap.GlobalMenuAction {
		private static final long serialVersionUID = 1L;
		public void actionPerformed(ActionEvent arg0) {
			setMeAsLastGraphicManager();
			if (!beforeCalendarOptionsRoute()) return;
			doCalendarOptionsDialog();
		}
	}


	public class InformationAction extends MenuActionsMap.DocumentMenuAction {
		private static final long serialVersionUID = 1L;
		public void actionPerformed(ActionEvent arg0) {
			setMeAsLastGraphicManager();
			doInformationDialog(false);
		}
	}
	public class RibbonTaskInformationAction extends MenuActionsMap.DocumentMenuAction {
		private static final long serialVersionUID = 1L;
		public void actionPerformed(ActionEvent arg0) {
			setMeAsLastGraphicManager();
			showTaskInformationForSelection(false);
		}
	}
	public class RibbonResourceInformationAction extends MenuActionsMap.DocumentMenuAction {
		private static final long serialVersionUID = 1L;
		public void actionPerformed(ActionEvent arg0) {
			setMeAsLastGraphicManager();
			showResourceInformationForSelection(false);
		}
	}
	public class NotesAction extends MenuActionsMap.DocumentMenuAction {
		private static final long serialVersionUID = 1L;
		public void actionPerformed(ActionEvent arg0) {
			setMeAsLastGraphicManager();
			doInformationDialog(true);
		}
	}

	public class AssignResourcesAction extends MenuActionsMap.DocumentMenuAction {
		private static final long serialVersionUID = 1L;
		public void actionPerformed(ActionEvent arg0) {
			setMeAsLastGraphicManager();
			if (!beforeAssignResourcesRoute(getCurrentFrame())) return;
			showAssignmentDialog(getCurrentFrame());
		}
	}

	public class TimesheetAction extends MenuActionsMap.DocumentMenuAction {
		private static final long serialVersionUID = 1L;
		public void actionPerformed(ActionEvent arg0) {
			setMeAsLastGraphicManager();
			if (!beforeTimesheetRoute(getCurrentFrame())) return;
			showTimesheetDialog(getCurrentFrame());
		}
	}

	public class SelectDocumentAction extends MenuActionsMap.GlobalMenuAction {
		private static final long serialVersionUID = 1L;
		DocumentFrame frame;
		public SelectDocumentAction(DocumentFrame frame) {
			this.frame = frame;
		}
		public void actionPerformed(ActionEvent arg0) {
			setMeAsLastGraphicManager();
			GraphicManager.this.setCurrentFrame(frame);
		}
		@Override
		public Object getValue(String key) {
			if (key == Action.NAME)
				return frame.getProject().getName();
			return super.getValue(key);
		}
	}

	// Document actions
	public class FindAction extends MenuActionsMap.DocumentMenuAction {
		private static final long serialVersionUID = 1L;
		public void actionPerformed(ActionEvent arg0) {
			setMeAsLastGraphicManager();
			if (isDocumentActive()) {
				String query = arg0.getSource() instanceof JTextField ? arg0.getActionCommand() : null;
				Field field = query == null || query.isBlank() ? null : Configuration.getFieldFromId("Field.name");
				doFind(getCurrentFrame().getTopSpreadSheet(), field, query);
			}
		}
		protected boolean allowed(boolean enable) {
			if (enable==false) return true;
			return (currentFrame.getActiveSpreadSheet() != null);
		}
	}

	public class GoToAction extends MenuActionsMap.DocumentMenuAction {
		private static final long serialVersionUID = 1L;
		public void actionPerformed(ActionEvent arg0) {
			setMeAsLastGraphicManager();
			if (isDocumentActive())
				doFind(getCurrentFrame().getTopSpreadSheet(),Configuration.getFieldFromId("Field.id"));
		}
		protected boolean allowed(boolean enable) {
			if (enable==false) return true;
			return (currentFrame.getActiveSpreadSheet() != null);
		}
	}

	public class RecalculateAction extends MenuActionsMap.DocumentMenuAction {
		private static final long serialVersionUID = 1L;
		public void actionPerformed(ActionEvent arg0) {
			setMeAsLastGraphicManager();
			if (isDocumentActive())
				recalculateProject(getCurrentFrame().getProject());
		}
	}

	void recalculateProject(Project project) {
		if (project != null)
			project.recalculate();
	}

	public class InsertTaskAction extends MenuActionsMap.DocumentMenuAction {
		private static final long serialVersionUID = 1L;
		public void actionPerformed(ActionEvent arg0) {
			setMeAsLastGraphicManager();
			if (isDocumentActive())
				getCurrentFrame().addNodeForImpl(null);
		}
		protected boolean allowed(boolean enable) {
			if (enable==false) return true;
			return isDocumentWritable();
		}
	}

	public class SaveProjectAction extends MenuActionsMap.DocumentMenuAction {
		private static final long serialVersionUID = 1L;
		public void actionPerformed(ActionEvent arg0) {
			setMeAsLastGraphicManager();
			if (!beforeExternalRoute("saveProject")) return;
			if (Environment.getStandAlone()) saveLocalProject(false);
			else{
				if (isDocumentActive()) {
					final DocumentFrame frame=getCurrentFrame();
					final Project project = frame.getProject();
					SaveOptions opt=new SaveOptions();
					opt.setPostSaving(new Consumer<Object>() { public void accept(Object arg0) {
							refreshSaveStatus(true);
						}
					});
					opt.setPreSaving(getSavingClosure());
					addHistory("saveProject", new Object[]{project.getName(),project.getUniqueId()});
					projectFactory.saveProject(project,opt);
				}
			}

		}
		protected boolean allowed(boolean enable) {
			if (enable==false) return true;
			NamedFrame frame=getCurrentFrame();
			if (frame==null) return false;
			Project project=getCurrentFrame().getProject();
			if (project==null) return false;
			return Environment.isProjectLibre() || (!project.isLocal()&&project.needsSaving());
		}
	}

	public class SaveProjectAsAction extends MenuActionsMap.DocumentMenuAction {
		private static final long serialVersionUID = 1L;
		public void actionPerformed(ActionEvent arg0) {
			setMeAsLastGraphicManager();
			if (!beforeExternalRoute("saveAsProject")) return;
			if (Environment.getStandAlone()) saveLocalProject(true);
			else{
				if (isDocumentActive()) {
					final DocumentFrame frame=getCurrentFrame();
					final Project project = frame.getProject();
					SaveOptions opt=new SaveOptions();
					opt.setPostSaving(new Consumer<Object>() { public void accept(Object arg0) {
							frame.setId(project.getUniqueId()+""); //$NON-NLS-1$
							refreshSaveStatus(true);
						}
					});
					opt.setSaveAs(true);
					opt.setPreSaving(getSavingClosure());
					projectFactory.saveProject(project,opt);
				}
			}
		}
		protected boolean allowed(boolean enable) {
			if (enable==false) return true;
			NamedFrame frame=getCurrentFrame();
			if (frame==null) return false;
			Project project=getCurrentFrame().getProject();
			if (project==null) return false;
			if (project.isMaster() && !Environment.getStandAlone() && !Environment.isProjectLibre())
				return false;

			return (project.isSavable());
//			return true;//!project.isLocal()&&!project.isMaster();
		}
	}

	public class PrintAction extends MenuActionsMap.DocumentMenuAction {
		private static final long serialVersionUID = 1L;
		public void actionPerformed(ActionEvent arg0) {
			setMeAsLastGraphicManager();
			if (!beforeExternalRoute("print")) return;
			if (isDocumentActive())
				print();
		}
	}
	public class PrintPreviewAction extends MenuActionsMap.DocumentMenuAction {
		private static final long serialVersionUID = 1L;
		public void actionPerformed(ActionEvent arg0) {
			setMeAsLastGraphicManager();
			if (!beforeExternalRoute("printPreview")) return;
			if (isDocumentActive()) {
				Component c = (Component)arg0.getSource();
				Cursor cur = c.getCursor();
				c.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				printPreview();
				c.setCursor(cur);

			}
			}
	}
	public class PDFAction extends MenuActionsMap.DocumentMenuAction {
		private static final long serialVersionUID = 1L;
		public void actionPerformed(ActionEvent arg0) {
//claur - PDF enabled
//			if (Environment.isProjectLibre()) {
//				PODOnlyFeature.doDialog(getFrame());
//				return;
//			}
			setMeAsLastGraphicManager();
			if (!beforeExternalRoute("pdf")) return;
			if (isDocumentActive()) {
				Component c = (Component)arg0.getSource();
				Cursor cur = c.getCursor();
				c.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				savePDF();
				c.setCursor(cur);
			}
		}
	}

	public class CloseProjectAction extends MenuActionsMap.DocumentMenuAction {
		private static final long serialVersionUID = 1L;
		public void actionPerformed(ActionEvent arg0) {
			setMeAsLastGraphicManager();
			if (!beforeExternalRoute("closeProject")) return;
			if (isDocumentActive())
				closeProject(getCurrentFrame().getProject());
		}
	}

	public class UndoAction extends MenuActionsMap.DocumentMenuAction {
		private static final long serialVersionUID = 1L;
		public void actionPerformed(ActionEvent arg0) {
			setMeAsLastGraphicManager();
			if (isDocumentActive()){
				doUndoRedo(true);
			}
		}
	}
	public class RedoAction extends MenuActionsMap.DocumentMenuAction {
		private static final long serialVersionUID = 1L;
		public void actionPerformed(ActionEvent arg0) {
			setMeAsLastGraphicManager();
			if (isDocumentActive()){
				doUndoRedo(false);
			}
		}
	}


	public void doUndoRedo(boolean isUndo){
		DocumentFrame frame=getCurrentFrame();
		UndoController undoController=getUndoController();
		Object[] args=null;
		if (undoController!=null){
			if (isUndo){
	            String name=undoController.getUndoName();
	            if (name!=null) args=new Object[]{true,name};
			}else{
	            String name=undoController.getRedoName();
	            if (name!=null) args=new Object[]{false,name};
			}
		}
		if (args==null) args=new Object[]{isUndo};
		addHistory("doUndoRedo",args);
		frame.doUndoRedo(isUndo);

	}

//	public class EnterpriseResourcesAction extends MenuActionsMap.DocumentMenuAction {
//		public void actionPerformed(ActionEvent arg0) {
//			if (isDocumentActive())
//				getCurrentFrame().doEnterpriseResourcesDialog();
//		}
//	}

	public class ChangeWorkingTimeAction extends MenuActionsMap.DocumentMenuAction {
		private static final long serialVersionUID = 1L;
		public void actionPerformed(ActionEvent arg0) {
			setMeAsLastGraphicManager();
			JDialog dlg = AbstractDialog.containedInDialog(arg0.getSource());
			boolean restrict = dlg != null;
			if (!beforeChangeWorkingTimeRoute(getCurrentFrame().getProject(), restrict)) return;
			if (isDocumentActive())
				getCurrentFrame().doChangeWorkingTimeDialog(restrict);
		}
	}

	public class LevelResourcesAction extends MenuActionsMap.DocumentMenuAction {
		private static final long serialVersionUID = 1L;
		public void actionPerformed(ActionEvent arg0) {
			setMeAsLastGraphicManager();
			if (isDocumentActive())
				getCurrentFrame().doLevelResourcesDialog();
		}
	}
	public class DelegateTasksAction extends MenuActionsMap.DocumentMenuAction {
		private static final long serialVersionUID = 1L;
		public void actionPerformed(ActionEvent arg0) {
			setMeAsLastGraphicManager();
			if (isDocumentActive())
				getCurrentFrame().doDelegateTasksDialog();
		}
	}

	public class UpdateTasksAction extends MenuActionsMap.DocumentMenuAction {
		private static final long serialVersionUID = 1L;
		public void actionPerformed(ActionEvent arg0) {
			setMeAsLastGraphicManager();
			if (!beforeUpdateTasksRoute(getCurrentFrame())) return;
			if (isDocumentActive())
				getCurrentFrame().doUpdateTasksDialog();
		}
		protected boolean allowed(boolean enable) {
			if (enable==false) return true;
			return isDocumentWritable();
		}
	}
	public class UpdateProjectAction extends MenuActionsMap.DocumentMenuAction {
		private static final long serialVersionUID = 1L;
		public void actionPerformed(ActionEvent arg0) {
			setMeAsLastGraphicManager();
			if (!beforeUpdateProjectRoute(getCurrentFrame())) return;
			if (isDocumentActive())
				getCurrentFrame().doUpdateProjectDialog();
		}
		protected boolean allowed(boolean enable) {
			if (enable==false) return true;
			return isDocumentWritable();
		}
	}

	public class DefineCodeAction extends MenuActionsMap.DocumentMenuAction {
		private static final long serialVersionUID = 1L;
		public void actionPerformed(ActionEvent arg0) {
			setMeAsLastGraphicManager();
			if (isDocumentActive())
				getCurrentFrame().doDefineCodeDialog();
		}
	}

	public class BarAction extends MenuActionsMap.DocumentMenuAction {
		private static final long serialVersionUID = 1L;
		public void actionPerformed(ActionEvent arg0) {
			setMeAsLastGraphicManager();
			if (!beforeChooserRoute(ACTION_BAR_STYLES)) return;
			showBarStyleChooser();
		}
	}
	public class TimescaleAction extends MenuActionsMap.DocumentMenuAction {
		private static final long serialVersionUID = 1L;
		public void actionPerformed(ActionEvent arg0) {
			setMeAsLastGraphicManager();
			if (!beforeChooserRoute(ACTION_TIMESCALE)) return;
			showTimescaleChooser();
		}
	}
	public class GridlinesAction extends MenuActionsMap.DocumentMenuAction {
		private static final long serialVersionUID = 1L;
		public void actionPerformed(ActionEvent arg0) {
			setMeAsLastGraphicManager();
			if (!beforeToggleRoute(ACTION_GRIDLINES)) return;
			toggleGridlines();
		}
	}
	public class TextStylesAction extends MenuActionsMap.DocumentMenuAction {
		private static final long serialVersionUID = 1L;
		public void actionPerformed(ActionEvent arg0) {
			setMeAsLastGraphicManager();
			if (!beforeChooserRoute(ACTION_TEXT_STYLES)) return;
			showTextStyleChooser();
		}
	}
	public class BarStylesAction extends MenuActionsMap.DocumentMenuAction {
		private static final long serialVersionUID = 1L;
		public void actionPerformed(ActionEvent arg0) {
			setMeAsLastGraphicManager();
			if (!beforeChooserRoute(ACTION_BAR_STYLES)) return;
			showBarStyleChooser();
		}
	}
	public class LayoutAction extends MenuActionsMap.DocumentMenuAction {
		private static final long serialVersionUID = 1L;
		public void actionPerformed(ActionEvent arg0) {
			setMeAsLastGraphicManager();
			if (!beforeChooserRoute(ACTION_LAYOUT)) return;
			showLayoutChooser();
		}
	}
	public class RecurringTaskAction extends MenuActionsMap.DocumentMenuAction {
		private static final long serialVersionUID = 1L;
		public void actionPerformed(ActionEvent arg0) {
			setMeAsLastGraphicManager();
			if (isDocumentActive())
				getCurrentFrame().doRecurringTaskDialog();
		}
	}
	public class SortAction extends MenuActionsMap.DocumentMenuAction {
		private static final long serialVersionUID = 1L;
		public void actionPerformed(ActionEvent arg0) {
			setMeAsLastGraphicManager();
			if (isDocumentActive())
				getCurrentFrame().doSortDialog();
		}
	}
	public class GroupAction extends MenuActionsMap.DocumentMenuAction {
		private static final long serialVersionUID = 1L;
		public void actionPerformed(ActionEvent arg0) {
			setMeAsLastGraphicManager();
			if (isDocumentActive())
				getCurrentFrame().doGroupDialog();
		}
	}
	public class SaveBaselineAction extends MenuActionsMap.DocumentMenuAction {
		private static final long serialVersionUID = 1L;
		public void actionPerformed(ActionEvent arg0) {
			setMeAsLastGraphicManager();
			if (!beforeSaveBaselineRoute(getCurrentFrame())) return;
			if (isDocumentActive())
				getCurrentFrame().doBaselineDialog(true);
		}
		protected boolean allowed(boolean enable) {
			if (enable==false) return true;
			return isDocumentWritable();
		}
	}

	public class ClearBaselineAction extends MenuActionsMap.DocumentMenuAction {
		private static final long serialVersionUID = 1L;
		public void actionPerformed(ActionEvent arg0) {
			setMeAsLastGraphicManager();
			if (!beforeClearBaselineRoute(getCurrentFrame())) return;
			if (isDocumentActive())
				getCurrentFrame().doBaselineDialog(false);
		}
		protected boolean allowed(boolean enable) {
			if (enable==false) return true;
			return isDocumentWritable();
		}
	}

	public class ToggleProgressLineAction extends MenuActionsMap.DocumentMenuAction {
		private static final long serialVersionUID = 1L;
		public void actionPerformed(ActionEvent arg0) {
			setMeAsLastGraphicManager();
			if (!beforeToggleRoute(ACTION_TOGGLE_PROGRESS_LINE)) return;
			if (!isActiveGanttView())
				return;
			var ganttView = getCurrentFrame().getGanttView();
			ganttView.setProgressLineEnabled(!ganttView.isProgressLineEnabled());
			syncGanttViewRibbonState();
		}
	}

	public class LabelResourceNamesAction extends MenuActionsMap.DocumentMenuAction {
		private static final long serialVersionUID = 1L;
		public void actionPerformed(ActionEvent arg0) {
			setMeAsLastGraphicManager();
			if (!beforeToggleRoute(ACTION_LABEL_RESOURCE_NAMES)) return;
			if (!isActiveGanttView())
				return;
			getCurrentFrame().getGanttView().setCurrentAnnotationFieldId(GanttView.ANNOTATION_FIELD_RESOURCE_NAMES);
			syncGanttViewRibbonState();
		}
	}

	public class LabelTaskNameAction extends MenuActionsMap.DocumentMenuAction {
		private static final long serialVersionUID = 1L;
		public void actionPerformed(ActionEvent arg0) {
			setMeAsLastGraphicManager();
			if (!beforeToggleRoute(ACTION_LABEL_TASK_NAME)) return;
			if (!isActiveGanttView())
				return;
			getCurrentFrame().getGanttView().setCurrentAnnotationFieldId(GanttView.ANNOTATION_FIELD_TASK_NAME);
			syncGanttViewRibbonState();
		}
	}
	
	public class LocaleAction extends MenuActionsMap.GlobalMenuAction {
		private static final long serialVersionUID = 1L;
		public void actionPerformed(ActionEvent arg0) {
			setMeAsLastGraphicManager();
			if (!beforeExternalRoute("locale")) return;
			Preferences pref = Preferences.userNodeForPackage(ConfigurationFile.class);
			String previousLocale = pref.get("locale", "default");
			LocaleDialog localeDialog = LocaleDialog.getInstance(getGraphicManager());
			if (!localeDialog.doModal()) {
				return;
			}
			String currentLocale = pref.get("locale", "default");
			if (Objects.equals(previousLocale, currentLocale)) {
				return;
			}
			Locale.setDefault(ConfigurationFile.getLocale(currentLocale));
			Messages.reset();
			StartupFactory startupFactory = getStartupFactory();
			if (startupFactory != null) {
				startupFactory.restart(GraphicManager.this);
			}
		}
	}

	public class LinkAction extends MenuActionsMap.DocumentMenuAction {
		private static final long serialVersionUID = 1L;
		public void actionPerformed(ActionEvent arg0) {
			setMeAsLastGraphicManager();
			if (isDocumentActive()) {
				if (!getCurrentFrame().hasTaskSelection(false, 2, true))
					return;
				if (beforeActionRoute("link"))
					return;
				getCurrentFrame().doLinkTasks();
			}
		}
	}
	public class UnlinkAction extends MenuActionsMap.DocumentMenuAction {
		private static final long serialVersionUID = 1L;
		public void actionPerformed(ActionEvent arg0) {
			setMeAsLastGraphicManager();
			if (isDocumentActive()) {
				if (!getCurrentFrame().hasTaskSelection(false, 1, true))
					return;
				if (beforeActionRoute("unlink"))
					return;
				getCurrentFrame().doUnlinkTasks();
			}
		}
	}
	public class ZoomInAction extends MenuActionsMap.DocumentMenuAction {
		private static final long serialVersionUID = 1L;
		public void actionPerformed(ActionEvent arg0) {
			setMeAsLastGraphicManager();
			if (isDocumentActive())
				getCurrentFrame().doZoomIn();
			setZoomButtons();

		}
	}
	public class ZoomOutAction extends MenuActionsMap.DocumentMenuAction {
		private static final long serialVersionUID = 1L;
		public void actionPerformed(ActionEvent arg0) {
			setMeAsLastGraphicManager();
			if (isDocumentActive())
				getCurrentFrame().doZoomOut();
			setZoomButtons();
		}
	}
	public class ScrollToTaskAction extends MenuActionsMap.DocumentMenuAction {
		private static final long serialVersionUID = 1L;
		public void actionPerformed(ActionEvent arg0) {
			setMeAsLastGraphicManager();
			if (isDocumentActive() && getCurrentFrame().hasTaskSelection(true, 1, false))
				getCurrentFrame().doScrollToTask();
		}
	}
	public class ExpandAction extends MenuActionsMap.DocumentMenuAction {
		private static final long serialVersionUID = 1L;
		public void actionPerformed(ActionEvent arg0) {
			setMeAsLastGraphicManager();
			if (isDocumentActive() && getCurrentFrame().hasTaskSelection(false, 1, false))
				getCurrentFrame().doExpand();
		}
		protected boolean allowed(boolean enable) {
			if (enable==false) return true;
			return isDocumentWritable();
		}
	}
	public class CollapseAction extends MenuActionsMap.DocumentMenuAction {
		private static final long serialVersionUID = 1L;
		public void actionPerformed(ActionEvent arg0) {
			setMeAsLastGraphicManager();
			if (isDocumentActive() && getCurrentFrame().hasTaskSelection(false, 1, false))
				getCurrentFrame().doCollapse();
		}
		protected boolean allowed(boolean enable) {
			if (enable==false) return true;
			return isDocumentWritable();
		}
	}

	public class IndentAction extends MenuActionsMap.DocumentMenuAction {
		private static final long serialVersionUID = 1L;
		public void actionPerformed(ActionEvent arg0) {
			setMeAsLastGraphicManager();
			if (isDocumentActive() && getCurrentFrame().hasTaskSelection(false, 1, false))
				getCurrentFrame().doIndent();
		}
		protected boolean allowed(boolean enable) {
			if (enable==false) return true;
			return isDocumentWritable();
		}
	}
	public class OutdentAction extends MenuActionsMap.DocumentMenuAction {
		private static final long serialVersionUID = 1L;
		public void actionPerformed(ActionEvent arg0) {
			setMeAsLastGraphicManager();
			if (isDocumentActive() && getCurrentFrame().hasTaskSelection(false, 1, false))
				getCurrentFrame().doOutdent();
		}
		protected boolean allowed(boolean enable) {
			if (enable==false) return true;
			return isDocumentWritable();
		}
	}
	public class MoveTaskUpAction extends MenuActionsMap.DocumentMenuAction {
		private static final long serialVersionUID = 1L;
		public void actionPerformed(ActionEvent event) {
			setMeAsLastGraphicManager();
			if (isDocumentActive() && getCurrentFrame().hasTaskSelection(false,1,false)) getCurrentFrame().doMoveSelectedTasks(-1);
		}
		protected boolean allowed(boolean enable) {
			return !enable || isDocumentWritable();
		}
	}
	public class MoveTaskDownAction extends MenuActionsMap.DocumentMenuAction {
		private static final long serialVersionUID = 1L;
		public void actionPerformed(ActionEvent event) {
			setMeAsLastGraphicManager();
			if (isDocumentActive() && getCurrentFrame().hasTaskSelection(false,1,false)) getCurrentFrame().doMoveSelectedTasks(1);
		}
		protected boolean allowed(boolean enable) {
			return !enable || isDocumentWritable();
		}
	}
	public class CutAction extends MenuActionsMap.DocumentMenuAction {
		private static final long serialVersionUID = 1L;
		public void actionPerformed(ActionEvent arg0) {
			setMeAsLastGraphicManager();
			if (isDocumentActive()){
				addHistory("doCut");
				getCurrentFrame().doCut();
			}
		}
	}
	public class CopyAction extends MenuActionsMap.DocumentMenuAction {
		private static final long serialVersionUID = 1L;
		public void actionPerformed(ActionEvent arg0) {
			setMeAsLastGraphicManager();
			if (isDocumentActive()){
				addHistory("doCopy");
				getCurrentFrame().doCopy();
			}
		}
	}
	public class PasteAction extends MenuActionsMap.DocumentMenuAction {
		private static final long serialVersionUID = 1L;
		public void actionPerformed(ActionEvent arg0) {
			setMeAsLastGraphicManager();
			if (isDocumentActive()){
				if (!getCurrentFrame().canPasteIntoCurrentSelection())
					return;
				if (beforeActionRoute("paste"))
					return;
				addHistory("doPaste");
				getCurrentFrame().doPaste();
			}
		}
		protected boolean allowed(boolean enable) {
			if (enable==false) return true;
			return isDocumentWritable();
		}
	}
	public class PasteInsertAction extends MenuActionsMap.DocumentMenuAction {
		private static final long serialVersionUID = 1L;
		public void actionPerformed(ActionEvent arg0) {
			setMeAsLastGraphicManager();
			if (isDocumentActive()) {
				if (!getCurrentFrame().canPasteIntoCurrentSelection())
					return;
				if (beforeActionRoute("pasteInsert"))
					return;
				addHistory("doPasteInsert");
				getCurrentFrame().doPasteInsert();
			}
		}
		protected boolean allowed(boolean enable) {
			if (enable==false) return true;
			return isDocumentWritable();
		}
	}

	public class DeleteAction extends MenuActionsMap.DocumentMenuAction {
		private static final long serialVersionUID = 1L;
		public void actionPerformed(ActionEvent arg0) {
			setMeAsLastGraphicManager();
			if (isDocumentActive())
				getCurrentFrame().doDelete();
		}
		protected boolean allowed(boolean enable) {
			if (enable==false) return true;
			return isDocumentWritable();
		}
	}

	public class ViewAction extends MenuActionsMap.DocumentMenuAction {
		private static final long serialVersionUID = 1L;
		private String viewName;
		public ViewAction(String viewName) {
			this.viewName = viewName;
		}
		public void actionPerformed(ActionEvent e) {
			setMeAsLastGraphicManager();
			if (getCurrentFrame() == null)
				return;
			if (!beforeViewSwitchRoute(viewName))
				return;
			setColorTheme(viewName);
			getCurrentFrame().activateView(viewName);
			setButtonState(null,currentFrame.getProject()); // disable buttons because no selection when first activated

		}
		public final String getViewName() {
			return viewName;
		}

	}

	public class TransformAction extends MenuActionsMap.DocumentMenuAction {
		private static final long serialVersionUID = 1L;
		private final int type;
		public TransformAction(int type) {
			this.type = type;
		}
		public void actionPerformed(ActionEvent e) {
			setMeAsLastGraphicManager();
			if (!isDocumentActive())
				return;
			String actionId = switch (type) {
			case TransformComboBoxModel.SORTER -> ACTION_CHOOSE_SORT;
			case TransformComboBoxModel.GROUPER -> ACTION_CHOOSE_GROUP;
			default -> ACTION_CHOOSE_FILTER;
			};
			if (!beforeChooserRoute(actionId))
				return;
	        CommonSpreadSheet spreadSheet=getCurrentFrame().getTopSpreadSheet();
	        if (spreadSheet!=null){
	            if (spreadSheet.isEditing())
	            	spreadSheet.getCellEditor().stopCellEditing();//.cancelCellEditing();
	            spreadSheet.clearSelection();
	        }
	        if (e.getSource() instanceof TransformComboBox combo) {
	        	combo.transformBasedOnValue();
	        	return;
	        }
	        showTransformChooser(type);
		}
	}


	public void closeApplication(){
		addHistory("closeApplication");
		updateStoredSession();
		quitting = true;
//		if (Environment.getStandAlone()) {
//			Frame frame=getFrame();
//			if (frame!=null)
//				frame.dispose();
//			System.exit(0);
//			return;
//		}

		(new Thread(){
			public void run(){
				JobRunnable exitRunnable=new JobRunnable("Local: closeProjects"){
					public Object run() throws Exception{
						Frame frame=getFrame();
						if (frame!=null) frame.dispose();
						System.exit(0);
		    	    	return null; //return not used anyway
					}
				};


				Job job=projectFactory.getPortfolio().getRemoveAllProjectsJob(exitRunnable,true,null);
				SessionFactory.getInstance().getLocalSession().schedule(job);

			}
		}).start();
	}

	public void initLayout() {
		getFrameManager().getWorkspace().setLayout(new BorderLayout());
	}
	public void initProject(){
		//projects loaded in doStartupAction
//        if (projectUrl == null && !GeneralOption.getInstance().isStartWithBlankProject()) {
//			//System.out.println("not opening anything");
//		} else if (projectUrl == null   || projectUrl.length==0 || projectUrl[0].startsWith("http")) { //same as in Main //$NON-NLS-1$
//			System.out.println("loading local project:" +projectUrl); //$NON-NLS-1$
//			boolean ok = loadLocalDocument(projectUrl[0],true); //if null then it will create a new project. WebStart will send a file name
//			if (!ok)
//				return;
//		}
////		else {
////			loadDownloadedDocument(); //not used anymore
////		}
		if (currentFrame != null)
			currentFrame.activateView(ACTION_GANTT);

	}

	void setEnabledDocumentMenuActions(boolean enable) {
		if (Environment.isPlugin()) return;
       actionsMap.setEnabledDocumentMenuActions(enable);
        if (getCurrentFrame() != null) {
        	getCurrentFrame().getFilterToolBarManager().setEnabled(enable);
        }
        if (topTabs != null)
        	topTabs.setTrackingEnabled(enable && isDocumentWritable());
		syncGanttViewRibbonState();
	}

	protected Document loadMasterProject() {
		return loadDocument(Session.MASTER,false,false);
	}
//	protected void loadDownloadedDocument(){
//		//showWaitCursor(true);
//
//		projectFactory.openDownloadedProject();
//		//showWaitCursor(false);
//	}
	public Document loadDocument(long id,boolean sync,boolean openAs){
		return loadDocument(id, sync, openAs, false, null);
	}
	protected Document loadDocument(long id,boolean sync,boolean openAs,boolean local){
		return loadDocument(id, sync, openAs, local, null);
	}
	protected Document loadDocument(long id,boolean sync,boolean openAs,Consumer<Object> endSwingClosure){
		return loadDocument(id, sync, openAs, false, endSwingClosure);
	}
	protected Document loadDocument(long id,boolean sync,boolean openAs,boolean local,Consumer<Object> endSwingClosure){
		addHistory("loadDocument", new Object[]{id,sync,openAs,endSwingClosure==null});
		//showWaitCursor(true);
		if (id==-1L)
			return null;
		ProjectFactory factory = projectFactory;
		factory.setServer(server);
		LoadOptions opt=new LoadOptions();
		opt.setId(id);
		opt.setLocal(local);
		opt.setSync(sync);
		opt.setOpenAs(openAs);
		opt.setEndSwingClosure(endSwingClosure);

		Document result = factory.openProject(opt);
		//showWaitCursor(false);
		return result;
	}
protected boolean loadLocalDocument(String fileName,boolean merge){ //uses server to merge
	addHistory("loadLocalDocument",new Object[]{fileName,merge});
		//showWaitCursor(true);
		Project project;
		if (fileName==null) {
			//System.out.println("creating empty project");
			project = projectFactory.createProject();

		} else {
			LoadOptions opt=ProjectLoadWorkflow.prepareLoadOptions(fileName, Environment.getStandAlone() || Environment.getUser() == null, getCollaborationUserKey());
			opt.setEndSwingClosure(new Consumer<Object>() { public void accept(Object arg0) {
					if (arg0 instanceof Project) {
						initializeCollaboration((Project)arg0);
					}
				}
			});

			if (merge) opt.setResourceMapping(new ResourceMappingForm(){
				public boolean execute(){
					if (getImportedResources().size() == 0) // don't show dialog if no resources were imported
						return true;
					if (resourceMappingDialog == null) {
						resourceMappingDialog = ResourceMappingDialog.getInstance(this);
						resourceMappingDialog.pack();
						resourceMappingDialog.setModal(true);
					} else resourceMappingDialog.setForm(this);
					resourceMappingDialog.bind(true);
					resourceMappingDialog.setLocationRelativeTo(getCurrentFrame());//to center on screen
					resourceMappingDialog.setVisible(true);
					return resourceMappingDialog.getDialogResult()==JOptionPane.OK_OPTION;
				}
			});

			project=projectFactory.openProject(opt);

		}
		//showWaitCursor(false);
		return project != null;
	}
	protected void saveLocalDocument(String fileName,final boolean saveAs){
		addHistory("saveLocalDocument",new Object[]{fileName,saveAs});
		//showWaitCursor(true);
		final DocumentFrame frame = getCurrentFrame();
		if (frame == null) {
			return;
		}
		final Project project=frame.getProject();
		final CollaborationSession collaborationSession = project.getCollaborationSession();
		SaveOptions opt = com.microproject.application.ProjectDocumentWorkflow.prepareSaveOptions(project, fileName, saveAs, true,
			collaborationSession,
			getCollaborationUserKey(),
			collaborationSession == null ? null : collaborationSession.getSidecarFileName(),
			new com.microproject.application.ProjectDocumentWorkflow.SaveCallbacks() {
				public void persistWorkspace(Project projectToPersist) {
					persistCollaborationWorkspace(projectToPersist);
				}

				public int resolveSaveDecision(Project projectToSave, CollaborationSession session) {
					if (!session.hasSaveConflicts()) {
						return CollaborationSession.SAVE_PROCEED;
					}
					return promptForCollaborationSave();
				}

				public String chooseSaveAsCopyFileName(Project projectToSave) {
					return SessionFactory.getInstance().getLocalSession().chooseFileName(true, projectToSave.getGuessedFileName());
				}

				public void afterSave(Project savedProject, boolean saveAsRequested, boolean fileNameChanged, boolean collaborationEnabled) {
					autoRecoveryManager.discard(savedProject);
					recentProjectStore.recordOpened(savedProject.getFileName());
					updateStoredSession();
					if (saveAsRequested) {
						frame.setId(savedProject.getUniqueId()+""); //$NON-NLS-1$
					}
					if (fileNameChanged && savedProject.getCollaborationSession() != null) {
						savedProject.getCollaborationSession().stop();
						savedProject.setCollaborationSession(null);
						initializeCollaboration(savedProject);
					}
					if (collaborationEnabled && savedProject.getCollaborationSession() != null) {
						savedProject.getCollaborationSession().afterSave();
						persistCollaborationWorkspace(savedProject);
					}
					refreshSaveStatus(true);
				}
			});
		if (opt == null) {
			return;
		}
		opt.setPreSaving(getSavingClosure());
		projectFactory.saveProject(project,opt);
		//showWaitCursor(false);
	}

	private Consumer<Object> getSavingClosure() {
		return null;
//		return new Consumer<Object>() {
//
//			public void accept(Object arg0) {
//				Project proj = (Project)arg0;
//				SpreadSheetFieldArray fieldArray = (SpreadSheetFieldArray) getCurrentFrame().getGanttView().getSpreadSheet().getFieldArray();
//				proj.getDocumentWorkspace().setSetting("fieldArray", fieldArray);
//			}
//
//		};
//
	}

	private Consumer<Object> getLoadClosure() {
		return null;
//		return new Consumer<Object>() {
//
//			public void accept(Object arg0) {
//				Project proj = (Project)arg0;
//				SpreadSheetFieldArray fieldArray = (SpreadSheetFieldArray) proj.getDocumentWorkspace().getSetting("fieldArray");
//				if (fieldArray != null)
//					getCurrentFrame().getGanttView().getSpreadSheet().setFieldArray(fieldArray);
//			}
//
//		};

	}
	protected void saveLocalDocument(Project project,String fileName){
		//showWaitCursor(true);
		CollaborationSession collaborationSession = project.getCollaborationSession();
		SaveOptions opt = com.microproject.application.ProjectDocumentWorkflow.prepareSaveOptions(project, fileName, false, true,
			collaborationSession,
			getCollaborationUserKey(),
			collaborationSession == null ? null : collaborationSession.getSidecarFileName(),
			new com.microproject.application.ProjectDocumentWorkflow.SaveCallbacks() {
				public void persistWorkspace(Project projectToPersist) {
					persistCollaborationWorkspace(projectToPersist);
				}

				public int resolveSaveDecision(Project projectToSave, CollaborationSession session) {
					if (!session.hasSaveConflicts()) {
						return CollaborationSession.SAVE_PROCEED;
					}
					return promptForCollaborationSave();
				}

				public String chooseSaveAsCopyFileName(Project projectToSave) {
					return SessionFactory.getInstance().getLocalSession().chooseFileName(true, projectToSave.getGuessedFileName());
				}

				public void afterSave(Project savedProject, boolean saveAsRequested, boolean fileNameChanged, boolean collaborationEnabled) {
					autoRecoveryManager.discard(savedProject);
					recentProjectStore.recordOpened(savedProject.getFileName());
					updateStoredSession();
					if (collaborationEnabled && savedProject.getCollaborationSession() != null) {
						savedProject.getCollaborationSession().afterSave();
					}
				}
			});
		if (opt == null) {
			return;
		}
		opt.setPreSaving(getSavingClosure());
	    projectFactory.saveProject(project,opt);
		//showWaitCursor(false);
	}

	protected void closeProject(Project project){
		autoRecoveryManager.discard(project);
		persistCollaborationWorkspace(project);
		if (project.getCollaborationSession() != null) {
			project.getCollaborationSession().stop();
			project.setCollaborationSession(null);
		}
		projectFactory.removeProject(project,true,true,true);
	}

	private int promptForCollaborationSave() {
		Object[] options = new Object[] {
			"Restore and Save",
			"Discard My Changes",
			"Save Copy"
		};
		int result = PopupDialogSupport.showOptionDialog(getCurrentFrame(),
			"One or more tasks you are editing were changed or deleted externally.\nChoose how to resolve the conflict.",
			"ProjectLibre",
			JOptionPane.DEFAULT_OPTION,
			JOptionPane.WARNING_MESSAGE,
			null,
			options,
			options[0],
			JOptionPane.CANCEL_OPTION);
		if (result == 0) {
			return CollaborationSession.SAVE_PROCEED;
		}
		if (result == 2) {
			return CollaborationSession.SAVE_AS_COPY;
		}
		return CollaborationSession.SAVE_CANCEL;
	}

	public void openLocalProject(){
		String fileName=SessionFactory.getInstance().getLocalSession().chooseFileName(false,null);
		if (fileName!=null) loadLocalDocument(fileName,!Environment.getStandAlone());
	}

	public void saveLocalProject(boolean saveAs){
		String fileName=null;
		Project project=getCurrentFrame().getProject();
		if (!saveAs){
			fileName=project.getFileName();
		}
		if (fileName==null) fileName=SessionFactory.getInstance().getLocalSession().chooseFileName(true,project.getGuessedFileName());
		if (fileName!=null) saveLocalDocument(fileName,saveAs);
	}


    public void showAboutDialog() {
    	if (aboutDialog == null) {
    		aboutDialog = AboutDialog.getInstance(getFrame());
    		aboutDialog.pack();
    		aboutDialog.setModal(true);
    	}
    	aboutDialog.setLocationRelativeTo(getFrame());//to center on screen
    	aboutDialog.setVisible(true);
    }

    public void showHelpDialog(/*DocumentFrame documentFrame*/) {
    	if (helpDialog == null) {
    		helpDialog = HelpDialog.getInstance(getFrame());
    		helpDialog.pack();
    		helpDialog.setModal(true);
    	}
    	helpDialog.setLocationRelativeTo(getFrame());//to center on screen
    	helpDialog.setVisible(true);
    }


/**
 * Show or focus the assignment dialog.  If showing, initilize to project
 * @param project
 */
    public void showAssignmentDialog(DocumentFrame documentFrame) {
		if (currentFrame==null||!getCurrentFrame().isActive())
			return;

    	if (assignResourcesDialog == null) {
    		assignResourcesDialog = new AssignmentDialog(documentFrame);
    		assignResourcesDialog.pack();
    		assignResourcesDialog.setModal(false);
    	}
    	assignResourcesDialog.setLocationRelativeTo(documentFrame);//to center on screen
        assignResourcesDialog.setVisible(true);
    }

    public void showTimesheetDialog(DocumentFrame documentFrame) {
		if (documentFrame == null || documentFrame.getProject() == null)
			return;

		documentFrame.finishAnyOperations();
		TimesheetDialog dialog = new TimesheetDialog(documentFrame, getSelectedResourcesForTimesheet(documentFrame));
		dialog.pack();
		dialog.setLocationRelativeTo(documentFrame);
		dialog.setVisible(true);
    }

    private List getSelectedResourcesForTimesheet(DocumentFrame documentFrame) {
		ArrayList selectedResources = new ArrayList();
		List selectedImpls = documentFrame.getSelectedImpls(false);
		for (int i = 0; i < selectedImpls.size(); i++) {
			Object impl = selectedImpls.get(i);
			if (impl instanceof Resource)
				selectedResources.add(impl);
		}
		return selectedResources;
    }


	void doCalendarOptionsDialog() {
		finishAnyOperations();
		CalendarOption calendarOption = CalendarOption.getInstance();
		CalendarDialogBox dialog = CalendarDialogBox.getInstance(getFrame(), calendarOption);
		if (dialog.doModal()) {
			dialog.getForm().copyToOption(calendarOption);
		}
	}

	private GanttView getActiveGanttView() {
		if (!isActiveGanttView() || currentFrame == null)
			return null;
		return currentFrame.getGanttView();
	}

	private void showTransformChooser(int type) {
		if (!isDocumentActive())
			return;
		String actionId = switch (type) {
		case TransformComboBoxModel.SORTER -> ACTION_CHOOSE_SORT;
		case TransformComboBoxModel.GROUPER -> ACTION_CHOOSE_GROUP;
		default -> ACTION_CHOOSE_FILTER;
		};
		TransformComboBox combo = new TransformComboBox(getMenuManager(), actionId, type);
		combo.setView(com.microproject.grouping.core.transform.ViewConfiguration.getView(getTopViewId()));
		if (combo.getItemCount() == 0)
			return;
		if (combo.getSelectedIndex() < 0)
			combo.setSelectedIndex(0);
		int choice = PopupDialogSupport.showConfirmDialog(getFrame(), combo,
				getMenuManager().getString(actionId + ".text"), JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, JOptionPane.CANCEL_OPTION);
		if (choice == JOptionPane.OK_OPTION) {
			combo.transformBasedOnValue();
		}
	}

	private void showTimescaleChooser() {
		GanttView ganttView = getActiveGanttView();
		if (ganttView == null)
			return;
		MenuManager manager = getMenuManager();
		int scaleCount = ganttView.getScaleCount();
		String[] labels = new String[scaleCount];
		for (int i = 0; i < scaleCount; i++) {
			labels[i] = manager.getString("RibbonTimescale.text") + " " + (i + 1);
		}
		JComboBox<String> combo = new JComboBox<>(labels);
		combo.setSelectedIndex(ganttView.getScale());
		int choice = PopupDialogSupport.showConfirmDialog(getFrame(), combo,
				manager.getString("RibbonTimescale.text"), JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, JOptionPane.CANCEL_OPTION);
		if (choice == JOptionPane.OK_OPTION) {
			ganttView.setScale(combo.getSelectedIndex());
			setZoomButtons();
		}
	}

	public void showBarStyleChooser() {
		GanttView ganttView = getActiveGanttView();
		if (ganttView == null)
			return;
		MenuManager manager = getMenuManager();
		String[] styles = { "standard", "Tracking" };
		JComboBox<String> combo = new JComboBox<>(styles);
		combo.setSelectedItem(ganttView.getCurrentBarStyleName());
		int choice = PopupDialogSupport.showConfirmDialog(getFrame(), combo,
				manager.getString("RibbonBarStyles.text"), JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, JOptionPane.CANCEL_OPTION);
		if (choice == JOptionPane.OK_OPTION) {
			ganttView.setBarStyles((String) combo.getSelectedItem());
			ganttView.getGantt().repaint();
		}
	}

	private void toggleGridlines() {
		GanttView ganttView = getActiveGanttView();
		if (ganttView == null)
			return;
		ganttView.setSpreadsheetGridVisible(!ganttView.isSpreadsheetGridVisible());
	}

	private void showTextStyleChooser() {
		GanttView ganttView = getActiveGanttView();
		if (ganttView == null)
			return;
		MenuManager manager = getMenuManager();
		String[] labels = {
			manager.getString("RibbonLabelResourceNames.text"),
			manager.getString("RibbonLabelTaskName.text")
		};
		JComboBox<String> combo = new JComboBox<>(labels);
		combo.setSelectedIndex(ganttView.isTaskNameAnnotationSelected() ? 1 : 0);
		int choice = PopupDialogSupport.showConfirmDialog(getFrame(), combo,
				manager.getString("RibbonTextStyles.text"), JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, JOptionPane.CANCEL_OPTION);
		if (choice == JOptionPane.OK_OPTION) {
			ganttView.setCurrentAnnotationFieldId(combo.getSelectedIndex() == 1
				? GanttView.ANNOTATION_FIELD_TASK_NAME
				: GanttView.ANNOTATION_FIELD_RESOURCE_NAMES);
			syncGanttViewRibbonState();
		}
	}

	private void showLayoutChooser() {
		GanttView ganttView = getActiveGanttView();
		if (ganttView == null)
			return;
		MenuManager manager = getMenuManager();
		int scaleCount = ganttView.getScaleCount();
		String[] scaleLabels = new String[scaleCount];
		for (int i = 0; i < scaleCount; i++) {
			scaleLabels[i] = manager.getString("RibbonTimescale.text") + " " + (i + 1);
		}
		JComboBox<String> timescaleCombo = new JComboBox<>(scaleLabels);
		timescaleCombo.setSelectedIndex(ganttView.getScale());
		JComboBox<String> barStyleCombo = new JComboBox<>(new String[] { "standard", "Tracking" });
		barStyleCombo.setSelectedItem(ganttView.getCurrentBarStyleName());
		JComboBox<String> labelCombo = new JComboBox<>(new String[] {
			manager.getString("RibbonLabelResourceNames.text"),
			manager.getString("RibbonLabelTaskName.text")
		});
		labelCombo.setSelectedIndex(ganttView.isTaskNameAnnotationSelected() ? 1 : 0);
		JCheckBox progressLine = new JCheckBox(manager.getString("RibbonToggleProgressLine.text"), ganttView.isProgressLineEnabled());
		JCheckBox gridlines = new JCheckBox(manager.getString("RibbonGridlines.text"), ganttView.isSpreadsheetGridVisible());

		JPanel panel = new JPanel(new GridLayout(0, 1, 0, 8));
		panel.setBorder(new EmptyBorder(8, 8, 8, 8));
		panel.add(new JLabel(manager.getString("RibbonTimescale.text")));
		panel.add(timescaleCombo);
		panel.add(new JLabel(manager.getString("RibbonBarStyles.text")));
		panel.add(barStyleCombo);
		panel.add(new JLabel(manager.getString("RibbonTextStyles.text")));
		panel.add(labelCombo);
		panel.add(progressLine);
		panel.add(gridlines);

		int choice = PopupDialogSupport.showConfirmDialog(getFrame(), panel,
				manager.getString("RibbonLayout.text"), JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, JOptionPane.CANCEL_OPTION);
		if (choice == JOptionPane.OK_OPTION) {
			ganttView.setScale(timescaleCombo.getSelectedIndex());
			ganttView.setBarStyles((String) barStyleCombo.getSelectedItem());
			ganttView.setCurrentAnnotationFieldId(labelCombo.getSelectedIndex() == 1
				? GanttView.ANNOTATION_FIELD_TASK_NAME
				: GanttView.ANNOTATION_FIELD_RESOURCE_NAMES);
			ganttView.setProgressLineEnabled(progressLine.isSelected());
			ganttView.setSpreadsheetGridVisible(gridlines.isSelected());
			ganttView.getGantt().repaint();
			setZoomButtons();
			syncGanttViewRibbonState();
		}
	}



	void print(){
		GraphPageable document=PrintDocumentFactory.getInstance().createDocument(getCurrentFrame(),true,false);
		if (document!=null) document.print();
	}


	void printPreview(){
		GraphPageable document=PrintDocumentFactory.getInstance().createDocument(getCurrentFrame(),false,false);
		if (document!=null) document.preview();
	}

	void savePDF() {
		GraphPageable document=PrintDocumentFactory.getInstance().createDocument(getCurrentFrame(),false,false);
		try {
			Class generator=ClassLoaderUtils.forName("org.projectlibre.export.ImageExport"); //claur
			generator.getMethod("export", new Class[]{GraphPageable.class,Component.class}).invoke(null,new Object[]{document,getContainer()});
		} catch (Exception e) {
			logger.log(Level.WARNING, "Failed to export PDF", e);
		}
	}


	public DocumentFrame getCurrentFrame() {
		return currentFrame;
	}

	public Frame getFrame(){
		return frame;
	}

	public Container getContainer() {
		return container;
	}

	public JobQueue getJobQueue(){
		if (jobQueue==null){
			jobQueue=new JobQueue("GraphicManager",false); //$NON-NLS-1$
		}
		return jobQueue;
	}


	public boolean isDocumentActive() {
		return currentFrame != null && currentFrame.isActive();
	}
	public boolean isDocumentWritable() {
		return currentFrame != null && currentFrame.isActive() && !currentFrame.getProject().isReadOnly();
	}


	public void namedFrameActivated(NamedFrameEvent evt) {
//		System.out.println("Frame activated");
		NamedFrame frame = evt.getNamedFrame();
		if (frame instanceof DocumentFrame){
			DocumentFrame df=(DocumentFrame)frame;
			setCurrentFrame(df);

		}
	}
	public void namedFrameShown(NamedFrameEvent arg0) {
	}
	public void namedFrameTabShown(NamedFrameEvent evt) {
		NamedFrame frame = evt.getNamedFrame();
		if (frame instanceof DocumentFrame){
			DocumentFrame df=(DocumentFrame)frame;
			setCurrentFrame(df);

		}
	}
	public void windowActivated(WindowEvent arg0) {
	}
	public void windowClosed(WindowEvent evt) {
		if (evt.getWindow() == assignResourcesDialog)
			assignResourcesDialog = null;
	}

	public void windowClosing(WindowEvent arg0) {
	}

	public void windowDeactivated(WindowEvent arg0) {
	}

	public void windowDeiconified(WindowEvent arg0) {
	}

	public void windowIconified(WindowEvent arg0) {
	}

	public void windowStateChanged(WindowEvent arg0) {
	}

	protected boolean resourceType=false;
	protected boolean taskType=false;
	public void setTaskInformation(boolean taskType,boolean resourceType){
		this.taskType=taskType;
		this.resourceType=resourceType;
//		JButton button = null;
//		String infoText = "Task Information";
//		String notesText = "Task Notes";
//		String insertText = getMenuManager().getString(ACTION_INSERT_TASK + ButtonFactory.TOOLTIP_SUFFIX);
//		if (resourceType&&!taskType){
//			infoText = "Resource Information";
//			notesText = "Resource Notes";
//			insertText = "Insert Resource";
//		}
//		getMenuManager().setText(ACTION_INFORMATION,infoText);
//		getMenuManager().setText(ACTION_NOTES,notesText);
//		getMenuManager().setText(ACTION_INSERT_TASK,insertText);
	}


	public void setConnected(boolean connected){
		getMenuManager().setActionEnabled(ACTION_IMPORT_MSPROJECT,connected);
		getMenuManager().setActionEnabled(ACTION_OPEN_PROJECT,connected);
		getMenuManager().setActionEnabled(ACTION_RECENT_PROJECTS,connected);
		getMenuManager().setActionEnabled(ACTION_NEW_PROJECT,connected);
		if (connected) refreshSaveStatus(true);

	}

	Set getActionSet(){
		Set actions=null;
		DocumentFrame df=getCurrentFrame();
		if (df!=null){
			SpreadSheet sp=df.getActiveSpreadSheet();
			actions=new HashSet();
			if (sp!=null){
				String[] a=sp.getActionList();
				if (a!=null){
					for (int i=0;i<a.length;i++) actions.add(a[i]);
				}
			}
		}
		return actions;
	}

	void setButtonState(Object currentImpl, Project project) {
		Set actions=getActionSet();
		boolean infoEnabled = currentImpl != null && (currentImpl instanceof Assignment||currentImpl instanceof Task||currentImpl instanceof Resource);
		boolean taskInfoEnabled = currentImpl != null && (currentImpl instanceof Task || (currentImpl instanceof Assignment && taskType));
		boolean resourceInfoEnabled = currentImpl != null && (currentImpl instanceof Resource || (currentImpl instanceof Assignment && resourceType));
		boolean notVoid = currentImpl != null && !(currentImpl instanceof VoidNodeImpl);

		boolean readOnly = !isDocumentWritable();
		getMenuManager().setActionEnabled(ACTION_INFORMATION,infoEnabled);
		getMenuManager().setActionEnabled("RibbonTaskInformation", taskInfoEnabled);
		getMenuManager().setActionEnabled("RibbonResourceInformation", resourceInfoEnabled);
		getMenuManager().setActionEnabled(ACTION_NOTES,infoEnabled);
		getMenuManager().setActionEnabled(ACTION_INSERT_TASK, !readOnly && (taskType || resourceType)&&(actions==null||actions.contains(ACTION_INSERT_TASK)));
		getMenuManager().setActionEnabled(ACTION_INSERT_RESOURCE, !readOnly && resourceType && (actions==null||actions.contains(ACTION_INSERT_RESOURCE)));
		getMenuManager().setActionEnabled(ACTION_INSERT_RECURRING, !readOnly && taskType);
		getMenuManager().setActionEnabled(ACTION_CUT,!readOnly &&notVoid&&(actions==null||actions.contains(ACTION_CUT)));
		getMenuManager().setActionEnabled(ACTION_COPY,notVoid&&(actions==null||actions.contains(ACTION_COPY)));
		getMenuManager().setActionEnabled(ACTION_PASTE,!readOnly && (actions==null||actions.contains(ACTION_PASTE)));
		getMenuManager().setActionEnabled(ACTION_DELETE,!readOnly && (actions==null||actions.contains(ACTION_DELETE)));
		boolean isTask = currentImpl != null && currentImpl instanceof Task;
		boolean isResource = currentImpl != null && currentImpl instanceof Resource;
		boolean isHasStartAndEnd = currentImpl != null && currentImpl instanceof HasStartAndEnd;
		boolean writable = (currentImpl != null && !ClassUtils.isObjectReadOnly(currentImpl));
		getMenuManager().setActionEnabled(ACTION_INDENT,!readOnly &&(isTask || isResource)&&(actions==null||actions.contains(ACTION_INDENT)));
		getMenuManager().setActionEnabled(ACTION_OUTDENT,!readOnly &&(isTask || isResource)&&(actions==null||actions.contains(ACTION_OUTDENT)));
		getMenuManager().setActionEnabled(ACTION_MOVE_TASK_UP,!readOnly && isTask && currentFrame != null && currentFrame.canMoveSelectedTasks(-1));
		getMenuManager().setActionEnabled(ACTION_MOVE_TASK_DOWN,!readOnly && isTask && currentFrame != null && currentFrame.canMoveSelectedTasks(1));
		getMenuManager().setActionEnabled(ACTION_EXPAND,!readOnly && notVoid && (actions==null||actions.contains(ACTION_EXPAND)));
		getMenuManager().setActionEnabled(ACTION_COLLAPSE,!readOnly && notVoid && (actions==null||actions.contains(ACTION_COLLAPSE)));
		getMenuManager().setActionEnabled(ACTION_LINK,isTask);
		getMenuManager().setActionEnabled(ACTION_UNLINK,isTask);
		getMenuManager().setActionEnabled(ACTION_ASSIGN_RESOURCES,isTask && writable);
		getMenuManager().setActionEnabled(ACTION_TIMESHEET,!readOnly && project != null);
		getMenuManager().setActionEnabled(ACTION_LEVEL_RESOURCES,!readOnly && project != null);
		getMenuManager().setActionEnabled(ACTION_DELEGATE_TASKS,isTask && writable);
		getMenuManager().setActionEnabled(ACTION_UPDATE_TASKS,!readOnly && isTask);
		getMenuManager().setActionEnabled(ACTION_CALENDAR_OPTIONS,currentFrame != null);


		boolean insertProject = getCurrentFrame().isCurrentRowInMainProject();


//			taskType && (!notVoid || currentImpl == null || ((Task)currentImpl).getOwningProject() == null || ((Task)currentImpl).getOwningProject() == project);
		getMenuManager().setActionEnabled(ACTION_INSERT_PROJECT,!readOnly &&insertProject);

		BaseView view=null;
		DocumentFrame frame=getCurrentFrame();
		if (frame!=null){
			view=(BaseView)frame.getMainView().getTopComponent();
		}
		getMenuManager().setActionEnabled(ACTION_SCROLL_TO_TASK,isHasStartAndEnd&&view.canScrollToTask());
		boolean hasDocument = currentFrame != null;
		getMenuManager().setActionEnabled(ACTION_CHOOSE_FILTER, hasDocument);
		getMenuManager().setActionEnabled(ACTION_CHOOSE_SORT, hasDocument);
		getMenuManager().setActionEnabled(ACTION_CHOOSE_GROUP, hasDocument);

		if (currentFrame != null) {
			currentFrame.refreshUndoButtons();
			//refreshSaveStatus(false);
		}
		boolean printable = currentFrame!= null && currentFrame.isPrintable();
		getMenuManager().setActionEnabled(ACTION_PRINT,printable);
		getMenuManager().setActionEnabled(ACTION_PRINT_PREVIEW,printable);

		setZoomButtons();

		Field f = FieldDictionary.getInstance().getActionField(ACTION_DOCUMENTS);
		getMenuManager().setActionVisible(ACTION_DOCUMENTS,currentFrame != null && f != null);
		getMenuManager().setActionEnabled(ACTION_DOCUMENTS,currentFrame != null && isEnabledFieldAction(ACTION_DOCUMENTS,  currentFrame.getProject()));


	}

	public void setZoomButtons() {
		getMenuManager().setActionEnabled(ACTION_ZOOM_IN,currentFrame != null && currentFrame.canZoomIn());
		getMenuManager().setActionEnabled(ACTION_ZOOM_OUT,currentFrame != null && currentFrame.canZoomOut());

	}

	private boolean isActiveGanttView() {
		if (currentFrame == null)
			return false;
		String topViewId = currentFrame.getTopViewId();
		return ACTION_GANTT.equals(topViewId) || ACTION_TRACKING_GANTT.equals(topViewId);
	}

	private void syncGanttViewRibbonState() {
		boolean ganttActive = isActiveGanttView();
		getMenuManager().setActionEnabled(ACTION_TOGGLE_PROGRESS_LINE, ganttActive);
		getMenuManager().setActionEnabled(ACTION_LABEL_RESOURCE_NAMES, ganttActive);
		getMenuManager().setActionEnabled(ACTION_LABEL_TASK_NAME, ganttActive);
		getMenuManager().setActionEnabled(ACTION_BAR, ganttActive);
		getMenuManager().setActionEnabled(ACTION_TIMESCALE, ganttActive);
		getMenuManager().setActionEnabled(ACTION_GRIDLINES, ganttActive);
		getMenuManager().setActionEnabled(ACTION_TEXT_STYLES, ganttActive);
		getMenuManager().setActionEnabled(ACTION_BAR_STYLES, ganttActive);
		getMenuManager().setActionEnabled(ACTION_LAYOUT, ganttActive);

		boolean progressSelected = false;
		boolean resourceLabelSelected = false;
		boolean taskLabelSelected = false;
		boolean gridlinesSelected = false;
		if (ganttActive) {
			GanttView ganttView = currentFrame.getGanttView();
			progressSelected = ganttView.isProgressLineEnabled();
			resourceLabelSelected = ganttView.isResourceNameAnnotationSelected();
			taskLabelSelected = ganttView.isTaskNameAnnotationSelected();
			gridlinesSelected = ganttView.isSpreadsheetGridVisible();
		}
		getMenuManager().setActionSelected(ACTION_TOGGLE_PROGRESS_LINE, progressSelected);
		getMenuManager().setActionSelected(ACTION_LABEL_RESOURCE_NAMES, resourceLabelSelected);
		getMenuManager().setActionSelected(ACTION_LABEL_TASK_NAME, taskLabelSelected);
		getMenuManager().setActionSelected(ACTION_GRIDLINES, gridlinesSelected);
	}
	/**
	 * React to selection changed events and forward them on to any bottom window
	 */
	protected Node lastNode=null;
	public void selectionChanged(SelectionNodeEvent e) {
		if (assignResourcesDialog != null)
			assignResourcesDialog.selectionChanged(e);

		Node currentNode=e.getCurrentNode();
		Object currentImpl=currentNode.getImpl();
		setButtonState(currentImpl,currentFrame.getProject());
		// if on resource view, hide task info and vice versa.  Otherwise just show it
		if (lastNode!=null&&taskInformationDialog!=null&&(lastNode.getImpl() instanceof Task||lastNode.getImpl() instanceof Assignment)&&currentNode.getImpl() instanceof Resource){
			taskInformationDialog.setVisible(false);
			doInformationDialog(false);
		} else if (lastNode!=null&&resourceInformationDialog!=null&&lastNode.getImpl() instanceof Resource&&(currentNode.getImpl() instanceof Task||currentNode.getImpl() instanceof Assignment)){
			resourceInformationDialog.setVisible(false);
			doInformationDialog(false);
		}else{
			if (taskInformationDialog != null)
				taskInformationDialog.selectionChanged(e);
			if (resourceInformationDialog != null)
				resourceInformationDialog.selectionChanged(e);
		}
		lastNode=currentNode;
	}

	void refreshSaveStatus(boolean isSaving) {
		getMenuManager().setActionEnabled(ACTION_SAVE_PROJECT,currentFrame != null && !isSaving && currentFrame.getProject().needsSaving());
		setTitle(isSaving);

		FrameManager dm=getFrameManager();
		if (dm!=null) dm.update(); //update project combo
	}

	public void objectChanged(ObjectEvent objectEvent) {

		if (objectEvent.getObject() instanceof Project) {
			Project project = (Project)objectEvent.getObject();
			if (objectEvent.isCreate()) {
				if (project.isOpenedAsSubproject())
					closeProjectFrame(project); // because it's now in a project
				else {
					DocumentFrame f = addProjectFrame(project);
				}

			} else if (objectEvent.isDelete()) {
				closeProjectFrame(project);
			}
			if (projectInformationDialog != null)
				projectInformationDialog.objectChanged(objectEvent);
			if (taskInformationDialog != null)
				taskInformationDialog.objectChanged(objectEvent);
			if (resourceInformationDialog != null)
				resourceInformationDialog.objectChanged(objectEvent);

		}
	}


	/**
	 * @return Returns the menuManager.
	 */
	public MenuManager getMenuManager() {
		if (menuManager == null) {
			menuManager = MenuManager.getInstance(this);
			addHandlers();
		}

		return menuManager;
	}

	public void finishAnyOperations() {
		if (getCurrentFrame() != null)
			getCurrentFrame().finishAnyOperations();
	}
	public void showWaitCursor(boolean show) {
		Frame frame=getFrame();
		if (frame==null) return;
		if (show)
			frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		else
			frame.setCursor(Cursor.getDefaultCursor());
	}


	public final ProjectFactory getProjectFactory() {
		return projectFactory;
	}


    public String getTopViewId() {
    	if (getCurrentFrame() == null)
    		return ACTION_GANTT;
    	else
    		return getCurrentFrame().getTopViewId();
    }


    public boolean isApplet() {
    	return false;
		//return container instanceof Applet;
    }
    
	public void setRibbon(MainRibbonFrame frame, MenuManager menuManger){
		frame.setRibbonPanel(menuManger.createRibbonPanel(MenuManager.STANDARD_RIBBON, this::showHelpDialog));
    }
    
    
//    public void addProjectTab(String projectName){
//    	if (!(container instanceof JRibbonFrame))
//    		return;
//    	JRibbonFrame frame=(JRibbonFrame)container;
//    	JRibbon ribbon=frame.getRibbon();
//    	
//    	ribbon.getFileSelector().addTab("projectName", new JLabel());
//    }
//
//    public void removeProjectTab(String projectName){
//    	if (!(container instanceof JRibbonFrame))
//    		return;
//    	JRibbonFrame frame=(JRibbonFrame)container;
//    	JRibbon ribbon=frame.getRibbon();
//    	
//    	ribbon.getFileSelector().removeTabAt(index)("projectName", new JLabel());
//    }
//
//    public void selectProjectTab(String projectName){
//    	if (!(container instanceof JRibbonFrame))
//    		return;
//    	JRibbonFrame frame=(JRibbonFrame)container;
//    	JRibbon ribbon=frame.getRibbon();
//    	
//    	ribbon.getFileSelector().addTab("projectName", new JLabel());
//    }

    
    
    public void setToolBarAndMenus(final Container contentPane) {
    	if (Environment.isRibbonUI()){
			if (ProjectLibreShell.showRestartMessageIfNeeded(contentPane, Environment.isNeedToRestart())) {
				return;
			}
			ProjectLibreShell.installRibbonShell((MainRibbonFrame) container, getMenuManager(), this::showHelpDialog,
				autoRecoveryManager);
    	} else if (Environment.isNewLook()) {
			if (ProjectLibreShell.showRestartMessageIfNeeded(contentPane, Environment.isNeedToRestart())) {
				return;
			}
			ProjectLibreShell.ShellHandles handles = ProjectLibreShell.installNewLookShell(
				contentPane,
				getMenuManager(),
				getLafManager(),
				getFrameManager(),
				isApplet(),
				Environment.isExternal(),
				Environment.getStandAlone(),
				Environment.isMac(),
				menuBar -> ((JFrame) container).setJMenuBar(menuBar));
			topTabs = handles.getTopTabs();
			projectListMenu = handles.getProjectListMenu();
		} else {
			ProjectLibreShell.ShellHandles handles = ProjectLibreShell.installClassicShell(
				contentPane,
				getMenuManager(),
				Environment.getStandAlone(),
				Environment.isMac(),
				menuBar -> ((JFrame) container).setJMenuBar(menuBar));
			filterToolBarManager = handles.getFilterToolBarManager();
			projectListMenu = handles.getProjectListMenu();
		}

		//accelerators
		    addCtrlAccel(KeyEvent.VK_G, ACTION_GOTO, null);
		    addCtrlAccel(KeyEvent.VK_L, ACTION_GOTO, null);
		    addCtrlAccel(KeyEvent.VK_F, ACTION_FIND, null);
		    addCtrlAccel(KeyEvent.VK_Z, ACTION_UNDO, null);			//- Sanhita
		    addCtrlAccel(KeyEvent.VK_Y, ACTION_REDO, null);
		    addCtrlAccel(KeyEvent.VK_N, ACTION_NEW_PROJECT, null);
		    addCtrlAccel(KeyEvent.VK_O, ACTION_OPEN_PROJECT, null);
		    addCtrlAccel(KeyEvent.VK_S, ACTION_SAVE_PROJECT, null);
		    addCtrlAccel(KeyEvent.VK_P, ACTION_PRINT, null);			//-Sanhita
		    addCtrlAccel(KeyEvent.VK_I, ACTION_INSERT_TASK, null);
		    addCtrlAccel(KeyEvent.VK_PERIOD, ACTION_INDENT, null);
		    addCtrlAccel(KeyEvent.VK_COMMA, ACTION_OUTDENT, null);
		    addCtrlAccel(KeyEvent.VK_PLUS, ACTION_EXPAND, new ExpandAction());
		    addCtrlAccel(KeyEvent.VK_ADD, ACTION_EXPAND, new ExpandAction());
		    addCtrlAccel(KeyEvent.VK_EQUALS, ACTION_EXPAND, new ExpandAction());
		    addCtrlAccel(KeyEvent.VK_MINUS, ACTION_COLLAPSE, new CollapseAction());
		    addCtrlAccel(KeyEvent.VK_SUBTRACT, ACTION_COLLAPSE, new CollapseAction());

			// To force a recalculation. This normally shouldn't be needed.
		    addCtrlAccel(KeyEvent.VK_R, ACTION_RECALCULATE, new RecalculateAction());
    }

    private void addCtrlAccel(int vk, String actionConstant, Action action) {
		RootPaneContainer root = (RootPaneContainer)container;
		InputMap inputMap = root.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);

		KeyStroke key = KeyStroke.getKeyStroke(vk, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()); // use the platform shortcut modifier on all supported JDKs.
		inputMap.put(key, actionConstant);
		if (action == null)
			action = menuManager.getActionFromId(actionConstant);
		root.getRootPane().getActionMap().put(actionConstant, action);
	}
    private LookAndFeel getPlaf() {
    	return getLafManager().getPlaf();
    }

    public void invalidate() {
    	container.invalidate();
    	((RootPaneContainer)container).getContentPane().invalidate();
    	((RootPaneContainer)container).getContentPane().repaint();
    }

    public void initLookAndFeel() {
   		getLafManager().initLookAndFeel();

    }


	private HashMap colorThemes = null;
	public HashMap getColorThemes() {
		if (colorThemes == null) {
			colorThemes = new HashMap();
			colorThemes.put(ACTION_GANTT,"Bloody Moon"); //$NON-NLS-1$
			colorThemes.put(ACTION_TRACKING_GANTT,"Mahogany"); //$NON-NLS-1$
			colorThemes.put(ACTION_NETWORK,"Emerald Grass"); //$NON-NLS-1$
			colorThemes.put(ACTION_RESOURCES,"Blue Yonder"); //$NON-NLS-1$
			colorThemes.put(ACTION_PROJECTS,"Emerald Grass"); //$NON-NLS-1$
			colorThemes.put(ACTION_WBS,"Sepia"); //$NON-NLS-1$
			colorThemes.put(ACTION_RBS,"Steel Blue"); //$NON-NLS-1$
			colorThemes.put(ACTION_REPORT,"Aqua"); //$NON-NLS-1$
			colorThemes.put(ACTION_TASK_USAGE_DETAIL,"Brown Velvet"); //$NON-NLS-1$
			colorThemes.put(ACTION_RESOURCE_USAGE_DETAIL,"Earth Fresco"); //$NON-NLS-1$
		}
		return colorThemes;
	}

	public void setPaletteText(String themeName){
		getMenuManager().setText(ACTION_PALETTE,themeName);
	}

	void setColorTheme(String viewName){
		getLafManager().setColorTheme(viewName);
	}

	public class PaletteAction extends MenuActionsMap.GlobalMenuAction {
		private static final long serialVersionUID = 1L;
		public void actionPerformed(ActionEvent arg0) {
			setMeAsLastGraphicManager();

			getLafManager().changePalette();

		}
		protected boolean allowed(boolean enable){
			LookAndFeel lookAndFeel = UIManager.getLookAndFeel();
			return getLafManager().isChangePaletteAllowed(lookAndFeel);
		}

	}

	public class LookAndFeelAction extends MenuActionsMap.GlobalMenuAction {
		private static final long serialVersionUID = 1L;
		public void actionPerformed(ActionEvent arg0) {
			setMeAsLastGraphicManager();

		}
	}

	public class FullScreenAction extends MenuActionsMap.GlobalMenuAction {
		private static final long serialVersionUID = 1L;
		public void actionPerformed(ActionEvent arg0) {
//			setMeAsLastGraphicManager();
//			encodeWorkspace(); // so new window takes this one's preferences
//			// see http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=5029025
//			try {
//				Class cl=Class.forName("netscape.javascript.JSObject");
//				Object win=cl.getMethod("getWindow", new Class[]{Applet.class}).invoke(null, new Object[]{container});
		}
	}
	public class RefreshAction extends MenuActionsMap.GlobalMenuAction {
		private static final long serialVersionUID = 1L;
		public void actionPerformed(ActionEvent arg0) {
			setMeAsLastGraphicManager();
			getStartupFactory().restart(GraphicManager.this);
		}
	}

/**
 * Decode the current workspace (currently using XML though could be binary)
 * @return workspace object decoded from lastWorkspace static
 */
	private Workspace decodeWorkspaceXML() {
		ByteArrayInputStream stream = new ByteArrayInputStream(((String)lastWorkspace).getBytes());
		XMLDecoder decoder = new XMLDecoder(new BufferedInputStream(stream));
		Workspace workspace = (Workspace) decoder.readObject();
		decoder.close();
		return workspace;
	}
	private Workspace decodeWorkspaceBinary() {
        ByteArrayInputStream bin=new ByteArrayInputStream((byte[]) lastWorkspace);
        ObjectInputStream in;
		try {
			in = SafeObjectInput.create(bin);
	        return (Workspace) in.readObject();
		} catch (IOException e) {
			logger.log(Level.WARNING, "Failed to decode binary workspace", e);
		} catch (ClassNotFoundException e) {
			logger.log(Level.WARNING, "Failed to decode binary workspace", e);
		}
		return null;
	}
	public Workspace decodeWorkspace() {
		if (lastWorkspace == null)
			return null;
		return BINARY_WORKSPACE ? decodeWorkspaceBinary() : decodeWorkspaceXML();
	}

/**
 * Encode the current workspace and store it off in lastWorkspace.
 * Currently I use an XML format for easier debugging. It could be serialized as binary as well since
 * all objects in the graph implement Serializable
 *
 */
	private void encodeWorkspaceXML() {
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		XMLEncoder encoder = new XMLEncoder(new BufferedOutputStream(stream));
		encoder.writeObject(createWorkspace(SavableToWorkspace.VIEW));
		encoder.close();
		lastWorkspace = stream.toString();
//		System.out.println(lastWorkspace);
	}
	private void encodeWorkspaceBinary() {
        ByteArrayOutputStream bout=new ByteArrayOutputStream();
        ObjectOutputStream out;
		try {
			out = new ObjectOutputStream(bout);
	        out.writeObject(createWorkspace(SavableToWorkspace.VIEW));
	        out.close();
	    	bout.close();
	    	lastWorkspace = bout.toByteArray();
		} catch (IOException e) {
			logger.log(Level.WARNING, "Failed to encode workspace", e);
		}

	}
	public void encodeWorkspace() {
		if (BINARY_WORKSPACE)
			encodeWorkspaceBinary();
		else
			encodeWorkspaceXML();
	}


	public void restoreWorkspace(WorkspaceSetting w, int context) {
		Workspace ws = (Workspace) w;
		colorThemes = ws.colorThemes;
		getFrameManager().restoreWorkspace(ws.frames, context);
	}

	public WorkspaceSetting createWorkspace(int context) {
		Workspace ws = new Workspace();
		ws.colorThemes = getColorThemes();
		ws.frames = getFrameManager().createWorkspace(context);
		return ws;
	}

	public static class Workspace implements WorkspaceSetting   {
		private static final long serialVersionUID = -6606344141026658401L;
		private HashMap colorThemes;
		WorkspaceSetting frames;
		public HashMap getColorThemes() {
			return colorThemes;
		}
		public void setColorThemes(HashMap colorThemes) {
			this.colorThemes = colorThemes;
		}
		public WorkspaceSetting getFrames() {
			return frames;
		}
		public void setFrames(WorkspaceSetting frames) {
			this.frames = frames;
		}
	}

	public static final Object getLastWorkspace() {
		return lastWorkspace;
	}


	public GraphicManager getGraphicManager() {
		return this;
	}


	public void setGraphicManager(GraphicManager manager) {
	}

	public FrameManager getFrameManager() {
		return frameManager;
	}

	public void setFrameManager(FrameManager frameManager) {
		this.frameManager = frameManager;
	}

	public void initView() {
		Container c=container;
		if (container!=null && container instanceof RootPaneContainer){
			c=((RootPaneContainer)container).getContentPane();
		}
        if (!Environment.isRibbonUI()) c.setLayout(new BorderLayout());
        JPanel panel = new JPanel();
        c.add(panel, "Center"); //$NON-NLS-1$
        setFrameManager(new DefaultFrameManager(container, panel,this));

		initLayout();

		if (!Environment.isPlugin()) setToolBarAndMenus(c);

        setEnabledDocumentMenuActions(false);
    	Workspace workspace = decodeWorkspace();
        if (workspace != null) {
        	restoreWorkspace(workspace, SavableToWorkspace.VIEW);

        } else
        	initProject();
//        container.invalidate();
 	}

	public BaselineDialog getBaselineDialog() {
		return baselineDialog;
	}

	public void setBaselineDialog(BaselineDialog baselineDialog) {
		this.baselineDialog = baselineDialog;
	}
	public StartupFactory getStartupFactory() {
		return startupFactory;
	}
	public void setStartupFactory(StartupFactory startupFactory) {
		this.startupFactory = startupFactory;
	}

	public boolean isEditingMasterProject() {
		Project currentProject=currentFrame.getProject();
		if (currentProject == null)
			return false;
		return currentProject.isMaster() && !currentProject.isReadOnly();

	}

	public GlobalPreferences getPreferences(){
		if (preferences==null) {
			preferences=new GlobalPreferences();
			if (Environment.isExternal())
				preferences.setShowProjectResourcesOnly(true);
		}
		return preferences;
	}


	//for AssignmentDialog
	private ResourceInTeamFilter assignmentDialogTransformerInitializationClosure;
	public Consumer<Object> setAssignmentDialogTransformerInitializationClosure(){
		return new Consumer<Object>() { public void accept(Object arg) {
				ViewTransformer transformer=(ViewTransformer)arg;
		        NodeFilter hiddenFilter=transformer.getHiddenFilter();
		        if (hiddenFilter!=null&& hiddenFilter instanceof ResourceInTeamFilter){
		        	assignmentDialogTransformerInitializationClosure=(ResourceInTeamFilter)hiddenFilter;
		        	assignmentDialogTransformerInitializationClosure.setFilterTeam(getGraphicManager().getPreferences().isShowProjectResourcesOnly());
		        }else assignmentDialogTransformerInitializationClosure=null;
			}
		};
	}
	public ResourceInTeamFilter getAssignmentDialogTransformerInitializationClosure() {
		return assignmentDialogTransformerInitializationClosure;
	}
	public FilterToolBarManager getFilterToolBarManager() {
		return filterToolBarManager;
	}

	boolean initialized=false;
	private Mutex initializing=new Mutex();
	public void beginInitialization(){
		showWaitCursor(true);
		initializing.lock();
	}
	public void finishInitialization(){
		container.setVisible(true);
		initialized=true;
		initializing.unlock();
		showWaitCursor(false);
	}
	public void waitInitialization(){
		initializing.waitUntilUnlocked();
	}

	/**
	 * Methods that are called using reflection to save workspace stuff into project
	 * @return
	 */
	public static SpreadSheetFieldArray getCurrentFieldArray() {
		return (SpreadSheetFieldArray) getDocumentFrameInstance().getGanttView().getSpreadSheet().getFieldArrayWithWidths(getDocumentFrameInstance().getGanttColumns());
	}
	public static void setCurrentFieldArray(Object fieldArray) {
		getDocumentFrameInstance().getGanttView().getSpreadSheet().setFieldArrayWithWidths((SpreadSheetFieldArray)fieldArray);
	}

	public static UndoController getUndoController(){
		DocumentFrame frame=GraphicManager.getDocumentFrameInstance();
		if (frame==null) return null;
		return frame.getUndoController();
	}
	public void setAllButResourceDisabled(boolean disable) {
		if (topTabs!=null) topTabs.setAllButResourceDisabled(disable);
	}
	public void doFind(Searchable searchable, Field field) {
		doFind(searchable, field, null);
	}
	public class TimelineAction extends MenuActionsMap.DocumentMenuAction {
		private static final long serialVersionUID = 1L;
		public void actionPerformed(ActionEvent event) {
			setMeAsLastGraphicManager();
			if (isDocumentActive()) getCurrentFrame().doTimelineDialog();
		}
	}
	public class CalendarViewAction extends MenuActionsMap.DocumentMenuAction {
		private static final long serialVersionUID = 1L;
		public void actionPerformed(ActionEvent event) {
			setMeAsLastGraphicManager();
			if (isDocumentActive()) getCurrentFrame().doCalendarViewDialog();
		}
	}
	public class CustomReportAction extends MenuActionsMap.DocumentMenuAction {
		private static final long serialVersionUID = 1L;
		public void actionPerformed(ActionEvent event) {
			setMeAsLastGraphicManager();
			if (isDocumentActive()) getCurrentFrame().doCustomReportDialog();
		}
	}
	public void doFind(Searchable searchable, Field field, String initialQuery) {
		if (currentFrame==null||!getCurrentFrame().isActive())
			return;
		if (searchable == null)
			return;
		if (!beforeFindRoute(searchable, field))
			return;
		currentFrame.doFind(searchable, field, initialQuery);

	}


    public void registerForMacOSXEvents() {

		if (Desktop.isDesktopSupported()) { //Mac OS X
			Desktop desktop = Desktop.getDesktop();

			// Check if AboutHandler is supported
			if (desktop.isSupported(Desktop.Action.APP_ABOUT)) {
				desktop.setAboutHandler(new AboutHandler() {
					@Override
					public void handleAbout(AboutEvent e) {
						showAboutDialog();
					}
				});
			}
			if (desktop.isSupported(Desktop.Action.APP_QUIT_HANDLER)) {
				desktop.setQuitHandler(new QuitHandler() {
					@Override
					public void handleQuitRequestWith(QuitEvent e, QuitResponse response){
                        try {
                            boolean continueQuit=quitApplication();
							if (continueQuit) response.performQuit();
							else response.cancelQuit();
                        } catch (Exception ex) {
                            throw new RuntimeException(ex);
                        }
                    }
				});
			}

			if (Environment.getStandAlone() &&
			desktop.isSupported(Desktop.Action.APP_OPEN_FILE)) {
				desktop.setOpenFileHandler(new OpenFilesHandler() {
					@Override
					public void openFiles(OpenFilesEvent e){
						if (e.getFiles()!=null && !e.getFiles().isEmpty()) {
							openFile(e.getFiles().getFirst().getPath());
						}
					}
				});
			}

		}
    }

    protected String lastFileName;
    public void openFile(String fileName){
    	lastFileName=fileName;
    	if (fileName!=null&&initialized) loadLocalDocument(fileName,!Environment.getStandAlone());
    }

	public String getLastFileName() {
		return lastFileName;
	}

	public boolean quitApplication() throws Exception{
		updateStoredSession();
		quitting = true;
		for (Object frameObj : new ArrayList(frameList)) {
			if (frameObj instanceof DocumentFrame) {
				Project project = ((DocumentFrame)frameObj).getProject();
				persistCollaborationWorkspace(project);
				if (project != null && project.getCollaborationSession() != null) {
					project.getCollaborationSession().stop();
				}
			}
		}
		final boolean[] lock=new boolean[]{false};

		JobRunnable exitRunnable=new JobRunnable("Local: closeProjects"){
			public Object run() throws Exception{
				synchronized (lock) {
					lock[0]=true;
					lock.notifyAll();
				}
    	    	return null;
			}
		};
		final boolean[] closeStatus=new boolean[]{false};
		final Job job=projectFactory.getPortfolio().getRemoveAllProjectsJob(exitRunnable,false,closeStatus);
		SessionFactory.getInstance().getLocalSession().schedule(job);

		synchronized(lock){
			while (!lock[0]){
				try{
						lock.wait();
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						logger.log(Level.FINE, "Interrupted while waiting for projects to close", e);
						return false;
					}
			}
		}
		if (closeStatus[0]){
			Frame frame=getFrame();
			if (frame!=null) frame.dispose();
			//System.exit(0);
			return true;
		}else return false;
	}


	public static Project getProject() {
		if (lastGraphicManager == null)
			return null;
		if (lastGraphicManager.currentFrame==null)
			return null;
		return lastGraphicManager.currentFrame.getProject();
	}

	public void addHistory(String command,Object[] args){
		history.add(new CommandInfo(command,args));
	}
	public void addHistory(String command){
		history.add(new CommandInfo(command,null));
	}
	public static List<CommandInfo> getHistory() {
		if (lastGraphicManager == null)
			return null;
		return lastGraphicManager.history;
	}

	Project loadRecoveryDocument(com.microproject.application.AutoRecoveryStore.Entry entry) {
		LoadOptions options = ProjectLoadWorkflow.prepareLoadOptions(entry.snapshot().toString(), true,
			getCollaborationUserKey());
		Project project = projectFactory.openProject(options);
		if (project != null) {
			project.setFileName(entry.originalFileName());
			project.setGroupDirty(true);
			if (entry.originalFileName() != null) {
				initializeCollaboration(project);
			}
		}
		return project;
	}

	boolean offerRecoveryAtStartup() {
		return autoRecoveryManager.offerRecoveryAtStartup();
	}

}

