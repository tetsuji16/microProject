package com.projectlibre1.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import javax.swing.JButton;
import javax.swing.JToggleButton;

import org.junit.jupiter.api.Test;

class FlatUiSupportButtonStateTest {
	@Test
	void toolbarActionButtonsUseHoverStateButIgnorePersistentSelectionWhenNotToggle() {
		JButton button = new JButton("Save");
		FlatUiSupport.styleToolBarButton(button);

		assertEquals(
			FlatUiSupport.BUTTON_STYLE_ROLE_TOOLBAR,
			button.getClientProperty(FlatUiSupport.BUTTON_STYLE_ROLE_PROPERTY));

		button.getModel().setRollover(true);
		assertEquals(
			FlatUiSupport.commandButtonHoverBackground(button),
			FlatUiSupport.resolveCommandButtonBackground(button));

		button.getModel().setRollover(false);
		button.getModel().setSelected(true);
		assertNull(FlatUiSupport.resolveCommandButtonBackground(button));
	}

	@Test
	void toggleButtonsPreferSelectedStateOverHover() {
		JToggleButton button = new JToggleButton("Toggle");
		FlatUiSupport.styleRibbonSmallButton(button);

		button.getModel().setRollover(true);
		assertEquals(
			FlatUiSupport.commandButtonHoverBackground(button),
			FlatUiSupport.resolveCommandButtonBackground(button));

		button.getModel().setSelected(true);
		assertEquals(
			FlatUiSupport.commandButtonSelectedBackground(button),
			FlatUiSupport.resolveCommandButtonBackground(button));
		assertNotEquals(
			FlatUiSupport.commandButtonHoverBackground(button),
			FlatUiSupport.resolveCommandButtonBackground(button));
	}

	@Test
	void disabledButtonsDoNotInheritHoverOrSelectedEmphasis() {
		JToggleButton button = new JToggleButton("Disabled");
		FlatUiSupport.styleRibbonLargeButton(button);
		button.getModel().setRollover(true);
		button.getModel().setSelected(true);
		button.setEnabled(false);

		assertNull(FlatUiSupport.resolveCommandButtonBackground(button));
		assertNotNull(FlatUiSupport.resolveCommandButtonBorderColor(button));
		assertNotEquals(
			FlatUiSupport.commandButtonSelectedBorderColor(button),
			FlatUiSupport.resolveCommandButtonBorderColor(button));
	}

	@Test
	void ribbonTabsKeepHoverAndSelectedStatesDistinct() {
		JToggleButton button = new JToggleButton("Task");
		FlatUiSupport.styleRibbonTabButton(button);

		button.getModel().setRollover(true);
		assertEquals(FlatUiSupport.ribbonTabHoverColor(), FlatUiSupport.resolveRibbonTabBackground(button));
		assertNull(FlatUiSupport.resolveRibbonTabBorderColor(button));

		button.getModel().setSelected(true);
		assertNull(FlatUiSupport.resolveRibbonTabBackground(button));
		assertNotNull(FlatUiSupport.resolveRibbonTabUnderlineColor(button));
	}
}
