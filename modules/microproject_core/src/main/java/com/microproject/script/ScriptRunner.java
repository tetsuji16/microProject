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
package com.microproject.script;

import java.util.List;

import com.microproject.script.object.LiteField;
import com.microproject.script.object.LiteProject;
import com.microproject.script.object.LiteResource;
import com.microproject.script.object.LiteTask;
import com.microproject.script.object.ReportData;

/**
 * Public entry point for script execution and inter-process collaboration.
 * <p>
 * Implementations of this interface provide the runtime contract between the
 * scripting engine (client-side or server-side) and the microProject core.
 * The interface is deliberately kept as a raw-type API to preserve source
 * compatibility with external implementors; {@code List} return types are
 * intentionally non-parameterized.
 * </p>
 * <p>
 * This interface is used for:
 * </p>
 * <ul>
 *   <li>Remote collaboration via AJAX or similar transport</li>
 *   <li>Macro and script execution sandbox</li>
 *   <li>Journaling and logging of project operations</li>
 *   <li>Formula and filter evaluation (wrapped by a scripting engine)</li>
 * </ul>
 *
 * @see com.microproject.script.object.LiteProject
 * @see com.microproject.script.object.LiteTask
 * @see com.microproject.script.object.LiteResource
 * @see com.microproject.script.object.LiteField
 * @see com.microproject.script.object.ReportData
 */
public interface ScriptRunner {

	// -----------------------------------------------------------------------
	// Type discriminators used by getContexts(int) and other operations
	// -----------------------------------------------------------------------

	/** Task node type discriminator. Value is {@code 1}. */
	public static final int TASK = 1;

	/** Resource node type discriminator. Value is {@code 2}. */
	public static final int RESOURCE = 2;

	/** Project node type discriminator. Value is {@code 3}. */
	public static final int PROJECT = 3;

	/** Assignment node type discriminator. Value is {@code 4}. */
	public static final int ASSIGNMENT = 4;

	// public static final int PORTFOLIO = 6; // reserved, not currently used

	// -----------------------------------------------------------------------
	// Context type discriminators for getContexts(int)
	// -----------------------------------------------------------------------

	/** Context type for project descriptors. Value is {@code 100}. */
	public static final int PROJECT_DESCRIPTOR = 100;

	/** Context type for project descriptors presented as tasks. Value is {@code 101}. */
	public static final int PROJECT_DESCRIPTOR_AS_TASK = 101;

	/** Context type for resources presented as tasks. Value is {@code 102}. */
	public static final int RESOURCE_AS_TASK = 102;

	/** Context type for resource usage data. Value is {@code 103}. */
	public static final int RESOURCE_USAGE = 103;

	/** Context type for user information. Value is {@code 200}. */
	public static final int USER = 200;

	// -----------------------------------------------------------------------
	// Special project identifiers used internally
	// -----------------------------------------------------------------------

	/** Internal project ID for portfolio container. Value is {@code 100000L}. */
	public static final long PORTFOLIO_PROJECT_ID = 100000L;

	/** Internal project ID for resource usage view. Value is {@code 200000L}. */
	public static final long RESOURCE_USAGE_PROJECT_ID = 200000L;

	/** Internal project ID for resource assignment container. Value is {@code 1000000000000000L}. */
	public static final long RESOURCE_ASSIGNMENT_PROJECT_ID = 1000000000000000L;

	// -----------------------------------------------------------------------
	// Project lifecycle
	// -----------------------------------------------------------------------

	/**
	 * Returns a list of project descriptors available to the caller.
	 * <p>
	 * The returned {@code List} contains descriptor objects whose exact type
	 * is implementation-dependent. This method intentionally returns a raw
	 * {@code List} to maintain source compatibility with external implementors.
	 * </p>
	 *
	 * @return raw {@code List} of project descriptors
	 * @throws Exception if the operation fails (transport, authentication, etc.)
	 */
	public List getProjectDescriptors() throws Exception;

