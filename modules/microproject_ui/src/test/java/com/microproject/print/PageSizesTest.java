/*
 * MIT License
 * Copyright (c) 2026 microProject
 */
package com.microproject.print;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.print.attribute.standard.MediaSize;
import javax.print.attribute.standard.MediaSizeName;

import org.junit.jupiter.api.Test;

class PageSizesTest {
	@Test
	void pageSizeQueriesShareFormatResolutionAndPreserveFallbacks() {
		PageSizes pageSizes = new PageSizes();
		PageSizes.Format paper = new PageSizes.Format("A4",
			new MediaSize(210f, 297f, MediaSize.MM, MediaSizeName.ISO_A4));
		PageSizes.Format custom = new PageSizes.Format("Custom", PageSizes.CUSTOM);
		PageSizes.Format big = new PageSizes.Format("Single page", PageSizes.BIG_PAGE);

		assertSame(paper.getDimension(), pageSizes.getPageDimension(paper));
		assertNull(pageSizes.getPageDimension(null));
		assertNull(pageSizes.getPageDimension(new Object()));
		assertTrue(pageSizes.isCustomPageSize(custom));
		assertFalse(pageSizes.isCustomPageSize(big));
		assertFalse(pageSizes.isCustomPageSize(new Object()));
		assertTrue(pageSizes.isBigPageSize(big));
		assertFalse(pageSizes.isBigPageSize(custom));
		assertFalse(pageSizes.isBigPageSize(null));
		assertEquals(297d, pageSizes.getPageDimension(paper).getHeight());
	}
}
