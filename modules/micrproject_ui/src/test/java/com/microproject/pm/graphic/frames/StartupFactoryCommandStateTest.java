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
package com.microproject.pm.graphic.frames;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import javax.swing.AbstractButton;
import javax.swing.JPanel;

import org.junit.jupiter.api.Test;

import com.microproject.menu.MenuManager;

class StartupFactoryCommandStateTest {
	@Test
	void successfulLoginRestoresGlobalFileCommandsAfterStartupGate() {
		GraphicManager graphicManager = new GraphicManager(new JPanel());
		MenuManager menuManager = graphicManager.getMenuManager();
		menuManager.createRibbonPanel(MenuManager.STANDARD_RIBBON, null);

		graphicManager.setConnected(false);
		assertCommandsEnabled(menuManager, false);

		StartupFactory.markLoginSuccessful(graphicManager);

		assertCommandsEnabled(menuManager, true);
	}

	private static void assertCommandsEnabled(MenuManager menuManager, boolean expected) {
		for (String id : List.of("RibbonNewProject", "RibbonOpenProject", "RibbonRecentProjects", "RibbonImportProject")) {
			AbstractButton button = menuManager.getToolButtonsFromId(id).stream()
				.map(AbstractButton.class::cast)
				.filter(candidate -> id.equals(candidate.getActionCommand()))
				.findFirst()
				.orElseThrow(() -> new AssertionError(id + " was not created"));
			if (expected) {
				assertTrue(button.isEnabled(), () -> id + " must be enabled after a successful login");
			} else {
				assertFalse(button.isEnabled(), () -> id + " must be disabled while startup is gated");
			}
		}
	}
}