	/**
	 * Returns a list of users known to the collaboration backend.
	 * <p>
	 * The returned {@code List} contains user objects whose exact type
	 * is implementation-dependent. This method intentionally returns a raw
	 * {@code List} to maintain source compatibility with external implementors.
	 * </p>
	 *
	 * @return raw {@code List} of user objects
	 * @throws Exception if the operation fails
	 */
	public List getUsers() throws Exception;

	/**
	 * Creates a new project with the given name.
	 *
	 * @param name the name of the new project
	 * @throws Exception if the project cannot be created
	 */
	public LiteProject createProject(String name) throws Exception;

	/**
	 * Saves the project identified by {@code projectId}.
	 *
	 * @param projectId the identifier of the project to save
	 * @throws Exception if the save operation fails
	 */
	public void saveProject(long projectId) throws Exception;

	/**
	 * Saves the project identified by {@code projectId} under a new name.
	 *
	 * @param projectId the identifier of the project to save
	 * @param name the new name for the project
	 * @throws Exception if the save-as operation fails
	 */
	public void saveProjectAs(long projectId, String name) throws Exception;

	/**
	 * Closes the project identified by {@code projectId}.
	 *
	 * @param projectId the identifier of the project to close
	 * @throws Exception if the close operation fails
	 */
	public void closeProject(long projectId) throws Exception;

	/**
	 * Closes multiple projects in a single operation.
	 *
	 * @param ids array of project identifiers to close
	 * @throws Exception if the operation fails
	 */
	public void closeProject(Long ids[]) throws Exception;

	// -----------------------------------------------------------------------
	// Project content access
	// -----------------------------------------------------------------------

	/**
	 * Returns the full project content for the given project ID and converter context.
	 * Opens the project if it is not already open.
	 *
	 * @param projectId the identifier of the project to retrieve
	 * @param context the converter context controlling serialization detail
	 * @return a {@code LiteProject} representing the project content
	 * @throws Exception if the project cannot be opened or read
	 */
	public LiteProject getProject(long projectId, ConverterContext context) throws Exception;

	/**
	 * Returns context-specific data for the given context type.
	 * <p>
	 * The returned {@code List} contains context objects whose exact type
	 * depends on the {@code type} discriminator (see {@link #TASK}, {@link #RESOURCE},
	 * {@link #PROJECT}, {@link #ASSIGNMENT}, {@link #PROJECT_DESCRIPTOR}, etc.).
	 * This method intentionally returns a raw {@code List} to maintain source
	 * compatibility with external implementors.
	 * </p>
	 *
	 * @param type one of the context type constants defined in this interface
	 * @return raw {@code List} of context objects
	 * @throws Exception if the operation fails
	 */
	public List getContexts(int type) throws Exception;

	/**
	 * Returns chart data for the given chart type.
	 *
	 * @param type the chart type discriminator
	 * @return array of chart data objects (implementation-dependent)
	 * @throws Exception if the operation fails
	 */
	public Object[] getCharts(int type) throws Exception;

	// -----------------------------------------------------------------------
	// Mutation operations
	// -----------------------------------------------------------------------

	/**
	 * Sets a field value on a node within the project.
	 *
	 * @param projectId the project identifier
	 * @param fieldId the field identifier
	 * @param type the node type discriminator (see {@link #TASK}, {@link #RESOURCE}, etc.)
	 * @param id the node identifier
	 * @param value the new field value as a string
	 * @param returnChanges whether to return the modified project content
	 * @return the modified {@code LiteProject} if {@code returnChanges} is true,
	 *         otherwise the current project state
	 * @throws Exception if the operation fails
	 */
	public LiteProject setValue(long projectId, String fieldId, int type, long id,
			String value, boolean returnChanges) throws Exception;

	/**
	 * Inserts a new node before the specified existing node.
	 *
	 * @param projectId the project identifier
	 * @param type the node type discriminator
	 * @param previousId the identifier of the node before which to insert
	 * @param returnChanges whether to return the modified project content
	 * @return a {@code LiteProject} containing only the new node if
	 *         {@code returnChanges} is true, otherwise the current project state
	 * @throws Exception if the operation fails
	 */
	public LiteProject insertBefore(long projectId, int type, long previousId,
			boolean returnChanges) throws Exception;

