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
package com.microproject.menu;

public interface MenuActionConstants {
	public static final String ACTION_NEW_PROJECT             = "NewProject";
	public static final String ACTION_NEW_MASTER_PROJECT      = "NewMasterProject";
	public static final String ACTION_OPEN_PROJECT            = "OpenProject";
	public static final String ACTION_RECENT_PROJECTS         = "RecentProjects";
	public static final String ACTION_IMPORT_MSPROJECT        = "ImportMSProject";
	public static final String ACTION_EXPORT_MSPROJECT        = "ExportMSProject";
	public static final String ACTION_CLOSE_PROJECT           = "CloseProject";
	public static final String ACTION_SAVE_PROJECT            = "SaveProject";
	public static final String ACTION_SAVE_PROJECT_AS         = "SaveProjectAs";
	public static final String ACTION_SAVE_MPO_AS             = "SaveMpoAs";
	public static final String ACTION_PRINT                   = "Print";
	public static final String ACTION_PRINT_PREVIEW           = "PrintPreview";
	public static final String ACTION_PDF         			  = "PDF";
	public static final String ACTION_EXIT                    = "Exit";

	public static final String ACTION_UNDO                    = "Undo";
	public static final String ACTION_REDO                    = "Redo";
	public static final String ACTION_CUT                     = "Cut";
	public static final String ACTION_COPY                    = "Copy";
	public static final String ACTION_PASTE                   = "Paste";
	public static final String ACTION_PASTE_INSERT            = "PasteInsert";

	public static final String ACTION_FILL_DOWN               = "FillDown";
	public static final String ACTION_FILL_RIGHT              = "FillRight";
	public static final String ACTION_FILL_UP                 = "FillUp";
	public static final String ACTION_FILL_LEFT               = "FillLeft";

	public static final String ACTION_CLEAR_ALL               = "ClearAll";
	public static final String ACTION_CLEAR_FORMATS           = "ClearFormats";
	public static final String ACTION_CLEAR_CONTENTS          = "ClearContents";
	public static final String ACTION_CLEAR_NOTES             = "ClearNotes";
	public static final String ACTION_CLEAR_HYPERLINKS        = "ClearHyperlinks";
	public static final String ACTION_CLEAR_ENTIRE            = "ClearEntire";
	public static final String ACTION_DELETE                  = "Delete";

	public static final String ACTION_LINK                    = "Link";
	public static final String ACTION_UNLINK                  = "Unlink";
	public static final String ACTION_SPLIT                   = "Split";

	public static final String ACTION_FIND                    = "Find";
	public static final String ACTION_REPLACE                 = "Replace";
	public static final String ACTION_GOTO                    = "GoTo";
	public static final String ACTION_RECALCULATE             = "Recalculate";

	public static final String ACTION_GANTT                   = "Gantt";
	public static final String ACTION_TRACKING_GANTT          = "TrackingGantt";
	public static final String ACTION_TASK_SHEET              = "TaskSheet";
	public static final String ACTION_TASK_USAGE_DETAIL       = "TaskUsageDetail";
	public static final String ACTION_RESOURCE_USAGE_DETAIL   = "ResourceUsageDetail";
	public static final String ACTION_NETWORK                 = "Network";
	public static final String ACTION_WBS                     = "WBS";
	public static final String ACTION_RBS                     = "RBS";
	public static final String ACTION_REPORT                  = "Report";
	public static final String ACTION_CUSTOM_REPORT           = "CustomReport";
	public static final String ACTION_RESOURCES               = "Resources";
	public static final String ACTION_PROJECTS                = "Projects";
	public static final String ACTION_PROJECTS_DIALOG         = "ProjectsDialog";
	public static final String ACTION_HISTOGRAM               = "Histogram";
	public static final String ACTION_CHARTS                  = "Charts";
	public static final String ACTION_TASK_USAGE              = "TaskUsage";
	public static final String ACTION_RESOURCE_USAGE          = "ResourceUsage";
	public static final String ACTION_DETAILS                 = "Details";
	public static final String ACTION_NO_SUB_WINDOW           = "NoSubWindow";
	public static final String ACTION_ARRANGE_ALL             = "ArrangeAll";
	public static final String ACTION_SWITCH_WINDOWS          = "SwitchWindows";
	public static final String ACTION_TEAM_PLANNER            = "TeamPlanner";
	public static final String ACTION_RECENT_REPORTS          = "RecentReports";
	public static final String ACTION_TIMELINE                = "Timeline";
	public static final String ACTION_CALENDAR_VIEW           = "CalendarView";

