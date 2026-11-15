/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic.chart;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.TexturePaint;
import java.util.EnumMap;
import java.util.Map;
import java.util.Properties;

import com.microproject.dialog.CalendarViewSettings.TextStyle;
import com.microproject.dialog.ViewSettingsState;

/**
 * Display choices of the Resource Graph that the Resource Graph Format tab edits: which value the
 * graph plots, how each of the four bar-style items is drawn, value labels, the availability line,
 * gridlines and the text style. The state is saved with the view workspace as a plain text blob.
 */
public final class ResourceGraphSettings implements ViewSettingsState {
	/** The values the Graph command can plot, in the order of the Microsoft Project gallery. */
	public enum GraphType {
		PEAK_UNITS, WORK, CUMULATIVE_WORK, OVERALLOCATION, PERCENT_ALLOCATION, REMAINING_AVAILABILITY,
		COST, CUMULATIVE_COST, WORK_AVAILABILITY, UNIT_AVAILABILITY;

		/** Resource bundle key of the display name. */
		public String messageKey() {
			return "ResourceGraph.type." + name();
		}
	}

	/** How one bar-style item is drawn (the ShowAs setting of ResourceGraphBarStyles). */
	public enum ShowAs {
		/** Stepped filled bars, one per timescale interval. */
		BAR,
		/** Filled area joining the interval values. */
		AREA,
		/** Thin stepped outline. */
		STEP,
		/** Line joining the interval values. */
		LINE,
		/** Heavy stepped outline. */
		STEP_LINE,
		DO_NOT_SHOW
	}

	/** Fill patterns of a bar style. */
	public enum Pattern {
		SOLID, DOTS, HORIZONTAL, VERTICAL, DIAGONAL_UP, DIAGONAL_DOWN, CROSS_HATCH;

