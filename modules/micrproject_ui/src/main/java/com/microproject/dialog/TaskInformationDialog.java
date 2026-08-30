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
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.DefaultTableModel;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.microproject.dialog.util.FieldComponentMap;
import com.microproject.dialog.util.ComponentFactory;
import com.microproject.help.HelpUtil;
import com.microproject.menu.MenuActionConstants;
import com.microproject.pm.graphic.frames.DocumentFrame;
import com.microproject.pm.graphic.frames.DocumentSelectedEvent;
import com.microproject.pm.graphic.frames.GraphicManager;
import com.microproject.pm.graphic.gantt.BarColorEditorPanel;
import com.microproject.pm.graphic.gantt.BarColorField;
import com.microproject.pm.graphic.gantt.Gantt;
import com.microproject.pm.graphic.gantt.GanttRenderer;
import com.microproject.graphic.configuration.GanttBarFormatOverrides.BarFormat;
import com.microproject.pm.graphic.model.cache.NodeModelCache;
import com.microproject.pm.graphic.spreadsheet.SpreadSheet;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetModel;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetUtils;
import com.microproject.pm.graphic.views.UsageDetailView;
import com.microproject.association.AssociationList;
import com.microproject.association.InvalidAssociationException;
import com.microproject.configuration.Configuration;
import com.microproject.field.Field;
import com.microproject.graphic.configuration.SpreadSheetCategories;
import com.microproject.graphic.configuration.shape.Colors;
import com.microproject.pm.assignment.Assignment;
import com.microproject.pm.assignment.AssignmentEntry;
import com.microproject.pm.dependency.Dependency;
import com.microproject.pm.dependency.DependencyNodeModelDataFactory;
import com.microproject.pm.dependency.DependencyService;
import com.microproject.pm.dependency.DependencyType;
import com.microproject.pm.key.HasId;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.ProjectFactory;
import com.microproject.pm.task.Task;
import com.microproject.pm.task.ScheduleDiagnosticsService;
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

	// Bar color fields shown in the General tab (issue #16)
	private BarColorField barStartColor;
	private BarColorField barMiddleColor;
	private BarColorField barEndColor;
	private BarColorEditorPanel barColorEditor;
	private BarColorField fontColorField;

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
		if (task == null || task.isReadOnly()
				|| barStartColor == null || barMiddleColor == null || barEndColor == null)
			return;
		Gantt gantt = getGantt();
		if (gantt == null)
			return;
		gantt.applyBarFormat(task,
				new BarFormat(barStartColor.getRgb(), barMiddleColor.getRgb(), barEndColor.getRgb()));
	}

	private void refreshBarColorFields() {
		Task task = (Task) getObject();
		if (task == null || barColorEditor == null)
			return;
		refreshBarColorFields(barColorEditor, currentBarFormat(task), task.isReadOnly());
	}

	static void refreshBarColorFields(BarColorEditorPanel editor, BarFormat format, boolean readOnly) {
		if (editor == null)
			return;
		BarFormat resolved = format == null ? BarFormat.automatic() : format;
		editor.setEnabled(!readOnly);
		editor.getStart().setRgb(resolved.getStartRgb());
		editor.getMiddle().setRgb(resolved.getMiddleRgb());
		editor.getEnd().setRgb(resolved.getEndRgb());
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
		FormLayout layout = new FormLayout("350dlu:grow", "fill:200dlu:grow"); //$NON-NLS-1$ //$NON-NLS-2$
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
		JScrollPane scrollPane = new JScrollPane(contents,
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setBorder(null);
		// JScrollPane otherwise reports the entire form as its preferred viewport
		// height and can make the dialog taller than the desktop.  Keep a stable
		// viewport; the complete form remains reachable through the scrollbar.
		scrollPane.setPreferredSize(new Dimension(700, 360));
		scrollPane.setMinimumSize(new Dimension(480, 240));
		return scrollPane;
	}

	private JComponent createTextStylePanel() {
		FieldComponentMap map = createMap();
		FormLayout layout = new FormLayout("p,3dlu,130dlu,12dlu,p,3dlu,80dlu", "max(24dlu;pref),3dlu,max(24dlu;pref),3dlu,max(24dlu;pref),3dlu,max(24dlu;pref),3dlu,max(24dlu;pref)");
		DefaultFormBuilder builder = new DefaultFormBuilder(layout);
		builder.setDefaultDialogBorder();
		builder.addSeparator(Messages.getString("TaskInformationDialog.TextStyle"));
		builder.nextLine(2);
		map.append(builder, "Field.fontFamily");
		map.append(builder, "Field.fontSize");
		builder.nextLine(2);
		map.append(builder, "Field.fontBold");
		map.append(builder, "Field.fontItalic");
		builder.nextLine(2);
		map.append(builder, "Field.fontStrikethrough");
		builder.nextLine(2);
		Task task = (Task)getObject();
		fontColorField = new BarColorField(this, task == null ? null : task.getFontColor(), 0x000000,
				"TaskInformationDialog.FontColor", null);
		fontColorField.setEnabled(task != null && !task.isReadOnly());
		builder.append(Messages.getString("TaskInformationDialog.FontColor"), fontColorField);
		return builder.getPanel();
	}

	private JComponent createDiagnosticsPanel() {
		String[] columns = {
			Messages.getString("TaskInformationDialog.Severity"),
			Messages.getString("TaskInformationDialog.Issue"),
			Messages.getString("TaskInformationDialog.Cause"),
			Messages.getString("TaskInformationDialog.Recommendation")
		};
		DefaultTableModel model = new DefaultTableModel(columns, 0) {
			private static final long serialVersionUID = 1L;
			@Override public boolean isCellEditable(int row, int column) { return false; }
		};
		for (var issue : new ScheduleDiagnosticsService().diagnose((Task) getObject())) {
			String key = "diagnostic." + issue.type().name().toLowerCase(Locale.ROOT);
			model.addRow(new Object[] { issue.severity(), UsabilityStrings.text(key + ".summary"), UsabilityStrings.text(key + ".cause"), UsabilityStrings.text(key + ".recommendation") });
		}
		JTable table = new JTable(model);
		table.setAutoCreateRowSorter(true);
		table.setRowHeight(Math.max(table.getRowHeight(), 24));
		table.getAccessibleContext().setAccessibleName(Messages.getString("TaskInformationDialog.Diagnostics"));
		JScrollPane pane = new JScrollPane(table);
		pane.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 8, 8, 8));
		return pane;
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
				,"p,3dlu"); //$NON-NLS-1$
		DefaultFormBuilder builder = new DefaultFormBuilder(layout);
		map.append(builder,"Field.name"); //$NON-NLS-1$
		builder.nextLine(); // border at bottom
		return builder.getPanel();
	}
	

	private JComponent createGeneralPanel(){
		FieldComponentMap map = createMap();
		JPanel panel = new JPanel(new GridBagLayout());
		panel.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 12, 10, 12));
		int row = 0;
		addGeneralField(panel, map, "Field.name", 0, row++, 0, 4); //$NON-NLS-1$
		addGeneralField(panel, map, "Field.duration", ComponentFactory.SOMETIMES_READ_ONLY, row, 0, 2); //$NON-NLS-1$
		addGeneralField(panel, map, "Field.estimated", 0, row++, 2, 2); //$NON-NLS-1$
		addGeneralField(panel, map, "Field.percentComplete", ComponentFactory.SOMETIMES_READ_ONLY, row, 0, 2); //$NON-NLS-1$
		addGeneralField(panel, map, "Field.priority", 0, row++, 2, 2); //$NON-NLS-1$
		addGeneralField(panel, map, "Field.manuallyScheduled", 0, row, 0, 2); //$NON-NLS-1$
		addGeneralField(panel, map, "Field.inactiveTask", 0, row++, 2, 2); //$NON-NLS-1$
		addGeneralField(panel, map, "Field.hiddenTask", 0, row++, 0, 2); //$NON-NLS-1$
		addGeneralField(panel, map, "Field.cost", 0, row, 0, 2); //$NON-NLS-1$
		addGeneralField(panel, map, "Field.work", 0, row++, 2, 2); //$NON-NLS-1$
		addGeneralSection(panel, Messages.getString("TaskInformationDialog.Dates"), row++); //$NON-NLS-1$
		addGeneralField(panel, map, "Field.start", 0, row, 0, 2); //$NON-NLS-1$
		addGeneralField(panel, map, "Field.finish", 0, row++, 2, 2); //$NON-NLS-1$
		addGeneralField(panel, map, "Field.baselineStart", 0, row, 0, 2); //$NON-NLS-1$
		addGeneralField(panel, map, "Field.baselineFinish", 0, row++, 2, 2); //$NON-NLS-1$
		addGeneralSection(panel, Messages.getString("TaskInformationDialog.BarColor"), row++); //$NON-NLS-1$
		Task task = (Task) getObject();
		Gantt gantt = getGantt();
		barColorEditor = new BarColorEditorPanel(this, currentBarFormat(task),
				gantt == null ? new GanttRenderer.DisplayedBarColors(
						null, null, null)
						: gantt.getDisplayedBarColors(task),
				task.isMilestone(), task.isSummary(), null);
		barColorEditor.setEnabled(!task.isReadOnly());
		barStartColor = barColorEditor.getStart();
		barMiddleColor = barColorEditor.getMiddle();
		barEndColor = barColorEditor.getEnd();
		GridBagConstraints constraints = generalConstraints(0, row);
		constraints.gridwidth = 4;
		constraints.weightx = 1.0;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		panel.add(barColorEditor, constraints);
		return panel;
	}

	private void addGeneralField(JPanel panel, FieldComponentMap map, String fieldId, int flag, int row, int column, int width) {
		JComponent component = map.getComponent(fieldId, flag);
		if (component instanceof JCheckBox) {
			GridBagConstraints constraints = generalConstraints(column, row);
			constraints.gridwidth = width;
			constraints.anchor = GridBagConstraints.WEST;
			panel.add(component, constraints);
			return;
		}
		GridBagConstraints label = generalConstraints(column, row);
		label.anchor = GridBagConstraints.EAST;
		panel.add(new JLabel(map.getLabel(fieldId) + ":"), label);
		GridBagConstraints value = generalConstraints(column + 1, row);
		value.gridwidth = Math.max(1, width - 1);
		value.weightx = 1.0;
		value.fill = GridBagConstraints.HORIZONTAL;
		panel.add(component, value);
	}

	private void addGeneralSection(JPanel panel, String text, int row) {
		GridBagConstraints label = generalConstraints(0, row);
		label.gridwidth = 1;
		label.anchor = GridBagConstraints.WEST;
		panel.add(new JLabel(text), label);
		GridBagConstraints line = generalConstraints(1, row);
		line.gridwidth = 3;
		line.weightx = 1.0;
		line.fill = GridBagConstraints.HORIZONTAL;
		panel.add(new JSeparator(), line);
	}

	private GridBagConstraints generalConstraints(int x, int y) {
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridx = x;
		constraints.gridy = y;
		constraints.insets = new Insets(4, 4, 4, 8);
		return constraints;
	}

	private JComponent createAdvancedPanel(){
		FieldComponentMap map = createMap();
		FormLayout layout = new FormLayout(
		        "max(50dlu;pref), 3dlu, 90dlu, 10dlu, p, 3dlu,90dlu,30dlu", // extra padding on right is for estimated field //$NON-NLS-1$
				"max(24dlu;pref),3dlu,max(24dlu;pref),3dlu,max(24dlu;pref),3dlu,max(24dlu;pref),3dlu,max(24dlu;pref),3dlu,max(24dlu;pref),3dlu,max(24dlu;pref),3dlu,max(24dlu;pref),3dlu,max(24dlu;pref),3dlu,max(24dlu;pref),3dlu,max(24dlu;pref),fill:50dlu:grow"); //$NON-NLS-1$

		DefaultFormBuilder builder = new DefaultFormBuilder(layout);
		builder.setDefaultDialogBorder();
		CellConstraints cc = new CellConstraints();
		
		builder.add(createHeaderFieldsPanel(map),cc.xyw(builder.getColumn(), builder
				.getRow(), 8));
		builder.nextLine(2);
		map.append(builder,"Field.wbs"); //$NON-NLS-1$
		map.append(builder,"Field.markTaskAsMilestone",3); //$NON-NLS-1$
		builder.nextLine(2);
		builder.addSeparator(Messages.getString("TaskInformationDialog.ConstrainTask")); //$NON-NLS-1$
		// addSeparator already advances one row. Move to the next content row,
		// not the following 3dlu spacer row, otherwise the constraint controls
		// are clipped to the height of that spacer.
		builder.nextLine();
		map.append(builder,"Field.constraintType"); //$NON-NLS-1$
		map.appendSometimesReadOnly(builder,"Field.constraintDate"); //$NON-NLS-1$
		builder.nextLine(2);
		map.append(builder,"Field.deadline"); //$NON-NLS-1$
		builder.nextLine(4);
		builder.addSeparator("	"); //$NON-NLS-1$
		builder.nextLine();
		map.append(builder,"Field.taskType"); //$NON-NLS-1$
		map.append(builder,"Field.effortDriven",3); //$NON-NLS-1$
		builder.nextLine(2);
		map.append(builder,"Field.taskCalendar"); //$NON-NLS-1$
		map.append(builder,"Field.ignoreResourceCalendar",3); //$NON-NLS-1$
		builder.nextLine(2);
		map.append(builder,"Field.earnedValueMethod"); //$NON-NLS-1$

		return builder.getPanel();
	}	
	
	public JComponent createPredecessorsPanel() {
		FieldComponentMap map = createMap();		
		FormLayout layout = new FormLayout("p:grow,3dlu,right:p","p,3dlu,p,3dlu,fill:150dlu:grow"); //$NON-NLS-1$ //$NON-NLS-2$

		DefaultFormBuilder builder = new DefaultFormBuilder(layout);
		builder.setDefaultDialogBorder();
		CellConstraints cc = new CellConstraints();
		builder.add(createHeaderFieldsPanel(map),cc.xyw(builder.getColumn(), builder
			.getRow(), 3));
		builder.nextLine(2);
		builder.append(Messages.format("Format.label", Messages.getString("Spreadsheet.Dependency.predecessors")),
				getDependencyButtons(true)); //$NON-NLS-1$
		builder.nextLine(2);
		builder.add(createPredecessorsSpreadsheet(),cc.xyw(builder.getColumn(), builder.getRow(), 3));
		JComponent pred = builder.getPanel();
		HelpUtil.addDocHelp(pred,"Linking");
		return pred;	
	}
	
	private class DependencySpreadSheet extends SpreadSheet {
    	InformationDialog dlg;
		Field clickField;
		boolean predecessor;
    	DependencySpreadSheet(InformationDialog dlg, boolean predecessor) {
    		this.dlg = dlg;
    		this.clickField = Configuration.getFieldFromId(predecessor ? "Field.predecessorName" : "Field.successorName");
    		this.predecessor = predecessor;
    	}
    	public void doDoubleClick(int row, int col) {}
    	public void doClick(int row, int col) {
    		Object obj = getCurrentRowImpl();
    		if (obj!= null) {
				Field field = ((SpreadSheetModel)getModel()).getFieldInColumn(col+1);
				if (field == clickField) {
        			NormalTask pred = (NormalTask) (predecessor ? ((Dependency)obj).getLeft() : ((Dependency)obj).getRight());
        			dlg.setObject(pred);
        			dlg.updateAll();
        			pred.getDocument().getObjectSelectionEventManager().fire(this,pred);
				}
    		}
    	}
    	
		public Component prepareRenderer(TableCellRenderer renderer, int row,
				int column) {
			Component component =  super.prepareRenderer(renderer, row, column);
			Field field = ((SpreadSheetModel)getModel()).getFieldInColumn(column+1);
			if (field == clickField) {
				JLabel l = (JLabel)component;
				l.setText("<html><a href=\"\">" + l.getText() + "</a></html>");
			}
			return component;
		}
		
	}
	
	protected SpreadSheet predecessorsSpreadSheet;
	private JButton newPredecessorsButton;
	private JButton removePredecessorsButton;
 	public static final String DEPENDENCY_SPREADSHEET=SpreadSheetCategories.dependencySpreadsheetCategory;
    protected JScrollPane createPredecessorsSpreadsheet() {
    	final TaskInformationDialog self = this;
        predecessorsSpreadSheet = new DependencySpreadSheet(this,true);
		predecessorsSpreadSheet.setSpreadSheetCategory(DEPENDENCY_SPREADSHEET);
    	predecessorsSpreadSheet.setCanModifyColumns(false);
    	predecessorsSpreadSheet.setCanSelectFieldArray(false);
    	predecessorsSpreadSheet.setActions(new String[]{MenuActionConstants.ACTION_DELETE});
    	SpreadSheetUtils.createCollectionSpreadSheet(predecessorsSpreadSheet
				,(object==null)?new AssociationList():((Task)object).getPredecessorList()
				//,(object==null)?null:((NormalTask)object).getDocument()
				,"View.TaskInformation.Predecessors" //$NON-NLS-1$
				,DEPENDENCY_SPREADSHEET
				,"Spreadsheet.Dependency.predecessors" //$NON-NLS-1$
				,true
				,new DependencyNodeModelDataFactory()
				, 0
//				,false
//				,true
			);
		installRemoveDependencyButtonState(predecessorsSpreadSheet, true);
	    return SpreadSheetUtils.makeSpreadsheetScrollPane(predecessorsSpreadSheet);

    }
    //cache reconstructed because the main cache holding edges isn't ordered
    protected void updatePredecessorsSpreadsheet() {
    	SpreadSheetUtils.updateCollectionSpreadSheet(predecessorsSpreadSheet
    					,(object==null)?new AssociationList():((Task)object).getPredecessorList()
						,new DependencyNodeModelDataFactory()
						, 0);
    }

	public JComponent createSuccessorsPanel() {
		FieldComponentMap map = createMap();		
		FormLayout layout = new FormLayout("p:grow,3dlu,right:p","p,3dlu,p,3dlu,fill:150dlu:grow"); //$NON-NLS-1$ //$NON-NLS-2$

		DefaultFormBuilder builder = new DefaultFormBuilder(layout);
		builder.setDefaultDialogBorder();
		CellConstraints cc = new CellConstraints();
		builder.add(createHeaderFieldsPanel(map),cc.xyw(builder.getColumn(), builder
			.getRow(), 3));
		builder.nextLine(2);
		builder.append(Messages.format("Format.label", Messages.getString("Spreadsheet.Dependency.successors")),
				getDependencyButtons(false)); //$NON-NLS-1$
		builder.nextLine(2);
		builder.add(createSuccessorsSpreadsheet(),cc.xyw(builder.getColumn(), builder.getRow(), 3));
		JComponent succ = builder.getPanel();
		HelpUtil.addDocHelp(succ,"Linking");
		return succ;	
	}
	
	protected SpreadSheet successorsSpreadSheet;
	private JButton newSuccessorsButton;
	private JButton removeSuccessorsButton;
    protected JScrollPane createSuccessorsSpreadsheet() {
        successorsSpreadSheet = new DependencySpreadSheet(this,false);
		successorsSpreadSheet.setSpreadSheetCategory(DEPENDENCY_SPREADSHEET);
    	successorsSpreadSheet.setCanModifyColumns(false);
    	successorsSpreadSheet.setCanSelectFieldArray(false);
    	successorsSpreadSheet.setActions(new String[]{MenuActionConstants.ACTION_DELETE});
    	
    	SpreadSheetUtils.createCollectionSpreadSheet(successorsSpreadSheet
				,(object==null)?new AssociationList():((Task)object).getSuccessorList()
				//,(object==null)?null:((NormalTask)object).getDocument()
				,"View.TaskInformation.Successors" //$NON-NLS-1$
				,DEPENDENCY_SPREADSHEET
				,"Spreadsheet.Dependency.successors" //$NON-NLS-1$
				,false
				,new DependencyNodeModelDataFactory()
				, 0
//				,false
//				,true
			);
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
		List<DependencyTaskChoice> choices = new ArrayList<>(candidates.size());
		for (Task candidate : candidates)
			choices.add(new DependencyTaskChoice(candidate));
		DependencyTaskChoice selected = (DependencyTaskChoice) JOptionPane.showInputDialog(this,
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
		try {
			createDependency(task, selected.task, predecessors, type.kind.code(), this);
			updateAll();
		} catch (InvalidAssociationException e) {
			Alert.warn(e.getMessage(), this);
		}
	}

	static Dependency createDependency(Task task, Task selectedTask, boolean predecessors, Object eventSource)
			throws InvalidAssociationException {
		return createDependency(task, selectedTask, predecessors, DependencyType.Kind.FS.code(), eventSource);
	}

	static Dependency createDependency(Task task, Task selectedTask, boolean predecessors, int dependencyType,
			Object eventSource) throws InvalidAssociationException {
		return DependencyService.getInstance().newDependency(
				predecessors ? selectedTask : task,
				predecessors ? task : selectedTask,
				dependencyType, 0, eventSource);
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
		ProjectFactory.getInstance().getPortfolio().forProjects(value -> {
			if (value instanceof Project project && !projects.contains(project))
				projects.add(project);
		});
		return getLinkableTasks(task, predecessors, projects);
	}

	static List<Task> getLinkableTasks(Task task, boolean predecessors, Iterable<Project> projects) {
		Set<Task> candidates = new LinkedHashSet<>();
		for (Project project : projects) {
			if (project == null)
				continue;
			for (Task candidate : project.getTaskList()) {
				if (candidate != task && !candidate.isReadOnly() && !candidate.isExternal()
						&& !isAlreadyLinked(task, candidate, predecessors))
					candidates.add(candidate);
			}
		}
		return new ArrayList<>(candidates);
	}

	private static boolean isAlreadyLinked(Task task, Task candidate, boolean predecessors) {
		return predecessors
				? task.getPredecessorList().findLeft(candidate) != null
				: task.getSuccessorList().findRight(candidate) != null;
	}

	private static final class DependencyTaskChoice {
		private final Task task;

		private DependencyTaskChoice(Task task) {
			this.task = task;
		}

		@Override
		public String toString() {
			Project project = task.getProject();
			String projectName = project == null ? "" : project.getName();
			return projectName + ": " + task.getName();
		}
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
    	SpreadSheetUtils.updateCollectionSpreadSheet(successorsSpreadSheet
				,(object==null)?new AssociationList():((Task)object).getSuccessorList()
				,new DependencyNodeModelDataFactory()
				, 0);
    }

	public JComponent createResourcesPanel() {
		FieldComponentMap map = createMap();
		
		FormLayout layout = new FormLayout("p:grow,0dlu,right:p","p,3dlu,p,3dlu,fill:150dlu:grow"); //$NON-NLS-1$ //$NON-NLS-2$

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
		assignmentSpreadSheet = SpreadSheetUtils.createFilteredSpreadsheet(GraphicManager.getInstance(this).getCurrentFrame()
        							,false
									,"View.TaskInformation.Assignments" //$NON-NLS-1$
									,UsageDetailView.resourceAssignmentSpreadsheetCategory
									,UsageDetailView.getUsageAssignmentSpreadsheetId(false)
									,true
									//, 0
									,new String[]{MenuActionConstants.ACTION_DELETE}
									/*, new int[] {SpreadSheet.DELETE}*/);
		updateAssignmentSpreadsheet();
	    return SpreadSheetUtils.makeSpreadsheetScrollPane(assignmentSpreadSheet);

    }
    protected void updateAssignmentSpreadsheet() {
    	SpreadSheetUtils.updateFilteredSpreadsheet(assignmentSpreadSheet,(object==null)?new AssociationList():((NormalTask)object).getAssignments());
    	((SpreadSheetModel)assignmentSpreadSheet.getModel()).fireUpdateAll();
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
		Task task = (Task)getObject();
		if (fontColorField == null)
			return;
		fontColorField.setEnabled(task != null && !task.isReadOnly());
		fontColorField.setRgb(task == null ? null : task.getFontColor());
	}

	private void applyFontColorFromField() {
		Task task = (Task)getObject();
		if (task == null || task.isReadOnly() || fontColorField == null)
			return;
		Integer color = fontColorField.getRgb();
		if (java.util.Objects.equals(task.getFontColor(), color))
			return;
		task.setFontColor(color);
		task.getProject().fireUpdateEvent(this, task);
	}

	public void documentSelected(DocumentSelectedEvent evt) {
		if (assignmentSpreadSheet==null) return;
        DocumentFrame df=evt.getCurrent();
        if (df!=null){
//        	List impls=df.getSelectedImpls();
//        	if (impls!=null&&impls.size()>0) setObject(impls.get(0));
        	NodeModelCache cache = df.createCache(false,Messages.getString("View.TaskInformation.Assignments")); //$NON-NLS-1$
			assignmentSpreadSheet.setCache(cache);
        }
	}
	
	
	protected void activateListeners() {
		super.activateListeners();
		if (predecessorsSpreadSheet != null)
			predecessorsSpreadSheet.getCache().setReceiveEvents(true);
		if (successorsSpreadSheet != null)
			successorsSpreadSheet.getCache().setReceiveEvents(true);
		//assignmentSpreadSheet.getCache().setReceiveEvents(true);
	}

	protected void desactivateListeners() {
		super.desactivateListeners();
		if (predecessorsSpreadSheet != null)
			predecessorsSpreadSheet.getCache().setReceiveEvents(false);
		if (successorsSpreadSheet != null)
			successorsSpreadSheet.getCache().setReceiveEvents(false);
		//assignmentSpreadSheet.getCache().setReceiveEvents(false); 
		//causes an update problem of the filtered cache
	}


	protected boolean hasHelpButton() {
		return true;
	}

	
}
