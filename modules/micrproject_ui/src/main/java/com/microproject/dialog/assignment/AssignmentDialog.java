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
package com.microproject.dialog.assignment;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.microproject.dialog.AbstractDialog;
import com.microproject.dialog.ButtonPanel;
import com.microproject.help.HelpUtil;
import com.microproject.pm.graphic.IconManager;
import com.microproject.pm.graphic.frames.DocumentFrame;
import com.microproject.pm.graphic.frames.DocumentSelectedEvent;
import com.microproject.pm.graphic.frames.GraphicManager;
import com.microproject.pm.graphic.spreadsheet.SpreadSheet;
import com.microproject.pm.graphic.spreadsheet.selection.event.SelectionNodeEvent;
import com.microproject.pm.graphic.spreadsheet.selection.event.SelectionNodeListener;
import com.microproject.pm.graphic.views.GanttView;
import com.microproject.configuration.Settings;
import com.microproject.document.ObjectEvent;
import com.microproject.grouping.core.NodeList;
import com.microproject.grouping.core.transform.filtering.NodeFilter;
import com.microproject.grouping.core.transform.filtering.NotAssignmentFilter;
import com.microproject.pm.assignment.Assignment;
import com.microproject.pm.assignment.AssignmentService;
import com.microproject.pm.resource.Resource;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.Task;
import com.microproject.preference.GlobalPreferences;
import com.microproject.strings.Messages;
import com.microproject.util.Alert;
import com.microproject.util.DataUtils;
import com.microproject.util.Environment;

@SuppressWarnings("unchecked")
public final class AssignmentDialog extends AbstractDialog implements DocumentSelectedEvent.Listener, SelectionNodeListener, ResourceAssigner, ObjectEvent.Listener, CommonAssignmentDialog  {
	private static final long serialVersionUID = 1L;
	DocumentFrame documentFrame;
	AssignmentEntryPane spreadSheetPane;
	JLabel taskNames;
//	JLabel projectName;
	JButton assignButton;
	JButton removeButton;
	JButton replaceButton;
	JButton stopEditorButton;
	JButton cancelEditorButton;
	JPanel editorsButtons=null;
	JLabel showingTeamAll = null;
	List<NormalTask> selectedTasks = null;
	
	public AssignmentDialog(DocumentFrame documentFrame) {
		super(documentFrame.getGraphicManager().getFrame(),Messages.getString("Text.AssignResources"),false); //$NON-NLS-1$
		setDocumentFrame(documentFrame);
		DocumentSelectedEvent.addListener(this);
		addDocHelp("Assign_Resources");
		//createContentPanel();
	}

	/**
	 * Creates, intializes and configures the UI components. Real applications
	 * may further bind the components to underlying models.
	 */
	protected void initControls() {
        GraphicManager mf = documentFrame.getGraphicManager();
        spreadSheetPane = new AssignmentEntryPane(this,documentFrame.getProject(),this,false,mf.setAssignmentDialogTransformerInitializationClosure());
//        projectName = new JLabel();
        taskNames = new JLabel();
        Project project=documentFrame.getProject();
		spreadSheetPane.setProject(project); //init content of spreadsheet
		SpreadSheet activeSpreadSheet = mf.getCurrentFrame() == null ? null : mf.getCurrentFrame().getTopSpreadSheet();
		setSelectedTasks(activeSpreadSheet == null ? emptyList : activeSpreadSheet.getSelectedNodes()); //update
        
//        projectName.setAlignmentX(JLabel.LEFT_ALIGNMENT);
//        projectName.setText(project == null ? "" : "Resources from: " + project.getName());
        AbstractAction assignAction = new AbstractAction(Messages.getString("Text.Assign")) { //$NON-NLS-1$
    		private static final long serialVersionUID = 1L;
  			public void actionPerformed(ActionEvent e) {
    			AssignmentDialog.this.assign();
    		}
    	};
        assignButton = new JButton(assignAction);
		assignButton.setName("assignResources");
    	
        AbstractAction removeAction  = new AbstractAction(Messages.getString("Text.Remove")) { //$NON-NLS-1$
    		private static final long serialVersionUID = 1L;
   			public void actionPerformed(ActionEvent e) {
    			AssignmentDialog.this.remove();
    		}
    	};
        removeButton = new JButton(removeAction);
		removeButton.setName("removeResources");
        
        AbstractAction replaceAction  = new AbstractAction(Messages.format("Format.ellipsis", Messages.getString("Text.Replace"))) { //$NON-NLS-1$
    		private static final long serialVersionUID = 1L;
 			public void actionPerformed(ActionEvent e) {
    			AssignmentDialog.this.replace();
    		}
    	};
        replaceButton = new JButton(replaceAction);
		replaceButton.setName("replaceResources");
		updateSharedPoolAvailability();
    	
        stopEditorButton = new JButton(new AbstractAction(null,IconManager.getIcon("dialog.ok")){ //$NON-NLS-1$
    		private static final long serialVersionUID = 1L;
  			public void actionPerformed(ActionEvent e) {
  				if (spreadSheetPane.getSpreadSheet().getCellEditor() != null)
  					spreadSheetPane.getSpreadSheet().getCellEditor().stopCellEditing();
    		}
        });
        cancelEditorButton = new JButton(new AbstractAction(null,IconManager.getIcon("dialog.cancel")){ //$NON-NLS-1$
    		private static final long serialVersionUID = 1L;
 			public void actionPerformed(ActionEvent e) {
   			    SpreadSheet sp=spreadSheetPane.getSpreadSheet();
   			    if (sp.getCellEditor() != null)
   			    	sp.getCellEditor().cancelCellEditing();
    			sp.clearSelection();
    		}
        });
        setEditorButtonsVisible(false);
        
        documentFrame.getProject().addObjectListener(this);
        documentFrame.getGraphicManager().getPreferences().addObjectListener(this);
        
	}
	
