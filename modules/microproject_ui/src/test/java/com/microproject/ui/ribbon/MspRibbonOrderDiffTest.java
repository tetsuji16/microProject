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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.microproject.ui.menu.ExtToolBarFactory;
import com.microproject.ui.menu.MenuActionMapSupport;
import com.microproject.ui.menu.MenuManager;
import com.microproject.ui.menu.MenuRibbonCommandSource;
import com.microproject.ui.menu.testsupport.MenuDefinitionSupport;
import com.microproject.ribbon.SwingRibbonModel;

/**
 * Order contract between the core ribbon tabs and Microsoft Project's published
 * ribbon layout (issue #1166, phase 1a).
 *
 * <p>{@code msp-ribbon-order.csv} is derived from Microsoft's published control list: rows are in
 * ribbon order, {@code position} counts the controls of a group in document order (menu children
 * included), and {@code parent} names the menu or gallery a child belongs to. Where Microsoft lists a
 * Standard and a Professional layout of one group, the Professional (non-Standard) group is used.
 * The test asserts, for the Task, Resource, Report, Project, View and Help tabs, that (a) our bands
 * follow Microsoft's group order, (b) every control that maps to a Microsoft control sits in the band
 * of Microsoft's group for it and in Microsoft's relative order, and (c) the items of a split button or
 * menu keep Microsoft's relative order.</p>
 *
 * <p>Everything outside that contract is an explicit, reasoned exception below. An exception
 * never excuses the order of a mapped control, and an exception that is no longer true fails the
 * test, so the lists can only shrink.</p>
 */
class MspRibbonOrderDiffTest {
	private static final String ORDER = "msp-ribbon-order.csv";
	private static final String CONTROL_SPEC = "msp-ribbon-control-spec.csv";

	private static final String PHASE_1B = "not implemented yet; phase 1b, #1166";
	private static final String ONLINE = "Project Online / Project Server scope is outside the standalone desktop goal (AGENTS.md)";
	/** Out-of-goal scope exclusions (not deferred work). */
	private static final String AGILE = "scope exclusion: Agile/Scrum features are phase 4 and outside the current compatibility goal";
	private static final String OFFICE_ADDIN = "scope exclusion: local Office add-ins / VBA / COM automation stay unresolved goal rows (AGENTS.md)";
	private static final String REPORT_ITEMS =
		"report galleries list the existing product reports; Microsoft's only child is More Reports";
	private static final String VIEW_GALLERY_ITEMS =
		"view gallery lists product views; Microsoft's children are Reset/Save/More; " + PHASE_1B;
	private static final String LIST_ITEMS = "list items; Microsoft's children are Clear/New/More; " + PHASE_1B;

	/** Our core tab -> Microsoft tab. */
	private static final String ASSIGNMENT_PROPERTIES =
		"assignments have no Assignment Information dialog and no notes of their own; Task and Resource Information are reached from the Task and Resource tabs";
	private static final String TEXT_STYLES_SHEET =
		"the Text Styles dialog only chooses which field labels the Gantt bars (task name or resource names); it has no per-item-type cell font to apply to sheet or usage rows";
	private static final String NO_FREE_PLACEMENT =
		"Network Diagram boxes are placed automatically by dependency level; there is no free box placement, so alignment of selected boxes has no engine";
	private static final String NO_PROJECT_SUMMARY =
		"the task model has no project summary task (task 0) row to show; the project is not a task";
	private static final String WRAP_TEXT =
		"cells render on one line; the sheet has no multi-line cell/row-height engine (only header wrapping exists, under Field Settings)";

	/**
	 * Microsoft tabs that belong to connected services or cloud features outside the standalone desktop goal (AGENTS.md).
	 */
	private static final Map<String, String> EXCLUDED_TABS = Map.of(
		"TabResourcePlanViewFormat",
		"scope exclusion: Resource Plan view belongs to Project Online / Server resource engagements (AGENTS.md)",
		"TabEngagementsFormat",
		"scope exclusion: " + ONLINE + " (engagements)",
		"TabShare",
		"scope exclusion: connected cloud sharing and email routes are outside the standalone desktop goal (AGENTS.md)",
		"TabPublish",
		"scope exclusion: Project Server / Project Online publishing is outside the standalone desktop goal (AGENTS.md)");

	/**
	 * Microsoft desktop tabs that are not yet implemented or whose feature engines/views remain unresolved gaps.
	 */
	private static final Map<String, String> UNRESOLVED_TABS = Map.ofEntries(
		Map.entry("TabDeveloper",
			OFFICE_ADDIN),
		Map.entry("TabTaskFormViewFormat",
			"the product has no task form lower-pane split view (only task dialogs exist), so its contextual format tab has no host view; phase 2, #1166 / #1180"),
		Map.entry("TabResourceFormViewFormat",
			"the product has no resource form lower-pane split view (only resource dialogs exist), so its contextual format tab has no host view; phase 2, #1166 / #1180"),
		Map.entry("TabCompareProjects",
			"the Compare Projects feature is not implemented yet; phase 1b, #1166"),
		Map.entry("TabProjectScrum",
			AGILE),
		Map.entry("TabProjectKanban",
			AGILE),
		Map.entry("TabTaskBoardViewFormat",
			AGILE),
		Map.entry("TabReportToolsDesign",
			"no interactive report designer exists (report preview is static); phase 3, #1166"),
		Map.entry("TabTableToolsDesign",
			"no interactive report designer table design exists; phase 3, #1166"),
		Map.entry("TabTableToolsLayout",
			"no interactive report designer table layout exists; phase 3, #1166"),
		Map.entry("TabChartToolsDesignNew",
			"no interactive report designer chart design exists; phase 3, #1166"),
		Map.entry("TabChartToolsFormatNew",
			"no interactive report designer chart formatting exists; phase 3, #1166"),
		Map.entry("TabDrawingToolsFormat",
			"no report drawing/shapes layer exists; phase 3, #1166"),
		Map.entry("TabPictureToolsFormat",
			"no report picture editing layer exists; phase 3, #1166"),
		Map.entry("TabRecent",
			"Backstage Open / Recent page is handled by Backstage view (#1116) rather than a ribbon tab"),
		Map.entry("TabSave",
			"Backstage Save / Save As page is handled by Backstage view (#1119) rather than a ribbon tab"),
		Map.entry("TabHelp",
			"Backstage Help page is handled by Backstage view rather than a ribbon tab"));

