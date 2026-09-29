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

/** Child JVM entry point for proving shared MPO locking and merge across processes. */
public final class MpoConcurrentSaveProcess {
	private MpoConcurrentSaveProcess() {
	}

	public static void main(String[] args) throws Exception {
		Path sharedArchive = Path.of(args[0]);
		long taskId = Long.parseLong(args[1]);
		String name = args[2];
		Path ready = Path.of(args[3]);
		Path release = Path.of(args[4]);

		MpoFileImporter reader = new MpoFileImporter();
		reader.setFileName(sharedArchive.toString());
		reader.setProjectFactory(ProjectFactory.getInstance());
		reader.importFile();
		Project project = reader.getProject();
		Task task = project.findByUniqueId(taskId);
		if (task == null)
			throw new IllegalStateException("Task " + taskId + " is missing from shared archive");
		task.setName(name);
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
		writer.exportFile();
	}
}