	public void setEditorButtonsVisible(boolean visible){
        stopEditorButton.setEnabled(visible);
        cancelEditorButton.setEnabled(visible);
	}

	/** A saved-but-unavailable pool must not look editable in the assignment UI. */
	private void updateSharedPoolAvailability() {
		boolean available = documentFrame == null || documentFrame.getProject() == null
				|| !documentFrame.getProject().isSharedResourcePoolUnresolved();
		String explanation = available ? null : Messages.getString("SharedResourcePool.poolNotOpen");
		if (assignButton != null) {
			assignButton.setEnabled(available);
			assignButton.setToolTipText(explanation);
		}
		if (removeButton != null) {
			removeButton.setEnabled(available);
			removeButton.setToolTipText(explanation);
		}
		if (replaceButton != null) {
			replaceButton.setEnabled(available);
			replaceButton.setToolTipText(explanation);
		}
	}
	
	
	void assign() {
	    SpreadSheet sp=spreadSheetPane.getSpreadSheet();
		if (sp.isEditing()) sp.getCellEditor().stopCellEditing();
		assign(getSelectedResources(),1.0D);
		spreadSheetPane.updateTable();
	}
	
	void assign(List<?> resourceList, double units) {
		if (documentFrame.getProject().isSharedResourcePoolUnresolved())
			return;
		if (selectedTasks == null) // if no selection, do nothing
			return;
		List<NormalTask> taskList = new ArrayList<>(selectedTasks.size());
		for (NormalTask task : selectedTasks) { // go thru all selected tasks
			if (!task.isAssignable())
				continue;
			taskList.add(task);
		}
		AssignmentService.getInstance().newAssignments(taskList,resourceList,units,0,this,true);
		spreadSheetPane.updateTable();
	}
	
	public void assign(Resource resource, double units) {
		ArrayList<Resource> list = new ArrayList<>(1);
		list.add(resource);
		assign(list,units);
	}

/**
 * Removes given the current task and resource selection
 *
 */	void remove() {
		remove(getSelectedResources());
	}

/**
 * Removes given the current task selection for the given resource lsit
 * @param resourceList
 */	void remove(List<?> resourceList) {
		for (Resource resource : getSelectedResources()) {
			remove(resource);
		}
		spreadSheetPane.updateTable();
	}
	
/**
 * Removes given resource from current task selection 
 * @param resource
 * @param selectedTasks
 */	void remove(Resource resource) {
		if (documentFrame.getProject().isSharedResourcePoolUnresolved())
			return;
		if (selectedTasks == null)
			return;
		Assignment assignment;
		for (NormalTask task : selectedTasks) {
			assignment = task.findAssignment(resource);
			if (assignment != null)
				AssignmentService.getInstance().remove(assignment,this,true);
		}
	}	
	
