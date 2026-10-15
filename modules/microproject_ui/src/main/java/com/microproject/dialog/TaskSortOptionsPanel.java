/*
 * MIT License
 *
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
 */
package com.microproject.dialog;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import java.awt.event.ItemListener;

import javax.swing.JComboBox;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.microproject.grouping.core.transform.CommonTransformFactory;
import com.microproject.grouping.core.transform.sorting.TaskSortSpec;
import com.microproject.strings.Messages;

/** Secondary and tertiary keys plus the view-only Keep Outline option. */
public final class TaskSortOptionsPanel extends JPanel {
	private final JComboBox<CommonTransformFactory>[] fields;
	private final JComboBox<String>[] directions;
	private final JCheckBox keepOutline;

	@SuppressWarnings("unchecked")
	public TaskSortOptionsPanel(List<CommonTransformFactory> choices, TaskSortSpec current) {
		super(new GridBagLayout());
		fields = new JComboBox[] { new JComboBox<>(), new JComboBox<>() };
		directions = new JComboBox[] { directionBox(), directionBox() };
		for (int index = 0; index < fields.length; index++) {
			JComboBox<CommonTransformFactory> field = fields[index];
			field.setName(index == 0 ? "microproject.taskSort.secondary" : "microproject.taskSort.tertiary");
			directions[index].setName(index == 0 ? "microproject.taskSort.secondaryDirection" : "microproject.taskSort.tertiaryDirection");
			field.addItem(null);
			for (CommonTransformFactory choice : choices) {
				if (choice.getId() != null && !"Sorter.None".equals(choice.getId())) field.addItem(choice);
			}
		}
		for (int index = 0; index < Math.min(2, current.keys().size() - 1); index++) {
			TaskSortSpec.Key key = current.keys().get(index + 1);
			for (int option = 0; option < fields[index].getItemCount(); option++) {
				CommonTransformFactory item = fields[index].getItemAt(option);
				if (item != null && key.fieldId().equals(item.getId())) fields[index].setSelectedIndex(option);
			}
			directions[index].setSelectedIndex(key.ascending() ? 0 : 1);
		}
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(3, 5, 3, 5);
		c.gridy = 0;
		for (int index = 0; index < fields.length; index++) {
			c.gridx = 0; c.anchor = GridBagConstraints.WEST;
			add(new JLabel(Messages.getString(index == 0 ? "Sort.Secondary" : "Sort.Tertiary")), c);
			c.gridx = 1; c.fill = GridBagConstraints.HORIZONTAL; c.weightx = 1;
			add(fields[index], c);
			c.gridx = 2; c.weightx = 0; c.fill = GridBagConstraints.NONE;
			add(directions[index], c);
			c.gridy++;
		}
		keepOutline = new JCheckBox(Messages.getString("Sort.KeepOutline"), current.keepOutline());
		keepOutline.setName("microproject.taskSort.keepOutline");
		c.gridx = 0; c.gridwidth = 3; c.anchor = GridBagConstraints.WEST;
		add(keepOutline, c);
		c.gridy++;
		add(new JLabel(Messages.getString("Sort.KeepOutlineNote")), c);
	}

	private static JComboBox<String> directionBox() {
		return new JComboBox<>(new String[] { Messages.getString("ResourceSortOrder.Ascending"),
				Messages.getString("ResourceSortOrder.Descending") });
	}

	public List<TaskSortSpec.Key> getAdditionalKeys() {
		List<TaskSortSpec.Key> keys = new ArrayList<>();
		for (int index = 0; index < fields.length; index++) {
			CommonTransformFactory selected = (CommonTransformFactory) fields[index].getSelectedItem();
			if (selected != null) keys.add(new TaskSortSpec.Key(selected.getId(), directions[index].getSelectedIndex() == 0));
		}
		return List.copyOf(keys);
	}

	public boolean isKeepOutline() { return keepOutline.isSelected(); }
	public void addKeepOutlineListener(ItemListener listener) { keepOutline.addItemListener(listener); }
}
