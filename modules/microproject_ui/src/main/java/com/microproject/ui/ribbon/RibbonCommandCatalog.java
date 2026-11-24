/*******************************************************************************
 * MIT License
 *
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
package com.microproject.ui.ribbon;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;

import com.microproject.ribbon.SwingRibbonModel;

/**
 * Canonical information architecture for commands displayed by the desktop ribbon.
 *
 * <p>The resource bundle controls visual order. This catalog controls ownership:
 * every command has a primary tab and only the shared Clipboard commands may also
 * appear in the other editing context.</p>
 */
final class RibbonCommandCatalog {
	enum CommandScope { GLOBAL, DOCUMENT, TASK, RESOURCE, REPORT, PROJECT, VIEW, FORMAT }

	record CommandDefinition(String id, String actionId, String iconKey,
		CommandScope scope, String primaryTab, Set<String> permittedTabs) {
		public CommandDefinition {
			permittedTabs = Set.copyOf(permittedTabs);
		}
	}

	private record Placement(CommandScope scope, String primaryTab, Set<String> permittedTabs) {
	}

	private static final String FILE = "FileRibbonTask";
	private static final String TASK = "TaskRibbonTask";
	private static final String RESOURCE = "ResourceRibbonTask";
	private static final String REPORT = "ReportRibbonTask";
	private static final String PROJECT = "ProjectRibbonTask";
	private static final String VIEW = "ViewRibbonTask";
	private static final String HELP = "HelpRibbonTask";
	private static final String FORMAT = "FormatRibbonTask";
	private static final String NETWORK_FORMAT = "NetworkFormatRibbonTask";
	private static final String CALENDAR_FORMAT = "CalendarFormatRibbonTask";
	private static final String TIMELINE_FORMAT = "TimelineFormatRibbonTask";
	private static final String TEAM_PLANNER_FORMAT = "TeamPlannerFormatRibbonTask";
	private static final String RESOURCE_GRAPH_FORMAT = "ResourceGraphFormatRibbonTask";
	private static final String TASK_SHEET_FORMAT = "TaskSheetFormatRibbonTask";
	private static final String TASK_USAGE_FORMAT = "TaskUsageFormatRibbonTask";
	private static final String RESOURCE_SHEET_FORMAT = "ResourceSheetFormatRibbonTask";
	private static final String RESOURCE_USAGE_FORMAT = "ResourceUsageFormatRibbonTask";
	private static final String QUICK_ACCESS = "QuickAccessToolbar";
	private static final Map<String, Placement> PLACEMENTS = placements();

	private RibbonCommandCatalog() {
	}

	static List<CommandDefinition> from(SwingRibbonModel model) {
		return from(model, new ResourceBundle[0]);
	}

	static List<CommandDefinition> from(SwingRibbonModel model, ResourceBundle... bundles) {
		return definitions(tabsByCommand(model), bundles);
	}

	private static List<CommandDefinition> definitions(Map<String, Set<String>> tabsByCommand, ResourceBundle... bundles) {
		List<CommandDefinition> result = new ArrayList<>(tabsByCommand.size());
		for (String id : tabsByCommand.keySet()) {
			Placement placement = PLACEMENTS.get(id);
			if (placement == null) {
				throw new IllegalStateException("Ribbon command is not cataloged: " + id);
			}
			result.add(new CommandDefinition(id, resolve(id + ".action", bundles),
				resolve(id + ".icon", bundles), placement.scope(), placement.primaryTab(), placement.permittedTabs()));
		}
		return List.copyOf(result);
	}

	static void validate(SwingRibbonModel model) {
		validate(model, new ResourceBundle[0]);
	}

	static void validate(SwingRibbonModel model, ResourceBundle... bundles) {
		Map<String, Set<String>> tabsByCommand = tabsByCommand(model);
		for (CommandDefinition definition : definitions(tabsByCommand, bundles)) {
			if (bundles.length > 0 && (definition.actionId() == null || definition.iconKey() == null)) {
				throw new IllegalStateException("Ribbon command is missing action or icon metadata: " + definition.id());
			}
			Set<String> actualTabs = tabsByCommand.get(definition.id());
			if (!definition.permittedTabs().containsAll(actualTabs)
				|| !actualTabs.contains(definition.primaryTab())) {
				throw new IllegalStateException("Ribbon command has invalid tab placement: " + definition.id()
					+ " expected " + definition.permittedTabs() + " but was " + actualTabs);
			}
		}
	}

