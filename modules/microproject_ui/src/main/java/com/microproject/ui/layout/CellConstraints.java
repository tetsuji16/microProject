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
package com.microproject.ui.layout;

import java.util.Objects;

/**
 * Grid position, span and alignment of one component in a {@link FormLayout}.
 * <p>
 * Columns and rows are numbered from 1. The {@code xy}, {@code xyw} and
 * {@code xywh} methods change this object and return it, so one instance can be
 * reused while adding components: {@link FormLayout} keeps its own copy of the
 * constraints of every component it lays out.
 */
public class CellConstraints implements Cloneable {

	/** Horizontal or vertical placement of a component inside its cell. */
	public static final class Alignment {
		private final String name;
		private final boolean horizontal;
		private final boolean vertical;

		private Alignment(String name, boolean horizontal, boolean vertical) {
			this.name = name;
			this.horizontal = horizontal;
			this.vertical = vertical;
		}

		@Override
		public String toString() {
			return name;
		}
	}

	/** Use the default alignment of the column or row; fill when the cell spans several. */
	public static final Alignment DEFAULT = new Alignment("default", true, true);
	public static final Alignment FILL = new Alignment("fill", true, true);
	public static final Alignment LEFT = new Alignment("left", true, false);
	public static final Alignment RIGHT = new Alignment("right", true, false);
	public static final Alignment CENTER = new Alignment("center", true, true);
	public static final Alignment TOP = new Alignment("top", false, true);
	public static final Alignment BOTTOM = new Alignment("bottom", false, true);

	int gridX = 1;
	int gridY = 1;
	int gridWidth = 1;
	int gridHeight = 1;
	Alignment hAlign = DEFAULT;
	Alignment vAlign = DEFAULT;

	public CellConstraints() {
	}

	public CellConstraints xy(int col, int row) {
		return xywh(col, row, 1, 1);
	}

	public CellConstraints xy(int col, int row, Alignment colAlign, Alignment rowAlign) {
		return xywh(col, row, 1, 1, colAlign, rowAlign);
	}

	public CellConstraints xyw(int col, int row, int colSpan) {
		return xywh(col, row, colSpan, 1, DEFAULT, DEFAULT);
	}

	public CellConstraints xyw(int col, int row, int colSpan, Alignment colAlign, Alignment rowAlign) {
		return xywh(col, row, colSpan, 1, colAlign, rowAlign);
	}

	public CellConstraints xywh(int col, int row, int colSpan, int rowSpan) {
		return xywh(col, row, colSpan, rowSpan, DEFAULT, DEFAULT);
	}

	public CellConstraints xywh(int col, int row, int colSpan, int rowSpan, Alignment colAlign, Alignment rowAlign) {
		Objects.requireNonNull(colAlign, "colAlign");
		Objects.requireNonNull(rowAlign, "rowAlign");
		if (!colAlign.horizontal)
			throw new IllegalArgumentException("Illegal horizontal alignment " + colAlign);
		if (!rowAlign.vertical)
			throw new IllegalArgumentException("Illegal vertical alignment " + rowAlign);
		gridX = col;
		gridY = row;
		gridWidth = colSpan;
		gridHeight = rowSpan;
		hAlign = colAlign;
		vAlign = rowAlign;
		return this;
	}

	void ensureInside(int columnCount, int rowCount) {
		if (gridX <= 0)
			throw new IndexOutOfBoundsException("The column index " + gridX + " must be positive.");
		if (gridX > columnCount)
			throw new IndexOutOfBoundsException(
					"The column index " + gridX + " must be less than or equal to " + columnCount + ".");
		if (gridX + gridWidth - 1 > columnCount)
			throw new IndexOutOfBoundsException(
					"The grid width " + gridWidth + " must be less than or equal to " + (columnCount - gridX + 1) + ".");
		if (gridY <= 0)
			throw new IndexOutOfBoundsException("The row index " + gridY + " must be positive.");
		if (gridY > rowCount)
			throw new IndexOutOfBoundsException(
					"The row index " + gridY + " must be less than or equal to " + rowCount + ".");
		if (gridY + gridHeight - 1 > rowCount)
			throw new IndexOutOfBoundsException(
					"The grid height " + gridHeight + " must be less than or equal to " + (rowCount - gridY + 1) + ".");
	}

	@Override
	public CellConstraints clone() {
		try {
			return (CellConstraints) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new AssertionError(e);
		}
	}

	@Override
	public String toString() {
		return "CellConstraints[x=" + gridX + ", y=" + gridY + ", w=" + gridWidth + ", h=" + gridHeight
				+ ", hAlign=" + hAlign + ", vAlign=" + vAlign + "]";
	}
}
