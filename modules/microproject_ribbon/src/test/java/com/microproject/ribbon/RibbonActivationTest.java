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
import com.microproject.ribbon.SwingRibbonModel.ButtonSize;
import com.microproject.ribbon.SwingRibbonModel.RibbonButton;

class RibbonActivationTest {
	@Test
	void parsesNamesIgnoringCase() {
		for (RibbonActivation activation : RibbonActivation.values()) {
			assertEquals(activation, RibbonActivation.parse(" " + activation.name().toLowerCase() + " "));
		}
		assertThrows(IllegalArgumentException.class, () -> RibbonActivation.parse("hover"));
		assertThrows(IllegalArgumentException.class, () -> RibbonActivation.parse(null));
	}

	@Test
	void traitsDescribeWhatStaysOnScreenAfterTheClick() {
		assertTrue(RibbonActivation.TOGGLE_STATE.keepsPressedState());
		assertTrue(RibbonActivation.EXCLUSIVE_VIEW.keepsPressedState());
		assertTrue(RibbonActivation.MODE.keepsPressedState());
		assertFalse(RibbonActivation.COMMAND.keepsPressedState());
		assertFalse(RibbonActivation.DIALOG.keepsPressedState());
		assertFalse(RibbonActivation.POPUP.keepsPressedState());
		assertTrue(RibbonActivation.DIALOG.opensSurface());
		assertTrue(RibbonActivation.POPUP.opensSurface());
		assertTrue(RibbonActivation.BACKSTAGE.opensSurface());
		assertFalse(RibbonActivation.COMMAND.opensSurface());
		assertTrue(RibbonActivation.POPUP.pressedWhileOpen());
		assertFalse(RibbonActivation.DIALOG.pressedWhileOpen());
	}

	@Test
	void defaultIsInferredFromTheOtherMetadata() {
		assertEquals(RibbonActivation.COMMAND, new RibbonButton("a", ButtonPriority.TOP).getActivation());
		assertEquals(RibbonActivation.TOGGLE_STATE, new RibbonButton("a", ButtonPriority.TOP, null, true).getActivation());
		assertEquals(RibbonActivation.COMMAND,
			new RibbonButton("a", ButtonPriority.TOP, null, false, ButtonPresentation.SPLIT).getActivation());
		assertEquals(RibbonActivation.POPUP, new RibbonButton("a", ButtonPriority.TOP)
			.withControl(RibbonControlKind.MENU, List.of("b"), null).getActivation());
		assertEquals(RibbonActivation.BACKSTAGE, new RibbonButton("a", "A", ButtonPriority.TOP, ButtonSize.LARGE, null,
			false, ButtonPresentation.BACKSTAGE, 0, "open", false).getActivation());
		assertEquals(RibbonActivation.COMMAND, new RibbonButton("a", "A", ButtonPriority.TOP, ButtonSize.LARGE, null,
			false, ButtonPresentation.BACKSTAGE, 0, "save", true).getActivation());
	}

	@Test
	void explicitActivationSurvivesWithControl() {
		RibbonButton dialog = new RibbonButton("a", ButtonPriority.TOP).withActivation(RibbonActivation.DIALOG);
		assertEquals(RibbonActivation.DIALOG, dialog.getActivation());
		assertEquals(RibbonActivation.DIALOG,
			dialog.withControl(RibbonControlKind.SPLIT_BUTTON, List.of("b"), "K").getActivation());
		assertEquals(RibbonControlKind.BUTTON, dialog.getControlKind());
	}
}