	void replace() {
		if (documentFrame.getProject().isSharedResourcePoolUnresolved())
			return;
		List<Resource> list = spreadSheetPane.getSelectedResources(true);
		if (list.size() > 1) {
			Alert.warn(Messages.getString("Message.onlyReplaceOneResourceAtATime"),this); //$NON-NLS-1$
			return;
		} else if (list.size() == 0) {
			return;
		}
		Resource resource = list.get(0);
		List<Resource> replacementList = ReplaceAssignmentDialog.getReplacementFromDialog(documentFrame,resource);
		if (replacementList == null || replacementList.isEmpty()) // cancelled or nothing chosen
			return;
		if (!replacementList.contains(resource)) // if resource was replaced, remove it
			remove(resource);
		else // resource is in new list too, so don't touch it
			replacementList.remove(resource);
		assign(replacementList,1.0); // Preserve the current unit assignment for the replacement flow.
	}

	
	// Building *************************************************************

	/**
	 * Builds the panel. Initializes and configures components first, then
	 * creates a FormLayout, configures the layout, creates a builder, sets a
	 * border, and finally adds the components.
	 * 
	 * @return the built panel
	 */

	private JLabel getTeamOrAllLabel() {
		if (showingTeamAll == null)
			showingTeamAll = new JLabel(Messages.getString("AssignmentDialog.ShowingAllResources"), IconManager.getIcon("menu24.showAllResources"),JLabel.LEFT); //$NON-NLS-1$ //$NON-NLS-2$

		if (documentFrame.getGraphicManager().getPreferences().isShowProjectResourcesOnly()) {
			showingTeamAll.setIcon(IconManager.getIcon("menu24.showTeamResources")); //$NON-NLS-1$
			showingTeamAll.setText(Messages.getString("AssignmentDialog.ShowingOnlyResourcesOnTheProjectTeam")); //$NON-NLS-1$
		} else {
			showingTeamAll.setIcon(IconManager.getIcon("menu24.showAllResources")); //$NON-NLS-1$
			showingTeamAll.setText(Messages.getString("AssignmentDialog.ShowingAllResources")); //$NON-NLS-1$
		}
		HelpUtil.addDocHelp(showingTeamAll,"Project_Team");
		return showingTeamAll;
	}
	public JComponent createContentPanel() {
        
		// Separating the component initialization and configuration
		// from the layout code makes both parts easier to read.
		initControls();
		FormLayout layout = new FormLayout("p, 1dlu, default ,3dlu, default", // cols //$NON-NLS-1$
		"p, 3dlu,"+(Environment.getStandAlone()?"":"p, 3dlu,")+"fill:200dlu:grow"); // rows //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

		// Create a builder that assists in adding components to the container.
		// Wrap the panel with a standardized border.
		DefaultFormBuilder builder = new DefaultFormBuilder(layout);
		builder.setDefaultDialogBorder();
		CellConstraints cc = new CellConstraints();
		// task names span whole dialog
		builder.add(taskNames,cc.xyw(builder.getColumn(), builder.getRow(), builder.getColumnCount()));
		builder.nextLine(2);
		
		if (!Environment.getStandAlone()){
			if (!Environment.isExternal())
				builder.add(getTeamOrAllLabel(),cc.xyw(builder.getColumn(), builder.getRow(), builder.getColumnCount()));
			builder.nextLine(2);
		}

//		builder.append(projectName);
//		builder.nextLine(2);

		builder.append(spreadSheetPane, createEditorsButtons(), createButtons());
		return builder.getPanel();
	}
	
	public JComponent createEditorsButtons() {
		FormLayout layout = new FormLayout("20px", // cols //$NON-NLS-1$
		"20dlu,20px, 3dlu, 20px"); // rows //$NON-NLS-1$
		DefaultFormBuilder builder = new DefaultFormBuilder(layout);
		builder.nextLine(1);
		builder.append(stopEditorButton);
		builder.nextLine(2);
		builder.append(cancelEditorButton);
		editorsButtons=builder.getPanel();
		return editorsButtons;
	}
	public JComponent createButtons() {
		FormLayout layout = new FormLayout("default", // cols //$NON-NLS-1$
		"50dlu,p,3dlu,p, 3dlu, p, 3dlu, p"); // rows //$NON-NLS-1$
		DefaultFormBuilder builder = new DefaultFormBuilder(layout);
		builder.nextLine(1);
		builder.append(assignButton);
		builder.nextLine(2);
		builder.append(removeButton);
		builder.nextLine(2);		
		builder.append(replaceButton);
		builder.nextLine(2);		
		builder.add(getHelpButton());

		return builder.getPanel();
	}
	