	public static final String ACTION_ZOOM_IN                 = "ZoomIn";
	public static final String ACTION_ZOOM_OUT                = "ZoomOut";
	public static final String ACTION_ZOOM_SELECTED_TASKS      = "ZoomSelectedTasks";
	public static final String ACTION_ZOOM_ENTIRE_PROJECT      = "ZoomEntireProject";
	public static final String ACTION_SCROLL_TO_TASK          = "ScrollToTask";
	public static final String ACTION_SCROLL_TO_TODAY         = "ScrollToToday";
	public static final String ACTION_TOGGLE_PROGRESS_LINE    = "ToggleProgressLine";
	public static final String ACTION_LABEL_RESOURCE_NAMES    = "LabelResourceNames";
	public static final String ACTION_LABEL_TASK_NAME         = "LabelTaskName";
	// Gantt Chart Format contextual tab
	public static final String ACTION_GFMT_INSERT_COLUMN = "GanttInsertColumn";
	public static final String ACTION_GFMT_HIDE_COLUMN = "GanttHideColumn";
	public static final String ACTION_GFMT_FIELD_SETTINGS = "GanttFieldSettings";
	public static final String ACTION_GFMT_ALIGN_LEFT = "GanttAlignLeft";
	public static final String ACTION_GFMT_ALIGN_CENTER = "GanttAlignCenter";
	public static final String ACTION_GFMT_ALIGN_RIGHT = "GanttAlignRight";
	public static final String ACTION_GFMT_FORMAT_BAR = "GanttFormatBar";
	public static final String ACTION_GFMT_CRITICAL_TASKS = "GanttCriticalTasks";
	public static final String ACTION_GFMT_SLACK = "GanttSlack";
	public static final String ACTION_GFMT_LATE_TASKS = "GanttLateTasks";
	public static final String ACTION_GFMT_BASELINE = "GanttBaseline";
	public static final String ACTION_GFMT_SLIPPAGE = "GanttSlippage";
	public static final String ACTION_GFMT_PATH_PREDECESSORS = "GanttPathPredecessors";
	public static final String ACTION_GFMT_PATH_DRIVING_PREDECESSORS = "GanttPathDrivingPredecessors";
	public static final String ACTION_GFMT_PATH_SUCCESSORS = "GanttPathSuccessors";
	public static final String ACTION_GFMT_PATH_DRIVEN_SUCCESSORS = "GanttPathDrivenSuccessors";
	public static final String ACTION_GFMT_PATH_REMOVE = "GanttPathRemove";
	public static final String ACTION_GFMT_STYLE_DEFAULT = "GanttStyleDefault";
	public static final String ACTION_GFMT_STYLE_BLUE = "GanttStyleBlue";
	public static final String ACTION_GFMT_STYLE_GREEN = "GanttStyleGreen";
	public static final String ACTION_GFMT_STYLE_ORANGE = "GanttStyleOrange";
	public static final String ACTION_GFMT_STYLE_PURPLE = "GanttStylePurple";
	public static final String ACTION_GFMT_STYLE_GRAY = "GanttStyleGray";
	public static final String ACTION_GFMT_OUTLINE_NUMBERS = "GanttOutlineNumbers";
	public static final String ACTION_GFMT_SUMMARY_TASKS = "GanttSummaryTasks";
	public static final String ACTION_SHEET_GRIDLINES = "SheetGridlines";
	public static final String ACTION_NETWORK_LAYOUT_NOW = "NetworkLayoutNow";
	public static final String ACTION_NETWORK_BOX_COLLAPSED = "NetworkBoxFieldsCollapsed";
	public static final String ACTION_NETWORK_BOX_FORMAT = "NetworkBoxFormat";
	public static final String ACTION_NETWORK_BOX_STYLES = "NetworkBoxStyles";
	public static final String ACTION_NETWORK_LAYOUT_OPTIONS = "NetworkLayoutOptions";
	public static final String ACTION_NETWORK_LINK_LABELS = "NetworkLinkLabels";
	public static final String ACTION_NETWORK_STRAIGHT_LINKS = "NetworkStraightLinks";
	public static final String ACTION_NETWORK_PROGRESS_MARKS = "NetworkProgressMarks";
	public static final String ACTION_CALENDAR_TEXT_STYLES = "CalendarTextStyles";
	public static final String ACTION_CALENDAR_BAR_STYLES = "CalendarBarStyles";
	public static final String ACTION_CALENDAR_LAYOUT_NOW = "CalendarLayoutNow";
	public static final String ACTION_CALENDAR_BEST_FIT = "CalendarWeekHeightBestFit";
	public static final String ACTION_CALENDAR_LAYOUT_OPTIONS = "CalendarLayoutOptions";
	public static final String ACTION_CALENDAR_TASK_LIST = "CalendarTaskList";
	public static final String ACTION_TEAM_PLANNER_ACTUAL_BORDER = "TeamPlannerActualBorder";
	public static final String ACTION_TEAM_PLANNER_ACTUAL_FILL = "TeamPlannerActualFill";
	public static final String ACTION_TEAM_PLANNER_ACTUAL_RESET = "TeamPlannerActualReset";
	public static final String ACTION_TEAM_PLANNER_AUTO_BORDER = "TeamPlannerAutoBorder";
	public static final String ACTION_TEAM_PLANNER_AUTO_FILL = "TeamPlannerAutoFill";
	public static final String ACTION_TEAM_PLANNER_AUTO_RESET = "TeamPlannerAutoReset";
	public static final String ACTION_TEAM_PLANNER_EXPAND_ROWS = "TeamPlannerExpandRows";
	public static final String ACTION_TEAM_PLANNER_EXTERNAL_BORDER = "TeamPlannerExternalBorder";
	public static final String ACTION_TEAM_PLANNER_EXTERNAL_FILL = "TeamPlannerExternalFill";
	public static final String ACTION_TEAM_PLANNER_EXTERNAL_RESET = "TeamPlannerExternalReset";
	public static final String ACTION_TEAM_PLANNER_GRIDLINES = "TeamPlannerGridlines";
	public static final String ACTION_TEAM_PLANNER_LATE_BORDER = "TeamPlannerLateBorder";
	public static final String ACTION_TEAM_PLANNER_LATE_FILL = "TeamPlannerLateFill";
	public static final String ACTION_TEAM_PLANNER_LATE_RESET = "TeamPlannerLateReset";
	public static final String ACTION_TEAM_PLANNER_MANUAL_BORDER = "TeamPlannerManualBorder";
	public static final String ACTION_TEAM_PLANNER_MANUAL_FILL = "TeamPlannerManualFill";
	public static final String ACTION_TEAM_PLANNER_MANUAL_RESET = "TeamPlannerManualReset";
	public static final String ACTION_TEAM_PLANNER_OUTLINE_LEVEL1 = "TeamPlannerOutlineLevel1";
	public static final String ACTION_TEAM_PLANNER_OUTLINE_LEVEL2 = "TeamPlannerOutlineLevel2";
	public static final String ACTION_TEAM_PLANNER_OUTLINE_LEVEL3 = "TeamPlannerOutlineLevel3";
	public static final String ACTION_TEAM_PLANNER_OUTLINE_LEVEL4 = "TeamPlannerOutlineLevel4";
	public static final String ACTION_TEAM_PLANNER_OUTLINE_LEVEL5 = "TeamPlannerOutlineLevel5";
	public static final String ACTION_TEAM_PLANNER_OUTLINE_LEVEL6 = "TeamPlannerOutlineLevel6";
	public static final String ACTION_TEAM_PLANNER_OUTLINE_LEVEL7 = "TeamPlannerOutlineLevel7";
	public static final String ACTION_TEAM_PLANNER_OUTLINE_LEVEL8 = "TeamPlannerOutlineLevel8";
	public static final String ACTION_TEAM_PLANNER_OUTLINE_LEVEL9 = "TeamPlannerOutlineLevel9";
	public static final String ACTION_TEAM_PLANNER_OUTLINE_SHOW_ALL = "TeamPlannerOutlineShowAll";
	public static final String ACTION_TEAM_PLANNER_PREVENT_OVERALLOCATION = "TeamPlannerPreventOverallocation";
	public static final String ACTION_TEAM_PLANNER_SELECTED_BORDER = "TeamPlannerSelectedBorder";
	public static final String ACTION_TEAM_PLANNER_SELECTED_FILL = "TeamPlannerSelectedFill";
	public static final String ACTION_TEAM_PLANNER_TEXT_LINES = "TeamPlannerTextLines";
	public static final String ACTION_TEAM_PLANNER_TEXT_STYLES = "TeamPlannerTextStyles";
	public static final String ACTION_TEAM_PLANNER_UNASSIGNED_TASKS_SHOW = "TeamPlannerUnassignedTasksShow";
	public static final String ACTION_TEAM_PLANNER_UNSCHEDULED_TASKS_SHOW = "TeamPlannerUnscheduledTasksShow";
	public static final String ACTION_TIMELINE_ADD_EXISTING_TASKS = "TimelineAddExistingTasks";
	public static final String ACTION_TIMELINE_BAR_SET_LABEL = "TimelineBarSetLabel";
	public static final String ACTION_TIMELINE_COPY_FOR_EMAIL = "TimelineCopyForEmail";
	public static final String ACTION_TIMELINE_COPY_FOR_PRESENTATION = "TimelineCopyForPresentation";
	public static final String ACTION_TIMELINE_COPY_FULL_SIZE = "TimelineCopyFullSize";
	public static final String ACTION_TIMELINE_DISPLAY_AS_CALLOUT = "TimelineDisplayAsCallout";
	public static final String ACTION_TIMELINE_DISPLAY_ON_TIMELINE = "TimelineDisplayOnTimeline";
	public static final String ACTION_TIMELINE_INSERT_BAR = "TimelineInsertBar";
	public static final String ACTION_TIMELINE_INSERT_CALLOUT = "TimelineInsertCallout";
	public static final String ACTION_TIMELINE_INSERT_MILESTONE = "TimelineInsertMilestone";
	public static final String ACTION_TIMELINE_INSERT_TASK = "TimelineInsertTask";
	public static final String ACTION_TIMELINE_REMOVE_TASK = "TimelineRemoveTask";
	public static final String ACTION_TIMELINE_SET_DATE_RANGE = "TimelineSetDateRange";
	public static final String ACTION_TIMELINE_SHOW_TASK_DATES = "TimelineShowTaskDates";
	public static final String ACTION_TIMELINE_SHOW_TASK_PROGRESS = "TimelineShowTaskProgress";
	public static final String ACTION_TIMELINE_SHOW_TIMESCALE = "TimelineShowTimescale";
	public static final String ACTION_TIMELINE_SHOW_TODAY = "TimelineShowToday";
	public static final String ACTION_TIMELINE_TEXT_STYLES = "TimelineTextStyles";
	public static final String ACTION_RESOURCE_GRAPH_TEXT_STYLES = "ResourceGraphTextStyles";
	public static final String ACTION_RESOURCE_GRAPH_GRIDLINES = "ResourceGraphGridlines";
	public static final String ACTION_RESOURCE_GRAPH_BAR_STYLES = "ResourceGraphBarStyles";
	public static final String ACTION_RESOURCE_GRAPH_PREVIOUS_RESOURCE = "ResourceGraphPreviousResource";
	public static final String ACTION_RESOURCE_GRAPH_NEXT_RESOURCE = "ResourceGraphNextResource";
	public static final String ACTION_RESOURCE_GRAPH_SHOW_VALUES = "ResourceGraphShowValues";
	public static final String ACTION_RESOURCE_GRAPH_PEAK_UNITS = "ResourceGraphPeakUnits";
	public static final String ACTION_RESOURCE_GRAPH_WORK = "ResourceGraphWork";
	public static final String ACTION_RESOURCE_GRAPH_CUMULATIVE_WORK = "ResourceGraphCumulativeWork";
	public static final String ACTION_RESOURCE_GRAPH_OVERALLOCATION = "ResourceGraphOverallocation";
	public static final String ACTION_RESOURCE_GRAPH_PERCENT_ALLOCATION = "ResourceGraphPercentAllocation";
	public static final String ACTION_RESOURCE_GRAPH_REMAINING_AVAILABILITY = "ResourceGraphRemainingAvailability";
	public static final String ACTION_RESOURCE_GRAPH_COST = "ResourceGraphCost";
	public static final String ACTION_RESOURCE_GRAPH_CUMULATIVE_COST = "ResourceGraphCumulativeCost";
	public static final String ACTION_RESOURCE_GRAPH_WORK_AVAILABILITY = "ResourceGraphWorkAvailability";
	public static final String ACTION_RESOURCE_GRAPH_UNIT_AVAILABILITY = "ResourceGraphUnitAvailability";
	public static final String ACTION_TIMELINE_VIEW_EXPAND = "TimelineViewExpand";
	public static final String ACTION_TIMELINE_VIEW_OVERLAPPED_TASKS_SHOW = "TimelineViewOverlappedTasksShow";
	public static final String ACTION_TIMELINE_VIEW_PAN_AND_ZOOM_SHOW = "TimelineViewPanAndZoomShow";
	public static final String ACTION_TIMELINE_VIEW_TEXT_LINE_HEIGHT = "TimelineViewTextLineHeight";
	public static final String ACTION_USAGE_DETAIL_WORK = "UsageDetailWork";
	public static final String ACTION_USAGE_DETAIL_ACTUAL_WORK = "UsageDetailActualWork";
	public static final String ACTION_USAGE_DETAIL_BASELINE_WORK = "UsageDetailBaselineWork";
	public static final String ACTION_USAGE_DETAIL_CUMULATIVE_WORK = "UsageDetailCumulativeWork";
	public static final String ACTION_USAGE_DETAIL_OVERALLOCATION = "UsageDetailOverallocation";
	public static final String ACTION_USAGE_DETAIL_REMAINING_AVAILABILITY = "UsageDetailRemainingAvailability";
	public static final String ACTION_USAGE_DETAIL_COST = "UsageDetailCost";
	public static final String ACTION_USAGE_DETAIL_ACTUAL_COST = "UsageDetailActualCost";
	public static final String ACTION_USAGE_DETAILS_ADD = "UsageDetailsAdd";

