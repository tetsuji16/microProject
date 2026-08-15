package com.projectlibre.ui.ribbon;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.ListResourceBundle;
import java.util.Locale;
import java.util.Set;

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
