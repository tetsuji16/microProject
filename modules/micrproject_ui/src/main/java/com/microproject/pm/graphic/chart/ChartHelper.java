/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2012-2019 ProjectLibre, Inc.  (Previous Copyright Holder)
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
package com.microproject.pm.graphic.chart;

/**
 * 
 */

import java.awt.Color;
import java.util.HashMap;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.SeriesRenderingOrder;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYStepAreaRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.chart.ui.RectangleInsets;

import com.microproject.graphic.configuration.shape.Colors;
import com.microproject.pm.assignment.TimeDistributedConstants;
import com.microproject.util.Environment;
import com.microproject.util.FlatUiSupport;

/**
 * A simple demonstration application showing how to create a vertical bar
 * chart.
 * 
 */
public class ChartHelper implements TimeDistributedConstants {
	public static final int BOTTOM_INSET = 7;// replace domain legend

	public static JFreeChart createChart(final XYDataset dataset, boolean bar,final XYDataset secondDataset) {
		
		JFreeChart chart;
		if (secondDataset != null)
			chart = createBarLineChart(dataset,secondDataset);
		else
			chart = bar ? createBarChart(dataset) : createLineChart(dataset);
		chart.setAntiAlias(false);// faster
		chart.setBorderVisible(false);
		return chart;
	}


	/**
	 * Creates a new chart.
	 * 
	 * @param dataset
	 *            the dataset.
	 * 
	 * @return The chart.
	 */
	public static JFreeChart createBarChart(final XYDataset dataset) {
		ValueAxis domainAxis = null;
		NumberAxis axis = new NumberAxis(null);
		axis.setAutoRangeIncludesZero(false);
		domainAxis = axis;

		ValueAxis valueAxis = new NumberAxis(null);
		XYItemRenderer barRenderer = new XYStepAreaRenderer(XYStepAreaRenderer.AREA, new StandardXYToolTipGenerator(), null);

		XYPlot plot = new XYPlot(dataset, domainAxis, valueAxis, barRenderer);
		plot.setOrientation(PlotOrientation.VERTICAL);
		JFreeChart chart = new JFreeChart(null, JFreeChart.DEFAULT_TITLE_FONT, plot, false);
		removeAxisAndInsets(chart);
		return chart;
	}

	public static JFreeChart createBarLineChart(final XYDataset barDataset, final XYDataset lineDataset) {
		JFreeChart chart =  createBarChart(barDataset);
		XYItemRenderer lineRenderer = new StandardXYItemRenderer(StandardXYItemRenderer.LINES);
		chart.getXYPlot().setDataset(1,lineDataset);
		chart.getXYPlot().setRenderer(1,lineRenderer);
		chart.getXYPlot().setSeriesRenderingOrder(SeriesRenderingOrder.FORWARD);
		chart.getXYPlot().setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD); // draw the line after the bar so it's superimposed
		return chart;
	}


	public static JFreeChart createLineChart(final XYDataset dataset) {
		NumberAxis xAxis = new NumberAxis(null);
		xAxis.setAutoRangeIncludesZero(false);
		NumberAxis yAxis = new NumberAxis(null);
		XYItemRenderer renderer = new StandardXYItemRenderer(StandardXYItemRenderer.LINES);
		XYPlot plot = new XYPlot(dataset, xAxis, yAxis, renderer);
		plot.setOrientation(PlotOrientation.VERTICAL);
		renderer.setDefaultToolTipGenerator(new StandardXYToolTipGenerator());
		JFreeChart chart = new JFreeChart(null, JFreeChart.DEFAULT_TITLE_FONT, plot, false);
		removeAxisAndInsets(chart);
		return chart;
	}

	public static void removeAxisAndInsets(JFreeChart chart) {
		XYPlot plot = chart.getXYPlot();
		removeAxisAndInsets(plot);
	}

	public static void removeAxisAndInsets(XYPlot plot) {
		plot.getRangeAxis().setVisible(false);
		plot.getDomainAxis().setVisible(false);
		plot.setDomainGridlinesVisible(false);
		plot.setInsets(new RectangleInsets(0, 0, BOTTOM_INSET, 0));
	}

	private static HashMap map = null;

	private static HashMap getMap() {
		if (map == null) {
			map = new HashMap();
			map.put(PERCENT_ALLOC, Colors.RED);
			map.put(OVERALLOCATED, Colors.RED);
			if (!Environment.getStandAlone()) map.put(OTHER_PROJECTS, Colors.GRAY);
			map.put(AVAILABILITY, Colors.BLACK);
			map.put(SELECTED, Colors.BLUE);
			map.put(THIS_PROJECT, Colors.GREEN);
			map.put(WORK, Colors.RED);
			map.put(ACTUAL_WORK, Colors.BROWN);
			map.put(REMAINING_WORK, Colors.PURPLE);
			map.put(BASELINE_WORK, Colors.DARK_SLATE_GRAY);
			map.put(COST, Colors.RED);
			map.put(ACTUAL_COST, Colors.BROWN);
			map.put(FIXED_COST, Colors.CORAL);
			map.put(ACTUAL_FIXED_COST, Colors.BURLY_WOOD);
			map.put(REMAINING_COST, Colors.PURPLE);
			map.put(BASELINE_COST, Colors.DARK_SLATE_GRAY);
			map.put(ACWP, Colors.RED);
			map.put(BCWP, Colors.OLIVE_DRAB);
			map.put(BCWS, Colors.GOLD);
			map.put(BASELINE1_WORK, Colors.MAGENTA);
			map.put(BASELINE2_WORK, Colors.KHAKI);
			map.put(BASELINE3_WORK, Colors.TAN);
			map.put(BASELINE4_WORK, Colors.NAVY);
			map.put(BASELINE5_WORK, Colors.TURQUOISE);
			map.put(BASELINE6_WORK, Colors.VIOLET);
			map.put(BASELINE7_WORK, Colors.MAROON);
			map.put(BASELINE8_WORK, Colors.SALMON);
			map.put(BASELINE9_WORK, Colors.ORANGE);
			map.put(BASELINE10_WORK, Colors.CYAN);
			map.put(BASELINE1_COST, Colors.MAGENTA);
			map.put(BASELINE2_COST, Colors.KHAKI);
			map.put(BASELINE3_COST, Colors.TAN);
			map.put(BASELINE4_COST, Colors.NAVY);
			map.put(BASELINE5_COST, Colors.TURQUOISE);
			map.put(BASELINE6_COST, Colors.VIOLET);
			map.put(BASELINE7_COST, Colors.MAROON);
			map.put(BASELINE8_COST, Colors.SALMON);
			map.put(BASELINE9_COST, Colors.ORANGE);
			map.put(BASELINE10_COST, Colors.CYAN);
		}
		return map;
	}

	public static Color getColorForField(Object field) {
		Color result = (Color) getMap().get(field);
		if (result == null)
			result = FlatUiSupport.labelForeground();
		return result;

	}

}

