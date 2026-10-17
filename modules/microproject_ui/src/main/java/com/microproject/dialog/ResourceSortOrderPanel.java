/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.dialog;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.microproject.field.Field;
import com.microproject.pm.graphic.views.ResourceView;
import com.microproject.strings.Messages;

/** Sort Order chooser with up to three keys (Microsoft Application.Sort Key1-Key3), sharing ResourceView's view-only sort contract. */
public final class ResourceSortOrderPanel extends JPanel {
	private final JComboBox<FieldChoice>[] fieldChoices;
	private final JComboBox<String>[] directions;

	@SuppressWarnings("unchecked")
	public ResourceSortOrderPanel(List<Field> fields, List<ResourceView.SortKey> current) {
		super(new GridBagLayout());
		fieldChoices = new JComboBox[ResourceView.MAX_SORT_KEYS];
		directions = new JComboBox[ResourceView.MAX_SORT_KEYS];
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.insets = new Insets(4, 5, 4, 5);
		constraints.anchor = GridBagConstraints.WEST;
		for (int index = 0; index < fieldChoices.length; index++) {
			JComboBox<FieldChoice> choice = new JComboBox<>();
			choice.setName("microproject.resourceSort.key" + (index + 1));
			// Only the primary key is mandatory; later keys may be left empty.
			if (index > 0) choice.addItem(FieldChoice.NONE);
			for (Field field : fields) choice.addItem(new FieldChoice(field));
			JComboBox<String> direction = new JComboBox<>(new String[] {
				Messages.getString("ResourceSortOrder.Ascending"), Messages.getString("ResourceSortOrder.Descending") });
			direction.setName("microproject.resourceSort.direction" + (index + 1));
			if (index < current.size()) {
				ResourceView.SortKey key = current.get(index);
				for (int option = 0; option < choice.getItemCount(); option++) {
					FieldChoice item = choice.getItemAt(option);
					if (item.field != null && item.field.getId().equals(key.fieldId())) choice.setSelectedIndex(option);
				}
				direction.setSelectedIndex(key.ascending() ? 0 : 1);
			}
			fieldChoices[index] = choice;
			directions[index] = direction;
			constraints.gridy = index;
			constraints.gridx = 0;
			constraints.fill = GridBagConstraints.NONE;
			constraints.weightx = 0;
			add(new JLabel(Messages.getString(index == 0 ? "ResourceSortOrder.SortBy" : "ResourceSortOrder.ThenBy")), constraints);
			constraints.gridx = 1;
			constraints.fill = GridBagConstraints.HORIZONTAL;
			constraints.weightx = 1;
			add(choice, constraints);
			constraints.gridx = 2;
			constraints.fill = GridBagConstraints.NONE;
			constraints.weightx = 0;
			add(direction, constraints);
		}
		constraints.gridx = 0;
		constraints.gridy = fieldChoices.length;
		constraints.gridwidth = 3;
		add(new JLabel(Messages.getString("ResourceSortOrder.ViewOnly")), constraints);
	}

	/** Selected keys in priority order; a repeated field keeps its first (highest-priority) occurrence. */
	public List<ResourceView.SortKey> getKeys() {
		List<ResourceView.SortKey> keys = new ArrayList<>();
		for (int index = 0; index < fieldChoices.length; index++) {
			FieldChoice selected = (FieldChoice) fieldChoices[index].getSelectedItem();
			if (selected == null || selected.field == null) continue;
			String id = selected.field.getId();
			if (keys.stream().noneMatch(key -> key.fieldId().equals(id)))
				keys.add(new ResourceView.SortKey(id, directions[index].getSelectedIndex() == 0));
		}
		return List.copyOf(keys);
	}

	/** A null field is the explicit "(none)" choice; a rendered label keeps every popup row selectable. */
	private record FieldChoice(Field field) {
		static final FieldChoice NONE = new FieldChoice(null);
		@Override public String toString() {
			return field == null ? Messages.getString("ResourceSortOrder.None") : field.getName();
		}
	}
}
