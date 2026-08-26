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
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.microproject.strings.Messages;

public final class RenameProjectDialog extends AbstractDialog {
	private static final long serialVersionUID = 1L;
	public static class Form {
		String name;
		Set projectNames;
		boolean saveAs;
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public Set getProjectNames() {
			return projectNames;
		}
		public void setProjectNames(Set projectNames) {
			this.projectNames = projectNames;
		}
		public boolean isSaveAs() {
			return saveAs;
		}
		public void setSaveAs(boolean saveAs) {
			this.saveAs = saveAs;
		}
		
		
	}
	private Form form;

	JTextField nameField;

	public static RenameProjectDialog getInstance(Frame owner, Form project) {
		return new RenameProjectDialog(owner, project);
	}

	private RenameProjectDialog(Frame owner, Form project) {
		super(owner, Messages.getString("RenameProjectDialog.RenameProject"), true); //$NON-NLS-1$
		if (project != null)
			this.form = project;
		else
			this.form = new Form();
	}

	// Component Creation and Initialization **********************************

	/**
	 * Creates, intializes and configures the UI components. Real applications
	 * may further bind the components to underlying models.
	 */
	protected void initControls() {
		nameField = new JTextField();
		nameField.getDocument().addDocumentListener(new DocumentListener() {
			@Override public void insertUpdate(DocumentEvent event) { updateOkState(); }
			@Override public void removeUpdate(DocumentEvent event) { updateOkState(); }
			@Override public void changedUpdate(DocumentEvent event) { updateOkState(); }
		});
		bind(true);
	}

	private void updateOkState() {
		if (ok == null) return;
		String text=nameField.getText();
		ok.setEnabled(!form.getProjectNames().contains(text)&&!text.isEmpty());
	}

	@Override
	protected boolean initialOkEnabledState() {
		String text=nameField.getText();
		return !form.getProjectNames().contains(text)&&!text.isEmpty();
	}
	
	@Override
	public void onOk() {
		if (!ok.isEnabled()) // enter key should not work if name not ok
			return;
		super.onOk();
	}

	protected boolean bind(boolean get) {
		if (form == null)
			return false;
		if (get) {
			String name=form.getName();
			String newName;
			if (name==null||name.length()==0) newName=Messages.getString("RenameProjectDialog.NewProject"); //$NON-NLS-1$
			else newName=name;
			for (int i=0;form.getProjectNames().contains(newName);i++){
				newName=name+"("+i+")"; //$NON-NLS-1$ //$NON-NLS-2$
			}
			
			nameField.setText(newName);
		} else {
			form.setName(nameField.getText());
		}
		return true;
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
		FormLayout layout = new FormLayout("250dlu:grow", // cols //$NON-NLS-1$
				"p, 3dlu,p, 3dlu"); // rows //$NON-NLS-1$

		// Create a builder that assists in adding components to the container.
		// Wrap the panel with a standardized border.
		DefaultFormBuilder builder = new DefaultFormBuilder(layout);
		builder.setDefaultDialogBorder();
		CellConstraints cc = new CellConstraints();
		if (form.isSaveAs()) builder.append(Messages.format("Format.threeParts",
				Messages.getString("Message.saveProjectAs1"), form.getName(), Messages.getString("Message.saveProjectAs2"))); //$NON-NLS-1$
		else builder.append(Messages.format("Format.threeParts",
				Messages.getString("Message.renameProject1"), form.getName(), Messages.getString("Message.renameProject2"))); //$NON-NLS-1$
		builder.nextLine(2);
		builder.append(createFieldPanel());
		return builder.getPanel();
	}
	public JComponent createFieldPanel(){
		FormLayout layout = new FormLayout("p,3dlu,100dlu:grow",// cols //$NON-NLS-1$
		"p"); // rows //$NON-NLS-1$
		DefaultFormBuilder builder = new DefaultFormBuilder(layout);
		builder.setDefaultDialogBorder();
		CellConstraints cc = new CellConstraints();
		builder.append(Messages.getString("RenameProjectDialog.NewProjectName"), nameField); //$NON-NLS-1$
		return builder.getPanel();
	}

	/**
	 * @return Returns the form.
	 */
	public Form getForm() {
		return form;
	}
	public Object getBean(){
		return form;
	}

}