	private static final Map<String, String> TABS = Map.ofEntries(
		Map.entry("TaskRibbonTask", "TabTask"),
		Map.entry("ResourceRibbonTask", "TabResource"),
		Map.entry("ReportRibbonTask", "TabReportOverview"),
		Map.entry("ProjectRibbonTask", "TabProject"),
		Map.entry("ViewRibbonTask", "TabView"),
		Map.entry("HelpRibbonTask", "HelpTab"),
		// contextual tabs: add one entry here (plus its bands below) to compare another one
		Map.entry("FormatRibbonTask", "TabGanttChartFormat"),
		Map.entry("TaskSheetFormatRibbonTask", "TabTaskSheetViewFormat"),
		Map.entry("TaskUsageFormatRibbonTask", "TabTaskUsageViewFormat"),
		Map.entry("ResourceSheetFormatRibbonTask", "TabResourceSheetViewFormat"),
		Map.entry("ResourceUsageFormatRibbonTask", "TabResourceUsageViewFormat"),
		Map.entry("NetworkFormatRibbonTask", "TabNetworkDiagramFormat"),
		Map.entry("CalendarFormatRibbonTask", "TabCalendarFormat"),
		Map.entry("TimelineFormatRibbonTask", "TabTimelineViewFormat"),
		Map.entry("TeamPlannerFormatRibbonTask", "TabTeamPlannerFormat"),
		Map.entry("ResourceGraphFormatRibbonTask", "TabResourceGraphFormat"));

	/** Our band ("tab/band") -> the Microsoft group of the same tab it implements. */
	private static final Map<String, String> BAND_GROUPS = Map.ofEntries(
		Map.entry("TaskRibbonTask/TaskViewRibbonBand", "GroupTaskViewSimple"),
		Map.entry("TaskRibbonTask/TaskClipboardRibbonBand", "GroupClipboard"),
		Map.entry("TaskRibbonTask/TaskFontRibbonBand", "GroupFont"),
		Map.entry("TaskRibbonTask/TaskTrackingRibbonBand", "GroupTaskSchedule"),
		Map.entry("TaskRibbonTask/TaskTasksRibbonBand", "GroupTasksLayout"),
		Map.entry("TaskRibbonTask/TaskInsertRibbonBand", "GroupTaskInsert"),
		Map.entry("TaskRibbonTask/TaskPropertiesRibbonBand", "GroupTaskProperties"),
		Map.entry("TaskRibbonTask/TaskEditingRibbonBand", "GroupEditing"),
		Map.entry("ResourceRibbonTask/ResourceViewRibbonBand", "GroupResourceViewSimple"),
		Map.entry("ResourceRibbonTask/ResourceAssignmentsRibbonBand", "GroupAssignments"),
		Map.entry("ResourceRibbonTask/ResourceInsertRibbonBand", "GroupResourceInsert"),
		Map.entry("ResourceRibbonTask/ResourcePropertiesRibbonBand", "GroupResourceProperties"),
		Map.entry("ResourceRibbonTask/ResourceLevelRibbonBand", "GroupLevel"),
		Map.entry("ReportRibbonTask/ReportViewsRibbonBand", "GroupViewReports"),
		Map.entry("ProjectRibbonTask/ProjectInsertRibbonBand", "GroupProjectInsert"),
		Map.entry("ProjectRibbonTask/ProjectInfoRibbonBand", "GroupProjectProperties"),
		Map.entry("ProjectRibbonTask/ProjectScheduleRibbonBand", "GroupProjectSchedule"),
		Map.entry("ProjectRibbonTask/ProjectStatusRibbonBand", "GroupProjectStatus"),
		Map.entry("ViewRibbonTask/ViewTaskRibbonBand", "GroupTaskViews"),
		Map.entry("ViewRibbonTask/ViewResourceRibbonBand", "GroupResourceViews"),
		Map.entry("ViewRibbonTask/ViewDataRibbonBand", "GroupData"),
		Map.entry("ViewRibbonTask/ViewZoomRibbonBand", "GroupZoom"),
		Map.entry("ViewRibbonTask/ViewSplitRibbonBand", "GroupViewSplit"),
		Map.entry("ViewRibbonTask/ViewWindowRibbonBand", "GroupWindow"),
		Map.entry("HelpRibbonTask/HelpRibbonBand", "GroupHelpAndSupport"),
		Map.entry("FormatRibbonTask/GanttFormatRibbonBand", "GroupGanttChartFormat"),
		Map.entry("FormatRibbonTask/SheetColumnsRibbonBand", "GroupColumns"),
		Map.entry("FormatRibbonTask/GanttBarStylesRibbonBand", "GroupGanttChartBarStyles"),
		Map.entry("FormatRibbonTask/GanttStylesRibbonBand", "GroupGanttChartSyles"),
		Map.entry("FormatRibbonTask/SheetShowHideRibbonBand", "GroupGanttChartShowHide"),
		Map.entry("TaskSheetFormatRibbonTask/SheetFormatRibbonBand", "GroupSheetFormat"),
		Map.entry("TaskSheetFormatRibbonTask/SheetColumnsRibbonBand", "GroupColumns"),
		Map.entry("TaskSheetFormatRibbonTask/SheetShowHideRibbonBand", "GroupGanttChartShowHide"),
		Map.entry("TaskUsageFormatRibbonTask/SheetFormatRibbonBand", "GroupUsageViewFormat"),
		Map.entry("TaskUsageFormatRibbonTask/SheetColumnsRibbonBand", "GroupColumns"),
		Map.entry("TaskUsageFormatRibbonTask/TaskUsageDetailsRibbonBand", "GroupTaskUsageViewDetails"),
				Map.entry("TaskUsageFormatRibbonTask/AssignmentInformationRibbonBand", "GroupAssignmentProperties"),
				Map.entry("TaskUsageFormatRibbonTask/SheetShowHideRibbonBand", "GroupTaskUsageShowHide"),
				Map.entry("ResourceSheetFormatRibbonTask/SheetFormatRibbonBand", "GroupSheetFormat"),
				Map.entry("ResourceSheetFormatRibbonTask/SheetColumnsRibbonBand", "GroupColumns"),
				Map.entry("ResourceUsageFormatRibbonTask/SheetFormatRibbonBand", "GroupUsageViewFormat"),
				Map.entry("ResourceUsageFormatRibbonTask/SheetColumnsRibbonBand", "GroupColumns"),
				Map.entry("ResourceUsageFormatRibbonTask/ResourceUsageDetailsRibbonBand", "GroupResourceUsageViewDetails"),
				Map.entry("ResourceUsageFormatRibbonTask/AssignmentInformationRibbonBand", "GroupAssignmentProperties"),
		Map.entry("NetworkFormatRibbonTask/NetworkDisplayRibbonBand", "GroupNetworkDiagramDisplay"),
		Map.entry("NetworkFormatRibbonTask/NetworkFormatRibbonBand", "GroupNetworkDiagramFormat"),
		Map.entry("NetworkFormatRibbonTask/NetworkShowHideRibbonBand", "GroupNetworkDiagramShowHide"),
		Map.entry("NetworkFormatRibbonTask/NetworkLayoutRibbonBand", "GroupNetworkDiagramLayout"),
		Map.entry("CalendarFormatRibbonTask/CalendarFormatRibbonBand", "GroupCalendarViewFormat"),
		Map.entry("CalendarFormatRibbonTask/CalendarLayoutRibbonBand", "GroupCalendarViewLayout"),
		Map.entry("CalendarFormatRibbonTask/CalendarShowHideRibbonBand", "GroupCalendarViewShowHide"),
		Map.entry("TimelineFormatRibbonTask/TimelineTextStylesRibbonBand", "GroupTimelineViewTextStyles"),
		Map.entry("TimelineFormatRibbonTask/TimelineFontRibbonBand", "GroupFont"),
		Map.entry("TimelineFormatRibbonTask/TimelineShowHideRibbonBand", "GroupTimelineViewShowHide"),
		Map.entry("TimelineFormatRibbonTask/TimelineInsertRibbonBand", "GroupTimelineViewInsert"),
		Map.entry("TimelineFormatRibbonTask/TimelineSelectionRibbonBand", "GroupTimelineViewCurrentSelection"),
		Map.entry("TimelineFormatRibbonTask/TimelineCopyRibbonBand", "GroupTimelineCopy"),
		Map.entry("TeamPlannerFormatRibbonTask/TeamPlannerFormatRibbonBand", "GroupTeamPlannerFormat"),
		Map.entry("TeamPlannerFormatRibbonTask/TeamPlannerStylesRibbonBand", "GroupStylesEdit"),
		Map.entry("TeamPlannerFormatRibbonTask/TeamPlannerScheduleRibbonBand", "GroupTeamPlannerSchedule"),
		Map.entry("TeamPlannerFormatRibbonTask/TeamPlannerShowHideRibbonBand", "GroupTeamPlannerShowHide"),
		Map.entry("ResourceGraphFormatRibbonTask/ResourceGraphFormatRibbonBand", "GroupResourceGraphFormat"),
		Map.entry("ResourceGraphFormatRibbonTask/ResourceGraphNavigateRibbonBand", "GroupResourceGraphNavigate"),
		Map.entry("ResourceGraphFormatRibbonTask/ResourceGraphDataRibbonBand", "GroupResourceGraphData"));

