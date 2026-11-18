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
package com.microproject.ui.ribbon.backstage;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import com.microproject.util.MarkupEscape;

/**
 * Office-style Backstage "Info" page: an identity heading, the commands assigned
 * to the page on the left, and read-only project properties on the right.
 *
 * <p>This class is a pure view. Every value and every action is supplied by the
 * caller, so the page can be built and verified without a window or a live
 * document. The two columns use a wrapping layout: when the available width is
 * too small the properties column moves below the commands column instead of
 * forcing a horizontal scrollbar.</p>
 */
public final class BackstageInfoPage extends JPanel {
	private static final long serialVersionUID = 1L;
	private static final int COLUMN_WIDTH = 250;
	private static final int DESCRIPTION_WIDTH = 220;
	private static final int VALUE_WIDTH = 180;

	/** A command assigned to the Info page, with its short display description. */
	public record Command(AbstractButton button, String description) { }

	/** A read-only label/value pair shown in the Project Information column. */
	public record Property(String label, String value) { }

	private final JLabel projectNameLabel;
	private final JLabel projectPathLabel;
	private final JComponent commandsPanel;
	private final JComponent propertiesPanel;
	private final Map<String, JLabel> valueLabels = new LinkedHashMap<>();

	/**
	 * @param projectName the current project's display name, never {@code null}
	 * @param filePath the current project's path, empty when it has not been saved
	 * @param commands the Info-page commands to show as large icon buttons
	 * @param properties the read-only project information rows
	 * @param openProjectInformation invoked when the Project Information heading is activated
	 * @param label resolves a {@code FileBackstage.info.*} key to localized text
	 */
	public BackstageInfoPage(String projectName, String filePath, List<Command> commands,
			List<Property> properties, Runnable openProjectInformation, Function<String, String> label) {
		super(new BorderLayout(0, 14));
		setName("officeBackstageInfoPage");
		setOpaque(false);
		Objects.requireNonNull(commands, "commands");
		Objects.requireNonNull(properties, "properties");
		Objects.requireNonNull(label, "label");

		JPanel heading = new JPanel();
		heading.setName("officeBackstageInfoHeading");
		heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
		heading.setOpaque(false);
		this.projectNameLabel = new JLabel(text(projectName, 520));
		this.projectNameLabel.setName("officeBackstageInfoProjectName");
		this.projectNameLabel.setFont(this.projectNameLabel.getFont().deriveFont(Font.BOLD, 22f));
		this.projectNameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		heading.add(this.projectNameLabel);
		this.projectPathLabel = new JLabel(text(filePath, 520));
		this.projectPathLabel.setName("officeBackstageInfoProjectPath");
		this.projectPathLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		this.projectPathLabel.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
		heading.add(this.projectPathLabel);
		add(heading, BorderLayout.NORTH);

		JPanel columns = new JPanel(new FlowLayout(FlowLayout.LEFT, 24, 0));
		columns.setName("officeBackstageInfoColumns");
		columns.setOpaque(false);
		this.commandsPanel = createCommands(commands);
		this.propertiesPanel = createProperties(properties, openProjectInformation, label);
		columns.add(this.commandsPanel);
		columns.add(this.propertiesPanel);
		add(columns, BorderLayout.CENTER);
	}

	/** The project-name label in the heading. */
	public JLabel getProjectNameLabel() {
		return projectNameLabel;
	}

	/** The file-path label in the heading; empty text for an unsaved project. */
	public JLabel getProjectPathLabel() {
		return projectPathLabel;
	}

	/** The left, command column. */
	public JComponent getCommandsPanel() {
		return commandsPanel;
	}

	/** The right, read-only property column. */
	public JComponent getPropertiesPanel() {
		return propertiesPanel;
	}

	/** The value label for a property, keyed by its visible label, or {@code null}. */
	public JLabel getPropertyValue(String label) {
		return valueLabels.get(label);
	}

