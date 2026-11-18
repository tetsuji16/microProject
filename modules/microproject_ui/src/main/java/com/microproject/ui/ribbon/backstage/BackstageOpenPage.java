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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;

import com.microproject.application.RecentProjectStore;
import com.microproject.util.MarkupEscape;

/**
 * Office-style Backstage "Open" page: location actions on the left, recent
 * projects on the right. This class is a pure view: all data and all actions are
 * supplied by the caller so it can be built and verified without a window.
 */
public final class BackstageOpenPage extends JPanel {
	private static final long serialVersionUID = 1L;
	private static final int LOCATION_WIDTH = 200;
	private static final int FOLDER_MAX_CHARS = 58;
	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");

	private final JList<RecentProjectStore.Entry> recentList;
	private final JLabel emptyMessage;

	/**
	 * @param entries the recent projects to show (already filtered by the store)
	 * @param recentItemsButton the existing "Recent Items" command control
	 * @param browseButton the existing "Browse" (Open) command control
	 * @param openAction invoked with the chosen entry when a row is clicked or Enter is pressed
	 * @param label resolves a {@code FileBackstage.open.*} key to localized text
	 */
	public BackstageOpenPage(List<RecentProjectStore.Entry> entries,
			AbstractButton recentItemsButton, AbstractButton browseButton,
			Consumer<RecentProjectStore.Entry> openAction, Function<String, String> label) {
		super(new GridBagLayout());
		setName("officeBackstageOpenPage");
		setOpaque(false);
		Objects.requireNonNull(entries, "entries");
		Objects.requireNonNull(openAction, "openAction");
		Objects.requireNonNull(label, "label");

		JPanel locations = createLocations(recentItemsButton, browseButton, label);
		JPanel recent = createRecentPanel(entries, openAction, label);
		this.recentList = findRecentList(recent);
		this.emptyMessage = findEmptyMessage(recent);

		GridBagConstraints left = new GridBagConstraints();
		left.gridx = 0;
		left.gridy = 0;
		left.weightx = 0;
		left.weighty = 1;
		left.anchor = GridBagConstraints.NORTHWEST;
		left.fill = GridBagConstraints.BOTH;
		left.insets = new Insets(0, 0, 0, 24);
		add(locations, left);

		GridBagConstraints right = new GridBagConstraints();
		right.gridx = 1;
		right.gridy = 0;
		right.weightx = 1;
		right.weighty = 1;
		right.anchor = GridBagConstraints.NORTHWEST;
		right.fill = GridBagConstraints.BOTH;
		add(recent, right);
	}

	/** The recent-project list, or {@code null} when there are no entries. */
	public JList<RecentProjectStore.Entry> getRecentList() {
		return recentList;
	}

	/** The "no recent files" label, or {@code null} when the list has entries. */
	public JLabel getEmptyMessage() {
		return emptyMessage;
	}

	private static JPanel createLocations(AbstractButton recentItemsButton, AbstractButton browseButton,
			Function<String, String> label) {
		JPanel locations = new JPanel();
		locations.setName("officeBackstageOpenLocations");
		locations.setLayout(new BoxLayout(locations, BoxLayout.Y_AXIS));
		locations.setOpaque(false);
		locations.setPreferredSize(new Dimension(LOCATION_WIDTH, 0));
		JLabel heading = new JLabel(label.apply("location"));
		heading.setName("officeBackstageOpenLocationHeading");
		heading.setAlignmentX(Component.LEFT_ALIGNMENT);
		heading.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
		locations.add(heading);
		for (AbstractButton button : List.of(recentItemsButton, browseButton)) {
			if (button == null) continue;
			button.setAlignmentX(Component.LEFT_ALIGNMENT);
			button.setHorizontalAlignment(SwingConstants.LEFT);
			Dimension preferred = button.getPreferredSize();
			button.setMaximumSize(new Dimension(LOCATION_WIDTH, preferred.height));
			locations.add(button);
			locations.add(Box.createVerticalStrut(8));
		}
		locations.add(Box.createVerticalGlue());
		return locations;
	}

