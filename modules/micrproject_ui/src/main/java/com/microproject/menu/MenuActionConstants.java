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
	public static final String ACTION_TIMELINE                = "Timeline";
	public static final String ACTION_CALENDAR_VIEW           = "CalendarView";

	public static final String ACTION_ZOOM_IN                 = "ZoomIn";
	public static final String ACTION_ZOOM_OUT                = "ZoomOut";
	public static final String ACTION_SCROLL_TO_TASK          = "ScrollToTask";
	public static final String ACTION_TOGGLE_PROGRESS_LINE    = "ToggleProgressLine";
	public static final String ACTION_LABEL_RESOURCE_NAMES    = "LabelResourceNames";
	public static final String ACTION_LABEL_TASK_NAME         = "LabelTaskName";

	public static final String ACTION_INSERT_TASK             = "InsertTask";
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
	public static final String ACTION_USE_RESOURCE_POOL       = "UseResourcePool";
	public static final String ACTION_CREATE_RESOURCE_POOL    = "CreateResourcePool";
	public static final String ACTION_REFRESH_RESOURCE_POOL   = "RefreshResourcePool";
	public static final String ACTION_CCPM_SETTINGS           = "CCPMSettings";
	public static final String ACTION_CCPM_CLEAR              = "CCPMClear";
	public static final String ACTION_CCPM_BUFFER_STATUS      = "CCPMBufferStatus";
	public static final String ACTION_CCPM_NETWORK            = "CCPMNetwork";
	public static final String ACTION_TOGGLE_CRITICAL_CHAIN   = "ToggleCriticalChain";
	public static final String ACTION_TRACKING                = "Tracking";
	public static final String ACTION_OPTIONS                 = "Options";

	public static final String ACTION_DELEGATE_TASKS          = "DelegateTasks";
	public static final String ACTION_UPDATE_TASKS            = "UpdateTasks";
	public static final String ACTION_UPDATE_PROJECT          = "UpdateProject";
	public static final String ACTION_SAVE_BASELINE           = "SaveBaseline";
	public static final String ACTION_CLEAR_BASELINE          = "ClearBaseline";
	public static final String ACTION_LOCALE		          = "LocaleAction";
	public static final String ACTION_CALENDAR_OPTIONS        = "CalendarOptions";


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


	public static final String ACTION_ABOUT_PROJECTLIBRE           = "AboutProjectLibre";
	public static final String ACTION_PROJECTLIBRE_DOCUMENTATION   = "ProjectLibreDocumentation";
	public static final String ACTION_PROJECTLIBRE	  	  = "ProjectLibre";


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

