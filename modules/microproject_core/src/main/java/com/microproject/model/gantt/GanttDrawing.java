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

import java.util.Objects;

/**
 * One drawing placed on the Gantt chart and anchored to a task bar end.
 * <p>
 * The coordinates are presentation-independent offsets in points: {@code x} runs along the
 * timescale from the anchored bar end and {@code y} runs down from the top of the chart. This type
 * holds no drawing code; the Gantt renderer owns presentation. Colors are ARGB values and a fill
 * color of {@code 0} means "no fill".
 */
public final class GanttDrawing {
	/** Shape kinds this first release supports. */
	public enum Kind { LINE, RECTANGLE, TEXT_BOX }

	/** Which end of the anchored Gantt bar the drawing is fixed to. */
	public enum AnchorSide { START, END }

	private final long id;
	private final Kind kind;
	private final long anchorTaskUniqueId;
	private final AnchorSide anchorSide;
	private final double x;
	private final double y;
	private final double width;
	private final double height;
	private final int lineColor;
	private final int fillColor;
	private final String text;

	public GanttDrawing(long id, Kind kind, long anchorTaskUniqueId, AnchorSide anchorSide, double x, double y,
			double width, double height, int lineColor, int fillColor, String text) {
		Objects.requireNonNull(kind, "kind");
		Objects.requireNonNull(anchorSide, "anchor side");
		if (id < 1) throw new IllegalArgumentException("Invalid Gantt drawing id");
		if (width < 0 || height < 0) throw new IllegalArgumentException("Invalid Gantt drawing size");
		this.id = id;
		this.kind = kind;
		this.anchorTaskUniqueId = anchorTaskUniqueId;
		this.anchorSide = anchorSide;
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
		this.lineColor = lineColor;
		this.fillColor = fillColor;
		this.text = text == null ? "" : text;
	}

	public long id() { return id; }
	public Kind kind() { return kind; }
	public long anchorTaskUniqueId() { return anchorTaskUniqueId; }
	public AnchorSide anchorSide() { return anchorSide; }
	public double x() { return x; }
	public double y() { return y; }
	public double width() { return width; }
	public double height() { return height; }
	public int lineColor() { return lineColor; }
	public int fillColor() { return fillColor; }
	public String text() { return text; }

	/** This drawing moved to a new offset from its anchor. */
	public GanttDrawing withPosition(double x, double y) {
		return new GanttDrawing(id, kind, anchorTaskUniqueId, anchorSide, x, y, width, height, lineColor, fillColor, text);
	}

	/** This drawing with new line and fill colors and text. */
	public GanttDrawing withFormat(int lineColor, int fillColor, String text) {
		return new GanttDrawing(id, kind, anchorTaskUniqueId, anchorSide, x, y, width, height, lineColor, fillColor, text);
	}

	@Override public boolean equals(Object other) {
		if (this == other) return true;
		if (!(other instanceof GanttDrawing value)) return false;
		return id == value.id && kind == value.kind && anchorTaskUniqueId == value.anchorTaskUniqueId
				&& anchorSide == value.anchorSide && Double.compare(x, value.x) == 0 && Double.compare(y, value.y) == 0
				&& Double.compare(width, value.width) == 0 && Double.compare(height, value.height) == 0
				&& lineColor == value.lineColor && fillColor == value.fillColor && text.equals(value.text);
	}

	@Override public int hashCode() {
		return Objects.hash(id, kind, anchorTaskUniqueId, anchorSide, x, y, width, height, lineColor, fillColor, text);
	}

	@Override public String toString() {
		return "GanttDrawing[" + id + " " + kind + " task " + anchorTaskUniqueId + " " + anchorSide + "]";
	}
}
