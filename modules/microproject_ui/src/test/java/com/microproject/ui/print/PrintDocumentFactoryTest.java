/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *******************************************************************************/
package com.microproject.ui.print;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javax.print.DocFlavor;
import javax.print.DocPrintJob;
import javax.print.PrintService;
import javax.print.ServiceUIFactory;
import javax.print.attribute.Attribute;
import javax.print.attribute.AttributeSet;
import javax.print.attribute.PrintServiceAttribute;
import javax.print.attribute.PrintServiceAttributeSet;
import javax.print.event.PrintServiceAttributeListener;

import org.junit.jupiter.api.Test;
import com.microproject.print.PDFPrintService;

class PrintDocumentFactoryTest {
	@Test
	void getInstanceReturnsTheSharedFactory() {
		PrintDocumentFactory factory = PrintDocumentFactory.getInstance();

		assertSame(factory, PrintDocumentFactory.getInstance());
	}

	/** Issue #1334: printers resolved by the caller are used as-is, without another lookup. */
	@Test
	void resolvedServicesKeepThePdfServiceFirstAndSkipTheLookup() throws Exception {
		PrintService first = new FakePrintService("Printer A");
		PrintService second = new FakePrintService("Printer B");
		AtomicInteger lookups = new AtomicInteger();
		try (AutoCloseable ignored = PrintDocument.overridePrintServiceLookup(
				() -> { lookups.incrementAndGet(); return new PrintService[0]; },
				() -> { lookups.incrementAndGet(); return null; })) {
			PrintDocument document = new TestDocument(new PrintService[] { first, second }, second);
			PrintService[] services = document.getPrintServices();

			assertEquals(0, lookups.get(), "resolved services must not trigger the AWT lookup");
			assertEquals(3, services.length);
			assertInstanceOf(PDFPrintService.class, services[0], "the PDF service stays first");
			assertSame(first, services[1]);
			assertSame(second, services[2]);
		}
	}

	@Test
	void theResolvingConstructorStillReadsTheSystemPrinters() throws Exception {
		PrintService only = new FakePrintService("Only printer");
		AtomicInteger lookups = new AtomicInteger();
		try (AutoCloseable ignored = PrintDocument.overridePrintServiceLookup(
				() -> { lookups.incrementAndGet(); return new PrintService[] { only }; },
				() -> { lookups.incrementAndGet(); return only; })) {
			PrintDocument document = new TestDocument();

			assertEquals(2, lookups.get());
			assertEquals(2, document.getPrintServices().length);
			assertSame(only, document.getPrintServices()[1]);
		}
	}

	@Test
	void anEmptyResolvedListFallsBackToTheResolvedDefaultPrinter() {
		PrintService only = new FakePrintService("Fallback");
		PrintDocument document = new TestDocument(new PrintService[0], only);

		assertEquals(2, document.getPrintServices().length);
		assertSame(only, document.getPrintServices()[1]);
	}

	private static final class TestDocument extends PrintDocument {
		TestDocument() {
			super(null, false, false, false, true);
		}

		TestDocument(PrintService[] services, PrintService defaultService) {
			super(null, false, false, false, true, services, defaultService);
		}

		@Override public void print() { }
		@Override public int getNumberOfPages() { return 1; }
		@Override public PageFormat getPageFormat(int pageIndex) { return getPageFormat(); }
		@Override public Printable getPrintable(int pageIndex) { return null; }
	}

	private static final class FakePrintService implements PrintService {
		private final String name;

		FakePrintService(String name) {
			this.name = name;
		}

		@Override public String getName() { return name; }
		@Override public DocFlavor[] getSupportedDocFlavors() { return new DocFlavor[0]; }
		@Override public boolean isDocFlavorSupported(DocFlavor flavor) { return false; }
		@Override public Class<?>[] getSupportedAttributeCategories() { return new Class<?>[0]; }
		@Override public boolean isAttributeCategorySupported(Class<? extends Attribute> category) { return false; }
		@Override public Object getDefaultAttributeValue(Class<? extends Attribute> category) { return null; }
		@Override public Object getSupportedAttributeValues(Class<? extends Attribute> category, DocFlavor flavor,
				AttributeSet attributes) { return null; }
		@Override public boolean isAttributeValueSupported(Attribute attribute, DocFlavor flavor, AttributeSet attributes) {
			return false;
		}
		@Override public AttributeSet getUnsupportedAttributes(DocFlavor flavor, AttributeSet attributes) { return null; }
		@Override public PrintServiceAttributeSet getAttributes() { return null; }
		@Override public <T extends PrintServiceAttribute> T getAttribute(Class<T> category) { return null; }
		@Override public ServiceUIFactory getServiceUIFactory() { return null; }
		@Override public DocPrintJob createPrintJob() { return null; }
		@Override public void addPrintServiceAttributeListener(PrintServiceAttributeListener listener) { }
		@Override public void removePrintServiceAttributeListener(PrintServiceAttributeListener listener) { }
	}
}
