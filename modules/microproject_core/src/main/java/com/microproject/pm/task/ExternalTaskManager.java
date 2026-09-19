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

import java.util.ArrayList;
import java.util.Iterator;
import java.io.File;
import java.io.IOException;

import com.microproject.association.InvalidAssociationException;
import com.microproject.pm.dependency.Dependency;
import com.microproject.pm.dependency.DependencyService;

public class ExternalTaskManager {
	private ArrayList externalTasks = new ArrayList();
	
	public void add(Task task) {
		externalTasks.add(task);
	}
	
	public void handleExternalTasks(Project project, boolean opening, boolean saving) {
		Iterator i = externalTasks.iterator();
		Task externalTask;
		Portfolio portfolio = ProjectFactory.getInstance().getPortfolio();
		while (i.hasNext()) {
			externalTask = (Task) i.next();
			
			if (externalTask.getProjectId() == project.getUniqueId()
					|| sameProjectFile(externalTask.getExternalProjectFile(), project.getFileName())) {
				Task realTask = project.findByUniqueId(externalTask.getUniqueId());
				if (realTask == null) {
					// The external task can refer to a task UID that was removed or
					// remapped in the target project. Keep the unresolved placeholder;
					// never dereference null during a project-open broadcast.
					continue;
				}
				treatOpenedTask(externalTask,realTask,opening);
				if (opening)
					i.remove();
			}
		}
	}

	private static boolean sameProjectFile(String first, String second) {
		if (first == null || second == null || first.isBlank() || second.isBlank())
			return false;
		try {
			return new File(first).getCanonicalFile().equals(new File(second).getCanonicalFile());
		} catch (IOException e) {
			return first.equalsIgnoreCase(second);
		}
	}
	
	private void treatOpenedTask(Task externalTask, Task realTask, boolean opening) {
		externalTask.setExternal(!opening);
		if (opening) {
			Iterator i = externalTask.getSuccessorList().iterator();
			Dependency dep;
			realTask.invalidateSchedules();
			while (i.hasNext()) {
				dep = (Dependency)i.next();
				dep.fireDeleteEvent(this);
				dep.replace(realTask, true);
				try {
					dep.testValid(false);
				} catch (InvalidAssociationException e) {
					dep.setDisabled(true);
					DependencyService.warnCircularCrossProjectLinkMessage(dep.getPredecessor(), dep.getSuccessor());
				}
				realTask.getSuccessorList().add(dep);
				// to fix a bug, I am invalidating both early and late schedules
//				
//				successor.invalidateSchedules();
//				successor.markTaskAsDirty();

				dep.fireCreateEvent(this);
			}
			// The historical resolver only replaced an unloaded task used as a
			// predecessor.  A persisted cross-project link may just as validly use
			// the unloaded task as its successor; keep the graph symmetric so both
			// directions resolve when the referenced project is opened.
			i = externalTask.getPredecessorList().iterator();
			while (i.hasNext()) {
				dep = (Dependency)i.next();
				dep.fireDeleteEvent(this);
				dep.replace(realTask, false);
				try {
					dep.testValid(false);
				} catch (InvalidAssociationException e) {
					dep.setDisabled(true);
					DependencyService.warnCircularCrossProjectLinkMessage(dep.getPredecessor(), dep.getSuccessor());
				}
				realTask.getPredecessorList().add(dep);
				dep.fireCreateEvent(this);
			}
//			System.out.println("removing external task " + externalTask + " from project " + externalTask.getProject());
			externalTask.getProject().removeExternal(externalTask);
			if (externalTask.liesInSubproject()) {
//				System.out.println("alo removing external task " + externalTask + " from project " + externalTask.getRootProject());
				externalTask.getRootProject().removeExternal(externalTask);
			}

//			externalTask.getSuccessorList().replaceAll(realTask, true);
			realTask.markAllDependentTasksAsNeedingRecalculation(true);
		} else {
//			externalTask.getProject().d
		}
	}

}
