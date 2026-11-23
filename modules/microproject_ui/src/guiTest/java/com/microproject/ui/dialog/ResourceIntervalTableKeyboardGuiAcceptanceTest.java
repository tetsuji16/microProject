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
package com.microproject.ui.dialog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.KeyEvent;

import javax.swing.JOptionPane;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.microproject.interval.ValueObjectForIntervalTable;
import com.microproject.pm.task.Project;
import com.microproject.strings.Messages;
import com.microproject.ui.testsupport.GuiAcceptanceSupport;
import com.microproject.ui.testsupport.GuiJourney;
import com.microproject.ui.testsupport.ResourceIntervalTableFixture;

/**
 * Issue #997: Microsoft Project deletes and cuts dialog table rows from the keyboard, and
 * it refuses to remove the first effective-dated row. The dialog root pane must route
 * Delete and Ctrl+X to the same refusal the row popup uses, leaving row, data, clipboard
 * and saved state untouched.
 */
class ResourceIntervalTableKeyboardGuiAcceptanceTest {
	@Test
	@GuiJourney(GuiJourney.InputMechanism.KEYBOARD_SHORTCUT)
	void physicalKeyboardRefusesTheFirstAvailabilityRow() throws Exception {
		runKeyboardScenario(false);
	}

	@Test
	@GuiJourney(GuiJourney.InputMechanism.KEYBOARD_SHORTCUT)
	void physicalKeyboardRefusesTheFirstCostRateRow() throws Exception {
		runKeyboardScenario(true);
	}

	private void runKeyboardScenario(boolean costRate) throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
				"A desktop session is required for Robot acceptance coverage.");
		try (ResourceIntervalTableFixture fixture = ResourceIntervalTableFixture.open(costRate)) {
			ValueObjectForIntervalTable table = fixture.table;
			Project project = fixture.project;
			int rowsBefore = fixture.sheet.getRowCount();
			assertEquals(2, table.getList().size());

			var clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			Transferable previous = clipboard.getContents(null);
			try {
				String marker = "interval-keyboard-clipboard-marker";
				clipboard.setContents(new StringSelection(marker), null);
				project.markSaved();

				fixture.selectRowHeader(0);
				pressCut(fixture);
				GuiAcceptanceSupport.await(() -> ResourceIntervalTableFixture.visibleError() != null,
						"Ctrl+X on a protected row must report the refusal");
				JOptionPane error = ResourceIntervalTableFixture.visibleError();
				assertEquals(Messages.getString("ValueObjectForIntervalTable.YouCannotRemoveTheFirst"),
						error.getMessage());
				fixture.dismissError();
				assertEquals(marker, clipboard.getData(DataFlavor.stringFlavor),
						"a refused keyboard cut must leave the clipboard alone");
				assertEquals(2, table.getList().size(), "the first interval must stay");
				assertEquals(rowsBefore, fixture.sheet.getRowCount(), "the table rows must stay");
				assertFalse(project.needsSaving(), "a refused cut must not dirty the project");

				fixture.selectRowHeader(0);
				pressDelete(fixture);
				GuiAcceptanceSupport.await(() -> ResourceIntervalTableFixture.visibleError() != null,
						"Delete on a protected row must report the refusal");
				assertEquals(Messages.getString("ValueObjectForIntervalTable.YouCannotRemoveTheFirst"),
						ResourceIntervalTableFixture.visibleError().getMessage());
				fixture.dismissError();
				assertEquals(2, table.getList().size(), "the first interval must stay");
				assertEquals(rowsBefore, fixture.sheet.getRowCount(), "the table rows must stay");
				assertFalse(project.needsSaving(), "a refused delete must not dirty the project");
			} finally {
				if (previous != null) clipboard.setContents(previous, null);
			}
		}
	}

	private static void pressCut(ResourceIntervalTableFixture fixture) {
		fixture.robot.keyPress(KeyEvent.VK_CONTROL);
		fixture.robot.keyPress(KeyEvent.VK_X);
		fixture.robot.keyRelease(KeyEvent.VK_X);
		fixture.robot.keyRelease(KeyEvent.VK_CONTROL);
		fixture.robot.waitForIdle();
	}

	private static void pressDelete(ResourceIntervalTableFixture fixture) {
		fixture.robot.keyPress(KeyEvent.VK_DELETE);
		fixture.robot.keyRelease(KeyEvent.VK_DELETE);
		fixture.robot.waitForIdle();
	}
}