	private static JPanel createRecentPanel(List<RecentProjectStore.Entry> entries,
			Consumer<RecentProjectStore.Entry> openAction, Function<String, String> label) {
		JPanel recent = new JPanel(new BorderLayout(0, 8));
		recent.setName("officeBackstageOpenRecent");
		recent.setOpaque(false);
		JLabel heading = new JLabel(label.apply("recentItems"));
		heading.setName("officeBackstageOpenRecentHeading");
		heading.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
		recent.add(heading, BorderLayout.NORTH);
		if (entries.isEmpty()) {
			JLabel empty = new JLabel(label.apply("empty"));
			empty.setName("officeBackstageOpenEmpty");
			empty.setBorder(BorderFactory.createEmptyBorder(24, 0, 0, 0));
			recent.add(empty, BorderLayout.CENTER);
		} else {
			JList<RecentProjectStore.Entry> list = new JList<>(entries.toArray(RecentProjectStore.Entry[]::new));
			list.setName("officeBackstageOpenRecentList");
			list.setCellRenderer(new RecentEntryRenderer(label));
			list.setVisibleRowCount(12);
			list.setFixedCellHeight(52);
			list.setOpaque(false);
			list.addMouseListener(new MouseAdapter() {
				@Override public void mouseClicked(MouseEvent event) {
					if (event.getButton() == MouseEvent.BUTTON1) openSelected(list, openAction);
				}
			});
			list.getInputMap(JComponent.WHEN_FOCUSED).put(
					KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ENTER, 0), "backstageOpenRecent");
			list.getActionMap().put("backstageOpenRecent", new AbstractAction() {
				private static final long serialVersionUID = 1L;
				@Override public void actionPerformed(ActionEvent event) { openSelected(list, openAction); }
			});
			JScrollPane scroll = new JScrollPane(list, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
					JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
			scroll.setBorder(BorderFactory.createEmptyBorder());
			scroll.setOpaque(false);
			scroll.getViewport().setOpaque(false);
			recent.add(scroll, BorderLayout.CENTER);
		}
		return recent;
	}

	private static void openSelected(JList<RecentProjectStore.Entry> list,
			Consumer<RecentProjectStore.Entry> openAction) {
		RecentProjectStore.Entry entry = list.getSelectedValue();
		if (entry != null) openAction.accept(entry);
	}

	private static JList<RecentProjectStore.Entry> findRecentList(JPanel recent) {
		JList<RecentProjectStore.Entry> found = null;
		for (Component component : recent.getComponents()) {
			if (component instanceof JScrollPane scroll && scroll.getViewport().getView() instanceof JList<?> list) {
				@SuppressWarnings("unchecked")
				JList<RecentProjectStore.Entry> typed = (JList<RecentProjectStore.Entry>) list;
				found = typed;
			}
		}
		return found;
	}

	private static JLabel findEmptyMessage(JPanel recent) {
		for (Component component : recent.getComponents())
			if (component instanceof JLabel label && "officeBackstageOpenEmpty".equals(label.getName()))
				return label;
		return null;
	}

	/** Two-line (name + folder + date) renderer; the full path stays in the tooltip. */
	static final class RecentEntryRenderer extends DefaultListCellRenderer {
		private static final long serialVersionUID = 1L;
		private final Function<String, String> label;

		RecentEntryRenderer(Function<String, String> label) { this.label = label; }

		@Override public Component getListCellRendererComponent(JList<?> list, Object value, int index,
				boolean selected, boolean focus) {
			RecentProjectStore.Entry entry = (RecentProjectStore.Entry) value;
			super.getListCellRendererComponent(list, "", index, selected, focus);
			String name = MarkupEscape.escape(entry.path().getFileName().toString());
			String fullFolder = entry.path().getParent() == null ? "" : entry.path().getParent().toString();
			String folder = MarkupEscape.escape(truncateMiddle(fullFolder.replace('\\', '/'), FOLDER_MAX_CHARS));
			String date = DATE.format(Instant.ofEpochMilli(entry.lastOpened()).atZone(ZoneId.systemDefault()));
			String modified = MarkupEscape.escape(label.apply("lastModified").replace("{0}", date));
			setText("<html><div style='line-height:1.2'><b>" + name + "</b></div>"
					+ "<div style='color:#6e6e6e;font-size:0.85em'>" + folder + "</div>"
					+ "<div style='color:#6e6e6e;font-size:0.85em'>" + modified + "</div></html>");
			setToolTipText(entry.path().toString().replace('\\', '/'));
			return this;
		}
	}

	/** Keeps a long folder readable on one line; the tooltip carries the whole path. */
	static String truncateMiddle(String text, int maxChars) {
		if (text == null || text.length() <= maxChars) return text;
		int head = (maxChars - 1) / 2;
		int tail = maxChars - 1 - head;
		return text.substring(0, head) + "…" + text.substring(text.length() - tail);
	}
}
