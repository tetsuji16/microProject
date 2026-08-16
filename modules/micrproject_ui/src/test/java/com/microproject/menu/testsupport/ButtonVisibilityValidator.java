/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2012-2019 ProjectLibre, Inc.  (Previous Copyright Holder)
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