	/**
	 * Bands with no group of their own on that Microsoft tab. They stay, are placed after every
	 * Microsoft group of the tab, and the value says why. A band that carries mapped controls is
	 * compared with the Microsoft group named in {@link #BORROWED_GROUPS}.
	 */
	private static final Map<String, String> EXTENSION_BANDS = Map.of(
		"ResourceRibbonTask/ClipboardRibbonBand",
		"Resource-sheet clipboard convenience; Microsoft has Clipboard only on the Task tab",
		"ReportRibbonTask/ReportAnalysisRibbonBand",
		"microProject usage and critical-chain buffer reports; no Microsoft report group of that shape",
		"ProjectRibbonTask/ProjectCcpmRibbonBand",
		"CCPM is a supported microProject extension with no Microsoft equivalent",
		"ResourceGraphFormatRibbonTask/ResourceGraphShowHideRibbonBand",
		"the Values toggle is a one-click form of the ShowValues setting that Microsoft keeps in the Resource Graph Bar Styles dialog; Microsoft has no Show/Hide group on that tab",
		"FormatRibbonTask/GanttFormatExtrasRibbonBand",
		"annotation labels and the CCPM critical chain are microProject Gantt options with no Microsoft Gantt Chart Format group");

	/** Extension bands whose mapped controls are ordered like this Microsoft group ("tab/group"). */
	private static final Map<String, String> BORROWED_GROUPS = Map.of(
			"ResourceRibbonTask/ClipboardRibbonBand", "TabTask/GroupClipboard",
			"TaskUsageFormatRibbonTask/TaskUsageDetailsRibbonBand", "TabTaskUsageViewFormat/GroupAssignmentProperties",
			"ResourceUsageFormatRibbonTask/ResourceUsageDetailsRibbonBand", "TabResourceUsageViewFormat/GroupAssignmentProperties");

	/** Microsoft groups of the compared tabs that we have no band for ("tab/group"), with the reason. */
	private static final Map<String, String> MISSING_GROUPS = Map.ofEntries(
			Map.entry("TabReportOverview/GroupCompareProject", "Compare Projects; " + PHASE_1B),
			Map.entry("TabReportOverview/GroupVisualReports",
				"desktop Visual Reports and local Excel/Visio integration remain unverified"),
			Map.entry("TabProject/GroupOfficeExtension",
				OFFICE_ADDIN),
			Map.entry("TabProject/GroupProofing", "Spelling; " + PHASE_1B),
			Map.entry("TabGanttChartFormat/GroupDrawings",
				"no drawing layer: the Gantt has no drawing objects (lines, arrows, shapes, text boxes) to create, edit, order or persist"),
			Map.entry("TabTimelineViewFormat/GroupSwitchTimeline",
				"the lower pane offers only the Timeline and Details check boxes; there is no lower-pane view picker, so the Timeline label, the view gallery and More Views have nothing to switch between"),
			Map.entry("TabView/GroupMacros",
				"Microsoft Project-compatible VBA/macros remain unresolved"));

	/** Controls Microsoft lists in two groups of one tab; one command implements both and sits in the first group; the second listing is exempt only while the command is mapped somewhere in the tab. */
		private static final Set<String> MSP_LISTS_TWICE = Set.of("TabNetworkDiagramFormat/NetworkDiagramLayout");

		/** Controls that Microsoft places in a different group than the band they appear in. Key: "band/control", value: "tab/group". */
		private static final Map<String, String> CONTROL_BORROWED_GROUPS = Map.of(
			"TaskUsageFormatRibbonTask/TaskUsageDetailsRibbonBand/RibbonAssignmentInformation", "TabTaskUsageViewFormat/GroupAssignmentProperties",
			"ResourceUsageFormatRibbonTask/ResourceUsageDetailsRibbonBand/RibbonAssignmentInformation", "TabResourceUsageViewFormat/GroupAssignmentProperties");

