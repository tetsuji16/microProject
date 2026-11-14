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
package com.microproject.ribbon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.microproject.ribbon.SwingRibbonModel.ButtonPresentation;
import com.microproject.ribbon.SwingRibbonModel.ButtonPriority;
import com.microproject.ribbon.SwingRibbonModel.RibbonBand;
import com.microproject.ribbon.SwingRibbonModel.RibbonButton;

class RibbonControlKindTest {
	@Test
	void parsesEnumNamesAndCustomUiElementNames() {
		for (RibbonControlKind kind : RibbonControlKind.values()) {
			assertEquals(kind, RibbonControlKind.parse(kind.name()));
			assertEquals(kind, RibbonControlKind.parse(" " + kind.customUiElement().toUpperCase() + " "));
		}
		assertThrows(IllegalArgumentException.class, () -> RibbonControlKind.parse("carousel"));
		assertThrows(IllegalArgumentException.class, () -> RibbonControlKind.parse(null));
	}

	@Test
	void traitsDescribeTheInputContract() {
		assertTrue(RibbonControlKind.SPLIT_BUTTON.invokesOnClick());
		assertTrue(RibbonControlKind.SPLIT_BUTTON.hasPopup());
		assertFalse(RibbonControlKind.MENU.invokesOnClick());
		assertTrue(RibbonControlKind.MENU.hasPopup());
		assertTrue(RibbonControlKind.TOGGLE_BUTTON.hasPressedState());
		assertTrue(RibbonControlKind.CHECK_BOX.hasPressedState());
		assertFalse(RibbonControlKind.BUTTON.hasPopup());
		assertTrue(RibbonControlKind.COMBO_BOX.hasValue());
		assertTrue(RibbonControlKind.EDIT_BOX.hasValue());
		assertFalse(RibbonControlKind.EDIT_BOX.hasPopup());
	}

	@Test
	void buttonsDefaultToTheKindImpliedByLegacyMetadata() {
		assertEquals(RibbonControlKind.BUTTON, new RibbonButton("a", ButtonPriority.TOP).getControlKind());
		assertEquals(RibbonControlKind.TOGGLE_BUTTON, new RibbonButton("a", ButtonPriority.TOP, null, true).getControlKind());
		assertEquals(RibbonControlKind.SPLIT_BUTTON,
			new RibbonButton("a", ButtonPriority.TOP, null, false, ButtonPresentation.SPLIT).getControlKind());
	}

	@Test
	void withControlCopiesTheButtonAndKeepsItsOtherMetadata() {
		RibbonButton base = new RibbonButton("a", ButtonPriority.TOP, "icon", false);
		RibbonButton menu = base.withControl(RibbonControlKind.MENU, List.of("b", "-", "c"), "M");
		assertEquals(RibbonControlKind.BUTTON, base.getControlKind());
		assertEquals(RibbonControlKind.MENU, menu.getControlKind());
		assertEquals(List.of("b", "-", "c"), menu.getItemIds());
		assertEquals("M", menu.getKeyTip());
		assertEquals("icon", menu.getIconKey());
		assertEquals(base.getButtonSize(), menu.getButtonSize());
		assertTrue(base.withControl(RibbonControlKind.SPLIT_BUTTON, List.of(), null).isSplit());
	}

	@Test
	void bandCarriesAnOptionalDialogLauncher() {
		RibbonBand band = new RibbonBand("band", "Band", List.of(new RibbonButton("a", ButtonPriority.TOP)));
		assertEquals(null, band.getDialogLauncherId());
		RibbonBand launched = band.withDialogLauncher("dialog");
		assertEquals("dialog", launched.getDialogLauncherId());
		assertEquals(band.getButtons(), launched.getButtons());
	}
}
