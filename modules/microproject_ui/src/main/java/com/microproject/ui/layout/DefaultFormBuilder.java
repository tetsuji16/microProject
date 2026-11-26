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
package com.microproject.ui.layout;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.util.ResourceBundle;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JSeparator;
import javax.swing.JPanel;
import javax.swing.border.Border;

/**
 * Fills a {@link FormLayout} panel row by row: labels and components are added at the
 * current column, which moves on by two (a component column and its gap) after each
 * addition, and {@link #nextLine(int)} moves down by the given number of grid rows.
 */
public class DefaultFormBuilder implements FormRowBuilder {
	private final JPanel panel;
	private final FormLayout layout;
	private int currentColumn = 1;
	private int currentRow = 1;
	private int leadingColumnOffset = 0;

	public DefaultFormBuilder(FormLayout layout) {
		this(new JPanel(), layout);
	}

	public DefaultFormBuilder(FormLayout layout, JPanel panel) {
		this(panel, layout);
	}

	public DefaultFormBuilder(JComponent panel, FormLayout layout) {
		this(asPanel(panel), layout);
	}

	public DefaultFormBuilder(FormLayout layout, ResourceBundle bundle) {
		this(layout);
	}

	public DefaultFormBuilder(JComponent panel, FormLayout layout, ResourceBundle bundle) {
		this(panel, layout);
	}

	private DefaultFormBuilder(JPanel panel, FormLayout layout) {
		this.panel = panel;
		this.layout = layout;
		this.panel.setLayout(layout);
	}

	public JPanel getPanel() {
		return panel;
	}

	@Override
	public int getColumn() {
		return currentColumn + leadingColumnOffset;
	}

	@Override
	public int getRow() {
		return currentRow;
	}

	public int getColumnCount() {
		return Math.max(1, layout.getColumnCount());
	}

	public void setLeadingColumnOffset(int leadingColumnOffset) {
		this.leadingColumnOffset = Math.max(0, leadingColumnOffset);
	}

	public void setDefaultDialogBorder() {
		panel.setBorder(BorderFactory.createEmptyBorder(9, 9, 9, 9));
	}

	public void setBorder(Border border) {
		panel.setBorder(border);
	}

	public void setBorder(Object border) {
		if (border instanceof Border) {
			setBorder((Border) border);
			return;
		}
		panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
	}

	public Component append(String text) {
		return add(new JLabel(text));
	}

	@Override
	public Component append(String text, Component component) {
		add(new JLabel(text));
		return add(component);
	}

	public Component append(String text, Component component, int columnSpan) {
		addLabel(text);
		return add(component, new CellConstraints().xyw(getColumn(), getRow(), Math.max(1, columnSpan)));
	}

	@Override
	public Component append(Component component) {
		return add(component);
	}

	public Component append(Component... components) {
		Component last = null;
		for (Component component : components) {
			last = add(component);
		}
		return last;
	}

	public Component add(Component component) {
		CellConstraints cc = new CellConstraints();
		panel.add(component, cc.xy(getColumn(), getRow()));
		currentColumn += 2;
		return component;
	}

	public Component add(Component component, Object constraints) {
		panel.add(component, constraints);
		currentColumn += 2;
		return component;
	}

	@Override
	public Component addSpanning(Component component, int col, int row, int colSpan) {
		return add(component, new CellConstraints().xyw(col, row, colSpan));
	}

	@Override
	public JLabel addLabel(String text) {
		JLabel label = new JLabel(text);
		add(label);
		return label;
	}

	public JLabel addLabel(String text, CellConstraints constraints) {
		JLabel label = new JLabel(text);
		panel.add(label, constraints);
		currentColumn += 2;
		return label;
	}

	public JSeparator addSeparator(String text) {
		JPanel separatorPanel = new JPanel(new BorderLayout(8, 0));
		separatorPanel.setOpaque(false);
		if (text != null && !text.isEmpty()) {
			JLabel label = new JLabel(text);
			label.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 4));
			separatorPanel.add(label, BorderLayout.WEST);
		}
		JSeparator separator = new JSeparator();
		separator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
		separatorPanel.add(separator, BorderLayout.CENTER);
		add(separatorPanel, new CellConstraints().xyw(getColumn(), getRow(), Math.max(1, getColumnCount() - getColumn() + 1)));
		nextLine();
		return separator;
	}

	public void nextLine() {
		nextLine(1);
	}

	@Override
	public void nextLine(int lineCount) {
		// This is the grid-row count from the original JGoodies builder API.
		// Most forms declare each content row and its spacer separately
		// ("p,3dlu,p"), so nextLine(2) must land on the next preferred-height
		// track.  Subtracting one puts controls on the 3dlu spacer and clips them.
		currentRow += Math.max(1, lineCount);
		currentColumn = 1;
	}

	public void nextColumn() {
		nextColumn(1);
	}

	@Override
	public void nextColumn(int gapColumns) {
		currentColumn += Math.max(1, gapColumns);
	}

	private static JPanel asPanel(JComponent component) {
		if (component instanceof JPanel) {
			return (JPanel) component;
		}
		throw new IllegalArgumentException("The builder requires a JPanel.");
	}
}
