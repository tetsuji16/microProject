/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.print;

import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import javax.swing.JFrame;
import javax.swing.WindowConstants;

import com.microproject.reports.render.ReportPreview;
import com.microproject.reports.render.ReportPrintable;
import com.microproject.strings.Messages;
import com.microproject.util.Alert;

/** Standard printer-job bridge for a report's Pageable pages. */
public final class ReportPageable extends GraphPageable {
	private final ReportPrintable report;
	public ReportPageable(ReportPrintable report, boolean printOnly) {
		super(report, printOnly);
		this.report = report;
	}
	@Override public int getColumnCount() { return 1; }
	@Override public void print() {
		try {
			PrinterJob job = PrinterJob.getPrinterJob();
			javax.print.PrintService service = getPrintService();
			if (service != null) job.setPrintService(service);
			job.setPageable(this);
			if (!job.printDialog()) return;
			job.print();
		} catch (PrinterException e) { Alert.error(e.getMessage()); }
	}
	@Override public void preview() {
		JFrame frame = new JFrame(Messages.getString("PrintPreviewFrame.PrintPreview"));
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		frame.setContentPane(new ReportPreview(report));
		frame.setSize(1000, 800);
		frame.setLocationByPlatform(true);
		frame.setVisible(true);
	}
	@Override public void printWithDefault(java.awt.Graphics graphics, int page) throws PrinterException {
		report.print(graphics, report.getPageFormat(page), page);
	}
}
