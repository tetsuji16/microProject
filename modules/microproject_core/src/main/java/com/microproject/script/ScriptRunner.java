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
package com.microproject.script;

import java.util.List;

import com.microproject.script.object.LiteField;
import com.microproject.script.object.LiteProject;
import com.microproject.script.object.LiteResource;
import com.microproject.script.object.LiteTask;
import com.microproject.script.object.ReportData;


/**
 * Will be subclassed for client and server (ajax) versions
 *
 * This will be used for:
 * AJAX communication to server
 * Real-time collaboration
 * and later on
 * as an API (with the advantage of being a sandbox - only can access this package
 * Macros
 * Logging
 * Journaling
 * Possibly using journaling as a way of saving projects instead of sending all data?
 * Should this be tied to undo?
 * And wrapped by bsh or groovy for formulas and filters
 * Need to add exception handling where needed
 *
 */
public interface ScriptRunner {
	public static final int TASK=1;
	public static final int RESOURCE=2;
	public static final int PROJECT=3;
	public static final int ASSIGNMENT=4;

	public static final int PROJECT_DESCRIPTOR=100;
	public static final int PROJECT_DESCRIPTOR_AS_TASK=101;
	public static final int RESOURCE_AS_TASK=102;
	public static final int RESOURCE_USAGE=103;
	public static final int USER=200;

	public static final long PORTFOLIO_PROJECT_ID=100000L;
	public static final long RESOURCE_USAGE_PROJECT_ID=200000L;
	public static final long RESOURCE_ASSIGNMENT_PROJECT_ID=1000000000000000L;


	public List getProjectDescriptors()  throws Exception;
	public List getUsers()  throws Exception;


	public LiteProject createProject(String name) throws Exception;
	public void saveProject(long projectId) throws Exception;
	public void saveProjectAs(long projectId,String name) throws Exception;
	public void closeProject(long projectId) throws Exception;
	public void closeProject(Long ids[]) throws Exception;

	/**
	 * Returns all the tasks of a project.
	 * Opens the project if it's not already opened
	 * @param projectId
	 * @return the content of the project
	 */
	public LiteProject getProject(long projectId,ConverterContext context) throws Exception;

	public List getContexts(int type) throws Exception;
	public Object[] getCharts(int type)  throws Exception;

	public LiteProject setValue(long projectId, String fieldId, int type, long id, String value,boolean returnChanges) throws Exception;
	/**
	 *
	 * @param projectId
	 * @param type
	 * @param previousId
	 * @param returnChanges
	 * @return a project with the new task only if returnChanges is true
	 * @throws Exception
	 */
	public LiteProject insertBefore(long projectId, int type, long previousId, boolean returnChanges) throws Exception;
	public List<Long> remove(long projectId, int type, long id, boolean returnRemovedIds) throws Exception;

	public LiteTask getTask(long projectId,long id) throws Exception;
	public List<LiteTask> getChildrenTasks(long projectId,long id) throws Exception;
	public LiteResource getResource(long projectId,long id) throws Exception;
	public List<LiteResource> getChildrenResource(long projectId,long id) throws Exception;

	public LiteProject link(long projectId,Long ids[],int type) throws Exception;
	public LiteProject unlink(long projectId,Long ids[]) throws Exception;
	public LiteProject indent(long projectId,Long ids[]) throws Exception;
	public LiteProject outdent(long projectId,Long ids[]) throws Exception;

	public LiteProject setInterval(long projectId, long id, long newStart, long newEnd, long oldStart, long oldEnd, boolean returnChanges) throws Exception;
	public LiteProject setCompleted(long projectId, long id, long completed, boolean returnChanges) throws Exception;


	public void setFieldArray(long projectId,int type,String fieldArrayId) throws Exception;
	public List<String> getFieldArrays(long projectId,int type) throws Exception;
	public List<LiteField> getFieldArray(ConverterContext ctx) throws Exception;
	public List<LiteField> getDefaultFieldArray(long projectId) throws Exception;
	public Object zoomTimeScale(long projectId,int type,int amount,float center,boolean returnChange) throws Exception;
	public Object translateWindow(long projectId,int type,int amount,float center,boolean returnChange) throws Exception;



	public ReportData getReport(String reportId, String fieldArrayId) throws Exception;

	public String ping(String message) throws Exception;
	public void close() throws Exception;;
}
