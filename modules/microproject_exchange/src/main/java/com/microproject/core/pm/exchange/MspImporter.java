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
package com.microproject.core.pm.exchange;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.mpxj.Duration;
import net.sf.mpxj.ProjectCalendar;
import net.sf.mpxj.ProjectFile;
import net.sf.mpxj.Relation;
import net.sf.mpxj.ResourceAssignment;
import net.sf.mpxj.mpp.MPPReader;
import net.sf.mpxj.mpx.MPXReader;
import net.sf.mpxj.mspdi.schema.TimephasedDataType;
import net.sf.mpxj.reader.AbstractProjectReader;
import net.sf.mpxj.reader.UniversalProjectReader;
import net.sf.mpxj.planner.PlannerReader;
import com.microproject.exchange.xlsx.ProjectLibreXlsxReader;

import com.microproject.core.pm.exchange.converters.mpx.MpxAssignmentConverter;
import com.microproject.core.pm.exchange.converters.mpx.MpxCalendarConverter;
import com.microproject.core.pm.exchange.converters.mpx.MpxDependencyConverter;
import com.microproject.core.pm.exchange.converters.mpx.MpxImportState;
import com.microproject.core.pm.exchange.converters.mpx.MpxOptionsConverter;
import com.microproject.core.pm.exchange.converters.mpx.MpxProjectConverter;
import com.microproject.core.pm.exchange.converters.mpx.MpxResourceConverter;
import com.microproject.core.pm.exchange.converters.mpx.MpxTaskConverter;
import com.microproject.core.pm.exchange.converters.mpx.MpxUtils;
import com.microproject.core.pm.exchange.converters.type.DateUTCConverter;
import com.microproject.core.pm.exchange.converters.type.PercentNumberRatioDoubleConverter;
import com.microproject.datatype.DurationDefaults;
import com.microproject.exchange.ImportedCalendarService;
import com.microproject.exchange.BoundedInput;
import com.microproject.grouping.core.Node;
import com.microproject.pm.calendar.CalendarService;
import com.microproject.pm.calendar.WorkCalendar;
import com.microproject.pm.calendar.WorkingCalendar;
import com.microproject.pm.resource.Resource;
import com.microproject.pm.resource.EnterpriseResource;
import com.microproject.pm.resource.ResourceImpl;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.assignment.Assignment;
import com.microproject.pm.dependency.Dependency;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.Task;
import com.microproject.pm.task.TaskSnapshot;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.SubProj;
import com.microproject.pm.task.DefaultSubProj;
import com.microproject.pm.criticalpath.TaskSchedule;
import com.microproject.undo.DataFactoryUndoController;

/**
 * @author Laurent Chretienneau
 *
 */
public class MspImporter {
	private static final Logger logger = Logger.getLogger(MspImporter.class.getName());
	protected ProjectFile mpxProjectFile;
	protected MpxImportState state=new MpxImportState();
	protected AbstractProjectReader reader;
	/** Native ProjectLibre XLSX payload, when present and valid. */
	protected Project nativeProject;
	protected long earliestTaskStart=-1L;
	protected net.sf.mpxj.Task mpxRootTask=null;
	private final List<ImportedAssignmentLevelingDelay> importedAssignmentLevelingDelays = new ArrayList<>();

	private record ImportedAssignmentLevelingDelay(ResourceAssignment source, Assignment target) {}
	
	public Project importProject(String name, ProgressClosure progress) throws Exception{
		progress.updateProgress(0.0f, "Start");
		parseProject(name);
		return importProject_(progress);		
	}

	public Project importProject(InputStream in, String extension, ProgressClosure progress) throws Exception{
		progress.updateProgress(0.0f, "Start");
		parseProject(in,extension);
		return importProject_(progress);		
	}

	private Project importProject_(ProgressClosure progress) throws Exception{
		if (nativeProject != null) {
			progress.updateProgress(1f, "Completed");
			return nativeProject;
		}
		progress.updateProgress(0.2f, "File parsed");
		state.initializeMpxTimephasedMap();
		importedAssignmentLevelingDelays.clear();

		DataFactoryUndoController undo = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("imported", undo);
		Project project = Project.createProject(resourcePool, undo);
		// Task creation and duration conversion happen before the remainder of
		// the project header is applied. Install the source unit defaults first so
		// task constructors and converters never interpret this project through
		// the process-wide calendar settings.
		project.setDurationDefaults(MpxProjectConverter.durationDefaults(mpxProjectFile.getProjectProperties()));

		importOptions(project);
		progress.updateProgress(0.3f, "Options converted");
		importCalendars(project);
		progress.updateProgress(0.4f, "Calendars converted");
		importResourcePool(project);
		progress.updateProgress(0.5f, "Resources converted");
		importTasks(project);
		progress.updateProgress(0.7f, "Tasks converted");
		importDependencies(project);
		progress.updateProgress(0.8f, "Dependencies converted");
		importProjectHeader(project); //must be done after tasks to correct project start
		progress.updateProgress(0.9f, "Project headers converted");
		// Tasks and links are created while importing is active, so their normal
		// incremental scheduling callbacks are intentionally suppressed.  Restore
		// the same initialized, calculated state as a persisted native project
		// before exposing this imported project to a Gantt view.
		project.initialize(false, false);
		project.recalculate();
		restoreImportedAssignmentLevelingDelays();
		state.validatePostSchedule();

		progress.updateProgress(1f, "Completed");
		return project;
	}

