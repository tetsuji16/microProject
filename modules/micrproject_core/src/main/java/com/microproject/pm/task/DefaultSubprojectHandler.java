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
package com.microproject.pm.task;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.model.NodeModelUtil;
import com.microproject.grouping.core.model.NodeModel;

public class DefaultSubprojectHandler implements SubprojectHandler {
	private final Project dummyProject;
	private Task containingSubprojectTask;
	private Collection referringSubprojectTasks = new ArrayList();

	public DefaultSubprojectHandler(Project dummy) {
		this.dummyProject = dummy;
	}
	public Task getContainingSubprojectTask() {
		return containingSubprojectTask;
	}

	public long getReferringSubprojectTaskDependencyDate() {
		long result = 0L;
		for (Object value : referringSubprojectTasks) {
			if (value instanceof Task)
				result = Math.max(result, ((Task) value).getDependencyStart());
		}
		return result;
	}

	public Collection getReferringSubprojectTasks() {
		return referringSubprojectTasks;
	}

	public String getSubprojectOf() {
		return dummyProject == null ? null : dummyProject.getName();
	}

	public void setContainingSubprojectTask(Task containingSubprojectTask) {
		this.containingSubprojectTask = containingSubprojectTask;
	}

	public void setReferringSubprojectTasks(Collection referringSubprojectTasks) {
		this.referringSubprojectTasks = referringSubprojectTasks == null
				? new ArrayList()
				: new ArrayList(referringSubprojectTasks);
	}

	public void switchToResourcesOfProject(Project useMe) {
		if (dummyProject != null && useMe != null)
			dummyProject.setResourcePool(useMe.getResourcePool());
	}

	public void addSubproject(Project subproject, Node subprojectNode, boolean creating, boolean currentlyOpen) {
		if (subproject == null)
			throw new IllegalArgumentException("Subproject cannot be null");
		subproject.setOpenedAsSubproject(true);
		if (subprojectNode != null && subprojectNode.getImpl() instanceof Task task) {
			subproject.setContainingSubprojectTask(task);
			if (task instanceof DefaultSubProj subprojectTask) {
				subprojectTask.setSubprojectFile(subproject.getFileName());
				SubprojectReferenceMetadata.record(dummyProject, subprojectTask, subproject);
				// Keep the insertion mode selected in the master as well as a child
				// that was already read-only at the file level.
				subprojectTask.setSubprojectReadOnly(subprojectTask.isSubprojectReadOnly() || subproject.isReadOnly());
			}
			if (task.getName() == null || task.getName().isBlank())
				task.setName(ProjectFactory.getDisplayNameForSavePrompt(subproject));
			Collection referringTasks = subproject.getReferringSubprojectTasks();
			if (!referringTasks.contains(task))
				referringTasks.add(task);
			attachLoadedTasks(subproject, subprojectNode);
			// Both the newly loaded and already-registered child paths converge here.
			// Mark the persisted reference OPEN only after its projection is attached,
			// so the master grid cannot report NOT_LOADED for visible child rows.
			if (task instanceof SubProj reference)
				reference.setLoadStatus(SubProj.LoadStatus.OPEN);
		}
	}

	@Override
	public void replaceSubproject(Project previous, Project replacement, Node subprojectNode) {
		if (previous == null || replacement == null || subprojectNode == null)
			throw new IllegalArgumentException("Both subprojects and their master reference are required");
		if (dummyProject == null)
			return;
		detachLoadedTasks(previous, subprojectNode);
		previous.setAllTasksInSubproject(false, dummyProject);
		previous.setAllNodesInSubproject(false);
		addSubproject(replacement, subprojectNode, false, true);
	}

	/**
	 * A subproject's tasks retain their owning project for persistence, while their
	 * outline nodes belong directly below the master placeholder.  This mirrors the
	 * multiproject tree used by the scheduler: the master can render a consolidated
	 * Gantt, and saving the child still starts from its enclosing placeholder.
	 */
	private void attachLoadedTasks(Project subproject, Node subprojectNode) {
		if (dummyProject == null)
			return;
		subproject.setAllTasksInSubproject(true, dummyProject);
		NodeModel childOutline = subproject.getTaskOutline();
		NodeModel masterOutline = dummyProject.getTaskOutline();
		List<Node> roots = new ArrayList<Node>();
		for (Enumeration<?> children = ((Node) childOutline.getRoot()).children(); children.hasMoreElements();) {
			Node child = (Node) children.nextElement();
			if (!child.isVoid())
				roots.add(child);
		}
		for (Node root : roots)
			masterOutline.getHierarchy().move(root, subprojectNode, NodeModel.SILENT);
		subproject.setAllNodesInSubproject(true);
		NodeModelUtil.cacheWbs(masterOutline, subprojectNode);
	}

	/** Moves the materialized child outline back to its owning child project. */
	private void detachLoadedTasks(Project subproject, Node subprojectNode) {
		NodeModel childOutline = subproject.getTaskOutline();
		NodeModel masterOutline = dummyProject.getTaskOutline();
		List<Node> roots = new ArrayList<Node>();
		for (Enumeration<?> children = subprojectNode.children(); children.hasMoreElements();) {
			Node child = (Node) children.nextElement();
			if (!child.isVoid())
				roots.add(child);
		}
		for (Node root : roots)
			masterOutline.getHierarchy().move(root, (Node) childOutline.getRoot(), NodeModel.SILENT);
	}

