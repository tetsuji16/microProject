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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Date;

import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JRadioButton;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.microproject.dialog.util.ComponentFactory;
import com.microproject.dialog.util.ExtDateField;
import com.microproject.options.CalendarOption;
import com.microproject.strings.Messages;

/**
 *
 */
public class UpdateProjectDialogBox extends AbstractDialog {
	private static final long serialVersionUID = 1L;
	public static class Form {
		Boolean update;
		Boolean progress;
		Boolean entireProject;
		Date updateDate = new Date(CalendarOption.getInstance().makeValidStart(System.currentTimeMillis(), true));		
		Date rescheduleDate = new Date(CalendarOption.getInstance().makeValidStart(System.currentTimeMillis(), true));
	
		public Date getActiveDate() {
			return update.booleanValue() ? updateDate : rescheduleDate;
		}
		
	    public Boolean getEntireProject() {
	        return entireProject;
	    }
	    public void setEntireProject(Boolean entireProject) {
	        this.entireProject = entireProject;
	    }
	    public Boolean getProgress() {
	        return progress;
	    }
	    public void setProgress(Boolean progress) {
	        this.progress = progress;
	    }
	    public Date getRescheduleDate() {
	        return rescheduleDate;
	    }
	    public void setRescheduleDate(Date rescheduleDate) {
	        this.rescheduleDate = rescheduleDate;
	    }
	    public Boolean getUpdate() {
	        return update;
	    }
	    public void setUpdate(Boolean update) {
	        this.update = update;
	    }
	    public Date getUpdateDate() {
	        return updateDate;
	    }
	    public void setUpdateDate(Date updateDate) {
	        this.updateDate = updateDate;
	    }
    }	
    
    private Form form;
    boolean hasTasksSelected;
    JRadioButton entireProject;
    JRadioButton selectedTask;
    ButtonGroup projectOrTask;
    JRadioButton update;
    JRadioButton reschedule;
    ButtonGroup updateOrReschedule;
	ExtDateField updateDateChooser;
	ExtDateField rescheduleDateChooser;
	JRadioButton progress;
	JRadioButton completeOrNotOnly;
	ButtonGroup progressCalculationType;

    

    
	public static UpdateProjectDialogBox getInstance(Frame owner, Form project, boolean hasTasksSelected) {
		return new UpdateProjectDialogBox(owner, project,hasTasksSelected);
	}

	private UpdateProjectDialogBox(Frame owner, Form project, boolean hasTasksSelected) {
		super(owner, Messages.getString("UpdateProjectDialogBox.UpdateProject"), true); //$NON-NLS-1$
		addDocHelp("Update_Project");
		this.hasTasksSelected = hasTasksSelected;
		if (project != null)
			this.form = project;
		else
			this.form = new Form();
	}
	
	protected void initControls() {
	    entireProject = new JRadioButton(Messages.getString("UpdateProjectDialogBox.EntireProject")); //$NON-NLS-1$
	    entireProject.setSelected(true);
	    selectedTask = new JRadioButton(Messages.getString("UpdateProjectDialogBox.SelectedTasks")); //$NON-NLS-1$
	    if (!hasTasksSelected)
	    	selectedTask.setEnabled(false);
	    projectOrTask = new ButtonGroup();
	    projectOrTask.add(entireProject);
	    projectOrTask.add(selectedTask);
	    
	    update = new JRadioButton();
	    update.setSelected(true);
	    reschedule = new JRadioButton();
	    updateOrReschedule = new ButtonGroup();
	    updateOrReschedule.add(update);
	    updateOrReschedule.add(reschedule);
	    
		updateDateChooser = ComponentFactory.createDateField();
		rescheduleDateChooser = ComponentFactory.createDateField();
		rescheduleDateChooser.setEnabled(false);
		
		progress= new JRadioButton(Messages.getString("UpdateProjectDialogBox.SetZeroHundred")); //$NON-NLS-1$
		progress.setSelected(true);
		completeOrNotOnly= new JRadioButton(Messages.getString("UpdateProjectDialogBox.SetZeroOrHundredOnly")); //$NON-NLS-1$
		progressCalculationType = new ButtonGroup();;
		progressCalculationType.add(progress);
		progressCalculationType.add(completeOrNotOnly);

		update.addActionListener(new ActionListener(){
		    public void actionPerformed(ActionEvent e){
		        progress.setEnabled(true);
		        completeOrNotOnly.setEnabled(true);
		        updateDateChooser.setEnabled(true);
		        rescheduleDateChooser.setEnabled(false);
		    }});
		reschedule.addActionListener(new ActionListener(){
		    public void actionPerformed(ActionEvent e){
		        progress.setEnabled(false);
		        completeOrNotOnly.setEnabled(false);
		        updateDateChooser.setEnabled(false);
		        rescheduleDateChooser.setEnabled(true);
		    }});




	}
	
