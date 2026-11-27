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
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;

import com.microproject.util.MarkupEscape;

/**
 * Office-style Backstage "Print" page: print settings on the left and a preview
 * of the current view on the right.
 *
 * <p>This class is a pure view. Every value and every action is supplied by the
 * caller, so the page can be built and verified without a window, a print job,
 * or a live document. Opening the page never dispatches a command or mutates
 * project data; the supplied preview component is shown even when no printer is
 * available.</p>
 */
public final class BackstagePrintPage extends JPanel {
	private static final long serialVersionUID = 1L;
	private static final int COLUMN_WIDTH = 300;
	private static final int FIELD_WIDTH = 170;
	/** Narrowest preview column kept beside the settings; below it the preview moves under them. */
	static final int MINIMUM_SIDE_PREVIEW_WIDTH = 320;
	private static final int COLUMN_GAP = 24;

	/** Immutable snapshot of the print settings shown in the left column. */
	public record Settings(int copies, List<String> printers, String printer,
			List<String> ranges, String range,
			List<String> orientations, String orientation,
			List<String> paperSizes, String paperSize) {
		public Settings {
			copies = Math.max(1, copies);
			printers = printers == null ? List.of() : List.copyOf(printers);
			ranges = ranges == null ? List.of() : List.copyOf(ranges);
			orientations = orientations == null ? List.of() : List.copyOf(orientations);
			paperSizes = paperSizes == null ? List.of() : List.copyOf(paperSizes);
		}
	}

	/** Actions the view delegates to; the view never implements print logic itself. */
	public interface Actions {
		/** Opens the existing Page Setup dialog. */
		void openPageSetup();
		/** Called when a setting changes so the caller can refresh the preview. */
		void settingsChanged();
	}

	private final AbstractButton printButton;
	private final JSpinner copiesField;
	private final JComboBox<String> printerCombo;
	private final JComboBox<String> rangeCombo;
	private final JComboBox<String> orientationCombo;
	private final JComboBox<String> paperSizeCombo;
	private final JButton pageSetupLink;
	private final JLabel statusLabel;
	private final String noPrintersText;
	private final String loadingText;
	private JComponent preview;

	/**
	 * @param settings the print-settings snapshot, never {@code null}
	 * @param printButton the caller-built button wired to the existing print command
	 * @param preview the caller-built preview of the current view, never {@code null}
	 * @param actions the actions invoked by the controls, never {@code null}
	 * @param label resolves a {@code FileBackstage.print.*} key to localized text
	 */
	public BackstagePrintPage(Settings settings, AbstractButton printButton, JComponent preview,
			Actions actions, Function<String, String> label) {
		super(new BorderLayout(0, 14));
		setName("officeBackstagePrintPage");
		setOpaque(false);
		Objects.requireNonNull(settings, "settings");
		Objects.requireNonNull(actions, "actions");
		Objects.requireNonNull(label, "label");
		this.preview = Objects.requireNonNull(preview, "preview");

		boolean hasPrinter = !settings.printers().isEmpty();
		this.noPrintersText = label.apply("noPrinters");
		this.loadingText = label.apply("loadingPrinters");

		JPanel left = fixedWidthPanel("officeBackstagePrintSettings");
		left.setLayout(new GridBagLayout());
		JPanel right = createPreviewPane();
		JPanel columns = new JPanel(new GridBagLayout()) {
			private static final long serialVersionUID = 1L;
			@Override public void doLayout() {
				// A narrow window cannot hold the fixed settings column and a usable preview side by
				// side; the preview then moves under the settings instead of being pushed out of view.
				arrangeColumns(this, left, right, getWidth() < COLUMN_WIDTH + COLUMN_GAP + MINIMUM_SIDE_PREVIEW_WIDTH);
				super.doLayout();
			}
		};
		columns.setName("officeBackstagePrintColumns");
		columns.setOpaque(false);
		columns.add(left, new GridBagConstraints());
		columns.add(right, new GridBagConstraints());
		arrangeColumns(columns, left, right, false);
		add(columns, BorderLayout.CENTER);

		this.printButton = Objects.requireNonNull(printButton, "printButton");
		this.printButton.setEnabled(hasPrinter);
		insertTopControl(left, this.printButton);

		this.copiesField = new JSpinner(new SpinnerNumberModel(settings.copies(), 1, 999, 1));
		this.copiesField.setName("officeBackstagePrintCopies");
		this.rangeCombo = combo("officeBackstagePrintRange", settings.ranges(), settings.range());
		this.orientationCombo = combo("officeBackstagePrintOrientation", settings.orientations(), settings.orientation());
		this.paperSizeCombo = combo("officeBackstagePrintPaperSize", settings.paperSizes(), settings.paperSize());
		this.printerCombo = combo("officeBackstagePrintPrinter", settings.printers(), settings.printer());
		this.statusLabel = new JLabel(hasPrinter ? "" : this.noPrintersText);
		this.statusLabel.setName("officeBackstagePrintStatus");
		this.pageSetupLink = link(label.apply("pageSetup"), "officeBackstagePrintPageSetup", actions::openPageSetup);

		int row = 1;
		addRow(left, label.apply("copies"), copiesField, row++);
		addRow(left, label.apply("printer"), printerCombo, row++);
		addRow(left, label.apply("range"), rangeCombo, row++);
		addRow(left, label.apply("orientation"), orientationCombo, row++);
		addRow(left, label.apply("paperSize"), paperSizeCombo, row++);
		GridBagConstraints statusConstraints = rowConstraints(row++, 0, 2);
		left.add(statusLabel, statusConstraints);
		GridBagConstraints linkConstraints = rowConstraints(row, 0, 2);
		left.add(pageSetupLink, linkConstraints);

		if (!hasPrinter) printerCombo.setEnabled(false);
		Runnable refresh = () -> {
			actions.settingsChanged();
			// The field holds the preview that is installed now, which may replace the caller's component.
			this.preview.revalidate();
			this.preview.repaint();
		};
		copiesField.addChangeListener(event -> refresh.run());
		printerCombo.addActionListener(event -> refresh.run());
		rangeCombo.addActionListener(event -> refresh.run());
		orientationCombo.addActionListener(event -> refresh.run());
		paperSizeCombo.addActionListener(event -> refresh.run());
	}

