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
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;

import com.microproject.pm.dependency.DependencyService;
import com.microproject.util.FilePathUtils;

public class ExternalTaskManager {
	private final List<Task> externalTasks = new ArrayList<>();
	private final List<Resolution> resolutions = new ArrayList<>();
	private final Deque<Integer> resolutionTransactions = new ArrayDeque<>();
	private record Resolution(Task placeholder, int index, Runnable rollback) { }
	
	public void add(Task task) {
		externalTasks.add(task);
	}
	
	public void handleExternalTasks(Project project, boolean opening, boolean saving) {
		Iterator<Task> i = externalTasks.iterator();
		Portfolio portfolio = ProjectFactory.getInstance().getPortfolio();
		while (i.hasNext()) {
			Task externalTask = i.next();
			
			if (externalTask.getProjectId() == project.getUniqueId()
					|| FilePathUtils.sameFileIdentity(externalTask.getExternalProjectFile(), project.getFileName())) {
				Task realTask = project.findByUniqueId(externalTask.getUniqueId());
				if (realTask == null) {
					// The external task can refer to a task UID that was removed or
					// remapped in the target project. Keep the unresolved placeholder;
					// never dereference null during a project-open broadcast.
					continue;
				}
				int index = externalTasks.indexOf(externalTask);
				Runnable rollback = treatOpenedTask(externalTask,realTask,opening);
				if (opening) {
					if (!resolutionTransactions.isEmpty())
						resolutions.add(new Resolution(externalTask, index, rollback));
					i.remove();
				}
			}
		}
	}

	private Runnable treatOpenedTask(Task externalTask, Task realTask, boolean opening) {
		if (opening) {
			Runnable rollback = DependencyService.getInstance().resolveExternalTaskAndRemovePlaceholder(externalTask, realTask, this);
			externalTask.setExternal(false);
			realTask.invalidateSchedules();
			realTask.markAllDependentTasksAsNeedingRecalculation(true);
			return rollback;
		} else {
			externalTask.setExternal(true);
			return () -> externalTask.setExternal(false);
		}
	}

	int beginResolutionTransaction() {
		int checkpoint = resolutions.size();
		resolutionTransactions.push(checkpoint);
		return checkpoint;
	}
	void commitResolutionTransaction(int checkpoint) {
		finishResolutionTransaction(checkpoint);
		if (resolutionTransactions.isEmpty()) resolutions.clear();
	}
	void rollbackResolutionTransaction(int checkpoint) {
		finishResolutionTransaction(checkpoint);
		Throwable failure = null;
		for (int i = resolutions.size() - 1; i >= checkpoint; i--) {
			Resolution resolution = resolutions.remove(i);
			try { resolution.rollback().run(); }
			catch (RuntimeException | Error rollbackFailure) {
				if (failure == null) failure = rollbackFailure; else failure.addSuppressed(rollbackFailure);
			}
			try {
				resolution.placeholder().setExternal(true);
				externalTasks.add(Math.min(resolution.index(), externalTasks.size()), resolution.placeholder());
			} catch (RuntimeException | Error restoreFailure) {
				if (failure == null) failure = restoreFailure; else failure.addSuppressed(restoreFailure);
			}
		}
		if (failure instanceof Error error) throw error;
		if (failure instanceof RuntimeException runtime) throw runtime;
	}
	private void finishResolutionTransaction(int checkpoint) {
		if (resolutionTransactions.isEmpty() || resolutionTransactions.peek() != checkpoint)
			throw new IllegalStateException("External task resolution transactions must finish in LIFO order");
		resolutionTransactions.pop();
	}
	int retainedResolutionCount() { return resolutions.size(); }

}