		/**
		 * Top-level Microsoft controls inside a group we implement that no control of that band maps to,
	 * with the reason. A control counts as present when it or one of its menu children is mapped.
	 */
	private static final Map<String, String> MISSING_CONTROLS = Map.ofEntries(
		Map.entry("TabNetworkDiagramFormat/ProjectSummaryTaskShow", NO_PROJECT_SUMMARY),
		Map.entry("TabCalendarFormat/ProjectSummaryTaskShow", NO_PROJECT_SUMMARY),
		Map.entry("TabNetworkDiagramFormat/NetworkDiagramAlignMenu", NO_FREE_PLACEMENT),
		Map.entry("TabResourceGraphFormat/GraphLabel",
			"static caption of the Graph gallery; the Graph menu button carries its own caption"),
		Map.entry("TabTask/DeliverablesMenu", ONLINE + " (deliverables)"),
		Map.entry("TabResource/AddEngagement", "scope exclusion: " + ONLINE + " (engagements)"),
		Map.entry("TabReportOverview/CreateNewReportMenu",
			"no report designer: Custom Report builds task reports only, so Blank/Chart/Table/Comparison report authoring has no engine"),
		Map.entry("TabReportOverview/ReportCostGallery", "no cost report exists in the report engine to list"),
		Map.entry("TabReportOverview/ReportGettingStartedGallery", "no Getting Started report templates exist to list"),
		Map.entry("TabReportOverview/ReportAgileGallery", AGILE),
		Map.entry("TabProject/SelectAgileMethodology", AGILE),
		Map.entry("TabProject/ManageSprints", AGILE),
		Map.entry("TabProject/LinksBetweenProjects", "external-task placeholders live in a private ExternalTaskManager with no query/re-point API, so a Change Links dialog cannot be backed by the engine yet"),
		Map.entry("TabProject/WbsMenu", "WBS codes are derived from the outline only; core has no WBS code mask or renumber facility to back Define Code/Renumber"),
		Map.entry("TabProject/StatusDateLabel", "the Status Date button already shows the date (StatusDateControlUpdater); a separate non-interactive label control needs ribbon label support; " + PHASE_1B),
		Map.entry("TabGanttChartFormat/WrapText",
			"cells render on one line; the sheet has no multi-line cell/row-height engine (only header wrapping exists, under Field Settings)"),
		Map.entry("TabGanttChartFormat/ProjectSummaryTaskShow",
			"the task model has no project summary task (task 0) row to show; the project is not a task"),
		Map.entry("TabTaskSheetViewFormat/TextStyles", TEXT_STYLES_SHEET),
		Map.entry("TabTaskSheetViewFormat/WrapText", WRAP_TEXT),
		Map.entry("TabTaskSheetViewFormat/ProjectSummaryTaskShow",
			"the task model has no project summary task (task 0) row to show; the project is not a task"),
		Map.entry("TabTaskUsageViewFormat/TextStyles", TEXT_STYLES_SHEET),
		Map.entry("TabResourceSheetViewFormat/TextStyles", TEXT_STYLES_SHEET),
		Map.entry("TabResourceUsageViewFormat/TextStyles", TEXT_STYLES_SHEET),
		Map.entry("TabTaskUsageViewFormat/WrapText", WRAP_TEXT),
		Map.entry("TabResourceSheetViewFormat/WrapText", WRAP_TEXT),
		Map.entry("TabResourceUsageViewFormat/WrapText", WRAP_TEXT),
		Map.entry("TabTaskUsageViewFormat/ProjectSummaryTaskShow",
			"the task model has no project summary task (task 0) row to show; the project is not a task"),
		Map.entry("TabView/TaskBoardViewGallery", AGILE),
		Map.entry("TabView/TaskViewsOtherGallery", "no further task views exist beyond those in the Task Views gallery"),
		Map.entry("TabView/ResourcePlanViewGallery", "scope exclusion: Resource Plan view belongs to Project Online / Server resource engagements (AGENTS.md)"),
		Map.entry("TabView/ResourceOtherViewGallery", "no further resource views exist beyond those in the Resource Views gallery"),
		Map.entry("TabView/TablesGallery", "views have no named table definitions to switch; columns are edited per view"),
		Map.entry("TabView/HighlightFiltersGroup", "filters only hide rows; the engine has no highlight (row emphasis) filter"),
		Map.entry("TabView/TimescaleLabel", "static label control is not supported by the ribbon; the Timescale list shows the current value"),
		Map.entry("TabView/GalleryTimelineView", "the lower pane offers only the Timeline and Details check boxes; no lower-pane view picker exists"),
		Map.entry("TabView/ViewDetailsGallery", "the lower pane offers only the Details check box; no lower-pane view picker exists"),
		Map.entry("TabView/WindowNew", "one document frame per project (addProjectFrame reuses it); a second window on the same project is not supported"),
		Map.entry("TabView/HideWindowMenu", "the frame manager has no hidden-window state to back Hide/Unhide"),
		Map.entry("HelpTab/OfficeFeedbackHelpTab", "Office feedback service is outside the standalone desktop goal"),
		Map.entry("HelpTab/ShowTraining", "Microsoft training content is an online service; no local equivalent and no network calls are added"),
		Map.entry("HelpTab/WhatsNewRecentUpdates", "Microsoft update announcements are an online service; no local equivalent and no network calls are added"),
				Map.entry("TabTaskUsageViewFormat/AssignmentNotes", "Assignment Notes is a separate command not yet implemented"),
				Map.entry("TabResourceUsageViewFormat/AssignmentNotes", "Assignment Notes is a separate command not yet implemented"));

	private record Extension(String after, String reason) {
	}

	/** Anchor meaning "after every mapped control of the band". */
	private static final String END = "END";

	private static Extension after(String mspControl, String reason) {
		return new Extension(mspControl, reason);
	}

	/**
	 * microProject commands without a Microsoft control (blank {@code mspControlId} in
	 * msp-ribbon-control-spec.csv) on the compared tabs. Each is placed directly after the named
	 * Microsoft control of its band, or after all mapped controls when the anchor is {@link #END}.
	 */
	private static final Map<String, Extension> EXTENSIONS = Map.ofEntries(
		Map.entry("RibbonMoveTaskUp", after("ScheduleModeMenu", "move-in-outline commands of the Tasks group")),
		Map.entry("RibbonMoveTaskDown", after("ScheduleModeMenu", "move-in-outline commands of the Tasks group")),
		Map.entry("RibbonDelegateTasks", after("ScheduleModeMenu", "task delegation, closest to the Tasks group")),
		Map.entry("RibbonDelete", after("EditFillMenu", "row deletion next to Clear")),
		Map.entry("RibbonSelectAll", after("EditFillMenu", "selection command of Editing")),
		Map.entry("RibbonScrollToToday", after("EditFillMenu", "Go To companion of Editing")),
		Map.entry("RibbonHideSelectedTasks", after("EditFillMenu", "row visibility command of Editing")),
		Map.entry("RibbonShowAllTasks", after("EditFillMenu", "row visibility command of Editing")),
		Map.entry("RibbonCreateResourcePool", after("ResourcesShare", "resource pool creation, next to sharing a pool")),
		Map.entry("RibbonTimesheet", after("ResourcePoolRefresh", "assignment-related command with no Microsoft control")),
		Map.entry("RibbonTeamFilter", after("ResourcePoolRefresh", "assignment-related command with no Microsoft control")),
		Map.entry("RibbonClearLevelSelection", after("LevelNow", "clear-leveling variant for the selection")),
		Map.entry("RibbonReport", after(END, "report entry points; Microsoft's report galleries are phase 1b, #1166")),
		Map.entry("RibbonCustomReport", after(END, "report entry points; Microsoft's report galleries are phase 1b, #1166")),
		Map.entry("RibbonHistogram", after(END, "report entry points; Microsoft's report galleries are phase 1b, #1166")),
		Map.entry("RibbonCharts", after(END, "report entry points; Microsoft's report galleries are phase 1b, #1166")),
		Map.entry("RibbonRefreshSubprojects", after("SubprojectInsert", "subproject maintenance next to Insert Subproject")),
		Map.entry("RibbonOpenSubproject", after("SubprojectInsert", "subproject maintenance next to Insert Subproject")),
		Map.entry("RibbonRemoveSubproject", after("SubprojectInsert", "subproject maintenance next to Insert Subproject")),
		Map.entry("RibbonCalendarOptions", after("ChangeWorkingTime", "calendar settings next to Change Working Time")),
		Map.entry("RibbonProjectsDialog", after("ChangeWorkingTime", "project list dialog of Properties")),
		Map.entry("RibbonTrackingGantt", after("CalendarViewGallery", "Gantt variant; no Microsoft gallery entry point")),
		Map.entry("RibbonTaskSheet", after("CalendarViewGallery", "Task Sheet view; Microsoft lists it under Other Views")),
		Map.entry("RibbonWBS", after("CalendarViewGallery", "WBS view; no Microsoft gallery entry point")),
		Map.entry("RibbonCCPMNetwork", after("CalendarViewGallery", "CCPM view, a supported extension")),
		Map.entry("RibbonRBS", after("ResourceSheetViewGallery", "RBS view; no Microsoft gallery entry point")),
		Map.entry("RibbonProjects", after("ResourceSheetViewGallery", "projects view; no Microsoft gallery entry point")),
		Map.entry("RibbonPrivacyMask", after("WindowsArrangeAll", "window-level display toggle")),
				Map.entry("RibbonAboutMicroProject", after(END, "product About; no Microsoft control mapped")));

