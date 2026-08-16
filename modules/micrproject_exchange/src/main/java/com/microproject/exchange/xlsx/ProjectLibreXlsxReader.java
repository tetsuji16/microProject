package com.microproject.exchange.xlsx;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;	import java.util.logging.Level;
	import java.util.logging.Logger;
	import java.util.regex.Matcher;
	import java.util.regex.Pattern;


import net.sf.mpxj.MPXJException;
import net.sf.mpxj.Duration;
import net.sf.mpxj.RelationType;
import net.sf.mpxj.TimeUnit;
import net.sf.mpxj.ProjectFile;
import net.sf.mpxj.reader.AbstractProjectReader;
import net.sf.mpxj.mspdi.MSPDIReader;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.microproject.pm.task.Project;
import com.microproject.server.data.DocumentData;
import com.microproject.server.data.Serializer;
import com.microproject.util.SafeObjectInput;

public class ProjectLibreXlsxReader extends AbstractProjectReader {
	private static final Logger logger = Logger.getLogger(ProjectLibreXlsxReader.class.getName());
	private static final String META_SHEET = "_PL_META";
	private static final String DATA_SHEET = "_PL_DATA";
	private static final String NATIVE_DATA_SHEET = "_PL_NATIVE";
	private static final String TASKS_SHEET = "Tasks";
	private static final String RESOURCES_SHEET = "Resources";
	private static final String ASSIGNMENTS_SHEET = "Assignments";
	private static final String DEPENDENCIES_SHEET = "Dependencies";

	public ProjectFile read(String fileName) throws MPXJException {
		return read(new File(fileName));
	}

	public List<ProjectFile> readAll(String fileName) throws MPXJException {
		return Collections.singletonList(read(fileName));
	}

	public ProjectFile read(File file) throws MPXJException {
		try {
			FileInputStream in = new FileInputStream(file);
			try {
				return read(in);
			} finally {
				in.close();
			}
		} catch (Exception e) {
			throw new MPXJException("Failed to read ProjectLibre XLSX file", e);
		}
	}

	public List<ProjectFile> readAll(File file) throws MPXJException {
		return Collections.singletonList(read(file));
	}

	public ProjectFile read(InputStream in) throws MPXJException {
		try {
			XSSFWorkbook workbook = new XSSFWorkbook(in);
			try {
				ProjectFile payload = readPayload(workbook);
				if (payload != null) {
					addListenersToProject(payload);
					return payload;
				}

				ProjectFile embeddedProject = readEmbeddedMspdi(workbook);
				if (embeddedProject != null) {
					addListenersToProject(embeddedProject);
					return embeddedProject;
				}

				ProjectFile summaryProject = readSummaryProject(workbook);
				addListenersToProject(summaryProject);
				return summaryProject;
			} finally {
				workbook.close();
			}
		} catch (MPXJException e) {
			throw e;
		} catch (Exception e) {
			throw new MPXJException("Failed to read ProjectLibre XLSX stream", e);
		}
	}

	public List<ProjectFile> readAll(InputStream in) throws MPXJException {
		return Collections.singletonList(read(in));
	}

	public static Project readProjectLibreProject(File file) throws Exception {
		try (FileInputStream in = new FileInputStream(file)) {
			return readProjectLibreProject(in);
		}
	}

