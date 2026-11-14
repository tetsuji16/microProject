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

import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.function.UnaryOperator;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import com.microproject.ribbon.RibbonActivation;
import com.microproject.ribbon.RibbonCommandSource;
import com.microproject.ribbon.RibbonControlKind;
import com.microproject.ribbon.SwingRibbonModel;

/**
 * Applies the input behavior of a {@link RibbonControlKind} to the Swing button
 * that {@link ModernRibbonPanel} created for a command.
 *
 * <p>Split buttons keep their command on the primary area and get a separate
 * arrow hit area that opens the popup. Menu-like controls have no command of
 * their own, so a click anywhere opens the popup. Kinds without a dedicated
 * renderer yet (see {@link #isNativelyRendered}) fall back to the closest of
 * those two behaviors so a declared control is never inert.</p>
 */
final class RibbonControlPopups {
	/** Client property holding the {@link RibbonControlKind} of a ribbon button. */
	static final String CONTROL_KIND_PROPERTY = "MicroProject.ribbon.controlKind";
	/** Client property holding the {@link JPopupMenu} of a popup-owning control. */
	static final String POPUP_PROPERTY = "MicroProject.ribbon.controlMenu";
	/** Client property holding the arrow hit area of a split button. */
	static final String ARROW_BUTTON_PROPERTY = "MicroProject.ribbon.controlMenuButton";
	/** Client property holding the declared key tip of a ribbon button. */
	static final String KEY_TIP_PROPERTY = "MicroProject.ribbon.keyTip";
	/** Client property holding the {@link RibbonActivation} of a ribbon button. */
	static final String ACTIVATION_PROPERTY = "MicroProject.ribbon.activation";
	/**
	 * Client property that is {@link Boolean#TRUE} while the popup of the control is
	 * showing. Office draws the control (the arrow part of a split button) pressed
	 * for exactly that time.
	 */
	static final String POPUP_OPEN_PROPERTY = "MicroProject.ribbon.popupOpen";
	static final String POPUP_NAME_PREFIX = "microproject.ribbon.controlMenu.";
	static final String ARROW_BUTTON_NAME_PREFIX = "microproject.ribbon.controlMenuButton.";
	private static final String SPLIT_PROPERTY = "MicroProject.ribbonSplit";
	private static final String ITEM_SEPARATOR = "-";

	private RibbonControlPopups() {
	}

	/**
	 * True when the ribbon draws and drives this kind as Office does. The other
	 * kinds are accepted by the model and rendered with the nearest supported
	 * behavior until their renderer exists.
	 */
	static boolean isNativelyRendered(RibbonControlKind kind) {
		return switch (kind) {
			case BUTTON, TOGGLE_BUTTON, SPLIT_BUTTON, MENU -> true;
			case GALLERY, DROP_DOWN, COMBO_BOX, CHECK_BOX, EDIT_BOX, DIALOG_LAUNCHER -> false;
		};
	}

	/**
	 * Records the control contract on the button and, when the control declares
	 * popup items, wires the popup.
	 *
	 * @param text resolves a resource key to its localized string, or {@code null}
	 */
	static void install(AbstractButton button, SwingRibbonModel.RibbonButton specification,
		RibbonCommandSource commandSource, UnaryOperator<String> text) {
		RibbonControlKind kind = specification.getControlKind();
		button.putClientProperty(CONTROL_KIND_PROPERTY, kind);
		button.putClientProperty(ACTIVATION_PROPERTY, specification.getActivation());
		if (specification.getKeyTip() != null) {
			button.putClientProperty(KEY_TIP_PROPERTY, specification.getKeyTip());
		}
		if (!kind.hasPopup() || specification.getItemIds().isEmpty()) {
			return;
		}
		JPopupMenu popup = createPopup(specification, commandSource, text);
		button.putClientProperty(POPUP_PROPERTY, popup);
		trackPopupOpen(button, popup);
		button.putClientProperty(SPLIT_PROPERTY, Boolean.TRUE);
		if (kind.invokesOnClick()) {
			installArrowHitArea(button, specification.getId(), popup);
		} else {
			openPopupOnClick(button, popup);
		}
	}

	private static JPopupMenu createPopup(SwingRibbonModel.RibbonButton specification,
		RibbonCommandSource commandSource, UnaryOperator<String> text) {
		JPopupMenu popup = new JPopupMenu();
		popup.setName(POPUP_NAME_PREFIX + specification.getId());
		for (String itemId : specification.getItemIds()) {
			if (ITEM_SEPARATOR.equals(itemId)) {
				popup.addSeparator();
				continue;
			}
			JMenuItem item = new JMenuItem(commandSource.createAction(itemId));
			String itemText = text.apply(itemId + ".text");
			item.setText(itemText == null ? itemId : itemText);
			String tooltip = text.apply(itemId + ".tooltip");
			if (tooltip != null) {
				item.setToolTipText(tooltip);
			}
			item.setActionCommand(itemId);
			item.setName(popup.getName() + "." + itemId);
			popup.add(item);
		}
		return popup;
	}

