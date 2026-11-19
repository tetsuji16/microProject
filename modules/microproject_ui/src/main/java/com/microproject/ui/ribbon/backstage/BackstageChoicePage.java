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
import java.awt.Dimension;
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
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;

/**
 * Shared Office-style Backstage shape: a list of choices on the left and their
 * detail on the right.  Every section stays attached so every command assigned
 * to the page remains physically discoverable; choosing a left entry brings its
 * section into view.  This is a pure view: all content is supplied by the caller.
 */
public class BackstageChoicePage extends JPanel {
	private static final long serialVersionUID = 1L;
	private static final int CHOICE_WIDTH = 220;
	/** Client property that marks the currently chosen left entry. */
	public static final String SELECTED_PROPERTY = "microproject.backstageChoice.selected";

	/** One left entry and the right-hand detail it points at. */
	public record Section(String id, String text, JComponent content, Runnable onSelect) {
		public Section {
			Objects.requireNonNull(id, "id");
			Objects.requireNonNull(content, "content");
			if (onSelect == null) onSelect = () -> { };
		}
	}

	private final Map<String, JButton> choiceButtons = new LinkedHashMap<>();
	private final Map<String, JComponent> sections = new LinkedHashMap<>();
	private final JScrollPane detailScroll;

	protected BackstageChoicePage(String pageName, List<Section> choices) {
		super(new GridBagLayout());
		setName("officeBackstage" + pageName + "Page");
		setOpaque(false);
		Objects.requireNonNull(choices, "choices");
		if (choices.isEmpty()) throw new IllegalArgumentException("A Backstage choice page needs at least one choice");

		JPanel list = new JPanel();
		list.setName("officeBackstage" + pageName + "Choices");
		list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
		list.setOpaque(false);
		list.setPreferredSize(new Dimension(CHOICE_WIDTH, 0));

		JPanel stack = new JPanel();
		stack.setName("officeBackstage" + pageName + "Sections");
		stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));
		stack.setOpaque(false);

		for (Section choice : choices) {
			JButton button = new JButton(choice.text());
			button.setName("officeBackstage" + pageName + "Choice-" + choice.id());
			button.setHorizontalAlignment(SwingConstants.LEFT);
			button.setAlignmentX(Component.LEFT_ALIGNMENT);
			button.setContentAreaFilled(false);
			button.setBorderPainted(false);
			button.setFocusPainted(true);
			button.setBorder(BorderFactory.createEmptyBorder(6, 4, 6, 4));
			button.setMaximumSize(new Dimension(CHOICE_WIDTH - 8, button.getPreferredSize().height));
			button.addActionListener(event -> {
				select(choice.id());
				choice.onSelect().run();
			});
			choiceButtons.put(choice.id(), button);
			list.add(button);
			list.add(Box.createVerticalStrut(6));

			JComponent content = choice.content();
			content.setName("officeBackstage" + pageName + "Section-" + choice.id());
			content.setAlignmentX(Component.LEFT_ALIGNMENT);
			sections.put(choice.id(), content);
			stack.add(content);
			stack.add(Box.createVerticalStrut(18));
		}
		list.add(Box.createVerticalGlue());

		detailScroll = new JScrollPane(stack, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		detailScroll.setName("officeBackstage" + pageName + "Detail");
		detailScroll.setBorder(BorderFactory.createEmptyBorder());
		detailScroll.setOpaque(false);
		detailScroll.getViewport().setOpaque(false);

		GridBagConstraints left = new GridBagConstraints();
		left.gridx = 0;
		left.gridy = 0;
		left.weightx = 0;
		left.weighty = 1;
		left.anchor = GridBagConstraints.NORTHWEST;
		left.fill = GridBagConstraints.BOTH;
		left.insets = new Insets(0, 0, 0, 24);
		add(list, left);
		GridBagConstraints right = new GridBagConstraints();
		right.gridx = 1;
		right.gridy = 0;
		right.weightx = 1;
		right.weighty = 1;
		right.anchor = GridBagConstraints.NORTHWEST;
		right.fill = GridBagConstraints.BOTH;
		add(detailScroll, right);

		select(choices.getFirst().id());
	}

	/** Marks an entry chosen and brings its detail into view without hiding the others. */
	public void select(String id) {
		JComponent target = sections.get(id);
		if (target == null) return;
		for (Map.Entry<String, JButton> entry : choiceButtons.entrySet()) {
			boolean selected = entry.getKey().equals(id);
			entry.getValue().putClientProperty(SELECTED_PROPERTY, selected);
			entry.getValue().setFont(entry.getValue().getFont().deriveFont(selected ? Font.BOLD : Font.PLAIN));
		}
		target.scrollRectToVisible(new java.awt.Rectangle(0, 0, Math.max(1, target.getWidth()),
				Math.max(1, target.getHeight())));
	}

	/** The left-list button for an entry, or {@code null} when the id is unknown. */
	public AbstractButton choiceButton(String id) { return choiceButtons.get(id); }

	/** The right-hand detail for an entry, or {@code null} when the id is unknown. */
	public JComponent section(String id) { return sections.get(id); }

	/** The scrolling host of all detail sections. */
	public JComponent detailHost() { return detailScroll; }

	/** All label and button texts below a root, used by tests and diagnostics. */
	static List<String> texts(Component root) {
		List<String> result = new ArrayList<>();
		collect(root, result);
		return result;
	}

	private static void collect(Component component, List<String> result) {
		if (component instanceof JLabel label && label.getText() != null) result.add(label.getText());
		if (component instanceof AbstractButton button && button.getText() != null) result.add(button.getText());
		if (component instanceof java.awt.Container container)
			for (Component child : container.getComponents()) collect(child, result);
	}
}
