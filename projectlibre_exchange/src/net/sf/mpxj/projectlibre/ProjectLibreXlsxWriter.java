package net.sf.mpxj.projectlibre;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import com.projectlibre1.pm.assignment.Assignment;
import com.projectlibre1.pm.calendar.WorkCalendar;
import com.projectlibre1.pm.calendar.WorkDay;
import com.projectlibre1.pm.calendar.WorkingCalendar;
import com.projectlibre1.pm.calendar.WorkingHours;
import com.projectlibre1.pm.dependency.Dependency;
import com.projectlibre1.pm.resource.ResourceImpl;
import com.projectlibre1.pm.resource.ResourcePool;
import com.projectlibre1.pm.task.NormalTask;
import com.projectlibre1.pm.task.Project;

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
		FileOutputStream out = new FileOutputStream(file);
		try {
			write(projectFile, out);
		} finally {
			out.close();
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
			writeMeta(workbook, "projectlibre", "2");
			writePayload(workbook, serializeProjectLibreXml(project), "projectlibre");
			writeProjectLibreSummarySheets(workbook, project);
			workbook.write(out);
		} catch (Exception e) {
			throw wrap("Failed to write ProjectLibre project to XLSX workbook", e);
		} finally {
			workbook.close();
		}
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
		return xml.toString("UTF-8");
	}

	private String serializeProjectLibreXml(Project project) throws Exception {
		ByteArrayOutputStream xml = new ByteArrayOutputStream();
		com.projectlibre1.server.data.mspdi.ModifiedMSPDIWriter data = new com.projectlibre1.server.data.MSPDISerializer().serializeProject(project);
		data.write(data.getProjectFile(), xml);
		return xml.toString("UTF-8");
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
		for (Object value : project.getTasks()) {
			if (!(value instanceof NormalTask)) {
				continue;
			}
			NormalTask task = (NormalTask) value;
			com.projectlibre1.pm.task.Task parent = task.getWbsParentTask();
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
		for (Object value : pool.getResourceList()) {
			if (!(value instanceof ResourceImpl)) {
				continue;
			}
			ResourceImpl resource = (ResourceImpl) value;
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
		for (Object value : project.getTasks()) {
			if (!(value instanceof NormalTask)) {
				continue;
			}
			NormalTask task = (NormalTask) value;
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
		for (Object value : project.getTasks()) {
			if (!(value instanceof NormalTask)) {
				continue;
			}
			NormalTask task = (NormalTask) value;
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
					valueOf(dependency.getLag()));
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
			for (Object value : pool.getResourceList()) {
				if (value instanceof ResourceImpl) {
					writeProjectCalendar(sheet, rowIndex++, ((ResourceImpl) value).getWorkCalendar());
				}
			}
		}
		for (Object value : project.getTasks()) {
			if (value instanceof NormalTask) {
				writeProjectCalendar(sheet, rowIndex++, ((NormalTask) value).getWorkCalendar());
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
			com.projectlibre1.pm.calendar.WorkRange range = (com.projectlibre1.pm.calendar.WorkRange) interval;
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

	private String safe(Object value) {
		return value == null ? "" : String.valueOf(value);
	}

	private String valueOf(Object value) {
		return value == null ? "" : String.valueOf(value);
	}
}
