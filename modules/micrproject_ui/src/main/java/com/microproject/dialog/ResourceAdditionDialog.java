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

import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.microproject.strings.Messages;
import com.microproject.util.FlatUiSupport;

public final class ResourceAdditionDialog extends AbstractDialog {
	private static final long serialVersionUID = -2638004002538054771L;
	public static class Form {
		protected List<Object> selectedResources = new ArrayList<>();

		public List<Object> getSelectedResources() {
			return selectedResources;
		}

		public void setSelectedResources(List<Object> selectedResources) {
			this.selectedResources = selectedResources;
		}
	}

	private Form form;

	protected JList<Object> resources;
	protected JLabel field1Label;

	public static ResourceAdditionDialog getInstance(JFrame owner, Form form) {
		return new ResourceAdditionDialog(owner,form);
	}

	private ResourceAdditionDialog(JFrame owner, Form form) {
		super(owner, Messages.getString("ResourceAdditionDialog.ResourceMerging"), true); //$NON-NLS-1$
		this.form = (form==null)?new Form():form;
	}

	// Component Creation and Initialization **********************************

	/**
	 * Creates, intializes and configures the UI components. Real applications
	 * may further bind the components to underlying models.
	 */
	protected void initControls() {
		resources=new JList<>(form.getSelectedResources().toArray(new Object[0]));
		field1Label=new JLabel(Messages.getString("ResourceAdditionDialog.SelectResourcesToAdd")); //$NON-NLS-1$
		bind(true);
	}
	
	protected boolean bind(boolean get) {
		if (form == null)
			return false;
		if (get) {
		} else {
			//associationTable.finishCurrentOperations();
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
		initControls();
		FormLayout layout = new FormLayout("200dlu:grow", // cols //$NON-NLS-1$
				FlatUiSupport.preferredFormRows(5)); // rows //$NON-NLS-1$

		// Create a builder that assists in adding components to the container.
		// Wrap the panel with a standardized border.
		DefaultFormBuilder builder = new DefaultFormBuilder(layout);
		builder.setDefaultDialogBorder();
		CellConstraints cc = new CellConstraints();
		builder.append(createFieldPanel());
		builder.nextLine(2);
		builder.add(new JScrollPane(resources));
		return builder.getPanel();
	}
	public JComponent createFieldPanel(){
		FormLayout layout = new FormLayout("p",// cols //$NON-NLS-1$
		"p"); // rows //$NON-NLS-1$
		DefaultFormBuilder builder = new DefaultFormBuilder(layout);
		builder.setDefaultDialogBorder();
		CellConstraints cc = new CellConstraints();
		builder.append(field1Label);
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
