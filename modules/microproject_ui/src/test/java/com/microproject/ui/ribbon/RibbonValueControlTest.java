/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.ui.ribbon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import com.microproject.ribbon.RibbonCommandSource;
import com.microproject.ribbon.RibbonControlKind;
import com.microproject.ribbon.SwingRibbonModel;

/** Rendering and value commit of the gallery, combo box and edit box controls. */
class RibbonValueControlTest {
	private final List<String> committed = new ArrayList<>();

	@Test
	void galleryIsAPopupGridWithFourColumnsAndCommitsTheItemId() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			JComponent control = RibbonControlPopups.createInputControl(
				spec(RibbonControlKind.GALLERY, "Sky", "Sea", "Sun", "Sand", "Snow"), source(), key -> null);
			JPopupMenu popup = assertInstanceOf(JPopupMenu.class, control);
			JPanel grid = assertInstanceOf(JPanel.class, popup.getComponent(0));
			assertEquals(4, assertInstanceOf(GridLayout.class, grid.getLayout()).getColumns());
			assertEquals(5, grid.getComponentCount());

			JToggleButton sea = (JToggleButton) grid.getComponent(1);
			sea.doClick(0);
			assertEquals(List.of("Sea"), committed);
			assertEquals("Sea", popup.getClientProperty(RibbonControlPopups.GALLERY_SELECTION_PROPERTY));
			assertTrue(sea.isSelected());
		});
	}

	@Test
	void galleryColumnsComeFromTheOwnerAndTheSelectedItemIsHighlighted() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			JComponent owner = new JPanel();
			owner.putClientProperty(RibbonControlPopups.GALLERY_COLUMNS_PROPERTY, 2);
			JPopupMenu popup = RibbonControlPopups.createGalleryPopup(owner,
				spec(RibbonControlKind.GALLERY, "Sky", "Sea", "Sun"), source(), key -> null);
			JPanel grid = (JPanel) popup.getComponent(0);
			assertEquals(2, ((GridLayout) grid.getLayout()).getColumns());

			RibbonControlPopups.selectGalleryItem(popup, "Sun");
			assertFalse(((JToggleButton) grid.getComponent(0)).isSelected());
			assertTrue(((JToggleButton) grid.getComponent(2)).isSelected());
			assertTrue(committed.isEmpty(), "highlighting a selection must not run the command");
		});
	}

	@Test
	void comboBoxIsEditableAndCommitsOnceOnEnterAndOnSelection() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			JComponent control = RibbonControlPopups.createInputControl(
				spec(RibbonControlKind.COMBO_BOX, "Small", "Large"), source(), key -> null);
			JComboBox<?> combo = assertInstanceOf(JComboBox.class, control);
			assertTrue(combo.isEditable());
			assertEquals(2, combo.getItemCount());
			assertTrue(committed.isEmpty());

			combo.setSelectedItem("Large");
			assertEquals(List.of("Large"), committed);

			JTextField editor = (JTextField) combo.getEditor().getEditorComponent();
			editor.setText("Custom");
			editor.postActionEvent();
			assertEquals(List.of("Large", "Custom"), committed);
		});
	}

	@Test
	void editBoxIsATextFieldThatCommitsItsTextOnEnter() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			JTextField field = assertInstanceOf(JTextField.class,
				RibbonControlPopups.createInputControl(spec(RibbonControlKind.EDIT_BOX), source(), key -> null));
			field.setText("12");
			assertTrue(committed.isEmpty());
			field.postActionEvent();
			assertEquals(List.of("12"), committed);
		});
	}

	@Test
	void inputValueReachesTheRealSharedDispatcher() throws Exception {
		Action command = new AbstractAction() {
			@Override public void actionPerformed(ActionEvent event) { committed.add(event.getActionCommand()); }
		};
		com.microproject.ui.menu.ProjectMenuActionMap actions = new com.microproject.ui.menu.ProjectMenuActionMap() {
			@Override public Action getAction(String key) { return command; }
			@Override public String getStringFromAction(Action action) { return "RibbonValue"; }
		};
		RibbonCommandSource actual = new com.microproject.ui.menu.MenuRibbonCommandSource(
			new com.microproject.ui.menu.ExtToolBarFactory(actions));
		SwingUtilities.invokeAndWait(() -> {
			JTextField field = (JTextField) RibbonControlPopups.createInputControl(
				spec(RibbonControlKind.EDIT_BOX), actual, key -> null);
			field.setText("chosen value");
			field.postActionEvent();
		});
		assertEquals(List.of("chosen value"), committed);
	}

	@Test
	void taskColorGalleryCommitsRgbThroughTheSharedDispatcher() throws Exception {
		java.util.concurrent.atomic.AtomicReference<String> received = new java.util.concurrent.atomic.AtomicReference<>();
		Action command = new AbstractAction("BackgroundColor") {
			@Override public void actionPerformed(ActionEvent event) { received.set(event.getActionCommand()); }
		};
		com.microproject.ui.menu.ProjectMenuActionMap actions = new com.microproject.ui.menu.ProjectMenuActionMap() {
			@Override public Action getAction(String key) { return command; }
			@Override public String getStringFromAction(Action action) { return "BackgroundColor"; }
		};
		RibbonCommandSource actual = new com.microproject.ui.menu.MenuRibbonCommandSource(
			new com.microproject.ui.menu.ExtToolBarFactory(actions,
				com.microproject.ui.menu.testsupport.MenuDefinitionSupport.ribbonBundles(java.util.Locale.ROOT)));
		java.util.ResourceBundle japaneseText = java.util.ResourceBundle.getBundle(
			"com.microproject.menu.menu", java.util.Locale.JAPAN);
		SwingUtilities.invokeAndWait(() -> {
			String commandId = "RibbonFillColor";
			JButton owner = new JButton(actual.createAction(commandId));
			var specification = new SwingRibbonModel.RibbonButton(commandId,
				SwingRibbonModel.ButtonPriority.MEDIUM).withControl(RibbonControlKind.GALLERY, List.of(), null);
			RibbonControlPopups.install(owner, specification, actual, japaneseText::getString);
			JPopupMenu popup = assertInstanceOf(JPopupMenu.class,
				owner.getClientProperty(RibbonControlPopups.POPUP_PROPERTY));
			JPanel grid = assertInstanceOf(JPanel.class,
				popup.getClientProperty(RibbonControlPopups.GALLERY_GRID_PROPERTY));
			assertEquals(10, ((GridLayout) grid.getLayout()).getColumns());
			assertEquals(60, grid.getComponentCount(), "the Theme gallery includes the six documented palette rows");
			JToggleButton accent = java.util.Arrays.stream(grid.getComponents())
				.filter(JToggleButton.class::isInstance).map(JToggleButton.class::cast)
				.filter(button -> button.getActionCommand().equals("#5B9BD5"))
				.findFirst().orElseThrow();
			assertEquals("アクセント 1 #5B9BD5", accent.getAccessibleContext().getAccessibleName());
			assertEquals(accent.getAccessibleContext().getAccessibleName(), accent.getToolTipText());
			accent.doClick(0);
		});
		assertEquals("#5B9BD5", received.get(), "the selected RGB value must arrive at the legacy action through shared dispatch");
	}

	@Test
	void colorGalleryExposesItsCommandRowsAsActionablePopupItems() throws Exception {
		java.util.concurrent.atomic.AtomicInteger dispatches = new java.util.concurrent.atomic.AtomicInteger();
		Action command = new AbstractAction("BackgroundColor") {
			@Override public void actionPerformed(ActionEvent event) { dispatches.incrementAndGet(); }
		};
		com.microproject.ui.menu.ProjectMenuActionMap actions = new com.microproject.ui.menu.ProjectMenuActionMap() {
			@Override public Action getAction(String key) { return command; }
			@Override public String getStringFromAction(Action action) { return "BackgroundColor"; }
		};
		com.microproject.ui.menu.ExtToolBarFactory factory = new com.microproject.ui.menu.ExtToolBarFactory(actions,
			com.microproject.ui.menu.testsupport.MenuDefinitionSupport.ribbonBundles(java.util.Locale.ROOT));
		RibbonCommandSource actual = new com.microproject.ui.menu.MenuRibbonCommandSource(factory);
		java.util.ResourceBundle english = java.util.ResourceBundle.getBundle(
			"com.microproject.menu.menu", java.util.Locale.ROOT);
		SwingUtilities.invokeAndWait(() -> {
			String commandId = "RibbonFillColor";
			JButton owner = new JButton(actual.createAction(commandId));
			var specification = new SwingRibbonModel.RibbonButton(commandId,
				SwingRibbonModel.ButtonPriority.MEDIUM).withControl(RibbonControlKind.GALLERY, List.of(), null);
			RibbonControlPopups.install(owner, specification, actual, english::getString);
			JPopupMenu popup = assertInstanceOf(JPopupMenu.class,
				owner.getClientProperty(RibbonControlPopups.POPUP_PROPERTY));
			List<String> actionable = com.microproject.ui.menu.testsupport.UiComponentWalker.flatten(popup).stream()
				.filter(javax.swing.AbstractButton.class::isInstance)
				.map(javax.swing.AbstractButton.class::cast)
				.map(javax.swing.AbstractButton::getActionCommand)
				.filter(id -> id != null && factory.getActionStringFromId(id) != null)
				.distinct().toList();
			assertEquals(List.of(commandId), actionable,
				"the gallery must expose command-addressable items so its popup is not inert");
			com.microproject.ui.menu.testsupport.UiComponentWalker.flatten(popup).stream()
				.filter(javax.swing.AbstractButton.class::isInstance)
				.map(javax.swing.AbstractButton.class::cast)
				.filter(button -> commandId.equals(button.getActionCommand()))
				.findFirst().orElseThrow().doClick(0);
		});
		assertEquals(1, dispatches.get(), "clicking a gallery command row must dispatch the mapped action");
	}

	@Test
	void buttonKindsAreNotInputControls() {
		assertThrows(IllegalArgumentException.class, () ->
			RibbonControlPopups.createInputControl(spec(RibbonControlKind.BUTTON), source(), key -> null));
	}

	@Test
	void controlsFollowTheSmallButtonRowHeight() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			int height = new FlatLafRibbonTheme().inlineButtonHeight();
			assertEquals(height, RibbonControlPopups.createInputControl(
				spec(RibbonControlKind.EDIT_BOX), source(), key -> null).getPreferredSize().height);
			assertEquals(height, RibbonControlPopups.createInputControl(
				spec(RibbonControlKind.COMBO_BOX, "A"), source(), key -> null).getPreferredSize().height);
		});
	}

	private static SwingRibbonModel.RibbonButton spec(RibbonControlKind kind, String... items) {
		return new SwingRibbonModel.RibbonButton("RibbonValue", SwingRibbonModel.ButtonPriority.MEDIUM)
			.withControl(kind, List.of(items), null);
	}

	/** A command source whose only working method creates an Action that records the committed value. */
	private RibbonCommandSource source() {
		Action action = new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent event) {
				committed.add(event.getActionCommand());
			}
		};
		return (RibbonCommandSource) Proxy.newProxyInstance(getClass().getClassLoader(),
			new Class<?>[] { RibbonCommandSource.class }, (proxy, method, args) -> {
				if (method.getName().equals("createAction")) {
					return action;
				}
				if (method.getName().equals("dispatch")) {
					var invocation = (com.microproject.ribbon.RibbonCommandInvocation) args[0];
					action.actionPerformed(new ActionEvent(invocation.source(), ActionEvent.ACTION_PERFORMED, invocation.value()));
					return com.microproject.ribbon.RibbonCommandResult.dispatched(invocation.commandId());
				}
				throw new UnsupportedOperationException(method.getName());
			});
	}
}
