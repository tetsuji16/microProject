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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import com.microproject.util.MarkupEscape;

/**
 * Office-style Backstage "Account" page, built for a sign-in-free desktop
 * product: product information, the existing update/About actions and theme
 * selection only. No account, user, connected-service or Microsoft 365 surface
 * is created here.
 *
 * <p>This class is a pure view. Every value and every action is supplied by the
 * caller, so it can be built and verified without a window or a live service.</p>
 */
public final class BackstageAccountPage extends JPanel {
	private static final long serialVersionUID = 1L;
	static final String NAME_PREFIX = "officeBackstageAccount";
	private static final int VALUE_WIDTH = 280;

	/** Read-only product facts shown on the page. */
	public record ProductInfo(String name, String version, String license) { }

	private final JLabel productName;
	private final JLabel productVersion;
	private final JLabel productLicense;
	private final JComboBox<String> theme;

	/**
	 * @param product the product facts to display
	 * @param darkTheme whether the application is currently in the dark theme
	 * @param checkForUpdates invoked by the "Check for updates" button
	 * @param about invoked by the "About" button
	 * @param themeAction receives {@code true} for dark and {@code false} for light
	 * @param label resolves a full resource key such as {@code FileBackstage.account.theme} to text
	 */
	public BackstageAccountPage(ProductInfo product, boolean darkTheme, Runnable checkForUpdates,
			Runnable about, Consumer<Boolean> themeAction, Function<String, String> label) {
		super(new BorderLayout(0, 18));
		setName(NAME_PREFIX + "Page");
		setOpaque(false);
		Objects.requireNonNull(product, "product");
		Objects.requireNonNull(label, "label");

		JPanel identity = new JPanel();
		identity.setName(NAME_PREFIX + "Identity");
		identity.setLayout(new BoxLayout(identity, BoxLayout.Y_AXIS));
		identity.setOpaque(false);
		JLabel heading = new JLabel(label.apply("FileBackstage.account.productInformation"));
		heading.setName(NAME_PREFIX + "ProductHeading");
		heading.setFont(heading.getFont().deriveFont(Font.BOLD, 18f));
		heading.setAlignmentX(Component.LEFT_ALIGNMENT);
		identity.add(heading);
		identity.add(Box.createVerticalStrut(10));

		JPanel rows = new JPanel(new GridBagLayout());
		rows.setName(NAME_PREFIX + "ProductRows");
		rows.setOpaque(false);
		GridBagConstraints nameConstraints = new GridBagConstraints();
		nameConstraints.gridx = 0;
		nameConstraints.gridy = 0;
		nameConstraints.anchor = GridBagConstraints.NORTHWEST;
		nameConstraints.insets = new Insets(0, 0, 6, 12);
		GridBagConstraints valueConstraints = new GridBagConstraints();
		valueConstraints.gridx = 1;
		valueConstraints.gridy = 0;
		valueConstraints.weightx = 1;
		valueConstraints.anchor = GridBagConstraints.NORTHWEST;
		valueConstraints.fill = GridBagConstraints.HORIZONTAL;
		valueConstraints.insets = new Insets(0, 0, 6, 0);
		this.productName = addRow(rows, nameConstraints, valueConstraints,
				label.apply("FileBackstage.account.name"), safe(product.name()), "ProductName");
		this.productVersion = addRow(rows, nameConstraints, valueConstraints,
				label.apply("FileBackstage.account.version"), safe(product.version()), "Version");
		this.productLicense = addRow(rows, nameConstraints, valueConstraints,
				label.apply("FileBackstage.account.license"), safe(product.license()), "License");
		identity.add(rows);
		identity.add(Box.createVerticalStrut(10));

		JButton updates = actionButton(label.apply("FileBackstage.account.checkForUpdates"),
				NAME_PREFIX + "CheckForUpdates", checkForUpdates);
		JButton aboutButton = actionButton(label.apply("FileBackstage.account.about"),
				NAME_PREFIX + "About", about);
		identity.add(updates);
		identity.add(Box.createVerticalStrut(6));
		identity.add(aboutButton);
		add(identity, BorderLayout.NORTH);

		JPanel appearance = new JPanel();
		appearance.setName(NAME_PREFIX + "Appearance");
		appearance.setLayout(new BoxLayout(appearance, BoxLayout.Y_AXIS));
		appearance.setOpaque(false);
		JLabel themeHeading = new JLabel(label.apply("FileBackstage.account.theme"));
		themeHeading.setName(NAME_PREFIX + "ThemeHeading");
		themeHeading.setFont(themeHeading.getFont().deriveFont(Font.BOLD, 18f));
		themeHeading.setAlignmentX(Component.LEFT_ALIGNMENT);
		appearance.add(themeHeading);
		appearance.add(Box.createVerticalStrut(10));
		this.theme = new JComboBox<>(new String[] {
				label.apply("FileBackstage.account.theme.light"),
				label.apply("FileBackstage.account.theme.dark") });
		this.theme.setName(NAME_PREFIX + "Theme");
		this.theme.setAlignmentX(Component.LEFT_ALIGNMENT);
		this.theme.setSelectedIndex(darkTheme ? 1 : 0);
		if (themeAction != null)
			this.theme.addActionListener(event -> themeAction.accept(theme.getSelectedIndex() == 1));
		appearance.add(this.theme);
		add(appearance, BorderLayout.CENTER);
	}

	private static JLabel addRow(JPanel rows, GridBagConstraints nameConstraints,
			GridBagConstraints valueConstraints, String name, String value, String suffix) {
		JLabel label = new JLabel(name + ":");
		label.setName(NAME_PREFIX + suffix + "Label");
		label.setFont(label.getFont().deriveFont(Font.BOLD));
		rows.add(label, nameConstraints);
		JLabel result = new JLabel(wrap(value));
		result.setName(NAME_PREFIX + suffix);
		rows.add(result, valueConstraints);
		nameConstraints.gridy++;
		valueConstraints.gridy++;
		return result;
	}

	private static JButton actionButton(String text, String name, Runnable action) {
		JButton button = new JButton(text);
		button.setName(name);
		button.setHorizontalAlignment(SwingConstants.LEFT);
		button.setAlignmentX(Component.LEFT_ALIGNMENT);
		if (action != null) button.addActionListener(event -> action.run());
		return button;
	}

	private static String safe(String value) {
		return value == null || value.isBlank() ? "" : value;
	}

	private static String wrap(String value) {
		if (value.isEmpty()) return "";
		return "<html><div style='width:" + VALUE_WIDTH + "px'>" + MarkupEscape.escape(value) + "</div></html>";
	}

	/** Product name label. */
	public JLabel getProductName() { return productName; }

	/** Product version label. */
	public JLabel getProductVersion() { return productVersion; }

	/** Product license label. */
	public JLabel getProductLicense() { return productLicense; }

	/** Theme selector; index 0 is light and index 1 is dark. */
	public JComboBox<String> getTheme() { return theme; }

	/** All visible label/button texts below a root, used by tests and diagnostics. */
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
