/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2012-2019 ProjectLibre, Inc.  (Previous Copyright Holder)
 * Copyright (c) 2026 microProject
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *******************************************************************************/
package com.microproject.dialog;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.microproject.dialog.util.FieldComponentMap;
import com.microproject.menu.MenuActionConstants;
import com.microproject.pm.graphic.frames.DocumentFrame;
import com.microproject.pm.graphic.frames.DocumentSelectedEvent;
import com.microproject.pm.graphic.frames.GraphicManager;
import com.microproject.pm.graphic.gantt.Gantt;
import com.microproject.pm.graphic.gantt.GanttRenderer;
import com.microproject.graphic.configuration.GanttBarFormatOverrides.BarFormat;
import com.microproject.pm.graphic.spreadsheet.SpreadSheet;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetUtils;
import com.microproject.association.InvalidAssociationException;
import com.microproject.datatype.Duration;
import com.microproject.datatype.DurationFormat;
import com.microproject.graphic.configuration.SpreadSheetCategories;
import com.microproject.graphic.configuration.shape.Colors;
import com.microproject.pm.assignment.Assignment;
import com.microproject.pm.assignment.AssignmentEntry;
import com.microproject.pm.dependency.Dependency;
import com.microproject.pm.dependency.DependencyService;
import com.microproject.pm.dependency.DependencyType;
import com.microproject.pm.key.HasId;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.ProjectFactory;
import com.microproject.pm.task.SubProj;
import com.microproject.pm.task.Task;
import com.microproject.strings.Messages;
import com.microproject.util.Alert;
import com.microproject.util.FlatUiSupport;
import com.microproject.ui.shell.WindowBoundsSupport;
/**
 *
 */
public class TaskInformationDialog extends InformationDialog {
	private static final long serialVersionUID = 1L;

	public static TaskInformationDialog getInstance(Frame owner, Task task, boolean notes) {
		return new TaskInformationDialog(owner, task, notes);
	}

	private TaskInformationDialog(Frame owner, Task task, boolean notes) {
		super(owner, Messages.getString("TaskInformationDialog.TaskInformation")); //$NON-NLS-1$
		setObjectClass(Task.class);
		setObject(task);
		addDocHelp("Task_Information_Dialog");
		}

	private JTabbedPane taskTabbedPane;
	private int notesTabIndex;
	private int resourcesTabIndex;

	@Override
	public void setLocationRelativeTo(Component reference) {
		super.setLocationRelativeTo(reference);
		WindowBoundsSupport.fitWithinUsableScreen(this);
	}

	// Bar color fields shown in the General tab (issue #16)
	private TaskGeneralPanel generalPanel;
	private TaskTextStylePanel textStylePanel;
	private TaskDependencyPanel predecessorsPanel;
	private TaskDependencyPanel successorsPanel;
	private TaskResourcesPanel resourcesPanel;

	private Gantt getGantt() {
		try {
			GraphicManager manager = GraphicManager.getInstance(this);
			DocumentFrame frame = manager == null ? null : manager.getCurrentFrame();
			if (frame == null)
				return null;
			return frame.getGanttView().getGantt();
		} catch (Exception e) {
			return null;
		}
	}

	private BarFormat currentBarFormat(Task task) {
		Gantt gantt = getGantt();
		if (gantt == null || task == null)
			return BarFormat.automatic();
		return gantt.getBarFormat(task);
	}

	private void applyBarFormatFromFields() {
		Task task = (Task) getObject();
		if (generalPanel != null)
			generalPanel.apply(task, getGantt());
	}

	private void refreshBarColorFields() {
		Task task = (Task) getObject();
		if (task == null || generalPanel == null)
			return;
		generalPanel.refresh(currentBarFormat(task), task.isReadOnly());
	}

