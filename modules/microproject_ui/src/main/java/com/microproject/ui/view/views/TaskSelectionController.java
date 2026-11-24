/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.ui.view.views;

import java.util.LinkedHashSet;
import java.util.Set;

import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.microproject.ui.view.gantt.Gantt;
import com.microproject.ui.view.model.cache.NodeModelCache;
import com.microproject.ui.view.model.cache.ProjectionRowKey;
import com.microproject.ui.view.model.cache.RevisionedProjectionIndex;
import com.microproject.ui.view.model.event.CacheListener;
import com.microproject.ui.view.model.event.CompositeCacheEvent;
import com.microproject.ui.view.spreadsheet.SpreadSheet;

/** Owns the selection identity shared by one task table and its Gantt view. */
final class TaskSelectionController implements ListSelectionListener, CacheListener, AutoCloseable {
	private final Gantt gantt;
	private final SpreadSheet sheet;
	private final Set<ProjectionRowKey> selectedKeys = new LinkedHashSet<>();
	private boolean applyingChartSelection;
	private boolean closed;
	private NodeModelCache listenedCache;
	private long selectionTopologyRevision = -1L;

	TaskSelectionController(Gantt gantt, SpreadSheet sheet) {
		this.gantt = gantt;
		this.sheet = sheet;
		if (sheet != null && sheet.getSelectionModel() != null) {
			sheet.getSelectionModel().addListSelectionListener(this);
			listenedCache = sheet.getCache();
			if (listenedCache != null) {
				listenedCache.addNodeModelListener(this);
			}
			captureTableSelection();
		}
		if (gantt != null) {
			gantt.setHighlightedRowKeys(selectedKeys);
			gantt.setBarSelectionListener(this::selectFromChart);
		}
	}

	@Override
	public void valueChanged(ListSelectionEvent event) {
		if (closed || applyingChartSelection || event == null || isColumnPresentationSelection()) {
			return;
		}
		RevisionedProjectionIndex projection = projection();
		if (projection != null && selectionTopologyRevision >= 0
				&& projection.topologyRevision() != selectionTopologyRevision) {
			SwingUtilities.invokeLater(this::reconcileAfterProjectionChange);
			return;
		}
		captureTableSelection();
	}

	@Override
	public void graphicNodesCompositeEvent(CompositeCacheEvent event) {
		if (closed) {
			return;
		}
		SwingUtilities.invokeLater(this::reconcileAfterProjectionChange);
	}

	private void reconcileAfterProjectionChange() {
		if (closed || sheet == null || gantt == null) {
			return;
		}
		RevisionedProjectionIndex projection = projection();
		if (projection == null) {
			selectedKeys.clear();
			selectionTopologyRevision = -1L;
			publish();
			return;
		}
		boolean topologyChanged = projection.topologyRevision() != selectionTopologyRevision;
		selectedKeys.removeIf(key -> projection.rowForKey(key) < 0);
		if (!topologyChanged) {
			publish();
			return;
		}
		selectionTopologyRevision = projection.topologyRevision();
		if (isColumnPresentationSelection()) {
			publish();
			return;
		}
		applyingChartSelection = true;
		try {
			sheet.clearSelection();
			boolean extend = false;
			for (ProjectionRowKey key : selectedKeys) {
				int modelRow = projection.rowForKey(key);
				int viewRow = modelRow < 0 ? -1 : sheet.convertRowIndexToView(modelRow);
				if (viewRow >= 0) {
					sheet.selectTaskRowFromGantt(viewRow, extend, false);
					extend = true;
				}
			}
		} finally {
			applyingChartSelection = false;
		}
		publish();
	}

	private void captureTableSelection() {
		selectedKeys.clear();
		RevisionedProjectionIndex projection = projection();
		if (projection == null || sheet == null) {
			selectionTopologyRevision = -1L;
			publish();
			return;
		}
		selectionTopologyRevision = projection.topologyRevision();
		for (int viewRow : sheet.getSelectedRows()) {
			int modelRow = sheet.convertRowIndexToModel(viewRow);
			if (modelRow >= 0 && modelRow < projection.size()) {
				selectedKeys.add(projection.keyAt(modelRow));
			}
		}
		publish();
	}

	private void selectFromChart(Gantt.BarClick click) {
		if (closed || click == null || sheet == null) {
			return;
		}
		if (click.node() == null) {
			selectedKeys.clear();
			applyingChartSelection = true;
			try {
				sheet.clearSelection();
			} finally {
				applyingChartSelection = false;
			}
			publish();
			return;
		}
		RevisionedProjectionIndex projection = projection();
		if (projection == null) {
			return;
		}
		int modelRow = click.rowKey() == null ? projection.rowForNode(click.node())
				: projection.rowForKey(click.rowKey());
		if (modelRow >= 0 && projection.nodeAt(modelRow) != click.node()) {
			modelRow = projection.rowForNode(click.node());
		}
		if (modelRow < 0) {
			return;
		}
		ProjectionRowKey key = projection.keyAt(modelRow);
		int viewRow = sheet.convertRowIndexToView(modelRow);
		if (viewRow < 0) {
			selectedKeys.remove(key);
			publish();
			return;
		}
		applyingChartSelection = true;
		try {
			sheet.selectTaskRowFromGantt(viewRow, click.toggle(), click.extend());
		} finally {
			applyingChartSelection = false;
		}
		captureTableSelection();
	}

	private boolean isColumnPresentationSelection() {
		return sheet != null && sheet.isHeaderColumnSelectionActive();
	}

	private RevisionedProjectionIndex projection() {
		if (sheet == null || gantt == null || sheet.getCache() == null || gantt.getCache() == null) {
			return null;
		}
		return sheet.getCache().getVisibleNodes().getProjectionIndex();
	}

	private void publish() {
		if (gantt == null) {
			return;
		}
		RevisionedProjectionIndex projection = projection();
		if (projection != null) {
			selectedKeys.removeIf(key -> projection.rowForKey(key) < 0);
		}
		gantt.setHighlightedRowKeys(selectedKeys);
	}

	@Override
	public void close() {
		if (closed) {
			return;
		}
		closed = true;
		if (sheet != null && sheet.getSelectionModel() != null) {
			sheet.getSelectionModel().removeListSelectionListener(this);
		}
		if (listenedCache != null) {
			listenedCache.removeNodeModelListener(this);
			listenedCache = null;
		}
		if (gantt != null) {
			gantt.setBarSelectionListener(null);
			gantt.setHighlightedRowKeys(Set.of());
		}
		selectedKeys.clear();
	}
}
