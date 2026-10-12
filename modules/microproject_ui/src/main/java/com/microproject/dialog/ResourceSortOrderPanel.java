/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.dialog;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.microproject.field.Field;
import com.microproject.strings.Messages;
/** Single-key Sort Order chooser sharing ResourceView's header sort contract. */
public final class ResourceSortOrderPanel extends JPanel {
	private final JComboBox<FieldChoice> fieldChoice = new JComboBox<>();
	private final JComboBox<String> direction = new JComboBox<>(new String[] {
		Messages.getString("ResourceSortOrder.Ascending"), Messages.getString("ResourceSortOrder.Descending") });

	public ResourceSortOrderPanel(List<Field> fields, String selectedFieldId, boolean ascending) {
		super(new GridBagLayout());
		for (Field field : fields) fieldChoice.addItem(new FieldChoice(field));
		for (int index = 0; index < fieldChoice.getItemCount(); index++) {
			if (selectedFieldId != null && selectedFieldId.equals(fieldChoice.getItemAt(index).field.getId())) {
				fieldChoice.setSelectedIndex(index);
				break;
			}
		}
		direction.setSelectedIndex(ascending ? 0 : 1);
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.insets = new Insets(4, 5, 4, 5);
		constraints.anchor = GridBagConstraints.WEST;
		constraints.gridx = 0;
		constraints.gridy = 0;
		add(new JLabel(Messages.getString("ResourceSortOrder.SortBy")), constraints);
		constraints.gridx = 1;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.weightx = 1;
		add(fieldChoice, constraints);
		constraints.gridx = 0;
		constraints.gridy = 1;
		constraints.fill = GridBagConstraints.NONE;
		constraints.weightx = 0;
		add(new JLabel(Messages.getString("ResourceSortOrder.Order")), constraints);
		constraints.gridx = 1;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.weightx = 1;
		add(direction, constraints);
		constraints.gridx = 0;
		constraints.gridy = 2;
		constraints.gridwidth = 2;
		add(new JLabel(Messages.getString("ResourceSortOrder.ViewOnly")), constraints);
	}

	public String getFieldId() {
		FieldChoice selected = (FieldChoice) fieldChoice.getSelectedItem();
		return selected == null ? null : selected.field.getId();
	}

	public boolean isAscending() {
		return direction.getSelectedIndex() == 0;
	}

	private record FieldChoice(Field field) {
		@Override public String toString() { return field.getName(); }
	}
}
