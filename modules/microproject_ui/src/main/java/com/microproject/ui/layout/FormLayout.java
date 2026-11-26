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

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager2;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Lays components out on a grid of columns and rows described by short text
 * specifications, as in {@code new FormLayout("right:pref, 3dlu, fill:80dlu:grow", "p, 3dlu, p")}.
 * <p>
 * Each column and row has a size ({@code pref}, {@code min}, {@code default}, a
 * constant in {@code px}, {@code dlu} or {@code pt}, or {@code max(constant;size)} /
 * {@code min(constant;size)}), an optional default alignment and an optional
 * {@code grow} weight. Sizes are computed from the components that occupy a single
 * column or row. When the container is smaller than the preferred size,
 * {@code default} columns and rows shrink towards their minimum; when it is larger,
 * the extra space goes to the growing columns and rows in proportion to their weights.
 * A component that spans several columns or rows widens the size of the container only
 * when one of the spanned columns or rows can grow; otherwise only components that
 * occupy a single column or row are measured.
 * <p>
 * Invisible components are ignored when sizes are computed.
 */
public final class FormLayout implements LayoutManager2 {
	private final List<FormSpec> columns;
	private final List<FormSpec> rows;
	private final Map<Component, CellConstraints> constraints = new LinkedHashMap<>();

	public FormLayout(String columnSpecs) {
		this(columnSpecs, "");
	}

	public FormLayout(String columnSpecs, String rowSpecs) {
		columns = Collections.unmodifiableList(FormSpec.parseList(columnSpecs, true));
		rows = Collections.unmodifiableList(FormSpec.parseList(Objects.requireNonNull(rowSpecs, "rowSpecs"), false));
	}

	public int getColumnCount() {
		return columns.size();
	}

	public int getRowCount() {
		return rows.size();
	}

	@Override
	public void addLayoutComponent(String name, Component component) {
		throw new UnsupportedOperationException("Add components with CellConstraints.");
	}

	@Override
	public void addLayoutComponent(Component component, Object constraint) {
		Objects.requireNonNull(component, "component");
		if (!(constraint instanceof CellConstraints cellConstraints))
			throw new IllegalArgumentException("FormLayout needs CellConstraints, not " + constraint);
		cellConstraints.ensureInside(getColumnCount(), getRowCount());
		constraints.put(component, cellConstraints.clone());
	}

	@Override
	public void removeLayoutComponent(Component component) {
		constraints.remove(component);
	}

	@Override
	public Dimension minimumLayoutSize(Container parent) {
		return layoutSize(parent, false);
	}

	@Override
	public Dimension preferredLayoutSize(Container parent) {
		return layoutSize(parent, true);
	}

	@Override
	public Dimension maximumLayoutSize(Container target) {
		return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
	}

	@Override
	public float getLayoutAlignmentX(Container target) {
		return 0.5f;
	}

	@Override
	public float getLayoutAlignmentY(Container target) {
		return 0.5f;
	}

	@Override
	public void invalidateLayout(Container target) {
		// Sizes are measured again on every layout pass.
	}

	@Override
	public void layoutContainer(Container parent) {
		synchronized (parent.getTreeLock()) {
			Measures measures = new Measures();
			Grid grid = grid();
			Dimension size = parent.getSize();
			Insets insets = parent.getInsets();
			int[] x = origins(parent, size.width - insets.left - insets.right, insets.left, columns, grid.columnMembers,
					measures.minimumWidth, measures.preferredWidth);
			int[] y = origins(parent, size.height - insets.top - insets.bottom, insets.top, rows, grid.rowMembers,
					measures.minimumHeight, measures.preferredHeight);
			for (Map.Entry<Component, CellConstraints> entry : constraints.entrySet())
				place(entry.getKey(), entry.getValue(), x, y, measures);
		}
	}