	public static Project readProjectLibreProject(InputStream in) throws Exception {
		try (XSSFWorkbook workbook = new XSSFWorkbook(in)) {
			Sheet sheet = workbook.getSheet(NATIVE_DATA_SHEET);
			if (sheet == null) {
				return null;
			}
			StringBuilder encoded = new StringBuilder();
			for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
				Row row = sheet.getRow(rowIndex);
				if (row != null && row.getCell(0) != null) {
					encoded.append(row.getCell(0).getStringCellValue());
				}
			}
			if (encoded.length() == 0) {
				return null;
			}
			byte[] serialized = Base64.getDecoder().decode(encoded.toString());
			try (var objectIn = SafeObjectInput.create(new ByteArrayInputStream(serialized))) {
				Object value = objectIn.readObject();
				if (!(value instanceof DocumentData)) {
					throw new MPXJException("Invalid ProjectLibre native XLSX payload");
				}
				return new Serializer().deserializeLocalDocument((DocumentData) value);
			} catch (Exception | LinkageError e) {
				logger.log(Level.WARNING, "Ignoring unusable ProjectLibre native XLSX payload", e);
				return null;
			}
		}
	}

	private ProjectFile readPayload(XSSFWorkbook workbook) throws Exception {
		Sheet sheet = workbook.getSheet(DATA_SHEET);
		if (sheet == null) {
			return null;
		}
		StringBuilder xml = new StringBuilder();
		for (int rowIndex = 2; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
			Row row = sheet.getRow(rowIndex);
			if (row == null || row.getCell(0) == null) {
				continue;
			}
			xml.append(row.getCell(0).getStringCellValue());
		}
		if (xml.length() == 0) {
			return null;
		}
		ByteArrayInputStream in = new ByteArrayInputStream(xml.toString().getBytes(StandardCharsets.UTF_8));
		return new MSPDIReader().read(in);
	}

	private ProjectFile readEmbeddedMspdi(XSSFWorkbook workbook) throws Exception {
		Sheet sheet = workbook.getSheet("_ProjectLibre_MSPDI");
		if (sheet == null) {
			return null;
		}
		StringBuilder xml = new StringBuilder();
		for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
			Row row = sheet.getRow(rowIndex);
			if (row == null) {
				continue;
			}
			xml.append(stringCell(row, 0));
		}
		String source = xml.toString();
		if (source.length() == 0) {
			return null;
		}
		ByteArrayInputStream in = new ByteArrayInputStream(source.getBytes(StandardCharsets.UTF_8));
		return new MSPDIReader().read(in);
	}

	private ProjectFile readSummaryProject(XSSFWorkbook workbook) throws Exception {
		ProjectFile project = new ProjectFile();
		project.addDefaultBaseCalendar();
		Map<Integer, net.sf.mpxj.Task> tasks = readTasks(project, workbook.getSheet(TASKS_SHEET));
		Map<Integer, net.sf.mpxj.Resource> resources = readResources(project, workbook.getSheet(RESOURCES_SHEET));
		readAssignments(workbook.getSheet(ASSIGNMENTS_SHEET), tasks, resources);
		readDependencies(workbook.getSheet(DEPENDENCIES_SHEET), tasks);
		return project;
	}

	private Map<Integer, net.sf.mpxj.Task> readTasks(ProjectFile project, Sheet sheet) {
		Map<Integer, net.sf.mpxj.Task> tasks = new LinkedHashMap<Integer, net.sf.mpxj.Task>();
		if (sheet == null) {
			return tasks;
		}
		for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
			Row row = sheet.getRow(rowIndex);
			if (row == null || isBlank(row)) {
				continue;
			}
			net.sf.mpxj.Task task = project.addTask();
			Integer uid = integerCell(row, 0);
			Integer id = integerCell(row, 1);
			if (uid != null) {
				task.setUniqueID(uid);
				tasks.put(uid, task);
			}
			if (id != null) {
				task.setID(id);
			}
			task.setName(stringCell(row, 3));
			task.setNotes(stringCell(row, 4));
			if (dateCell(row, 5) != null) {
				task.setStart(dateCell(row, 5));
			}
			if (dateCell(row, 6) != null) {
				task.setFinish(dateCell(row, 6));
			}
			Double percent = doubleCell(row, 7);
			if (percent != null) {
				task.setPercentageComplete(percent);
			}
			String wbs = stringCell(row, 8);
			if (wbs.length() > 0) {
				task.setWBS(wbs);
			}
		}
		return tasks;
	}

	private Map<Integer, net.sf.mpxj.Resource> readResources(ProjectFile project, Sheet sheet) {
		Map<Integer, net.sf.mpxj.Resource> resources = new LinkedHashMap<Integer, net.sf.mpxj.Resource>();
		if (sheet == null) {
			return resources;
		}
		for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
			Row row = sheet.getRow(rowIndex);
			if (row == null || isBlank(row)) {
				continue;
			}
			net.sf.mpxj.Resource resource = project.addResource();
			Integer uid = integerCell(row, 0);
			Integer id = integerCell(row, 1);
			if (uid != null) {
				resource.setUniqueID(uid);
				resources.put(uid, resource);
			}
			if (id != null) {
				resource.setID(id);
			}
			resource.setName(stringCell(row, 2));
			resource.setNotes(stringCell(row, 3));
			resource.setGroup(stringCell(row, 4));
			resource.setEmailAddress(stringCell(row, 5));
			Double maxUnits = doubleCell(row, 6);
			if (maxUnits != null) {
				resource.setMaxUnits(maxUnits);
			}
		}
		return resources;
	}

	private void readAssignments(Sheet sheet, Map<Integer, net.sf.mpxj.Task> tasks, Map<Integer, net.sf.mpxj.Resource> resources) {
		if (sheet == null) {
			return;
		}
		for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
			Row row = sheet.getRow(rowIndex);
			if (row == null || isBlank(row)) {
				continue;
			}
			Integer taskUid = integerCell(row, 0);
			Integer resourceUid = integerCell(row, 1);
			net.sf.mpxj.Task task = tasks.get(taskUid);
			net.sf.mpxj.Resource resource = resources.get(resourceUid);
			if (task == null || resource == null) {
				continue;
			}
			net.sf.mpxj.ResourceAssignment assignment = task.addResourceAssignment(resource);
			Double units = doubleCell(row, 2);
			if (units != null) {
				assignment.setUnits(units);
			}
		}
	}

	private void readDependencies(Sheet sheet, Map<Integer, net.sf.mpxj.Task> tasks) {
		if (sheet == null) {
			return;
		}
		for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
			Row row = sheet.getRow(rowIndex);
			if (row == null || isBlank(row)) {
				continue;
			}
			Integer successorUid = integerCell(row, 0);
			Integer predecessorUid = integerCell(row, 1);
			net.sf.mpxj.Task successor = tasks.get(successorUid);
			net.sf.mpxj.Task predecessor = tasks.get(predecessorUid);
			if (successor == null || predecessor == null) {
				continue;
			}
			int relationType = integerCell(row, 2) == null ? RelationType.FINISH_START.getValue() : integerCell(row, 2).intValue();
			Duration lag = durationCell(row, 3);
			successor.addPredecessor(predecessor, RelationType.getInstance(relationType),
					lag == null ? Duration.getInstance(0, TimeUnit.DAYS) : lag);
		}
	}

	private static final long ENCODED_LAG_THRESHOLD = 1L << 57; // unit flags occupy bits 57+, plain millis never reach this

	/**
	 * Reads the Dependencies sheet Lag cell (issue #162). Supported forms:
	 * <ul>
	 * <li>plain milliseconds (current ProjectLibreXlsxWriter format)</li>
	 * <li>the old encoded long (unit flags in bits 57+) for backward compatibility</li>
	 * <li>{@code %<fraction>} / {@code e%<fraction>} percent lags</li>
	 * <li>MPXJ {@code Duration#toString()} form (e.g. {@code 2.0d}, {@code -1.0h}, {@code 90.0m})</li>
	 * </ul>
	 * Returns null when the cell is empty or unparseable (caller falls back to zero).
	 */
	private Duration durationCell(Row row, int column) {
		Cell cell = row.getCell(column);
		if (cell == null) {
			return null;
		}
		if (cell.getCellType() == CellType.NUMERIC) {
			long value = (long) cell.getNumericCellValue();
			if (Math.abs(value) >= ENCODED_LAG_THRESHOLD) {
				return decodeEncodedLag(value); // old writer format
			}
			return Duration.getInstance(value / 60000.0, TimeUnit.MINUTES); // plain millis
		}
		String text = stringCell(row, column).trim();
		if (text.length() == 0) {
			return null;
		}
		if (text.startsWith("%") || text.startsWith("e%")) {
			boolean elapsed = text.startsWith("e");
			try {
				double fraction = Double.parseDouble(text.substring(elapsed ? 2 : 1));
				return Duration.getInstance(fraction * 100.0,
						elapsed ? TimeUnit.ELAPSED_PERCENT : TimeUnit.PERCENT);
			} catch (NumberFormatException e) {
				return null;
			}
		}
		Matcher matcher = MPXJ_DURATION_PATTERN.matcher(text);
		if (matcher.matches()) {
			double value = Double.parseDouble(matcher.group(2));
			if (matcher.group(1) != null) {
				value = -value;
			}
			boolean elapsed = matcher.group(3) != null;
			TimeUnit unit = mpxjUnit(matcher.group(4), elapsed);
			return Duration.getInstance(value, unit);
		}
		return null;
	}

	private static final Pattern MPXJ_DURATION_PATTERN =
			Pattern.compile("^(-)?(\\d+(?:\\.\\d+)?)(e)?([mhdw]|mo|y|%)$");

	private static TimeUnit mpxjUnit(String unit, boolean elapsed) {
		switch (unit) {
		case "m": return elapsed ? TimeUnit.ELAPSED_MINUTES : TimeUnit.MINUTES;
		case "h": return elapsed ? TimeUnit.ELAPSED_HOURS : TimeUnit.HOURS;
		case "d": return elapsed ? TimeUnit.ELAPSED_DAYS : TimeUnit.DAYS;
		case "w": return elapsed ? TimeUnit.ELAPSED_WEEKS : TimeUnit.WEEKS;
		case "mo": return elapsed ? TimeUnit.ELAPSED_MONTHS : TimeUnit.MONTHS;
		case "y": return elapsed ? TimeUnit.ELAPSED_YEARS : TimeUnit.YEARS;
		case "%": return elapsed ? TimeUnit.ELAPSED_PERCENT : TimeUnit.PERCENT;
		default: return TimeUnit.MINUTES;
		}
	}

	/**
	 * Decodes the old ProjectLibreXlsxWriter format: a datatype Duration encoded
	 * long (millis in the low bits, unit flags in bits 57+).
	 */
	private static Duration decodeEncodedLag(long encoded) {
		int type = com.microproject.datatype.Duration.getType(encoded);
		if (type == com.microproject.datatype.TimeUnit.PERCENT) {
			return Duration.getInstance(com.microproject.datatype.Duration.getPercentAsDecimal(encoded) * 100.0, TimeUnit.PERCENT);
		}
		if (type == com.microproject.datatype.TimeUnit.ELAPSED_PERCENT) {
			return Duration.getInstance(com.microproject.datatype.Duration.getPercentAsDecimal(encoded) * 100.0, TimeUnit.ELAPSED_PERCENT);
		}
		long millis = com.microproject.datatype.Duration.millis(encoded);
		TimeUnit unit = mpxjUnit(unitSuffix(type), com.microproject.datatype.Duration.isElapsed(encoded));
		return Duration.getInstance(millis / 60000.0, unit);
	}

	private static String unitSuffix(int type) {
		switch (type) {
		case com.microproject.datatype.TimeUnit.MINUTES: case com.microproject.datatype.TimeUnit.ELAPSED_MINUTES: return "m";
		case com.microproject.datatype.TimeUnit.HOURS: case com.microproject.datatype.TimeUnit.ELAPSED_HOURS: return "h";
		case com.microproject.datatype.TimeUnit.DAYS: case com.microproject.datatype.TimeUnit.ELAPSED_DAYS: return "d";
		case com.microproject.datatype.TimeUnit.WEEKS: case com.microproject.datatype.TimeUnit.ELAPSED_WEEKS: return "w";
		case com.microproject.datatype.TimeUnit.MONTHS: case com.microproject.datatype.TimeUnit.ELAPSED_MONTHS: return "mo";
		case com.microproject.datatype.TimeUnit.YEARS: case com.microproject.datatype.TimeUnit.ELAPSED_YEARS: return "y";
		default: return "m";
		}
	}

	private boolean isBlank(Row row) {
		for (int i = 0; i <= 8; i++) {
			if (stringCell(row, i).length() > 0) {
				return false;
			}
		}
		return true;
	}

	private String stringCell(Row row, int column) {
		Cell cell = row.getCell(column);
		if (cell == null) {
			return "";
		}
		switch (cell.getCellType()) {
		case NUMERIC:
			double value = cell.getNumericCellValue();
			long asLong = (long) value;
			return Double.compare(value, asLong) == 0 ? String.valueOf(asLong) : String.valueOf(value);
		case STRING:
			return cell.getStringCellValue();
		case BOOLEAN:
			return String.valueOf(cell.getBooleanCellValue());
		default:
			return "";
		}
	}

	private Integer integerCell(Row row, int column) {
		Double value = doubleCell(row, column);
		return value == null ? null : Integer.valueOf(value.intValue());
	}

	private Double doubleCell(Row row, int column) {
		Cell cell = row.getCell(column);
		if (cell == null) {
			return null;
		}
		if (cell.getCellType() == CellType.NUMERIC) {
			return Double.valueOf(cell.getNumericCellValue());
		}
		String text = stringCell(row, column);
		if (text.length() == 0) {
			return null;
		}
		return Double.valueOf(text);
	}

	private java.util.Date dateCell(Row row, int column) {
		Double value = doubleCell(row, column);
		return value == null || value.longValue() <= 0L ? null : new java.util.Date(value.longValue());
	}
}
