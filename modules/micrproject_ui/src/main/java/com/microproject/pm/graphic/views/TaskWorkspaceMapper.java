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
package com.microproject.pm.graphic.views;

import java.awt.Container;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.swing.JViewport;
import javax.swing.table.TableColumn;

import com.microproject.field.Field;
import com.microproject.pm.graphic.model.cache.ProjectionRowKey;
import com.microproject.pm.graphic.model.cache.ProjectionRowKeyCodec;
import com.microproject.pm.graphic.model.cache.RevisionedProjectionIndex;
import com.microproject.pm.graphic.model.cache.ViewNodeModelCache;
import com.microproject.pm.graphic.spreadsheet.SpreadSheet;
import com.microproject.pm.graphic.spreadsheet.common.CommonSpreadSheet;

/** Maps task-view Workspace state without persisting transient row numbers as truth. */
final class TaskWorkspaceMapper {
	static final int SCHEMA_VERSION = 2;

	private TaskWorkspaceMapper() {
	}

	static void capture(SpreadSheet sheet, ViewNodeModelCache cache, CommonSpreadSheet.Workspace workspace) {
		if (sheet == null || cache == null || workspace == null)
			return;
		workspace.setSchemaVersion(SCHEMA_VERSION);
		workspace.setSelectedEntityKeys(keysAt(cache, sheet.getSelectedRows()));
		workspace.setSelectedFieldIds(fieldIdsAt(sheet, sheet.getSelectedColumns()));
		int activeRow = sheet.getSelection() == null ? sheet.getSelectedRow() : sheet.getSelection().getActiveRow();
		int activeColumn = sheet.getSelection() == null ? sheet.getSelectedColumn() : sheet.getSelection().getActiveColumn();
		workspace.setActiveEntityKey(encodedKeyAt(cache, activeRow).orElse(null));
		workspace.setActiveFieldId(fieldIdAt(sheet, activeColumn));
		captureScrollAnchor(sheet, cache, workspace);
	}

	static void restore(SpreadSheet sheet, ViewNodeModelCache cache, CommonSpreadSheet.Workspace workspace) {
		if (sheet == null || cache == null || workspace == null || workspace.getSchemaVersion() < SCHEMA_VERSION)
			return;
		sheet.clearSelection();
		for (String encoded : nonNull(workspace.getSelectedEntityKeys())) {
			int row = rowOf(cache, encoded);
			if (row >= 0 && row < sheet.getRowCount())
				sheet.addRowSelectionInterval(row, row);
		}
		for (String fieldId : nonNull(workspace.getSelectedFieldIds())) {
			int column = columnOf(sheet, fieldId);
			if (column >= 0)
				sheet.addColumnSelectionInterval(column, column);
		}
		int activeRow = rowOf(cache, workspace.getActiveEntityKey());
		int activeColumn = columnOf(sheet, workspace.getActiveFieldId());
		if (sheet.getSelection() != null) {
			if (activeRow >= 0 && activeColumn >= 0)
				sheet.getSelection().setActiveCell(activeRow, activeColumn);
			else
				sheet.getSelection().clearActiveCell();
		}
		restoreScrollAnchor(sheet, cache, workspace);
	}

	private static void captureScrollAnchor(SpreadSheet sheet, ViewNodeModelCache cache,
			CommonSpreadSheet.Workspace workspace) {
		Container parent = sheet.getParent();
		if (!(parent instanceof JViewport viewport) || sheet.getRowCount() == 0)
			return;
		Point position = viewport.getViewPosition();
		int row = sheet.rowAtPoint(new Point(0, position.y));
		if (row < 0)
			row = Math.min(sheet.getRowCount() - 1, Math.max(0, position.y / Math.max(1, sheet.getRowHeight())));
		workspace.setTopVisibleEntityKey(encodedKeyAt(cache, row).orElse(null));
		workspace.setNextVisibleEntityKey(nextDurableKey(cache, row, 1).orElse(null));
		workspace.setPreviousVisibleEntityKey(nextDurableKey(cache, row, -1).orElse(null));
		workspace.setTopVisibleRowOffset(Math.max(0, position.y - row * sheet.getRowHeight()));
	}

	private static void restoreScrollAnchor(SpreadSheet sheet, ViewNodeModelCache cache,
			CommonSpreadSheet.Workspace workspace) {
		int row = rowOf(cache, workspace.getTopVisibleEntityKey());
		if (row < 0)
			row = rowOf(cache, workspace.getNextVisibleEntityKey());
		if (row < 0)
			row = rowOf(cache, workspace.getPreviousVisibleEntityKey());
		if (row < 0 && sheet.getRowCount() > 0)
			row = 0;
		if (row < 0)
			return;
		int y = row * sheet.getRowHeight() + Math.max(0, workspace.getTopVisibleRowOffset());
		Container parent = sheet.getParent();
		if (parent instanceof JViewport viewport) {
			Point current = viewport.getViewPosition();
			viewport.setViewPosition(new Point(current.x, y));
		} else {
			Rectangle cell = sheet.getCellRect(row, 0, true);
			sheet.scrollRectToVisible(cell);
		}
	}

	private static String[] keysAt(ViewNodeModelCache cache, int[] rows) {
		List<String> keys = new ArrayList<>();
		if (rows != null) {
			for (int row : rows)
				encodedKeyAt(cache, row).ifPresent(keys::add);
		}
		return keys.toArray(String[]::new);
	}

	private static String[] fieldIdsAt(SpreadSheet sheet, int[] columns) {
		List<String> ids = new ArrayList<>();
		if (columns != null) {
			for (int column : columns) {
				String id = fieldIdAt(sheet, column);
				if (id != null)
					ids.add(id);
			}
		}
		return ids.toArray(String[]::new);
	}

	private static Optional<String> encodedKeyAt(ViewNodeModelCache cache, int row) {
		return ProjectionRowKeyCodec.encodeDurable(cache.getRowKeyAt(row));
	}

	private static Optional<String> nextDurableKey(ViewNodeModelCache cache, int row, int direction) {
		for (int candidate = row + direction; candidate >= 0 && candidate < cache.getSize(); candidate += direction) {
			Optional<String> encoded = encodedKeyAt(cache, candidate);
			if (encoded.isPresent())
				return encoded;
		}
		return Optional.empty();
	}

	private static int rowOf(ViewNodeModelCache cache, String encoded) {
		Optional<ProjectionRowKey> decoded = ProjectionRowKeyCodec.decodeDurable(encoded);
		if (decoded.isEmpty())
			return -1;
		RevisionedProjectionIndex.Snapshot snapshot = cache.getProjectionSnapshot();
		return snapshot.rowOf(decoded.get());
	}

	private static String fieldIdAt(SpreadSheet sheet, int column) {
		if (column < 0 || column >= sheet.getColumnCount())
			return null;
		TableColumn tableColumn = sheet.getColumnModel().getColumn(column);
		return tableColumn.getIdentifier() instanceof Field field ? field.getId() : null;
	}

	private static int columnOf(SpreadSheet sheet, String fieldId) {
		if (fieldId == null)
			return -1;
		for (int column = 0; column < sheet.getColumnCount(); column++) {
			if (fieldId.equals(fieldIdAt(sheet, column)))
				return column;
		}
		return -1;
	}

	private static String[] nonNull(String[] values) {
		return values == null ? new String[0] : values;
	}
}