	/** Items of popups (by ribbon id) -> the Microsoft menu child they correspond to. */
	private static final Map<String, String> ITEM_CONTROLS = Map.ofEntries(
		Map.entry("RibbonPaste", "Paste"),
		Map.entry("RibbonPasteSpecial", "PasteSpecialDialog"),
		Map.entry("RibbonCopy", "Copy"),
		Map.entry("RibbonCopyPicture", "CopyPicture"),
		Map.entry("RibbonFind", "FindDialog"),
		Map.entry("RibbonReplace", "ReplaceDialog"),
		Map.entry("RibbonInspectTask", "TaskInspect"),
		Map.entry("RibbonShowTaskWarnings", "TaskWarningsShow"),
		Map.entry("RibbonShowTaskSuggestions", "TaskSuggestionsShow"),
		Map.entry("RibbonShowIgnoredProblems", "IgnoredTaskWarningsShow"),
		Map.entry("RibbonMarkOnTrack", "UpdateAsScheduled"),
		Map.entry("RibbonUpdateTasks", "TasksUpdate"),
		Map.entry("RibbonInsert", "TaskInsert"),
		Map.entry("RibbonInsertRecurring", "RecurringTaskInsert"),
		Map.entry("RibbonInsertBlankRow", "BlankRowInsert"),
		Map.entry("RibbonSaveBaseline", "BaselineSave"),
		Map.entry("RibbonClearBaseline", "BaselineClear"),
		Map.entry("RibbonMoveForwardDay", "TaskMoveForwardOneDay"),
		Map.entry("RibbonMoveForwardWeek", "TaskMoveForwardOneWeek"),
		Map.entry("RibbonMoveForwardFourWeeks", "TaskMoveForwardFourWeeks"),
		Map.entry("RibbonMoveForwardCustom", "TaskMoveForwardCustom"),
		Map.entry("RibbonMoveBackDay", "TaskMoveBackOneDay"),
		Map.entry("RibbonMoveBackWeek", "TaskMoveBackOneWeek"),
		Map.entry("RibbonMoveBackFourWeeks", "TaskMoveBackFourWeeks"),
		Map.entry("RibbonMoveBackCustom", "TaskMoveBackCustom"),
		Map.entry("RibbonMoveWhenResourcesAvailable", "ScheduleTaskWhenResourcesAvailable"),
		Map.entry("RibbonFillDown", "FillDown"),
		Map.entry("RibbonFillLeft", "FillLeft"),
		Map.entry("RibbonFillRight", "FillRight"),
		Map.entry("RibbonFillUp", "FillUp"),
		Map.entry("RibbonSortBy", "SortByDialog"),
		Map.entry("RibbonTimescale", "Timescale"),
		Map.entry("RibbonGridlines", "GridlinesProject"),
		Map.entry("RibbonToggleProgressLine", "ProgressLines"),
		Map.entry("RibbonHideColumn", "ColumnHideProject"),
		Map.entry("RibbonFieldSettings", "FieldSettings"),
		Map.entry("RibbonFormatBar", "GanttBarFormat"),
		Map.entry("RibbonBarStylesItem", "BarStylesFormat"),
		Map.entry("RibbonPathPredecessors", "HighlightPredecessors"),
		Map.entry("RibbonPathDrivingPredecessors", "HighlightDrivingPredecessors"),
		Map.entry("RibbonPathSuccessors", "HighlightSuccessors"),
		Map.entry("RibbonPathDrivenSuccessors", "HighlightDrivenSuccessors"),
		Map.entry("RibbonPathRemove", "RemoveTaskPathHighlight"),
		Map.entry("RibbonTimelineShowTaskDates", "TimelineShowTaskDates"),
		Map.entry("RibbonTimelineShowToday", "TimelineShowToday"),
		Map.entry("RibbonTimelineShowTimescale", "TimelineViewTimescaleShow"),
		Map.entry("RibbonTimelineShowProgress", "TimelineShowTaskProgress"),
		Map.entry("RibbonTimelineCopyEmail", "TimelineViewCopyForEmail"),
		Map.entry("RibbonTimelineCopyPresentation", "TimelineViewCopyForPresentation"),
		Map.entry("RibbonTimelineCopyFullSize", "TimelineViewCopyFullSize"),
		Map.entry("RibbonTeamPlannerOutlineAll", "TeamPlannerOutlineShowAll"),
		Map.entry("RibbonTeamPlannerSelectedFill", "TeamPlannerTaskFillColorGallery"),
		Map.entry("RibbonTeamPlannerSelectedBorder", "TeamPlannerTaskBorderColorGallery"),
		Map.entry("RibbonTeamPlannerAutoBorder", "TeamPlannerAutoScheduledBorder"),
		Map.entry("RibbonTeamPlannerAutoFill", "TeamPlannerAutoScheduledFillColor"),
		Map.entry("RibbonTeamPlannerAutoReset", "TeamPlannerAutoScheduleResetToDefault"),
		Map.entry("RibbonTeamPlannerManualBorder", "TeamPlannerManualBorder"),
		Map.entry("RibbonTeamPlannerManualFill", "TeamPlannerManuallyScheduledFillColor"),
		Map.entry("RibbonTeamPlannerManualReset", "TeamPlannerManuallyScheduleResetToDefault"),
		Map.entry("RibbonTeamPlannerActualBorder", "TeamPlannerActualBorderColor"),
		Map.entry("RibbonTeamPlannerActualFill", "TeamPlannerActualFillColor"),
		Map.entry("RibbonTeamPlannerActualReset", "TeamPlannerActualResetToDefault"),
		Map.entry("RibbonTeamPlannerExternalBorder", "TeamPlannerExternalBorder"),
		Map.entry("RibbonTeamPlannerExternalFill", "TeamPlannerExternalFillColor"),
		Map.entry("RibbonTeamPlannerExternalReset", "TeamPlannerExternalResetToDefault"),
		Map.entry("RibbonTeamPlannerLateBorder", "TeamPlannerLateBorder"),
		Map.entry("RibbonTeamPlannerLateFill", "TeamPlannerLateFillColor"),
		Map.entry("RibbonTeamPlannerLateReset", "TeamPlannerLateResetToDefault"),
		Map.entry("RibbonTeamPlannerOutline1", "TeamPlannerOutlineLevel1"),
		Map.entry("RibbonTeamPlannerOutline2", "TeamPlannerOutlineLevel2"),
		Map.entry("RibbonTeamPlannerOutline3", "TeamPlannerOutlineLevel3"),
		Map.entry("RibbonTeamPlannerOutline4", "TeamPlannerOutlineLevel4"),
		Map.entry("RibbonTeamPlannerOutline5", "TeamPlannerOutlineLevel5"),
		Map.entry("RibbonTeamPlannerOutline6", "TeamPlannerOutlineLevel6"),
		Map.entry("RibbonTeamPlannerOutline7", "TeamPlannerOutlineLevel7"),
		Map.entry("RibbonTeamPlannerOutline8", "TeamPlannerOutlineLevel8"),
		Map.entry("RibbonTeamPlannerOutline9", "TeamPlannerOutlineLevel9"));

