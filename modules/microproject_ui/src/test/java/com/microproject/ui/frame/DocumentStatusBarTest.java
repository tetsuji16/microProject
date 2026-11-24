package com.microproject.ui.frame;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Calendar;
import java.text.MessageFormat;
import java.util.Arrays;

import org.junit.jupiter.api.Test;
import com.microproject.i18n.Messages;
import com.microproject.configuration.Configuration;
import com.microproject.timescale.TimeScale;

/**
 * Regression test for #202: the status bar exposes the current zoom level and
 * selected-task count. Assertions are locale-independent (labels are localized
 * through the Messages bundle).
 */
class DocumentStatusBarTest {

	@Test
	void zoomLabelShowsOneBasedScalePosition() {
		String text = DocumentStatusBar.formatZoom(2, 9);
		assertTrue(text.contains("3"), "one-based zoom position missing in: " + text);
		assertTrue(text.contains("9"), "scale count missing in: " + text);
	}

	@Test
	void zoomLabelClampsDegenerateInput() {
		String clamped = DocumentStatusBar.formatZoom(-5, 0);
		assertTrue(clamped.contains("1"), "clamped zoom missing 1/1 in: " + clamped);
		assertEquals(MessageFormat.format(Messages.getString("StatusBar.Zoom"), 9, 9),
				DocumentStatusBar.formatZoom(10, 9), "zoom index must be clamped to the available scale count");
	}

	@Test
	void semanticZoomLabelIncludesConfiguredInterval() {
		TimeScale scale = new TimeScale();
		scale.setCalendarField1(Calendar.MONTH);
		scale.setNumber1(3);
		String text = DocumentStatusBar.formatZoom(7, 9, scale);
		assertTrue(text.contains(Messages.getString("StatusBar.Interval.Quarter")));
		assertTrue(!text.contains("8/9"));
	}

	@Test
	void semanticZoomLabelUsesTheConfiguredIntervalInsteadOfInferringFromIndex() {
		TimeScale scale = new TimeScale();
		scale.setCalendarField1(Calendar.MONTH);
		scale.setNumber1(6);
		String text = DocumentStatusBar.formatZoom(5, 9, scale);
		assertTrue(text.contains(Messages.getString("StatusBar.Interval.HalfYear")));
		assertTrue(!text.contains("6/9"));
	}

	@Test
	void timescaleChoiceIncludesMeaningAndPosition() {
		TimeScale scale = new TimeScale();
		scale.setCalendarField1(Calendar.DAY_OF_MONTH);
		scale.setNumber1(3);
		scale.setCalendarField2(Calendar.MONTH);
		scale.setNumber2(1);
		String label = DocumentStatusBar.formatScaleChoice("Timescale", 3, 9, scale);
		assertEquals(MessageFormat.format(Messages.getString("StatusBar.ScaleHierarchy"),
				Messages.getString("StatusBar.Interval.Month"),
				MessageFormat.format(Messages.getString("StatusBar.Interval.Days"), 3)), label);
	}

	@Test
	void timescaleChoiceWithoutScaleUsesStableFallback() {
		assertEquals("Timescale 9", DocumentStatusBar.formatScaleChoice("Timescale", 20, 9, null));
	}

	@Test
	void configuredNineScaleChoicesShowOnlyTheirParentAndChildIntervals() {
		var scales = Configuration.getInstance().getTimeScales();
		assertEquals(9, scales.getScaleCount());
		String[] actual = DocumentStatusBar.formatScaleChoices("Timescale", scales.getScaleCount(), scales::getScale);
		String[][] intervals = {
			{"Day", "Hours", "2"},
			{"Day", "Hours", "6"},
			{"Week", "Day", null},
			{"Month", "Days", "3"},
			{"Month", "Week", null},
			{"Quarter", "Month", null},
			{"HalfYear", "Month", null},
			{"Year", "Quarter", null},
			{"Year", "HalfYear", null}
		};
		String[] expected = Arrays.stream(intervals).map(parts -> {
			String parent = Messages.getString("StatusBar.Interval." + parts[0]);
			String child = Messages.getString("StatusBar.Interval." + parts[1]);
			if (parts[2] != null) child = MessageFormat.format(child, Integer.parseInt(parts[2]));
			return MessageFormat.format(Messages.getString("StatusBar.ScaleHierarchy"), parent, child);
		}).toArray(String[]::new);
		assertEquals(Arrays.asList(expected), Arrays.asList(actual));
	}

