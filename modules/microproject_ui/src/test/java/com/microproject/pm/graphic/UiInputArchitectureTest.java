/*******************************************************************************
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
 *******************************************************************************/
package com.microproject.pm.graphic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

class UiInputArchitectureTest {
	@Test
	void spreadsheetPackageDoesNotContainAnIndependentKeyboardInputSandbox() throws Exception {
		Path sourceRoot = findProjectRoot().resolve(
			"modules/microproject_ui/src/main/java/com/microproject/pm/graphic/spreadsheet");
		try (var files = Files.walk(sourceRoot)) {
			assertFalse(files.anyMatch(path -> path.getFileName().toString().equals("SpreadsheetImeSandbox.java")),
				"Production spreadsheet input must use the document shortcut and editor paths, not an isolated sandbox");
		}
	}


	/** #414: shortcuts resolve through the root-pane InputMap; no production KeyListener may dispatch commands. */
	@Test
	void productionUiHasNoKeyListenerInputPath() throws Exception {
		assertEquals(List.of(), sourcesMatching("\\b(addKeyListener|KeyAdapter|implements\\s+KeyListener)\\b"),
			"use InputMap/ActionMap (applyMicrosoftShortcuts or editor-local bindings) instead of a KeyListener");
	}

	/** #414: one wheel owner per synchronized view, with symmetric registration in ScrollPaneSynchronizer. */
	@Test
	void mouseWheelListenersAreOwnedOnlyByTheScrollPaneSynchronizer() throws Exception {
		assertEquals(List.of("pm/graphic/views/synchro/ScrollPaneSynchronizer.java"),
			sourcesMatching("\\baddMouseWheelListener\\s*\\("),
			"a second wheel path would double-handle Ctrl/Shift/plain wheel events");
	}

	/** #414: GlobalPreferences outlive documents and dialogs, so every registration must be undone by its owner. */
	@Test
	void everyGlobalPreferencesListenerRegistrationHasAMatchingRemoval() throws Exception {
		List<String> adds = sourcesMatching(Pattern.quote("getPreferences().addObjectListener("));
		List<String> removes = sourcesMatching(Pattern.quote("getPreferences().removeObjectListener("));
		assertEquals(adds, removes, "a class that registers on GlobalPreferences must also unregister on cleanup/dispose");
	}

	/**
	 * #414 / Windows-Office dialog rule: Enter presses the primary command. AbstractDialog does this centrally;
	 * FlatLafDialog boxes declare a default button. CalendarViewDialogBox has only navigation buttons, so Escape
	 * closes it and Enter is left to the focused control.
	 */
	@Test
	void everyFlatLafDialogBoxDeclaresItsEnterDefaultButton() throws Exception {
		List<String> dialogs = new ArrayList<>(sourcesMatching(Pattern.quote("extends FlatLafDialog")));
		dialogs.remove("dialog/CalendarViewDialogBox.java");
		dialogs.remove("dialog/AbstractDialog.java"); // binds Enter to the focused button or onOk itself
		List<String> withDefault = sourcesMatching(Pattern.quote("getRootPane().setDefaultButton("));
		assertEquals(List.of(), dialogs.stream().filter(path -> !withDefault.contains(path)).toList(),
			"FlatLafDialog boxes without a default button for Enter");
	}

	private static List<String> sourcesMatching(String regex) throws Exception {
		Pattern pattern = Pattern.compile(regex);
		Path sourceRoot = findProjectRoot().resolve("modules/microproject_ui/src/main/java/com/microproject");
		try (var files = Files.walk(sourceRoot)) {
			List<String> matches = new ArrayList<>();
			for (Path path : files.filter(p -> p.toString().endsWith(".java")).sorted().toList()) {
				if (pattern.matcher(Files.readString(path)).find())
					matches.add(sourceRoot.relativize(path).toString().replace('\\', '/'));
			}
			return matches;
		}
	}
	private static Path findProjectRoot() throws java.io.IOException {
		for (Path current = Path.of("").toAbsolutePath(); current != null; current = current.getParent()) {
			if (Files.isDirectory(current.resolve("modules/microproject_ui")))
				return current;
		}
		throw new java.io.IOException("Project root was not found");
	}
}
