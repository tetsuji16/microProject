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
package com.microproject.export;

import java.awt.Component;
import java.awt.Graphics2D;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import com.formdev.flatlaf.util.SystemFileChooser;
import com.formdev.flatlaf.util.SystemFileChooser.FileNameExtensionFilter;

import com.lowagie.text.Document;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfWriter;
import com.microproject.print.ExtendedPageFormat;
import com.microproject.print.GraphPageable;
import com.microproject.print.ViewPrintable;
import com.microproject.job.Job;
import com.microproject.job.JobQueue;
import com.microproject.job.JobRunnable;
import com.microproject.session.SessionFactory;
import com.microproject.util.PdfExportUtil;

public class PDFExport {
	public static void export(final GraphPageable pageable,Component parentComponent) throws IOException{
		final File file=chooseFile(pageable.getRenderer().getProject().getName(),parentComponent);
		final JobQueue jobQueue=SessionFactory.getInstance().getJobQueue();
		Job job=new Job(jobQueue,"PDF Export","Exporting PDF...",true,parentComponent);
		job.addRunnable(new JobRunnable("PDF Export",1.0f){
			public Object run() throws Exception{
				Document document = new Document();
				PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(file));
				pageable.update();
				int pageCount = pageable.getNumberOfPages();
				if (pageCount>0){
					ViewPrintable printable=pageable.getSafePrintable();
					ExtendedPageFormat pageFormat=pageable.getSafePageFormat();
					double width=pageFormat.getWidth();
					double height=pageFormat.getHeight();
					float startIncrement=0.1f;
					float endIncrement=0.0f;						
					float progressIncrement = (1.0f-startIncrement-endIncrement)/pageCount;
					for (int p=0;p< pageCount;p++) {
						setProgress(startIncrement+p*progressIncrement);
						document.setPageSize(new Rectangle((float)width,(float)height));
						if (p==0) document.open();
						else document.newPage();
						
						Graphics2D g = writer.getDirectContent().createGraphics((float)width, (float)height);
						printable.print(g, p);
						g.dispose();
					}
					document.close();
				}
				setProgress(1.0f);
				return null;
			}
		});
		jobQueue.schedule(job);
	}

    private static SystemFileChooser chooser=null;
    private static FileNameExtensionFilter pdfFilter=null;
    private static File chooseFile(String projectName, Component parentComponent) {
    	if (chooser == null){
    		chooser = new SystemFileChooser();
    		chooser.setFileSelectionMode(SystemFileChooser.FILES_ONLY);
    		chooser.setAcceptAllFileFilterUsed(true);
    		pdfFilter = new FileNameExtensionFilter("PDF (*.pdf)", "pdf");
    		chooser.addChoosableFileFilter(pdfFilter);
    	}
		if (projectName.length()==0)
			projectName="project";
		chooser.setSelectedFile(new File(projectName+".pdf"));
		chooser.setFileFilter(pdfFilter);
		if (chooser.showSaveDialog(parentComponent) == SystemFileChooser.APPROVE_OPTION){
			return PdfExportUtil.appendPdfExtensionIfMissing(chooser.getSelectedFile());
		} else return null;
    }

}

