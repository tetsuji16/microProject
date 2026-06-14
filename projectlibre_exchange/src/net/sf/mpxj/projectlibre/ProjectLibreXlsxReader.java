package net.sf.mpxj.projectlibre;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.sf.mpxj.MPXJException;
import net.sf.mpxj.Duration;
import net.sf.mpxj.RelationType;
import net.sf.mpxj.TimeUnit;
import net.sf.mpxj.ProjectFile;
import net.sf.mpxj.reader.AbstractProjectReader;
import net.sf.mpxj.mspdi.MSPDIReader;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class ProjectLibreXlsxReader extends AbstractProjectReader {
	private static final String META_SHEET = "_PL_META";
	private static final String DATA_SHEET = "_PL_DATA";
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
			successor.addPredecessor(predecessor, RelationType.getInstance(relationType), Duration.getInstance(0, TimeUnit.DAYS));
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
		case Cell.CELL_TYPE_NUMERIC:
			double value = cell.getNumericCellValue();
			long asLong = (long) value;
			return Double.compare(value, asLong) == 0 ? String.valueOf(asLong) : String.valueOf(value);
		case Cell.CELL_TYPE_STRING:
			return cell.getStringCellValue();
		case Cell.CELL_TYPE_BOOLEAN:
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
		if (cell.getCellType() == Cell.CELL_TYPE_NUMERIC) {
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