	private Dimension layoutSize(Container parent, boolean preferred) {
		synchronized (parent.getTreeLock()) {
			Measures measures = new Measures();
			FormSpec.Measure widthMeasure = preferred ? measures.preferredWidth : measures.minimumWidth;
			FormSpec.Measure heightMeasure = preferred ? measures.preferredHeight : measures.minimumHeight;
			Grid grid = grid();
			int[] widths = sizes(parent, columns, grid.columnMembers, measures.minimumWidth, measures.preferredWidth,
					widthMeasure);
			int[] heights = sizes(parent, rows, grid.rowMembers, measures.minimumHeight, measures.preferredHeight,
					heightMeasure);
			int[] x = cumulative(widths, 0);
			int[] y = cumulative(heights, 0);
			int totalWidth = sum(widths);
			int totalHeight = sum(heights);
			int width = totalWidth;
			int height = totalHeight;
			int[] fixedColumns = fixedSpans(columns);
			int[] fixedRows = fixedSpans(rows);
			for (Map.Entry<Component, CellConstraints> entry : constraints.entrySet()) {
				Component component = entry.getKey();
				CellConstraints cell = entry.getValue();
				if (!component.isVisible())
					continue;
				if (cell.gridWidth > 1 && cell.gridWidth > fixedColumns[cell.gridX - 1]) {
					int first = cell.gridX - 1;
					int lead = x[first];
					int trail = totalWidth - x[first + cell.gridWidth];
					width = Math.max(width, lead + widthMeasure.sizeOf(component) + trail);
				}
				if (cell.gridHeight > 1 && cell.gridHeight > fixedRows[cell.gridY - 1]) {
					int first = cell.gridY - 1;
					int lead = y[first];
					int trail = totalHeight - y[first + cell.gridHeight];
					height = Math.max(height, lead + heightMeasure.sizeOf(component) + trail);
				}
			}
			Insets insets = parent.getInsets();
			return new Dimension(width + insets.left + insets.right, height + insets.top + insets.bottom);
		}
	}

	/** Visible components that occupy exactly one column, and exactly one row. */
	private record Grid(List<List<Component>> columnMembers, List<List<Component>> rowMembers) {
	}

	private Grid grid() {
		List<List<Component>> columnMembers = new ArrayList<>(columns.size());
		for (int index = 0; index < columns.size(); index++)
			columnMembers.add(new ArrayList<>());
		List<List<Component>> rowMembers = new ArrayList<>(rows.size());
		for (int index = 0; index < rows.size(); index++)
			rowMembers.add(new ArrayList<>());
		for (Map.Entry<Component, CellConstraints> entry : constraints.entrySet()) {
			Component component = entry.getKey();
			if (!component.isVisible())
				continue;
			CellConstraints cell = entry.getValue();
			if (cell.gridWidth == 1)
				columnMembers.get(cell.gridX - 1).add(component);
			if (cell.gridHeight == 1)
				rowMembers.get(cell.gridY - 1).add(component);
		}
		return new Grid(columnMembers, rowMembers);
	}

	private static int[] sizes(Container container, List<FormSpec> specs, List<List<Component>> members,
			FormSpec.Measure minimum, FormSpec.Measure preferred, FormSpec.Measure defaultMeasure) {
		int[] result = new int[specs.size()];
		for (int index = 0; index < result.length; index++)
			result[index] = specs.get(index).maximumSize(container, members.get(index), minimum, preferred, defaultMeasure);
		return result;
	}

	private static int[] origins(Container container, int available, int offset, List<FormSpec> specs,
			List<List<Component>> members, FormSpec.Measure minimum, FormSpec.Measure preferred) {
		int[] minimumSizes = sizes(container, specs, members, minimum, preferred, minimum);
		int[] preferredSizes = sizes(container, specs, members, minimum, preferred, preferred);
		int[] compressed = compressed(specs, available, sum(minimumSizes), sum(preferredSizes), minimumSizes,
				preferredSizes);
		return cumulative(distributed(specs, available, sum(compressed), compressed), offset);
	}

	/** Shrinks {@code default} sizes in proportion when there is less space than preferred. */
	private static int[] compressed(List<FormSpec> specs, int available, int totalMinimum, int totalPreferred,
			int[] minimumSizes, int[] preferredSizes) {
		if (available < totalMinimum)
			return minimumSizes;
		if (available >= totalPreferred)
			return preferredSizes;
		double factor = (double) (totalPreferred - available) / (totalPreferred - totalMinimum);
		int[] result = new int[specs.size()];
		for (int index = 0; index < result.length; index++) {
			result[index] = preferredSizes[index];
			if (specs.get(index).size.compressible())
				result[index] -= (int) Math.round((preferredSizes[index] - minimumSizes[index]) * factor);
		}
		return result;
	}

	/** Gives the free space to growing columns or rows by weight, carrying rounding to the next one. */
	private static int[] distributed(List<FormSpec> specs, int available, int total, int[] input) {
		double free = available - total;
		if (free < 0)
			return input;
		double totalWeight = 0.0;
		for (FormSpec spec : specs)
			totalWeight += spec.resizeWeight;
		if (totalWeight == 0.0)
			return input;
		int[] result = new int[specs.size()];
		double rest = free;
		int roundedRest = (int) free;
		for (int index = 0; index < result.length; index++) {
			double weight = specs.get(index).resizeWeight;
			if (weight == FormSpec.NO_GROW) {
				result[index] = input[index];
			} else {
				double correction = rest - roundedRest;
				double extra = free * weight / totalWeight;
				int roundedExtra = (int) Math.round(extra - correction);
				result[index] = input[index] + roundedExtra;
				rest -= extra;
				roundedRest -= roundedExtra;
			}
		}
		return result;
	}

