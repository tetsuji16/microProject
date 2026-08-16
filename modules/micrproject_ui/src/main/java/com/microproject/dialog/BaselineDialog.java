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

import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JRadioButton;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.microproject.pm.graphic.frames.GraphicManager;
import com.microproject.configuration.Settings;
import com.microproject.pm.snapshot.SnapshottableImpl;
import com.microproject.strings.Messages;

public final class BaselineDialog extends AbstractDialog {
	private static final long serialVersionUID = 1L;
	boolean hasTasksSelected;
    boolean entire = true;
	public static class Form {
		int baselineNumber = 0; // baselines start at 1
		boolean entireProject = true;
		/**
		 * @return Returns the baselineNumber.
		 */
		public int getBaselineNumber() {
			return baselineNumber;
		}
		/**
		 * @param baselineNumber The baselineNumber to set.
		 */
		public void setBaselineNumber(int baselineNumber) {
			this.baselineNumber = baselineNumber;
		}
		/**
		 * @return Returns the entireProject.
		 */
		public boolean isEntireProject() {
			return entireProject;
		}
		/**
		 * @param entireProject The entireProject to set.
		 */
		public void setEntireProject(boolean entireProject) {
			this.entireProject = entireProject;
		}
	}
	private Form form;
	
	// use property utils to copy to project like struts

	JComboBox baseline;
	JRadioButton entireProject;
	JRadioButton selectedTasks;
	ButtonGroup radioGroup;

	public final void setHasTasksSelected(boolean hasTasksSelected) {
		this.hasTasksSelected = hasTasksSelected;
    	selectedTasks.setEnabled(hasTasksSelected);
    	if (!hasTasksSelected) {
    		entire = true;
    	}
    		
	}
	protected boolean bind(boolean get) {
		if (form == null)
			return false;
		if (get) {
			selectedTasks.setSelected(!entire);
			entireProject.setSelected(entire);
			baseline.setSelectedIndex(form.getBaselineNumber());
		} else {
			form.setEntireProject(entire);
			form.setBaselineNumber(baseline.getSelectedIndex());
		}
		return true;
	}
	public static BaselineDialog getInstance(GraphicManager graphicManager, Form project, boolean save, boolean hasTasksSelected) {
//		BaselineDialog instance = graphicManager.getBaselineDialog();
		BaselineDialog instance =null;// having problems with the radio buttons, so I'm creating a new one each time
		if (instance == null) {
			instance = new BaselineDialog(graphicManager.getFrame(), project,hasTasksSelected);
			graphicManager.setBaselineDialog(instance);
		} else
			instance.setHasTasksSelected(hasTasksSelected);
		instance.setTitle(Messages.getString( save ? "Text.SaveBaseline" : "Text.ClearBaseline")); //$NON-NLS-1$ //$NON-NLS-2$
		instance.addDocHelp(save ? "Save_Baseline_Dialog" : "Clear_Baseline_Dialog");
		
		return instance;
	}

	private BaselineDialog(Frame owner, Form baselineForm, boolean hasTasksSelected) {
		super(owner, "", true); //$NON-NLS-1$
		this.hasTasksSelected = hasTasksSelected;
		if (baselineForm != null)
			form = baselineForm;
		else
			form = new Form();
		
	}

	// Component Creation and Initialization **********************************

	/**
	 * Creates, intializes and configures the UI components. Real applications
	 * may further bind the components to underlying models.
	 */
	protected void initControls() {
		baseline = new JComboBox(SnapshottableImpl.getSnapshotNames());

		entireProject = new JRadioButton(Messages.getString("Text.EntireProject")); //$NON-NLS-1$
		selectedTasks = new JRadioButton(Messages.getString("Text.SelectedTasks")); //$NON-NLS-1$
		
		// for some strange reason, the value of the buttons is not correct in bind at the end, so I am using a listener instead
		entireProject.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				entire = ((JRadioButton)e.getSource()).isSelected();
			}});
		selectedTasks.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				entire = !((JRadioButton)e.getSource()).isSelected();
			}});
		radioGroup = new ButtonGroup();
		radioGroup.add(entireProject);
		radioGroup.add(selectedTasks);
    	setHasTasksSelected(hasTasksSelected);

		bind(true);
	}


	// Building *************************************************************

	/**
	 * Builds the panel. Initializes and configures components first, then
	 * creates a FormLayout, configures the layout, creates a builder, sets a
	 * border, and finally adds the components.
	 * 
	 * @return the built panel
	 */

	public JComponent createContentPanel() {
		// Separating the component initialization and configuration
		// from the layout code makes both parts easier to read.
		initControls();
		FormLayout layout = new FormLayout("default, 3dlu, fill:80dlu:grow", // cols //$NON-NLS-1$
				"p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p"); // rows //$NON-NLS-1$

		// Create a builder that assists in adding components to the container.
		// Wrap the panel with a standardized border.
		DefaultFormBuilder builder = new DefaultFormBuilder(layout);
		builder.setDefaultDialogBorder();
		CellConstraints cc = new CellConstraints();
		builder.append(Messages.getString("BaselineDialog.Baseline")); //$NON-NLS-1$
		builder.append(baseline);
		builder.nextLine(2);
		builder.addSeparator(""); //$NON-NLS-1$
		builder.nextLine();
		builder.append(Messages.getString("BaselineDialog.For")); //$NON-NLS-1$
		builder.nextLine(2);
		builder.append(entireProject);
		builder.nextLine(2);
		builder.append(selectedTasks);
		return builder.getPanel();
	}

	/**
	 * @return Returns the form.
	 */
	public Form getForm() {
		return form;
	}
	
	public Object getBean() {
		return form;
	}
}

