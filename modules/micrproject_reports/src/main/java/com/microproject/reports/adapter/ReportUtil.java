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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.xml.JRXmlLoader;

import com.microproject.configuration.Dictionary;
import com.microproject.configuration.ReportDefinition;
import com.microproject.graphic.configuration.SpreadSheetFieldArray;
import com.microproject.timescale.TimeIterator;
import com.microproject.util.ClassLoaderUtils;
import com.microproject.contrib.util.Log;
import com.microproject.contrib.util.LogFactory;

/**
 *
 */
public class ReportUtil {
	private static final Log log = LogFactory.getLog(ReportUtil.class);
	private static final String REPORT_ROOT = "com/microproject/reports/definition/";
	private static final String JASPER_XML_LOGGER_NAME = "net.sf.jasperreports.engine.xml";
	private static final Object JASPER_XML_LOGGER_LOCK = new Object();

	private static InputStream openReport(String fileName) {
		String urlName = REPORT_ROOT + fileName;
		URL url = ClassLoaderUtils.getLocalClassLoader().getResource(urlName);
		if (url == null) {
			throw new IllegalArgumentException("Report definition not found on classpath: " + urlName);
		}
		try {
			return url.openStream();
		} catch (IOException e) {
			throw new IllegalStateException("Unable to open report definition: " + urlName, e);
		}
	}

	private static JasperDesign loadBundledJrxml(String fileName) throws JRException {
		try (InputStream reportDefinitionStream = openReport(fileName)) {
			byte[] jrxmlBytes = reportDefinitionStream.readAllBytes();
			synchronized (JASPER_XML_LOGGER_LOCK) {
				Logger xmlLogger = Logger.getLogger(JASPER_XML_LOGGER_NAME);
				Level previousLevel = xmlLogger.getLevel();
				try {
					// JasperReports 6.21.5 still accepts the bundled JRXML shape, so keep the
					// source document intact and only suppress loader warnings.
					xmlLogger.setLevel(Level.SEVERE);
					return JRXmlLoader.load(new ByteArrayInputStream(jrxmlBytes));
				} finally {
					xmlLogger.setLevel(previousLevel);
				}
			}
		} catch (IllegalArgumentException | IllegalStateException e) {
			throw new JRException("Unable to load report definition " + fileName, e);
		} catch (IOException e) {
			log.error("Unable to close report definition " + fileName, e);
			throw new JRException("Unable to close report definition " + fileName, e);
		}
	}
	
	public static JasperReport getReport(ReportDefinition reportDefinition, TimeIterator iterator, SpreadSheetFieldArray columns) throws JRException {
	    JasperReport report = (JasperReport) reportDefinition.getReportObject(columns); // if it is already compiled, reuse it
		if (report == null) {
			
			JasperDesign jasperDesign = null;
			
			if(null != reportDefinition.getFile()) {
				// regular jrxml file
				jasperDesign = loadBundledJrxml(reportDefinition.getFile());
			} else {
				// jasper design made by ReportAdapter
				ReportAdapter reportAdapter = new ReportAdapter(reportDefinition);
				reportAdapter.generateDesign(columns);
				jasperDesign = reportAdapter.getJasperDesign();
			}
			

			// check if design needs timescale
//			System.out.println("is report time based: " + jasperDesign.getProperty("timeBased"));
//			if(null != jasperDesign.getProperty("timeBased")) {
			//				jasperDesign = DataSourceProvider.addTimescale(jasperDesign, iterator, java.lang.String.class);
			//		}

		
			report = JasperCompileManager.compileReport(jasperDesign);
			
			reportDefinition.setReportObject(report,columns);
		}
		return report;
	}
	
	public static Object[] getReportDefinitions() {
		return Dictionary.getAll(ReportDefinition.CATEGORY);
	}
	
	public static ReportDefinition getFromName(String name) {
		Object[] defs = getReportDefinitions();
		for (int i =0; i < defs.length; i++) {
			ReportDefinition def = (ReportDefinition)defs[i];
			if (def.getName().equals(name))
				return def;
		}
		return null;
	}
}
