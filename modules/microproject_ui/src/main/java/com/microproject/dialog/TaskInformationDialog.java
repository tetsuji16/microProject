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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.microproject.dialog.util.FieldComponentMap;
import com.microproject.help.HelpUtil;
import com.microproject.menu.MenuActionConstants;
import com.microproject.pm.graphic.frames.DocumentFrame;
import com.microproject.pm.graphic.frames.DocumentSelectedEvent;
import com.microproject.pm.graphic.frames.GraphicManager;
import com.microproject.pm.graphic.gantt.Gantt;
import com.microproject.pm.graphic.gantt.GanttRenderer;
import com.microproject.graphic.configuration.GanttBarFormatOverrides.BarFormat;
import com.microproject.pm.graphic.spreadsheet.SpreadSheet;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetUtils;
import com.microproject.association.AssociationList;
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
		GraphicsConfiguration configuration = getGraphicsConfiguration();
		if (configuration == null || GraphicsEnvironment.isHeadless())
			return;
		Rectangle monitor = configuration.getBounds();
		Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(configuration);
		int left = monitor.x + insets.left;
		int top = monitor.y + insets.top;
		int right = monitor.x + monitor.width - insets.right;
		int bottom = monitor.y + monitor.height - insets.bottom;
		Rectangle constrained = constrainToUsableBounds(getBounds(),
				new Rectangle(left, top, right - left, bottom - top));
		setLocation(constrained.x, constrained.y);
	}

	static Rectangle constrainToUsableBounds(Rectangle dialog, Rectangle usable) {
		int maxX = Math.max(usable.x, usable.x + usable.width - dialog.width);
		int maxY = Math.max(usable.y, usable.y + usable.height - dialog.height);
		int x = Math.max(usable.x, Math.min(dialog.x, maxX));
		int y = Math.max(usable.y, Math.min(dialog.y, maxY));
		return new Rectangle(x, y, dialog.width, dialog.height);
	}

	// Bar color fields shown in the General tab (issue #16)
	private TaskGeneralPanel generalPanel;
	private TaskTextStylePanel textStylePanel;

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
		JPanel panel = new JPanel(new BorderLayout(0, 4));
		JPanel header = new JPanel(new BorderLayout(0, 4));
		header.add(createHeaderFieldsPanel(map), BorderLayout.NORTH);
		JPanel actions = new JPanel(new BorderLayout(8, 0));
		actions.setOpaque(false);
		actions.add(new JLabel(Messages.format("Format.label", Messages.getString(
				predecessors ? "Spreadsheet.Dependency.predecessors" : "Spreadsheet.Dependency.successors"))), //$NON-NLS-1$ //$NON-NLS-2$
				BorderLayout.WEST);
		actions.add(getDependencyButtons(predecessors), BorderLayout.EAST);
		header.add(actions, BorderLayout.SOUTH);
		panel.add(header, BorderLayout.NORTH);
		panel.add(predecessors ? createPredecessorsSpreadsheet() : createSuccessorsSpreadsheet(), BorderLayout.CENTER);
		// Empty dependency tables report a zero preferred width.  Without a
		// minimum content width the outer scroll pane lays out the header at
		// width 0, making New/Remove buttons disappear or paint clipped.
		panel.setPreferredSize(new Dimension(700, 420));
		panel.setMinimumSize(new Dimension(480, 300));
		HelpUtil.addDocHelp(panel, "Linking"); //$NON-NLS-1$
		return panel;
	}
	
	protected SpreadSheet predecessorsSpreadSheet;
	private JButton newPredecessorsButton;
	private JButton removePredecessorsButton;
 	public static final String DEPENDENCY_SPREADSHEET=SpreadSheetCategories.dependencySpreadsheetCategory;
    protected JScrollPane createPredecessorsSpreadsheet() {
		predecessorsSpreadSheet = TaskDependencySpreadsheet.create(this,
				TaskDependencySpreadsheet.Direction.PREDECESSORS, (Task) object);
		installRemoveDependencyButtonState(predecessorsSpreadSheet, true);
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
	private JButton newSuccessorsButton;
	private JButton removeSuccessorsButton;
    protected JScrollPane createSuccessorsSpreadsheet() {
		successorsSpreadSheet = TaskDependencySpreadsheet.create(this,
				TaskDependencySpreadsheet.Direction.SUCCESSORS, (Task) object);
		installRemoveDependencyButtonState(successorsSpreadSheet, false);

	    return SpreadSheetUtils.makeSpreadsheetScrollPane(successorsSpreadSheet);

    }

	private JComponent getDependencyButtons(boolean predecessors) {
		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 3, 0));
		buttons.setOpaque(false);
		buttons.add(getNewDependencyButton(predecessors));
		buttons.add(getRemoveDependencyButton(predecessors));
		return buttons;
	}

	private JButton getNewDependencyButton(boolean predecessors) {
		JButton button = new JButton(Messages.getString("Spreadsheet.Action.new")); //$NON-NLS-1$
		button.setName(predecessors ? "newPredecessorLink" : "newSuccessorLink"); //$NON-NLS-1$ //$NON-NLS-2$
		button.addActionListener(event -> addDependency(predecessors));
		if (predecessors)
			newPredecessorsButton = button;
		else
			newSuccessorsButton = button;
		return button;
	}

	private JButton getRemoveDependencyButton(boolean predecessors) {
		JButton button = new JButton(Messages.getString("Text.Remove")); //$NON-NLS-1$
		button.setName(predecessors ? "removePredecessorLink" : "removeSuccessorLink"); //$NON-NLS-1$ //$NON-NLS-2$
		button.setEnabled(false);
		button.addActionListener(event -> removeSelectedDependencies(predecessors));
		if (predecessors)
			removePredecessorsButton = button;
		else
			removeSuccessorsButton = button;
		return button;
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

	private void updateRemoveDependencyButton(boolean predecessors) {
		SpreadSheet spreadsheet = predecessors ? predecessorsSpreadSheet : successorsSpreadSheet;
		JButton button = predecessors ? removePredecessorsButton : removeSuccessorsButton;
		Task task = (Task) getObject();
		if (button != null)
			button.setEnabled(spreadsheet != null && spreadsheet.getSelectedRowCount() > 0
					&& task != null && !task.isReadOnly());
	}

	private void updateNewDependencyButton(boolean predecessors) {
		JButton button = predecessors ? newPredecessorsButton : newSuccessorsButton;
		Task task = (Task) getObject();
		if (button != null)
			button.setEnabled(task != null && !task.isReadOnly() && !getLinkableTasks(task, predecessors).isEmpty());
	}

	private void installRemoveDependencyButtonState(SpreadSheet spreadsheet, boolean predecessors) {
		spreadsheet.getSelectionModel().addListSelectionListener(event -> {
			if (!event.getValueIsAdjusting())
				updateRemoveDependencyButton(predecessors);
		});
	}
    //cache reconstructed because the main cache holding edges isn't ordered
    protected void updateSuccessorsSpreadsheet() {
		TaskDependencySpreadsheet.update((TaskDependencySpreadsheet) successorsSpreadSheet,
				(Task) object, TaskDependencySpreadsheet.Direction.SUCCESSORS);
    }

	public JComponent createResourcesPanel() {
		FieldComponentMap map = createMap();
		
		// The builder advances by two rows after each section.  Spacer tracks
		// in those positions used to receive the labels at runtime, reducing
		// them to a few pixels and clipping their text.
		FormLayout layout = new FormLayout("p:grow,0dlu,right:p","p,p,p,p,fill:150dlu:grow"); //$NON-NLS-1$ //$NON-NLS-2$

		DefaultFormBuilder builder = new DefaultFormBuilder(layout);
		builder.setDefaultDialogBorder();
		CellConstraints cc = new CellConstraints();
		builder.add(createHeaderFieldsPanel(map),cc.xyw(builder.getColumn(), builder
				.getRow(), 3));
		builder.nextLine(2);
		builder.append(Messages.format("Format.label", Messages.getString("TaskInformationDialog.Resources")), getAssignResourceButton()); //$NON-NLS-1$
		builder.nextLine(2);
		builder.add(createAssignmentSpreadsheet(),cc.xyw(builder.getColumn(), builder
				.getRow(), 3));
		JComponent panel = builder.getPanel();
		HelpUtil.addDocHelp(panel,"Assign_Resources");
		return panel;	
	}

    protected SpreadSheet assignmentSpreadSheet;
	protected JScrollPane createAssignmentSpreadsheet() {
		assignmentSpreadSheet = AssignmentSpreadsheetSupport.create(this,
				AssignmentSpreadsheetSupport.Perspective.TASK_ASSIGNMENTS);
		updateAssignmentSpreadsheet();
		return AssignmentSpreadsheetSupport.scrollPane(assignmentSpreadSheet);

    }
    protected void updateAssignmentSpreadsheet() {
		AssociationList assignments = object == null ? null : ((NormalTask)object).getAssignments();
		AssignmentSpreadsheetSupport.update(assignmentSpreadSheet, assignments,
				AssignmentSpreadsheetSupport.Perspective.TASK_ASSIGNMENTS);
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
		updateRemoveDependencyButton(true);
		updateRemoveDependencyButton(false);
		updateNewDependencyButton(true);
		updateNewDependencyButton(false);
		if (assignmentSpreadSheet != null)
			updateAssignmentSpreadsheet();
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
		AssignmentSpreadsheetSupport.selectDocument(assignmentSpreadSheet, evt.getCurrent(),
				AssignmentSpreadsheetSupport.Perspective.TASK_ASSIGNMENTS);
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
