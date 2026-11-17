/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic.chart;

import java.awt.Color;
import java.awt.Font;
import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYTextAnnotation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.renderer.xy.XYAreaRenderer;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYStepAreaRenderer;
import org.jfree.chart.renderer.xy.XYStepRenderer;
import org.jfree.chart.ui.TextAnchor;
import org.jfree.data.xy.XYIntervalSeries;
import org.jfree.data.xy.XYIntervalSeriesCollection;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import com.microproject.datatype.Money;
import com.microproject.graphic.configuration.shape.PredefinedStroke;
import com.microproject.pm.graphic.chart.ResourceGraphData.ValueKind;
import com.microproject.pm.graphic.chart.ResourceGraphSettings.BarStyle;
import com.microproject.pm.graphic.chart.ResourceGraphSettings.ShowAs;
import com.microproject.util.FlatUiSupport;

/** Builds the JFreeChart of a Resource Graph: one dataset and renderer per visible bar-style item. */
public final class ResourceGraphChart {
	private static final Color AVAILABILITY_COLOR = new Color(0x404040);
	/** More value labels than this would only overprint each other. */
	private static final int MAX_LABELS = 400;
	/** Width given to the final interval when no earlier interval tells it (one day, in milliseconds). */
	private static final double DEFAULT_INTERVAL = 86_400_000D;

	private ResourceGraphChart() {
	}

