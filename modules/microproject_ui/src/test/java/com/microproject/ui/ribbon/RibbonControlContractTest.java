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
package com.microproject.ui.ribbon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.ActionEvent;
import java.util.List;
import java.util.ListResourceBundle;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.junit.jupiter.api.Test;

import com.microproject.menu.ExtToolBarFactory;
import com.microproject.menu.MenuActionMapSupport;
import com.microproject.menu.MenuManager;
import com.microproject.menu.MenuRibbonCommandSource;
import com.microproject.menu.testsupport.MenuDefinitionSupport;
import com.microproject.ribbon.RibbonActivation;
import com.microproject.ribbon.RibbonControlKind;
import com.microproject.ribbon.SwingRibbonModel;

/** Resource parsing and popup wiring of the ribbon control-kind contract. */
class RibbonControlContractTest {
	@Test
	void commandsWithoutControlMetadataKeepTheKindImpliedByTypeAndPresentation() {
		SwingRibbonModel model = model(Map.of());
		assertEquals(RibbonControlKind.BUTTON, button(model, "RibbonCut").getControlKind());
		assertEquals(RibbonControlKind.TOGGLE_BUTTON, button(model, "RibbonFormatPainter").getControlKind());
		assertEquals(RibbonControlKind.SPLIT_BUTTON, button(model, "RibbonChooseSort").getControlKind());
		assertTrue(button(model, "RibbonCut").getItemIds().isEmpty());
		assertNull(button(model, "RibbonDelete").getKeyTip());
	}

	@Test
	void controlItemsAndKeyTipAreReadFromResources() {
		SwingRibbonModel model = model(Map.of(
			"RibbonPaste.control", "splitButton",
			"RibbonPaste.items", "RibbonPaste - RibbonCopy",
			"RibbonPaste.keytip", " V ",
			"RibbonClear.control", "MENU",
			"RibbonClear.items", "RibbonCut"));
		SwingRibbonModel.RibbonButton paste = button(model, "RibbonPaste");
		assertEquals(RibbonControlKind.SPLIT_BUTTON, paste.getControlKind());
		assertTrue(paste.isSplit());
		assertEquals(List.of("RibbonPaste", "-", "RibbonCopy"), paste.getItemIds());
		assertEquals("V", paste.getKeyTip());
		assertEquals(RibbonControlKind.MENU, button(model, "RibbonClear").getControlKind());
	}

	@Test
	void groupDialogLauncherIsReadFromResources() {
		String bandId = bandOf(model(Map.of()), "RibbonPaste").getId();
		assertNull(bandOf(model(Map.of()), "RibbonPaste").getDialogLauncherId());
		SwingRibbonModel model = model(Map.of(bandId + ".dialogLauncher", "RibbonInformation"));
		assertEquals("RibbonInformation", bandOf(model, "RibbonPaste").getDialogLauncherId());
	}

	@Test
	void invalidControlMetadataFailsModelCreation() {
		assertThrows(IllegalStateException.class, () -> model(Map.of("RibbonPaste.control", "carousel")));
		assertThrows(IllegalStateException.class, () -> model(Map.of("RibbonCut.items", "RibbonCopy")));
		assertThrows(IllegalStateException.class, () -> model(Map.of("RibbonPaste.control", "dialogBoxLauncher")));
	}

	@Test
	void splitButtonKeepsItsCommandAndGetsAnArrowThatOwnsThePopup() {
		AtomicInteger invoked = new AtomicInteger();
		JButton button = countingButton(invoked);
		RibbonControlPopups.install(button, specification(RibbonControlKind.SPLIT_BUTTON), commandSource(), key -> null);

		button.doClick(0);
		assertEquals(1, invoked.get());
		JPopupMenu popup = assertInstanceOf(JPopupMenu.class, button.getClientProperty(RibbonControlPopups.POPUP_PROPERTY));
		assertEquals(RibbonControlPopups.POPUP_NAME_PREFIX + "RibbonPaste", popup.getName());
		assertEquals(3, popup.getComponentCount());
		assertEquals("RibbonCopy", assertInstanceOf(JMenuItem.class, popup.getComponent(0)).getActionCommand());
		assertInstanceOf(JPopupMenu.Separator.class, popup.getComponent(1));
		JButton arrow = assertInstanceOf(JButton.class, button.getClientProperty(RibbonControlPopups.ARROW_BUTTON_PROPERTY));
		assertEquals(RibbonControlPopups.ARROW_BUTTON_NAME_PREFIX + "RibbonPaste", arrow.getName());
		assertEquals(button, arrow.getParent());
		assertEquals(Boolean.TRUE, button.getClientProperty("MicroProject.ribbonSplit"));
		assertEquals("V", button.getClientProperty(RibbonControlPopups.KEY_TIP_PROPERTY));
	}

