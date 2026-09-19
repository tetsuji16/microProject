/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2012-2019 ProjectLibre, Inc.  (Previous Copyright Holder)
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
package com.microproject.print;

import javax.print.DocFlavor;
import javax.print.DocPrintJob;
import javax.print.PrintService;
import javax.print.ServiceUIFactory;
import javax.print.attribute.Attribute;
import javax.print.attribute.AttributeSet;
import javax.print.attribute.PrintServiceAttribute;
import javax.print.attribute.PrintServiceAttributeSet;
import javax.print.event.PrintServiceAttributeListener;

import com.microproject.strings.Messages;

public class PDFPrintService implements PrintService{

	public void addPrintServiceAttributeListener(
			PrintServiceAttributeListener listener) {
	}

	public DocPrintJob createPrintJob() {
		return null;
	}

	public <T extends PrintServiceAttribute> T getAttribute(Class<T> category) {
		return null;
	}

	public PrintServiceAttributeSet getAttributes() {
		return null;
	}

	public Object getDefaultAttributeValue(Class<? extends Attribute> category) {
		return null;
	}

	public String getName() {
		return Messages.getString("PageSetupDialog.PrinterPDFService");
	}

	public ServiceUIFactory getServiceUIFactory() {
		return null;
	}

	public Class<?>[] getSupportedAttributeCategories() {
		return null;
	}

	public Object getSupportedAttributeValues(
			Class<? extends Attribute> category, DocFlavor flavor,
			AttributeSet attributes) {
		return null;
	}

	public DocFlavor[] getSupportedDocFlavors() {
		return null;
	}

	public AttributeSet getUnsupportedAttributes(DocFlavor flavor,
			AttributeSet attributes) {
		return null;
	}

	public boolean isAttributeCategorySupported(
			Class<? extends Attribute> category) {
		return false;
	}

	public boolean isAttributeValueSupported(Attribute attrval,
			DocFlavor flavor, AttributeSet attributes) {
		return false;
	}

	public boolean isDocFlavorSupported(DocFlavor flavor) {
		return false;
	}

	public void removePrintServiceAttributeListener(
			PrintServiceAttributeListener listener) {
	}

}
