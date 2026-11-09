/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic.frames;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.microproject.pm.graphic.frames.workspace.DefaultFrameManager;
import com.microproject.pm.graphic.model.cache.GraphicNode;
import com.microproject.pm.graphic.spreadsheet.SpreadSheet;
import com.microproject.pm.graphic.spreadsheet.common.CommonSpreadSheetModel;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.ribbon.CommandId;
import com.microproject.ribbon.RibbonCommandResult;
import com.microproject.undo.DataFactoryUndoController;
import com.microproject.util.Environment;

/**
 * Headless harness that drives a real {@link DocumentFrame} over a real
 * {@link Project}: select tasks in the task spreadsheet, run a canonical
 * document command, then inspect the model and the real undo controller.
 */
final class HeadlessDocumentFixture implements AutoCloseable {
	private final boolean previousNewLook = Environment.isNewLook();
	final DataFactoryUndoController undo = new DataFactoryUndoController();
	final Project project;
	final List<NormalTask> tasks = new ArrayList<>();
	final GraphicManager manager;
	final DocumentFrame frame;

	HeadlessDocumentFixture(String... taskNames) throws Exception {
		// New-look builds the frame's filter toolbar itself; the classic path needs initView().
		Environment.setNewLook(true);
		project = Project.createProject(ResourcePool.createRourcePool("headless-fixture", undo), undo);
		project.initialize(false, false);
		for (String name : taskNames) {
			NormalTask task = project.createScriptedTask();
			task.setName(name);
			tasks.add(task);
		}
		project.recalculate();
		AtomicReference<GraphicManager> managerRef = new AtomicReference<>();
		AtomicReference<DocumentFrame> frameRef = new AtomicReference<>();
		onEdt(() -> {
			JPanel host = new JPanel();
			GraphicManager created = new GraphicManager(host);
			// initView() installs the ribbon shell, which needs a real JFrame. The
			// frame manager is the only part of it that document commands require.
			created.setFrameManager(new DefaultFrameManager(host, new JPanel(), created));
			DocumentFrame createdFrame = created.addProjectFrame(project);
			createdFrame.activateGanttView();
			managerRef.set(created);
			frameRef.set(createdFrame);
		});
		manager = managerRef.get();
		frame = frameRef.get();
	}

	@Override
	public void close() {
		Environment.setNewLook(previousNewLook);
	}

	NormalTask task(int index) {
		return tasks.get(index);
	}

	SpreadSheet sheet() {
		return frame.getActiveSpreadSheet();
	}

	/** Selects exactly the given tasks in the task spreadsheet (model rows to view rows). */
	void select(NormalTask... selected) throws Exception {
		onEdt(() -> {
			SpreadSheet sheet = sheet();
			sheet.clearSelection();
			CommonSpreadSheetModel model = (CommonSpreadSheetModel) sheet.getModel();
			for (NormalTask task : selected) {
				int row = -1;
				for (int i = 0; i < sheet.getRowCount() && row < 0; i++) {
					GraphicNode node = model.getNode(i);
					if (node != null && node.getNode().getImpl() == task)
						row = i;
				}
				if (row < 0)
					throw new AssertionError("Task not visible in spreadsheet: " + task.getName());
				sheet.addRowSelectionInterval(row, row);
			}
		});
	}

	RibbonCommandResult run(CommandId command) throws Exception {
		AtomicReference<RibbonCommandResult> result = new AtomicReference<>();
		onEdt(() -> result.set(frame.routeTaskCommand(command)));
		return result.get();
	}

	void undo() throws Exception {
		onEdt(undo::undo);
	}

	void redo() throws Exception {
		onEdt(undo::redo);
	}

	static void onEdt(ThrowingRunnable action) throws Exception {
		Throwable[] failure = new Throwable[1];
		SwingUtilities.invokeAndWait(() -> {
			try {
				action.run();
			} catch (Throwable t) {
				failure[0] = t;
			}
		});
		if (failure[0] instanceof Exception e)
			throw e;
		if (failure[0] instanceof Error e)
			throw e;
	}

	@FunctionalInterface
	interface ThrowingRunnable {
		void run() throws Exception;
	}
}