	/** Publishes {@link #POPUP_OPEN_PROPERTY} for as long as the popup is showing. */
	private static void trackPopupOpen(AbstractButton button, JPopupMenu popup) {
		popup.addPopupMenuListener(new PopupMenuListener() {
			@Override
			public void popupMenuWillBecomeVisible(PopupMenuEvent event) {
				setPopupOpen(true);
			}

			@Override
			public void popupMenuWillBecomeInvisible(PopupMenuEvent event) {
				setPopupOpen(false);
			}

			@Override
			public void popupMenuCanceled(PopupMenuEvent event) {
				setPopupOpen(false);
			}

			private void setPopupOpen(boolean open) {
				button.putClientProperty(POPUP_OPEN_PROPERTY, open ? Boolean.TRUE : null);
				button.repaint();
			}
		});
	}

	/** Menu-like controls: the button keeps its caption and icon but only opens the popup. */
	private static void openPopupOnClick(AbstractButton button, JPopupMenu popup) {
		Action command = button.getAction();
		for (ActionListener listener : button.getActionListeners()) {
			button.removeActionListener(listener);
		}
		ActionListener opener = event -> {
			if (button.isShowing() && button.isEnabled()) {
				popup.show(button, 0, button.getHeight());
			}
		};
		if (command == null) {
			button.addActionListener(opener);
			return;
		}
		// Removing the command from the button would also drop its enabled and
		// selected state, so the button keeps an Action that mirrors the command
		// and only opens the popup.
		AbstractAction mirror = new AbstractAction() {
			@Override
			public void actionPerformed(java.awt.event.ActionEvent event) {
				opener.actionPerformed(event);
			}
		};
		for (String key : new String[] { Action.NAME, Action.SMALL_ICON, Action.LARGE_ICON_KEY, Action.SHORT_DESCRIPTION,
			Action.LONG_DESCRIPTION, Action.SELECTED_KEY }) {
			mirror.putValue(key, command.getValue(key));
		}
		mirror.setEnabled(command.isEnabled());
		command.addPropertyChangeListener(change -> {
			String name = change.getPropertyName();
			if ("enabled".equals(name)) {
				mirror.setEnabled(command.isEnabled());
			} else if (name != null) {
				mirror.putValue(name, command.getValue(name));
			}
		});
		String text = button.getText();
		javax.swing.Icon icon = button.getIcon();
		String tooltip = button.getToolTipText();
		String actionCommand = button.getActionCommand();
		int mnemonic = button.getMnemonic();
		button.setAction(mirror);
		button.setText(text);
		button.setIcon(icon);
		button.setToolTipText(tooltip);
		button.setActionCommand(actionCommand);
		button.setMnemonic(mnemonic);
	}

	/** Split buttons: a transparent child over the arrow strip opens the popup. */
	private static void installArrowHitArea(AbstractButton button, String commandId, JPopupMenu popup) {
		JButton arrow = new JButton();
		arrow.setName(ARROW_BUTTON_NAME_PREFIX + commandId);
		arrow.setFocusable(true);
		arrow.setBorderPainted(false);
		arrow.setContentAreaFilled(false);
		arrow.setOpaque(false);
		arrow.setEnabled(button.isEnabled());
		arrow.getAccessibleContext().setAccessibleName(button.getText() == null ? commandId : button.getText());
		arrow.addActionListener(event -> {
			if (arrow.isShowing() && button.isEnabled()) {
				popup.show(button, 0, button.getHeight());
			}
		});
		button.addPropertyChangeListener("enabled", event -> arrow.setEnabled(button.isEnabled()));
		button.putClientProperty(ARROW_BUTTON_PROPERTY, arrow);
		button.setLayout(null);
		button.add(arrow);
		int arrowWidth = RibbonButtonStyler.SPLIT_BUTTON_EXTRA_WIDTH;
		Runnable resize = () -> arrow.setBounds(Math.max(0, button.getWidth() - arrowWidth), 0,
			Math.min(arrowWidth, button.getWidth()), button.getHeight());
		button.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent event) {
				resize.run();
			}

			@Override
			public void componentShown(ComponentEvent event) {
				resize.run();
			}
		});
		resize.run();
	}
}