	private static Map<String, Set<String>> tabsByCommand(SwingRibbonModel model) {
		int commandOccurrences = model.getTaskBarButtons().size();
		for (SwingRibbonModel.RibbonTab tab : model.getTabs()) {
			for (SwingRibbonModel.RibbonBand band : tab.getBands()) {
				commandOccurrences += band.getButtons().size();
			}
		}
		Map<String, Set<String>> tabsByCommand = LinkedHashMap.newLinkedHashMap(commandOccurrences);
		for (SwingRibbonModel.RibbonTab tab : model.getTabs()) {
			for (SwingRibbonModel.RibbonBand band : tab.getBands()) {
				for (SwingRibbonModel.RibbonButton button : band.getButtons()) {
					tabsByCommand.computeIfAbsent(button.getId(), ignored -> new LinkedHashSet<>()).add(tab.getId());
				}
			}
		}
		for (String buttonId : model.getTaskBarButtons()) {
			tabsByCommand.computeIfAbsent(buttonId, ignored -> new LinkedHashSet<>()).add(QUICK_ACCESS);
		}
		return tabsByCommand;
	}

	private static Map<String, Placement> placements() {
		Map<String, Placement> result = new LinkedHashMap<>();
		register(result, CommandScope.GLOBAL, FILE,
			"RibbonNewProject", "RibbonNewMasterProject", "RibbonOpenProject", "RibbonRecentProjects", "RibbonImportProject",
			"RibbonBackstageOptions", "RibbonLocale");
		register(result, CommandScope.GLOBAL, HELP,
			"RibbonMicroProjectDocumentation", "RibbonAboutMicroProject");
		register(result, CommandScope.DOCUMENT, FILE,
			"RibbonSaveProject", "RibbonSaveProjectAs", "RibbonSaveMpoAs", "RibbonCloseProject", "RibbonExportProject",
			"RibbonBackstageProjectInformation", "RibbonPrint", "RibbonPrintPreview", "RibbonPDF");
		register(result, CommandScope.TASK, TASK,
					"RibbonInsert", "RibbonInsertRecurring", "RibbonIndent", "RibbonOutdent",
					"RibbonMoveTaskUp", "RibbonMoveTaskDown",
					"RibbonLink", "RibbonUnlink", "RibbonTaskSplit", "RibbonRespectLinks",
					"RibbonDelegateTasks", "RibbonTaskInformation", "RibbonTaskInspector", "RibbonNotes", "RibbonUpdateTasks", "RibbonMarkOnTrack", "RibbonDelete",
					"RibbonFind", "RibbonScrollToTask", "RibbonScrollToToday", "RibbonTaskModeManual", "RibbonTaskModeAutomatic",
					"RibbonFormatPainter",
					"RibbonInactivateTask",
					"RibbonInsertMilestone", "RibbonInsertSummary", "RibbonClear", "RibbonSelectAll", "RibbonTaskDetails",
					"RibbonPercentComplete0", "RibbonPercentComplete25", "RibbonPercentComplete50", "RibbonPercentComplete75", "RibbonPercentComplete100",
					"RibbonHideSelectedTasks", "RibbonShowAllTasks");
		register(result, CommandScope.RESOURCE, RESOURCE,
			"RibbonInsertResource", "RibbonResourceInformation", "RibbonTimesheet", "RibbonTeamFilter",
			"RibbonLevelSelection", "RibbonLevelResource", "RibbonLevelAll", "RibbonClearLevelSelection", "RibbonClearLevelAll",
			"RibbonLevelResources", "RibbonNextOverallocation",
			"RibbonUseResourcePool", "RibbonCreateResourcePool", "RibbonRefreshResourcePool");
		register(result, CommandScope.TASK, RESOURCE, "RibbonAssignResources");
		register(result, CommandScope.TASK, TASK, "RibbonBold", "RibbonItalic", "RibbonFontColor", "RibbonFont", "RibbonFill",
			"RibbonFontName", "RibbonFontSize", "RibbonUnderline", "RibbonFillColor",
			"RibbonMoveTask", "RibbonScheduleMode", "RibbonAddToTimeline");
		register(result, CommandScope.VIEW, TASK, "RibbonTaskViewGantt");
		register(result, CommandScope.VIEW, RESOURCE, "RibbonResourceViews");
		register(result, CommandScope.RESOURCE, RESOURCE, "RibbonResourceNotes", "RibbonResourceDetails");
		register(result, CommandScope.TASK, PROJECT, "RibbonInsertProject");
		// Custom Fields is also a Columns-group command of the Gantt Chart Format tab.
		register(result, CommandScope.TASK, PROJECT, Set.of(PROJECT, FORMAT), "RibbonCustomFields");
		register(result, CommandScope.TASK, VIEW, "RibbonExpand", "RibbonCollapse");
		register(result, CommandScope.REPORT, REPORT,
			"RibbonReport", "RibbonCustomReport", "RibbonHistogram", "RibbonCharts", "RibbonTaskUsage", "RibbonResourceUsage", "RibbonCCPMBufferStatus",
			"RibbonReportDashboards", "RibbonReportResources", "RibbonReportInProgress", "RibbonReportCustom", "RibbonReportRecent");
		register(result, CommandScope.PROJECT, PROJECT,
			"RibbonProjectInformation", "RibbonProjectsDialog", "RibbonChangeWorkingTime",
			"RibbonCalendarOptions", "RibbonStatusDate", "RibbonUpdateProject", "RibbonMoveProject", "RibbonRecalculate", "RibbonRefreshSubprojects", "RibbonOpenSubproject", "RibbonRemoveSubproject", "RibbonSaveBaseline", "RibbonClearBaseline",
			"RibbonCCPMSettings", "RibbonCCPMClear");
	register(result, CommandScope.VIEW, VIEW,
			"RibbonGantt", "RibbonTrackingGantt", "RibbonTaskSheet", "RibbonNetwork", "RibbonWBS", "RibbonResources", "RibbonRBS",
			"RibbonTimeline", "RibbonCalendarView", "RibbonProjects", "RibbonTaskUsageDetail", "RibbonResourceUsageDetail", "RibbonDetails", "RibbonNoTextNoSubWindow",
			"RibbonArrangeAll", "RibbonChooseFilter", "RibbonChooseSort", "RibbonChooseGroup", "RibbonZoomIn", "RibbonZoomOut", "RibbonZoomEntireProject", "RibbonZoomSelectedTasks", "RibbonCCPMNetwork", "RibbonPrivacyMask",
			"RibbonTeamPlanner", "RibbonTimescaleGallery", "RibbonSwitchWindows");
		register(result, CommandScope.FORMAT, FORMAT,
			"RibbonToggleProgressLine", "RibbonLabelResourceNames", "RibbonLabelTaskName", "RibbonGridlines",
			"RibbonToggleCriticalChain", "RibbonTimescale", "RibbonBarStyles", "RibbonTextStyles", "RibbonLayout");
		// Gantt Chart Format tab commands; they act on the active Gantt view and appear on no other tab.
		register(result, CommandScope.FORMAT, FORMAT,
			"RibbonInsertColumn", "RibbonAlignLeft", "RibbonAlignCenter", "RibbonAlignRight", "RibbonColumnSettings",
			"RibbonHideColumn", "RibbonFieldSettings", "RibbonFormatBarStyles", "RibbonFormatBar", "RibbonBarStylesItem", "RibbonCriticalTasks", "RibbonSlack", "RibbonLateTasks", "RibbonTaskPath", "RibbonPathPredecessors", "RibbonPathDrivingPredecessors", "RibbonPathSuccessors", "RibbonPathDrivenSuccessors", "RibbonPathRemove", "RibbonShowBaseline", "RibbonSlippage", "RibbonGanttStyleGallery", "RibbonOutlineNumbers", "RibbonSummaryTasks", "RibbonGanttStyleDefault", "RibbonGanttStyleBlue", "RibbonGanttStyleGreen", "RibbonGanttStyleOrange", "RibbonGanttStylePurple", "RibbonGanttStyleGray");
		// Table-view Format tabs reuse the Gantt Columns and Show/Hide commands; each acts on the active spreadsheet view.
				for (String id : List.of("RibbonInsertColumn", "RibbonAlignLeft", "RibbonAlignCenter", "RibbonAlignRight",
					"RibbonColumnSettings", "RibbonHideColumn", "RibbonFieldSettings", "RibbonCustomFields")) {
			Placement placement = result.get(id);
			Set<String> tabs = new java.util.HashSet<>(placement.permittedTabs());
			tabs.addAll(Set.of(TASK_SHEET_FORMAT, TASK_USAGE_FORMAT, RESOURCE_SHEET_FORMAT, RESOURCE_USAGE_FORMAT));
			result.put(id, new Placement(placement.scope(), placement.primaryTab(), Set.copyOf(tabs)));
		}
		register(result, CommandScope.FORMAT, TASK_USAGE_FORMAT, Set.of(TASK_USAGE_FORMAT, RESOURCE_USAGE_FORMAT),
			"RibbonAssignmentInformation");
		for (String id : List.of("RibbonOutlineNumbers", "RibbonSummaryTasks")) {
			Placement placement = result.get(id);
			result.put(id, new Placement(placement.scope(), placement.primaryTab(),
				"RibbonSummaryTasks".equals(id) ? Set.of(FORMAT, TASK_SHEET_FORMAT, TASK_USAGE_FORMAT, NETWORK_FORMAT)
					: Set.of(FORMAT, TASK_SHEET_FORMAT, TASK_USAGE_FORMAT)));
		}
		register(result, CommandScope.FORMAT, NETWORK_FORMAT, Set.of(NETWORK_FORMAT), "RibbonNetworkLayoutNow", "RibbonNetworkBoxFieldsCollapsed",
			"RibbonNetworkBoxFormat", "RibbonNetworkBoxStyles", "RibbonNetworkLayout", "RibbonNetworkLinkLabels", "RibbonNetworkStraightLinks",
			"RibbonNetworkProgressMarks");
		register(result, CommandScope.FORMAT, CALENDAR_FORMAT, Set.of(CALENDAR_FORMAT), "RibbonCalendarTextStyles", "RibbonCalendarBarStyles",
			"RibbonCalendarLayoutNow", "RibbonCalendarWeekHeightBestFit", "RibbonCalendarLayout", "RibbonCalendarTaskList");
		register(result, CommandScope.FORMAT, TIMELINE_FORMAT, Set.of(TIMELINE_FORMAT), "RibbonTimelineTextStyles", "RibbonTimelineDateRange",
			"RibbonTimelineDates", "RibbonTimelineExpand", "RibbonTimelineBarLabels", "RibbonTimelineOverlapped", "RibbonTimelinePanZoom",
			"RibbonTimelineTextLines", "RibbonTimelineInsertBar", "RibbonTimelineAddExisting", "RibbonTimelineInsertTask",
			"RibbonTimelineInsertCallout", "RibbonTimelineInsertMilestone", "RibbonTimelineDisplay", "RibbonTimelineCallout",
			"RibbonTimelineRemove", "RibbonTimelineCopy");
		// The Timeline Format Font group reuses the Task tab font commands; they act on the tasks selected on the timeline.
		for (String id : List.of("RibbonFontName", "RibbonFontSize", "RibbonBold", "RibbonItalic", "RibbonUnderline",
			"RibbonFillColor", "RibbonFontColor", "RibbonFont")) {
			Placement placement = result.get(id);
			Set<String> tabs = new java.util.HashSet<>(placement.permittedTabs());
			tabs.add(TIMELINE_FORMAT);
			result.put(id, new Placement(placement.scope(), placement.primaryTab(), Set.copyOf(tabs)));
		}
		register(result, CommandScope.FORMAT, RESOURCE_GRAPH_FORMAT, Set.of(RESOURCE_GRAPH_FORMAT),
			"RibbonResourceGraphTextStyles", "RibbonResourceGraphGridlines", "RibbonResourceGraphBarStyles", "RibbonResourceGraphPrevious", "RibbonResourceGraphNext", "RibbonResourceGraphData", "RibbonResourceGraphValues",
			"RibbonResourceGraphPeakUnits", "RibbonResourceGraphWork", "RibbonResourceGraphCumulativeWork", "RibbonResourceGraphOverallocation", "RibbonResourceGraphPercentAllocation",
			"RibbonResourceGraphRemainingAvailability", "RibbonResourceGraphCost", "RibbonResourceGraphCumulativeCost", "RibbonResourceGraphWorkAvailability", "RibbonResourceGraphUnitAvailability");
		register(result, CommandScope.FORMAT, TEAM_PLANNER_FORMAT, Set.of(TEAM_PLANNER_FORMAT), "RibbonTeamPlannerOutline",
			"RibbonTeamPlannerGridlines", "RibbonTeamPlannerTextStyles", "RibbonTeamPlannerTextLines", "RibbonTeamPlannerSelectedStyle",
			"RibbonTeamPlannerAutoStyle", "RibbonTeamPlannerManualStyle", "RibbonTeamPlannerActualStyle", "RibbonTeamPlannerExternalStyle",
			"RibbonTeamPlannerLateStyle", "RibbonTeamPlannerPreventOverallocation", "RibbonTeamPlannerExpandRows",
			"RibbonTeamPlannerUnassigned", "RibbonTeamPlannerUnscheduled");
		register(result, CommandScope.FORMAT, TASK_USAGE_FORMAT, Set.of(TASK_SHEET_FORMAT, TASK_USAGE_FORMAT, RESOURCE_SHEET_FORMAT, RESOURCE_USAGE_FORMAT, CALENDAR_FORMAT),
			"RibbonSheetGridlines");
		// Usage Details rows are shared by the two usage Format tabs.
		register(result, CommandScope.FORMAT, TASK_USAGE_FORMAT, Set.of(TASK_USAGE_FORMAT, RESOURCE_USAGE_FORMAT),
			"RibbonUsageDetailWork", "RibbonUsageDetailActualWork", "RibbonUsageDetailBaselineWork", "RibbonUsageDetailCost",
			"RibbonUsageDetailActualCost", "RibbonUsageDetailsAdd", "RibbonUsageDetailCumulativeWork");
		// Overallocation and Remaining Availability only exist for resources.
		register(result, CommandScope.FORMAT, RESOURCE_USAGE_FORMAT, Set.of(RESOURCE_USAGE_FORMAT),
			"RibbonUsageDetailOverallocation", "RibbonUsageDetailRemainingAvailability");
		register(result, CommandScope.DOCUMENT, TASK, Set.of(TASK, RESOURCE), "RibbonPaste", "RibbonCopy", "RibbonCut");
		register(result, CommandScope.DOCUMENT, QUICK_ACCESS,
			"RibbonTopBarSaveProject", "RibbonTopBarUndo", "RibbonTopBarRedo");
		return Map.copyOf(result);
	}

