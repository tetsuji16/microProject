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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionListener;

import com.microproject.pm.graphic.gantt.Gantt;
import com.microproject.pm.graphic.model.cache.ProjectionRowKey;
import com.microproject.pm.graphic.model.cache.ViewNodeModelCache;
import com.microproject.pm.graphic.model.event.CacheListener;
import com.microproject.pm.graphic.model.event.CompositeCacheEvent;

/**
 * Owns the bidirectional table/Gantt selection contract for one task view.
 * Selection is stored as projection identity; row numbers exist only while
 * adapting to Swing's row-based selection API.
 */
final class TaskSelectionController implements AutoCloseable, CacheListener {
	private final Gantt gantt;
	private final JTable table;
	private final ViewNodeModelCache cache;
	private final ListSelectionListener tableSelectionListener = event -> syncFromTable();
	private volatile Set<ProjectionRowKey> selectedKeys = Collections.emptySet();
	private long reconciledTopologyRevision = -1L;
	private boolean synchronizing;
	private volatile boolean closed;

	TaskSelectionController(Gantt gantt, JTable table) {
		this.gantt = gantt;
		this.table = table;
		this.cache = gantt != null && gantt.getCache() instanceof ViewNodeModelCache viewCache ? viewCache : null;
		if (table != null)
			table.getSelectionModel().addListSelectionListener(tableSelectionListener);
		if (gantt != null)
			gantt.setBarSelectionListener(this::syncFromChart);
		if (cache != null)
			cache.addNodeModelListener(this);
		syncFromTable();
	}

	private void syncFromTable() {
		if (closed || synchronizing || gantt == null)
			return;
		int[] rows = table == null ? null : table.getSelectedRows();
		if (rows == null || rows.length == 0) {
			publish(Collections.emptySet());
			return;
		}
		Set<ProjectionRowKey> keys = new HashSet<>(rows.length);
		for (int row : rows) {
			ProjectionRowKey key = row < 0 ? null : gantt.getProjectionRowKey(row);
			if (key != null)
				keys.add(key);
		}
		publish(keys);
	}

	private void syncFromChart(Gantt.BarClick click) {
		if (closed || click == null || table == null)
			return;
		if (click.rowKey() == null) {
			table.clearSelection();
			return;
		}
		int row = projectionRowForClick(gantt, click);
		if (row < 0 || row >= table.getRowCount())
			return;
		int column = table.getSelectedColumn();
		if (column < 0)
			column = 0;
		if (column >= table.getColumnCount())
			column = Math.max(0, table.getColumnCount() - 1);
		table.changeSelection(row, column, click.toggle(), click.extend());
	}

	@Override
	public void graphicNodesCompositeEvent(CompositeCacheEvent event) {
		if (closed || cache == null || table == null)
			return;
		long expectedRevision = cache.getProjectionSnapshot().topologyRevision();
		if (!SwingUtilities.isEventDispatchThread()) {
			SwingUtilities.invokeLater(() -> reconcileProjection(expectedRevision));
			return;
		}
		reconcileProjection(expectedRevision);
	}

	private void reconcileProjection(long expectedRevision) {
		if (closed || cache == null || table == null
				|| cache.getProjectionSnapshot().topologyRevision() != expectedRevision
				|| expectedRevision <= reconciledTopologyRevision)
			return;
		reconciledTopologyRevision = expectedRevision;
		Set<ProjectionRowKey> surviving = new HashSet<>(selectedKeys.size());
		for (ProjectionRowKey key : selectedKeys) {
			if (cache.getRowAt(key) >= 0)
				surviving.add(key);
		}
		synchronizing = true;
		try {
			table.clearSelection();
			for (ProjectionRowKey key : surviving) {
				int row = cache.getRowAt(key);
				if (row >= 0 && row < table.getRowCount())
					table.addRowSelectionInterval(row, row);
			}
		} finally {
			synchronizing = false;
		}
		publish(surviving);
	}

	private void publish(Set<ProjectionRowKey> keys) {
		selectedKeys = keys.isEmpty() ? Collections.emptySet() : Set.copyOf(keys);
		gantt.setHighlightedRowKeys(selectedKeys);
	}

	static int projectionRowForClick(Gantt gantt, Gantt.BarClick click) {
		if (gantt == null || click == null || click.rowKey() == null) return -1;
		if (gantt.getCache() instanceof ViewNodeModelCache cache) {
			var installed=cache.getInstalledProjectionSnapshot();
			if (click.domainRevision() != installed.topology().domainRevision()
					|| click.topologyRevision() != installed.topology().topologyRevision()) return -1;
		}
		return gantt.getProjectionRow(click.rowKey());
	}

	@Override
	public void close() {
		if (closed)
			return;
		closed = true;
		if (cache != null)
			cache.removeNodeModelListener(this);
		if (table != null)
			table.getSelectionModel().removeListSelectionListener(tableSelectionListener);
		if (gantt != null) {
			gantt.setBarSelectionListener(null);
			gantt.setHighlightedRowKeys(Collections.emptySet());
		}
		selectedKeys = Collections.emptySet();
	}
}
