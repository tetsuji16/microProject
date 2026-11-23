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
 *
 * <p>The detail column narrows with the window instead of being laid out at its
 * preferred width, which would push the right side of a section out of view in a
 * narrow window or at a large display scale. Only below the sections' minimum
 * width does the column scroll sideways.</p>
 */
public class BackstageChoicePage extends JPanel {
	private static final long serialVersionUID = 1L;
	private static final int CHOICE_WIDTH = 220;
	/** The choice list may give up width in a narrow window before the detail column is squeezed. */
	private static final int CHOICE_MINIMUM_WIDTH = 110;
	/** Wrapping width of a section description; wide enough to read, narrow enough to fit a small window. */
	private static final int DESCRIPTION_WIDTH = 200;
	/** Below this width the choice list moves above the detail instead of squeezing it. */
	static final int STACKED_BELOW_WIDTH = 560;
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
	private final JPanel choiceList;
	private final JScrollPane detailScroll;

	protected BackstageChoicePage(String pageName, List<Section> choices) {
		super(new GridBagLayout());
		setName("officeBackstage" + pageName + "Page");
		setOpaque(false);
		Objects.requireNonNull(choices, "choices");
		if (choices.isEmpty()) throw new IllegalArgumentException("A Backstage choice page needs at least one choice");

		JPanel list = new JPanel();
		choiceList = list;
		list.setName("officeBackstage" + pageName + "Choices");
		list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
		list.setOpaque(false);
		list.setPreferredSize(new Dimension(CHOICE_WIDTH, 0));
		list.setMinimumSize(new Dimension(CHOICE_MINIMUM_WIDTH, 0));

		DetailStack stack = new DetailStack();
		stack.setName("officeBackstage" + pageName + "Sections");

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
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		detailScroll.setName("officeBackstage" + pageName + "Detail");
		detailScroll.setBorder(BorderFactory.createEmptyBorder());
		detailScroll.setOpaque(false);
		detailScroll.getViewport().setOpaque(false);

		add(choiceList);
		add(detailScroll);
		arrangeColumns(false);

		select(choices.getFirst().id());
	}

	@Override
	public void doLayout() {
		// A narrow window cannot hold the choice list and a readable detail column side by side;
		// the list then moves above the detail instead of being cut off or squeezing it.
		arrangeColumns(getWidth() > 0 && getWidth() < STACKED_BELOW_WIDTH);
		super.doLayout();
	}

	private void arrangeColumns(boolean stacked) {
		GridBagLayout layout = (GridBagLayout) getLayout();
		GridBagConstraints left = new GridBagConstraints();
		left.gridx = 0;
		left.gridy = 0;
		left.weightx = stacked ? 1 : 0;
		left.weighty = stacked ? 0 : 1;
		left.anchor = GridBagConstraints.NORTHWEST;
		left.fill = stacked ? GridBagConstraints.HORIZONTAL : GridBagConstraints.BOTH;
		left.insets = stacked ? new Insets(0, 0, 16, 0) : new Insets(0, 0, 0, 24);
		layout.setConstraints(choiceList, left);
		GridBagConstraints right = new GridBagConstraints();
		right.gridx = stacked ? 0 : 1;
		right.gridy = stacked ? 1 : 0;
		right.weightx = 1;
		right.weighty = 1;
		right.anchor = GridBagConstraints.NORTHWEST;
		right.fill = GridBagConstraints.BOTH;
		layout.setConstraints(detailScroll, right);
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

	/**
	 * A section description that wraps inside the column instead of widening it. A plain label reports
	 * its whole text as its minimum width, which is what pushed the right side of a section out of view.
	 */
	static JLabel description(String text, String name) {
		JLabel description = new JLabel("<html><div style='width:" + DESCRIPTION_WIDTH + "px'>"
				+ com.microproject.util.MarkupEscape.escape(text) + "</div></html>");
		description.setName(name);
		description.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
		return description;
	}

	/**
	 * The scrolling content of the detail column. It tracks the viewport width so the sections are laid
	 * out at the width the user can see, and only keeps its own width below the sections' minimum.
	 */
	private static final class DetailStack extends JPanel implements javax.swing.Scrollable {
		private static final long serialVersionUID = 1L;

		DetailStack() {
			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
			setOpaque(false);
		}

		@Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }

		@Override public int getScrollableUnitIncrement(java.awt.Rectangle visibleRect, int orientation, int direction) {
			return 16;
		}

		@Override public int getScrollableBlockIncrement(java.awt.Rectangle visibleRect, int orientation, int direction) {
			return Math.max(16, orientation == SwingConstants.VERTICAL ? visibleRect.height : visibleRect.width);
		}

		@Override public boolean getScrollableTracksViewportWidth() {
			return getParent() instanceof javax.swing.JViewport viewport && viewport.getWidth() >= getMinimumSize().width;
		}

		@Override public boolean getScrollableTracksViewportHeight() { return false; }
	}

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
