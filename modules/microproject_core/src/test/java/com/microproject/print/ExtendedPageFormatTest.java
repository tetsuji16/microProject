package com.microproject.print;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Proxy;

import javax.print.PrintService;
import javax.print.attribute.standard.MediaSize;
import javax.print.attribute.standard.MediaSizeName;
import javax.print.attribute.standard.MediaPrintableArea;

import org.junit.jupiter.api.Test;

class ExtendedPageFormatTest {
	@Test
	void printableAreaIsClampedToPrinterSupportedArea() {
		MediaPrintableArea printerLimit = new MediaPrintableArea(10, 12, 100, 120, MediaSize.MM);
		PrintService printService = (PrintService) Proxy.newProxyInstance(PrintService.class.getClassLoader(),
				new Class<?>[] { PrintService.class }, (proxy, method, arguments) ->
						"getSupportedAttributeValues".equals(method.getName())
								? new MediaPrintableArea[] { printerLimit }
								: null);
		MediaPrintableArea requested = new MediaPrintableArea(15, 18, 130, 150, MediaSize.MM);

		MediaPrintableArea adapted = ExtendedPageFormat.adaptMediaPrintableArea(requested, printService,
				MediaSizeName.ISO_A4);

		assertEquals(10, adapted.getX(MediaSize.MM), 0.01);
		assertEquals(12, adapted.getY(MediaSize.MM), 0.01);
		assertEquals(100, adapted.getWidth(MediaSize.MM), 0.01);
		assertEquals(120, adapted.getHeight(MediaSize.MM), 0.01);
	}
}
