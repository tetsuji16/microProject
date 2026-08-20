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
import java.awt.Dimension;
import java.awt.Frame;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.prefs.Preferences;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.AbstractTableModel;

import com.microproject.configuration.Configuration;
import com.microproject.field.Field;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.Task;
import com.microproject.util.Alert;
import com.microproject.help.HelpUtil;
import com.microproject.util.PopupDialogSupport;

/** Central editor for task custom-field names, values, and optional lookup validation. */
public final class CustomFieldsDialogBox extends JDialog {
	private static final long serialVersionUID = 1L;
	private static final Preferences PREFS = Preferences.userNodeForPackage(CustomFieldsDialogBox.class).node("lookups");
	private final List<Field> fields;
	private final List<Task> tasks;
	private final JComboBox<Field> fieldChoice;
	private final JCheckBox restrictValues = new JCheckBox(UsabilityStrings.text("fields.restrict"));
	private final JTextField lookupValues = new JTextField(32);
	private final ValueModel model = new ValueModel();

	public CustomFieldsDialogBox(Frame owner, Project project, List<Task> selectedTasks) {
		super(owner, UsabilityStrings.text("fields.title"), false);
		HelpUtil.addDocHelp(getRootPane(), "Custom_Fields");
		getAccessibleContext().setAccessibleDescription(UsabilityStrings.text("fields.savedHint"));
		PopupDialogSupport.bindEscapeToDispose(this);
		fields = Configuration.getInstance().getFieldDictionary().getTaskFields().stream()
			.filter(Field::isCustom).sorted(Comparator.comparing(Field::getName)).toList();
		tasks = selectedTasks == null || selectedTasks.isEmpty() ? allTasks(project) : List.copyOf(selectedTasks);
		fieldChoice = new JComboBox<>(fields.toArray(Field[]::new));
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setLayout(new BorderLayout(6, 6));
		JPanel header = new JPanel();
		header.add(new JLabel(UsabilityStrings.text("fields.field"))); header.add(fieldChoice);
		JButton rename = new JButton(UsabilityStrings.text("fields.rename"));
		rename.addActionListener(event -> { Field field = selectedField(); if (field != null) { FieldAliasDialog.doRename(field); fieldChoice.repaint(); model.fireTableStructureChanged(); } });
		header.add(rename); header.add(restrictValues); header.add(lookupValues);
		add(header, BorderLayout.NORTH);
		JTable table = new JTable(model);
		table.setRowHeight(Math.max(table.getRowHeight(), 24));
		table.setAutoCreateRowSorter(true);
		table.getAccessibleContext().setAccessibleName(UsabilityStrings.text("fields.accessible"));
		add(new JScrollPane(table), BorderLayout.CENTER);
		JPanel footer = new JPanel();
		JButton apply = new JButton(UsabilityStrings.text("fields.applyLookup"));
		apply.addActionListener(event -> saveLookup());
		JButton close = new JButton(UsabilityStrings.text("common.close")); close.addActionListener(event -> dispose());
		footer.add(new JLabel(UsabilityStrings.text("fields.savedHint"))); footer.add(apply); footer.add(close);
		add(footer, BorderLayout.SOUTH);
		fieldChoice.addActionListener(event -> { loadLookup(); model.fireTableDataChanged(); });
		restrictValues.addActionListener(event -> lookupValues.setEnabled(restrictValues.isSelected()));
		loadLookup();
		setMinimumSize(new Dimension(760, 480)); setSize(900, 620); setLocationRelativeTo(owner);
	}

	private static List<Task> allTasks(Project project) {
		List<Task> result = new ArrayList<>();
		for (var iterator = project.getTaskOutlineIterator(); iterator.hasNext();) result.add((Task) iterator.next());
		return result;
	}

	private Field selectedField() { return (Field) fieldChoice.getSelectedItem(); }
	private String prefKey(Field field) { return field.getId().replaceAll("[^A-Za-z0-9_.-]", "_"); }
	private void loadLookup() {
		Field field = selectedField(); if (field == null) return;
		String values = PREFS.get(prefKey(field), "");
		restrictValues.setSelected(!values.isBlank()); lookupValues.setText(values); lookupValues.setEnabled(!values.isBlank());
	}
	private void saveLookup() {
		Field field = selectedField(); if (field == null) return;
		String value = restrictValues.isSelected() ? lookupValues.getText().trim() : "";
		PREFS.put(prefKey(field), value); lookupValues.setEnabled(restrictValues.isSelected());
	}
	private boolean permitted(String value) {
		if (!restrictValues.isSelected() || value == null || value.isBlank()) return true;
		for (String allowed : lookupValues.getText().split("[,;\\n]", -1)) if (allowed.trim().equalsIgnoreCase(value.trim())) return true;
		return false;
	}

	private final class ValueModel extends AbstractTableModel {
		private static final long serialVersionUID = 1L;
		@Override public int getRowCount() { return tasks.size(); }
		@Override public int getColumnCount() { return 2; }
		@Override public String getColumnName(int column) { return column == 0 ? UsabilityStrings.text("common.task") : selectedField() == null ? UsabilityStrings.text("fields.value") : selectedField().getName(); }
		@Override public boolean isCellEditable(int row, int column) { return column == 1 && !tasks.get(row).isReadOnly(); }
		@Override public Object getValueAt(int row, int column) {
			if (column == 0) return tasks.get(row).getName();
			Field field = selectedField(); return field == null ? "" : field.getText(tasks.get(row), null);
		}
		@Override public void setValueAt(Object value, int row, int column) {
			Field field = selectedField(); String text = value == null ? "" : value.toString();
			if (field == null || !permitted(text)) { Alert.warn(UsabilityStrings.text("fields.invalidLookup")); return; }
			try { field.setText(tasks.get(row), text, null); tasks.get(row).setDirty(true); fireTableCellUpdated(row, column); }
			catch (Exception exception) { Alert.error(UsabilityStrings.text("fields.invalidValue") + " " + exception.getMessage()); }
		}
	}
}
