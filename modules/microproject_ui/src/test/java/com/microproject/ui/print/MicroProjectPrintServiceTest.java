/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.ui.print;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Rectangle;
import java.lang.reflect.Proxy;

import org.junit.jupiter.api.Test;

import com.microproject.ui.view.graph.GraphParams;

class MicroProjectPrintServiceTest {
	@Test
	void nonSpreadsheetWidthUsesDrawingBounds() {
		GraphParams params = graphParams(new Rectangle(0, 0, 80, 20));

		double ratio = ExtendedPrintServiceFactory.getExtendedPrintService().getWRatio(1, 80, params);

		assertEquals(1.0, ratio);
	}

	@Test
	void horizontalRatioRejectsNullAndNonSpreadsheetParams() {
		ExtendedPrintService service = ExtendedPrintServiceFactory.getExtendedPrintService();

		assertEquals(-1.0, service.getHRatio(1, 80, null));
		assertEquals(-1.0, service.getHRatio(1, 80, graphParams(new Rectangle())));
	}

	private static GraphParams graphParams(Rectangle drawingBounds) {
		return (GraphParams) Proxy.newProxyInstance(GraphParams.class.getClassLoader(),
				new Class<?>[] { GraphParams.class }, (proxy, method, args) -> {
					if (method.getName().equals("getDrawingBounds")) return drawingBounds;
					if (method.getName().equals("isLeftPartVisible")) return true;
					return null;
				});
	}
}
