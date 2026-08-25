/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *******************************************************************************/
package com.microproject.pm.graphic.views;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

class TaskArchitectureAuditTest {
	@Test
	void taskUiComponentsCannotCreateFallbackCommandGateways() throws IOException {
		Path sourceRoot = sourceRoot();
		try (Stream<Path> files = Files.walk(sourceRoot)) {
			for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
				String relative = sourceRoot.relativize(file).toString().replace('\\', '/');
				if (relative.equals("com/microproject/pm/graphic/frames/DocumentFrame.java")
						|| relative.equals("com/microproject/pm/graphic/frames/GraphicManager.java")) continue;
				String source = Files.readString(file, StandardCharsets.UTF_8);
				assertFalse(source.contains("new TaskCommandGateway(")
						|| source.contains("new com.microproject.application.task.TaskCommandGateway("),
						() -> "fallback TaskCommandGateway in " + relative);
			}
		}
	}

	@Test
	void sharedGraphicNodesCannotOwnProjectionRows() throws IOException {
		Path file = sourceRoot().resolve("com/microproject/pm/graphic/model/cache/GraphicNode.java");
		String source = Files.readString(file, StandardCharsets.UTF_8);
		assertFalse(source.contains(" setRow(") || source.contains(" getRow("),
				"GraphicNode must not own a view row");
	}

	@Test
	void ganttPaintCannotReadMutableProjectionOrDomainObjects() throws IOException {
		Path file = sourceRoot().resolve("com/microproject/pm/graphic/gantt/GanttRenderer.java");
		String source = Files.readString(file, StandardCharsets.UTF_8);
		assertFalse(source.contains("GraphicNode") || source.contains("GraphicDependency")
				|| source.contains("getVisibleDependencies()") || source.contains(".getNode()"),
				"Gantt paint must consume TaskProjectionSnapshot values only");
	}

	@Test
	void dependencyDialogMustCommitTheRevisionCapturedWhenItOpened() throws IOException {
		Path file = sourceRoot().resolve("com/microproject/dialog/DependencyDialog.java");
		String source = Files.readString(file, StandardCharsets.UTF_8);
		int commitStart = source.indexOf("private boolean commitDraft()");
		int commitEnd = source.indexOf("protected void initComponents()", commitStart);
		String commit = source.substring(commitStart, commitEnd);
		assertFalse(commit.contains("getDomainChangeJournal().revision()"),
				"dependency commit must not replace its open-time optimistic-lock revision");
	}

	private static Path sourceRoot() {
		for (Path candidate : new Path[] { Path.of("src/main/java"),
				Path.of("modules/micrproject_ui/src/main/java") })
			if (Files.isDirectory(candidate)) return candidate;
		throw new IllegalStateException("micrproject_ui source root not found");
	}
}
