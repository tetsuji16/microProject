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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.microproject.menu.ExtToolBarFactory;
import com.microproject.menu.MenuActionMapSupport;
import com.microproject.menu.MenuManager;
import com.microproject.menu.MenuRibbonCommandSource;
import com.microproject.menu.testsupport.MenuDefinitionSupport;
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
	private static final String REPORT_GALLERIES = PHASE_1B + " (report galleries)";
	private static final String VIEW_GALLERY_ITEMS =
		"view gallery lists product views; Microsoft's children are Reset/Save/More; " + PHASE_1B;
	private static final String LIST_ITEMS = "list items; Microsoft's children are Clear/New/More; " + PHASE_1B;

	/** Our core tab -> Microsoft tab. */
	private static final Map<String, String> TABS = Map.of(
		"TaskRibbonTask", "TabTask",
		"ResourceRibbonTask", "TabResource",
		"ReportRibbonTask", "TabReportOverview",
		"ProjectRibbonTask", "TabProject",
		"ViewRibbonTask", "TabView",
		"HelpRibbonTask", "HelpTab");

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
		Map.entry("HelpRibbonTask/HelpRibbonBand", "GroupHelpAndSupport"));

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
		"CCPM is a supported microProject extension with no Microsoft equivalent");

	/** Extension bands whose mapped controls are ordered like this Microsoft group ("tab/group"). */
	private static final Map<String, String> BORROWED_GROUPS = Map.of(
		"ResourceRibbonTask/ClipboardRibbonBand", "TabTask/GroupClipboard");

	/** Microsoft groups of the compared tabs that we have no band for ("tab/group"), with the reason. */
	private static final Map<String, String> MISSING_GROUPS = Map.ofEntries(
		Map.entry("TabReportOverview/GroupCompareProject", "Compare Projects; " + PHASE_1B),
		Map.entry("TabReportOverview/GroupVisualReports",
			"scope exclusion: Visual Reports require external Excel/Visio (EXCLUDED in msp-ribbon-spec.csv)"),
		Map.entry("TabProject/GroupOfficeExtension",
			OFFICE_ADDIN),
		Map.entry("TabProject/GroupProofing", "Spelling; " + PHASE_1B),
		Map.entry("TabView/GroupMacros",
			"scope exclusion: " + "VBA/macros excluded by the #797 scope decision (EXCLUDED in msp-ribbon-spec.csv)"));

	/**
	 * Top-level Microsoft controls inside a group we implement that no control of that band maps to,
	 * with the reason. A control counts as present when it or one of its menu children is mapped.
	 */
	private static final Map<String, String> MISSING_CONTROLS = Map.ofEntries(
		Map.entry("TabTask/TaskSplit", "requires task split model; tracked separately"),
		Map.entry("TabTask/DeliverablesMenu", ONLINE + " (deliverables)"),
		Map.entry("TabResource/AddEngagement", "scope exclusion: " + ONLINE + " (engagements)"),
		Map.entry("TabReportOverview/CreateNewReportMenu", REPORT_GALLERIES),
		Map.entry("TabReportOverview/ReportDashboardGallery", REPORT_GALLERIES),
		Map.entry("TabReportOverview/ReportResourceGallery", REPORT_GALLERIES),
		Map.entry("TabReportOverview/ReportCostGallery", REPORT_GALLERIES),
		Map.entry("TabReportOverview/ReportInProgressGallery", REPORT_GALLERIES),
		Map.entry("TabReportOverview/ReportGettingStartedGallery", REPORT_GALLERIES),
		Map.entry("TabReportOverview/ReportCustomGallery", REPORT_GALLERIES),
		Map.entry("TabReportOverview/ReportRecentGallery", REPORT_GALLERIES),
		Map.entry("TabReportOverview/ReportAgileGallery", AGILE),
		Map.entry("TabProject/SelectAgileMethodology", AGILE),
		Map.entry("TabProject/ManageSprints", AGILE),
		Map.entry("TabProject/LinksBetweenProjects", "external-task placeholders live in a private ExternalTaskManager with no query/re-point API, so a Change Links dialog cannot be backed by the engine yet"),
		Map.entry("TabProject/WbsMenu", "WBS codes are derived from the outline only; core has no WBS code mask or renumber facility to back Define Code/Renumber"),
		Map.entry("TabProject/StatusDateLabel", "the Status Date button already shows the date (StatusDateControlUpdater); a separate non-interactive label control needs ribbon label support; " + PHASE_1B),
		Map.entry("TabView/TaskBoardViewGallery", AGILE),
		Map.entry("TabView/TaskViewsOtherGallery", PHASE_1B),
		Map.entry("TabView/TeamPlannerViewGallery", PHASE_1B),
		Map.entry("TabView/ResourcePlanViewGallery", "scope exclusion: Resource Plan view belongs to Project Online / Server resource engagements (AGENTS.md)"),
		Map.entry("TabView/ResourceOtherViewGallery", PHASE_1B),
		Map.entry("TabView/TablesGallery", PHASE_1B),
		Map.entry("TabView/HighlightFiltersGroup", PHASE_1B),
		Map.entry("TabView/TimescaleLabel", PHASE_1B + " (label control)"),
		Map.entry("TabView/TimescaleGallery", PHASE_1B),
		Map.entry("TabView/GalleryTimelineView", PHASE_1B),
		Map.entry("TabView/ViewDetailsGallery", PHASE_1B),
		Map.entry("TabView/WindowNew", PHASE_1B),
		Map.entry("TabView/WindowSwitchWindowsMenuWord", PHASE_1B),
		Map.entry("TabView/HideWindowMenu", PHASE_1B),
		Map.entry("HelpTab/Help", "our Help commands are product-specific and not mapped; " + PHASE_1B),
		Map.entry("HelpTab/OfficeFeedbackHelpTab", "Office feedback service is outside the standalone desktop goal"),
		Map.entry("HelpTab/ShowTraining", PHASE_1B),
		Map.entry("HelpTab/WhatsNewRecentUpdates", PHASE_1B));

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
		Map.entry("RibbonWBS", after("CalendarViewGallery", "WBS view; no Microsoft gallery entry point")),
		Map.entry("RibbonCCPMNetwork", after("CalendarViewGallery", "CCPM view, a supported extension")),
		Map.entry("RibbonRBS", after("ResourceSheetViewGallery", "RBS view; no Microsoft gallery entry point")),
		Map.entry("RibbonProjects", after("ResourceSheetViewGallery", "projects view; no Microsoft gallery entry point")),
		Map.entry("RibbonPrivacyMask", after("WindowsArrangeAll", "window-level display toggle")),
		Map.entry("RibbonMicroProjectDocumentation", after(END, "product documentation; no Microsoft control mapped")),
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
		Map.entry("RibbonSortBy", "SortByDialog"));

	/**
	 * Popups (by ribbon id) with items that have no Microsoft child to be ordered against, with the
	 * reason. Items listed in {@link #ITEM_CONTROLS} are always checked.
	 */
	private static final Map<String, String> UNORDERED_ITEM_POPUPS = Map.ofEntries(
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
		Map.entry("RibbonChooseFilter", LIST_ITEMS),
		Map.entry("RibbonChooseSort", "sort presets are product commands; only Sort By maps"),
		Map.entry("RibbonChooseGroup", LIST_ITEMS));

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
			int previous = 0;
			String previousId = null;
			for (OurControl control : controls) {
				if (!control.mapped()) {
					continue;
				}
				OrderRow row = find(mspTab, group, control.idMso());
				if (row == null) {
					problems.add(key + ": " + control.ribbonId() + " maps to " + control.idMso()
						+ " which Microsoft does not place in " + group + "; it is in " + locate(control.idMso()));
				} else if (row.position() <= previous) {
					problems.add(key + ": Microsoft orders " + control.idMso() + " before " + previousId + " in " + group
						+ " (ours: " + control.ribbonId() + " after it)");
				} else {
					previous = row.position();
					previousId = control.idMso();
				}
			}
		});
		assertTrue(problems.isEmpty(), String.join("\n", problems));
	}

	@Test
	void microsoftControlsWeLackAreReasonedAndStillMissing() {
		List<String> problems = new ArrayList<>();
		Set<String> used = new TreeSet<>();
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