	public static JFreeChart build(ResourceGraphData data, ResourceGraphSettings settings) {
		NumberAxis domain = new NumberAxis(null);
		domain.setAutoRangeIncludesZero(false);
		NumberAxis range = new NumberAxis(null);
		range.setNumberFormatOverride(axisFormat(data.getKind()));
		XYPlot plot = new XYPlot(null, domain, range, null);
		plot.setOrientation(PlotOrientation.VERTICAL);
		plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);
		int index = 0;
		// filled shapes first, then line-only shapes, so a fill cannot paint over a line drawn before it
		java.util.List<ResourceGraphData.Series> ordered = new java.util.ArrayList<>(data.getSeries());
		ordered.sort(java.util.Comparator.comparing(series -> isLineOnly(settings.getStyle(series.item()).showAs())));
		java.util.List<ResourceGraphData.Series> bars = ordered.stream()
			.filter(series -> settings.getStyle(series.item()).showAs() == ShowAs.BAR && !series.isEmpty()).toList();
		for (ResourceGraphData.Series series : ordered) {
			BarStyle style = settings.getStyle(series.item());
			if (style.showAs() == ShowAs.DO_NOT_SHOW || series.isEmpty())
				continue;
			XYItemRenderer renderer;
			if (style.showAs() == ShowAs.BAR) {
				// a stacked bar spans its own lower and upper bound, so hiding another item cannot expose it;
				// other shapes trace the stacked upper bound with their own renderer
				XYIntervalSeries values = new XYIntervalSeries(series.item().name(), false, true);
				int barIndex = bars.indexOf(series);
				int barCount = bars.size();
				double overlap = settings.getPercentBarOverlap() / 100D;
				for (int i = 0; i < series.x().length; i++)
					values.add(series.x()[i], barStart(series.x(), i, barIndex, barCount, overlap),
						barEnd(series.x(), i, barIndex, barCount, overlap), series.y()[i],
						series.base() == null ? 0D : series.base()[i], series.y()[i]);
				XYIntervalSeriesCollection collection = new XYIntervalSeriesCollection();
				collection.addSeries(values);
				plot.setDataset(index, collection);
				renderer = barRenderer();
			} else {
				XYSeries values = new XYSeries(series.item().name(), false, true);
				for (int i = 0; i < series.x().length; i++)
					values.add(series.x()[i], series.y()[i]);
				// the last interval needs its end boundary, or a step area has no width there
				int last = series.x().length - 1;
				values.add(intervalEnd(series.x(), last), series.y()[last]);
				plot.setDataset(index, new XYSeriesCollection(values));
				renderer = renderer(style.showAs());
			}
			renderer.setSeriesPaint(0, style.pattern().paint(new Color(style.rgb())));
			if (style.showAs() == ShowAs.STEP_LINE || style.showAs() == ShowAs.LINE)
				renderer.setSeriesStroke(0, PredefinedStroke.LARGE_FRAMED);
			plot.setRenderer(index, renderer);
			index++;
		}
		double[] lineX = data.getAvailabilityX();
		if (settings.isShowAvailabilityLine() && lineX.length > 0) {
			XYSeries line = new XYSeries("availability", false, true);
			for (int i = 0; i < lineX.length; i++)
				line.add(lineX[i], data.getAvailabilityY()[i]);
			plot.setDataset(index, new XYSeriesCollection(line));
			StandardXYItemRenderer renderer = new StandardXYItemRenderer(StandardXYItemRenderer.LINES);
			renderer.setSeriesPaint(0, AVAILABILITY_COLOR);
			renderer.setSeriesStroke(0, PredefinedStroke.LARGE_FRAMED);
			plot.setRenderer(index, renderer);
		}
		ChartHelper.removeAxisAndInsets(plot);
		plot.setRangeGridlinesVisible(settings.isHorizontalGridlines());
		plot.setDomainGridlinesVisible(settings.isVerticalGridlines());
		applyTextStyle(plot, range, settings);
		if (settings.isShowValues())
			addValueLabels(plot, data, settings);
		JFreeChart chart = new JFreeChart(null, JFreeChart.DEFAULT_TITLE_FONT, plot, false);
		chart.setAntiAlias(false);
		chart.setBorderVisible(false);
		return chart;
	}

	/** End of interval {@code i}: the start of the next one, or one more interval width after the last start. */
	static double intervalEnd(double[] starts, int i) {
		if (i + 1 < starts.length)
			return starts[i + 1];
		return i > 0 ? starts[i] + (starts[i] - starts[i - 1]) : starts[i] + DEFAULT_INTERVAL;
	}

	private static XYItemRenderer barRenderer() {
		XYBarRenderer renderer = new XYBarRenderer(0D);
		renderer.setUseYInterval(true);
		renderer.setShadowVisible(false);
		renderer.setDrawBarOutline(false);
		renderer.setBarPainter(new StandardXYBarPainter());
		return renderer;
	}

	private static double barStart(double[] starts, int i, int series, int count, double overlap) {
		double left = starts[i];
		double right = intervalEnd(starts, i);
		double width = right - left;
		double center = left + width / 2D;
		double slotCenter = left + width * (series + 0.5D) / Math.max(1, count);
		double barCenter = slotCenter + (center - slotCenter) * overlap;
		double barWidth = width * (1D / Math.max(1, count) + (1D - 1D / Math.max(1, count)) * overlap);
		return barCenter - barWidth / 2D;
	}

	private static double barEnd(double[] starts, int i, int series, int count, double overlap) {
		double left = starts[i];
		double right = intervalEnd(starts, i);
		double width = right - left;
		double center = left + width / 2D;
		double slotCenter = left + width * (series + 0.5D) / Math.max(1, count);
		double barCenter = slotCenter + (center - slotCenter) * overlap;
		double barWidth = width * (1D / Math.max(1, count) + (1D - 1D / Math.max(1, count)) * overlap);
		return barCenter + barWidth / 2D;
	}

	static boolean isLineOnly(ShowAs showAs) {
		return showAs == ShowAs.LINE || showAs == ShowAs.STEP || showAs == ShowAs.STEP_LINE;
	}

	static XYItemRenderer renderer(ShowAs showAs) {
		return switch (showAs) {
			case AREA -> new XYAreaRenderer(XYAreaRenderer.AREA);
			case STEP, STEP_LINE -> new XYStepRenderer();
			case LINE -> new StandardXYItemRenderer(StandardXYItemRenderer.LINES);
			default -> new XYStepAreaRenderer(XYStepAreaRenderer.AREA);
		};
	}

	private static void applyTextStyle(XYPlot plot, NumberAxis range, ResourceGraphSettings settings) {
		Font base = range.getTickLabelFont();
		int size = settings.getTextStyle().size() > 0 ? settings.getTextStyle().size() : base.getSize();
		range.setTickLabelFont(base.deriveFont(settings.getTextStyle().bold() ? Font.BOLD : Font.PLAIN, (float) size));
		range.setTickLabelPaint(FlatUiSupport.labelForeground());
	}

	/** The value labels of the Values command, one per interval of each visible item. */
	private static void addValueLabels(XYPlot plot, ResourceGraphData data, ResourceGraphSettings settings) {
		NumberFormat format = labelFormat(data.getKind());
		int size = settings.getTextStyle().size() > 0 ? settings.getTextStyle().size() : 9;
		Font font = new Font(Font.SANS_SERIF, settings.getTextStyle().bold() ? Font.BOLD : Font.PLAIN, size);
		int count = 0;
		// filled shapes first, then line-only shapes, so a fill cannot paint over a line drawn before it
		java.util.List<ResourceGraphData.Series> ordered = new java.util.ArrayList<>(data.getSeries());
		ordered.sort(java.util.Comparator.comparing(series -> isLineOnly(settings.getStyle(series.item()).showAs())));
		for (ResourceGraphData.Series series : ordered) {
			BarStyle style = settings.getStyle(series.item());
			if (style.showAs() == ShowAs.DO_NOT_SHOW)
				continue;
			for (int i = 0; i < series.x().length && count < MAX_LABELS; i++) {
				if (series.raw()[i] == 0D)
					continue;
				XYTextAnnotation label = new XYTextAnnotation(format.format(series.raw()[i]), series.x()[i], series.y()[i]);
				label.setFont(font);
				label.setPaint(FlatUiSupport.labelForeground());
				label.setTextAnchor(TextAnchor.BOTTOM_LEFT);
				plot.addAnnotation(label);
				count++;
			}
		}
	}

	/** Number format of the value axis for a kind of value. */
	public static NumberFormat axisFormat(ValueKind kind) {
		return switch (kind) {
			case HOURS -> hours();
			case MONEY -> money();
			default -> NumberFormat.getPercentInstance();
		};
	}

	private static NumberFormat labelFormat(ValueKind kind) {
		return kind == ValueKind.UNITS ? NumberFormat.getPercentInstance() : axisFormat(kind);
	}

	private static NumberFormat hours() {
		return new DecimalFormat("#,##0.##");
	}

	private static NumberFormat money() {
		return new NumberFormat() {
			private static final long serialVersionUID = 1L;

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
	}
}