	private void restoreImportedAssignmentLevelingDelays() {
		MpxAssignmentConverter converter = new MpxAssignmentConverter();
		for (ImportedAssignmentLevelingDelay imported : importedAssignmentLevelingDelays) {
			converter.restoreLevelingDelay(imported.source(), imported.target(), state);
		}
	}
	
	
	public void parseProject(InputStream in, String extension) throws Exception {
		try {
			InputStream source;
			if ("xlsx".equalsIgnoreCase(extension)) {
				// ProjectLibre XLSX contains both a lossless native payload and an
				// MSPDI payload. Keep a replayable copy so native failure can use the
				// established MSPDI fallback reader.
				byte[] data = BoundedInput.readXlsxImport(in);
				try {
					nativeProject = ProjectLibreXlsxReader.readProjectLibreProject(
						new ByteArrayInputStream(data));
				} catch (Exception e) {
					// Non-ZIP XML supplied with an XLSX extension is an existing
					// compatibility case; let the normal extension normalization and
					// MSPDI reader handle it below.
					nativeProject = null;
				}
				if (nativeProject != null) {
					return;
				}
				source = prepareProjectStream(new ByteArrayInputStream(data));
			} else {
				nativeProject = null;
				if ("xml".equalsIgnoreCase(extension)) {
					byte[] data = in.readAllBytes();
					RawMspdiData rawData = parseRawMspdiData(data);
					state.setRawMspdiAssignmentWork(rawData.assignmentWork());
					state.setRawMspdiAssignmentBaselineWork(rawData.assignmentBaselineWork());
					state.setRawMspdiAssignmentBaselineCost(rawData.assignmentBaselineCost());
					state.setRawMspdiAssignmentBaselines(rawData.assignmentBaselines());
					state.setRawMspdiTaskBaselines(rawData.taskBaselines());
					state.setRawMspdiResourceRates(rawData.resourceRates());
					source = prepareProjectStream(new ByteArrayInputStream(data));
				} else {
					source = prepareProjectStream(in);
				}
			}
			String effectiveExtension = normalizeExtension(extension, source);
			reader = createReader(effectiveExtension);
			mpxProjectFile = readProjectFile(source);
			state.setMpxProjectFile(mpxProjectFile);
		} finally {
			if (in!=null)
				in.close();
		}	
	}

	private AbstractProjectReader createReader(String extension) {
		if (extension == null) {
			throw new IllegalArgumentException("Unsupported import extension: null");
		}
		if (isMspdiExtension(extension)) {
			state.setMspdi(true);
			return new net.sf.mpxj.mspdi.MSPDIReader();
		}
		if (extension.equals("mpp"))
			return new MPPReader();
		if (extension.equals("mpx"))
			return new MPXReader();
		if (extension.equals("planner"))
			return new PlannerReader();
		if (extension.equals("xlsx"))
			return new ProjectLibreXlsxReader();
		throw new IllegalArgumentException("Unsupported import extension: " + extension);
	}

	private boolean isMspdiExtension(String extension) {
		if (extension == null) {
			return false;
		}
		return extension.equals("xml") || extension.equals("pod");
	}

	private ProjectFile readProjectFile(InputStream source) throws Exception {
		return reader.read(source);
	}

	private InputStream prepareProjectStream(InputStream in) {
		if (in instanceof BufferedInputStream) {
			return in;
		}
		return new BufferedInputStream(in);
	}

	private record RawMspdiData(Map<Long, MpxImportState.RawMspdiAssignmentWork> assignmentWork,
		Map<Long, Map<Integer, List<MpxImportState.RawMspdiWorkInterval>>> assignmentBaselineWork,
		Map<Long, Map<Integer, List<MpxImportState.RawMspdiCostInterval>>> assignmentBaselineCost,
		Map<Long, Map<Integer, MpxImportState.RawMspdiAssignmentBaseline>> assignmentBaselines,
		Map<Long, Map<Integer, MpxImportState.RawMspdiTaskBaseline>> taskBaselines,
		Map<Long, MpxImportState.RawMspdiResourceRates> resourceRates) { }

