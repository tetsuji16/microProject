/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.ui.testsupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Window;
import java.util.List;
import java.util.Map;

import javax.swing.JViewport;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import com.microproject.job.JobQueue;
import com.microproject.model.resource.ResourcePool;
import com.microproject.model.task.Project;
import com.microproject.session.SessionFactory;
import com.microproject.ui.frame.DocumentFrame;
import com.microproject.ui.frame.GraphicManager;
import com.microproject.ui.frame.MainRibbonFrame;
import com.microproject.ui.view.gantt.Gantt;
import com.microproject.ui.view.views.GanttView;
import com.microproject.undo.DataFactoryUndoController;
import com.microproject.util.Environment;

/** The state snapshot reports the Gantt chart's left edge exactly as the viewport shows it. */
class GanttLeftEdgeSnapshotGuiAcceptanceTest {
	private static final long DAY = 24L * 60 * 60 * 1000;

	@Test
	void snapshotLeftEdgeMatchesTheViewportAfterScrollToDate() throws Exception {
		boolean ribbonUi = Environment.isRibbonUI();
		boolean newLook = Environment.isNewLook();
		JobQueue previousQueue = SessionFactory.getInstance().getLocalSession().getJobQueue();
		Environment.setRibbonUI(true);
		Environment.setNewLook(true);
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("gantt-left-edge", undo), undo);
		project.initialize(false, false);
		project.createScriptedTask().setName("Left edge task 1");
		project.createScriptedTask().setName("Left edge task 2");
		MainRibbonFrame[] frame = new MainRibbonFrame[1];
		GraphicManager[] manager = new GraphicManager[1];
		try {
			SwingUtilities.invokeAndWait(() -> {
				frame[0] = new MainRibbonFrame("Gantt left edge acceptance", null);
				manager[0] = new GraphicManager(frame[0]);
				SessionFactory.getInstance().setJobQueue(manager[0].getJobQueue());
				frame[0].setGraphicManager(manager[0]);
				manager[0].initView();
				manager[0].addProjectFrame(project);
				frame[0].setSize(1600, 700);
				frame[0].setLocationByPlatform(true);
				frame[0].setVisible(true);
			});
			GuiRunRecorder.attach(manager[0]);
			GuiAcceptanceSupport.await(() -> frame[0].isShowing() && manager[0].getCurrentFrame() != null
				&& manager[0].getCurrentFrame().getActiveTopView() instanceof GanttView view && view.getGantt() != null
				&& view.getGantt().getParent() instanceof JViewport, "the Gantt chart did not show");

			long[] expected = new long[2];
			GuiAcceptanceSupport.runOnEdtWithTimeout(() -> {
				DocumentFrame document = manager[0].getCurrentFrame();
				Gantt gantt = ((GanttView) document.getActiveTopView()).getGantt();
				assertTrue(gantt.scrollToDate(project.getStart() + 30 * DAY));
			}, "Gantt scroll");
			// the chart may re-lay out after the scroll; read the edges once the viewport has settled
			long[] previous = {-1};
			GuiAcceptanceSupport.await(() -> {
				try {
					GuiAcceptanceSupport.runOnEdtWithTimeout(() -> {
						Gantt gantt = ((GanttView) manager[0].getCurrentFrame().getActiveTopView()).getGantt();
						JViewport viewport = (JViewport) gantt.getParent();
						int x = viewport.getViewPosition().x;
						expected[0] = (long) gantt.getCoord().toTime(x);
						expected[1] = (long) gantt.getCoord().toTime(x + viewport.getExtentSize().width);
					}, "Gantt edge read");
					Thread.sleep(200);
				} catch (Exception failure) {
					throw new IllegalStateException(failure);
				}
				boolean settled = expected[0] == previous[0];
				previous[0] = expected[0];
				return settled;
			}, "the Gantt viewport did not settle");

			GuiRunRecorder.step("scrolled", Map.of("days", 30));
			GuiRunRecorder.snapshot("after-scroll");

			Map<String, Object> gantt = castMap(GuiStateSnapshot.capture(manager[0]).get("gantt"));
			assertNotNull(gantt, "the snapshot has no gantt section");
			assertEquals(expected[0], ((Number) gantt.get("leftEdgeMillis")).longValue());
			assertEquals(ProjectStateSnapshot.isoUtc(expected[0]), gantt.get("leftEdge"));
			assertEquals(expected[1], ((Number) gantt.get("rightEdgeMillis")).longValue());

			Map<String, Object> stepGantt = castMap(GuiRunRecorder.current().events("step").get(0).get("gantt"));
			assertNotNull(stepGantt, "the step has no gantt item");
			assertEquals(expected[0], ((Number) stepGantt.get("leftEdgeMillis")).longValue());
			assertEquals(ProjectStateSnapshot.isoUtc(expected[1]), stepGantt.get("rightEdge"));
			List<Map<String, Object>> tasks = ProjectStateSnapshot.tasks(project);
			assertEquals(2, tasks.size());
		} finally {
			SwingUtilities.invokeAndWait(() -> {
				for (Window window : Window.getWindows()) {
					if (window != frame[0] && window.isShowing()) window.dispose();
				}
				if (manager[0] != null) manager[0].cleanUp();
				if (frame[0] != null) frame[0].dispose();
			});
			SessionFactory.getInstance().setJobQueue(previousQueue);
			Environment.setRibbonUI(ribbonUi);
			Environment.setNewLook(newLook);
		}
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> castMap(Object value) {
		return (Map<String, Object>) value;
	}
}
