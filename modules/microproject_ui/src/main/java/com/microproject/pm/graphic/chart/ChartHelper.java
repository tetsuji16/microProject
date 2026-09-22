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
import java.util.Map;

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
	private static final Color OFFICE_BLUE = new Color(0x4472C4);
	private static final Color OFFICE_ORANGE = new Color(0xED7D31);
	private static final Color OFFICE_GRAY = new Color(0xA5A5A5);
	private static final Color OFFICE_GOLD = new Color(0xFFC000);
	private static final Color OFFICE_LIGHT_BLUE = new Color(0x5B9BD5);
	private static final Color OFFICE_GREEN = new Color(0x70AD47);
	private static final Color OFFICE_RED = new Color(0xC00000);
	private static final Color[] OFFICE_SERIES = {
		OFFICE_BLUE, OFFICE_ORANGE, OFFICE_GRAY, OFFICE_GOLD, OFFICE_LIGHT_BLUE, OFFICE_GREEN,
		new Color(0x264478), new Color(0x9E480E), new Color(0x636363), new Color(0x997300)
	};

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

	private static volatile Map<Object, Color> map;

	private static Map<Object, Color> getMap() {
		Map<Object, Color> result = map;
		if (result == null) {
			synchronized (ChartHelper.class) {
				result = map;
				if (result == null) {
					Map<Object, Color> m = new HashMap<>();
					m.put(PERCENT_ALLOC, OFFICE_BLUE);
					m.put(OVERALLOCATED, OFFICE_RED);
					if (!Environment.getStandAlone()) m.put(OTHER_PROJECTS, OFFICE_GRAY);
					m.put(AVAILABILITY, new Color(0x404040));
					m.put(SELECTED, OFFICE_LIGHT_BLUE);
					m.put(THIS_PROJECT, OFFICE_GREEN);
					m.put(WORK, OFFICE_BLUE);
					m.put(ACTUAL_WORK, OFFICE_ORANGE);
					m.put(REMAINING_WORK, OFFICE_GREEN);
					m.put(BASELINE_WORK, OFFICE_GRAY);
					m.put(COST, OFFICE_BLUE);
					m.put(ACTUAL_COST, OFFICE_ORANGE);
					m.put(FIXED_COST, OFFICE_GRAY);
					m.put(ACTUAL_FIXED_COST, OFFICE_GOLD);
					m.put(REMAINING_COST, OFFICE_GREEN);
					m.put(BASELINE_COST, new Color(0x7F7F7F));
					m.put(ACWP, OFFICE_RED);
					m.put(BCWP, OFFICE_GREEN);
					m.put(BCWS, OFFICE_BLUE);
					m.put(BASELINE1_WORK, OFFICE_SERIES[0]);
					m.put(BASELINE2_WORK, OFFICE_SERIES[1]);
					m.put(BASELINE3_WORK, OFFICE_SERIES[2]);
					m.put(BASELINE4_WORK, OFFICE_SERIES[3]);
					m.put(BASELINE5_WORK, OFFICE_SERIES[4]);
					m.put(BASELINE6_WORK, OFFICE_SERIES[5]);
					m.put(BASELINE7_WORK, OFFICE_SERIES[6]);
					m.put(BASELINE8_WORK, OFFICE_SERIES[7]);
					m.put(BASELINE9_WORK, OFFICE_SERIES[8]);
					m.put(BASELINE10_WORK, OFFICE_SERIES[9]);
					m.put(BASELINE1_COST, OFFICE_SERIES[0]);
					m.put(BASELINE2_COST, OFFICE_SERIES[1]);
					m.put(BASELINE3_COST, OFFICE_SERIES[2]);
					m.put(BASELINE4_COST, OFFICE_SERIES[3]);
					m.put(BASELINE5_COST, OFFICE_SERIES[4]);
					m.put(BASELINE6_COST, OFFICE_SERIES[5]);
					m.put(BASELINE7_COST, OFFICE_SERIES[6]);
					m.put(BASELINE8_COST, OFFICE_SERIES[7]);
					m.put(BASELINE9_COST, OFFICE_SERIES[8]);
					m.put(BASELINE10_COST, OFFICE_SERIES[9]);
					result = Map.copyOf(m);
					map = result;
				}
			}
		}
		return result;
	}

	public static Color getColorForField(Object field) {
		Color result = (Color) getMap().get(field);
		if (result == null)
			result = FlatUiSupport.labelForeground();
		return result;

	}

}