	/** Default tab IDs; the File tab is fixed. */
	static List<String> defaultTabIds() {
		return List.of(FILE, TASK, RESOURCE, REPORT, PROJECT, VIEW, HELP, FORMAT, NETWORK_FORMAT, CALENDAR_FORMAT,
			TIMELINE_FORMAT, TEAM_PLANNER_FORMAT, RESOURCE_GRAPH_FORMAT, TASK_SHEET_FORMAT, TASK_USAGE_FORMAT,
			RESOURCE_SHEET_FORMAT, RESOURCE_USAGE_FORMAT);
	}

	private static final List<String> QUICK_ACCESS_CANDIDATES = PLACEMENTS.keySet().stream().sorted().toList();
	private static final Set<String> QUICK_ACCESS_CANDIDATE_IDS = Set.copyOf(QUICK_ACCESS_CANDIDATES);

	static List<String> quickAccessCandidates() {
		return QUICK_ACCESS_CANDIDATES;
	}

	static boolean isQuickAccessCandidate(String commandId) {
		return QUICK_ACCESS_CANDIDATE_IDS.contains(commandId);
	}

	private static void register(Map<String, Placement> placements, CommandScope scope, String tab, String... ids) {
		register(placements, scope, tab, Set.of(tab), ids);
	}

	private static void register(Map<String, Placement> placements, CommandScope scope, String primaryTab,
		Set<String> permittedTabs, String... ids) {
		for (String id : ids) {
			Placement previous = placements.put(id, new Placement(scope, primaryTab, permittedTabs));
			if (previous != null) {
				throw new IllegalStateException("Ribbon command is cataloged more than once: " + id);
			}
		}
	}

	private static String resolve(String key, ResourceBundle... bundles) {
		for (ResourceBundle bundle : bundles) {
			try {
				String value = bundle.getString(key);
				if (value != null && !value.isBlank()) {
					return value.trim();
				}
			} catch (MissingResourceException ignored) {
			}
		}
		return null;
	}
}
