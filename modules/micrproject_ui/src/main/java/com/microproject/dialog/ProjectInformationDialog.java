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

import java.awt.Frame;
import java.awt.Dimension;
import java.util.Collection;

import javax.swing.JComponent;
import javax.swing.JTabbedPane;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.microproject.dialog.util.FieldComponentMap;
import com.microproject.pm.graphic.frames.DocumentSelectedEvent;
import com.microproject.pm.graphic.spreadsheet.selection.event.SelectionNodeEvent;
import com.microproject.configuration.FieldDictionary;
import com.microproject.document.ObjectEvent;
import com.microproject.pm.resource.Resource;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.Task;
import com.microproject.strings.Messages;
import com.microproject.util.FlatUiSupport;
import com.microproject.util.Environment;
/**
 *
 */
public class ProjectInformationDialog extends InformationDialog {
	private static final long serialVersionUID = 1L;

	public static ProjectInformationDialog getInstance(Frame owner, Project project) {
		return new ProjectInformationDialog(owner, project);
	}

	private ProjectInformationDialog(Frame owner, Project project) {
		super(owner, Messages.getString("ProjectInformationDialog.ProjectInformation")); //$NON-NLS-1$
		setObjectClass(Project.class);
		setObject(project);
		addDocHelp("Project_Information_Dialog");

	}

	private JTabbedPane tabbedPane;
	
	public JComponent createContentPanel() {	
	    	
		FormLayout layout = new FormLayout("350dlu:grow","fill:250dlu:grow"); //$NON-NLS-1$ //$NON-NLS-2$
		DefaultFormBuilder builder = new DefaultFormBuilder(layout);
		builder.setDefaultDialogBorder();
		CellConstraints cc = new CellConstraints();
		
		tabbedPane= new JTabbedPane();
		FlatUiSupport.styleTabbedPane(tabbedPane);
		tabbedPane.setMinimumSize(new Dimension(700, 420));
		tabbedPane.addTab(Messages.getString("ProjectInformationDialog.General"),createGeneralPanel()); //$NON-NLS-1$
		tabbedPane.addTab(Messages.getString("ProjectInformationDialog.Statistics"),createStatisticsPanel()); //$NON-NLS-1$
		tabbedPane.addTab(Messages.getString("ProjectInformationDialog.Notes"), createNotesPanel()); //$NON-NLS-1$
		builder.add(tabbedPane);
		mainComponent = tabbedPane;
		return builder.getPanel();
	}

	private JComponent createGeneralPanel(){
		FieldComponentMap map = createMap();
		FormLayout layout = new FormLayout(
		        "max(50dlu;pref), 3dlu, 90dlu, 10dlu, p, 3dlu,max(90dlu;pref),60dlu", // extra padding on right is for estimated field //$NON-NLS-1$
				"p,p,p,p,p,p,p,p,p,p,p,p,p,p,p,p,p,p,p, 6dlu, fill:50dlu:grow"); //$NON-NLS-1$

		DefaultFormBuilder builder = new DefaultFormBuilder(layout);
		CellConstraints cc = new CellConstraints();
		builder.setDefaultDialogBorder();
		builder.add(createHeaderFieldsPanel(map),cc.xyw(builder.getColumn(), builder
				.getRow(), 8));
		builder.nextLine(2);
		map.append(builder,"Field.manager"); //$NON-NLS-1$
		builder.nextLine(2);
		
		map.appendSometimesReadOnly(builder,"Field.startDate"); //$NON-NLS-1$
		map.append(builder,"Field.currentDate"); //$NON-NLS-1$
		builder.nextLine(2);
		map.appendSometimesReadOnly(builder,"Field.finishDate"); //$NON-NLS-1$
		JComponent statusDateComponent = map.append(builder,"Field.statusDate"); //$NON-NLS-1$
		if (statusDateComponent != null)
			statusDateComponent.setToolTipText(Messages.getString("ProjectInformationDialog.StatusDateTooltip")); //$NON-NLS-1$
		
		builder.nextLine(2);
		map.append(builder,"Field.forward"); //$NON-NLS-1$
		builder.nextColumn(2);
		map.append(builder,"Field.baseCalendar"); //$NON-NLS-1$
		builder.nextLine(2);
		map.append(builder,"Field.priority"); //$NON-NLS-1$
		map.append(builder,"Field.projectStatus"); //$NON-NLS-1$
		builder.nextLine(2);
		map.append(builder,"Field.projectType"); //$NON-NLS-1$
		map.append(builder,"Field.expenseType"); //$NON-NLS-1$
		builder.nextLine(2);
		map.append(builder,"Field.projectDivision"); //$NON-NLS-1$
		map.append(builder,"Field.projectGroup"); //$NON-NLS-1$
		builder.nextLine(2);
		map.append(builder,"Field.netPresentValue"); //$NON-NLS-1$
		map.append(builder,"Field.benefit"); //$NON-NLS-1$
		builder.nextLine(2);
		map.append(builder,"Field.risk"); //$NON-NLS-1$
		builder.nextLine(2);

		if (!Environment.getStandAlone()){
			map.append(builder,"Field.accessControlPolicy",3); //$NON-NLS-1$
			builder.nextLine(2);
		}
		Collection extraFields = FieldDictionary.extractExtraFields(FieldDictionary.getInstance().getProjectFields(),false);
		JComponent extra = createFieldsPanel(map, extraFields);
		if (extra != null) {
			builder.add(extra,cc.xyw(builder.getColumn(), builder
					.getRow(), 7));
		}
		return builder.getPanel();
	}
	