	public static final String ACTION_INSERT_TASK             = "InsertTask";
	public static final String ACTION_TASK_MODE_MANUAL       = "TaskModeManual";
	public static final String ACTION_TASK_MODE_AUTOMATIC    = "TaskModeAutomatic";
	public static final String ACTION_INACTIVATE_TASK        = "InactivateTask";
	public static final String ACTION_INSPECT_TASK           = "InspectTask";
	public static final String ACTION_SHOW_TASK_WARNINGS     = "ShowTaskWarnings";
	public static final String ACTION_SHOW_TASK_SUGGESTIONS  = "ShowTaskSuggestions";
	public static final String ACTION_SHOW_IGNORED_PROBLEMS  = "ShowIgnoredProblems";
	public static final String ACTION_STATUS_DATE             = "StatusDate";
	public static final String ACTION_MARK_ON_TRACK           = "MarkOnTrack";
	public static final String ACTION_PERCENT_COMPLETE_0 = "PercentComplete0";
	public static final String ACTION_PERCENT_COMPLETE_25 = "PercentComplete25";
	public static final String ACTION_PERCENT_COMPLETE_50 = "PercentComplete50";
	public static final String ACTION_PERCENT_COMPLETE_75 = "PercentComplete75";
	public static final String ACTION_PERCENT_COMPLETE_100 = "PercentComplete100";
	public static final String ACTION_INSERT_MILESTONE       = "InsertMilestone";
	public static final String ACTION_INSERT_SUMMARY         = "InsertSummary";
	public static final String ACTION_SELECT_ALL             = "SelectAll";
	public static final String ACTION_INSERT_RESOURCE         = "InsertResource";
	public static final String ACTION_INSERT_RECURRING        = "InsertRecurring";
	public static final String ACTION_INSERT_PROJECT          = "InsertProject";
	public static final String ACTION_REFRESH_SUBPROJECTS     = "RefreshSubprojects";
	public static final String ACTION_OPEN_SUBPROJECT          = "OpenSubproject";
	public static final String ACTION_REMOVE_SUBPROJECT        = "RemoveSubproject";
	public static final String ACTION_INSERT_COLUMN           = "InsertColumn";
	public static final String ACTION_INSERT_HYPERLINK        = "InsertHyperlink";
	public static final String ACTION_NEW             	      = ACTION_INSERT_TASK;