	public ButtonPanel createButtonPanel() {
		return null;
	}

	/**
	 * @return Returns the project.
	 */
	public DocumentFrame getDocumentFrame() {
		return documentFrame;
	}
	/**
	 * @param project The project to set.
	 */
	public void setDocumentFrame(DocumentFrame documentFrame) {
		detachDocumentFrame(this.documentFrame);
		this.documentFrame = documentFrame;
		attachDocumentFrame(documentFrame);
//		if (projectName != null)
//			projectName.setText(project == null ? "" : "Resources from: " + project.getName());
	}

	private void detachDocumentFrame(DocumentFrame frame) {
		if (frame != null && frame.getProject() != null) {
			frame.getProject().removeObjectListener(this);
		}
	}

	private void attachDocumentFrame(DocumentFrame frame) {
		if (frame == null || frame.getProject() == null) {
			return;
		}
		Project project = frame.getProject();
		project.getResourcePool().addObjectListener(this);
	}
	
	private static final List<Object> emptyList = List.of();
	public void documentSelected(DocumentSelectedEvent evt) {
		setDocumentFrame(evt.getCurrent());
		if (getDocumentFrame() != null) {
			spreadSheetPane.setProject(getDocumentFrame().getProject());
		}
		updateSharedPoolAvailability();
		setSelectedTasks(emptyList);
	}

	public void selectionChanged(SelectionNodeEvent e) {
		if (e.getCategory() != GanttView.spreadsheetCategory) // Keep spreadsheet events scoped to the assignment pane.
			return;
		List<?> selectedNodes = e.getNodes();
		setSelectedTasks(selectedNodes);
	}

	private static final NodeFilter filter=NotAssignmentFilter.getWritableInstance();
	private void setSelectedTasks(List<?> selectedNodes) {
		selectedTasks = toSelectedTasks(selectedNodes);
		String names;
		if (selectedTasks.isEmpty())
			names = Messages.getString("AssignmentDialog.none"); //$NON-NLS-1$
		else
			names = DataUtils.stringListWithMaxAndMessage(selectedTasks,Settings.STRING_LIST_LIMIT,Messages.getString("Message.tooManyTasksSelectedToList")); //$NON-NLS-1$

		String label = Messages.format("Format.labelValue", Messages.getString("Text.Tasks"), names); //$NON-NLS-1$
		if (documentFrame.getProject().isSharedResourcePoolUnresolved())
			label += " — " + Messages.getString("SharedResourcePool.poolNotOpen");
		taskNames.setText(label);
		spreadSheetPane.setSelectedTasks(selectedTasks);
//		setEnabled(!selectedTasks.isEmpty());
	}
	
	public List<Resource> getSelectedResources(){
  		return spreadSheetPane.getSelectedResources(false);
  	}

	@SuppressWarnings("unchecked")
	private List<NormalTask> toSelectedTasks(List<?> selectedNodes) {
		List<?> objects = NodeList.nodeListToImplList(selectedNodes, filter);
		List<NormalTask> tasks = new ArrayList<>(objects.size());
		for (Object current : objects) {
			if (current instanceof NormalTask task && task.isAssignable()) {
				tasks.add(task);
			}
		}
		return tasks;
	}


	public void objectChanged(ObjectEvent objectEvent) {
		if (documentFrame == null)
			return;
		if (objectEvent.getObject() instanceof Resource)
			spreadSheetPane.setProject(documentFrame.getProject());	
		else if (objectEvent.getObject() instanceof Assignment)
			spreadSheetPane.updateTable(); // Undo and spreadsheet changes still funnel through the table refresh path.
		else if (objectEvent.getObject() instanceof GlobalPreferences)
			getTeamOrAllLabel();
			
	}
}
