package net.sf.mpxj.writer;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import net.sf.mpxj.json.JsonWriter;
import net.sf.mpxj.mpx.MPXWriter;
import net.sf.mpxj.mspdi.MSPDIWriter;
import net.sf.mpxj.planner.PlannerWriter;
import net.sf.mpxj.primavera.PrimaveraPMFileWriter;
import com.projectlibre1.exchange.xlsx.ProjectLibreXlsxWriter;
import net.sf.mpxj.sdef.SDEFWriter;

public final class ProjectWriterUtility {
	private static final Map<String, Supplier<? extends ProjectWriter>> WRITER_MAP = new HashMap<String, Supplier<? extends ProjectWriter>>();

	static {
		WRITER_MAP.put("MPX", MPXWriter::new);
		WRITER_MAP.put("XML", MSPDIWriter::new);
		WRITER_MAP.put("PMXML", PrimaveraPMFileWriter::new);
		WRITER_MAP.put("PLANNER", PlannerWriter::new);
		WRITER_MAP.put("JSON", JsonWriter::new);
		WRITER_MAP.put("SDEF", SDEFWriter::new);
		WRITER_MAP.put("XLSX", ProjectLibreXlsxWriter::new);
	}

	private ProjectWriterUtility() {
	}

	public static ProjectWriter getProjectWriter(String fileName) throws InstantiationException, IllegalAccessException {
		int extensionPosition = fileName.lastIndexOf('.');
		if (extensionPosition == -1) {
			throw new IllegalArgumentException("Filename has no extension: " + fileName);
		}
		String extension = fileName.substring(extensionPosition + 1).toUpperCase();
		Supplier<? extends ProjectWriter> writerFactory = WRITER_MAP.get(extension);
		if (writerFactory == null) {
			throw new IllegalArgumentException("Cannot write files of type: " + fileName);
		}
		return writerFactory.get();
	}

	public static Set<String> getSupportedFileExtensions() {
		return Collections.unmodifiableSet(WRITER_MAP.keySet());
	}
}
