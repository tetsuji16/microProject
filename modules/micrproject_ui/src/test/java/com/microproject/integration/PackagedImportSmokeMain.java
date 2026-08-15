package com.microproject.integration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.microproject.collaboration.ProjectMergeService;
import com.microproject.pm.task.Project;

public final class PackagedImportSmokeMain {
	private PackagedImportSmokeMain() {
	}

	public static void main(String[] args) {
		if (args == null || args.length == 0) {
			throw new IllegalArgumentException("usage: PackagedImportSmokeMain <file>...");
		}
		int fileIndex = 0;
		if (args.length >= 2 && "--windows-script".equals(args[0])) {
			verifyWindowsStartScript(Path.of(args[1]));
			fileIndex = 2;
		}
		if (fileIndex >= args.length) {
			throw new IllegalArgumentException("usage: PackagedImportSmokeMain [--windows-script <file>] <file>...");
		}
		ProjectMergeService mergeService = new ProjectMergeService();
		for (int i = fileIndex; i < args.length; i++) {
			String fileName = args[i];
			Project project = mergeService.loadExternalProject(fileName);
			if (project == null) {
				throw new IllegalStateException("Failed to load project file: " + fileName);
			}
			System.out.println("OK " + fileName + " tasks=" + project.getTasks().size());
		}
	}

	private static void verifyWindowsStartScript(Path scriptPath) {
		String script;
		try {
			script = Files.readString(scriptPath);
		} catch (IOException e) {
			throw new IllegalStateException("Failed to read generated Windows start script: " + scriptPath, e);
		}
		String expectedClasspath = "set CLASSPATH=%APP_HOME%\\lib\\micrproject_ui.jar;"
				+ "%APP_HOME%\\lib\\jgoodies-forms-1.9.0.jar;%APP_HOME%\\lib\\*";
		if (!script.contains(expectedClasspath)) {
			throw new IllegalStateException(
					"Generated Windows start script does not preserve the required classpath order: " + scriptPath);
		}
		System.out.println("OK " + scriptPath + " preserves compatibility jars before the wildcard classpath");
	}
}