	/**
	 * Removes one or more nodes from the project.
	 *
	 * @param projectId the project identifier
	 * @param type the node type discriminator
	 * @param id the identifier of the node to remove
	 * @param returnRemovedIds whether to return the list of removed identifiers
	 * @return list of removed node identifiers if {@code returnRemovedIds} is true,
	 *         otherwise an empty list
	 * @throws Exception if the operation fails
	 */
	public List<Long> remove(long projectId, int type, long id, boolean returnRemovedIds)
			throws Exception;

	// -----------------------------------------------------------------------
	// Task and resource access
	// -----------------------------------------------------------------------

	/**
	 * Returns the task with the given identifier.
	 *
	 * @param projectId the project identifier
	 * @param id the task identifier
	 * @return the {@code LiteTask} for the requested task
	 * @throws Exception if the task cannot be found or read
	 */
	public LiteTask getTask(long projectId, long id) throws Exception;

	/**
	 * Returns the child tasks of the given parent task.
	 *
	 * @param projectId the project identifier
	 * @param id the parent task identifier
	 * @return list of {@code LiteTask} children
	 * @throws Exception if the operation fails
	 */
	public List<LiteTask> getChildrenTasks(long projectId, long id) throws Exception;

	/**
	 * Returns the resource with the given identifier.
	 *
	 * @param projectId the project identifier
	 * @param id the resource identifier
	 * @return the {@code LiteResource} for the requested resource
	 * @throws Exception if the resource cannot be found or read
	 */
	public LiteResource getResource(long projectId, long id) throws Exception;

	/**
	 * Returns the child resources of the given parent resource.
	 *
	 * @param projectId the project identifier
	 * @param id the parent resource identifier
	 * @return list of {@code LiteResource} children
	 * @throws Exception if the operation fails
	 */
	public List<LiteResource> getChildrenResource(long projectId, long id)
			throws Exception;

	// -----------------------------------------------------------------------
	// Structure manipulation
	// -----------------------------------------------------------------------

	/**
	 * Creates dependency links between the specified nodes.
	 *
	 * @param projectId the project identifier
	 * @param ids array of node identifiers to link
	 * @param type the link type discriminator
	 * @return the modified {@code LiteProject}
	 * @throws Exception if the operation fails
	 */
	public LiteProject link(long projectId, Long ids[], int type) throws Exception;

	/**
	 * Removes dependency links for the specified nodes.
	 *
	 * @param projectId the project identifier
	 * @param ids array of node identifiers to unlink
	 * @return the modified {@code LiteProject}
	 * @throws Exception if the operation fails
	 */
	public LiteProject unlink(long projectId, Long ids[]) throws Exception;

	/**
	 * Indents the specified nodes (makes them children of the preceding sibling).
	 *
	 * @param projectId the project identifier
	 * @param ids array of node identifiers to indent
	 * @return the modified {@code LiteProject}
	 * @throws Exception if the operation fails
	 */
	public LiteProject indent(long projectId, Long ids[]) throws Exception;

	/**
	 * Outdents the specified nodes (moves them up one level in the hierarchy).
	 *
	 * @param projectId the project identifier
	 * @param ids array of node identifiers to outdent
	 * @return the modified {@code LiteProject}
	 * @throws Exception if the operation fails
	 */
	public LiteProject outdent(long projectId, Long ids[]) throws Exception;

	// -----------------------------------------------------------------------
	// Scheduling and progress
	// -----------------------------------------------------------------------

	/**
	 * Sets the scheduled interval for a task.
	 *
	 * @param projectId the project identifier
	 * @param id the task identifier
	 * @param newStart the new start time in milliseconds (GMT)
	 * @param newEnd the new end time in milliseconds (GMT)
	 * @param oldStart the previous start time in milliseconds (GMT)
	 * @param oldEnd the previous end time in milliseconds (GMT)
	 * @param returnChanges whether to return the modified project content
	 * @return the modified {@code LiteProject} if {@code returnChanges} is true
	 * @throws Exception if the operation fails
	 */
	public LiteProject setInterval(long projectId, long id, long newStart, long newEnd,
			long oldStart, long oldEnd, boolean returnChanges) throws Exception;

