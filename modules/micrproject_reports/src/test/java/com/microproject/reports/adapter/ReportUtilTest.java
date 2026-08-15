package com.microproject.reports.adapter;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;

import com.microproject.configuration.ReportDefinition;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperReport;

class ReportUtilTest {
	private static final String JASPER_XML_LOGGER_NAME = "net.sf.jasperreports.engine.xml";

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
