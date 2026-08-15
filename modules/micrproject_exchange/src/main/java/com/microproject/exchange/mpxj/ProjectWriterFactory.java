package com.microproject.exchange.mpxj;

import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import com.microproject.exchange.xlsx.ProjectLibreXlsxWriter;

import net.sf.mpxj.json.JsonWriter;
import net.sf.mpxj.mpx.MPXWriter;
import net.sf.mpxj.mspdi.MSPDIWriter;
import net.sf.mpxj.planner.PlannerWriter;
import net.sf.mpxj.primavera.PrimaveraPMFileWriter;
import net.sf.mpxj.sdef.SDEFWriter;
import net.sf.mpxj.writer.ProjectWriter;

/** Selects the ProjectLibre-compatible writer for an output file extension. */
public final class ProjectWriterFactory {
	private static final Map<String, Supplier<? extends ProjectWriter>> WRITERS = Map.of(
		"MPX", MPXWriter::new,
		"XML", MSPDIWriter::new,
		"PMXML", PrimaveraPMFileWriter::new,
		"PLANNER", PlannerWriter::new,
		"JSON", JsonWriter::new,
		"SDEF", SDEFWriter::new,
		"XLSX", ProjectLibreXlsxWriter::new);

	private ProjectWriterFactory() {
	}

	public static ProjectWriter forFile(String fileName) {
		int extensionPosition = fileName.lastIndexOf('.');
		if (extensionPosition == -1)
			throw new IllegalArgumentException("Filename has no extension: " + fileName);
		String extension = fileName.substring(extensionPosition + 1).toUpperCase(java.util.Locale.ROOT);
		Supplier<? extends ProjectWriter> writerFactory = WRITERS.get(extension);
		if (writerFactory == null)
			throw new IllegalArgumentException("Cannot write files of type: " + fileName);
		return writerFactory.get();
	}

	public static Set<String> supportedFileExtensions() {
		return WRITERS.keySet();
	}
}