	private static void arrangeColumns(JPanel columns, JComponent left, JComponent right, boolean stacked) {
		GridBagLayout layout = (GridBagLayout) columns.getLayout();
		GridBagConstraints leftConstraints = new GridBagConstraints();
		leftConstraints.gridx = 0;
		leftConstraints.gridy = 0;
		leftConstraints.weightx = stacked ? 1 : 0;
		leftConstraints.anchor = GridBagConstraints.NORTHWEST;
		leftConstraints.insets = stacked ? new Insets(0, 0, 12, 0) : new Insets(0, 0, 0, COLUMN_GAP);
		layout.setConstraints(left, leftConstraints);
		GridBagConstraints rightConstraints = new GridBagConstraints();
		rightConstraints.gridx = stacked ? 0 : 1;
		rightConstraints.gridy = stacked ? 1 : 0;
		rightConstraints.weightx = 1;
		rightConstraints.weighty = 1;
		rightConstraints.fill = GridBagConstraints.BOTH;
		rightConstraints.anchor = GridBagConstraints.NORTHWEST;
		layout.setConstraints(right, rightConstraints);
	}

	private JPanel createPreviewPane() {
		JPanel panel = new JPanel(new BorderLayout());
		panel.setName("officeBackstagePrintPreviewPane");
		panel.setBorder(BorderFactory.createLineBorder(new java.awt.Color(200, 200, 200)));
		panel.add(preview, BorderLayout.CENTER);
		return panel;
	}

	private void insertTopControl(JPanel panel, JComponent control) {
		GridBagConstraints constraints = rowConstraints(0, 0, 2);
		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.ipady = 8;
		panel.add(control, constraints);
	}

	private void addRow(JPanel panel, String caption, JComponent field, int row) {
		JLabel name = new JLabel(caption + ":");
		name.setName("officeBackstagePrintFieldLabel");
		name.setFont(name.getFont().deriveFont(Font.BOLD));
		GridBagConstraints nameConstraints = rowConstraints(row, 0, 1);
		nameConstraints.anchor = GridBagConstraints.WEST;
		panel.add(name, nameConstraints);
		GridBagConstraints fieldConstraints = rowConstraints(row, 1, 1);
		fieldConstraints.fill = GridBagConstraints.HORIZONTAL;
		fieldConstraints.weightx = 1;
		panel.add(field, fieldConstraints);
	}

	private static GridBagConstraints rowConstraints(int row, int column, int width) {
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridx = column;
		constraints.gridy = row;
		constraints.gridwidth = width;
		constraints.anchor = GridBagConstraints.WEST;
		constraints.insets = new Insets(0, 0, 8, 0);
		return constraints;
	}

