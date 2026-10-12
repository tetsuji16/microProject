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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Font;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;

import com.microproject.configuration.ReportDefinition;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRField;
import net.sf.jasperreports.engine.JRPrintText;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;

class ReportUtilTest {
	private static final String JASPER_XML_LOGGER_NAME = "net.sf.jasperreports.engine.xml";

	@Test
	void previewFontIsACompositeFontThatCanRenderJapanese() {
		Font font = new Font(ReportFontSupport.PREVIEW_FONT_NAME, Font.PLAIN, 12);

		assertTrue(font.canDisplayUpTo("日本語") < 0,
			() -> "Report font cannot display Japanese: " + font);
	}

	@Test
	void loadsBundledJrxmlReportDefinitions() throws Exception {
		ReportDefinition definition = new ReportDefinition();
		definition.setName("projectDetails");
		definition.setFile("projectDetails.jrxml");

		JasperReport report = ReportUtil.getReport(definition, null, null);

		assertNotNull(report);
		assertNotNull(report.getName());
	}

	@Test
	void bundledProjectDetailsReportFillsAndExportsJapaneseAndEnglishProjectNames() throws Exception {
		ReportDefinition definition = new ReportDefinition();
		definition.setName("projectDetails");
		definition.setFile("projectDetails.jrxml");
		JasperReport report = ReportUtil.getReport(definition, null, null);

		for (String projectName : List.of("Quarterly plan", "日本語プロジェクト")) {
			Map<String, Object> parameters = new HashMap<>();
			parameters.put("projectName", projectName);
			Map<String, Object> row = new HashMap<>();
			for (JRField field : report.getFields()) {
				if (field.getValueClass() == String.class) {
					row.put(field.getName(), "工程 Alpha / 日本語");
				} else if (field.getValueClass() == Date.class) {
					row.put(field.getName(), new Date(0));
				}
			}
			row.put("notes", "Milestone note / 日本語");
			JRDataSource dataSource = new JRMapCollectionDataSource(List.of(row));
			JasperPrint print = JasperFillManager.fillReport(report, parameters, dataSource);
			List<String> printedText = print.getPages().stream()
				.flatMap(page -> page.getElements().stream())
				.filter(JRPrintText.class::isInstance)
				.map(JRPrintText.class::cast)
				.map(JRPrintText::getFullText)
				.toList();
			ByteArrayOutputStream pdf = new ByteArrayOutputStream();
			JasperExportManager.exportReportToPdfStream(print, pdf);
			byte[] pdfBytes = pdf.toByteArray();

			assertTrue(print.getPages().size() > 0, "Expected at least one rendered page");
			assertTrue(printedText.contains(projectName), "Expected project name in rendered report: " + projectName);
			assertTrue(printedText.contains("Milestone note / 日本語"), "Expected data-source notes in rendered report");
			assertTrue(pdfBytes.length > 0, "Expected non-empty PDF output for project name: " + projectName);
			assertTrue(pdfBytes.length >= 4
				&& new String(pdfBytes, 0, 4, StandardCharsets.US_ASCII).equals("%PDF"),
				"Expected PDF export to produce a PDF document");
		}
	}

	@Test
	void suppressesLegacyJrxmlWarningsWhenLoadingBundledReports() throws Exception {
		ReportDefinition definition = new ReportDefinition();
		definition.setName("projectDetails");
		definition.setFile("projectDetails.jrxml");

		Logger xmlLogger = Logger.getLogger(JASPER_XML_LOGGER_NAME);
		Level previousLevel = xmlLogger.getLevel();
		boolean previousUseParentHandlers = xmlLogger.getUseParentHandlers();
		CapturingHandler handler = new CapturingHandler();
		xmlLogger.addHandler(handler);
		xmlLogger.setLevel(Level.ALL);
		xmlLogger.setUseParentHandlers(false);
		try {
			ReportUtil.getReport(definition, null, null);
		} finally {
			xmlLogger.removeHandler(handler);
			xmlLogger.setUseParentHandlers(previousUseParentHandlers);
			xmlLogger.setLevel(previousLevel);
		}

		assertTrue(handler.warningMessages().isEmpty(),
			() -> "Expected JRXML warnings to be suppressed, but got: " + handler.warningMessages());
	}

	@Test
	void failsClearlyWhenReportDefinitionIsMissing() {
		ReportDefinition definition = new ReportDefinition();
		definition.setName("missing");
		definition.setFile("missing-report.jrxml");

		JRException exception = assertThrows(JRException.class, () -> ReportUtil.getReport(definition, null, null));

		assertTrue(exception.getMessage().contains("missing-report.jrxml"));
	}

	private static final class CapturingHandler extends Handler {
		private final List<String> warningMessages = new ArrayList<>();

		@Override
		public void publish(LogRecord record) {
			if (record != null && record.getLevel().intValue() >= Level.WARNING.intValue()) {
				warningMessages.add(record.getMessage());
			}
		}

		@Override
		public void flush() {
		}

		@Override
		public void close() {
		}

		List<String> warningMessages() {
			return warningMessages;
		}
	}
}