	@Test
	void markOnTrackIsASplitButtonWithItsUpdateTasksSibling() {
		SwingRibbonModel.RibbonButton markOnTrack = button(model(Map.of()), "RibbonMarkOnTrack");
		assertEquals(RibbonControlKind.SPLIT_BUTTON, markOnTrack.getControlKind());
		assertEquals(List.of("RibbonMarkOnTrack", "RibbonUpdateTasks"), markOnTrack.getItemIds());
	}

	@Test
	void clipboardInsertAndFindAreSplitButtonsWithTheirCommandsInThePopup() {
		SwingRibbonModel model = model(Map.of());
		Map<String, List<String>> expected = Map.of(
			"RibbonPaste", List.of("RibbonPaste"),
			"RibbonCopy", List.of("RibbonCopy"),
			"RibbonInsert", List.of("RibbonInsert", "RibbonInsertRecurring"),
			"RibbonFind", List.of("RibbonFind"));
		expected.forEach((id, items) -> {
			SwingRibbonModel.RibbonButton button = button(model, id);
			assertEquals(RibbonControlKind.SPLIT_BUTTON, button.getControlKind(), id);
			assertEquals(items, button.getItemIds(), id);
		});
	}

	@Test
	void clearInsertResourceAndGridlinesAreMenusKeepingTheirCommands() {
		SwingRibbonModel model = model(Map.of());
		Map<String, List<String>> expected = Map.of(
			"RibbonClear", List.of("RibbonClear"),
			"RibbonInsertResource", List.of("RibbonInsertResource"),
			"RibbonGridlines", List.of("RibbonGridlines", "RibbonToggleProgressLine"));
		expected.forEach((id, items) -> {
			SwingRibbonModel.RibbonButton button = button(model, id);
			assertEquals(RibbonControlKind.MENU, button.getControlKind(), id);
			assertEquals(RibbonActivation.POPUP, button.getActivation(), id);
			assertEquals(items, button.getItemIds(), id);
		});
	}

	@Test
	void saveBaselineIsAMenuWithSetAndClearItems() {
		SwingRibbonModel.RibbonButton button = button(model(Map.of()), "RibbonSaveBaseline");
		assertEquals(RibbonControlKind.MENU, button.getControlKind());
		assertEquals(RibbonActivation.POPUP, button.getActivation());
		assertEquals(List.of("RibbonSaveBaseline", "RibbonClearBaseline"), button.getItemIds());
	}

	@Test
	void filterAndGroupAreDropDownsWhoseFixedItemOpensTheirDialog() {
		SwingRibbonModel model = model(Map.of());
		for (String id : List.of("RibbonChooseFilter", "RibbonChooseGroup")) {
			SwingRibbonModel.RibbonButton button = button(model, id);
			assertEquals(RibbonControlKind.DROP_DOWN, button.getControlKind(), id);
			assertEquals(RibbonActivation.POPUP, button.getActivation(), id);
			assertEquals(List.of(id), button.getItemIds(), id);
		}
	}