		/** The paint of this pattern in the given colour. */
		public Paint paint(Color color) {
			if (this == SOLID)
				return color;
			int size = 8;
			BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = image.createGraphics();
			try {
				g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 70));
				g.fillRect(0, 0, size, size);
				g.setColor(color);
				switch (this) {
					case DOTS -> {
						g.fillRect(1, 1, 2, 2);
						g.fillRect(5, 5, 2, 2);
					}
					case HORIZONTAL -> g.fillRect(0, 3, size, 2);
					case VERTICAL -> g.fillRect(3, 0, 2, size);
					case DIAGONAL_UP -> {
						for (int i = -size; i < size; i++)
							g.drawLine(i, size, i + size, 0);
					}
					case DIAGONAL_DOWN -> {
						for (int i = -size; i < size; i++)
							g.drawLine(i, 0, i + size, size);
					}
					case CROSS_HATCH -> {
						g.fillRect(0, 3, size, 2);
						g.fillRect(3, 0, 2, size);
					}
					default -> { }
				}
			} finally {
				g.dispose();
			}
			return new TexturePaint(image, new Rectangle(0, 0, size, size));
		}
	}

	/** The four items of ResourceGraphBarStyles: Overallocated/Allocated for the filtered resources and for the resource. */
	public enum Item {
		OVERALLOCATED_FILTERED(ShowAs.LINE, 0xC00000),
		ALLOCATED_FILTERED(ShowAs.BAR, 0x70AD47),
		OVERALLOCATED_RESOURCE(ShowAs.DO_NOT_SHOW, 0xC00000),
		ALLOCATED_RESOURCE(ShowAs.BAR, 0x5B9BD5);

		private final BarStyle defaultStyle;

		Item(ShowAs showAs, int rgb) {
			defaultStyle = new BarStyle(showAs, rgb, Pattern.SOLID);
		}

		public BarStyle defaultStyle() {
			return defaultStyle;
		}

		/** Resource bundle key of the display name. */
		public String messageKey() {
			return "ResourceGraph.item." + name();
		}
	}

	/** Shape, colour and pattern of one item. */
	public record BarStyle(ShowAs showAs, int rgb, Pattern pattern) {
		public BarStyle {
			if (showAs == null)
				showAs = ShowAs.BAR;
			if (pattern == null)
				pattern = Pattern.SOLID;
			rgb &= 0xFFFFFF;
		}
	}

	private GraphType graphType = GraphType.PEAK_UNITS;
	private final Map<Item, BarStyle> styles = new EnumMap<>(Item.class);
	private boolean showValues;
	private boolean showAvailabilityLine = true;
	private boolean horizontalGridlines = true;
	private boolean verticalGridlines;
	private TextStyle textStyle = TextStyle.DEFAULT;

	public GraphType getGraphType() { return graphType; }
	public void setGraphType(GraphType type) { graphType = type == null ? GraphType.PEAK_UNITS : type; }

	public BarStyle getStyle(Item item) {
		BarStyle style = styles.get(item);
		return style == null ? item.defaultStyle() : style;
	}

	public void setStyle(Item item, BarStyle style) {
		if (style == null || style.equals(item.defaultStyle()))
			styles.remove(item);
		else
			styles.put(item, style);
	}

	/** Returns every item to its default bar style. */
	public void resetBarStyles() {
		styles.clear();
	}

	public boolean isShowValues() { return showValues; }
	public void setShowValues(boolean value) { showValues = value; }
	public boolean isShowAvailabilityLine() { return showAvailabilityLine; }
	public void setShowAvailabilityLine(boolean value) { showAvailabilityLine = value; }
	public boolean isHorizontalGridlines() { return horizontalGridlines; }
	public void setHorizontalGridlines(boolean value) { horizontalGridlines = value; }
	public boolean isVerticalGridlines() { return verticalGridlines; }
	public void setVerticalGridlines(boolean value) { verticalGridlines = value; }
	public boolean isGridlines() { return horizontalGridlines || verticalGridlines; }
	public TextStyle getTextStyle() { return textStyle; }
	public void setTextStyle(TextStyle style) { textStyle = style == null ? TextStyle.DEFAULT : style; }

	@Override
	public String saveSettings() {
		Properties p = new Properties();
		if (graphType != GraphType.PEAK_UNITS) p.setProperty("graph", graphType.name());
		for (Map.Entry<Item, BarStyle> e : styles.entrySet()) {
			BarStyle s = e.getValue();
			p.setProperty("style." + e.getKey().name(), s.showAs().name() + "," + s.rgb() + "," + s.pattern().name());
		}
		if (showValues) p.setProperty("showValues", "true");
		if (!showAvailabilityLine) p.setProperty("availabilityLine", "false");
		if (!horizontalGridlines) p.setProperty("horizontalGridlines", "false");
		if (verticalGridlines) p.setProperty("verticalGridlines", "true");
		if (!TextStyle.DEFAULT.equals(textStyle)) p.setProperty("text", textStyle.size() + "," + textStyle.bold());
		return Codec.write(p);
	}

	@Override
	public void restoreSettings(String state) {
		Properties p = Codec.read(state);
		graphType = GraphType.PEAK_UNITS;
		try {
			String type = p.getProperty("graph");
			if (type != null)
				graphType = GraphType.valueOf(type.trim());
		} catch (IllegalArgumentException malformed) {
			// keep the default graph
		}
		styles.clear();
		for (Item item : Item.values()) {
			String value = p.getProperty("style." + item.name());
			if (value == null)
				continue;
			String[] parts = value.split(",");
			try {
				if (parts.length == 3)
					setStyle(item, new BarStyle(ShowAs.valueOf(parts[0].trim()), Integer.parseInt(parts[1].trim()), Pattern.valueOf(parts[2].trim())));
			} catch (IllegalArgumentException malformed) {
				// keep the default style of this item
			}
		}
		showValues = Codec.bool(p, "showValues", false);
		showAvailabilityLine = Codec.bool(p, "availabilityLine", true);
		horizontalGridlines = Codec.bool(p, "horizontalGridlines", true);
		verticalGridlines = Codec.bool(p, "verticalGridlines", false);
		textStyle = TextStyle.DEFAULT;
		String text = p.getProperty("text");
		if (text != null) {
			String[] parts = text.split(",");
			try {
				if (parts.length == 2) textStyle = new TextStyle(Integer.parseInt(parts[0].trim()), Boolean.parseBoolean(parts[1].trim()));
			} catch (NumberFormatException malformed) {
				// keep the default style
			}
		}
	}
}
