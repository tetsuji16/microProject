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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import com.microproject.pm.graphic.frames.GraphicManager;
import com.microproject.field.Field;
import com.microproject.strings.Messages;

public final class ColumnDialog extends AbstractDialog {
	private static final long serialVersionUID = 1L;
	JComboBox combo = null;
	List<Field> fieldList;
	List<Field> currentFields;
	Field field;
	public static Field getFieldFromDialog(Component component, List<Field> fieldList, List<Field> currentFields) {
		ColumnDialog dlg = new ColumnDialog(component,fieldList,currentFields);
		if (dlg.doModal())
			return dlg.getField();
		return null;
	}
	
	
	private ColumnDialog(Component component,List<Field> fieldList, List<Field> currentFields) {
		super(GraphicManager.getInstance(component).getFrame(), Messages.getString("ColumnDialog.InsertColumn"), true); //$NON-NLS-1$
		this.fieldList = fieldList;
		this.currentFields = currentFields;
		addDocHelp("Spreadsheet");
	}

	// Component Creation and Initialization **********************************

	/**
	 * Creates, intializes and configures the UI components. Real applications
	 * may further bind the components to underlying models.
	 */
	protected void initControls() {
		bind(true);
	}

	protected boolean bind(boolean get) {
		if (get) {
			ArrayList<Field> l = new ArrayList<>(fieldList);
			Collections.sort(l);
			combo = new JComboBox(new DefaultComboBoxModel(l.toArray()));
		} else {
			field = (Field)combo.getSelectedItem();
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
		FormLayout layout = new FormLayout("default, 3dlu, 120dlu:grow", // cols //$NON-NLS-1$
				"p"); // rows //$NON-NLS-1$

		// Create a builder that assists in adding components to the container.
		// Wrap the panel with a standardized border.
		DefaultFormBuilder builder = new DefaultFormBuilder(layout);
		builder.setDefaultDialogBorder();
		builder.append(Messages.getString("Text.Field"), combo); //$NON-NLS-1$
		return builder.getPanel();
	}
	public final Field getField() {
		return field;
	}

}

