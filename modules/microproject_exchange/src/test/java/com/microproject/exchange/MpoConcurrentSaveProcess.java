/*
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
 */
package com.microproject.exchange;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import com.microproject.pm.task.Project;
import com.microproject.pm.task.ProjectFactory;
import com.microproject.pm.task.Task;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.dependency.Dependency;
import com.microproject.pm.dependency.DependencyService;
import com.microproject.pm.dependency.DependencyType;

/** Child JVM entry point for proving shared MPO locking and merge across processes. */
public final class MpoConcurrentSaveProcess {
	private MpoConcurrentSaveProcess() {
	}

	public static void main(String[] args) throws Exception {
		Path sharedArchive = Path.of(args[0]);
		long taskId = Long.parseLong(args[1]);
		String field = args[2];
		String value = args[3];
		Path ready = Path.of(args[4]);
		Path release = Path.of(args[5]);

		MpoFileImporter reader = new MpoFileImporter();
		reader.setFileName(sharedArchive.toString());
		reader.setProjectFactory(ProjectFactory.getInstance());
		reader.importFile();
		Project project = reader.getProject();
		Task task = project.findByUniqueId(taskId);
		if (task == null)
			throw new IllegalStateException("Task " + taskId + " is missing from shared archive");
		switch (field) {
			case "name" -> task.setName(value);
			case "notes" -> task.setNotes(value);
			case "delete" -> project.removeExternal(task);
			default -> {
				if (field.startsWith("dependencyLag:")) {
					long successorId = Long.parseLong(field.substring("dependencyLag:".length()));
					Task successor = project.findByUniqueId(successorId);
					if (successor == null) throw new IllegalStateException("Successor task " + successorId + " is missing");
					boolean changed = false;
					for (java.util.Iterator<?> links = task.getSuccessorList().iterator(); links.hasNext();) {
						Dependency dependency = (Dependency) links.next();
						if (dependency.getSuccessor() == successor) {
							DependencyType.Kind kind = dependency.getDependencyKind();
							DependencyService.getInstance().setFields(dependency, Long.parseLong(value), kind, null);
							changed = true;
							break;
						}
					}
					if (!changed) throw new IllegalStateException("Dependency to task " + successorId + " is missing");
				} else {
					throw new IllegalArgumentException("Unsupported worker task field: " + field);
				}
			}
		}
		Files.createFile(ready);

		long deadline = System.nanoTime() + Duration.ofSeconds(30).toNanos();
		while (!Files.exists(release)) {
			if (System.nanoTime() >= deadline)
				throw new IllegalStateException("Timed out waiting for parent to release concurrent saves");
			Thread.sleep(20L);
		}

		MpoFileImporter writer = new MpoFileImporter();
		writer.setFileName(sharedArchive.toString());
		writer.setProject(project);
		try {
			System.out.println("MPO_SAVE_ATTEMPT=" + field);
			writer.exportFile();
		} catch (MpoConflictRecoveryException conflict) {
			System.out.println("MPO_CONFLICT_RECOVERY=" + conflict.recoveryCopy());
		}
	}
}
