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
import java.awt.Font;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import com.microproject.application.RecentProjectStore;

/**
 * Office-style Backstage "Home" page: a single New project action on top and the
 * recent-projects list below. The list is the same component the Open page uses,
 * so both pages share its renderer, activation and empty state.
 *
 * <p>This class is a pure view. The caller supplies the existing command button,
 * the recent entries, the open action and the label resolver; nothing is fetched
 * or dispatched here, so the page can be built and verified without a window.</p>
 */
public final class BackstageHomePage extends JPanel {
	private static final long serialVersionUID = 1L;
	static final String NAME_PREFIX = "officeBackstageHome";

	private final JList<RecentProjectStore.Entry> recentList;
	private final JLabel emptyMessage;

	/**
	 * @param newProjectButton the existing New project command control supplied by the caller
	 * @param entries the recent projects to show (already filtered by the store)
	 * @param openAction invoked with the chosen entry when a row is clicked or Enter is pressed
	 * @param label resolves a full resource key such as {@code FileBackstage.home.recent} to text
	 */
	public BackstageHomePage(AbstractButton newProjectButton, List<RecentProjectStore.Entry> entries,
			Consumer<RecentProjectStore.Entry> openAction, Function<String, String> label) {
		super(new BorderLayout(0, 22));
		setName(NAME_PREFIX + "Page");
		setOpaque(false);
		Objects.requireNonNull(entries, "entries");
		Objects.requireNonNull(openAction, "openAction");
		Objects.requireNonNull(label, "label");

		JPanel top = new JPanel();
		top.setName(NAME_PREFIX + "NewProjectSection");
		top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
		top.setOpaque(false);
		JLabel heading = new JLabel(label.apply("FileBackstage.home.newProject"));
		heading.setName(NAME_PREFIX + "NewProjectHeading");
		heading.setFont(heading.getFont().deriveFont(Font.BOLD, 18f));
		heading.setAlignmentX(Component.LEFT_ALIGNMENT);
		top.add(heading);
		JLabel description = new JLabel(label.apply("FileBackstage.home.newProjectDescription"));
		description.setName(NAME_PREFIX + "NewProjectDescription");
		description.setAlignmentX(Component.LEFT_ALIGNMENT);
		description.setBorder(BorderFactory.createEmptyBorder(2, 0, 10, 0));
		top.add(description);
		if (newProjectButton != null) {
			newProjectButton.setAlignmentX(Component.LEFT_ALIGNMENT);
			newProjectButton.setHorizontalAlignment(SwingConstants.LEFT);
			top.add(newProjectButton);
		}
		add(top, BorderLayout.NORTH);

		Function<String, String> openLabel = key -> label.apply("FileBackstage.open." + key);
		JPanel recent = BackstageOpenPage.recentProjectsPanel(entries, openAction, openLabel, NAME_PREFIX,
				label.apply("FileBackstage.home.recent"));
		this.recentList = BackstageOpenPage.findRecentList(recent);
		this.emptyMessage = findEmpty(recent);
		add(recent, BorderLayout.CENTER);
	}

	/** The recent-project list, or {@code null} when there are no entries. */
	public JList<RecentProjectStore.Entry> getRecentList() {
		return recentList;
	}

	/** The "no recent files" label, or {@code null} when the list has entries. */
	public JLabel getEmptyMessage() {
		return emptyMessage;
	}

	private static JLabel findEmpty(JPanel recent) {
		for (Component component : recent.getComponents())
			if (component instanceof JLabel label && (NAME_PREFIX + "Empty").equals(label.getName()))
				return label;
		return null;
	}
}
