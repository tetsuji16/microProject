/*******************************************************************************
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
 *******************************************************************************/
package com.microproject.dialog;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.function.BooleanSupplier;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.microproject.help.HelpUtil;
import com.microproject.pm.graphic.spreadsheet.SpreadSheet;
import com.microproject.strings.Messages;

/** Owns the shared predecessor/successor tab layout and its selection controls. */
final class TaskDependencyPanel extends JPanel {
	private static final long serialVersionUID = 1L;

	private final SpreadSheet spreadsheet;
	private final BooleanSupplier canEdit;
	private final JButton addButton;
	private final JButton removeButton;

	TaskDependencyPanel(TaskDependencySpreadsheet.Direction direction, JComponent headerFields,
			JScrollPane spreadsheetPane, SpreadSheet spreadsheet, Runnable addAction, Runnable removeAction,
			BooleanSupplier canEdit) {
		super(new BorderLayout(0, 4));
		this.spreadsheet = spreadsheet;
		this.canEdit = canEdit;

		JPanel header = new JPanel(new BorderLayout(0, 4));
		header.add(headerFields, BorderLayout.NORTH);
		JPanel actions = new JPanel(new BorderLayout(8, 0));
		actions.setOpaque(false);
		actions.add(new JLabel(Messages.format("Format.label", Messages.getString(direction.labelKey()))),
				BorderLayout.WEST);
		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 3, 0));
		buttons.setOpaque(false);
		addButton = new JButton(Messages.getString("Spreadsheet.Action.new")); //$NON-NLS-1$
		addButton.setName(direction.isPredecessors() ? "newPredecessorLink" : "newSuccessorLink"); //$NON-NLS-1$ //$NON-NLS-2$
		addButton.addActionListener(event -> addAction.run());
		removeButton = new JButton(Messages.getString("Text.Remove")); //$NON-NLS-1$
		removeButton.setName(direction.isPredecessors() ? "removePredecessorLink" : "removeSuccessorLink"); //$NON-NLS-1$ //$NON-NLS-2$
		removeButton.setEnabled(false);
		removeButton.addActionListener(event -> removeAction.run());
		buttons.add(addButton);
		buttons.add(removeButton);
		actions.add(buttons, BorderLayout.EAST);
		header.add(actions, BorderLayout.SOUTH);
		add(header, BorderLayout.NORTH);
		add(spreadsheetPane, BorderLayout.CENTER);

		spreadsheet.getSelectionModel().addListSelectionListener(event -> {
			if (!event.getValueIsAdjusting())
				refreshRemoveButton();
		});
		setPreferredSize(new Dimension(700, 420));
		setMinimumSize(new Dimension(480, 300));
		HelpUtil.addDocHelp(this, "Linking"); //$NON-NLS-1$
	}

	void setAddEnabled(boolean enabled) {
		addButton.setEnabled(enabled);
		refreshRemoveButton();
	}

	private void refreshRemoveButton() {
		removeButton.setEnabled(canEdit.getAsBoolean() && spreadsheet.getSelectedRowCount() > 0);
	}
}