	@Test
	void englishHierarchyUsesRequestedTitleCaseWithoutStatusPosition() {
		ResourceBundle english = ResourceBundle.getBundle("com.microproject.strings.client", Locale.ENGLISH);
		String[] keys = {"Day", "Hours", "Week", "Days", "Month", "Quarter", "HalfYear", "Year"};
		String[] values = {"Day", "{0} Hours", "Week", "{0} Days", "Month", "Quarter", "Half Year", "Year"};
		for (int i = 0; i < keys.length; i++) assertEquals(values[i], english.getString("StatusBar.Interval." + keys[i]));
		assertEquals("Zoom: {0}", english.getString("StatusBar.ZoomSemantic"));
	}

	@Test
	void timescaleHierarchyShowsConfiguredParentAndChildIntervals() {
		TimeScale scale = new TimeScale();
		scale.setCalendarField1(Calendar.MONTH);
		scale.setNumber1(3);
		scale.setCalendarField2(Calendar.YEAR);
		scale.setNumber2(1);
		assertTrue(DocumentStatusBar.formatZoom(7, 9, scale).contains(
				MessageFormat.format(Messages.getString("StatusBar.ScaleHierarchy"),
						Messages.getString("StatusBar.Interval.Year"),
						Messages.getString("StatusBar.Interval.Quarter"))));
	}

	@Test
	void hourHierarchyUsesReadablePluralLabel() {
		TimeScale scale = new TimeScale();
		scale.setCalendarField1(Calendar.HOUR_OF_DAY);
		scale.setNumber1(6);
		scale.setCalendarField2(Calendar.DAY_OF_MONTH);
		scale.setNumber2(1);
		String expected = MessageFormat.format(Messages.getString("StatusBar.ScaleHierarchy"),
				Messages.getString("StatusBar.Interval.Day"),
				MessageFormat.format(Messages.getString("StatusBar.Interval.Hours"), 6));
		assertTrue(DocumentStatusBar.formatScaleChoice("Timescale", 1, 9, scale).contains(expected));
	}

	@Test
	void semanticZoomLabelFallsBackForEmptyScaleCollection() {
		TimeScale scale = new TimeScale();
		scale.setCalendarField1(Calendar.HOUR_OF_DAY);
		scale.setNumber1(0);
		String text = DocumentStatusBar.formatZoom(10, 0, scale);
		assertTrue(!text.contains("1/1"), "semantic zoom must omit the position: " + text);
		assertTrue(text.contains(MessageFormat.format(Messages.getString("StatusBar.Interval.Hour"), 1)),
				"non-positive interval must be clamped: " + text);
	}

	@Test
	void selectionLabelShowsCount() {
		assertTrue(DocumentStatusBar.formatSelection(4).contains("4"), "selected count missing");
	}

	@Test
	void selectionLabelClampsNegativeCount() {
		assertTrue(DocumentStatusBar.formatSelection(-2).contains("0"), "negative count must clamp to 0");
	}

	@Test
	void statusBarUpdatesLabels() {
		DocumentStatusBar bar = new DocumentStatusBar();
		bar.setZoom(3, 9);
		bar.setSelectedCount(7);
		assertTrue(bar.getComponent(0).isVisible());
		assertTrue(bar.getComponent(1).isVisible());
		assertTrue(bar.getComponent(2).isVisible());
	}

	@Test
	void modeLabelResolvesLocalizedModeText() {
		DocumentStatusBar bar = new DocumentStatusBar();
		bar.setMode("StatusBar.Ready");
		assertTrue(bar.getComponent(2).isVisible());
	}

	@Test
	void japaneseStatusLabelsDescribeCountsAndZoomLevels() {
		ResourceBundle japanese = ResourceBundle.getBundle("com.microproject.strings.client", Locale.JAPANESE);
		assertEquals("選択中のタスク数: {0}", japanese.getString("StatusBar.SelectedTasks"));
		assertEquals("ズーム: {0}", japanese.getString("StatusBar.ZoomSemantic"));
		assertEquals("{0} ＞ {1}", japanese.getString("StatusBar.ScaleHierarchy"));
		assertEquals("{0}分", japanese.getString("StatusBar.Interval.Minute"));
		assertEquals("{0}時間", japanese.getString("StatusBar.Interval.Hour"));
		assertEquals("{0}日", japanese.getString("StatusBar.Interval.Days"));
		assertEquals("四半期", japanese.getString("StatusBar.Scale.Quarterly"));
		assertEquals("半年", japanese.getString("StatusBar.Scale.HalfYearly"));
		assertEquals("microProject エラー", japanese.getString("Title.ApplicationError"));
		assertTrue(japanese.getString("Message.invalidDuration").contains("3ed"));
	}
}