	/**
	 * Popups (by ribbon id) with items that have no Microsoft child to be ordered against, with the
	 * reason. Items listed in {@link #ITEM_CONTROLS} are always checked.
	 */
	private static final Map<String, String> UNORDERED_ITEM_POPUPS = Map.ofEntries(
		Map.entry("RibbonResourceGraphData",
			"the menu lists the ten Graph options; the published control list gives GraphViewGallery no children"),
		Map.entry("RibbonClear",
			"Microsoft's Clear menu children (Formatting, Hyperlinks, Notes, All, Row); " + PHASE_1B),
		Map.entry("RibbonInsertResource", "Microsoft's Add Resources menu children; " + PHASE_1B),
		Map.entry("RibbonTaskViewGantt", VIEW_GALLERY_ITEMS),
		Map.entry("RibbonResourceViews", VIEW_GALLERY_ITEMS),
		Map.entry("RibbonGantt", VIEW_GALLERY_ITEMS),
		Map.entry("RibbonCalendarView", VIEW_GALLERY_ITEMS),
		Map.entry("RibbonNetwork", VIEW_GALLERY_ITEMS),
		Map.entry("RibbonTaskUsageDetail", VIEW_GALLERY_ITEMS),
		Map.entry("RibbonResources", VIEW_GALLERY_ITEMS),
		Map.entry("RibbonResourceUsageDetail", VIEW_GALLERY_ITEMS),
		Map.entry("RibbonFontName", "the font list is the installed font families; only the More Fonts entry is a command"),
		Map.entry("RibbonFontSize", "the size list is a fixed set of sizes; only the More Font Sizes entry is a command"),
		Map.entry("RibbonScheduleMode",
			"Microsoft's Mode menu lists project-start/finish scheduling choices; the Manually/Auto Schedule items are mode commands"),
		Map.entry("RibbonReportDashboards", REPORT_ITEMS),
		Map.entry("RibbonReportResources", REPORT_ITEMS),
		Map.entry("RibbonReportInProgress", REPORT_ITEMS),
		Map.entry("RibbonReportCustom", REPORT_ITEMS),
		Map.entry("RibbonTeamPlanner", VIEW_GALLERY_ITEMS),
		Map.entry("RibbonChooseFilter", LIST_ITEMS),
		Map.entry("RibbonChooseSort", "sort presets are product commands; only Sort By maps"),
		Map.entry("RibbonChooseGroup", LIST_ITEMS),
		Map.entry("RibbonGanttStyleGallery",
			"colour-scheme presets of the Gantt Chart Style gallery; Microsoft's gallery lists its own bar-style presets"));

	private record OrderRow(String tab, String group, int position, String idMso, String parent) {
	}

	private record OurControl(String ribbonId, String idMso, List<String> itemIds) {
		boolean mapped() {
			return !idMso.isEmpty();
		}
	}

	private interface BandVisitor {
		void visit(String tab, SwingRibbonModel.RibbonBand band, List<OurControl> controls);
	}

	private static List<OrderRow> order;
	private static Map<String, String> mspIds;
	private static Map<String, List<SwingRibbonModel.RibbonBand>> bandsByTab;

	@BeforeAll
	static void load() throws IOException {
		order = readOrder();
		mspIds = readControlSpec();
		ExtToolBarFactory factory = new ExtToolBarFactory(MenuActionMapSupport.noopActionMap(),
			MenuDefinitionSupport.ribbonBundles(Locale.ROOT));
		SwingRibbonModel model = new SwingRibbonFactory(new MenuRibbonCommandSource(factory),
			MenuDefinitionSupport.ribbonBundles(Locale.ROOT)).createModel(MenuManager.STANDARD_RIBBON);
		bandsByTab = new LinkedHashMap<>();
		for (SwingRibbonModel.RibbonTab tab : model.getTabs()) {
			if (TABS.containsKey(tab.getId())) {
				bandsByTab.put(tab.getId(), tab.getBands());
			}
		}
	}

	@Test
	void orderResourceCoversCoreAndContextualTabSetsWithoutStandardVariants() {
		Set<String> tabs = new TreeSet<>();
		for (OrderRow row : order) {
			tabs.add(row.tab());
		}
		assertTrue(tabs.containsAll(TABS.values()), "core tabs missing from " + ORDER + ": " + tabs);
		assertTrue(tabs.containsAll(Set.of("TabGanttChartFormat", "TabNetworkDiagramFormat", "TabCalendarFormat",
			"TabTaskUsageViewFormat", "TabResourceUsageViewFormat", "TabResourceSheetViewFormat", "TabTimelineViewFormat",
			"TabTeamPlannerFormat", "TabReportToolsDesign")), "contextual tabs missing from " + ORDER);
		assertTrue(order.stream().noneMatch(row -> row.group().contains("Standard")),
			"Standard layout variants must be left out in favour of the Professional group");
		assertEquals(bandsByTab.keySet(), TABS.keySet(), "a compared core tab is missing from the ribbon");
	}

	@Test
	void everyMicrosoftTabIsComparedOrHasADocumentedExclusionOrUnresolvedReason() {
		Set<String> allTabs = new TreeSet<>();
		for (OrderRow row : order) {
			allTabs.add(row.tab());
		}
		Set<String> accountedFor = new TreeSet<>();
		accountedFor.addAll(TABS.values());
		accountedFor.addAll(EXCLUDED_TABS.keySet());
		accountedFor.addAll(UNRESOLVED_TABS.keySet());

		Set<String> unaccounted = new TreeSet<>(allTabs);
		unaccounted.removeAll(accountedFor);
		assertTrue(unaccounted.isEmpty(),
			() -> "Microsoft Project tabs without an order comparison, documented exclusion, or unresolved gap: " + unaccounted);

		Set<String> overlapTabsExcluded = new TreeSet<>(TABS.values());
		overlapTabsExcluded.retainAll(EXCLUDED_TABS.keySet());
		assertTrue(overlapTabsExcluded.isEmpty(), () -> "TABS overlaps with EXCLUDED_TABS: " + overlapTabsExcluded);

		Set<String> overlapTabsUnresolved = new TreeSet<>(TABS.values());
		overlapTabsUnresolved.retainAll(UNRESOLVED_TABS.keySet());
		assertTrue(overlapTabsUnresolved.isEmpty(), () -> "TABS overlaps with UNRESOLVED_TABS: " + overlapTabsUnresolved);

		Set<String> overlapExcludedUnresolved = new TreeSet<>(EXCLUDED_TABS.keySet());
		overlapExcludedUnresolved.retainAll(UNRESOLVED_TABS.keySet());
		assertTrue(overlapExcludedUnresolved.isEmpty(), () -> "EXCLUDED_TABS overlaps with UNRESOLVED_TABS: " + overlapExcludedUnresolved);

		Set<String> staleExcluded = new TreeSet<>(EXCLUDED_TABS.keySet());
		staleExcluded.removeAll(allTabs);
		assertTrue(staleExcluded.isEmpty(), () -> "EXCLUDED_TABS has entries not in " + ORDER + ": " + staleExcluded);

		Set<String> staleUnresolved = new TreeSet<>(UNRESOLVED_TABS.keySet());
		staleUnresolved.removeAll(allTabs);
		assertTrue(staleUnresolved.isEmpty(), () -> "UNRESOLVED_TABS has entries not in " + ORDER + ": " + staleUnresolved);
	}

