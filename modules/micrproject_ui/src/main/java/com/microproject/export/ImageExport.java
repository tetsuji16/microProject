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

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;
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
import com.microproject.strings.Messages;

public class ImageExport {
	public static void export(final GraphPageable pageable,Component parentComponent) throws IOException{
		final File file=chooseFile(pageable.getRenderer().getProject().getName(),parentComponent);
		if (file == null) {
			return;
		}
		final JobQueue jobQueue=SessionFactory.getInstance().getJobQueue();
		Job job=new Job(jobQueue,"Image Export",Messages.getString("LocalFileImporter.Exporting"),true,parentComponent);
		job.addRunnable(new JobRunnable("Image Export",1.0f){
			public Object run() throws Exception{
				boolean pdf=true;
				if (file.getName().endsWith(".png"))
					pdf=false;
				try (FileOutputStream output = pdf ? new FileOutputStream(file) : null) {
				Document document = null;
				PdfWriter writer = null;
				if (pdf){
					document = new Document();
					writer = PdfWriter.getInstance(document, output);
				}
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
						if (pdf){
							document.setPageSize(new Rectangle((float)width,(float)height));
							if (p==0)
								document.open();
							else document.newPage();
							Graphics2D g = writer.getDirectContent().createGraphics((float)width, (float)height);
							printable.print(g, p);
							g.dispose();
						}else{
							BufferedImage bi = new BufferedImage((int)width, (int)height,BufferedImage.TYPE_INT_ARGB);
							
							Graphics2D g2 = (Graphics2D)bi.createGraphics();
							g2.setBackground(Color.WHITE);
							printable.print(g2, p);
				            g2.dispose();
		            try (FileOutputStream pngOutput = new FileOutputStream(file)) {
		             ImageIO.write(bi, "png", pngOutput);
			            }
				            break;
						}
					}
					if (pdf)
						document.close();
				}
				}
				setProgress(1.0f);
				return null;
			}
		});
		jobQueue.schedule(job);
	}

    private static SystemFileChooser chooser=null;
    private static FileNameExtensionFilter pdfFilter=null;
    private static FileNameExtensionFilter pngFilter=null;
	private static File chooseFile(String projectName, Component parentComponent) {
    	if (chooser == null){
    		chooser = new SystemFileChooser();
    		chooser.setFileSelectionMode(SystemFileChooser.FILES_ONLY);
    		chooser.setAcceptAllFileFilterUsed(true);
    		pdfFilter=new FileNameExtensionFilter("PDF (*.pdf)", "pdf");
    		pngFilter=new FileNameExtensionFilter("PNG (*.png)", "png");
    		chooser.addChoosableFileFilter(pdfFilter);
    		//chooser.addChoosableFileFilter(pngFilter);
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
