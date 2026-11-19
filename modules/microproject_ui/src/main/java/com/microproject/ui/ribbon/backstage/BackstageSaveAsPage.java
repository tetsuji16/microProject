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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * Office-style Backstage "Save As" page.  The caller supplies the current
 * folder, the distinct recent folders, the existing Save As commands and the
 * action that opens the existing dialog.  The view never writes a file.
 */
public final class BackstageSaveAsPage extends BackstageChoicePage {
	private static final long serialVersionUID = 1L;
	private static final int DETAIL_WIDTH = 460;

	/**
	 * @param currentFolder folder of the open file, or {@code null} for an unsaved project
	 * @param recentFolders distinct parent folders from the recent-project store
	 * @param mpoButton existing "save as MPO" command control
	 * @param otherButton existing "save as other format" command control
	 * @param openAction called with the chosen folder (or {@code null} for Browse)
	 * @param label resolves a {@code FileBackstage.saveAs.*} key to localized text
	 */
	public BackstageSaveAsPage(String currentFolder, List<Path> recentFolders,
			AbstractButton mpoButton, AbstractButton otherButton,
			Consumer<Path> openAction, Function<String, String> label) {
		super("SaveAs", List.of(
				new Section("thisPc", label.apply("FileBackstage.saveAs.thisPc"),
						thisPc(currentFolder, recentFolders, openAction, label), () -> { }),
				new Section("browse", label.apply("FileBackstage.saveAs.browse"),
						browse(mpoButton, otherButton, label), () -> openAction.accept(null))));
		Objects.requireNonNull(openAction, "openAction");
		Objects.requireNonNull(label, "label");
	}

	private static JComponent thisPc(String currentFolder, List<Path> recentFolders,
			Consumer<Path> openAction, Function<String, String> label) {
		JPanel panel = panel("officeBackstageSaveAsThisPc");
		int row = 0;
		Path current = toPath(currentFolder);
		if (current != null) {
			row = addHeading(panel, label.apply("FileBackstage.saveAs.currentFolder"), row);
			JButton currentButton = folderButton(current, "officeBackstageSaveAsCurrentFolder", openAction);
			add(panel, currentButton, row++);
		}
		row = addHeading(panel, label.apply("FileBackstage.saveAs.recentFolders"), row);
		List<Path> folders = recentFolders == null ? List.of() : recentFolders.stream().distinct().toList();
		if (folders.isEmpty()) {
			JLabel empty = new JLabel(label.apply("FileBackstage.saveAs.empty"));
			empty.setName("officeBackstageSaveAsEmpty");
			empty.setBorder(BorderFactory.createEmptyBorder(4, 2, 4, 2));
			add(panel, empty, row++);
		} else {
			for (int index = 0; index < folders.size(); index++) {
				Path folder = folders.get(index);
				add(panel, folderButton(folder, "officeBackstageSaveAsFolder-" + index, openAction), row++);
			}
		}
		return panel;
	}

	private static JComponent browse(AbstractButton mpoButton, AbstractButton otherButton,
			Function<String, String> label) {
		JPanel panel = panel("officeBackstageSaveAsBrowse");
		JLabel description = new JLabel(label.apply("FileBackstage.saveAs.description"));
		description.setName("officeBackstageSaveAsDescription");
		description.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
		add(panel, description, 0);
		int row = 1;
		if (mpoButton != null) {
			mpoButton.setText(label.apply("FileBackstage.saveAs.mpo"));
			add(panel, mpoButton, row++);
		}
		if (otherButton != null) {
			otherButton.setText(label.apply("FileBackstage.saveAs.otherFormat"));
			add(panel, otherButton, row++);
		}
		return panel;
	}

	private static JPanel panel(String name) {
		JPanel panel = new JPanel(new GridBagLayout()) {
			private static final long serialVersionUID = 1L;
			@Override public Dimension getPreferredSize() {
				Dimension preferred = super.getPreferredSize();
				return new Dimension(Math.max(DETAIL_WIDTH, preferred.width), preferred.height);
			}
			@Override public Dimension getMaximumSize() { return getPreferredSize(); }
		};
		panel.setName(name);
		panel.setOpaque(false);
		panel.setAlignmentX(Component.LEFT_ALIGNMENT);
		return panel;
	}

	private static int addHeading(JPanel panel, String text, int row) {
		JLabel heading = new JLabel(text);
		heading.setName("officeBackstageSaveAsHeading");
		heading.setFont(heading.getFont().deriveFont(java.awt.Font.BOLD));
		heading.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
		add(panel, heading, row);
		return row + 1;
	}

	private static Path toPath(String value) {
		if (value == null || value.isBlank()) return null;
		try { return Path.of(value); } catch (RuntimeException invalidPath) { return null; }
	}

	private static JButton folderButton(Path folder, String name, Consumer<Path> openAction) {
		String path = folder.toString();
		JButton button = new JButton(path.replace('\\', '/'));
		button.setName(name);
		button.setHorizontalAlignment(SwingConstants.LEFT);
		button.setAlignmentX(Component.LEFT_ALIGNMENT);
		button.setContentAreaFilled(false);
		button.setBorderPainted(false);
		button.setToolTipText(path);
		button.addActionListener(event -> openAction.accept(folder));
		return button;
	}

	private static void add(JPanel panel, JComponent component, int row) {
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridx = 0;
		constraints.gridy = row;
		constraints.weightx = 1;
		constraints.anchor = GridBagConstraints.NORTHWEST;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.insets = new Insets(0, 2, 5, 0);
		panel.add(component, constraints);
	}
}
