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
package com.microproject.ui.view.views;

import com.microproject.ui.view.model.cache.NodeModelCache;
import com.microproject.ui.view.spreadsheet.SpreadSheet;
import com.microproject.ui.print.GraphPageable;
import com.microproject.graphic.configuration.SpreadSheetCategories;
import com.microproject.undo.UndoController;
import com.microproject.workspace.SavableToWorkspace;

public interface BaseView extends SpreadSheetCategories, SavableToWorkspace{
	UndoController getUndoController();
	void zoomIn();
	void zoomOut();
	void scrollToTask();
	boolean canZoomIn();
	boolean canZoomOut();
	boolean canScrollToTask();
	int getScale();
	SpreadSheet getSpreadSheet();
	/** Whether the Name column shows outline numbers (sheet Format, Show/Hide). */
	default boolean isOutlineNumbersVisible() {
		SpreadSheet sheet = getSpreadSheet();
		return sheet != null && sheet.isOutlineNumbersVisible();
	}
	default void setOutlineNumbersVisible(boolean visible) {
		SpreadSheet sheet = getSpreadSheet();
		if (sheet != null)
			sheet.setOutlineNumbersVisible(visible);
	}
	/** Whether the view can hide its summary task rows through the view transform. */
	default boolean supportsSummaryTasksToggle() { return false; }
	default boolean isSummaryTasksVisible() { return true; }
	default void setSummaryTasksVisible(boolean visible) { }
	boolean hasNormalMinWidth();
	String getViewName();
	/** Stable semantic state for the active view's rendered projection (for diagnostics). */
	default String getProjectionStateSignature() { return ""; }
	boolean showsTasks();
	boolean showsResources();
	void onActivate(boolean activate);
	boolean isPrintable();
	/** Optional specialized document that still participates in the shared print/export pipeline. */
	default GraphPageable createPrintDocument(boolean printOnly, boolean pdfAsDefault) { return null; }
	/** Same as above, with printers the caller already resolved off the event thread. */
	default GraphPageable createPrintDocument(boolean printOnly, boolean pdfAsDefault,
			javax.print.PrintService[] resolvedServices, javax.print.PrintService resolvedDefaultService) {
		return null;
	}
	void cleanUp();
	NodeModelCache getCache();
}

