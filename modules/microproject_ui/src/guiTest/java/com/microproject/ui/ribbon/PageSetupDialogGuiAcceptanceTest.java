/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.ui.ribbon;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Dialog;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.util.Locale;

import javax.swing.AbstractButton;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.microproject.i18n.Messages;
import com.microproject.ui.dialog.PageSetupDialog;
import com.microproject.ui.menu.testsupport.MenuDefinitionSupport;
import com.microproject.ui.menu.testsupport.UiComponentWalker;
import com.microproject.ui.testsupport.DialogScreenshotArtifacts;
import com.microproject.ui.testsupport.GuiAcceptanceSupport;
import com.microproject.ui.testsupport.GuiJourney;

/** Physical Page Setup route and localized field-width regression for issue #1359. */
class PageSetupDialogGuiAcceptanceTest {
	@Test
	@GuiJourney(GuiJourney.InputMechanism.DIALOG)
	void pageSetupControlsRemainVisibleAtPreferredWidth() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Page Setup acceptance.");
		try (RibbonCommandGuiFixture fixture = RibbonCommandGuiFixture.open(4)) {
			Locale locale = Locale.getDefault();
			String fileTabText = MenuDefinitionSupport.menuBundle(locale).getString("FileRibbonTask.title");
			AbstractButton fileTab = UiComponentWalker.flatten(fixture.window).stream()
				.filter(AbstractButton.class::isInstance).map(AbstractButton.class::cast)
				.filter(button -> button.isShowing() && fileTabText.equals(button.getText())).findFirst().orElse(null);
			assertNotNull(fileTab, "File tab must be visible");
			fixture.click(fileTab);
			GuiAcceptanceSupport.await(() -> named(fixture.window, "officeBackstageOverlay") != null,
				"File tab did not open Backstage");

			AbstractButton printPage = awaitButton(fixture, "officeBackstageNav-print", "Print navigation item");
			fixture.click(printPage);
			GuiAcceptanceSupport.await(() -> named(fixture.window, "officeBackstagePrintPage") != null,
				"Print page did not open");
			AbstractButton pageSetup = awaitButton(fixture, "officeBackstagePrintPageSetup", "Page Setup link");
			GuiAcceptanceSupport.await(pageSetup::isEnabled, "Page Setup link did not become enabled after preview creation");
			fixture.click(pageSetup);
			GuiAcceptanceSupport.await(() -> visiblePageSetup() != null, "Page Setup dialog did not open from Print");
			PageSetupDialog dialog = visiblePageSetup();
			assertTrue(dialog.isActive(), "Page Setup dialog must become active after its physical open route");

			Rectangle dialogBounds = new Rectangle(0, 0, dialog.getRootPane().getWidth(), dialog.getRootPane().getHeight());
			DialogScreenshotArtifacts.capture(dialog, "issue-1359-page-setup-" + locale.getLanguage());
			int checked = 0;
			for (Component control : UiComponentWalker.flatten(dialog)) {
				if (!control.isShowing() || !isTextControl(control)) continue;
				Rectangle bounds = SwingUtilities.convertRectangle(control.getParent(), control.getBounds(), dialog.getRootPane());
				assertTrue(dialogBounds.contains(bounds), () -> control.getClass().getSimpleName() + " text=" + textOf(control)
					+ " is clipped: bounds=" + bounds + " dialog=" + dialogBounds + " size=" + dialog.getSize());
				assertTrue(control.getWidth() >= control.getPreferredSize().width,
					() -> control.getClass().getSimpleName() + " is narrower than preferred: " + control.getBounds()
						+ " preferred=" + control.getPreferredSize() + " locale=" + locale);
				checked++;
			}
			assertTrue(checked >= 15, "Page Setup must expose its paper, margin and scaling controls; checked=" + checked);

			AbstractButton cancel = UiComponentWalker.flatten(dialog).stream()
				.filter(AbstractButton.class::isInstance).map(AbstractButton.class::cast)
				.filter(button -> Messages.getString("ButtonText.Cancel").equals(button.getText())).findFirst().orElse(null);
			assertNotNull(cancel, "Page Setup must expose its Cancel button");
			fixture.click(cancel);
			GuiAcceptanceSupport.await(() -> !dialog.isShowing(), "Page Setup did not close through its visible Cancel button");
		}
	}

	private static String textOf(Component component) {
		if (component instanceof JLabel label) return label.getText();
		if (component instanceof AbstractButton button) return button.getText();
		return component.toString();
	}

	private static boolean isTextControl(Component component) {
		if (component instanceof JLabel label) return label.getText() != null && !label.getText().isBlank();
		return component instanceof JFormattedTextField || component instanceof JComboBox<?> || component instanceof JSpinner;
	}

	private static Component named(Component root, String name) {
		return UiComponentWalker.flatten(root).stream()
			.filter(component -> component.isShowing() && name.equals(component.getName())).findFirst().orElse(null);
	}

	private static AbstractButton awaitButton(RibbonCommandGuiFixture fixture, String name, String description) throws Exception {
		final AbstractButton[] result = new AbstractButton[1];
		GuiAcceptanceSupport.await(() -> {
			Component component = named(fixture.window, name);
			result[0] = component instanceof AbstractButton button ? button : null;
			return result[0] != null;
		}, description + " did not become visible");
		return result[0];
	}

	private static PageSetupDialog visiblePageSetup() {
		for (java.awt.Window window : java.awt.Window.getWindows())
			if (window instanceof PageSetupDialog dialog && dialog.isShowing()) return dialog;
		return null;
	}
}