	@Test
	void dropDownShowsTheCurrentValueListsTheChoicesAndAppliesTheSelection() {
		AtomicInteger invoked = new AtomicInteger();
		JButton button = countingButton(invoked);
		button.setText("Filter");
		FakeDropDownSource source = new FakeDropDownSource();
		RibbonControlPopups.install(button, specification(RibbonControlKind.DROP_DOWN), commandSource(), key -> null,
			() -> source);

		assertEquals("All Tasks", button.getText());
		assertEquals("All Tasks", button.getClientProperty(RibbonControlPopups.DROP_DOWN_VALUE_PROPERTY));
		button.doClick(0);
		assertEquals(0, invoked.get(), "a drop-down opens its popup and has no command of its own");

		JPopupMenu popup = assertInstanceOf(JPopupMenu.class, button.getClientProperty(RibbonControlPopups.POPUP_PROPERTY));
		PopupMenuEvent event = new PopupMenuEvent(popup);
		for (PopupMenuListener listener : popup.getPopupMenuListeners()) {
			listener.popupMenuWillBecomeVisible(event);
		}
		// Choices first, then a separator, then the fixed items declared in the resources.
		assertEquals(6, popup.getComponentCount());
		JRadioButtonMenuItem first = assertInstanceOf(JRadioButtonMenuItem.class, popup.getComponent(0));
		JRadioButtonMenuItem second = assertInstanceOf(JRadioButtonMenuItem.class, popup.getComponent(1));
		assertEquals("All Tasks", first.getText());
		assertTrue(first.isSelected());
		assertEquals("Critical", second.getText());
		assertFalse(second.isSelected());
		assertInstanceOf(JPopupMenu.Separator.class, popup.getComponent(2));
		assertEquals("RibbonCopy", assertInstanceOf(JMenuItem.class, popup.getComponent(3)).getActionCommand());

		second.doClick(0);
		assertEquals(List.of("RibbonPaste:1"), source.selections);
		assertEquals("Critical", button.getText(), "the label follows the applied choice");

		// Reopening replaces the choices instead of accumulating them.
		for (PopupMenuListener listener : popup.getPopupMenuListeners()) {
			listener.popupMenuWillBecomeVisible(event);
		}
		assertEquals(6, popup.getComponentCount());
		assertTrue(assertInstanceOf(JRadioButtonMenuItem.class, popup.getComponent(1)).isSelected());

		// A value changed elsewhere (view switch, filter dialog) is picked up by the refresh hook.
		source.current = "Late Tasks";
		((Runnable) button.getClientProperty(RibbonControlPopups.REFRESH_PROPERTY)).run();
		assertEquals("Late Tasks", button.getText());
		source.current = null;
		((Runnable) button.getClientProperty(RibbonControlPopups.REFRESH_PROPERTY)).run();
		assertEquals("Filter", button.getText(), "without a value the label falls back to the caption");
	}

	private static final class FakeDropDownSource implements RibbonDropDownSource {
		String current = "All Tasks";
		final List<String> selections = new java.util.ArrayList<>();

		@Override
		public String currentValue(String commandId) {
			return current;
		}

		@Override
		public List<String> choices(String commandId) {
			return List.of("All Tasks", "Critical");
		}

		@Override
		public void select(String commandId, int index) {
			selections.add(commandId + ":" + index);
			current = choices(commandId).get(index);
		}
	}

	@Test
	void viewSwitchButtonsAreToggleSplitButtonsListingTheirFamilyOfViews() {
		SwingRibbonModel model = model(Map.of());
		List<String> taskViews = List.of("RibbonGantt", "RibbonTrackingGantt", "RibbonCalendarView", "RibbonNetwork",
			"RibbonWBS", "RibbonTaskUsageDetail");
		List<String> resourceViews = List.of("RibbonResources", "RibbonRBS", "RibbonResourceUsageDetail");
		Map<String, List<String>> expected = Map.of(
			"RibbonGantt", taskViews, "RibbonNetwork", taskViews, "RibbonCalendarView", taskViews,
			"RibbonTaskUsageDetail", taskViews, "RibbonResources", resourceViews,
			"RibbonResourceUsageDetail", resourceViews);
		expected.forEach((id, items) -> {
			SwingRibbonModel.RibbonButton button = button(model, id);
			assertEquals(RibbonControlKind.SPLIT_BUTTON, button.getControlKind(), id);
			assertTrue(button.isToggle(), id);
			assertEquals(RibbonActivation.EXCLUSIVE_VIEW, button.getActivation(), id);
			assertEquals(items, button.getItemIds(), id);
		});
	}

	@Test
	void splitToggleKeepsItsPressedStateWhenTheArrowIsUsed() {
		AtomicInteger invoked = new AtomicInteger();
		javax.swing.JToggleButton button = new javax.swing.JToggleButton(new AbstractAction("Gantt") {
			@Override
			public void actionPerformed(ActionEvent event) {
				invoked.incrementAndGet();
			}
		});
		RibbonControlPopups.install(button, specification(RibbonControlKind.SPLIT_BUTTON), commandSource(), key -> null);
		JButton arrow = assertInstanceOf(JButton.class, button.getClientProperty(RibbonControlPopups.ARROW_BUTTON_PROPERTY));

		arrow.doClick(0);
		assertFalse(button.isSelected());
		assertEquals(0, invoked.get());

		// The body keeps the toggle behavior; the application then resets the state from the model.
		button.doClick(0);
		assertEquals(1, invoked.get());
	}