	@Test
	void everyCoreTabHasItsBandsInMicrosoftGroupOrder() {
		List<String> problems = new ArrayList<>();
		for (Map.Entry<String, List<SwingRibbonModel.RibbonBand>> tab : bandsByTab.entrySet()) {
			List<String> groups = groups(TABS.get(tab.getKey()));
			int previous = -1;
			String previousBand = null;
			boolean extensionSeen = false;
			for (SwingRibbonModel.RibbonBand band : tab.getValue()) {
				String key = tab.getKey() + "/" + band.getId();
				String group = BAND_GROUPS.get(key);
				if (group == null) {
					if (!EXTENSION_BANDS.containsKey(key)) {
						problems.add(key + ": band is neither mapped to a Microsoft group nor a reasoned extension band");
					}
					extensionSeen = true;
					continue;
				}
				int index = groups.indexOf(group);
				if (index < 0) {
					problems.add(key + ": Microsoft group " + group + " is not on " + TABS.get(tab.getKey()));
				} else if (extensionSeen) {
					problems.add(key + ": follows an extension band; extension bands go after every Microsoft group");
				} else if (index <= previous) {
					problems.add(key + ": Microsoft puts " + group + " before " + previousBand);
				} else {
					previous = index;
					previousBand = band.getId();
				}
			}
		}
		assertTrue(problems.isEmpty(), String.join("\n", problems));
	}

	@Test
	void everyMicrosoftGroupIsABandOrAReasonedMissingGroup() {
		List<String> problems = new ArrayList<>();
		for (Map.Entry<String, List<SwingRibbonModel.RibbonBand>> tab : bandsByTab.entrySet()) {
			Set<String> present = new TreeSet<>();
			for (SwingRibbonModel.RibbonBand band : tab.getValue()) {
				String group = BAND_GROUPS.get(tab.getKey() + "/" + band.getId());
				if (group != null) {
					present.add(group);
				}
			}
			for (String group : groups(TABS.get(tab.getKey()))) {
				String key = TABS.get(tab.getKey()) + "/" + group;
				boolean missing = MISSING_GROUPS.containsKey(key);
				if (present.contains(group) && missing) {
					problems.add(key + " now has a band: remove it from MISSING_GROUPS");
				} else if (!present.contains(group) && !missing) {
					problems.add(key + " has no band and no MISSING_GROUPS reason");
				}
			}
		}
		for (String key : MISSING_GROUPS.keySet()) {
			String[] parts = key.split("/");
			if (!groups(parts[0]).contains(parts[1])) {
				problems.add("MISSING_GROUPS names unknown group " + key);
			}
		}
		assertTrue(problems.isEmpty(), String.join("\n", problems));
	}

	@Test
		void mappedControlsSitInMicrosoftGroupAndRelativeOrder() {
			List<String> problems = new ArrayList<>();
			forEachBand((tab, band, controls) -> {
				String key = tab + "/" + band.getId();
				String mspTab = TABS.get(tab);
				String group = BAND_GROUPS.get(key);
				if (group == null) {
					String borrowed = BORROWED_GROUPS.get(key);
					if (borrowed == null) {
						return;
					}
					mspTab = borrowed.split("/")[0];
					group = borrowed.split("/")[1];
				}
				// Track previous position per effective Microsoft group
				Map<String, Integer> previousByGroup = new HashMap<>();
				Map<String, String> previousIdByGroup = new HashMap<>();
				for (OurControl control : controls) {
					if (!control.mapped()) {
						continue;
					}
					// Check for control-specific borrowed group
					String controlKey = key + "/" + control.ribbonId();
					String controlBorrowed = CONTROL_BORROWED_GROUPS.get(controlKey);
					String effectiveMspTab = mspTab;
					String effectiveGroup = group;
					if (controlBorrowed != null) {
						effectiveMspTab = controlBorrowed.split("/")[0];
						effectiveGroup = controlBorrowed.split("/")[1];
					}
					OrderRow row = find(effectiveMspTab, effectiveGroup, control.idMso());
					if (row == null) {
						problems.add(key + ": " + control.ribbonId() + " maps to " + control.idMso()
							+ " which Microsoft does not place in " + effectiveGroup + "; it is in " + locate(control.idMso()));
					} else {
						String groupKey = effectiveMspTab + "/" + effectiveGroup;
						Integer previous = previousByGroup.get(groupKey);
						String previousId = previousIdByGroup.get(groupKey);
						if (previous != null && row.position() <= previous) {
							problems.add(key + ": Microsoft orders " + control.idMso() + " before " + previousId + " in " + effectiveGroup
								+ " (ours: " + control.ribbonId() + " after it)");
						} else {
							previousByGroup.put(groupKey, row.position());
							previousIdByGroup.put(groupKey, control.idMso());
						}
					}
				}
			});
			assertTrue(problems.isEmpty(), String.join("\n", problems));
		}

	@Test
	void microsoftControlsWeLackAreReasonedAndStillMissing() {
		List<String> problems = new ArrayList<>();
		Set<String> used = new TreeSet<>();
		Set<String> mappedInTab = new TreeSet<>();
		forEachBand((tab, band, controls) -> {
			for (OurControl control : controls) {
				if (control.mapped()) {
					mappedInTab.add(TABS.get(tab) + "/" + control.idMso());
				}
			}
		});
		forEachBand((tab, band, controls) -> {
			String group = BAND_GROUPS.get(tab + "/" + band.getId());
			if (group == null) {
				return;
			}
			String mspTab = TABS.get(tab);
			Set<String> mapped = new TreeSet<>();
			for (OurControl control : controls) {
				if (control.mapped()) {
					mapped.add(control.idMso());
				}
			}
			List<OrderRow> rows = rows(mspTab, group);
			for (OrderRow row : rows) {
				if (!row.parent().isEmpty()) {
					continue;
				}
				boolean present = mapped.contains(row.idMso());
				for (OrderRow child : rows) {
					present |= child.parent().equals(row.idMso()) && mapped.contains(child.idMso());
				}
				String key = mspTab + "/" + row.idMso();
				if (!present && MSP_LISTS_TWICE.contains(key) && mappedInTab.contains(key)) {
					continue;
				}
				if (present && MISSING_CONTROLS.containsKey(key)) {
					problems.add(key + " is now present: remove it from MISSING_CONTROLS");
				} else if (!present && !MISSING_CONTROLS.containsKey(key)) {
					problems.add(key + " (" + group + ") has no control in " + band.getId()
						+ " and no MISSING_CONTROLS reason");
				} else if (!present) {
					used.add(key);
				}
			}
		});
		Set<String> stale = new TreeSet<>(MISSING_CONTROLS.keySet());
		stale.removeAll(used);
		assertTrue(stale.isEmpty(), "MISSING_CONTROLS entries that match no missing control of an implemented group: " + stale);
		assertTrue(problems.isEmpty(), String.join("\n", problems));
	}

