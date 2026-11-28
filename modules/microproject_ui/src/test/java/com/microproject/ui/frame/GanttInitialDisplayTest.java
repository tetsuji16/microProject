/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.ui.frame;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Point;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JViewport;

import org.junit.jupiter.api.Test;

import com.microproject.ui.view.timescale.ScaledScrollPane;
import com.microproject.ui.view.views.GanttView;
import com.microproject.workspace.SavableToWorkspace;
import com.microproject.workspace.WorkspaceSetting;

class GanttInitialDisplayTest {
	@Test
	void initialProjectStartRemainsVisibleAfterFirstSplitResize() throws Exception {
		try (HeadlessDocumentFixture fx = new HeadlessDocumentFixture()) {
			GanttView view = fx.frame.getGanttView();
			HeadlessDocumentFixture.onEdt(() -> {
				view.reinitialize();
				view.setSize(0, 0);
				JViewport viewport = findScrollPane(view).getViewport();
				viewport.setExtentSize(new Dimension(499, 400));
				viewport.setViewSize(new Dimension(510, 400));
				view.fitProjectRangeOnInitialDisplay();
			});
			HeadlessDocumentFixture.onEdt(() -> { });
			HeadlessDocumentFixture.onEdt(() -> {
				findScrollPane(view).getViewport().setExtentSize(new Dimension(252, 400));
				view.setSize(1010, 600);
				view.doLayout();
			});
			HeadlessDocumentFixture.onEdt(() -> {
				view.doLayout();
				findScrollPane(view).doLayout();
			});
			HeadlessDocumentFixture.onEdt(() -> { });
			HeadlessDocumentFixture.onEdt(() -> {
				var visible = view.getVisibleDateRange();
				assertTrue(visible.start() <= fx.project.getStart() && visible.end() >= fx.project.getEnd(),
					"project range must remain in the initial viewport; visible=" + visible);
			});
		}
	}

	@Test
	void pendingInitialFitDoesNotOverrideRestoredWorkspace() throws Exception {
		try (HeadlessDocumentFixture fx = new HeadlessDocumentFixture()) {
			GanttView ganttView = fx.frame.getGanttView();
			ScaledScrollPane pane = findScrollPane(ganttView);
			JViewport viewport = pane.getViewport();
			long start = fx.project.getStart();
			long finish = fx.project.getEnd();
			int taskCount = fx.project.getTasks().size();
			boolean canUndo = fx.undo.canUndo();
			AtomicReference<Point> restoredPosition = new AtomicReference<>();

			HeadlessDocumentFixture.onEdt(() -> {
				viewport.setExtentSize(new Dimension(499, 400));
				viewport.setViewSize(new Dimension(4_000, 400));
				viewport.setViewPosition(new Point(620, 0));
				WorkspaceSetting saved = ganttView.createWorkspace(SavableToWorkspace.PERSIST);
				viewport.setViewPosition(new Point(0, 0));
				ganttView.fitProjectRangeOnInitialDisplay();
				ganttView.restoreWorkspace(saved, SavableToWorkspace.PERSIST);
				restoredPosition.set(viewport.getViewPosition());
			});
			HeadlessDocumentFixture.onEdt(() -> { });

			HeadlessDocumentFixture.onEdt(() -> {
				assertAll(
					() -> assertEquals(restoredPosition.get(), viewport.getViewPosition(),
						"the queued initial fit must not replace the restored viewport position"),
					() -> assertEquals(start, fx.project.getStart()),
					() -> assertEquals(finish, fx.project.getEnd()),
					() -> assertEquals(taskCount, fx.project.getTasks().size()),
					() -> assertEquals(canUndo, fx.undo.canUndo()));
			});
		}
	}

	@Test
	void pendingInitialFitDoesNotOverrideExplicitZoomAndManualScroll() throws Exception {
		try (HeadlessDocumentFixture fx = new HeadlessDocumentFixture()) {
			GanttView ganttView = fx.frame.getGanttView();
			ScaledScrollPane pane = findScrollPane(ganttView);
			JViewport viewport = pane.getViewport();
			long start = fx.project.getStart();
			long finish = fx.project.getEnd();
			int taskCount = fx.project.getTasks().size();
			boolean canUndo = fx.undo.canUndo();
			double zoomBefore = fx.frame.getCoord().getWidth();
			AtomicReference<Point> manualPosition = new AtomicReference<>();
			AtomicReference<Double> manualZoom = new AtomicReference<>();

			HeadlessDocumentFixture.onEdt(() -> {
				viewport.setExtentSize(new Dimension(499, 400));
				viewport.setViewSize(new Dimension(4_000, 400));
				ganttView.fitProjectRangeOnInitialDisplay();
				fx.frame.doZoomIn();
				viewport.setViewPosition(new Point(720, 0));
				manualPosition.set(viewport.getViewPosition());
				manualZoom.set(fx.frame.getCoord().getWidth());
			});
			HeadlessDocumentFixture.onEdt(() -> { });

			HeadlessDocumentFixture.onEdt(() -> {
				assertAll(
					() -> assertEquals(manualPosition.get(), viewport.getViewPosition(),
						"the queued initial fit must not replace a later manual scroll"),
					() -> assertEquals(manualZoom.get(), fx.frame.getCoord().getWidth(),
						"the queued initial fit must not replace the explicit zoom"),
					() -> assertTrue(manualZoom.get() > zoomBefore,
						"the explicit zoom operation must have changed the timeline scale"),
					() -> assertEquals(start, fx.project.getStart()),
					() -> assertEquals(finish, fx.project.getEnd()),
					() -> assertEquals(taskCount, fx.project.getTasks().size()),
					() -> assertEquals(canUndo, fx.undo.canUndo()));
			});
		}
	}

	@Test
	void pendingInitialFitDoesNotOverrideScrollbarDrag() throws Exception {
		try (HeadlessDocumentFixture fx = new HeadlessDocumentFixture()) {
			GanttView view = fx.frame.getGanttView();
			ScaledScrollPane pane = findScrollPane(view);
			AtomicReference<Point> position = new AtomicReference<>();
			HeadlessDocumentFixture.onEdt(() -> {
				JViewport viewport = pane.getViewport();
				viewport.setExtentSize(new Dimension(499, 400));
				viewport.setViewSize(new Dimension(4_000, 400));
				view.fitProjectRangeOnInitialDisplay();
				pane.getHorizontalScrollBar().setValueIsAdjusting(true);
				pane.getHorizontalScrollBar().setValue(720);
				pane.getHorizontalScrollBar().setValueIsAdjusting(false);
				position.set(viewport.getViewPosition());
				assertTrue(position.get().x > 0, "the scrollbar drag must move the timeline");
			});
			HeadlessDocumentFixture.onEdt(() -> { });
			HeadlessDocumentFixture.onEdt(() -> assertEquals(position.get(), pane.getViewport().getViewPosition(),
				"the queued initial fit must not replace a scrollbar drag"));
		}
	}

	private static ScaledScrollPane findScrollPane(Container container) {
		for (Component child : container.getComponents()) {
			if (child instanceof ScaledScrollPane pane)
				return pane;
			if (child instanceof Container nested) {
				ScaledScrollPane pane = findScrollPane(nested);
				if (pane != null)
					return pane;
			}
		}
		return null;
	}
}
