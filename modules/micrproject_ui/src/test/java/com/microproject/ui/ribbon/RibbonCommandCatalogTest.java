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
package com.microproject.ui.ribbon;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.ListResourceBundle;
import java.util.Locale;
import java.util.Set;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.microproject.menu.ExtToolBarFactory;
import com.microproject.menu.MenuActionMapSupport;
import com.microproject.menu.MenuManager;
import com.microproject.menu.testsupport.MenuDefinitionSupport;

class RibbonCommandCatalogTest {
	@Test
	void standardRibbonMatchesTheExplicitCatalogInEnglishAndJapanese() {
		assertDoesNotThrow(() -> validate(Locale.ROOT));
		assertDoesNotThrow(() -> validate(Locale.JAPAN));
	}

	@Test
	void catalogKeepsClipboardAsTheOnlyCrossTabCommandSet() {
		SwingRibbonModel model = validate(Locale.ROOT);
		var commands = RibbonCommandCatalog.from(model, MenuDefinitionSupport.ribbonBundles(Locale.ROOT));
		var clipboard = commands.stream()
			.filter(command -> command.id().equals("RibbonPaste"))
			.findFirst().orElseThrow();
		assertEquals("TaskRibbonTask", clipboard.primaryTab());
		assertEquals(2, clipboard.permittedTabs().size());
		assertEquals(RibbonCommandCatalog.CommandScope.GLOBAL, commands.stream()
			.filter(command -> command.id().equals("RibbonImportProject"))
			.findFirst().orElseThrow().scope());
		assertEquals(RibbonCommandCatalog.CommandScope.DOCUMENT, commands.stream()
			.filter(command -> command.id().equals("RibbonSaveProject"))
			.findFirst().orElseThrow().scope());
	}

	@Test
	void catalogIncludesAndValidatesQuickAccessCommands() {
		SwingRibbonModel model = validate(Locale.ROOT);
		Set<String> quickAccessIds = RibbonCommandCatalog.from(
			model, MenuDefinitionSupport.ribbonBundles(Locale.ROOT)).stream()
			.filter(command -> command.primaryTab().equals("QuickAccessToolbar"))
			.map(RibbonCommandCatalog.CommandDefinition::id)
			.collect(java.util.stream.Collectors.toSet());
		assertEquals(Set.of("RibbonTopBarSaveProject", "RibbonTopBarUndo", "RibbonTopBarRedo"), quickAccessIds);

		SwingRibbonModel missingMetadata = new SwingRibbonModel(
			"test", List.of(), List.of("RibbonTopBarSaveProject"));
		var emptyBundle = new ListResourceBundle() {
			@Override
			protected Object[][] getContents() {
				return new Object[0][0];
			}
		};
		assertThrows(IllegalStateException.class,
			() -> RibbonCommandCatalog.validate(missingMetadata, emptyBundle));
	}

	@Test
	void documentedGuiUseCasesCoverEveryStandardRibbonCommand() throws Exception {
		SwingRibbonModel model = validate(Locale.ROOT);
		Path documentPath = Path.of("docs", "RIBBON_COMMAND_GUI_TEST_CASES_JA.md");
		Path directory = Path.of("").toAbsolutePath();
		while (!Files.exists(documentPath) && directory.getParent() != null) {
			directory = directory.getParent();
			documentPath = directory.resolve("docs").resolve("RIBBON_COMMAND_GUI_TEST_CASES_JA.md");
		}
		assertTrue(Files.exists(documentPath), "Ribbon GUI use-case document was not found from " + Path.of("").toAbsolutePath());
		String document = Files.readString(documentPath, StandardCharsets.UTF_8);
		for (RibbonCommandCatalog.CommandDefinition command : RibbonCommandCatalog.from(model)) {
			assertTrue(document.contains(command.id()), "Missing ribbon GUI use case for " + command.id());
		}
	}

	@Test
	void rejectsUncatalogedCommandsAndInvalidDuplicatePlacements() {
		SwingRibbonModel.RibbonButton button = new SwingRibbonModel.RibbonButton(
			"RibbonDelete", SwingRibbonModel.ButtonPriority.TOP);
		SwingRibbonModel duplicate = new SwingRibbonModel("test", List.of(
			new SwingRibbonModel.RibbonTab("TaskRibbonTask", "Task", List.of(new SwingRibbonModel.RibbonBand("a", "A", List.of(button)))),
			new SwingRibbonModel.RibbonTab("ResourceRibbonTask", "Resource", List.of(new SwingRibbonModel.RibbonBand("b", "B", List.of(button))))), List.of());
		assertThrows(IllegalStateException.class, () -> RibbonCommandCatalog.validate(duplicate));

		SwingRibbonModel unknown = new SwingRibbonModel("test", List.of(
			new SwingRibbonModel.RibbonTab("TaskRibbonTask", "Task", List.of(
				new SwingRibbonModel.RibbonBand("a", "A", List.of(new SwingRibbonModel.RibbonButton("Unknown", SwingRibbonModel.ButtonPriority.TOP)))))), List.of());
		assertThrows(IllegalStateException.class, () -> RibbonCommandCatalog.validate(unknown));

		SwingRibbonModel unknownQuickAccess = new SwingRibbonModel(
			"test", List.of(), List.of("UnknownQuickAccess"));
		assertThrows(IllegalStateException.class, () -> RibbonCommandCatalog.validate(unknownQuickAccess));
	}

	private static SwingRibbonModel validate(Locale locale) {
		ExtToolBarFactory buttons = new ExtToolBarFactory(MenuActionMapSupport.noopActionMap(), MenuDefinitionSupport.ribbonBundles(locale));
		SwingRibbonFactory factory = new SwingRibbonFactory(buttons, MenuDefinitionSupport.ribbonBundles(locale));
		return factory.createModel(MenuManager.STANDARD_RIBBON);
	}
}