	/**
	 * Sets the completion percentage for a task.
	 *
	 * @param projectId the project identifier
	 * @param id the task identifier
	 * @param completed the completed duration in milliseconds
	 * @param returnChanges whether to return the modified project content
	 * @return the modified {@code LiteProject} if {@code returnChanges} is true
	 * @throws Exception if the operation fails
	 */
	public LiteProject setCompleted(long projectId, long id, long completed,
			boolean returnChanges) throws Exception;

	// -----------------------------------------------------------------------
	// Field arrays (column layouts)
	// -----------------------------------------------------------------------

	/**
	 * Sets the active field array (column layout) for the given type.
	 *
	 * @param projectId the project identifier
	 * @param type the node type discriminator
	 * @param fieldArrayId the identifier of the field array to activate
	 * @throws Exception if the operation fails
	 */
	public void setFieldArray(long projectId, int type, String fieldArrayId)
			throws Exception;

	/**
	 * Returns the list of available field array identifiers for the given type.
	 *
	 * @param projectId the project identifier
	 * @param type the node type discriminator
	 * @return list of field array identifiers
	 * @throws Exception if the operation fails
	 */
	public List<String> getFieldArrays(long projectId, int type) throws Exception;

	/**
	 * Returns the field array definition for the current converter context.
	 * <p>
	 * The returned {@code List} contains {@code LiteField} objects.
	 * </p>
	 *
	 * @param ctx the converter context
	 * @return list of {@code LiteField} definitions
	 * @throws Exception if the operation fails
	 */
	public List<LiteField> getFieldArray(ConverterContext ctx) throws Exception;

	/**
	 * Returns the default field array for the project.
	 * <p>
	 * The returned {@code List} contains {@code LiteField} objects.
	 * </p>
	 *
	 * @param projectId the project identifier
	 * @return list of {@code LiteField} definitions
	 * @throws Exception if the operation fails
	 */
	public List<LiteField> getDefaultFieldArray(long projectId) throws Exception;

	// -----------------------------------------------------------------------
	// View manipulation
	// -----------------------------------------------------------------------

	/**
	 * Zooms the time scale by the specified amount.
	 *
	 * @param projectId the project identifier
	 * @param type the view type discriminator
	 * @param amount the zoom amount (positive = zoom in, negative = zoom out)
	 * @param center the center point for zoom (0.0 to 1.0)
	 * @param returnChange whether to return the change details
	 * @return implementation-dependent result object
	 * @throws Exception if the operation fails
	 */
	public Object zoomTimeScale(long projectId, int type, int amount, float center,
			boolean returnChange) throws Exception;

	/**
	 * Translates (scrolls) the view window by the specified amount.
	 *
	 * @param projectId the project identifier
	 * @param type the view type discriminator
	 * @param amount the translation amount
	 * @param center the center point for translation (0.0 to 1.0)
	 * @param returnChange whether to return the change details
	 * @return implementation-dependent result object
	 * @throws Exception if the operation fails
	 */
	public Object translateWindow(long projectId, int type, int amount, float center,
			boolean returnChange) throws Exception;

	// -----------------------------------------------------------------------
	// Reporting
	// -----------------------------------------------------------------------

	/**
	 * Generates a report for the given report and field array identifiers.
	 *
	 * @param reportId the report identifier
	 * @param fieldArrayId the field array identifier
	 * @return {@code ReportData} containing the generated report
	 * @throws Exception if the report cannot be generated
	 */
	public ReportData getReport(String reportId, String fieldArrayId) throws Exception;

	// -----------------------------------------------------------------------
	// Utility
	// -----------------------------------------------------------------------

	/**
	 * Simple connectivity test method.
	 *
	 * @param message a message to echo back
	 * @return the echoed message (implementation-dependent format)
	 * @throws Exception if the connection test fails
	 */
	public String ping(String message) throws Exception;

	/**
	 * Closes this script runner and releases any associated resources.
	 *
	 * @throws Exception if the close operation fails
	 */
	public void close() throws Exception;
}