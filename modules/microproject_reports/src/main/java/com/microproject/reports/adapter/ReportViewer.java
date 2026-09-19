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
package com.microproject.reports.adapter;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.swing.JRViewer;

/**
 * Subclass of JRViewer that allows changing the contents of the report while
 * reusing the viewer
 */
public class ReportViewer extends JRViewer {
	/**
	 * @param arg0
	 * @throws net.sf.jasperreports.engine.JRException
	 */
	public ReportViewer(JasperPrint arg0) throws JRException {
		super(arg0);
	}

	/**
	 * Change the report in the current viewer
	 * @param jrPrint
	 * @throws JRException
	 */
	public void changeReport(JasperPrint jrPrint) throws JRException {
		viewerContext.loadReport(jrPrint);
		viewerContext.refreshPage();
	}
	
	public void zoomIn() {
		viewerContext.setZoomRatio(clampZoomRatio(viewerContext.getZoom() * 1.1f));
	}
	public void zoomOut() {
		viewerContext.setZoomRatio(clampZoomRatio(viewerContext.getZoom() / 1.1f));
	}
	public float getZoomRatio() {
		return viewerContext.getZoom();
	}
	public void setZoomRatio(float zoomRatio) {
		viewerContext.setZoomRatio(clampZoomRatio(zoomRatio));
	}
	static float clampZoomRatio(float zoomRatio) {
		if (Float.isNaN(zoomRatio)) {
			return 1.0f;
		}
		return Math.max(0.1f, Math.min(zoomRatio, 4.0f));
	}
}