	public void setObject(Object object) {
		super.setObject(object);
		String title = Messages.getString("TaskInformationDialog.TaskInformation");
		if (object != null)
			title += " - " + ((HasId)object).getId();
		this.setTitle(title);
	}
	public JComponent createContentPanel() {	
	    	
		// Keep the dialog within a normal desktop viewport.  Every tab receives a
		// real scroll viewport, so locale/DPI-specific preferred heights do not
		// overlap controls or push the dialog beyond the screen.
		FormLayout layout = new FormLayout("430dlu:grow", "fill:pref:grow"); //$NON-NLS-1$ //$NON-NLS-2$
		DefaultFormBuilder builder = new DefaultFormBuilder(layout);
		builder.setDefaultDialogBorder();
		CellConstraints cc = new CellConstraints();
		
		taskTabbedPane= new JTabbedPane();
		FlatUiSupport.styleTabbedPane(taskTabbedPane);
		taskTabbedPane.addTab(Messages.getString("TaskInformationDialog.General"),scrollableTab(createGeneralPanel())); //$NON-NLS-1$
		taskTabbedPane.addTab(Messages.getString("TaskInformationDialog.TextStyle"),scrollableTab(createTextStylePanel())); //$NON-NLS-1$
		taskTabbedPane.addTab(Messages.getString("TaskInformationDialog.Predecessors"),scrollableTab(createPredecessorsPanel())); //$NON-NLS-1$
		taskTabbedPane.addTab(Messages.getString("TaskInformationDialog.Successors"),scrollableTab(createSuccessorsPanel())); //$NON-NLS-1$
		String resources = Messages.getString("TaskInformationDialog.Resources"); //$NON-NLS-1$
		taskTabbedPane.addTab(resources,scrollableTab(createResourcesPanel()));
		resourcesTabIndex = taskTabbedPane.indexOfTab(resources);

		taskTabbedPane.addTab(Messages.getString("TaskInformationDialog.Advanced"),scrollableTab(createAdvancedPanel())); //$NON-NLS-1$
		taskTabbedPane.addTab(Messages.getString("TaskInformationDialog.Diagnostics"), scrollableTab(createDiagnosticsPanel())); //$NON-NLS-1$
		
		String notes = Messages.getString("TaskInformationDialog.Notes"); //$NON-NLS-1$
		taskTabbedPane.addTab(notes,scrollableTab(createNotesPanel()));
		notesTabIndex = taskTabbedPane.indexOfTab(notes);
		builder.add(taskTabbedPane);
		mainComponent = taskTabbedPane;

		return builder.getPanel();
	}

