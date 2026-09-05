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
package com.microproject.ui.ribbon;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;

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
	private static final String FORMAT = "FormatRibbonTask";
	private static final String QUICK_ACCESS = "QuickAccessToolbar";
	private static final Map<String, Placement> PLACEMENTS = placements();

	private RibbonCommandCatalog() {
	}

	static List<CommandDefinition> from(SwingRibbonModel model) {
		return from(model, new ResourceBundle[0]);
	}

	static List<CommandDefinition> from(SwingRibbonModel model, ResourceBundle... bundles) {
		Map<String, Set<String>> tabsByCommand = tabsByCommand(model);
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
		for (CommandDefinition definition : from(model, bundles)) {
			if (bundles.length > 0 && (definition.actionId() == null || definition.iconKey() == null)) {
				throw new IllegalStateException("Ribbon command is missing action or icon metadata: " + definition.id());
			}
			Set<String> actualTabs = tabsByCommand.get(definition.id());
			if (!definition.permittedTabs().equals(actualTabs)) {
				throw new IllegalStateException("Ribbon command has invalid tab placement: " + definition.id()
					+ " expected " + definition.permittedTabs() + " but was " + actualTabs);
			}
		}
	}

	private static Map<String, Set<String>> tabsByCommand(SwingRibbonModel model) {
		Map<String, Set<String>> tabsByCommand = new LinkedHashMap<>();
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
			"RibbonLocale", "RibbonProjectLibreDocumentation", "RibbonAboutProjectLibre");
		register(result, CommandScope.DOCUMENT, FILE,
			"RibbonSaveProject", "RibbonSaveProjectAs", "RibbonSaveMpoAs", "RibbonCloseProject", "RibbonExportProject",
			"RibbonPrint", "RibbonPrintPreview", "RibbonPDF");
		register(result, CommandScope.TASK, TASK,
			"RibbonInsert", "RibbonInsertRecurring", "RibbonInsertProject", "RibbonIndent", "RibbonOutdent",
			"RibbonMoveTaskUp", "RibbonMoveTaskDown",
			"RibbonExpand", "RibbonCollapse", "RibbonLink", "RibbonUnlink", "RibbonAssignResources",
			"RibbonDelegateTasks", "RibbonTaskInformation", "RibbonNotes", "RibbonUpdateTasks", "RibbonDelete",
			"RibbonCustomFields", "RibbonFind", "RibbonScrollToTask", "RibbonHideSelectedTasks", "RibbonShowAllTasks");
		register(result, CommandScope.RESOURCE, RESOURCE,
			"RibbonInsertResource", "RibbonResourceInformation", "RibbonTimesheet", "RibbonTeamFilter", "RibbonLevelResources",
			"RibbonUseResourcePool", "RibbonCreateResourcePool", "RibbonRefreshResourcePool");
		register(result, CommandScope.REPORT, REPORT,
			"RibbonReport", "RibbonCustomReport", "RibbonHistogram", "RibbonCharts", "RibbonTaskUsage", "RibbonResourceUsage", "RibbonCCPMBufferStatus");
		register(result, CommandScope.PROJECT, PROJECT,
			"RibbonProjectInformation", "RibbonProjectsDialog", "RibbonChangeWorkingTime",
			"RibbonCalendarOptions", "RibbonUpdateProject", "RibbonRecalculate", "RibbonRefreshSubprojects", "RibbonOpenSubproject", "RibbonRemoveSubproject", "RibbonSaveBaseline", "RibbonClearBaseline",
			"RibbonCCPMSettings", "RibbonCCPMClear");
		register(result, CommandScope.VIEW, VIEW,
			"RibbonGantt", "RibbonTrackingGantt", "RibbonNetwork", "RibbonWBS", "RibbonResources", "RibbonRBS",
			"RibbonTimeline", "RibbonCalendarView", "RibbonProjects", "RibbonTaskUsageDetail", "RibbonResourceUsageDetail", "RibbonNoTextNoSubWindow",
			"RibbonArrangeAll", "RibbonChooseFilter", "RibbonChooseSort", "RibbonChooseGroup", "RibbonZoomIn", "RibbonZoomOut", "RibbonCCPMNetwork");
		register(result, CommandScope.FORMAT, FORMAT,
			"RibbonToggleProgressLine", "RibbonLabelResourceNames", "RibbonLabelTaskName", "RibbonGridlines",
			"RibbonToggleCriticalChain", "RibbonTimescale", "RibbonBar", "RibbonBarStyles", "RibbonTextStyles", "RibbonLayout");
		register(result, CommandScope.DOCUMENT, TASK, Set.of(TASK, RESOURCE), "RibbonPaste", "RibbonCopy", "RibbonCut");
		register(result, CommandScope.DOCUMENT, QUICK_ACCESS,
			"RibbonTopBarSaveProject", "RibbonTopBarUndo", "RibbonTopBarRedo");
		return Map.copyOf(result);
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