	private static JComponent createCommands(List<Command> commands) {
		JPanel panel = new JPanel() {
			private static final long serialVersionUID = 1L;
			@Override public Dimension getPreferredSize() {
				return new Dimension(COLUMN_WIDTH, super.getPreferredSize().height);
			}
		};
		panel.setName("officeBackstageInfoCommands");
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setOpaque(false);
		for (Command command : commands) {
			if (command == null || command.button() == null) continue;
			JPanel item = new JPanel(new BorderLayout(0, 4));
			item.setName("officeBackstageInfoCommand");
			item.setOpaque(false);
			item.setAlignmentX(Component.LEFT_ALIGNMENT);
			AbstractButton button = command.button();
			button.setHorizontalAlignment(SwingConstants.LEFT);
			button.setAlignmentX(Component.LEFT_ALIGNMENT);
			item.add(button, BorderLayout.CENTER);
			if (command.description() != null && !command.description().isBlank()) {
				JLabel description = new JLabel(text(command.description(), DESCRIPTION_WIDTH));
				description.setName("officeBackstageInfoCommandDescription");
				description.setAlignmentX(Component.LEFT_ALIGNMENT);
				item.add(description, BorderLayout.SOUTH);
			}
			panel.add(item);
			panel.add(Box.createVerticalStrut(10));
		}
		panel.add(Box.createVerticalGlue());
		return panel;
	}

	private JComponent createProperties(List<Property> properties, Runnable openProjectInformation,
			Function<String, String> label) {
		JPanel panel = new JPanel() {
			private static final long serialVersionUID = 1L;
			@Override public Dimension getPreferredSize() {
				return new Dimension(COLUMN_WIDTH, super.getPreferredSize().height);
			}
		};
		panel.setName("officeBackstageInfoProperties");
		panel.setLayout(new BorderLayout(0, 10));
		panel.setOpaque(false);

		JButton link = new JButton("<html><u>" + MarkupEscape.escape(label.apply("projectInformation")) + "</u></html>");
		link.setName("officeBackstageInfoPropertiesLink");
		link.setContentAreaFilled(false);
		link.setBorderPainted(false);
		link.setFocusPainted(false);
		link.setHorizontalAlignment(SwingConstants.LEFT);
		link.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		link.setFont(link.getFont().deriveFont(Font.BOLD, 16f));
		link.setAlignmentX(Component.LEFT_ALIGNMENT);
		if (openProjectInformation != null) link.addActionListener(event -> openProjectInformation.run());
		panel.add(link, BorderLayout.NORTH);

		JPanel rows = new JPanel(new GridBagLayout());
		rows.setName("officeBackstageInfoPropertyRows");
		rows.setOpaque(false);
		GridBagConstraints nameConstraints = new GridBagConstraints();
		nameConstraints.gridx = 0;
		nameConstraints.gridy = 0;
		nameConstraints.weightx = 0;
		nameConstraints.anchor = GridBagConstraints.NORTHWEST;
		nameConstraints.insets = new Insets(0, 0, 6, 12);
		GridBagConstraints valueConstraints = new GridBagConstraints();
		valueConstraints.gridx = 1;
		valueConstraints.gridy = 0;
		valueConstraints.weightx = 1;
		valueConstraints.anchor = GridBagConstraints.NORTHWEST;
		valueConstraints.fill = GridBagConstraints.HORIZONTAL;
		valueConstraints.insets = new Insets(0, 0, 6, 0);
		for (Property property : properties) {
			JLabel name = new JLabel(property.label() + ":");
			name.setName("officeBackstageInfoPropertyLabel");
			name.setFont(name.getFont().deriveFont(Font.BOLD));
			rows.add(name, nameConstraints);
			JLabel value = new JLabel(text(property.value(), VALUE_WIDTH));
			value.setName("officeBackstageInfoPropertyValue");
			rows.add(value, valueConstraints);
			valueLabels.put(property.label(), value);
			nameConstraints.gridy++;
			valueConstraints.gridy++;
		}
		panel.add(rows, BorderLayout.CENTER);
		return panel;
	}

	/** Escaped, width-limited HTML so long values wrap instead of scrolling sideways. */
	private static String text(String value, int width) {
		if (value == null || value.isEmpty()) return "";
		return "<html><div style='width:" + width + "px'>" + MarkupEscape.escape(value) + "</div></html>";
	}

	/** All visible label texts below a root, used by tests and diagnostics. */
	static List<String> visibleTexts(Component root) {
		List<String> result = new ArrayList<>();
		collectTexts(root, result);
		return result;
	}

	private static void collectTexts(Component component, List<String> result) {
		if (component instanceof JLabel label) result.add(label.getText());
		if (component instanceof AbstractButton button) result.add(button.getText());
		if (component instanceof java.awt.Container container)
			for (Component child : container.getComponents()) collectTexts(child, result);
	}
}