	private static JPanel fixedWidthPanel(String name) {
		JPanel panel = new JPanel() {
			private static final long serialVersionUID = 1L;
			@Override public Dimension getPreferredSize() {
				Dimension preferred = super.getPreferredSize();
				return new Dimension(COLUMN_WIDTH, preferred.height);
			}
		};
		panel.setName(name);
		panel.setOpaque(false);
		return panel;
	}

	private static JComboBox<String> combo(String name, List<String> values, String selected) {
		ComboBoxModel<String> model = new DefaultComboBoxModel<>(values.toArray(String[]::new));
		if (selected != null && values.contains(selected)) model.setSelectedItem(selected);
		JComboBox<String> combo = new JComboBox<>(model);
		combo.setName(name);
		combo.setPreferredSize(new Dimension(FIELD_WIDTH, combo.getPreferredSize().height));
		return combo;
	}

	private static JButton link(String text, String name, Runnable action) {
		JButton button = new JButton("<html><u>" + MarkupEscape.escape(text) + "</u></html>");
		button.setName(name);
		button.setContentAreaFilled(false);
		button.setBorderPainted(false);
		button.setFocusPainted(false);
		button.setHorizontalAlignment(SwingConstants.LEFT);
		button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		button.addActionListener(event -> action.run());
		return button;
	}

	/**
	 * Shows that the printer list is still being read. The printer controls stay disabled until
	 * {@link #setPrinters} delivers a list. The Page Setup link also stays disabled: it needs the
	 * print document, which is created from that list, and the caller enables it once it exists.
	 */
	public void showPrintersLoading() {
		printerCombo.setModel(new DefaultComboBoxModel<>());
		printerCombo.setEnabled(false);
		printButton.setEnabled(false);
		pageSetupLink.setEnabled(false);
		statusLabel.setText(loadingText);
	}

	/** Fills in the printer list once it is known and enables printing when one exists. */
	public void setPrinters(List<String> printers, String selected) {
		List<String> values = printers == null ? List.of() : List.copyOf(printers);
		printerCombo.setModel(new DefaultComboBoxModel<>(values.toArray(String[]::new)));
		if (selected != null && values.contains(selected)) printerCombo.setSelectedItem(selected);
		boolean hasPrinter = !values.isEmpty();
		printerCombo.setEnabled(hasPrinter);
		printButton.setEnabled(hasPrinter);
		statusLabel.setText(hasPrinter ? "" : noPrintersText);
	}

	/** Replaces the preview, e.g. once the print document has been created. */
	public void setPreviewComponent(JComponent component) {
		Objects.requireNonNull(component, "component");
		if (component == preview) return;
		Container pane = preview.getParent();
		if (pane == null) return;
		pane.remove(preview);
		pane.add(component, BorderLayout.CENTER);
		preview = component;
		pane.revalidate();
		pane.repaint();
	}

	/** Selects the orientation that matches the document's page format. */
	public void setOrientation(String orientation) {
		if (orientation == null) return;
		ComboBoxModel<String> model = orientationCombo.getModel();
		for (int i = 0; i < model.getSize(); i++) {
			if (orientation.equals(model.getElementAt(i))) {
				orientationCombo.setSelectedItem(orientation);
				return;
			}
		}
	}

	/** The caller-supplied print button, wired to the existing print command. */
	public AbstractButton getPrintButton() { return printButton; }

	/** The copies field. */
	public JSpinner getCopiesField() { return copiesField; }

	/** The printer selector; disabled with a "no printers" status when none exist. */
	public JComboBox<String> getPrinterCombo() { return printerCombo; }

	/** The print-range selector. */
	public JComboBox<String> getRangeCombo() { return rangeCombo; }

	/** The orientation selector. */
	public JComboBox<String> getOrientationCombo() { return orientationCombo; }

	/** The paper-size selector. */
	public JComboBox<String> getPaperSizeCombo() { return paperSizeCombo; }

	/** The Page Setup link. */
	public JButton getPageSetupLink() { return pageSetupLink; }

	/** Printer status label; carries the "no printers" message when none exist. */
	public JLabel getStatusLabel() { return statusLabel; }

	/** The caller-supplied preview component shown in the right column. */
	public JComponent getPreviewComponent() { return preview; }

	/** All visible label and button texts below a root, used by tests and diagnostics. */
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