	public static final String ACTION_FONT                    = "Font";
	public static final String ACTION_BOLD                    = "Bold";
	public static final String ACTION_ITALIC                  = "Italic";
	public static final String ACTION_FONT_COLOR              = "FontColor";
	public static final String ACTION_UNDERLINE               = "Underline";
	public static final String ACTION_BACKGROUND_COLOR        = "BackgroundColor";
	public static final String ACTION_FONT_NAME               = "FontName";
	public static final String ACTION_FONT_SIZE               = "FontSize";
	public static final String ACTION_BAR                     = "Bar";
	public static final String ACTION_TIMESCALE               = "Timescale";
	public static final String ACTION_GRIDLINES               = "Gridlines";
	public static final String ACTION_TEXT_STYLES             = "TextStyles";
	public static final String ACTION_BAR_STYLES              = "BarStyles";
	public static final String ACTION_LAYOUT                  = "Layout";

	public static final String ACTION_CHANGE_WORKING_TIME     = "ChangeWorkingTime";
	public static final String ACTION_ASSIGN_RESOURCES        = "AssignResources";
	public static final String ACTION_TIMESHEET               = "Timesheet";
	public static final String ACTION_LEVEL_RESOURCES         = "LevelResources";
	public static final String ACTION_LEVEL_ALL               = "LevelAll";
	public static final String ACTION_LEVEL_SELECTION         = "LevelSelection";
	public static final String ACTION_CLEAR_LEVEL_ALL          = "ClearLevelAll";
	public static final String ACTION_CLEAR_LEVEL_SELECTION    = "ClearLevelSelection";
	public static final String ACTION_NEXT_OVERALLOCATION     = "NextOverallocation";
	public static final String ACTION_USE_RESOURCE_POOL       = "UseResourcePool";
	public static final String ACTION_CREATE_RESOURCE_POOL    = "CreateResourcePool";
	public static final String ACTION_REFRESH_RESOURCE_POOL   = "RefreshResourcePool";
	public static final String ACTION_CCPM_SETTINGS           = "CCPMSettings";
	public static final String ACTION_CCPM_CLEAR              = "CCPMClear";
	public static final String ACTION_CCPM_BUFFER_STATUS      = "CCPMBufferStatus";
	/** Canonical command id for retracting one selected CCPM buffer observation. */
	public static final String ACTION_CCPM_BUFFER_OBSERVATION_RETRACT = "CCPM_BUFFER_OBSERVATION_RETRACT";
	public static final String ACTION_CCPM_NETWORK            = "CCPMNetwork";
	public static final String ACTION_TOGGLE_CRITICAL_CHAIN   = "ToggleCriticalChain";
	public static final String ACTION_TRACKING                = "Tracking";
	public static final String ACTION_OPTIONS                 = "Options";

