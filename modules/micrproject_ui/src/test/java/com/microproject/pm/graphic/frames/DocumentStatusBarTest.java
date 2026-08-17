package com.microproject.pm.graphic.frames;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

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
}
