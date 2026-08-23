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
import java.util.Locale;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import com.microproject.pm.graphic.frames.GraphicManager;
import com.microproject.field.Field;
import com.microproject.strings.Messages;

public final class ColumnDialog extends AbstractDialog {
	private static final long serialVersionUID = 1L;
	private JComboBox<Field> combo = null;
	private JTextField filter = null;
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
			combo = new JComboBox<>();
			filter = new JTextField();
			filter.setToolTipText(Messages.getString("ColumnDialog.Filter.ToolTip")); //$NON-NLS-1$
			filter.getDocument().addDocumentListener(new DocumentListener() {
				@Override public void insertUpdate(DocumentEvent event) { updateFilteredFields(); }
				@Override public void removeUpdate(DocumentEvent event) { updateFilteredFields(); }
				@Override public void changedUpdate(DocumentEvent event) { updateFilteredFields(); }
			});
			updateFilteredFields();
		} else {
			field = combo.getItemCount() == 0 ? null : (Field) combo.getSelectedItem();
		}
		return get || field != null;
	}

	private void updateFilteredFields() {
		String query = filter == null ? "" : filter.getText(); //$NON-NLS-1$
		List<Field> filtered = filterFields(fieldList, currentFields, query);
		combo.setModel(new DefaultComboBoxModel<>(filtered.toArray(new Field[0])));
		if (!filtered.isEmpty()) combo.setSelectedIndex(0);
	}

	static List<Field> filterFields(List<Field> fields, List<Field> currentFields, String query) {
		String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT); //$NON-NLS-1$
		List<Field> result = new ArrayList<>();
		for (Field candidate : fields) {
			if (candidate == null || isCurrentField(candidate, currentFields)) continue;
			String searchable = (candidate.getName() + " " + candidate.getId()).toLowerCase(Locale.ROOT); //$NON-NLS-1$
			if (normalizedQuery.isEmpty() || searchable.contains(normalizedQuery)) result.add(candidate);
		}
		Collections.sort(result);
		return result;
	}

	private static boolean isCurrentField(Field candidate, List<Field> currentFields) {
		if (currentFields == null) return false;
		for (Field current : currentFields) {
			if (current == candidate || (current != null && current.getId().equals(candidate.getId()))) return true;
		}
		return false;
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
				"p, 3dlu, p"); // rows //$NON-NLS-1$

		// Create a builder that assists in adding components to the container.
		// Wrap the panel with a standardized border.
		DefaultFormBuilder builder = new DefaultFormBuilder(layout);
		builder.setDefaultDialogBorder();
		builder.append(Messages.getString("ColumnDialog.Filter"), filter); //$NON-NLS-1$
		builder.append(Messages.getString("Text.Field"), combo); //$NON-NLS-1$
		return builder.getPanel();
	}
	public final Field getField() {
		return field;
	}

}
