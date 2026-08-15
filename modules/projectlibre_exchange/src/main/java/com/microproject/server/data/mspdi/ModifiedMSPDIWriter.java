/*
 * file:       ModifiedMSPDIWriter.java
 * author:     Jon Iles
 * copyright:  (c) Tapster Rock Limited 2002-2003
 * date:       20/02/2003
 */

/*
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 */

package com.microproject.server.data.mspdi;

import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.microproject.pm.task.Project;

import net.sf.mpxj.ProjectFile;
import net.sf.mpxj.Resource;
import net.sf.mpxj.ResourceAssignment;
import net.sf.mpxj.mspdi.MSPDIWriter;

/**
 * ProjectLibre wrapper around the MPXJ MSPDI writer.
 */
public class ModifiedMSPDIWriter
{
	public ModifiedMSPDIWriter() {
	}

	public void setProjectFile(ProjectFile pf) {
		projectFile = pf;
	}

	public ProjectFile getProjectFile() {
		return projectFile;
	}

	public void setOPPrProject(Project projectlibre1Project) {
		this.projectlibre1Project = projectlibre1Project;
	}

	public void putOPPrTaskMap(Object mpx, Object projectlibre1) {
		projectlibreTaskMap.put(mpx, projectlibre1);
	}

	public void putOPPrAssignmentMap(Object mpx, Object projectlibre1) {
		projectlibreAssignmentMap.put(mpx, projectlibre1);
	}

	public void putOPPrSnapshotIdMap(Object mpx, Object projectlibre1) {
		projectlibreSnapshotIdMap.put(mpx, projectlibre1);
	}

	public void putTimephasedList(Object mpx, Object timephasedList) {
		timephasedMap.put(mpx, timephasedList);
	}

	public List<?> getTimephasedList(Object mpx) {
		return (List<?>) timephasedMap.get(mpx);
	}

	public Resource getResourceByUniqueID(int id) {
		return projectFile == null ? null : projectFile.getResourceByUniqueID(id);
	}

	public void consumeTimephased(Object timephased) {
		// No-op in the jar-backed writer path.
	}

	public boolean acceptValue(double value) {
		return true;
	}

	public void write(ProjectFile projectFile, OutputStream out) throws Exception {
		new MSPDIWriter().write(projectFile, out);
	}

	private ProjectFile projectFile;
	protected Project projectlibre1Project;
	protected Map<Object, Object> projectlibreTaskMap = new HashMap<>();
	protected Map<Object, Object> projectlibreAssignmentMap = new HashMap<>();
	protected Map<Object, Object> projectlibreSnapshotIdMap = new HashMap<>();
	protected Map<Object, Object> timephasedMap = new HashMap<>();
}