	/**
	 * For each column or row, how many consecutive ones starting there can be spanned
	 * without reaching one that grows ({@link Integer#MAX_VALUE} when none grows).
	 */
	private static int[] fixedSpans(List<FormSpec> specs) {
		int[] table = new int[specs.size()];
		int span = Integer.MAX_VALUE;
		for (int index = specs.size() - 1; index >= 0; index--) {
			if (specs.get(index).canGrow())
				span = 0;
			table[index] = span;
			if (span < Integer.MAX_VALUE)
				span++;
		}
		return table;
	}

	private void place(Component component, CellConstraints cell, int[] x, int[] y, Measures measures) {
		FormSpec column = cell.gridWidth == 1 ? columns.get(cell.gridX - 1) : null;
		FormSpec row = cell.gridHeight == 1 ? rows.get(cell.gridY - 1) : null;
		FormSpec.Align horizontal = concrete(cell.hAlign, column);
		FormSpec.Align vertical = concrete(cell.vAlign, row);
		int cellX = x[cell.gridX - 1];
		int cellY = y[cell.gridY - 1];
		int cellWidth = x[cell.gridX - 1 + cell.gridWidth] - cellX;
		int cellHeight = y[cell.gridY - 1 + cell.gridHeight] - cellY;
		int width = componentSize(component, column, cellWidth, measures.minimumWidth, measures.preferredWidth);
		int height = componentSize(component, row, cellHeight, measures.minimumHeight, measures.preferredHeight);
		component.setBounds(origin(horizontal, cellX, cellWidth, width), origin(vertical, cellY, cellHeight, height),
				horizontal == FormSpec.Align.FILL ? cellWidth : width,
				vertical == FormSpec.Align.FILL ? cellHeight : height);
	}

	private static FormSpec.Align concrete(CellConstraints.Alignment alignment, FormSpec spec) {
		if (alignment == CellConstraints.FILL)
			return FormSpec.Align.FILL;
		if (alignment == CellConstraints.LEFT)
			return FormSpec.Align.LEFT;
		if (alignment == CellConstraints.RIGHT)
			return FormSpec.Align.RIGHT;
		if (alignment == CellConstraints.CENTER)
			return FormSpec.Align.CENTER;
		if (alignment == CellConstraints.TOP)
			return FormSpec.Align.TOP;
		if (alignment == CellConstraints.BOTTOM)
			return FormSpec.Align.BOTTOM;
		return spec == null ? FormSpec.Align.FILL : spec.defaultAlignment;
	}

	private static int componentSize(Component component, FormSpec spec, int cellSize, FormSpec.Measure minimum,
			FormSpec.Measure preferred) {
		if (spec == null || spec.size == FormSpec.ComponentSize.PREFERRED)
			return preferred.sizeOf(component);
		if (spec.size == FormSpec.ComponentSize.MINIMUM)
			return minimum.sizeOf(component);
		return Math.min(cellSize, preferred.sizeOf(component));
	}

	private static int origin(FormSpec.Align alignment, int cellOrigin, int cellSize, int componentSize) {
		return switch (alignment) {
			case RIGHT, BOTTOM -> cellOrigin + cellSize - componentSize;
			case CENTER -> cellOrigin + (cellSize - componentSize) / 2;
			default -> cellOrigin;
		};
	}

	private static int[] cumulative(int[] sizes, int offset) {
		int[] origins = new int[sizes.length + 1];
		origins[0] = offset;
		for (int index = 1; index <= sizes.length; index++)
			origins[index] = origins[index - 1] + sizes[index - 1];
		return origins;
	}

	private static int sum(int[] sizes) {
		int total = 0;
		for (int size : sizes)
			total += size;
		return total;
	}

	/** Component sizes, measured once per layout pass. */
	private static final class Measures {
		private final Map<Component, Dimension> minimumSizes = new IdentityHashMap<>();
		private final Map<Component, Dimension> preferredSizes = new IdentityHashMap<>();
		final FormSpec.Measure minimumWidth = c -> minimum(c).width;
		final FormSpec.Measure minimumHeight = c -> minimum(c).height;
		final FormSpec.Measure preferredWidth = c -> preferred(c).width;
		final FormSpec.Measure preferredHeight = c -> preferred(c).height;

		private Dimension minimum(Component component) {
			return minimumSizes.computeIfAbsent(component, Component::getMinimumSize);
		}

		private Dimension preferred(Component component) {
			return preferredSizes.computeIfAbsent(component, Component::getPreferredSize);
		}
	}
}
