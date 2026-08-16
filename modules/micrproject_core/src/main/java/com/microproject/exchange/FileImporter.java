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
package com.microproject.exchange;

import java.io.InputStream;
import java.io.OutputStream;

import com.microproject.job.Job;
import com.microproject.job.JobQueue;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.ProjectFactory;

/**
 * Abstract class for importing and exporting files
 */
public abstract class FileImporter /*implements Runnable*/ {
	protected JobQueue jobQueue=null;
	protected String fileName;
	protected InputStream fileInputStream;
	protected Project project;
	protected ResourceMappingForm resourceMapping;
	protected ProjectFactory projectFactory=null;

/**
 * Constructor
 * @param fileName
 * @param document 
 */	
	protected FileImporter() {
	}

	public abstract Job getImportFileJob();
	public abstract Job getExportFileJob();
	public abstract void importFile() throws Exception;
	public abstract void exportFile() throws Exception;
	public abstract boolean saveProject(Project project,OutputStream out) throws Exception;
	public abstract Project loadProject(InputStream in) throws Exception;

	
	/**
	 * @param fileName The fileName to set.
	 */
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	/**
	 * @param project The project to set.
	 */
	public void setProject(Project project) {
		this.project = project;
	}
	/**
	 * @return Returns the fileName.
	 */
	public String getFileName() {
		return fileName;
	}
	/**
	 * @return Returns the project.
	 */
	public Project getProject() {
		return project;
	}

	
	
	public ProjectFactory getProjectFactory() {
		return projectFactory;
	}
	public void setProjectFactory(ProjectFactory projectFactory) {
		this.projectFactory = projectFactory;
	}
	public JobQueue getJobQueue() {
		return jobQueue;
	}
	public void setJobQueue(JobQueue jobQueue) {
		this.jobQueue = jobQueue;
	}

	public ResourceMappingForm getResourceMapping() {
		return resourceMapping;
	}

	public void setResourceMapping(ResourceMappingForm resourceMapping) {
		this.resourceMapping = resourceMapping;
	}

	public InputStream getFileInputStream() {
		return fileInputStream;
	}

	public void setFileInputStream(InputStream fileInputStream) {
		this.fileInputStream = fileInputStream;
	}
	
	
	
	
	
	
	
}
