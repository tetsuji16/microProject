package com.microproject.menu.testsupport;

import java.awt.Component;

import javax.swing.AbstractButton;

import org.junit.jupiter.api.Assertions;

public final class ButtonVisibilityValidator {
	private ButtonVisibilityValidator() {
	}

	public static void assertValidSwingButton(String id, AbstractButton button, boolean expectAction) {
		Assertions.assertNotNull(button, () -> id + " button was not created");
		Assertions.assertTrue(button.isVisible(), () -> id + " button is unexpectedly hidden");
		Assertions.assertTrue(hasText(button.getText()) || button.getIcon() != null,
			() -> id + " button is missing both text and icon");
		Assertions.assertTrue(hasText(button.getToolTipText()) || hasText(button.getText()),
			() -> id + " button is missing both tooltip and text");
		if (expectAction) {
			Assertions.assertNotNull(button.getAction(), () -> id + " swing button is missing an action");
		}
	}

	public static void assertAttachedButtonsAreVisible(Component root, String context) {
		for (Component component : UiComponentWalker.flatten(root)) {
			if (component instanceof AbstractButton swingButton) {
				Assertions.assertNotNull(swingButton.getParent(),
					() -> context + ": swing button is detached from the component tree");
				Assertions.assertTrue(swingButton.isVisible(),
					() -> context + ": swing button is unexpectedly hidden");
			}
		}
	}

	private static boolean hasText(String value) {
		return value != null && !value.isBlank();
	}
}