	private RawMspdiData parseRawMspdiData(byte[] xml)
			throws Exception {
		javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
		factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
		factory.setXIncludeAware(false);
		factory.setExpandEntityReferences(false);
		factory.setAttribute(javax.xml.XMLConstants.ACCESS_EXTERNAL_DTD, "");
		factory.setAttribute(javax.xml.XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
		org.w3c.dom.Document document = factory.newDocumentBuilder()
			.parse(new ByteArrayInputStream(xml));
		String namespace = document.getDocumentElement().getNamespaceURI();
		DurationDefaults durationDefaults = rawDurationDefaults(document.getDocumentElement(), namespace);
		org.w3c.dom.NodeList assignments = document.getElementsByTagNameNS(namespace, "Assignment");
		Map<Long, MpxImportState.RawMspdiAssignmentWork> result = new HashMap<>();
		Map<Long, Map<Integer, List<MpxImportState.RawMspdiWorkInterval>>> assignmentBaselineWork = new HashMap<>();
		Map<Long, Map<Integer, List<MpxImportState.RawMspdiCostInterval>>> assignmentBaselineCost = new HashMap<>();
		Map<Long, Map<Integer, MpxImportState.RawMspdiAssignmentBaseline>> assignmentBaselines = new HashMap<>();
		Map<Long, Map<Integer, MpxImportState.RawMspdiTaskBaseline>> taskBaselines = new HashMap<>();
		Map<Long, MpxImportState.RawMspdiResourceRates> resourceRates = new HashMap<>();
		org.w3c.dom.NodeList resources = document.getElementsByTagNameNS(namespace, "Resource");
		for (int index = 0; index < resources.getLength(); index++) {
			org.w3c.dom.Element resource = (org.w3c.dom.Element) resources.item(index);
			String rawUid = xmlChildText(resource, "UID", namespace);
			if (rawUid == null) continue;
			MpxImportState.RawMspdiResourceRates rates = new MpxImportState.RawMspdiResourceRates(
				parseMspdiResourceRate(xmlChildText(resource, "StandardRate", namespace),
					xmlChildText(resource, "StandardRateFormat", namespace), durationDefaults),
				parseMspdiResourceRate(xmlChildText(resource, "OvertimeRate", namespace),
					xmlChildText(resource, "OvertimeRateFormat", namespace), durationDefaults),
				parseMspdiResourceCost(xmlChildText(resource, "CostPerUse", namespace)));
			if ((rates.standardRate() != null || rates.overtimeRate() != null || rates.costPerUse() != null)
				&& resourceRates.put(Long.valueOf(rawUid), rates) != null)
				throw new IllegalArgumentException("MSPDI contains duplicate resource UIDs with scalar rates");
		}
		org.w3c.dom.NodeList tasks = document.getElementsByTagNameNS(namespace, "Task");
		for (int index = 0; index < tasks.getLength(); index++) {
			org.w3c.dom.Element task = (org.w3c.dom.Element) tasks.item(index);
			String rawUid = xmlChildText(task, "UID", namespace);
			if (rawUid == null) continue;
			long uid = Long.parseLong(rawUid);
			Map<Integer, MpxImportState.RawMspdiTaskBaseline> baselines = new HashMap<>();
			for (org.w3c.dom.Element baseline : xmlDirectChildren(task, "Baseline", namespace)) {
				String rawNumber = xmlChildText(baseline, "Number", namespace);
				String rawFormat = xmlChildText(baseline, "DurationFormat", namespace);
				String rawDuration = xmlChildText(baseline, "Duration", namespace);
				if (rawNumber == null || rawFormat == null || rawDuration == null) continue;
				int durationFormat = Integer.parseInt(rawFormat);
				if (elapsedDurationUnit(durationFormat) == com.microproject.datatype.TimeUnit.NONE) continue;
				int slot = Integer.parseInt(rawNumber);
				if (slot < 0 || slot > 10) throw new IllegalArgumentException("MSPDI baseline number is outside 0..10");
				long durationMillis = parseMspdiDuration(rawDuration);
				if (durationMillis < 0L) throw new IllegalArgumentException("MSPDI baseline duration cannot be negative");
				MpxImportState.RawMspdiTaskBaseline previous = baselines.put(Integer.valueOf(slot),
					new MpxImportState.RawMspdiTaskBaseline(durationMillis, durationFormat));
				if (previous != null) throw new IllegalArgumentException("MSPDI task contains duplicate baseline slots");
			}
			if (!baselines.isEmpty() && taskBaselines.put(Long.valueOf(uid), Map.copyOf(baselines)) != null)
				throw new IllegalArgumentException("MSPDI contains duplicate task UIDs with baseline durations");
		}
		for (int index = 0; index < assignments.getLength(); index++) {
			org.w3c.dom.Element assignment = (org.w3c.dom.Element) assignments.item(index);
			String rawAssignmentUid = xmlChildText(assignment, "UID", namespace);
			if (rawAssignmentUid == null) continue;
			long assignmentUid = Long.parseLong(rawAssignmentUid);
			Map<Integer, List<MpxImportState.RawMspdiWorkInterval>> baselineSlots = new HashMap<>();
			Map<Integer, List<MpxImportState.RawMspdiCostInterval>> baselineCostSlots = new HashMap<>();
			Map<Integer, MpxImportState.RawMspdiAssignmentBaseline> scalarBaselineSlots = new HashMap<>();
			for (org.w3c.dom.Element baseline : xmlDirectChildren(assignment, "Baseline", namespace)) {
				String rawNumber = xmlChildText(baseline, "Number", namespace);
				if (rawNumber == null) continue;
				int slot = Integer.parseInt(rawNumber);
				if (slot < 0 || slot > 10)
					throw new IllegalArgumentException("MSPDI assignment baseline number is outside 0..10");
				String rawStart = xmlChildText(baseline, "Start", namespace);
				String rawFinish = xmlChildText(baseline, "Finish", namespace);
				String rawWork = xmlChildText(baseline, "Work", namespace);
				if (rawStart != null || rawFinish != null || rawWork != null) {
					MpxImportState.RawMspdiAssignmentBaseline scalar =
						new MpxImportState.RawMspdiAssignmentBaseline(
							parseNullableMspdiWallTime(rawStart), parseNullableMspdiWallTime(rawFinish),
							parseMspdiDuration(rawWork));
					if (scalarBaselineSlots.put(Integer.valueOf(slot), scalar) != null)
						throw new IllegalArgumentException("MSPDI assignment contains duplicate baseline scalar slots");
				}
				List<MpxImportState.RawMspdiWorkInterval> intervals = new ArrayList<>();
				int expectedType = MpxImportState.assignmentBaselineWorkType(slot);
				List<MpxImportState.RawMspdiCostInterval> costIntervals = new ArrayList<>();
				int expectedCostType = MpxImportState.assignmentBaselineCostType(slot);
				for (org.w3c.dom.Element row : xmlDirectChildren(baseline, "TimephasedData", namespace)) {
					String rawType = xmlChildText(row, "Type", namespace);
					if (rawType == null) continue;
					int type = Integer.parseInt(rawType);
					if (type != expectedType && type != expectedCostType) continue;
					if (!"2".equals(xmlChildText(row, "Unit", namespace)))
						throw new IllegalArgumentException("Assignment baseline rows require per-day units");
					long start = parseMspdiWallTime(xmlChildText(row, "Start", namespace));
					long finish = parseMspdiWallTime(xmlChildText(row, "Finish", namespace));
					if (finish <= start)
						throw new IllegalArgumentException("Assignment baseline row has invalid bounds");
					String rawValue = xmlChildText(row, "Value", namespace);
					if (type == expectedType) {
						Long rowWork = parseMspdiDuration(rawValue);
						if (rowWork == null || rowWork.longValue() < 0L)
							throw new IllegalArgumentException("Assignment baseline work row has invalid value");
						intervals.add(new MpxImportState.RawMspdiWorkInterval(expectedType, start, finish,
							rowWork.longValue()));
					} else {
						double rowCost = parseMspdiCost(rawValue);
						costIntervals.add(new MpxImportState.RawMspdiCostInterval(expectedCostType, start, finish, rowCost));
					}
				}
				if (!intervals.isEmpty() && baselineSlots.put(Integer.valueOf(slot), List.copyOf(intervals)) != null)
					throw new IllegalArgumentException("MSPDI assignment contains duplicate baseline work slots");
				if (!costIntervals.isEmpty() && baselineCostSlots.put(Integer.valueOf(slot), List.copyOf(costIntervals)) != null)
					throw new IllegalArgumentException("MSPDI assignment contains duplicate baseline cost slots");
			}
			if (!baselineSlots.isEmpty() && assignmentBaselineWork.put(Long.valueOf(assignmentUid),
				Map.copyOf(baselineSlots)) != null)
				throw new IllegalArgumentException("MSPDI contains duplicate assignment UIDs with baseline work");
			if (!baselineCostSlots.isEmpty() && assignmentBaselineCost.put(Long.valueOf(assignmentUid),
				Map.copyOf(baselineCostSlots)) != null)
				throw new IllegalArgumentException("MSPDI contains duplicate assignment UIDs with baseline cost");
			if (assignmentBaselines.put(Long.valueOf(assignmentUid), Map.copyOf(scalarBaselineSlots)) != null)
				throw new IllegalArgumentException("MSPDI contains duplicate assignment UIDs with baseline scalars");
			if (!"8".equals(xmlChildText(assignment, "WorkContour", namespace))) continue;
			long uid = assignmentUid;
			Long assignmentStart = parseNullableMspdiWallTime(xmlChildText(assignment, "Start", namespace));
			Long assignmentFinish = parseNullableMspdiWallTime(xmlChildText(assignment, "Finish", namespace));
			Long work = parseMspdiDuration(xmlChildText(assignment, "Work", namespace));
			Long actual = parseMspdiDuration(xmlChildText(assignment, "ActualWork", namespace));
			Long remaining = parseMspdiDuration(xmlChildText(assignment, "RemainingWork", namespace));
			List<MpxImportState.RawMspdiWorkInterval> intervals = new ArrayList<>();
			for (org.w3c.dom.Element row : xmlDirectChildren(assignment, "TimephasedData", namespace)) {
				String rawType = xmlChildText(row, "Type", namespace);
				if (!"1".equals(rawType) && !"2".equals(rawType)) continue;
				if (!"2".equals(xmlChildText(row, "Unit", namespace)))
					throw new IllegalArgumentException("Custom MSPDI work rows require per-day units");
				long start = parseMspdiWallTime(xmlChildText(row, "Start", namespace));
				long finish = parseMspdiWallTime(xmlChildText(row, "Finish", namespace));
				Long rowWork = parseMspdiDuration(xmlChildText(row, "Value", namespace));
				if (finish <= start || rowWork == null || rowWork.longValue() < 0L)
					throw new IllegalArgumentException("Custom MSPDI work row has invalid bounds or value");
				intervals.add(new MpxImportState.RawMspdiWorkInterval(Integer.parseInt(rawType),
					start, finish, rowWork.longValue()));
			}
			if (result.put(Long.valueOf(uid), new MpxImportState.RawMspdiAssignmentWork(
				assignmentStart, assignmentFinish, work, actual, remaining, intervals)) != null)
				throw new IllegalArgumentException("MSPDI contains duplicate assignment UIDs");
		}
		return new RawMspdiData(Map.copyOf(result), Map.copyOf(assignmentBaselineWork),
			Map.copyOf(assignmentBaselineCost), Map.copyOf(assignmentBaselines), Map.copyOf(taskBaselines),
			Map.copyOf(resourceRates));
	}

	private static DurationDefaults rawDurationDefaults(org.w3c.dom.Element project, String namespace) {
		DurationDefaults product = DurationDefaults.PRODUCT_DEFAULTS;
		return new DurationDefaults(rawProjectNumber(project, "MinutesPerDay", namespace, product.minutesPerDay()),
			rawProjectNumber(project, "MinutesPerWeek", namespace, product.minutesPerWeek()),
			rawProjectNumber(project, "DaysPerMonth", namespace, product.daysPerMonth()),
			product.durationEnteredIn(), product.workUnit());
	}

	private static double rawProjectNumber(org.w3c.dom.Element project, String field, String namespace,
			double defaultValue) {
		String value = xmlChildText(project, field, namespace);
		return value == null ? defaultValue : Double.parseDouble(value.trim());
	}

	private static com.microproject.datatype.Rate parseMspdiResourceRate(String rawValue, String rawFormat,
			DurationDefaults defaults) {
		if (rawValue == null) return null;
		double amount = new java.math.BigDecimal(rawValue.trim()).doubleValue();
		int format = rawFormat == null ? 2 : Integer.parseInt(rawFormat.trim());
		int unit = switch (format) {
			case 1 -> com.microproject.datatype.TimeUnit.MINUTES;
			case 2 -> com.microproject.datatype.TimeUnit.HOURS;
			case 3 -> com.microproject.datatype.TimeUnit.DAYS;
			case 4 -> com.microproject.datatype.TimeUnit.WEEKS;
			case 5 -> com.microproject.datatype.TimeUnit.MONTHS;
			case 7 -> com.microproject.datatype.TimeUnit.YEARS;
			case 8 -> com.microproject.datatype.TimeUnit.NON_TEMPORAL;
			default -> com.microproject.datatype.TimeUnit.NONE;
		};
		if (unit == com.microproject.datatype.TimeUnit.NONE) return null;
		double factor = unit == com.microproject.datatype.TimeUnit.NON_TEMPORAL ? 1.0D
			: com.microproject.datatype.Duration.timeUnitFactor(unit, defaults);
		return new com.microproject.datatype.Rate(amount / factor, unit);
	}

	private static Double parseMspdiResourceCost(String rawValue) {
		return rawValue == null ? null : Double.valueOf(new java.math.BigDecimal(rawValue.trim()).doubleValue());
	}

	private static double parseMspdiCost(String value) {
		if (value == null)
			throw new IllegalArgumentException("Assignment baseline cost row has no value");
		double parsed;
		try {
			parsed = new java.math.BigDecimal(value.trim()).doubleValue();
		} catch (NumberFormatException exception) {
			throw new IllegalArgumentException("Assignment baseline cost row has invalid value", exception);
		}
		if (!Double.isFinite(parsed) || parsed < 0.0D)
			throw new IllegalArgumentException("Assignment baseline cost must be finite and nonnegative");
		return parsed;
	}

	private static String xmlChildText(org.w3c.dom.Element parent, String name, String namespace) {
		for (org.w3c.dom.Node child = parent.getFirstChild(); child != null; child = child.getNextSibling())
			if (child instanceof org.w3c.dom.Element element && name.equals(element.getLocalName())
					&& namespace.equals(element.getNamespaceURI())) return element.getTextContent();
		return null;
	}

	private static List<org.w3c.dom.Element> xmlDirectChildren(org.w3c.dom.Element parent, String name,
		String namespace) {
		List<org.w3c.dom.Element> result = new ArrayList<>();
		for (org.w3c.dom.Node child = parent.getFirstChild(); child != null; child = child.getNextSibling())
			if (child instanceof org.w3c.dom.Element element && name.equals(element.getLocalName())
					&& namespace.equals(element.getNamespaceURI())) result.add(element);
		return result;
	}

	private static Long parseMspdiDuration(String value) {
		if (value == null || value.isBlank()) return null;
		java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(
			"^P(?:(\\d+)Y)?(?:(\\d+)M)?(?:(\\d+)D)?(?:T(?:(\\d+)H)?(?:(\\d+)M)?(?:(\\d+(?:\\.\\d+)?)S)?)?$").matcher(value);
		if (!matcher.matches() || (matcher.group(1) == null && matcher.group(2) == null
				&& matcher.group(3) == null && matcher.group(4) == null && matcher.group(5) == null
				&& matcher.group(6) == null))
			throw new IllegalArgumentException("Invalid MSPDI duration: " + value);
		long years = matcher.group(1) == null ? 0L : Long.parseLong(matcher.group(1));
		long months = matcher.group(2) == null ? 0L : Long.parseLong(matcher.group(2));
		long days = matcher.group(3) == null ? 0L : Long.parseLong(matcher.group(3));
		long hours = matcher.group(4) == null ? 0L : Long.parseLong(matcher.group(4));
		long minutes = matcher.group(5) == null ? 0L : Long.parseLong(matcher.group(5));
		java.math.BigDecimal seconds = matcher.group(6) == null ? java.math.BigDecimal.ZERO
			: new java.math.BigDecimal(matcher.group(6));
		long millis = seconds.movePointRight(3).longValueExact();
		long total = Math.multiplyExact(years, 365L * 86_400_000L);
		// These match the core's existing elapsed duration factors (365d/year,
		// 30d/month); do not reinterpret them using the project working calendar.
		total = Math.addExact(total, Math.multiplyExact(months, 30L * 86_400_000L));
		total = Math.addExact(total, Math.multiplyExact(days, 86_400_000L));
		total = Math.addExact(total, Math.multiplyExact(hours, 3_600_000L));
		total = Math.addExact(total, Math.multiplyExact(minutes, 60_000L));
		return Long.valueOf(Math.addExact(total, millis));
	}

	private static int elapsedDurationUnit(int durationFormat) {
		return switch (durationFormat) {
			case 4, 36 -> com.microproject.datatype.TimeUnit.ELAPSED_MINUTES;
			case 6, 38 -> com.microproject.datatype.TimeUnit.ELAPSED_HOURS;
			case 8, 40 -> com.microproject.datatype.TimeUnit.ELAPSED_DAYS;
			case 10, 42 -> com.microproject.datatype.TimeUnit.ELAPSED_WEEKS;
			case 12, 44 -> com.microproject.datatype.TimeUnit.ELAPSED_MONTHS;
			default -> com.microproject.datatype.TimeUnit.NONE;
		};
	}

	private static long toRawBaselineDuration(MpxImportState.RawMspdiTaskBaseline baseline) {
		int unit = elapsedDurationUnit(baseline.durationFormat());
		double value = baseline.durationMillis() / com.microproject.datatype.Duration.timeUnitFactor(unit);
		long duration = com.microproject.datatype.Duration.getInstance(value, unit);
		if (baseline.durationFormat() >= 35)
			duration = com.microproject.datatype.Duration.setAsEstimated(duration, true);
		return duration;
	}

	private static long parseMspdiWallTime(String value) {
		if (value == null) throw new IllegalArgumentException("Custom MSPDI work row is missing a date bound");
		return java.time.LocalDateTime.parse(value).toInstant(java.time.ZoneOffset.UTC).toEpochMilli();
	}

	private static Long parseNullableMspdiWallTime(String value) {
		return value == null || value.isBlank() ? null : Long.valueOf(parseMspdiWallTime(value));
	}

	private String normalizeExtension(String extension, InputStream in) throws Exception {
		if (!"xlsx".equals(extension) || !in.markSupported()) {
			return extension;
		}
		in.mark(256);
		byte[] header = new byte[256];
		int read = in.read(header);
		in.reset();
		if (read <= 0) {
			return extension;
		}
		String prefix = new String(header, 0, read, StandardCharsets.UTF_8).trim();
		if (prefix.startsWith("<?xml") || prefix.startsWith("<Project")) {
			return "xml"; //$NON-NLS-1$
		}
		return extension;
	}
	protected void parseProject(String fileName) throws Exception {
		fileName=fileName.trim();
		int extensionPosition=fileName.lastIndexOf("."); 
		String extension = extensionPosition==-1 ? "xml" : fileName.substring(extensionPosition+1).toLowerCase(java.util.Locale.ROOT);
		try (FileInputStream input = new FileInputStream(fileName)) {
			parseProject(input, extension);
		}
	}
	
	
	protected void importOptions(Project project) {
		// Obsolete CalendarOptions conversion removed (see issue #154).
	}
	
	protected void importProjectHeader(Project project) {
		MpxProjectConverter converter=new MpxProjectConverter();
		converter.from(mpxProjectFile.getProjectProperties(), project, state);

		if (earliestTaskStart!=-1L) //fix project start
			project.setStart(earliestTaskStart);
	}

	
	protected void importCalendars(Project project) {
		state.setProjectTitle(mpxProjectFile.getProjectProperties().getProjectTitle());

		MpxCalendarConverter converter = new MpxCalendarConverter();
		WorkCalendar standardBaseCalendar = null;
		for (ProjectCalendar mpxBaseCalendar : mpxProjectFile.getCalendars()) {
			WorkingCalendar calendar = WorkingCalendar.getStandardBasedInstance();
			if (ProjectCalendar.DEFAULT_BASE_CALENDAR_NAME.equals(mpxBaseCalendar.getName())) {
				state.setMpxStandardBaseCalendar(mpxBaseCalendar);
				standardBaseCalendar = calendar;
			}
			converter.from(mpxBaseCalendar, calendar, state);
			// register into the global calendar service via the import bridge
			ImportedCalendarService.getInstance().addImportedCalendar((WorkingCalendar) calendar, mpxBaseCalendar);
			state.mapBaseCalendar(calendar, mpxBaseCalendar);
		}
		if (standardBaseCalendar == null) {
			standardBaseCalendar = CalendarService.getInstance().getStandardInstance();
		}
		try {
			project.setBaseCalendar(standardBaseCalendar);
		} catch (com.microproject.configuration.CircularDependencyException e) {
			// ignore: a self-referential base calendar is not expected here
		}
		project.setWorkCalendar(standardBaseCalendar);
		state.setProjectBaseCalendar(standardBaseCalendar);
	}
	
	protected void importResourcePool(Project project) {
		ResourcePool resourcePool = project.getResourcePool();
		state.setResourcePool(resourcePool);
		importResources(resourcePool);
	}

	protected void importResources(ResourcePool resourcePool) {
		state.setResourcePool(resourcePool);
		MpxResourceConverter converter=new MpxResourceConverter();
		for (net.sf.mpxj.Resource mpxResource : mpxProjectFile.getResources()){
			if (shouldSkipResource(mpxResource)) {
				logSkippedResource(mpxResource);
				continue;
			}
			Resource resource;
			if (mpxResource.getID()==0) {
				resource=ResourceImpl.getUnassignedInstance();
				state.mapResource(mpxResource, resource);
				continue;
			}
			resource=resourcePool.newResourceInstance();
			converter.from(mpxResource, resource, state);
			state.mapResource(mpxResource, resource);
		}
	}

	private boolean shouldSkipResource(net.sf.mpxj.Resource mpxResource) {
		return mpxResource.getNull() || mpxResource.getID() == null;
	}

	private void logSkippedResource(net.sf.mpxj.Resource mpxResource) {
		logger.log(Level.FINE, "Skipping resource with missing identity: {0}", mpxResource);
	}
	
	protected void importTasks(Project project) {
		for (net.sf.mpxj.Task mpxTask : mpxProjectFile.getChildTasks()){
			importTasks(project,mpxTask,null);
		}
		// MPXJ materializes a cross-project predecessor as an external task
		// placeholder. These placeholders are present in getTasks() but need not
		// be reachable from the normal task hierarchy, so import them explicitly
		// before resolving predecessor links.
		for (net.sf.mpxj.Task mpxTask : mpxProjectFile.getTasks()) {
			if (mpxTask.getExternalTask() && state.getTask(mpxTask)==null && !shouldSkipTask(mpxTask))
				importRegularTask(project,mpxTask,null);
		}
	}
	
	protected void importTasks(Project project,net.sf.mpxj.Task mpxTask, Task parentTask) {
		if (shouldSkipTask(mpxTask)) {
			logSkippedTask(mpxTask);
			return;
		}

		Task task = null;
		if (isRootTask(mpxTask)) {
			registerRootTask(mpxTask);
		} else {
			task = importRegularTask(project, mpxTask, parentTask);
		}
		
		for (net.sf.mpxj.Task mpxChildTask : mpxTask.getChildTasks()){
			importTasks(project,mpxChildTask,task);
		}
	}

	private boolean shouldSkipTask(net.sf.mpxj.Task mpxTask) {
		return mpxTask.getNull() || mpxTask.getID()==null;
	}

	private void logSkippedTask(net.sf.mpxj.Task mpxTask) {
		logger.log(Level.FINE, "Skipping task with missing identity: {0}", mpxTask);
	}

	private boolean isRootTask(net.sf.mpxj.Task mpxTask) {
		return mpxTask.getOutlineNumber()!=null && mpxTask.getOutlineLevel() == 0;
	}

	private void registerRootTask(net.sf.mpxj.Task mpxTask) {
		if (mpxRootTask==null)
			mpxRootTask=mpxTask;
	}

	private Task importRegularTask(Project project, net.sf.mpxj.Task mpxTask, Task parentTask) {
		Task task = createTask(project, mpxTask, parentTask);
		if (mpxTask.getExternalTask()) {
			task.setExternal(true);
			task.setProjectId(externalProjectId(mpxTask));
			task.setExternalProjectFile(externalProjectFile(mpxTask));
			project.addExternalTask(task);
		}
		updateEarliestTaskStart(task, mpxTask.getStart());
		state.mapTask(mpxTask, task);
		importTaskSnapshots(mpxTask, task);
		importAssignments(mpxTask, task);
		// MS Project stores task % complete separately from assignment % work
		// complete. Restore it after assignments have been imported.
		if (task instanceof NormalTask && mpxTask.getPercentageComplete() != null)
			((NormalTask) task).setImportedPercentComplete(mpxTask.getPercentageComplete().doubleValue() / 100.0D);
		if (task instanceof NormalTask && mpxTask.getPercentageWorkComplete() != null)
			((NormalTask) task).setImportedPercentWorkComplete(mpxTask.getPercentageWorkComplete().doubleValue() / 100.0D);
		return task;
	}

	private long externalProjectId(net.sf.mpxj.Task mpxTask) {
		String externalProject=mpxTask.getExternalTaskProject();
		if (externalProject==null)
			externalProject=mpxTask.getSubprojectFile();
		if (externalProject==null)
			return 0L;
		try {
			return Long.parseLong(externalProject);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private String externalProjectFile(net.sf.mpxj.Task mpxTask) {
		String externalProject = mpxTask.getExternalTaskProject();
		if (isFileReference(externalProject))
			return externalProject;
		String subprojectFile = mpxTask.getSubprojectFile();
		return isFileReference(subprojectFile) ? subprojectFile : null;
	}

	private boolean isFileReference(String value) {
		if (value == null || value.isBlank())
			return false;
		try {
			Long.parseLong(value);
			return false;
		} catch (NumberFormatException e) {
			return true;
		}
	}

	private Task createTask(Project project, net.sf.mpxj.Task mpxTask, Task parentTask) {
		MpxTaskConverter converter=new MpxTaskConverter();
		Node parentNode = parentTask == null ? null : project.getTaskModel().search(parentTask);
		Node node = project.createLocalTaskNode(parentNode, false);
		Task task = (Task) node.getImpl();
		if (mpxTask.getSubProject() != null) {
			project.getTasks().remove(task);
			SubProj subproject = project.getSubprojectHandler().createSubProj(0L);
			task = (Task) subproject;
			node.setImpl(task);
			project.connectTask(task);
			task.setWbsParent(parentTask);
		}
		converter.from(mpxTask, task, state);
		if (task instanceof SubProj subproject) {
			task.setSubprojectFile(mpxTask.getSubprojectFile());
			((DefaultSubProj) subproject).setSubprojectReadOnly(mpxTask.getSubprojectReadOnly());
		}
		return task;
	}

	private void updateEarliestTaskStart(Task task, Date importedStart) {
		// Task.setStart() records a scheduling constraint while importing instead
		// of directly setting the calculated schedule. Read MPXJ's source date here
		// so a task before the project header start can move the project boundary
		// before the final recalculation. Reading task.getStart() at this point
		// only sees the default project start and loses the imported earlier date.
		final long taskStart = importedStart == null ? task.getStart()
			: DateUTCConverter.toModelTime(importedStart);
		if (taskStart == 0L) {
			return;
		}
		if (earliestTaskStart == -1L || taskStart < earliestTaskStart)
			earliestTaskStart = taskStart;
	}

	private void importTaskSnapshots(net.sf.mpxj.Task mpxTask, Task task) {
		// Slot 11 is the current snapshot. MSPDI baseline 0..10 map directly to
		// the microProject baseline indices 0..10; keep the live schedule intact.
		for (int snapshotId = 0; snapshotId <= 10; snapshotId++) {
			Date start = getTaskBaselineStart(mpxTask, snapshotId);
			Date finish = getTaskBaselineFinish(mpxTask, snapshotId);
			Duration duration = getTaskBaselineDuration(mpxTask, snapshotId);
			MpxImportState.RawMspdiTaskBaseline rawBaseline = state.getRawMspdiTaskBaseline(
				mpxTask.getUniqueID() == null ? null : Long.valueOf(mpxTask.getUniqueID().longValue()), snapshotId);
			if (start == null && finish == null && duration == null && rawBaseline == null)
				continue;

			TaskSnapshot snapshot = new TaskSnapshot();
			// Snapshot clone and serialization helpers expect the assignment holder
			// to be initialized even when this baseline contains task dates only.
			snapshot.getHasAssignments();
			TaskSchedule schedule = new TaskSchedule(task, TaskSchedule.Kind.CURRENT);
			if (start != null)
				schedule.setStart(DateUTCConverter.toModelTime(start));
			if (finish != null)
				schedule.setFinish(DateUTCConverter.toModelTime(finish));
			if (rawBaseline != null) {
				schedule.setRawDuration(toRawBaselineDuration(rawBaseline));
			} else if (duration != null) {
				schedule.setRawDuration(MpxUtils.toProjectDuration(duration, task.getProject().getDurationDefaults()));
			}
			snapshot.setCurrentSchedule(schedule);
			task.setSnapshot(Integer.valueOf(snapshotId), snapshot);
		}
	}

	private Date getTaskBaselineStart(net.sf.mpxj.Task mpxTask, int snapshotId) {
		if (snapshotId==0)
			return mpxTask.getBaselineStart();
		return mpxTask.getBaselineStart(snapshotId);
	}

	private Date getTaskBaselineFinish(net.sf.mpxj.Task mpxTask, int snapshotId) {
		if (snapshotId==0)
			return mpxTask.getBaselineFinish();
		return mpxTask.getBaselineFinish(snapshotId);
	}

	private Duration getTaskBaselineDuration(net.sf.mpxj.Task mpxTask, int snapshotId) {
		if (snapshotId==0)
			return mpxTask.getBaselineDuration();
		return mpxTask.getBaselineDuration(snapshotId);
	}
	
	protected void importAssignments(net.sf.mpxj.Task mpxTask, Task task) {
		for (net.sf.mpxj.ResourceAssignment mpxAssignment : mpxTask.getResourceAssignments()) {
			if (isUnassignedAssignment(mpxAssignment))
				continue;
			MpxAssignmentConverter converter = new MpxAssignmentConverter();
			Assignment assignment = Assignment.getInstance(task, ResourceImpl.getUnassignedInstance(), 0, 0);
			converter.from(mpxAssignment, assignment, state, task, 0);
			((NormalTask) task).addAssignment(assignment);
			// Attaching a non-default assignment can copy the default assignment's
			// detail object and erase this imported value.
			converter.restoreLevelingDelay(mpxAssignment, assignment, state);
			converter.importBaselines(mpxAssignment, assignment, state);
			importedAssignmentLevelingDelays.add(new ImportedAssignmentLevelingDelay(mpxAssignment, assignment));
		}
	}

	private boolean isUnassignedAssignment(net.sf.mpxj.ResourceAssignment assignment) {
		Integer resourceUniqueId = assignment.getResourceUniqueID();
		return resourceUniqueId == null || resourceUniqueId.intValue() == EnterpriseResource.UNASSIGNED_ID;
	}

	private Date getAssignmentBaselineStart(net.sf.mpxj.ResourceAssignment mpxAssignment, int snapshotId) {
		if (snapshotId==0)
			return mpxAssignment.getBaselineStart();
		return mpxAssignment.getBaselineStart(snapshotId);
	}

	private Date getAssignmentBaselineFinish(net.sf.mpxj.ResourceAssignment mpxAssignment, int snapshotId) {
		if (snapshotId==0)
			return mpxAssignment.getBaselineFinish();
		return mpxAssignment.getBaselineFinish(snapshotId);
	}

	private Duration getAssignmentBaselineWork(net.sf.mpxj.ResourceAssignment mpxAssignment, int snapshotId) {
		if (snapshotId==0)
			return mpxAssignment.getBaselineWork();
		return mpxAssignment.getBaselineWork(snapshotId);
	}
	
	protected void importDependencies(Project project) {
		MpxDependencyConverter converter=new MpxDependencyConverter();
		for (net.sf.mpxj.Task mpxTask : mpxProjectFile.getTasks()){
//			if (mpxTask.getNull() || mpxTask.getID()==null)
//				continue;
			if (mpxTask==mpxRootTask)
				continue;
			List<Relation> mpxRelations=mpxTask.getPredecessors();
			if (mpxRelations==null) {
				logger.log(Level.FINE, "Task has no predecessors to import: {0}", mpxTask);
				continue;
			}
			for (Relation mpxRelation : mpxRelations){
				Dependency dependency=converter.from(mpxRelation, state);
				if (dependency!=null) {
					// dependency registered via DependencyService in converter
			}
		}
	}
	}

	public interface ProgressClosure{
		public void updateProgress(float progress, String label);
	}

	
	

	
	

}
