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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Paint;
import java.awt.Rectangle;
import java.text.FieldPosition;
import java.text.Format;
import java.text.NumberFormat;
import java.text.ParsePosition;

import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.UIResource;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartRenderingInfo;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.CustomXYToolTipGenerator;
import org.jfree.data.xy.XYDataset;

import com.microproject.pm.graphic.timescale.CoordinatesConverter;
import com.microproject.pm.graphic.timescale.ScaledComponent;
import com.microproject.datatype.Money;
import com.microproject.field.Field;
import com.microproject.graphic.configuration.shape.PredefinedStroke;
import com.microproject.pm.assignment.HasTimeDistributedData;

public class TimeChartPanel extends ChartPanel implements Scrollable, ScaledComponent {
	private static final long serialVersionUID = 2034704461047717965L;

	ChartInfo chartInfo;

	JViewport viewport;

	/**
	 * @param chart
	 */
	public TimeChartPanel(ChartInfo chartInfo) {
		super(chartInfo.setChart(buildChart(chartInfo.getModel())), true);
		this.chartInfo = chartInfo;
		setMaximumDrawWidth(4000);
		setMaximumDrawHeight(1000);
	}

	// protected JScrollPane scrollPane;
	public void configureScrollPaneHeaders(JScrollPane scrollPane, JComponent rowHeader) {
		viewport = scrollPane.getViewport();
		if (viewport == null || viewport.getView() != this)
			return;

		JViewport vp = new JViewport();
		vp.setView(rowHeader);
		vp.setPreferredSize(rowHeader.getPreferredSize());
		scrollPane.setRowHeader(vp);

		scrollPane.setCorner(JScrollPane.UPPER_LEFT_CORNER, new ChartCorner(this));

		Border border = scrollPane.getBorder();
		if (border == null || border instanceof UIResource) {
			scrollPane.setBorder(UIManager.getBorder("Table.scrollPaneBorder"));
		}

		// left scale synchro
		viewport.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				updateTimeScaleComponentSize();
			}
		});

	}

	// left scale synchro
	private Dimension olddmain = null;

	public void updateTimeScaleComponentSize() {
		Dimension dmain = viewport.getViewSize();

		if (dmain.equals(olddmain))
			return;
		olddmain = dmain;
		Dimension d = chartInfo.getAxisPanel().getPreferredSize();
		d.setSize(d.getWidth(), dmain.getHeight());
		chartInfo.getAxisPanel().revalidate();
	}

	protected JFreeChart buildChart() {
		JFreeChart newChart = ChartHelper.createChart(chartInfo.getModel().getDataset(), chartInfo.isHistogram(), chartInfo.getModel()
				.getSecondDataset());
		NumberFormat numberFormat = NumberFormat.getPercentInstance(); // default
		Object[] traces = chartInfo.getTraces();
		// chartInfo.getModel().dumpDataset(traces);
		if (!chartInfo.isSimple() && (traces.length > 0 && traces[0] instanceof Field)) {
			Field field = (Field) traces[0];
			if (field.isMoney()){
				numberFormat=new NumberFormat(){

					@Override
					public StringBuffer format(double number, StringBuffer toAppendTo, FieldPosition pos) {
						return toAppendTo.append(Money.formatCurrency(number, true));
					}

					@Override
					public StringBuffer format(long number, StringBuffer toAppendTo, FieldPosition pos) {
						return format((double) number, toAppendTo, pos);
					}

					@Override
					public Number parse(String source, ParsePosition parsePosition) {
						throw new UnsupportedOperationException("Parsing chart axis values is not supported");
					}
				};
			}else{
				Format format = field.getFormat();
				if (format instanceof NumberFormat)
					numberFormat = (NumberFormat) format;
				else
					numberFormat = NumberFormat.getNumberInstance();
			}
		}

		((NumberAxis) newChart.getXYPlot().getRangeAxis()).setNumberFormatOverride(numberFormat);
		return newChart;
	}

	public static JFreeChart buildChart(ChartModel model) {
		return ChartHelper.createChart(model.getDataset(), true, model.getSecondDataset());
	}

	public void updateChart() {
		JFreeChart chart = chartInfo.getChart();
		final Object[] traces = chartInfo.getTraces();
		setChart(chart);

		Color color;
		Paint paint;
		int series = 0;
		for (int i = 0; i < traces.length; i++) {
			color = ChartHelper.getColorForField(traces[i]);

			if (traces[i] == HasTimeDistributedData.AVAILABILITY) {
				chart.getXYPlot().getRenderer(1).setSeriesPaint(0, color);
				chart.getXYPlot().getRenderer(1).setSeriesStroke(0, PredefinedStroke.LARGE_FRAMED);
				continue; // do not increment series
			}

			chart.getXYPlot().getRenderer().setSeriesPaint(series, color);

			chart.getXYPlot().getRenderer().setDefaultToolTipGenerator(new CustomXYToolTipGenerator() {
				public String generateToolTip(XYDataset data, int series, int item) {
					return traces[0] + " ";
				}

			});
			series++; // excludes availability from count
		}
		// chart.getXYPlot().addRangeMarker(new ValueMarker(1.0));

		chart.getXYPlot().getDomainAxis().setLowerBound(chartInfo.getCoord().getOrigin());
		chart.getXYPlot().getDomainAxis().setUpperBound(Math.max(chartInfo.getCoord().getEnd(), chartInfo.getCoord().toTime(viewport.getWidth())));
	}

	/**
	 * Gets space used by legend and headers if any
	 * 
	 * @return
	 */
	public double getNonPlotHeight() {
		ChartRenderingInfo info = getChartRenderingInfo();
		return info.getChartArea().getHeight() - info.getPlotInfo().getDataArea().getHeight();
	}

	public void setCoord(CoordinatesConverter coord) {
		chartInfo.setCoord(coord);
	}

	public CoordinatesConverter getCoord() {
		return chartInfo.getCoord();
	}

	protected boolean verticalScrolling = false;

	/**
	 * @return Returns the verticalScrolling.
	 */
	public boolean isVerticalScrolling() {
		return verticalScrolling;
	}

	/**
	 * @param verticalScrolling
	 *            The verticalScrolling to set.
	 */
	public void setVerticalScrolling(boolean verticalScrolling) {
		this.verticalScrolling = verticalScrolling;
	}

	public Dimension getPreferredScrollableViewportSize() {
		return getPreferredSize();
	}

	public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
		return (orientation == SwingConstants.VERTICAL) ? visibleRect.height : visibleRect.width;
	}

	public boolean getScrollableTracksViewportHeight() {
		if (getParent() instanceof JViewport) {
			if (((JViewport) getParent()).getHeight() > getPreferredSize().height)
				return true;
			else
				return !verticalScrolling;
		}
		return false;
	}

	public boolean getScrollableTracksViewportWidth() {
		if (getParent() instanceof JViewport) {
			return (((JViewport) getParent()).getWidth() > getPreferredSize().width);
		}
		return false;
	}

	public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
		if (orientation == SwingConstants.VERTICAL) {
			return 2;
		}
		return 4;
	}
	JMenuItem verticalScrollingItem;
	protected JPopupMenu createPopupMenu(boolean arg0, boolean arg1, boolean arg2, boolean arg3) {
		JPopupMenu menu = super.createPopupMenu(false, arg1, arg2, false); // hide
																			// properties
																			// and
																			// zoom
//		menu.add(new JSeparator());
//		menu.add(verticalScrollingItem = TimeChartPopupMenu.buildVerticalScrollingItem(this));
		return menu;
	}

}

