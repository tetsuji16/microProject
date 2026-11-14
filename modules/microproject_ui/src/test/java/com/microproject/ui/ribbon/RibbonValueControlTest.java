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
				throw new UnsupportedOperationException(method.getName());
			});
	}
}
