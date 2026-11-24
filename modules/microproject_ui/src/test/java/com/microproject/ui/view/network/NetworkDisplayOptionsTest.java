/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.ui.view.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;

import org.junit.jupiter.api.Test;

import com.microproject.datatype.Duration;
import com.microproject.datatype.DurationFormat;
import com.microproject.model.dependency.DependencyType;
import com.microproject.ui.view.link_routing.DefaultNetworkLinkRouting;
import com.microproject.ui.view.link_routing.StraightNetworkLinkRouting;

/** The pure decisions behind the Network Diagram Format tab. */
class NetworkDisplayOptionsTest {
	private static int segments(GeneralPath path) {
		int count = 0;
		for (PathIterator it = path.getPathIterator(null); !it.isDone(); it.next()) count++;
		return count;
	}

	@Test
	void linkLabelShowsTypeAndNonzeroLag() {
		assertEquals("FS", NetworkDisplayOptions.linkLabel(DependencyType.Kind.FS, 0));
		long twoDays = 2 * 24 * 60 * 60 * 1000L;
		String plus = NetworkDisplayOptions.linkLabel(DependencyType.Kind.SS, twoDays);
		assertTrue(plus.startsWith("SS+"), plus);
		assertEquals("SS+" + DurationFormat.getInstance().format(new Duration(twoDays)), plus);
		String lead = NetworkDisplayOptions.linkLabel(DependencyType.Kind.FS, -twoDays);
		assertTrue(lead.startsWith("FS-"), lead);
	}

	@Test
	void lagLabelUsesTheProjectsHoursPerDay() {
		long eightHours = 8 * 60 * 60 * 1000L;
		com.microproject.datatype.DurationDefaults fourHourDays = new com.microproject.datatype.DurationDefaults(240.0D, 1200.0D, 20.0D);
		String twoDays = NetworkDisplayOptions.linkLabel(DependencyType.Kind.FS, eightHours, fourHourDays);
		assertTrue(twoDays.startsWith("FS+2"), twoDays);
		String oneDay = NetworkDisplayOptions.linkLabel(DependencyType.Kind.FS, eightHours,
			com.microproject.datatype.DurationDefaults.PRODUCT_DEFAULTS);
		assertTrue(oneDay.startsWith("FS+1"), oneDay);
		assertTrue(NetworkDisplayOptions.linkLabel(DependencyType.Kind.FS, -eightHours, fourHourDays).startsWith("FS-2"));
	}

	@Test
	void progressMarkIsSlashInProgressAndCrossWhenComplete() {
		assertEquals(NetworkDisplayOptions.ProgressMark.NONE, NetworkDisplayOptions.progressMark(0.0));
		assertEquals(NetworkDisplayOptions.ProgressMark.SLASH, NetworkDisplayOptions.progressMark(0.4));
		assertEquals(NetworkDisplayOptions.ProgressMark.CROSS, NetworkDisplayOptions.progressMark(1.0));
	}

	@Test
	void straightRoutingDrawsOneSegmentWhereOrthogonalDrawsSeveral() {
		GeneralPath straight = new GeneralPath();
		new StraightNetworkLinkRouting().routePath(straight, 10, 10, 110, 90, 50, DependencyType.Kind.FS);
		assertEquals(2, segments(straight), "move to the start, one line to the end");
		assertEquals(110.0, straight.getCurrentPoint().getX(), 0.001);
		assertEquals(90.0, straight.getCurrentPoint().getY(), 0.001);
		GeneralPath orthogonal = new GeneralPath();
		new DefaultNetworkLinkRouting().routePath(orthogonal, 10, 10, 110, 90, 50, DependencyType.Kind.FS);
		assertTrue(segments(orthogonal) > 2, "right-angled links bend");
	}

	@Test
	void collapsedBoxesShrinkAndSpacingIsClamped() {
		NetworkDisplayOptions options = new NetworkDisplayOptions();
		assertEquals(new Dimension(100, 50), options.boxSize(100, 50));
		options.setCollapsed(true);
		assertEquals(new Dimension(40, 20), options.boxSize(100, 50));
		options.setBoxSpacing(-5);
		assertEquals(0, options.getBoxSpacing());
		options.setBoxSpacing(100000);
		assertEquals(NetworkDisplayOptions.MAX_SPACING, options.getBoxSpacing());
	}

	@Test
	void styleOutlinesAndPersistedTextRoundTrip() {
		NetworkDisplayOptions options = new NetworkDisplayOptions();
		options.setStyle(NetworkDisplayOptions.BoxType.CRITICAL, new NetworkDisplayOptions.BoxStyle(0xC0392B, NetworkDisplayOptions.BoxShape.DIAMOND));
		options.setStyle(NetworkDisplayOptions.BoxType.MILESTONE, new NetworkDisplayOptions.BoxStyle(null, NetworkDisplayOptions.BoxShape.ROUNDED));
		String text = options.encodeStyles();
		NetworkDisplayOptions restored = new NetworkDisplayOptions();
		restored.decodeStyles(text);
		assertEquals(options.getStyle(NetworkDisplayOptions.BoxType.CRITICAL), restored.getStyle(NetworkDisplayOptions.BoxType.CRITICAL));
		assertEquals(new Color(0xC0392B), restored.getStyle(NetworkDisplayOptions.BoxType.CRITICAL).color());
		assertEquals(NetworkDisplayOptions.BoxShape.ROUNDED, restored.getStyle(NetworkDisplayOptions.BoxType.MILESTONE).shape());
		assertEquals(NetworkDisplayOptions.BoxStyle.DEFAULT, restored.getStyle(NetworkDisplayOptions.BoxType.NORMAL));
		options.setStyle(NetworkDisplayOptions.BoxType.CRITICAL, NetworkDisplayOptions.BoxStyle.DEFAULT);
		options.setStyle(NetworkDisplayOptions.BoxType.MILESTONE, NetworkDisplayOptions.BoxStyle.DEFAULT);
		assertFalse(options.hasStyles());
		Rectangle2D bounds = new Rectangle2D.Double(0, 0, 40, 20);
		assertTrue(NetworkDisplayOptions.outline(NetworkDisplayOptions.BoxShape.DIAMOND, bounds).contains(20, 10));
		assertFalse(NetworkDisplayOptions.outline(NetworkDisplayOptions.BoxShape.DIAMOND, bounds).contains(1, 1), "a diamond leaves the corners empty");
		assertTrue(NetworkDisplayOptions.outline(NetworkDisplayOptions.BoxShape.RECTANGLE, bounds).contains(1, 1));
	}
}
