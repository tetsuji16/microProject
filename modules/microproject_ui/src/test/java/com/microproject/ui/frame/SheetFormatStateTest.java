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
package com.microproject.ui.frame;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.microproject.ui.view.views.GanttView;
import com.microproject.ui.view.views.UsageDetailView;
import com.microproject.workspace.SavableToWorkspace;
import com.microproject.workspace.WorkspaceSetting;

/** Sheet gridline command target state and Task Usage summary-task visibility persistence. */
class SheetFormatStateTest {
	@Test
	void sheetGridlinesCommandKeepsTheGanttViewStoredStateInSync() throws Exception {
		try (HeadlessDocumentFixture fx = new HeadlessDocumentFixture("First")) {
			HeadlessDocumentFixture.onEdt(() -> {
				GanttView gantt = fx.frame.getGanttView();
				GraphicManager.applySheetGridVisibility(gantt, false);
				assertFalse(gantt.isSpreadsheetGridVisible());
				assertFalse(gantt.getSpreadSheet().isGridVisible());
				GraphicManager.applySheetGridVisibility(gantt, true);
				assertTrue(gantt.isSpreadsheetGridVisible());
				assertTrue(gantt.getSpreadSheet().isGridVisible());
			});
		}
	}

	@Test
	void taskUsageHideSummaryTasksRoundTripsThroughTheSerializedWorkspace() throws Exception {
		try (HeadlessDocumentFixture fx = new HeadlessDocumentFixture("First")) {
			AtomicReference<UsageDetailView> view = new AtomicReference<>();
			AtomicReference<WorkspaceSetting> saved = new AtomicReference<>();
			HeadlessDocumentFixture.onEdt(() -> {
				view.set(fx.frame.getTaskUsageDetailView());
				assertTrue(view.get().supportsSummaryTasksToggle());
				assertTrue(view.get().isSummaryTasksVisible());
				view.get().setSummaryTasksVisible(false);
				saved.set(view.get().createWorkspace(SavableToWorkspace.PERSIST));
			});
			assertEquals(Boolean.FALSE, ((UsageDetailView.Workspace) saved.get()).getSummaryTasksVisible());

			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
				out.writeObject(saved.get());
			}
			WorkspaceSetting loaded;
			try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
				loaded = (WorkspaceSetting) in.readObject();
			}

			HeadlessDocumentFixture.onEdt(() -> {
				view.get().setSummaryTasksVisible(true);
				view.get().restoreWorkspace(loaded, SavableToWorkspace.PERSIST);
				assertFalse(view.get().isSummaryTasksVisible());
				// a workspace saved before the switch existed leaves summary tasks visible
				view.get().restoreWorkspace(new UsageDetailView.Workspace(), SavableToWorkspace.PERSIST);
				assertTrue(view.get().isSummaryTasksVisible());
			});
		}
	}
}
