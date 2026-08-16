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
