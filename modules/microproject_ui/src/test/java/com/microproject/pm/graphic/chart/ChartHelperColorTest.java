/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic.chart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;

import org.junit.jupiter.api.Test;

import com.microproject.pm.assignment.TimeDistributedConstants;
import com.microproject.util.Environment;

/**
 * Microsoft documents blue and red Resource Graph bars for in-capacity and
 * overallocated work, but does not publish a fixed color table for every
 * time-distributed series. Use the standard Office theme chart accents for
 * those series, as Office charts use theme colors and change them with the
 * document theme. See
 * https://support.microsoft.com/en-us/project/view-resource-workloads-and-availability-in-project-desktop
 * and https://support.microsoft.com/en-us/excel/change-the-color-or-style-of-a-chart-in-office.
 */
class ChartHelperColorTest implements TimeDistributedConstants {
	@Test
	void usesOfficeThemeColorsForChartSeriesAndResourceGraphStates() {
		assertEquals(new Color(0x4472C4), ChartHelper.getColorForField(PERCENT_ALLOC));
		assertEquals(new Color(0xC00000), ChartHelper.getColorForField(OVERALLOCATED));
		if (!Environment.getStandAlone())
			assertEquals(new Color(0xA5A5A5), ChartHelper.getColorForField(OTHER_PROJECTS));
		assertEquals(new Color(0x404040), ChartHelper.getColorForField(AVAILABILITY));
		assertEquals(new Color(0x5B9BD5), ChartHelper.getColorForField(SELECTED));
		assertEquals(new Color(0x70AD47), ChartHelper.getColorForField(THIS_PROJECT));
		assertEquals(new Color(0x4472C4), ChartHelper.getColorForField(WORK));
		assertEquals(new Color(0xED7D31), ChartHelper.getColorForField(ACTUAL_WORK));
		assertEquals(new Color(0x70AD47), ChartHelper.getColorForField(REMAINING_WORK));
		assertEquals(new Color(0xA5A5A5), ChartHelper.getColorForField(BASELINE_WORK));
		assertEquals(new Color(0x4472C4), ChartHelper.getColorForField(COST));
		assertEquals(new Color(0xED7D31), ChartHelper.getColorForField(ACTUAL_COST));
		assertEquals(new Color(0xA5A5A5), ChartHelper.getColorForField(FIXED_COST));
		assertEquals(new Color(0xFFC000), ChartHelper.getColorForField(ACTUAL_FIXED_COST));
		assertEquals(new Color(0x70AD47), ChartHelper.getColorForField(REMAINING_COST));
		assertEquals(new Color(0x7F7F7F), ChartHelper.getColorForField(BASELINE_COST));
		assertEquals(new Color(0xC00000), ChartHelper.getColorForField(ACWP));
		assertEquals(new Color(0x70AD47), ChartHelper.getColorForField(BCWP));
		assertEquals(new Color(0x4472C4), ChartHelper.getColorForField(BCWS));
	}

	@Test
	void keepsBaselineWorkAndCostSeriesOnTheMicrosoftProjectPalette() {
		Object[] workFields = { BASELINE1_WORK, BASELINE2_WORK, BASELINE3_WORK, BASELINE4_WORK, BASELINE5_WORK,
			BASELINE6_WORK, BASELINE7_WORK, BASELINE8_WORK, BASELINE9_WORK, BASELINE10_WORK };
		Object[] costFields = { BASELINE1_COST, BASELINE2_COST, BASELINE3_COST, BASELINE4_COST, BASELINE5_COST,
			BASELINE6_COST, BASELINE7_COST, BASELINE8_COST, BASELINE9_COST, BASELINE10_COST };
		for (int i = 0; i < workFields.length; i++) {
			Color workColor = ChartHelper.getColorForField(workFields[i]);
			assertEquals(workColor, ChartHelper.getColorForField(costFields[i]));
			assertEquals(new Color(new int[] { 0x4472C4, 0xED7D31, 0xA5A5A5, 0xFFC000, 0x5B9BD5,
				0x70AD47, 0x264478, 0x9E480E, 0x636363, 0x997300 }[i]), workColor);
		}
	}

	@Test
	void selectedSeriesLabelsChooseReadableTextForTheirColorBackgrounds() {
		Object[] fields = { PERCENT_ALLOC, OVERALLOCATED, WORK, ACTUAL_WORK, REMAINING_WORK, BASELINE_WORK,
			COST, ACTUAL_COST, FIXED_COST, ACTUAL_FIXED_COST, REMAINING_COST, BASELINE_COST, ACWP, BCWP, BCWS,
			BASELINE1_WORK, BASELINE2_WORK, BASELINE3_WORK, BASELINE4_WORK, BASELINE5_WORK, BASELINE6_WORK,
			BASELINE7_WORK, BASELINE8_WORK, BASELINE9_WORK, BASELINE10_WORK };
		for (Object field : fields) {
			Color background = ChartHelper.getColorForField(field);
			Color foreground = ChartLegend.readableTextColor(background);
			assertTrue(contrastRatio(background, foreground) >= 4.5, field + " should have readable selected text");
		}
	}

	private static double contrastRatio(Color first, Color second) {
		double firstLuminance = luminance(first);
		double secondLuminance = luminance(second);
		return (Math.max(firstLuminance, secondLuminance) + 0.05)
				/ (Math.min(firstLuminance, secondLuminance) + 0.05);
	}

	private static double luminance(Color color) {
		return 0.2126 * linearize(color.getRed() / 255.0)
				+ 0.7152 * linearize(color.getGreen() / 255.0)
				+ 0.0722 * linearize(color.getBlue() / 255.0);
	}

	private static double linearize(double channel) {
		return channel <= 0.04045 ? channel / 12.92 : Math.pow((channel + 0.055) / 1.055, 2.4);
	}
}
