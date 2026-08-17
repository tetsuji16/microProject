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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.microproject.field.Field;
import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.model.NodeModel;
import com.microproject.grouping.core.transform.ViewTransformer;
import com.microproject.grouping.core.transform.filtering.ColumnValueFilter;
import com.microproject.pm.graphic.model.cache.NodeModelCache;
import com.microproject.pm.graphic.model.cache.VisibleNodes;
import com.microproject.pm.graphic.model.transform.NodeCacheTransformer;
import com.microproject.pm.graphic.spreadsheet.common.CommonSpreadSheet;
import com.microproject.strings.Messages;

/**
 * Per-column auto-filter dialog (issue #205): lists the distinct values of a
 * column and lets the user keep only the checked ones, mirroring the MS Project
 * column-filter dropdown. The active filter is applied to the view's hidden
 * filter ({@link ColumnValueFilter}) so both the table and the chart stay in
 * sync.
 */
public final class AutoFilterDialog extends AbstractDialog {
	private static final long serialVersionUID = 1L;

	private final CommonSpreadSheet spreadSheet;
	private Field field;
	private JComboBox<Field> fieldCombo;
	private final JPanel valuesPanel = new JPanel();
	private List<JCheckBox> checkBoxes = new ArrayList<>();
	private JButton apply;
	private JButton clear;

	public AutoFilterDialog(Frame owner, CommonSpreadSheet spreadSheet, Field field) {
		super(owner, Messages.getString("AutoFilterDialog.Title"), false);
		this.spreadSheet = spreadSheet;
		this.field = field;
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
	}

	public static void open(Frame owner, CommonSpreadSheet spreadSheet, Field field) {
		new AutoFilterDialog(owner, spreadSheet, field).setVisible(true);
	}

	@Override
	public JComponent createContentPanel() {
		valuesPanel.setLayout(new java.awt.GridLayout(0, 1));
		valuesPanel.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));

		JComboBox<Field> combo = new JComboBox<>(availableFields().toArray(new Field[0]));
		if (field != null)
			combo.setSelectedItem(field);
		combo.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				field = (Field) combo.getSelectedItem();
				rebuildValues();
			}
		});
		fieldCombo = combo;

		JScrollPane scroll = new JScrollPane(valuesPanel);
		scroll.setPreferredSize(new Dimension(320, 280));

		JPanel content = new JPanel(new BorderLayout(6, 6));
		JPanel fieldRow = new JPanel(new BorderLayout(6, 0));
		fieldRow.add(new JLabel(Messages.getString("AutoFilterDialog.Field")), BorderLayout.WEST);
		fieldRow.add(combo, BorderLayout.CENTER);
		content.add(fieldRow, BorderLayout.NORTH);
		content.add(scroll, BorderLayout.CENTER);
		rebuildValues();
		return content;
	}

	@Override
	public ButtonPanel createButtonPanel() {
		ButtonPanel panel = new ButtonPanel();
		apply = new JButton(Messages.getString("AutoFilterDialog.Apply"));
		clear = new JButton(Messages.getString("AutoFilterDialog.Clear"));
		panel.addButton(apply);
		panel.addButton(clear);
		apply.addActionListener(e -> {
			applyFilter();
			dispose();
		});
		clear.addActionListener(e -> {
			clearFilter();
			dispose();
		});
		return panel;
	}

	@Override
	public void onCancel() {
		dispose();
	}

	@Override
	protected boolean hasOkAndCancelButtons() {
		return false;
	}

	private List<Field> availableFields() {
		List<Field> fields = new ArrayList<>();
		if (spreadSheet != null && spreadSheet.getAvailableFields() != null)
			fields.addAll(spreadSheet.getAvailableFields());
		Collections.sort(fields);
		return fields;
	}

	private void rebuildValues() {
		checkBoxes = new ArrayList<>();
		valuesPanel.removeAll();
		if (field == null) {
			valuesPanel.add(new JLabel(Messages.getString("AutoFilterDialog.NoValues")));
			valuesPanel.revalidate();
			return;
		}
		for (String value : distinctValues(field)) {
			JCheckBox box = new JCheckBox(value);
			box.setSelected(true);
			checkBoxes.add(box);
			valuesPanel.add(box);
		}
		if (checkBoxes.isEmpty()) {
			valuesPanel.add(new JLabel(Messages.getString("AutoFilterDialog.NoValues")));
		}
		valuesPanel.revalidate();
	}

	private List<String> distinctValues(Field f) {
		TreeSet<String> values = new TreeSet<>();
		NodeModelCache cache = spreadSheet == null ? null : spreadSheet.getCache();
		if (cache == null)
			return new ArrayList<>(values);
		NodeModel model = cache.getModel();
		if (model == null)
			return new ArrayList<>(values);
		for (Iterator<?> it = model.iterator(); it.hasNext();) {
			Object obj = it.next();
			if (!(obj instanceof Node node))
				continue;
			Object impl = node.getImpl();
			if (impl == null)
				continue;
			String text = f.getText(impl, null);
			values.add(text == null ? "" : text);
		}
		return new ArrayList<>(values);
	}

	private void applyFilter() {
		List<String> selected = new ArrayList<>();
		for (JCheckBox box : checkBoxes)
			if (box.isSelected())
				selected.add(box.getText());
		ViewTransformer transformer = viewTransformer();
		if (transformer == null)
			return;
		if (selected.isEmpty()) {
			transformer.setHiddenFilter(null);
			return;
		}
		ColumnValueFilter filter = new ColumnValueFilter(field);
		filter.setAcceptedValues(selected, false, false);
		transformer.setHiddenFilter(filter);
	}

	private void clearFilter() {
		ViewTransformer transformer = viewTransformer();
		if (transformer != null)
			transformer.setHiddenFilter(null);
	}

	private ViewTransformer viewTransformer() {
		NodeModelCache cache = spreadSheet == null ? null : spreadSheet.getCache();
		if (cache == null)
			return null;
		VisibleNodes visible = cache.getVisibleNodes();
		if (visible == null || !(visible.getTransformer() instanceof NodeCacheTransformer transformer))
			return null;
		return transformer.getTransformer();
	}
}
