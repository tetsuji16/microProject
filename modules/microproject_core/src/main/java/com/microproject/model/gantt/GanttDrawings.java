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
package com.microproject.model.gantt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import com.microproject.model.task.Project;
import com.microproject.undo.GanttDrawingEdit;

/**
 * The ordered Gantt drawings of one project document.
 * <p>
 * This is transient document state (like the Network Diagram manual positions): POD keeps no
 * drawings and MPO saves them through its own sidecar entry. Every mutation that changes the list
 * is one undoable user operation; the loader replaces the list without touching the undo history.
 * Drawings are anchored by task unique id, so moving a task keeps them attached.
 */
public final class GanttDrawings {
	private final List<GanttDrawing> drawings = new ArrayList<>();
	private long nextId = 1;

	/** The drawings in painting order: later entries paint over earlier ones. */
	public List<GanttDrawing> getDrawings() {
		return List.copyOf(drawings);
	}

	public boolean isEmpty() {
		return drawings.isEmpty();
	}

	public int size() {
		return drawings.size();
	}

	public GanttDrawing find(long id) {
		for (GanttDrawing drawing : drawings)
			if (drawing.id() == id) return drawing;
		return null;
	}

	/** Adds a drawing anchored to a task bar end as one undoable operation. */
	public GanttDrawing add(Project project, GanttDrawing.Kind kind, long anchorTaskUniqueId,
			GanttDrawing.AnchorSide anchorSide, double x, double y, double width, double height,
			int lineColor, int fillColor, String text) {
		GanttDrawing drawing = new GanttDrawing(nextId, Objects.requireNonNull(kind, "kind"), anchorTaskUniqueId,
				Objects.requireNonNull(anchorSide, "anchor side"), x, y, width, height, lineColor, fillColor, text);
		nextId++;
		List<GanttDrawing> after = new ArrayList<>(drawings);
		after.add(drawing);
		apply(project, after);
		return drawing;
	}

	/** Moves a drawing to a new offset from its anchor as one undoable operation. */
	public boolean move(Project project, long id, double x, double y) {
		return replaceOne(project, id, drawing -> drawing.withPosition(x, y));
	}

	/** Changes the colors and text of a drawing as one undoable operation. */
	public boolean format(Project project, long id, int lineColor, int fillColor, String text) {
		return replaceOne(project, id, drawing -> drawing.withFormat(lineColor, fillColor, text));
	}

	/** Removes one drawing as one undoable operation. */
	public boolean remove(Project project, long id) {
		GanttDrawing existing = find(id);
		if (existing == null) return false;
		List<GanttDrawing> after = new ArrayList<>(drawings);
		after.remove(existing);
		return apply(project, after);
	}

	/**
	 * Removes every drawing anchored to a deleted task as one undoable operation. The task-deletion
	 * command posts this inside its own undo update, so one Undo restores the task and its drawings.
	 */
	public int removeForTask(Project project, long taskUniqueId) {
		List<GanttDrawing> after = new ArrayList<>(drawings.size());
		int removed = 0;
		for (GanttDrawing drawing : drawings) {
			if (drawing.anchorTaskUniqueId() == taskUniqueId) removed++;
			else after.add(drawing);
		}
		if (removed == 0) return 0;
		apply(project, after);
		return removed;
	}

	/** Removes the drawings of a task during a replayed removal, without another undo step. */
	public int removeForTaskSilently(long taskUniqueId) {
		int before = drawings.size();
		drawings.removeIf(drawing -> drawing.anchorTaskUniqueId() == taskUniqueId);
		return before - drawings.size();
	}

	/** Loads the persisted drawings; the undo history is not involved. */
	public void setDrawings(Collection<GanttDrawing> values) {
		replace(values == null ? List.of() : values);
	}

	/** Restores a drawing snapshot for the undo edit; callers use the operations above. */
	public void restore(List<GanttDrawing> values) {
		replace(values);
	}

	private boolean replaceOne(Project project, long id, java.util.function.UnaryOperator<GanttDrawing> change) {
		GanttDrawing existing = find(id);
		if (existing == null) return false;
		GanttDrawing updated = change.apply(existing);
		if (updated == null || updated.equals(existing)) return false;
		List<GanttDrawing> after = new ArrayList<>(drawings);
		after.set(after.indexOf(existing), updated);
		return apply(project, after);
	}

	private boolean apply(Project project, List<GanttDrawing> after) {
		List<GanttDrawing> before = List.copyOf(drawings);
		if (before.equals(after)) return false;
		replace(after);
		if (project != null && project.getUndoController() != null)
			project.getUndoController().getEditSupport()
					.postEdit(new GanttDrawingEdit(this, before, after));
		return true;
	}

	private void replace(Collection<GanttDrawing> values) {
		drawings.clear();
		drawings.addAll(values);
		long maxId = 0;
		for (GanttDrawing drawing : drawings)
			maxId = Math.max(maxId, drawing.id());
		// Ids never repeat, even after undoing the newest drawing.
		nextId = Math.max(nextId, maxId + 1);
	}
}