	@Test
	void ourControlsWithoutAMicrosoftControlAreReasonedAndPlacedAfterTheirAnchor() {
		List<String> problems = new ArrayList<>();
		Set<String> seen = new TreeSet<>();
		forEachBand((tab, band, controls) -> {
			String key = tab + "/" + band.getId();
			boolean extensionBand = EXTENSION_BANDS.containsKey(key);
			String lastMapped = null;
			for (int i = 0; i < controls.size(); i++) {
				OurControl control = controls.get(i);
				if (control.mapped()) {
					lastMapped = control.idMso();
					continue;
				}
				if (extensionBand) {
					continue;
				}
				seen.add(control.ribbonId());
				Extension extension = EXTENSIONS.get(control.ribbonId());
				if (extension == null) {
					problems.add(key + ": " + control.ribbonId() + " has no Microsoft control and no EXTENSIONS reason");
				} else if (END.equals(extension.after())) {
					for (int j = i + 1; j < controls.size(); j++) {
						if (controls.get(j).mapped()) {
							problems.add(key + ": " + control.ribbonId() + " must follow every mapped control");
							break;
						}
					}
				} else if (!extension.after().equals(lastMapped)) {
					problems.add(key + ": " + control.ribbonId() + " must follow " + extension.after()
						+ " but follows " + lastMapped);
				}
			}
		});
		Set<String> stale = new TreeSet<>(EXTENSIONS.keySet());
		stale.removeAll(seen);
		assertTrue(stale.isEmpty(), "EXTENSIONS entries that are no longer unmapped controls of a compared band: " + stale);
		assertTrue(problems.isEmpty(), String.join("\n", problems));
	}

	@Test
	void popupItemsKeepMicrosoftRelativeOrder() {
		List<String> problems = new ArrayList<>();
		Set<String> usedPopups = new TreeSet<>();
		forEachBand((tab, band, controls) -> {
			String key = tab + "/" + band.getId();
			String mspTab = TABS.get(tab);
			String group = BAND_GROUPS.get(key);
			if (group == null) {
				return;
			}
			for (OurControl control : controls) {
				int previous = 0;
				String previousId = null;
				boolean unordered = false;
				for (String item : control.itemIds()) {
					String child = ITEM_CONTROLS.get(item);
					if (child == null) {
						unordered = true;
						continue;
					}
					OrderRow row = find(mspTab, group, child);
					if (row == null || !row.parent().equals(control.idMso())) {
						problems.add(key + ": item " + item + " of " + control.ribbonId()
							+ " is not a Microsoft child of " + control.idMso());
					} else if (row.position() <= previous) {
						problems.add(key + ": Microsoft orders " + child + " before " + previousId + " in the "
							+ control.idMso() + " menu");
					} else {
						previous = row.position();
						previousId = child;
					}
				}
				if (unordered) {
					usedPopups.add(control.ribbonId());
					if (!UNORDERED_ITEM_POPUPS.containsKey(control.ribbonId())) {
						problems.add(key + ": " + control.ribbonId()
							+ " has items without a Microsoft child and no UNORDERED_ITEM_POPUPS reason");
					}
				}
			}
		});
		Set<String> stale = new TreeSet<>(UNORDERED_ITEM_POPUPS.keySet());
		stale.removeAll(usedPopups);
		assertTrue(stale.isEmpty(), "UNORDERED_ITEM_POPUPS entries that no longer have unmapped items: " + stale);
		assertTrue(problems.isEmpty(), String.join("\n", problems));
	}

	private static void forEachBand(BandVisitor visitor) {
		for (Map.Entry<String, List<SwingRibbonModel.RibbonBand>> tab : bandsByTab.entrySet()) {
			for (SwingRibbonModel.RibbonBand band : tab.getValue()) {
				List<OurControl> controls = new ArrayList<>();
				for (SwingRibbonModel.RibbonButton button : band.getButtons()) {
					controls.add(new OurControl(button.getId(), mspIds.getOrDefault(button.getId(), ""),
						button.getItemIds()));
				}
				visitor.visit(tab.getKey(), band, controls);
			}
		}
	}

	private static List<String> groups(String mspTab) {
		List<String> groups = new ArrayList<>();
		for (OrderRow row : order) {
			if (row.tab().equals(mspTab) && !groups.contains(row.group())) {
				groups.add(row.group());
			}
		}
		return groups;
	}

	private static List<OrderRow> rows(String mspTab, String group) {
		return order.stream().filter(row -> row.tab().equals(mspTab) && row.group().equals(group)).toList();
	}

	private static OrderRow find(String mspTab, String group, String idMso) {
		return rows(mspTab, group).stream().filter(row -> row.idMso().equals(idMso)).findFirst().orElse(null);
	}

	private static String locate(String idMso) {
		return order.stream().filter(row -> row.idMso().equals(idMso))
			.map(row -> row.tab() + "/" + row.group()).distinct().toList().toString();
	}

	private static List<OrderRow> readOrder() throws IOException {
		List<OrderRow> rows = new ArrayList<>();
		try (InputStream stream = MspRibbonOrderDiffTest.class.getResourceAsStream(ORDER)) {
			assertNotNull(stream, ORDER + " is missing");
			BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
			assertEquals("tab,group,position,idMso,parent", reader.readLine());
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.isBlank()) {
					continue;
				}
				String[] cells = line.split(",", -1);
				assertEquals(5, cells.length, "Malformed row: " + line);
				rows.add(new OrderRow(cells[0], cells[1], Integer.parseInt(cells[2]), cells[3], cells[4]));
			}
		}
		return rows;
	}

	private static Map<String, String> readControlSpec() throws IOException {
		Map<String, String> ids = new LinkedHashMap<>();
		try (InputStream stream = MspRibbonOrderDiffTest.class.getResourceAsStream(CONTROL_SPEC)) {
			assertNotNull(stream, CONTROL_SPEC + " is missing");
			BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
			reader.readLine();
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.isBlank()) {
					continue;
				}
				String[] cells = line.split(",", 7);
				ids.put(cells[0], cells[1]);
			}
		}
		return ids;
	}
}
