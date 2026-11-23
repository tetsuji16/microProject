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
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * Office-style Backstage "Export" page.  The left list separates the PDF/XPS
 * document action from the other file types; both sections remain attached so
 * every export command stays physically discoverable.  Only commands supplied
 * by the caller are shown, so no unsupported format or POD export is added.
 */
public final class BackstageExportPage extends BackstageChoicePage {
	private static final long serialVersionUID = 1L;
	private static final int DETAIL_WIDTH = 460;

	/**
	 * @param pdfButton existing PDF export command control
	 * @param exportButton existing "export to another file type" command control
	 * @param importButton existing "import from another file type" command control
	 * @param label resolves a {@code FileBackstage.export.*} key to localized text
	 */
	public BackstageExportPage(AbstractButton pdfButton, AbstractButton exportButton,
			AbstractButton importButton, Function<String, String> label) {
		super("Export", List.of(
				new Section("pdf", label.apply("FileBackstage.export.pdfDocument"),
						pdfSection(pdfButton, label), () -> { }),
				new Section("changeType", label.apply("FileBackstage.export.changeType"),
						changeTypeSection(exportButton, importButton, label), () -> { })));
		Objects.requireNonNull(label, "label");
	}

	private static JComponent pdfSection(AbstractButton pdfButton, Function<String, String> label) {
		JPanel panel = panel("officeBackstageExportPdf");
		JComponent description = BackstageChoicePage.description(
				label.apply("FileBackstage.export.pdfDescription"), "officeBackstageExportPdfDescription");
		add(panel, description, 0);
		JLabel caption = new JLabel(label.apply("FileBackstage.export.description.RibbonPDF"));
		caption.setName("officeBackstageExportPdfCaption");
		add(panel, caption, 1);
		if (pdfButton != null) {
			pdfButton.setText(label.apply("FileBackstage.export.pdfAction"));
			pdfButton.setHorizontalAlignment(SwingConstants.LEFT);
			add(panel, pdfButton, 2);
		}
		return panel;
	}

	private static JComponent changeTypeSection(AbstractButton exportButton, AbstractButton importButton,
			Function<String, String> label) {
		JPanel panel = panel("officeBackstageExportChangeType");
		JComponent description = BackstageChoicePage.description(
				label.apply("FileBackstage.export.changeTypeDescription"), "officeBackstageExportChangeTypeDescription");
		add(panel, description, 0);
		int row = 1;
		for (AbstractButton command : new AbstractButton[] { exportButton, importButton }) {
			if (command == null) continue;
			String id = command.getActionCommand();
			JLabel caption = new JLabel(label.apply("FileBackstage.export.description." + id));
			caption.setName("officeBackstageExportCaption-" + id);
			add(panel, caption, row++);
			command.setHorizontalAlignment(SwingConstants.LEFT);
			add(panel, command, row++);
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
		};
		panel.setName(name);
		panel.setOpaque(false);
		panel.setAlignmentX(Component.LEFT_ALIGNMENT);
		return panel;
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