	public static final String ACTION_DELEGATE_TASKS          = "DelegateTasks";
	public static final String ACTION_UPDATE_TASKS            = "UpdateTasks";
	public static final String ACTION_UPDATE_PROJECT          = "UpdateProject";
	/** MSP Project tab command that shifts a forward-scheduled project. */
	public static final String ACTION_MOVE_PROJECT            = "MoveProject";
	public static final String ACTION_SAVE_BASELINE           = "SaveBaseline";
	public static final String ACTION_CLEAR_BASELINE          = "ClearBaseline";
	public static final String ACTION_LOCALE		          = "LocaleAction";
	public static final String ACTION_CALENDAR_OPTIONS        = "CalendarOptions";
	public static final String ACTION_CALENDAR_VIEW_SETTINGS  = "CalendarViewSettings";


	public static final String ACTION_SORT                    = "Sort";
	public static final String ACTION_FILTER                  = "Filter";
	public static final String ACTION_GROUP                   = "Group";
	public static final String ACTION_INFORMATION        	  = "Information";
	public static final String ACTION_NOTES              	  = "Notes";
	public static final String ACTION_PROJECT_INFORMATION     = "ProjectInformation";
	public static final String ACTION_DEFINE_CODE             = "DefineCode";
	public static final String ACTION_TEAM_FILTER		      = "TeamFilter";
	public static final String ACTION_ENTERPRISE_RESOURCES    = "EnterpriseResources";
	public static final String ACTION_DOCUMENTS				  = "Documents";

