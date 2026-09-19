/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Path helpers that keep persisted Windows paths portable on non-Windows CI. */
public final class FilePathUtils {
	private FilePathUtils() {
	}

	public static String canonicalPath(String fileName) {
		if (fileName == null || fileName.isBlank())
			return fileName;
		if (isWindowsPath(fileName))
			return normalizeWindowsPath(fileName);
		try {
			return new File(fileName).getCanonicalPath();
		} catch (IOException exception) {
			return new File(fileName).getAbsolutePath();
		}
	}

	public static String fileName(String fileName) {
		if (fileName == null || fileName.isBlank())
			return fileName;
		String normalized = fileName.replace('\\', '/');
		return normalized.substring(normalized.lastIndexOf('/') + 1);
	}

	public static String relativePath(String parentFileName, String childFileName) {
		if (isWindowsPath(parentFileName) && isWindowsPath(childFileName))
			return windowsRelativePath(parentFileName, childFileName);
		try {
			File parentFile = new File(parentFileName).getCanonicalFile();
			File childFile = new File(childFileName).getCanonicalFile();
			if (parentFile.toPath().getParent() != null
					&& parentFile.toPath().getRoot().equals(childFile.toPath().getRoot()))
				return parentFile.toPath().getParent().relativize(childFile.toPath()).toString();
		} catch (IOException | IllegalArgumentException exception) {
			// Fall through to the canonical absolute path.
		}
		return childFileName;
	}

	private static boolean isWindowsPath(String value) {
		return value != null && value.matches("(?i)^[a-z]:[\\\\/].*");
	}

	private static String normalizeWindowsPath(String value) {
		String normalized = value.replace('/', '\\');
		String root = normalized.substring(0, 3);
		String[] parts = normalized.substring(3).split("\\\\+");
		List<String> result = new ArrayList<>();
		for (String part : parts) {
			if (part.isEmpty() || ".".equals(part))
				continue;
			if ("..".equals(part)) {
				if (!result.isEmpty())
					result.remove(result.size() - 1);
				continue;
			}
			result.add(part);
		}
		return root + String.join("\\", result);
	}

	private static String windowsRelativePath(String parentFileName, String childFileName) {
		String parent = normalizeWindowsPath(parentFileName);
		String child = normalizeWindowsPath(childFileName);
		if (!parent.substring(0, 2).equalsIgnoreCase(child.substring(0, 2)))
			return child;
		String[] parentParts = parent.substring(3).split("\\\\");
		String[] childParts = child.substring(3).split("\\\\");
		int common = 0;
		while (common < parentParts.length && common < childParts.length
				&& parentParts[common].equalsIgnoreCase(childParts[common]))
			common++;
		List<String> result = new ArrayList<>();
		for (int i = common; i < parentParts.length - 1; i++)
			result.add("..");
		for (int i = common; i < childParts.length; i++)
			result.add(childParts[i]);
		return String.join("\\", result);
	}
}
