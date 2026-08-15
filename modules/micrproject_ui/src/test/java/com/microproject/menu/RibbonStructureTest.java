package com.microproject.menu;

import static com.microproject.menu.testsupport.MenuDefinitionSupport.menuInternalBundle;
import static com.microproject.menu.testsupport.MenuDefinitionSupport.menuBundle;
import static com.microproject.menu.testsupport.MenuDefinitionSupport.ribbonBandIds;
import static com.microproject.menu.testsupport.MenuDefinitionSupport.ribbonButtonIds;
import static com.microproject.menu.testsupport.MenuDefinitionSupport.ribbonTaskIds;
import static com.microproject.menu.testsupport.MenuDefinitionSupport.toolBarButtonIds;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Locale;

import org.junit.jupiter.api.Test;

/** Locks the MS Project-style information architecture into the resource map. */
class RibbonStructureTest {
	@Test
	void standardRibbonUsesTheDesktopProjectTabOrder() {
		assertEquals(List.of(
			"FileRibbonTask", "TaskRibbonTask", "ResourceRibbonTask", "ReportRibbonTask",
			"ProjectRibbonTask", "ViewRibbonTask", "FormatRibbonTask"), ribbonTaskIds());
	}

	@Test
	void quickAccessContainsOnlyDocumentWideCommands() {
		assertEquals(Set.of("RibbonTopBarSaveProject", "RibbonTopBarUndo", "RibbonTopBarRedo"),
			toolBarButtonIds("StandardRibbon.TaskBar"));
		assertTrue(!ribbonBandIds("FileRibbonTask").contains("FileQuickRibbonBand"));
	}

	@Test
	void issue37FileTabExposesRecentProjectsBesideOpen() {
		assertEquals(List.of(
			"RibbonNewProject", "RibbonOpenProject", "RibbonRecentProjects",
			"RibbonSaveProject", "RibbonSaveProjectAs", "RibbonCloseProject"),
			ribbonButtonIds("FileRibbonBand"));
		assertEquals("RecentProjectsAction",
			menuInternalBundle().getString("RibbonRecentProjects.action"));
	}

	@Test
	void taskAndResourceTabsSeparateOutlineAssignmentsAndTrackingWork() {
		assertEquals(List.of("RibbonImportProject", "RibbonExportProject"),
			ribbonButtonIds("FileExchangeRibbonBand"));
		assertEquals(List.of(
			"ClipboardRibbonBand", "TaskInsertRibbonBand", "TaskOutlineRibbonBand", "TaskDependenciesRibbonBand",
			"TaskPropertiesRibbonBand", "TaskTrackingRibbonBand", "TaskEditingRibbonBand"),
			ribbonBandIds("TaskRibbonTask"));
		assertEquals(List.of("RibbonIndent", "RibbonOutdent", "RibbonMoveTaskUp", "RibbonMoveTaskDown", "RibbonExpand", "RibbonCollapse"),
			ribbonButtonIds("TaskOutlineRibbonBand"));
		assertEquals("MoveTaskUpAction",menuInternalBundle().getString("RibbonMoveTaskUp.action"));
		assertEquals("MoveTaskDownAction",menuInternalBundle().getString("RibbonMoveTaskDown.action"));
		assertTrue(menuBundle(Locale.ROOT).getString("RibbonMoveTaskUp.tooltip").contains("Alt+Shift+Up"));
		assertTrue(menuBundle(Locale.ROOT).getString("RibbonMoveTaskDown.tooltip").contains("Alt+Shift+Down"));
		assertEquals(List.of("RibbonLink", "RibbonUnlink", "RibbonAssignResources", "RibbonDelegateTasks"),
			ribbonButtonIds("TaskDependenciesRibbonBand"));
		assertEquals(List.of("RibbonUpdateTasks"), ribbonButtonIds("TaskTrackingRibbonBand"));
		assertEquals(List.of(
			"ClipboardRibbonBand", "ResourceInsertRibbonBand", "ResourcePropertiesRibbonBand",
			"ResourceAssignmentsRibbonBand", "ResourceLevelRibbonBand"), ribbonBandIds("ResourceRibbonTask"));
		assertEquals(List.of("RibbonTimesheet", "RibbonTeamFilter"), ribbonButtonIds("ResourceAssignmentsRibbonBand"));
		assertEquals(List.of("RibbonChangeWorkingTime", "RibbonCalendarOptions", "RibbonUpdateProject", "RibbonRecalculate"),
			ribbonButtonIds("ProjectScheduleRibbonBand"));
	}

	@Test
	void issue36ResourceCommandsMatchTheMsProjectRibbonModel() {
		assertEquals(List.of("RibbonTaskUsageDetail", "RibbonResourceUsageDetail"),
			ribbonButtonIds("ViewResourceRibbonBand").stream()
				.filter(id -> id.endsWith("UsageDetail"))
				.toList());
		assertEquals(List.of("RibbonTimesheet", "RibbonTeamFilter"),
			ribbonButtonIds("ResourceAssignmentsRibbonBand"));
		assertEquals(List.of("RibbonLevelResources"), ribbonButtonIds("ResourceLevelRibbonBand"));
		assertEquals("TOGGLE", menuInternalBundle().getString("RibbonTeamFilter.type"));
	}

	@Test
	void projectTabDoesNotExposeTheUnimplementedDefineWbsCommand() {
		assertTrue(!ribbonButtonIds("ProjectInfoRibbonBand").contains("RibbonDefineWBS"));
	}

	@Test
	void commandsHaveOnePrimaryTabExceptClipboard() {
		Map<String, Set<String>> tabsByButton = new LinkedHashMap<>();
		for (String task : ribbonTaskIds()) {
			for (String button : ribbonButtonIdsForTask(task)) {
				tabsByButton.computeIfAbsent(button, ignored -> new LinkedHashSet<>()).add(task);
			}
		}
		Set<String> permittedDuplicates = Set.of("RibbonPaste", "RibbonCopy", "RibbonCut");
		for (Map.Entry<String, Set<String>> entry : tabsByButton.entrySet()) {
			assertTrue(entry.getValue().size() == 1 || permittedDuplicates.contains(entry.getKey()),
				() -> "Unexpected duplicate ribbon command: " + entry.getKey() + " in " + entry.getValue());
		}
	}

	private static Set<String> ribbonButtonIdsForTask(String task) {
		Set<String> buttons = new LinkedHashSet<>();
		for (String band : ribbonBandIds(task)) {
			buttons.addAll(ribbonButtonIds(band));
		}
		return buttons;
	}
}