	private JComponent createStatisticsPanel(){
		FieldComponentMap map = createMap();
		FormLayout layout = new FormLayout(
		        "p, 3dlu, 50dlu, 20dlu, p, 3dlu, 50dlu:grow", //$NON-NLS-1$
		"p,p,p,p,p, 10dlu, p,p,p, 10dlu, p,p,p, 10dlu, p,p,p"); //$NON-NLS-1$

		DefaultFormBuilder builder = new DefaultFormBuilder(layout);
		builder.setDefaultDialogBorder();
		CellConstraints cc = new CellConstraints();
		builder.setDefaultDialogBorder();
		builder.add(createHeaderFieldsPanel(map),cc.xyw(builder.getColumn(), builder
				.getRow(), 7));
		builder.nextLine(2);
		map.appendReadOnly(builder,"Field.startDate"); //$NON-NLS-1$
		map.appendReadOnly(builder,"Field.finishDate"); //$NON-NLS-1$
		builder.nextLine(2);
		map.appendReadOnly(builder,"Field.baselineStart"); //$NON-NLS-1$
		map.appendReadOnly(builder,"Field.baselineFinish"); //$NON-NLS-1$
		builder.nextLine(2);
		map.appendReadOnly(builder,"Field.actualStart"); //$NON-NLS-1$
		map.appendReadOnly(builder,"Field.actualFinish"); //$NON-NLS-1$
		
		builder.nextLine(2);
		map.appendReadOnly(builder,"Field.duration"); //$NON-NLS-1$
		map.appendReadOnly(builder,"Field.baselineDuration"); //$NON-NLS-1$
		builder.nextLine(2);
		map.appendReadOnly(builder,"Field.actualDuration"); //$NON-NLS-1$
		map.appendReadOnly(builder,"Field.remainingDuration"); //$NON-NLS-1$
		
		builder.nextLine(2);
		map.appendReadOnly(builder,"Field.work"); //$NON-NLS-1$
		map.appendReadOnly(builder,"Field.baselineWork"); //$NON-NLS-1$
		builder.nextLine(2);
		map.appendReadOnly(builder,"Field.actualWork"); //$NON-NLS-1$
		map.appendReadOnly(builder,"Field.remainingWork"); //$NON-NLS-1$
		
		builder.nextLine(2);
		map.appendReadOnly(builder,"Field.cost"); //$NON-NLS-1$
		map.appendReadOnly(builder,"Field.baselineCost"); //$NON-NLS-1$
		builder.nextLine(2);
		map.appendReadOnly(builder,"Field.actualCost"); //$NON-NLS-1$
		map.appendReadOnly(builder,"Field.remainingCost"); //$NON-NLS-1$
		return builder.getPanel();
	}

	protected JComponent createHeaderFieldsPanel(FieldComponentMap map) {
		FormLayout layout = new FormLayout(
        "p, 3dlu, 300dlu", //$NON-NLS-1$
		  "p"); //$NON-NLS-1$
		DefaultFormBuilder builder = new DefaultFormBuilder(layout);
		map.append(builder,"Field.name"); //$NON-NLS-1$
		return builder.getPanel();
	}
	

	public void documentSelected(DocumentSelectedEvent evt) {
		setObject(evt.getCurrent().getProject());
		updateAll();
	}

	public void selectionChanged(SelectionNodeEvent e) {
		
	}
	public void objectChanged(ObjectEvent objectEvent) {
		if (!isVisible())
			return;
		Object obj = objectEvent.getObject();
		Project project = (Project)getObject();
		boolean update = false;
		if (obj == getObject()) {
			update = true;
		} else if (obj instanceof Task) {
			if (((Task)obj).getProject() == project)
				update = true;
				
		} else if (obj instanceof Resource) {
			if (((Resource)obj).getDocument() == project.getResourcePool())
				update = true;
		}
		if (update)
			updateAll();
	}
	
	public void setObject(Object object) {
		super.setObject(object);
		if (object != null)	
			updateAll();
	}	

	public void onOk() {
		if (!bind(false))
			return;
		Project project = (Project) getObject();
		if (project != null && !project.isStatusDateSet())
			project.setStatusDate(System.currentTimeMillis());
		setDialogResult(javax.swing.JOptionPane.OK_OPTION);
		setVisible(false);
	}

}