	protected boolean bind(boolean get) {
		if (form == null)
			return false;
		if (get) {
		    entireProject.setSelected((form.getEntireProject()).booleanValue());
		    update.setSelected((form.getUpdate()).booleanValue());
		    progress.setSelected(form.getProgress().booleanValue());			
			updateDateChooser.setValue(form.getUpdateDate());
			rescheduleDateChooser.setValue(form.getRescheduleDate());
		} else {
			Boolean b1=Boolean.valueOf(entireProject.isSelected());
			form.setEntireProject(b1);
			Boolean b2=Boolean.valueOf(update.isSelected());
			form.setUpdate(b2);
			Boolean b3=Boolean.valueOf(progress.isSelected());
			form.setProgress(b3);	    		    
			long d1 = updateDateChooser.getDateValue().getTime();
			d1 = CalendarOption.getInstance().makeValidStart(d1, true);
			form.setUpdateDate(new Date(d1));
			long d2 = rescheduleDateChooser.getDateValue().getTime();
			d2 = CalendarOption.getInstance().makeValidStart(d2, true);
			form.setRescheduleDate(new Date(d2));
		}
		return true;
	}
	
	public JComponent createContentPanel() {
	
		initControls();
		
		FormLayout layout = new FormLayout(
		        "20dlu,3dlu,p, 3dlu,75dlu,3dlu,30dlu ", //$NON-NLS-1$
	    		  "p,1dlu,p,1dlu,p,10dlu,p,3dlu,p,3dlu,p,3dlu,p,3dlu,p,3dlu,p,3dlu,p,3dlu,p"); //$NON-NLS-1$

		DefaultFormBuilder builder = new DefaultFormBuilder(layout);
		builder.setDefaultDialogBorder();
		CellConstraints cc = new CellConstraints();

		builder.append(update);
		builder.append(Messages.getString("UpdateProjectDialogBox.UpdateWorkAsCompleteThrough")); //$NON-NLS-1$
		builder.append(updateDateChooser);
		builder.nextLine(2);
		builder.nextColumn(2);
		builder.add(progress,cc.xyw(builder.getColumn(), builder
				.getRow(), 5));
		builder.nextLine(2);
		builder.nextColumn(2);
		builder.add(completeOrNotOnly,cc.xyw(builder.getColumn(), builder
				.getRow(), 5));
		builder.nextLine(2);
		builder.append(reschedule);
		builder.append(Messages.getString("UpdateProjectDialogBox.RescheduleCompletedWorkToStartAfter")); //$NON-NLS-1$
		builder.append(rescheduleDateChooser);
		builder.nextLine(8);
		builder.addSeparator(""); //$NON-NLS-1$
		builder.nextLine();
		builder.append(Messages.getString("UpdateProjectDialogBox.For")); //$NON-NLS-1$
		builder.nextLine(2);
		builder.nextColumn(2);
		builder.append(entireProject);
		builder.nextLine(2);
		builder.nextColumn(2);
		builder.append(selectedTask);

		return builder.getPanel();
	}
	
	
	public Form getForm() {
		return form;
	}
	public Object getBean(){
		return form;
	}
	
}