	public boolean canInsertProject(long projectId) {
		if (projectId <= 0L)
			return false;
		if (dummyProject != null && projectId == dummyProject.getUniqueId())
			return false;
		Project candidate = ProjectFactory.getInstance().findFromId(projectId);
		// Insert Subproject normally selects a project file that is not open yet.
		// Only reject the master itself or a child already attached elsewhere.
		return candidate == null || (candidate != dummyProject && !candidate.isOpenedAsSubproject()
				&& !wouldCreateCircularReference(candidate));
	}

	@Override
	public boolean wouldCreateCircularReference(Project candidate) {
		if (dummyProject == null || candidate == null)
			return false;
		return referencesProject(candidate, dummyProject, new HashSet<Long>(), new HashSet<String>());
	}

	@Override
	public String describeCircularReference(Project candidate) {
		if (dummyProject == null || candidate == null)
			return "";
		List<String> path = new ArrayList<String>();
		path.add(projectLabel(dummyProject));
		if (!findReferencePath(candidate, dummyProject, path, new HashSet<Long>(), new HashSet<String>()))
			return "";
		return String.join(" -> ", path);
	}

	private boolean findReferencePath(Project source, Project target, List<String> path, Set<Long> visitedIds,
			Set<String> visitedPaths) {
		path.add(projectLabel(source));
		if (sameProject(source, target))
			return true;
		long sourceId = source.getUniqueId();
		if (sourceId > 0L && !visitedIds.add(sourceId)) {
			path.remove(path.size() - 1);
			return false;
		}
		String sourcePath = source.getFileName();
		if (sourcePath != null && !sourcePath.isBlank() && !visitedPaths.add(canonicalPath(sourcePath))) {
			path.remove(path.size() - 1);
			return false;
		}
		for (java.util.Iterator<?> tasks = source.getTaskOutlineIterator(); tasks.hasNext();) {
			Object value = tasks.next();
			if (!(value instanceof SubProj reference))
				continue;
			if (sameProjectReference(reference, target)) {
				path.add(projectLabel(target));
				return true;
			}
			Project nested = reference.getSubproject();
			if (nested != null && findReferencePath(nested, target, path, visitedIds, visitedPaths))
				return true;
		}
		path.remove(path.size() - 1);
		return false;
	}

	private static String projectLabel(Project project) {
		if (project == null)
			return "(unknown project)";
		if (project.getName() != null && !project.getName().isBlank())
			return project.getName();
		if (project.getFileName() != null && !project.getFileName().isBlank())
			return new File(project.getFileName()).getName();
		return "(unnamed project)";
	}

	private boolean referencesProject(Project source, Project target, Set<Long> visitedIds, Set<String> visitedPaths) {
		if (sameProject(source, target))
			return true;
		long sourceId = source.getUniqueId();
		if (sourceId > 0L && !visitedIds.add(sourceId))
			return false;
		String sourcePath = source.getFileName();
		if (sourcePath != null && !sourcePath.isBlank() && !visitedPaths.add(canonicalPath(sourcePath)))
			return false;
		for (java.util.Iterator<?> tasks = source.getTaskOutlineIterator(); tasks.hasNext();) {
			Object value = tasks.next();
			if (!(value instanceof SubProj reference))
				continue;
			if (sameProjectReference(reference, target))
				return true;
			Project nested = reference.getSubproject();
			if (nested != null && referencesProject(nested, target, visitedIds, visitedPaths))
				return true;
		}
		return false;
	}

	private static boolean sameProject(Project first, Project second) {
		if (first == second)
			return true;
		if (first == null || second == null)
			return false;
		return first.getUniqueId() > 0L && first.getUniqueId() == second.getUniqueId()
				|| sameCanonicalFile(first.getFileName(), second.getFileName());
	}

	private static boolean sameProjectReference(SubProj reference, Project project) {
		return reference.getSubprojectUniqueId() > 0L
				&& reference.getSubprojectUniqueId() == project.getUniqueId()
				|| sameCanonicalFile(reference.getSubprojectFile(), project.getFileName());
	}

	private static String canonicalPath(String fileName) {
		if (fileName == null || fileName.isBlank())
			return fileName;
		try {
			return new File(fileName).getCanonicalPath();
		} catch (IOException exception) {
			return new File(fileName).getAbsolutePath();
		}
	}

	@Override
	public boolean hasSubprojectReference(String fileName) {
		if (dummyProject == null || fileName == null || fileName.isBlank())
			return false;
		java.util.Iterator<?> tasks = dummyProject.getTaskOutlineIterator();
		while (tasks.hasNext()) {
			Object value = tasks.next();
			if (value instanceof SubProj reference && sameCanonicalFile(reference.getSubprojectFile(), fileName))
				return true;
		}
		return false;
	}

	private static boolean sameCanonicalFile(String first, String second) {
		if (first == null || second == null || first.isBlank() || second.isBlank())
			return false;
		try {
			return new File(first).getCanonicalFile().equals(new File(second).getCanonicalFile());
		} catch (IOException exception) {
			return new File(first).getAbsoluteFile().equals(new File(second).getAbsoluteFile());
		}
	}

	public SubProj createSubProj(long subprojectUniqueId) {
		return new DefaultSubProj(dummyProject, subprojectUniqueId);
	}

}