	public static final String ACTION_INDENT                  = "Indent";
	public static final String ACTION_OUTDENT                 = "Outdent";
	public static final String ACTION_MOVE_TASK_UP            = "MoveTaskUp";
	public static final String ACTION_MOVE_TASK_DOWN          = "MoveTaskDown";
	public static final String ACTION_EXPAND                = "Expand";
	public static final String ACTION_COLLAPSE                = "Collapse";
	public static final String ACTION_HIDE_ASSIGNMENTS        = "HideAssignments";
	public static final String ACTION_HIDE_OUTLINE_SYMBOLS    = "HideOutlineSymbols";
	public static final String ACTION_HIDE_SELECTED_TASKS     = "HideSelectedTasks";
	public static final String ACTION_SHOW_ALL_TASKS          = "ShowAllTasks";
	/** MSP F3 command: clear the active view's user filter. */
	public static final String ACTION_CLEAR_FILTER            = "ClearFilter";
	public static final String ACTION_TOGGLE_PRIVACY_MASK     = "TogglePrivacyMask";

	public static final String ACTION_ALL_CHILDREN            = "AllChildren";
	public static final String ACTION_LEVEL1                  = "Level1";
	public static final String ACTION_LEVEL2                  = "Level2";
	public static final String ACTION_LEVEL3                  = "Level3";
	public static final String ACTION_LEVEL4                  = "Level4";
	public static final String ACTION_LEVEL5                  = "Level5";
	public static final String ACTION_LEVEL6                  = "Level6";
	public static final String ACTION_LEVEL7                  = "Level7";
	public static final String ACTION_LEVEL8                  = "Level8";
	public static final String ACTION_LEVEL9                  = "Level9";