	@Test
	void largeSplitButtonHasItsArrowOnTheLowerHalfAndSmallOnTheRightEdge() throws Exception {
		javax.swing.SwingUtilities.invokeAndWait(() -> {
			JButton large = countingButton(new AtomicInteger());
			large.putClientProperty(RibbonButtonStyler.SIZE_PROPERTY, "large");
			RibbonControlPopups.install(large, specification(RibbonControlKind.SPLIT_BUTTON), commandSource(), key -> null);
			large.setSize(80, 61);
			JButton largeArrow = assertInstanceOf(JButton.class, large.getClientProperty(RibbonControlPopups.ARROW_BUTTON_PROPERTY));
			assertEquals(new java.awt.Rectangle(0, 30, 80, 31), com.microproject.util.FlatUiSupport.ribbonSplitArrowBounds(large));

			JButton small = countingButton(new AtomicInteger());
			small.putClientProperty(RibbonButtonStyler.SIZE_PROPERTY, "small");
			RibbonControlPopups.install(small, specification(RibbonControlKind.SPLIT_BUTTON), commandSource(), key -> null);
			small.setSize(80, 22);
			assertEquals(new java.awt.Rectangle(62, 0, 18, 22), com.microproject.util.FlatUiSupport.ribbonSplitArrowBounds(small));
			assertNotNull(largeArrow);
		});
		// Resizing is delivered through the event queue.
		javax.swing.SwingUtilities.invokeAndWait(() -> { });
	}

	@Test
	void theArrowChildFollowsTheSizeClassOfItsButton() throws Exception {
		JButton[] buttons = new JButton[2];
		javax.swing.SwingUtilities.invokeAndWait(() -> {
			for (int i = 0; i < 2; i++) {
				buttons[i] = countingButton(new AtomicInteger());
				buttons[i].putClientProperty(RibbonButtonStyler.SIZE_PROPERTY, i == 0 ? "large" : "small");
				RibbonControlPopups.install(buttons[i], specification(RibbonControlKind.SPLIT_BUTTON), commandSource(), key -> null);
				buttons[i].setSize(80, i == 0 ? 60 : 22);
			}
		});
		javax.swing.SwingUtilities.invokeAndWait(() -> { });
		JButton largeArrow = (JButton) buttons[0].getClientProperty(RibbonControlPopups.ARROW_BUTTON_PROPERTY);
		JButton smallArrow = (JButton) buttons[1].getClientProperty(RibbonControlPopups.ARROW_BUTTON_PROPERTY);
		assertEquals(new java.awt.Rectangle(0, 30, 80, 30), largeArrow.getBounds());
		assertEquals(new java.awt.Rectangle(62, 0, 18, 22), smallArrow.getBounds());
	}

	@Test
	void menuOnlyOpensItsPopup() {
		AtomicInteger invoked = new AtomicInteger();
		JButton button = countingButton(invoked);
		RibbonControlPopups.install(button, specification(RibbonControlKind.MENU), commandSource(), key -> null);

		button.doClick(0);
		assertEquals(0, invoked.get(), "a menu control has no command of its own");
		assertEquals("Paste", button.getText(), "a menu keeps its caption");
		assertNotNull(button.getClientProperty(RibbonControlPopups.POPUP_PROPERTY));
		assertNull(button.getClientProperty(RibbonControlPopups.ARROW_BUTTON_PROPERTY));
		assertEquals(RibbonControlKind.MENU, button.getClientProperty(RibbonControlPopups.CONTROL_KIND_PROPERTY));
	}

	@Test
	void controlsWithoutItemsAreLeftAlone() {
		AtomicInteger invoked = new AtomicInteger();
		JButton button = countingButton(invoked);
		RibbonControlPopups.install(button,
			new SwingRibbonModel.RibbonButton("RibbonPaste", SwingRibbonModel.ButtonPriority.MEDIUM),
			commandSource(), key -> null);

		button.doClick(0);
		assertEquals(1, invoked.get());
		assertNull(button.getClientProperty(RibbonControlPopups.POPUP_PROPERTY));
		assertEquals(0, button.getComponentCount());
	}

