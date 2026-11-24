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

import static com.microproject.ui.testsupport.ResourceIntervalTableFixture.click;
import static com.microproject.ui.testsupport.ResourceIntervalTableFixture.find;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.InputEvent;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.microproject.interval.ValueObjectForIntervalTable;
import com.microproject.ui.menu.MenuActionConstants;
import com.microproject.ui.view.spreadsheet.SpreadSheet;
import com.microproject.ui.view.spreadsheet.SpreadSheetPopupMenu;
import com.microproject.pm.task.Project;
import com.microproject.i18n.Messages;
import com.microproject.ui.testsupport.GuiAcceptanceSupport;
import com.microproject.ui.testsupport.ResourceIntervalTableFixture;

/** Removes an availability row by physical right-click, then requires the project to prompt for saving (#987). */
class ResourceAvailabilityRowDeleteGuiAcceptanceTest {
	@Test
	void physicalPopupDeleteOfAvailabilityRowMarksProjectForSaving() throws Exception {
		runIntervalRowScenario(false);
	}

	@Test
	void physicalPopupDeleteOfCostRateRowMarksProjectForSaving() throws Exception {
		runIntervalRowScenario(true);
	}

	private void runIntervalRowScenario(boolean costRate) throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
				"A desktop session is required for Robot acceptance coverage.");
		try (ResourceIntervalTableFixture fixture = ResourceIntervalTableFixture.open(costRate)) {
			SpreadSheet sheet = fixture.sheet;
			ValueObjectForIntervalTable table = fixture.table;
			Project project = fixture.project;

			fixture.rightClickCell(1, 1);
			SpreadSheetPopupMenu popup = sheet.getPopup();
			assertEquals(1, popup.getRow(), "Popup context must resolve the clicked row");

			JMenuItem delete = find(popup, JMenuItem.class,
					c -> ("popup." + MenuActionConstants.ACTION_DELETE).equals(c.getName())); //$NON-NLS-1$
			assertNotNull(delete, "The interval popup must offer Delete");
			click(fixture.robot, delete, new Rectangle(delete.getSize()), InputEvent.BUTTON1_DOWN_MASK);

			GuiAcceptanceSupport.await(() -> table.getList().size() == 1, "Popup Delete did not remove the interval row");
			GuiAcceptanceSupport.await(() -> sheet.getRowCount() == 2, "The table must render one data row and the void row");
			assertTrue(project.needsSaving(), "Removing an interval row must prompt to save");

			// The first interval is protected. Exercise the actual row-header popup
			// cut route and verify that its rejection does not replace the clipboard.
			var clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			Transferable previous = clipboard.getContents(null);
			try {
				String marker = "interval-cut-clipboard-marker";
				clipboard.setContents(new StringSelection(marker), null);
				project.markSaved();
				fixture.selectRowHeader(0);
				fixture.rightClickCell(0, 1);
				JMenuItem cut = find(sheet.getPopup(), JMenuItem.class,
						c -> ("popup." + MenuActionConstants.ACTION_CUT).equals(c.getName())); //$NON-NLS-1$
				assertNotNull(cut, "The row popup must offer Cut");
				click(fixture.robot, cut, new Rectangle(cut.getSize()), InputEvent.BUTTON1_DOWN_MASK);
				GuiAcceptanceSupport.await(() -> ResourceIntervalTableFixture.visibleError() != null,
						"Protected row cut must report the refusal");
				JOptionPane error = ResourceIntervalTableFixture.visibleError();
				assertEquals(Messages.getString("ValueObjectForIntervalTable.YouCannotRemoveTheFirst"),
						error.getMessage());
				fixture.dismissError();
				assertEquals(marker, clipboard.getData(DataFlavor.stringFlavor));
				assertEquals(1, table.getList().size());
				assertEquals(2, sheet.getRowCount());
				assertFalse(project.needsSaving(), "A refused cut must not dirty the project");
			} finally {
				if (previous != null) clipboard.setContents(previous, null);
			}
		}
	}
}
