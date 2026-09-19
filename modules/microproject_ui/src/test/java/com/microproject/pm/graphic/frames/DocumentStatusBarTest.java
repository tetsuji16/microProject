package com.microproject.pm.graphic.frames;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Calendar;
import java.text.MessageFormat;

import org.junit.jupiter.api.Test;
import com.microproject.strings.Messages;
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
		assertTrue(text.contains(Messages.getString("StatusBar.Scale.Quarterly")));
		assertTrue(text.contains("8/9"));
	}

	@Test
	void semanticZoomLabelUsesMspCompatibleScaleNames() {
		TimeScale scale = new TimeScale();
		scale.setCalendarField1(Calendar.MONTH);
		scale.setNumber1(6);
		assertTrue(DocumentStatusBar.formatZoom(8, 9, scale).contains(Messages.getString("StatusBar.Scale.HalfYearly")));
		assertTrue(DocumentStatusBar.formatZoom(5, 9, scale).contains(Messages.getString("StatusBar.Scale.BiMonthly")));
		assertTrue(DocumentStatusBar.formatZoom(9, 10, scale).contains(Messages.getString("StatusBar.Scale.Yearly")));
	}

	@Test
	void semanticZoomLabelFallsBackForEmptyScaleCollection() {
		TimeScale scale = new TimeScale();
		scale.setCalendarField1(Calendar.HOUR_OF_DAY);
		scale.setNumber1(0);
		String text = DocumentStatusBar.formatZoom(10, 0, scale);
		assertTrue(text.contains("1/1"), "empty scale collection must remain displayable: " + text);
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
		assertEquals("ズーム: {0} ({1}/{2} 段階)", japanese.getString("StatusBar.ZoomSemantic"));
		assertEquals("{0}時間", japanese.getString("StatusBar.Interval.Hour"));
		assertEquals("{0}日", japanese.getString("StatusBar.Interval.Days"));
		assertEquals("四半期", japanese.getString("StatusBar.Scale.Quarterly"));
		assertEquals("半年", japanese.getString("StatusBar.Scale.HalfYearly"));
		assertEquals("microProject エラー", japanese.getString("Title.ProjectLibreError"));
		assertTrue(japanese.getString("Message.invalidDuration").contains("3ed"));
	}
}