	@Test
	void activationIsReadFromResourcesOrInferred() {
		SwingRibbonModel model = model(Map.of(
			"RibbonPaste.activation", " dialog ",
			"RibbonClear.control", "MENU",
			"RibbonClear.items", "RibbonCut"));
		assertEquals(RibbonActivation.DIALOG, button(model, "RibbonPaste").getActivation());
		assertEquals(RibbonActivation.POPUP, button(model, "RibbonClear").getActivation());
		assertEquals(RibbonActivation.COMMAND, button(model, "RibbonCut").getActivation());
		assertEquals(RibbonActivation.MODE, button(model, "RibbonFormatPainter").getActivation());
		assertEquals(RibbonActivation.EXCLUSIVE_VIEW, button(model, "RibbonGantt").getActivation());
		assertEquals(RibbonActivation.TOGGLE_STATE, button(model, "RibbonHistogram").getActivation());
		assertEquals(RibbonActivation.BACKSTAGE, button(model, "RibbonOpenProject").getActivation());
		assertEquals(RibbonActivation.COMMAND, button(model, "RibbonSaveProject").getActivation());
	}

	@Test
	void activationThatContradictsTheOtherMetadataFailsModelCreation() {
		assertThrows(IllegalStateException.class, () -> model(Map.of("RibbonPaste.activation", "hover")));
		// A pressed state needs type=TOGGLE, and a toggle needs a pressed state.
		assertThrows(IllegalStateException.class, () -> model(Map.of("RibbonPaste.activation", "TOGGLE_STATE")));
		assertThrows(IllegalStateException.class, () -> model(Map.of("RibbonHistogram.activation", "DIALOG")));
		// POPUP is exactly the kinds whose click only opens their popup.
		assertThrows(IllegalStateException.class, () -> model(Map.of("RibbonPaste.activation", "POPUP")));
		assertThrows(IllegalStateException.class, () -> model(Map.of(
			"RibbonClear.control", "MENU", "RibbonClear.items", "RibbonCut", "RibbonClear.activation", "DIALOG")));
		// BACKSTAGE is exactly the commands that open a Backstage page.
		assertThrows(IllegalStateException.class, () -> model(Map.of("RibbonPaste.activation", "BACKSTAGE")));
		assertThrows(IllegalStateException.class, () -> model(Map.of("RibbonOpenProject.activation", "DIALOG")));
	}

	@Test
	void buttonPublishesItsActivationAndWhetherItsPopupIsOpen() {
		JButton button = countingButton(new AtomicInteger());
		RibbonControlPopups.install(button, specification(RibbonControlKind.MENU), commandSource(), key -> null);
		assertEquals(RibbonActivation.POPUP, button.getClientProperty(RibbonControlPopups.ACTIVATION_PROPERTY));
		assertNull(button.getClientProperty(RibbonControlPopups.POPUP_OPEN_PROPERTY));

		JPopupMenu popup = assertInstanceOf(JPopupMenu.class, button.getClientProperty(RibbonControlPopups.POPUP_PROPERTY));
		PopupMenuEvent event = new PopupMenuEvent(popup);
		for (PopupMenuListener listener : popup.getPopupMenuListeners()) {
			listener.popupMenuWillBecomeVisible(event);
		}
		assertEquals(Boolean.TRUE, button.getClientProperty(RibbonControlPopups.POPUP_OPEN_PROPERTY));
		for (PopupMenuListener listener : popup.getPopupMenuListeners()) {
			listener.popupMenuWillBecomeInvisible(event);
		}
		assertNull(button.getClientProperty(RibbonControlPopups.POPUP_OPEN_PROPERTY));

		JButton plain = countingButton(new AtomicInteger());
		RibbonControlPopups.install(plain,
			new SwingRibbonModel.RibbonButton("RibbonPaste", SwingRibbonModel.ButtonPriority.MEDIUM)
				.withActivation(RibbonActivation.DIALOG),
			commandSource(), key -> null);
		assertEquals(RibbonActivation.DIALOG, plain.getClientProperty(RibbonControlPopups.ACTIVATION_PROPERTY));
	}

	@Test
	void everyKindButTheGroupLauncherHasADedicatedRenderer() {
		for (RibbonControlKind kind : RibbonControlKind.values()) {
			assertEquals(kind != RibbonControlKind.DIALOG_LAUNCHER, RibbonControlPopups.isNativelyRendered(kind), kind.name());
		}
		assertFalse(RibbonControlPopups.isNativelyRendered(RibbonControlKind.DIALOG_LAUNCHER));
	}

	@Test
	void undoAndRedoAreQuickAccessSplitButtons() {
		SwingRibbonModel model = model(Map.of());
		for (String id : List.of("RibbonTopBarUndo", "RibbonTopBarRedo")) {
			SwingRibbonModel.RibbonButton control = model.getTaskBarControls().stream()
				.filter(candidate -> candidate.getId().equals(id)).findFirst().orElseThrow();
			assertEquals(RibbonControlKind.SPLIT_BUTTON, control.getControlKind(), id);
		}
		assertEquals(List.of("RibbonTopBarSaveProject", "RibbonTopBarUndo", "RibbonTopBarRedo"), model.getTaskBarButtons());
		assertEquals(RibbonControlKind.BUTTON, model.getTaskBarControls().get(0).getControlKind());
	}

