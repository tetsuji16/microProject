package com.microproject.exchange.xlsx;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import com.microproject.pm.assignment.Assignment;
import com.microproject.pm.calendar.WorkCalendar;
import com.microproject.pm.calendar.WorkDay;
import com.microproject.pm.calendar.WorkingCalendar;
import com.microproject.pm.calendar.WorkingHours;
import com.microproject.pm.dependency.Dependency;
import com.microproject.pm.resource.ResourceImpl;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.server.data.Serializer;

import net.sf.mpxj.ProjectFile;
import net.sf.mpxj.ProjectProperties;
import net.sf.mpxj.Day;
import net.sf.mpxj.Resource;
import net.sf.mpxj.ResourceAssignment;
import net.sf.mpxj.Task;
import net.sf.mpxj.TimephasedWork;
import net.sf.mpxj.mspdi.MSPDIWriter;
import net.sf.mpxj.writer.ProjectWriter;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class ProjectLibreXlsxWriter implements ProjectWriter {
	private static final String META_SHEET = "_PL_META";
	private static final String DATA_SHEET = "_PL_DATA";
	private static final String NATIVE_DATA_SHEET = "_PL_NATIVE";
	private static final String TASKS_SHEET = "Tasks";
	private static final String RESOURCES_SHEET = "Resources";
	private static final String ASSIGNMENTS_SHEET = "Assignments";
	private static final String DEPENDENCIES_SHEET = "Dependencies";
	private static final String CALENDARS_SHEET = "Calendars";
	private static final String TIMEPHASED_SHEET = "Timephased";
	private static final int PAYLOAD_CHUNK_SIZE = 30000;

	public void write(ProjectFile projectFile, String fileName) throws IOException {
		write(projectFile, new File(fileName));
	}

	public void write(ProjectFile projectFile, File file) throws IOException {
		try (FileOutputStream out = new FileOutputStream(file)) {
			write(projectFile, out);
		}
	}

	public void write(ProjectFile projectFile, OutputStream out) throws IOException {
		writeWorkbookFromProjectFile(projectFile, out);
	}

	public void writeProjectLibreProject(Project project, OutputStream out) throws IOException {
		writeWorkbookFromProjectLibreProject(project, out);
	}

	private void writeWorkbookFromProjectFile(ProjectFile projectFile, OutputStream out) throws IOException {
		XSSFWorkbook workbook = new XSSFWorkbook();
		try {
			writeMeta(workbook, "projectfile", "2");
			writePayload(workbook, serializeProjectFileXml(projectFile), "projectfile");
			writeProjectFileSummarySheets(workbook, projectFile);
			workbook.write(out);
		} catch (Exception e) {
			throw wrap("Failed to write ProjectLibre XLSX workbook", e);
		} finally {
			workbook.close();
		}
	}

	private void writeWorkbookFromProjectLibreProject(Project project, OutputStream out) throws IOException {
		XSSFWorkbook workbook = new XSSFWorkbook();
		try {
			writeMeta(workbook, "projectlibre", "3");
			writePayload(workbook, serializeProjectLibreXml(project), "projectlibre");
			writeNativePayload(workbook, project);
			writeProjectLibreSummarySheets(workbook, project);
			workbook.write(out);
		} catch (Exception e) {
			throw wrap("Failed to write ProjectLibre project to XLSX workbook", e);
		} finally {
			workbook.close();
		}
	}

	private void writeNativePayload(XSSFWorkbook workbook, Project project) throws Exception {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
			out.writeObject(new Serializer().serializeDocument(project));
		}
		String payload = Base64.getEncoder().encodeToString(bytes.toByteArray());
		Sheet sheet = workbook.createSheet(NATIVE_DATA_SHEET);
		sheet.createRow(0).createCell(0).setCellValue("ProjectLibreNativeData");
		int rowIndex = 1;
		for (int offset = 0; offset < payload.length(); offset += PAYLOAD_CHUNK_SIZE) {
			int end = Math.min(offset + PAYLOAD_CHUNK_SIZE, payload.length());
			sheet.createRow(rowIndex++).createCell(0).setCellValue(payload.substring(offset, end));
		}
		workbook.setSheetHidden(workbook.getSheetIndex(sheet), true);
	}

	private IOException wrap(String message, Exception cause) {
		IOException wrapped = new IOException(message);
		wrapped.initCause(cause);
		return wrapped;
	}

	private void writeMeta(XSSFWorkbook workbook, String workbookKind, String version) {
		Sheet sheet = workbook.createSheet(META_SHEET);
		sheet.createRow(0).createCell(0).setCellValue("WorkbookKind");
		sheet.createRow(0).createCell(1).setCellValue(workbookKind);
		sheet.createRow(1).createCell(0).setCellValue("SchemaVersion");
		sheet.createRow(1).createCell(1).setCellValue(version);
	}

	private String serializeProjectFileXml(ProjectFile projectFile) throws Exception {
		ByteArrayOutputStream xml = new ByteArrayOutputStream();
		new MSPDIWriter().write(projectFile, xml);
		return xml.toString(StandardCharsets.UTF_8.name());
	}

	private String serializeProjectLibreXml(Project project) throws Exception {
		ByteArrayOutputStream xml = new ByteArrayOutputStream();
		com.microproject.server.data.mspdi.ModifiedMSPDIWriter data = new com.microproject.server.data.MSPDISerializer().serializeProject(project);
		data.write(data.getProjectFile(), xml);
		return xml.toString(StandardCharsets.UTF_8.name());
	}

	private void writePayload(XSSFWorkbook workbook, String payload, String kind) {
		Sheet sheet = workbook.createSheet(DATA_SHEET);
		sheet.createRow(0).createCell(0).setCellValue("PayloadKind");
		sheet.getRow(0).createCell(1).setCellValue(kind);
		sheet.createRow(1).createCell(0).setCellValue("Payload");
		int rowIndex = 2;
		for (int offset = 0; offset < payload.length(); offset += PAYLOAD_CHUNK_SIZE) {
			int end = Math.min(offset + PAYLOAD_CHUNK_SIZE, payload.length());
			Row row = sheet.createRow(rowIndex++);
			row.createCell(0).setCellValue(payload.substring(offset, end));
		}
	}

	private void writeProjectFileSummarySheets(XSSFWorkbook workbook, ProjectFile projectFile) {
		writeProjectFileTasks(workbook, projectFile);
		writeProjectFileResources(workbook, projectFile);
		writeProjectFileAssignments(workbook, projectFile);
		writeProjectFileDependencies(workbook, projectFile);
		writeProjectFileCalendars(workbook, projectFile);
		writeProjectFileTimephased(workbook, projectFile);
	}

	private void writeProjectLibreSummarySheets(XSSFWorkbook workbook, Project project) {
		writeProjectLibreTasks(workbook, project);
		writeProjectLibreResources(workbook, project);
		writeProjectLibreAssignments(workbook, project);
		writeProjectLibreDependencies(workbook, project);
		writeProjectLibreCalendars(workbook, project);
		writeProjectLibreTimephased(workbook, project);
	}

	private void writeProjectFileTasks(XSSFWorkbook workbook, ProjectFile projectFile) {
		Sheet sheet = workbook.createSheet(TASKS_SHEET);
		writeRow(sheet, 0, "UniqueID", "ID", "ParentUniqueID", "Name", "Notes", "Start", "Finish", "PercentComplete", "WBS");
		int rowIndex = 1;
		for (Task task : projectFile.getTasks()) {
			if (task == null || task.getNull()) {
				continue;
			}
			Task parent = task.getParentTask();
			writeRow(sheet, rowIndex++,
				valueOf(task.getUniqueID()),
				valueOf(task.getID()),
				parent == null ? "" : valueOf(parent.getUniqueID()),
				safe(task.getName()),
				safe(task.getNotes()),
				valueOf(task.getStart()),
				valueOf(task.getFinish()),
				valueOf(task.getPercentageComplete()),
				safe(task.getWBS()));
		}
		autoSize(sheet, 9);
	}

	private void writeProjectLibreTasks(XSSFWorkbook workbook, Project project) {
		Sheet sheet = workbook.createSheet(TASKS_SHEET);
		writeRow(sheet, 0, "UniqueID", "ID", "ParentUniqueID", "Name", "Notes", "Start", "Finish", "PercentComplete", "WBS");
		int rowIndex = 1;
		for (com.microproject.pm.task.Task taskValue : project.getTaskList()) {
			if (!(taskValue instanceof NormalTask)) {
				continue;
			}
			NormalTask task = (NormalTask) taskValue;
			com.microproject.pm.task.Task parent = task.getWbsParentTask();
			writeRow(sheet, rowIndex++,
				valueOf(task.getUniqueId()),
				valueOf(task.getId()),
				parent == null ? "" : valueOf(parent.getUniqueId()),
				safe(task.getName()),
				safe(task.getNotes()),
				valueOf(task.getStart()),
				valueOf(task.getEnd()),
				valueOf(task.getPercentComplete()),
				safe(task.getWbs()));
		}
		autoSize(sheet, 9);
	}

	private void writeProjectFileResources(XSSFWorkbook workbook, ProjectFile projectFile) {
		Sheet sheet = workbook.createSheet(RESOURCES_SHEET);
		writeRow(sheet, 0, "UniqueID", "ID", "Name", "Notes", "Group", "Email", "MaxUnits");
		int rowIndex = 1;
		for (Resource resource : projectFile.getResources()) {
			if (resource == null || resource.getNull()) {
				continue;
			}
			writeRow(sheet, rowIndex++,
				valueOf(resource.getUniqueID()),
				valueOf(resource.getID()),
				safe(resource.getName()),
				safe(resource.getNotes()),
				safe(resource.getGroup()),
				safe(resource.getEmailAddress()),
				valueOf(resource.getMaxUnits()));
		}
		autoSize(sheet, 6);
	}

	private void writeProjectLibreResources(XSSFWorkbook workbook, Project project) {
		Sheet sheet = workbook.createSheet(RESOURCES_SHEET);
		writeRow(sheet, 0, "UniqueID", "ID", "Name", "Notes", "Group", "Email", "MaxUnits");
		int rowIndex = 1;
		ResourcePool pool = project.getResourcePool();
		if (pool == null) {
			return;
		}
		for (com.microproject.pm.resource.Resource resourceValue : pool.getResourceList()) {
			if (!(resourceValue instanceof ResourceImpl)) {
				continue;
			}
			ResourceImpl resource = (ResourceImpl) resourceValue;
			writeRow(sheet, rowIndex++,
				valueOf(resource.getUniqueId()),
				valueOf(resource.getId()),
				safe(resource.getName()),
				safe(resource.getNotes()),
				safe(resource.getGroup()),
				safe(resource.getEmailAddress()),
				valueOf(resource.getMaximumUnits()));
		}
		autoSize(sheet, 6);
	}

	private void writeProjectFileAssignments(XSSFWorkbook workbook, ProjectFile projectFile) {
		Sheet sheet = workbook.createSheet(ASSIGNMENTS_SHEET);
		writeRow(sheet, 0, "TaskUniqueID", "ResourceUniqueID", "Units", "Delay", "LevelingDelay", "WorkContour");
		int rowIndex = 1;
		for (Task task : projectFile.getTasks()) {
			if (task == null || task.getNull()) {
				continue;
			}
			for (ResourceAssignment assignment : task.getResourceAssignments()) {
				writeRow(sheet, rowIndex++,
					valueOf(assignment.getTaskUniqueID()),
					valueOf(assignment.getResourceUniqueID()),
					valueOf(assignment.getUnits()),
					safe(assignment.getDelay()),
					safe(assignment.getLevelingDelay()),
					safe(assignment.getWorkContour()));
			}
		}
		autoSize(sheet, 5);
	}

	private void writeProjectLibreAssignments(XSSFWorkbook workbook, Project project) {
		Sheet sheet = workbook.createSheet(ASSIGNMENTS_SHEET);
		writeRow(sheet, 0, "TaskUniqueID", "ResourceUniqueID", "Units", "Delay", "LevelingDelay", "WorkContour");
		int rowIndex = 1;
		for (com.microproject.pm.task.Task taskValue : project.getTaskList()) {
			if (!(taskValue instanceof NormalTask)) {
				continue;
			}
			NormalTask task = (NormalTask) taskValue;
			for (Object assignmentValue : task.getAssignments()) {
				if (!(assignmentValue instanceof Assignment)) {
					continue;
				}
				Assignment assignment = (Assignment) assignmentValue;
				writeRow(sheet, rowIndex++,
					valueOf(assignment.getTask().getUniqueId()),
					valueOf(assignment.getResource().getUniqueId()),
					valueOf(assignment.getUnits()),
					safe(assignment.getDelay()),
					safe(assignment.getLevelingDelay()),
					valueOf(assignment.getWorkContourType()));
			}
		}
		autoSize(sheet, 5);
	}

	private void writeProjectFileDependencies(XSSFWorkbook workbook, ProjectFile projectFile) {
		Sheet sheet = workbook.createSheet(DEPENDENCIES_SHEET);
		writeRow(sheet, 0, "SuccessorUniqueID", "PredecessorUniqueID", "Type", "Lag");
		int rowIndex = 1;
		for (Task task : projectFile.getTasks()) {
			if (task == null || task.getNull()) {
				continue;
			}
			for (net.sf.mpxj.Relation relation : task.getPredecessors()) {
				writeRow(sheet, rowIndex++,
					valueOf(task.getUniqueID()),
					valueOf(relation.getTargetTask().getUniqueID()),
					valueOf(relation.getType().getValue()),
					safe(relation.getLag()));
			}
		}
		autoSize(sheet, 4);
	}

	private void writeProjectLibreDependencies(XSSFWorkbook workbook, Project project) {
		Sheet sheet = workbook.createSheet(DEPENDENCIES_SHEET);
		writeRow(sheet, 0, "SuccessorUniqueID", "PredecessorUniqueID", "Type", "Lag");
		int rowIndex = 1;
		for (com.microproject.pm.task.Task taskValue : project.getTaskList()) {
			if (!(taskValue instanceof NormalTask)) {
				continue;
			}
			NormalTask task = (NormalTask) taskValue;
			for (Object depValue : task.getPredecessorList()) {
				if (!(depValue instanceof Dependency)) {
					continue;
				}
				Dependency dependency = (Dependency) depValue;
				NormalTask predecessor = (NormalTask) dependency.getPredecessor();
				writeRow(sheet, rowIndex++,
					valueOf(task.getUniqueId()),
					valueOf(predecessor.getUniqueId()),
					valueOf(dependency.getDependencyType()),
					lagCellValue(dependency.getLag()));
			}
		}
		autoSize(sheet, 4);
	}

	private void writeProjectFileCalendars(XSSFWorkbook workbook, ProjectFile projectFile) {
		Sheet sheet = workbook.createSheet(CALENDARS_SHEET);
		writeRow(sheet, 0, "UniqueID", "Name", "BaseCalendar", "WorkingSpec", "Exceptions");
		int rowIndex = 1;
		for (net.sf.mpxj.ProjectCalendar calendar : projectFile.getCalendars()) {
			writeRow(sheet, rowIndex++,
				valueOf(calendar.getUniqueID()),
				safe(calendar.getName()),
				safe(calendar.getParent() == null ? null : calendar.getParent().getName()),
				describeCalendar(calendar),
				safe(calendar.getCalendarExceptions()));
		}
		autoSize(sheet, 5);
	}

	private void writeProjectLibreCalendars(XSSFWorkbook workbook, Project project) {
		Sheet sheet = workbook.createSheet(CALENDARS_SHEET);
		writeRow(sheet, 0, "UniqueID", "Name", "BaseCalendar", "WorkingSpec", "Exceptions");
		int rowIndex = 1;
		writeProjectCalendar(sheet, rowIndex++, project.getWorkCalendar());
		ResourcePool pool = project.getResourcePool();
		if (pool != null) {
			for (com.microproject.pm.resource.Resource resourceValue : pool.getResourceList()) {
				if (resourceValue instanceof ResourceImpl) {
					writeProjectCalendar(sheet, rowIndex++, ((ResourceImpl) resourceValue).getWorkCalendar());
				}
			}
		}
		for (com.microproject.pm.task.Task task : project.getTaskList()) {
			if (task instanceof NormalTask) {
				writeProjectCalendar(sheet, rowIndex++, ((NormalTask) task).getWorkCalendar());
			}
		}
		autoSize(sheet, 5);
	}

	private void writeProjectCalendar(Sheet sheet, int rowIndex, WorkCalendar calendar) {
		if (!(calendar instanceof WorkingCalendar)) {
			return;
		}
		WorkingCalendar working = (WorkingCalendar) calendar;
		StringBuilder week = new StringBuilder();
		for (int i = 0; i < 7; i++) {
			WorkDay day = working.getWeekDay(i);
			if (i > 0) {
				week.append('|');
			}
			week.append(i).append('=').append(describeDay(day));
		}
		StringBuilder exceptions = new StringBuilder();
		WorkDay[] days = working.getExceptionDays();
		if (days != null) {
			for (int i = 0; i < days.length; i++) {
				if (i > 0) {
					exceptions.append('|');
				}
				exceptions.append(describeDay(days[i]));
			}
		}
		writeRow(sheet, rowIndex,
			valueOf(working.getUniqueId()),
			safe(working.getName()),
			working.getBaseCalendar() == null ? "" : safe(working.getBaseCalendar().getName()),
			week.toString(),
			exceptions.toString());
	}

	private String describeCalendar(net.sf.mpxj.ProjectCalendar calendar) {
		StringBuilder result = new StringBuilder();
		for (Day day : Day.values()) {
			if (result.length() > 0) {
				result.append('|');
			}
			result.append(day.name()).append('=');
			net.sf.mpxj.DayType type = calendar.getCalendarDayType(day);
			result.append(type == null ? "null" : type.toString());
			net.sf.mpxj.ProjectCalendarHours hours = calendar.getHours(day);
			if (hours != null && hours.size() > 0) {
				result.append(':');
				for (int i = 0; i < hours.size(); i++) {
					if (i > 0) {
						result.append(',');
					}
					net.sf.mpxj.DateRange range = hours.get(i);
					result.append(range == null ? "" : range.toString());
				}
			}
		}
		return result.toString();
	}

	private void writeProjectFileTimephased(XSSFWorkbook workbook, ProjectFile projectFile) {
		Sheet sheet = workbook.createSheet(TIMEPHASED_SHEET);
		writeRow(sheet, 0, "TaskUniqueID", "ResourceUniqueID", "Kind", "Start", "Finish", "Value");
		int rowIndex = 1;
		for (Task task : projectFile.getTasks()) {
			if (task == null || task.getNull()) {
				continue;
			}
			for (ResourceAssignment assignment : task.getResourceAssignments()) {
				List<TimephasedWork> work = assignment.getTimephasedWork();
				if (work == null) {
					continue;
				}
				for (TimephasedWork item : work) {
					writeRow(sheet, rowIndex++,
						valueOf(task.getUniqueID()),
						valueOf(assignment.getResourceUniqueID()),
						"work",
						safe(item.getStart()),
						safe(item.getFinish()),
						safe(item.getTotalAmount()));
				}
			}
		}
		autoSize(sheet, 6);
	}

	private void writeProjectLibreTimephased(XSSFWorkbook workbook, Project project) {
		Sheet sheet = workbook.createSheet(TIMEPHASED_SHEET);
		writeRow(sheet, 0, "TaskUniqueID", "ResourceUniqueID", "Kind", "Start", "Finish", "Value");
		writeRow(sheet, 1, "", "", "payload", "", "", "see _PL_DATA for full timephased data");
		autoSize(sheet, 6);
	}

	private String describeDay(WorkDay day) {
		if (day == null) {
			return "";
		}
		WorkingHours hours = day.getWorkingHours();
		if (hours == null) {
			return "";
		}
		StringBuilder result = new StringBuilder();
		for (Object interval : hours.getIntervals()) {
			if (interval == null) {
				continue;
			}
			com.microproject.pm.calendar.WorkRange range = (com.microproject.pm.calendar.WorkRange) interval;
			if (result.length() > 0) {
				result.append(';');
			}
			result.append(range.getStart()).append('-').append(range.getEnd());
		}
		return result.toString();
	}

	private void writeRow(Sheet sheet, int rowIndex, Object... values) {
		Row row = sheet.createRow(rowIndex);
		for (int i = 0; i < values.length; i++) {
			Object value = values[i];
			if (value != null) {
				row.createCell(i).setCellValue(String.valueOf(value));
			}
		}
	}

	private void autoSize(Sheet sheet, int columns) {
		for (int i = 0; i <= columns; i++) {
			sheet.autoSizeColumn(i);
		}
	}

	/**
	 * Renders a dependency lag for the Dependencies sheet (issue #162).
	 * Time-based lags are written as plain milliseconds so the value survives
	 * the numeric cell round-trip exactly (the old encoded form lost low bits
	 * through the double cell and, worse, was never read back). Percent lags
	 * are written as {@code %<fraction>} ({@code e%<fraction>} when elapsed)
	 * so the reader can rebuild the percent encoding.
	 */
	private String lagCellValue(long lag) {
		int type = com.microproject.datatype.Duration.getType(lag);
		if (type == com.microproject.datatype.TimeUnit.PERCENT)
			return "%" + com.microproject.datatype.Duration.getPercentAsDecimal(lag);
		if (type == com.microproject.datatype.TimeUnit.ELAPSED_PERCENT)
			return "e%" + com.microproject.datatype.Duration.getPercentAsDecimal(lag);
		return String.valueOf(com.microproject.datatype.Duration.millis(lag));
	}

	private String safe(Object value) {
		return value == null ? "" : String.valueOf(value);
	}

	private String valueOf(Object value) {
		return value == null ? "" : String.valueOf(value);
	}
}