	private JComponent scrollableTab(JComponent contents) {
		JScrollPane scrollPane;
		JComponent viewportContent;
		if (contents instanceof JScrollPane existingScrollPane) {
			scrollPane = existingScrollPane;
			viewportContent = existingScrollPane.getViewport().getView() instanceof JComponent view ? view : contents;
		} else {
			scrollPane = new JScrollPane(contents,
					JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
			scrollPane.setBorder(null);
			viewportContent = contents;
		}
		// Use the available monitor height without letting this form make the
		// dialog taller than the desktop. The complete form remains scrollable.
		int viewportHeight = preferredViewportHeight(viewportContent);
		scrollPane.setPreferredSize(new Dimension(700, viewportHeight));
		scrollPane.setMinimumSize(new Dimension(480, Math.min(300, viewportHeight)));
		return scrollPane;
	}

	private int preferredViewportHeight(JComponent contents) {
		if (GraphicsEnvironment.isHeadless())
			return 460;
		GraphicsConfiguration configuration = getGraphicsConfiguration();
		if (configuration == null && owner != null)
			configuration = owner.getGraphicsConfiguration();
		if (configuration == null)
			return 460;
		Insets screenInsets = Toolkit.getDefaultToolkit().getScreenInsets(configuration);
		int usableHeight = configuration.getBounds().height - screenInsets.top - screenInsets.bottom;
		return preferredViewportHeight(contents.getPreferredSize().height, usableHeight);
	}

	static int preferredViewportHeight(int contentHeight, int usableHeight) {
		int baseHeight = 460;
		// Reserve room for the title bar, tab strip, dialog buttons, and borders.
		// The extra margin covers font/DPI rounding in the native window insets.
		int maxViewportHeight = Math.max(120, usableHeight - 160);
		int desiredHeight = Math.max(baseHeight, contentHeight);
		return Math.min(desiredHeight, maxViewportHeight);
	}

	private JComponent createTextStylePanel() {
		textStylePanel = new TaskTextStylePanel(this, createMap(), (Task)getObject());
		return textStylePanel.component();
	}

	private JComponent createDiagnosticsPanel() {
		return new TaskDiagnosticsPanel((Task) getObject());
	}

	public void showNotes() {
		taskTabbedPane.setSelectedIndex(notesTabIndex);
	}
	public void showResources() {
		taskTabbedPane.setSelectedIndex(resourcesTabIndex);
	}

	protected JComponent createHeaderFieldsPanel(FieldComponentMap map) {
		// Repeat of fields from general tab 
		FormLayout layout = new FormLayout(
		        "p,3dlu,300dlu" //$NON-NLS-1$
				,"p"); //$NON-NLS-1$
		DefaultFormBuilder builder = new DefaultFormBuilder(layout);
		map.append(builder,"Field.name"); //$NON-NLS-1$
		return builder.getPanel();
	}
	

	private JComponent createGeneralPanel(){
		FieldComponentMap map = createMap();
		Task task = (Task) getObject();
		Gantt gantt = getGantt();
		GanttRenderer.DisplayedBarColors displayed = gantt == null
				? new GanttRenderer.DisplayedBarColors(null, null, null) : gantt.getDisplayedBarColors(task);
		generalPanel = new TaskGeneralPanel(this, map, task, currentBarFormat(task), displayed);
		return generalPanel.component();
	}

	private JComponent createAdvancedPanel(){
		FieldComponentMap map = createMap();
		return new TaskAdvancedPanel(map, createHeaderFieldsPanel(map)).component();
	}	
	
	public JComponent createPredecessorsPanel() {
		return createDependencyPanel(true);
	}

	private JComponent createDependencyPanel(boolean predecessors) {
		FieldComponentMap map = createMap();
		TaskDependencySpreadsheet.Direction direction = predecessors
				? TaskDependencySpreadsheet.Direction.PREDECESSORS
				: TaskDependencySpreadsheet.Direction.SUCCESSORS;
		JScrollPane spreadsheetPane = predecessors ? createPredecessorsSpreadsheet() : createSuccessorsSpreadsheet();
		SpreadSheet spreadsheet = predecessors ? predecessorsSpreadSheet : successorsSpreadSheet;
		TaskDependencyPanel panel = new TaskDependencyPanel(direction, createHeaderFieldsPanel(map), spreadsheetPane,
				spreadsheet, () -> addDependency(predecessors), () -> removeSelectedDependencies(predecessors),
				() -> getObject() instanceof Task task && !task.isReadOnly());
		if (predecessors)
			predecessorsPanel = panel;
		else
			successorsPanel = panel;
		return panel;
	}
	
	protected SpreadSheet predecessorsSpreadSheet;
 	public static final String DEPENDENCY_SPREADSHEET=SpreadSheetCategories.dependencySpreadsheetCategory;
	protected JScrollPane createPredecessorsSpreadsheet() {
		predecessorsSpreadSheet = TaskDependencySpreadsheet.create(this,
				TaskDependencySpreadsheet.Direction.PREDECESSORS, (Task) object);
	    return SpreadSheetUtils.makeSpreadsheetScrollPane(predecessorsSpreadSheet);

    }
    //cache reconstructed because the main cache holding edges isn't ordered
    protected void updatePredecessorsSpreadsheet() {
		TaskDependencySpreadsheet.update((TaskDependencySpreadsheet) predecessorsSpreadSheet,
				(Task) object, TaskDependencySpreadsheet.Direction.PREDECESSORS);
    }

	public JComponent createSuccessorsPanel() {
		return createDependencyPanel(false);
	}
	
	protected SpreadSheet successorsSpreadSheet;
    protected JScrollPane createSuccessorsSpreadsheet() {
		successorsSpreadSheet = TaskDependencySpreadsheet.create(this,
				TaskDependencySpreadsheet.Direction.SUCCESSORS, (Task) object);
	    return SpreadSheetUtils.makeSpreadsheetScrollPane(successorsSpreadSheet);

    }

	private void addDependency(boolean predecessors) {
		Task task = (Task) getObject();
		if (task == null || task.isReadOnly())
			return;
		List<Task> candidates = getLinkableTasks(task, predecessors);
		if (candidates.isEmpty())
			return;
		List<TaskDependencyChoices.Choice> choices = new ArrayList<>(candidates.size());
		for (Task candidate : candidates)
			choices.add(new TaskDependencyChoices.Choice(candidate));
		TaskDependencyChoices.Choice selected = (TaskDependencyChoices.Choice) JOptionPane.showInputDialog(this,
				Messages.getString(predecessors ? "TaskInformationDialog.Predecessors" : "TaskInformationDialog.Successors"), //$NON-NLS-1$ //$NON-NLS-2$
				Messages.getString("Text.TaskDependency"), JOptionPane.PLAIN_MESSAGE, null,
				choices.toArray(), choices.get(0)); //$NON-NLS-1$
		if (selected == null)
			return;
		DependencyTypeChoice type = (DependencyTypeChoice) JOptionPane.showInputDialog(this,
				Messages.getString("Text.Type"), Messages.getString("Text.TaskDependency"), //$NON-NLS-1$ //$NON-NLS-2$
				JOptionPane.PLAIN_MESSAGE, null, dependencyTypeChoices(), dependencyTypeChoices()[0]);
		if (type == null)
			return;
		Long lag = chooseDependencyLag();
		if (lag == null)
			return;
		try {
			createDependency(task, selected.task(), predecessors, type.kind, lag.longValue(), this);
			updateAll();
		} catch (InvalidAssociationException e) {
			Alert.warn(e.getMessage(), this);
		}
	}

	static Dependency createDependency(Task task, Task selectedTask, boolean predecessors, Object eventSource)
			throws InvalidAssociationException {
		return createDependency(task, selectedTask, predecessors, DependencyType.Kind.FS.code(), eventSource);
	}

	/** Collect lag/lead while the user creates a link, rather than requiring a later edit. */
	private Long chooseDependencyLag() {
		String value = (String) JOptionPane.showInputDialog(this,
				Messages.getString("Text.Lag"), Messages.getString("Text.TaskDependency"),
				JOptionPane.PLAIN_MESSAGE, null, null, "0");
		if (value == null)
			return null;
		try {
			Duration duration = (Duration) DurationFormat.getSignedInstance().parseObject(value.trim());
			if (duration == null)
				throw new ParseException(value, 0);
			return Long.valueOf(duration.getEncodedMillis());
		} catch (ParseException e) {
			Alert.warn(Messages.getString("Message.invalidDuration"), this);
			return null;
		}
	}

	static Dependency createDependency(Task task, Task selectedTask, boolean predecessors, int dependencyType,
			Object eventSource) throws InvalidAssociationException {
		return createDependency(task, selectedTask, predecessors, dependencyType, 0L, eventSource);
	}

	static Dependency createDependency(Task task, Task selectedTask, boolean predecessors, int dependencyType, long lag,
			Object eventSource) throws InvalidAssociationException {
		return createDependency(task, selectedTask, predecessors, DependencyType.Kind.fromCode(dependencyType), lag, eventSource);
	}

	static Dependency createDependency(Task task, Task selectedTask, boolean predecessors, DependencyType.Kind dependencyType, long lag,
			Object eventSource) throws InvalidAssociationException {
		return DependencyService.getInstance().newDependency(
				predecessors ? selectedTask : task,
				predecessors ? task : selectedTask,
				dependencyType, lag, eventSource);
	}

	static DependencyTypeChoice[] dependencyTypeChoices() {
		return new DependencyTypeChoice[] {
			new DependencyTypeChoice(DependencyType.Kind.FS), new DependencyTypeChoice(DependencyType.Kind.SS),
			new DependencyTypeChoice(DependencyType.Kind.FF), new DependencyTypeChoice(DependencyType.Kind.SF) };
	}

	private List<Task> getLinkableTasks(Task task, boolean predecessors) {
		List<Project> projects = new ArrayList<>();
		if (task.getProject() != null)
			projects.add(task.getProject());
		for (Task candidate : task.getProject() == null ? List.<Task>of() : task.getProject().getTaskList()) {
			if (candidate instanceof SubProj subproject && subproject.getSubproject() != null
					&& !projects.contains(subproject.getSubproject()))
				projects.add(subproject.getSubproject());
		}
		ProjectFactory.getInstance().getPortfolio().forProjects(project -> {
			if (!projects.contains(project))
				projects.add(project);
		});
		GraphicManager manager = GraphicManager.getInstance(this);
		if (manager != null) {
			for (Project project : manager.getOpenProjects()) {
				if (!projects.contains(project))
					projects.add(project);
			}
		}
		return TaskDependencyChoices.linkableTasks(task, predecessors, projects);
	}

	static final class DependencyTypeChoice {
		private final DependencyType.Kind kind;

		DependencyTypeChoice(DependencyType.Kind kind) {
			this.kind = kind;
		}

		@Override
		public String toString() {
			return DependencyType.toLongString(kind.code()) + " (" + kind.name() + ')';
		}
	}

	private void removeSelectedDependencies(boolean predecessors) {
		SpreadSheet spreadsheet = predecessors ? predecessorsSpreadSheet : successorsSpreadSheet;
		if (spreadsheet == null || spreadsheet.getSelectedRowCount() == 0)
			return;
		// Use the existing Delete action so multi-selection, confirmation,
		// collaboration locks, and undo behave identically to the Delete key.
		spreadsheet.executeAction(MenuActionConstants.ACTION_DELETE);
		updateAll();
	}

	private void updateDependencyPanel(TaskDependencyPanel panel, boolean predecessors) {
		Task task = (Task) getObject();
		if (panel != null)
			panel.setAddEnabled(task != null && !task.isReadOnly()
					&& !getLinkableTasks(task, predecessors).isEmpty());
	}
    //cache reconstructed because the main cache holding edges isn't ordered
    protected void updateSuccessorsSpreadsheet() {
		TaskDependencySpreadsheet.update((TaskDependencySpreadsheet) successorsSpreadSheet,
				(Task) object, TaskDependencySpreadsheet.Direction.SUCCESSORS);
    }

	public JComponent createResourcesPanel() {
		FieldComponentMap map = createMap();
		resourcesPanel = new TaskResourcesPanel(this, createHeaderFieldsPanel(map), getAssignResourceButton(),
				(Task) object);
		return resourcesPanel;
	}

	public void updateAll() {
		activateListeners();
		super.updateAll();
		// This dialog instance is reused for different tasks. Keep the custom bar
		// controls in sync just like the FieldComponentMap-backed controls; this
		// also discards unconfirmed edits when Cancel refreshes the dialog.
		refreshBarColorFields();
		refreshTextStyleFields();
		if (predecessorsSpreadSheet != null)
			updatePredecessorsSpreadsheet();
		if (successorsSpreadSheet != null)
			updateSuccessorsSpreadsheet();
		updateDependencyPanel(predecessorsPanel, true);
		updateDependencyPanel(successorsPanel, false);
		if (resourcesPanel != null)
			resourcesPanel.update((Task) object);
	}

	@Override
	protected boolean bind(boolean get) {
		if (!super.bind(get))
			return false;
		Task task = (Task) getObject();
		if (task == null)
			return true;
		if (get) {
			refreshBarColorFields();
			refreshTextStyleFields();
		} else {
			// Commit bar color changes when the user confirms the dialog.
			applyBarFormatFromFields();
			applyFontColorFromField();
		}
		return true;
	}

	private void refreshTextStyleFields() {
		if (textStylePanel != null)
			textStylePanel.refresh((Task)getObject());
	}

	private void applyFontColorFromField() {
		if (textStylePanel != null)
			textStylePanel.applyFontColor((Task)getObject(), this);
	}

	public void documentSelected(DocumentSelectedEvent evt) {
		if (resourcesPanel != null)
			resourcesPanel.selectDocument(evt.getCurrent());
	}
	
	
	protected void activateListeners() {
		super.activateListeners();
		if (predecessorsSpreadSheet != null)
			predecessorsSpreadSheet.getCache().setReceiveEvents(true);
		if (successorsSpreadSheet != null)
			successorsSpreadSheet.getCache().setReceiveEvents(true);
	}

	protected void desactivateListeners() {
		super.desactivateListeners();
		if (predecessorsSpreadSheet != null)
			predecessorsSpreadSheet.getCache().setReceiveEvents(false);
		if (successorsSpreadSheet != null)
			successorsSpreadSheet.getCache().setReceiveEvents(false);
	}


	protected boolean hasHelpButton() {
		return true;
	}

	
}