	public static final String ACTION_ABOUT_MICROPROJECT           = "AboutMicroProject";
	public static final String ACTION_MICROPROJECT_DOCUMENTATION   = "MicroProjectDocumentation";
	public static final String ACTION_MICROPROJECT	  	  = "MicroProject";


	public static final String ACTION_CHOOSE_FILTER			  = "ChooseFilter";
	public static final String ACTION_CHOOSE_SORT			  = "ChooseSort";
	public static final String ACTION_CHOOSE_GROUP			  = "ChooseGroup";

	public static final String ACTION_PRINTPREVIEW_PRINT			  = "PrintPreviewPrint";
	public static final String ACTION_PRINTPREVIEW_PDF			  = "PrintPreviewPDF";
	public static final String ACTION_PRINTPREVIEW_FORMAT			  = "PrintPreviewFormat";
	public static final String ACTION_PRINTPREVIEW_BACK			  = "PrintPreviewBack";
	public static final String ACTION_PRINTPREVIEW_FORWARD			  = "PrintPreviewForward";
	public static final String ACTION_PRINTPREVIEW_UP			  = "PrintPreviewUp";
	public static final String ACTION_PRINTPREVIEW_DOWN			  = "PrintPreviewDown";
	public static final String ACTION_PRINTPREVIEW_FIRST			  = "PrintPreviewFirst";
	public static final String ACTION_PRINTPREVIEW_LAST			  = "PrintPreviewLast";
	public static final String ACTION_PRINTPREVIEW_ZOOMIN			  = "PrintPreviewZoomIn";
	public static final String ACTION_PRINTPREVIEW_ZOOMOUT			  = "PrintPreviewZoomOut";
	public static final String ACTION_PRINTPREVIEW_ZOOMRESET			  = "PrintPreviewZoomReset";
	public static final String ACTION_PRINTPREVIEW_LEFT_VIEW			  = "PrintPreviewLeftView";
	public static final String ACTION_PRINTPREVIEW_RIGHT_VIEW			  = "PrintPreviewRightView";

	public static final String ACTION_PALETTE                = "Palette";
	public static final String ACTION_LOOK_AND_FEEL                = "LookAndFeel";
	public static final String ACTION_FULL_SCREEN			=	"FullScreen";
	public static final String ACTION_REFRESH			=	"Refresh";

}

