package com.jgoodies.forms.builder;

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

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

/**
 * ProjectLibre compatibility shim for the older JGoodies DefaultFormBuilder API.
 */
public class DefaultFormBuilder {
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

	public int getColumn() {
		return currentColumn + leadingColumnOffset;
	}

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

	public Component append(String text, Component component) {
		add(new JLabel(text));
		return add(component);
	}

	public Component append(String text, Component component, int columnSpan) {
		addLabel(text);
		return add(component, new CellConstraints().xyw(getColumn(), getRow(), Math.max(1, columnSpan)));
	}

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
		add(separatorPanel, new CellConstraints().xyw(getColumn(), getRow(), Math.max(1, getColumnCount() - currentColumn + 1)));
		nextLine();
		return separator;
	}

	public void nextLine() {
		nextLine(1);
	}

	public void nextLine(int gapRows) {
		currentRow += Math.max(1, gapRows);
		currentColumn = 1;
	}

	public void nextColumn() {
		nextColumn(1);
	}

	public void nextColumn(int gapColumns) {
		currentColumn += Math.max(1, gapColumns);
	}

	private static JPanel asPanel(JComponent component) {
		if (component instanceof JPanel) {
			return (JPanel) component;
		}
		JPanel panel = new JPanel(new BorderLayout());
		panel.add(component);
		return panel;
	}
}
