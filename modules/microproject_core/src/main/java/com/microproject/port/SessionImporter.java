/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.port;

import java.io.InputStream;
import java.io.OutputStream;

import com.microproject.job.Job;
import com.microproject.job.JobQueue;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.ProjectFactory;

/**
 * Core workflow contract for a file session.  It deliberately contains no
 * format or exchange implementation type; format adapters implement it in
 * the exchange module.
 */
public interface SessionImporter {
	Job getImportFileJob();
	Job getExportFileJob();
	void importFile() throws Exception;
	void exportFile() throws Exception;
	boolean saveProject(Project project, OutputStream out) throws Exception;
	Project loadProject(InputStream in) throws Exception;
	void setFileName(String fileName);
	String getFileName();
	void setProject(Project project);
	Project getProject();
	void setProjectFactory(ProjectFactory projectFactory);
	ProjectFactory getProjectFactory();
	void setJobQueue(JobQueue jobQueue);
	JobQueue getJobQueue();
	void setResourceMapping(Object resourceMapping);
	Object getResourceMapping();
	void setFileInputStream(InputStream fileInputStream);
	InputStream getFileInputStream();
}