	@Test
	void historySplitButtonListsDynamicEntriesAndRunsTheChosenOne() {
		java.util.ArrayList<String> chosen = new java.util.ArrayList<>();
		java.util.ArrayList<String> history = new java.util.ArrayList<>(List.of("Edit C", "Edit B", "Edit A"));
		RibbonDropDownSource source = new RibbonDropDownSource() {
			@Override
			public String currentValue(String commandId) {
				return null;
			}

			@Override
			public List<String> choices(String commandId) {
				return List.of();
			}

			@Override
			public void select(String commandId, int index) {
			}

			@Override
			public List<String> popupEntries(String commandId) {
				return List.copyOf(history);
			}

			@Override
			public void selectPopupEntry(String commandId, int index) {
				chosen.add(commandId + ":" + index);
			}
		};
		JButton button = countingButton(new AtomicInteger());
		SwingRibbonModel.RibbonButton undo = new SwingRibbonModel.RibbonButton("RibbonTopBarUndo",
			SwingRibbonModel.ButtonPriority.LOW).withControl(RibbonControlKind.SPLIT_BUTTON, List.of(), null);
		RibbonControlPopups.install(button, undo, commandSource(), key -> null, () -> source);

		JPopupMenu popup = assertInstanceOf(JPopupMenu.class, button.getClientProperty(RibbonControlPopups.POPUP_PROPERTY));
		assertTrue(RibbonControlPopups.prepareForShow(popup));
		assertEquals(3, popup.getComponentCount());
		assertEquals("Edit B", assertInstanceOf(JMenuItem.class, popup.getComponent(1)).getText());
		((JMenuItem) popup.getComponent(1)).doClick(0);
		assertEquals(List.of("RibbonTopBarUndo:1"), chosen);

		history.clear();
		assertFalse(RibbonControlPopups.prepareForShow(popup));
	}

	private static SwingRibbonModel.RibbonButton specification(RibbonControlKind kind) {
		return new SwingRibbonModel.RibbonButton("RibbonPaste", SwingRibbonModel.ButtonPriority.MEDIUM)
			.withControl(kind, List.of("RibbonCopy", "-", "RibbonCut"), "V");
	}

	private static JButton countingButton(AtomicInteger invoked) {
		return new JButton(new AbstractAction("Paste") {
			@Override
			public void actionPerformed(ActionEvent event) {
				invoked.incrementAndGet();
			}
		});
	}

	private static MenuRibbonCommandSource commandSource() {
		return new MenuRibbonCommandSource(new ExtToolBarFactory(MenuActionMapSupport.noopActionMap(),
			MenuDefinitionSupport.ribbonBundles(Locale.ROOT)));
	}

	private static SwingRibbonModel model(Map<String, String> overrides) {
		ResourceBundle[] base = MenuDefinitionSupport.ribbonBundles(Locale.ROOT);
		ResourceBundle[] bundles = new ResourceBundle[base.length + 1];
		bundles[0] = new ListResourceBundle() {
			@Override
			protected Object[][] getContents() {
				return overrides.entrySet().stream()
					.map(entry -> new Object[] { entry.getKey(), entry.getValue() })
					.toArray(Object[][]::new);
			}
		};
		System.arraycopy(base, 0, bundles, 1, base.length);
		return new SwingRibbonFactory(new MenuRibbonCommandSource(
			new ExtToolBarFactory(MenuActionMapSupport.noopActionMap(), bundles)), bundles)
			.createModel(MenuManager.STANDARD_RIBBON);
	}

	private static SwingRibbonModel.RibbonButton button(SwingRibbonModel model, String id) {
		return bandOf(model, id).getButtons().stream()
			.filter(candidate -> candidate.getId().equals(id)).findFirst().orElseThrow();
	}

	private static SwingRibbonModel.RibbonBand bandOf(SwingRibbonModel model, String id) {
		for (SwingRibbonModel.RibbonTab tab : model.getTabs()) {
			for (SwingRibbonModel.RibbonBand band : tab.getBands()) {
				if (band.getButtons().stream().anyMatch(candidate -> candidate.getId().equals(id))) {
					return band;
				}
			}
		}
		throw new AssertionError("No ribbon band contains " + id);
	}
}
