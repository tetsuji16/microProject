/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.reports.model;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

/** RFC 4180 CSV export for standard and custom reports. */
public final class ReportCsvWriter {
	private ReportCsvWriter() { }
	public static void write(ReportModel model, OutputStream output) throws IOException {
		write(model.columns().stream().map(field -> field.getName()).toList(), model.rows(), output);
	}
	public static void write(List<String> headers, List<? extends List<?>> rows, OutputStream output) throws IOException {
		try (Writer writer = new OutputStreamWriter(output, StandardCharsets.UTF_8)) {
			writer.write('\uFEFF');
			try (CSVPrinter csv = new CSVPrinter(writer, CSVFormat.RFC4180)) {
				csv.printRecord(headers);
				for (List<?> row : rows) csv.printRecord(row.stream()
					.map(value -> value == null ? "" : String.valueOf(value)).toList());
			}
		}
	}
}
