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

import java.awt.Container;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.JMenuItem;
import javax.swing.JMenu;
import javax.swing.MenuElement;
import javax.swing.MenuSelectionManager;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import com.microproject.ribbon.RibbonActivation;
import com.microproject.ribbon.RibbonCommandSource;
import com.microproject.ribbon.RibbonControlKind;
import com.microproject.ribbon.SwingRibbonModel;
import com.microproject.ui.util.FlatUiSupport;

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
	static final String ARROW_BUTTON_PROPERTY = FlatUiSupport.SPLIT_ARROW_PROPERTY;
	/** Client property holding the declared key tip of a ribbon button. */
	static final String KEY_TIP_PROPERTY = "MicroProject.ribbon.keyTip";
	/** Client property holding the {@link RibbonActivation} of a ribbon button. */
	static final String ACTIVATION_PROPERTY = "MicroProject.ribbon.activation";
	/**
	 * Client property that is {@link Boolean#TRUE} while the popup of the control is
	 * showing. Office draws the control (the arrow part of a split button) pressed
	 * for exactly that time.
	 */
	static final String POPUP_OPEN_PROPERTY = FlatUiSupport.POPUP_OPEN_PROPERTY;
	/**
	 * Client property holding the {@link Runnable} that re-reads the current value
	 * of a drop-down and shows it as the control's label.
	 */
	static final String REFRESH_PROPERTY = "MicroProject.ribbon.dropDownRefresh";
	/** Client property holding the current value shown by a drop-down, or absent when there is none. */
	static final String DROP_DOWN_VALUE_PROPERTY = "MicroProject.ribbon.dropDownValue";
	/**
	 * Client property of the popup holding the {@link Runnable} that re-reads its
	 * dynamic entries (see {@link #DYNAMIC_ENTRY_COMMANDS}); absent for fixed popups.
	 */
	static final String ENTRIES_REFRESH_PROPERTY = "MicroProject.ribbon.popupEntriesRefresh";
	/**
	 * Commands whose popup lists entries produced just before it opens, by
	 * {@link RibbonDropDownSource#popupEntries}, instead of fixed {@code <id>.items}.
	 */
	static final java.util.Set<String> DYNAMIC_ENTRY_COMMANDS = java.util.Set.of("RibbonTopBarUndo", "RibbonTopBarRedo",
		"RibbonSwitchWindows", "RibbonReportRecent");
	/** Client property (an {@link Integer}) of a gallery's button naming the grid's column count. */
	static final String GALLERY_COLUMNS_PROPERTY = "MicroProject.ribbon.galleryColumns";
	/** Client property of a gallery popup holding the id of the selected item, or absent. */
	static final String GALLERY_SELECTION_PROPERTY = "MicroProject.ribbon.gallerySelection";
	/** Client property of a gallery popup holding its item grid. */
	static final String GALLERY_GRID_PROPERTY = "MicroProject.ribbon.galleryGrid";
	static final int DEFAULT_GALLERY_COLUMNS = 4;
	static final String INPUT_NAME_PREFIX = "microproject.ribbon.input.";
	static final String POPUP_NAME_PREFIX = "microproject.ribbon.controlMenu.";
	static final String ARROW_BUTTON_NAME_PREFIX = "microproject.ribbon.controlMenuButton.";
	private static final String SPLIT_PROPERTY = "MicroProject.ribbonSplit";
	private static final String ITEM_SEPARATOR = "-";
	private static final String CHOICE_PROPERTY = "MicroProject.ribbon.dropDownChoice";
	private static final int MAX_LABEL_LENGTH = 16;

	/** Mirrors a command without letting the long-lived command retain its button tree. */
	private static final class WeakActionMirrorListener implements PropertyChangeListener {
		private final WeakReference<AbstractAction> mirror;

		WeakActionMirrorListener(AbstractAction mirror) {
			this.mirror = new WeakReference<>(mirror);
		}

		@Override
		public void propertyChange(PropertyChangeEvent event) {
			AbstractAction target = mirror.get();
			if (target == null) {
				if (event.getSource() instanceof Action source) {
					source.removePropertyChangeListener(this);
				}
				return;
			}
			if (!(event.getSource() instanceof Action source)) {
				return;
			}
			String name = event.getPropertyName();
			if ("enabled".equals(name)) {
				target.setEnabled(source.isEnabled());
			} else if (name != null && !java.util.Objects.equals(target.getValue(name), source.getValue(name))) {
				// AbstractAction fires for null-to-null, so an unguarded copy can ping-pong with the binding.
				target.putValue(name, source.getValue(name));
			}
		}
	}

	private RibbonControlPopups() {
	}

	/**
	 * Returns a native submenu facade for a popup-only command placed inside another
	 * ribbon popup. Reusing the original popup keeps its listeners and dynamic item
	 * source intact while allowing Swing to maintain the complete menu-selection path.
	 */
	static JMenu createNestedMenu(AbstractButton button) {
		Object kindValue = button.getClientProperty(CONTROL_KIND_PROPERTY);
		Object popupValue = button.getClientProperty(POPUP_PROPERTY);
		if (!(kindValue instanceof RibbonControlKind kind) || kind.invokesOnClick()
			|| !(popupValue instanceof JPopupMenu popup)) {
			return null;
		}
		return new ExistingPopupMenu(button, popup);
	}

	/**
	 * Wraps a transient split button in a direct-child JMenu so BasicMenuUI can keep
	 * the containing popup in its selection path. The primary button and its arrow
	 * remain the physical hit targets inside that row.
	 */
	static JMenu createNestedSplitMenu(AbstractButton button) {
		return createNestedSplitMenu(button, MenuSelectionManager.defaultManager(), javax.swing.SwingUtilities::invokeLater);
	}

	static JMenu createNestedSplitMenu(AbstractButton button, MenuSelectionManager selectionManager,
		Consumer<Runnable> defer) {
		if (button.getClientProperty(CONTROL_KIND_PROPERTY) != RibbonControlKind.SPLIT_BUTTON
			|| !(button.getClientProperty(POPUP_PROPERTY) instanceof JPopupMenu popup)
			|| !(button.getClientProperty(ARROW_BUTTON_PROPERTY) instanceof JButton arrow)) {
			return null;
		}
		if (arrow.getParent() != button) {
			return null;
		}
		ExistingPopupMenu menu = new ExistingPopupMenu(button, popup, true);
		for (ActionListener listener : arrow.getActionListeners()) {
			arrow.removeActionListener(listener);
		}
		arrow.addActionListener(event -> {
			if (arrow.isShowing() && button.isEnabled() && prepareForShow(popup)) {
				selectNestedPopupPath(menu, popup, selectionManager);
			}
		});
		button.addActionListener(event -> defer.accept(selectionManager::clearSelectedPath));
		menu.setLayout(null);
		menu.attachSplitPrimary(button);
		menu.setPreferredSize(button.getPreferredSize());
		menu.setMinimumSize(button.getMinimumSize());
		menu.setMaximumSize(button.getMaximumSize());
		return menu;
	}

	/** Selects the containing popup, its split menu and the existing submenu as one Swing path. */
	static boolean selectNestedPopupPath(JMenu menu, JPopupMenu popup) {
		return selectNestedPopupPath(menu, popup, MenuSelectionManager.defaultManager());
	}

	/**
	 * Selects the nested popup path through the supplied manager. The overload keeps
	 * path construction testable without asking headless Swing to show native popups.
	 */
	static boolean selectNestedPopupPath(JMenu menu, JPopupMenu popup, MenuSelectionManager selectionManager) {
		if (!(menu.getParent() instanceof JPopupMenu outerPopup)) {
			return false;
		}
		selectionManager.setSelectedPath(new MenuElement[] { outerPopup, menu, popup });
		return true;
	}

	/** JMenu backed by the existing control popup, rather than an independent popup. */
	private static final class ExistingPopupMenu extends JMenu {
		private final AbstractButton source;
		private final JPopupMenu popup;
		private final boolean splitHost;

		ExistingPopupMenu(AbstractButton source, JPopupMenu popup) {
			this(source, popup, false);
		}

		ExistingPopupMenu(AbstractButton source, JPopupMenu popup, boolean splitHost) {
			this.source = source;
			this.popup = popup;
			this.splitHost = splitHost;
			// JMenu's own popup is normally assigned this invoker by
			// ensurePopupMenuCreated(). This facade returns an existing popup instead,
			// so establish the same invoker relationship explicitly; JPopupMenu's
			// menuSelectionChanged() opens a submenu only for a JMenu invoker.
			popup.setInvoker(this);
			copyPresentation();
			for (String property : new String[] { "text", "icon", "toolTipText", "enabled", "mnemonic", "actionCommand" }) {
				source.addPropertyChangeListener(property, event -> copyPresentation());
			}
			putClientProperty(CONTROL_KIND_PROPERTY, source.getClientProperty(CONTROL_KIND_PROPERTY));
			putClientProperty(POPUP_PROPERTY, popup);
			putClientProperty(KEY_TIP_PROPERTY, splitHost ? null : source.getClientProperty(KEY_TIP_PROPERTY));
			popup.addPopupMenuListener(new PopupMenuListener() {
				@Override
				public void popupMenuWillBecomeVisible(PopupMenuEvent event) {
					putClientProperty(POPUP_OPEN_PROPERTY, Boolean.TRUE);
					repaint();
				}

				@Override
				public void popupMenuWillBecomeInvisible(PopupMenuEvent event) {
					putClientProperty(POPUP_OPEN_PROPERTY, null);
					repaint();
				}

				@Override
				public void popupMenuCanceled(PopupMenuEvent event) {
					putClientProperty(POPUP_OPEN_PROPERTY, null);
					repaint();
				}
			});
		}

		void attachSplitPrimary(AbstractButton primary) {
			// JMenu.add(Component) adds to its submenu. Use Container.addImpl here
			// because the split primary is a visual hit target in the same row.
			super.addImpl(primary, null, -1);
		}

		private void copyPresentation() {
			setText(splitHost ? null : source.getText());
			setIcon(splitHost ? null : source.getIcon());
			setToolTipText(source.getToolTipText());
			setEnabled(source.isEnabled());
			setMnemonic(source.getMnemonic());
			setActionCommand(splitHost ? null : source.getActionCommand());
			setFont(source.getFont());
			setForeground(source.getForeground());
			setBackground(source.getBackground());
			setName(splitHost ? ARROW_BUTTON_NAME_PREFIX + source.getActionCommand() : source.getName());
			putClientProperty(RibbonButtonStyler.SIZE_PROPERTY,
				source.getClientProperty(RibbonButtonStyler.SIZE_PROPERTY));
			putClientProperty(RibbonButtonStyler.ICON_KEY_PROPERTY,
				source.getClientProperty(RibbonButtonStyler.ICON_KEY_PROPERTY));
			putClientProperty("MicroProject.ribbonSplit", source.getClientProperty("MicroProject.ribbonSplit"));
			getAccessibleContext().setAccessibleName(source.getAccessibleContext().getAccessibleName());
			getAccessibleContext().setAccessibleDescription(source.getAccessibleContext().getAccessibleDescription());
		}

		@Override
		public void doLayout() {
			if (splitHost) {
				source.setBounds(0, 0, getWidth(), getHeight());
				source.doLayout();
				return;
			}
			super.doLayout();
		}

		@Override
		public JPopupMenu getPopupMenu() {
			return popup;
		}

		@Override
		public boolean isPopupMenuVisible() {
			return popup.isVisible();
		}

		@Override
		public MenuElement[] getSubElements() {
			return new MenuElement[] { popup };
		}
	}

	/**
	 * True when the ribbon draws and drives this kind as Office does. The other
	 * kinds are accepted by the model and rendered with the nearest supported
	 * behavior until their renderer exists.
	 */
	static boolean isNativelyRendered(RibbonControlKind kind) {
		return switch (kind) {
			case BUTTON, TOGGLE_BUTTON, SPLIT_BUTTON, MENU, DROP_DOWN, CHECK_BOX, GALLERY, COMBO_BOX, EDIT_BOX -> true;
			case DIALOG_LAUNCHER -> false;
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
		install(button, specification, commandSource, text, () -> null);
	}

	/**
	 * @param dropDownSource supplies the values of {@code DROP_DOWN} controls; it is
	 *        asked each time the value or the popup is needed, so it may be set after
	 *        the ribbon was built
	 */
	static void install(AbstractButton button, SwingRibbonModel.RibbonButton specification,
		RibbonCommandSource commandSource, UnaryOperator<String> text,
		Supplier<RibbonDropDownSource> dropDownSource) {
		RibbonControlKind kind = specification.getControlKind();
		button.putClientProperty(CONTROL_KIND_PROPERTY, kind);
		button.putClientProperty(ACTIVATION_PROPERTY, specification.getActivation());
		if (specification.getKeyTip() != null) {
			button.putClientProperty(KEY_TIP_PROPERTY, specification.getKeyTip());
		}
		// On a ribbon button a combo box shows its current value and lists its choices like a
		// drop-down; the editable JComboBox form is only built by createInputControl.
		boolean dropDown = kind == RibbonControlKind.DROP_DOWN || kind == RibbonControlKind.COMBO_BOX;
		boolean dynamic = DYNAMIC_ENTRY_COMMANDS.contains(specification.getId());
		boolean taskColorGallery = TaskColorGallery.supports(specification.getId());
		if (!kind.hasPopup() || (specification.getItemIds().isEmpty() && !dropDown && !dynamic && !taskColorGallery)) {
			return;
		}
		// A gallery on a ribbon button lists commands that run their own actions (for example
		// Sort), so it is built like a menu; createInputControl keeps the value-committing grid.
		JPopupMenu popup = taskColorGallery
			? TaskColorGallery.createPopup(specification, commandSource, text)
			: createPopup(specification, commandSource, text);
		button.putClientProperty(POPUP_PROPERTY, popup);
		trackPopupOpen(button, popup);
		if (dropDown) {
			installDropDown(button, specification.getId(), popup, dropDownSource);
		}
		if (dynamic) {
			installDynamicEntries(popup, specification.getId(), dropDownSource);
		}
		button.putClientProperty(SPLIT_PROPERTY, Boolean.TRUE);
		if (kind.invokesOnClick()) {
			installArrowHitArea(button, specification.getId(), popup);
		} else {
			openPopupOnClick(button, popup);
		}
	}

	/**
	 * Creates the component of a value-carrying control that Office draws as itself rather
	 * than as a button: a popup grid for {@code GALLERY}, an editable {@link JComboBox} for
	 * {@code COMBO_BOX} and a {@link JTextField} for {@code EDIT_BOX}. Committing a value
	 * runs the command's Action once with the value as the {@code actionCommand} of the event.
	 *
	 * @throws IllegalArgumentException for the other kinds, which are buttons
	 */
	static JComponent createInputControl(SwingRibbonModel.RibbonButton specification,
		RibbonCommandSource commandSource, UnaryOperator<String> text) {
		return switch (specification.getControlKind()) {
			case GALLERY -> TaskColorGallery.supports(specification.getId())
				? TaskColorGallery.createPopup(specification, commandSource, text)
				: createGalleryPopup(null, specification, commandSource, text);
			case COMBO_BOX -> createComboBox(specification, commandSource, text);
			case EDIT_BOX -> createEditBox(specification, commandSource);
			default -> throw new IllegalArgumentException(specification.getControlKind() + " is not an input control");
		};
	}

	/** Passes a committed value to the command of the control; the value is the event's action command. */
	private static void commit(RibbonCommandSource commandSource, String commandId, Object source, String value) {
		Action action = commandSource.createAction(commandId);
		if (action != null && action.isEnabled()) {
			commandSource.dispatch(new com.microproject.ribbon.RibbonCommandInvocation(commandId,
				com.microproject.ribbon.RibbonCommandInvocation.Origin.INPUT_CONTROL, source, value));
		}
	}

	private static int controlHeight() {
		return new FlatLafRibbonTheme().inlineButtonHeight();
	}

	private static java.awt.Font controlFont() {
		return new FlatLafRibbonTheme().buttonFont();
	}

	/**
	 * A popup that lays the items out as an icon grid, {@value #DEFAULT_GALLERY_COLUMNS}
	 * columns unless {@code owner} (or, without an owner, nothing) carries
	 * {@link #GALLERY_COLUMNS_PROPERTY}. The selected item is highlighted; picking an item
	 * selects it and commits the item id.
	 */
	static JPopupMenu createGalleryPopup(JComponent owner, SwingRibbonModel.RibbonButton specification,
		RibbonCommandSource commandSource, UnaryOperator<String> text) {
		JPopupMenu popup = new JPopupMenu();
		popup.setName(POPUP_NAME_PREFIX + specification.getId());
		int columns = DEFAULT_GALLERY_COLUMNS;
		if (owner != null && owner.getClientProperty(GALLERY_COLUMNS_PROPERTY) instanceof Integer declared && declared > 0) {
			columns = declared;
		}
		javax.swing.JPanel grid = new javax.swing.JPanel(new java.awt.GridLayout(0, columns, 2, 2));
		grid.setOpaque(false);
		javax.swing.ButtonGroup group = new javax.swing.ButtonGroup();
		for (String itemId : specification.getItemIds()) {
			if (ITEM_SEPARATOR.equals(itemId)) {
				continue;
			}
			Action itemAction = commandSource.createAction(itemId);
			javax.swing.JToggleButton cell = new javax.swing.JToggleButton();
			cell.setName(popup.getName() + "." + itemId);
			cell.setActionCommand(itemId);
			javax.swing.Icon icon = itemAction == null ? null : (javax.swing.Icon) itemAction.getValue(Action.SMALL_ICON);
			String itemText = text.apply(itemId + ".text");
			if (icon != null) {
				cell.setIcon(icon);
			} else {
				cell.setText(itemText == null ? itemId : itemText);
			}
			cell.setToolTipText(itemText == null ? itemId : itemText);
			cell.setFont(controlFont());
			cell.setFocusable(false);
			cell.addActionListener(event -> {
				popup.putClientProperty(GALLERY_SELECTION_PROPERTY, itemId);
				popup.setVisible(false);
				commit(commandSource, specification.getId(), cell, itemId);
			});
			group.add(cell);
			grid.add(cell);
		}
		popup.putClientProperty(GALLERY_GRID_PROPERTY, grid);
		popup.add(grid);
		popup.addPropertyChangeListener(GALLERY_SELECTION_PROPERTY, event -> highlightGallerySelection(grid, (String) event.getNewValue()));
		return popup;
	}

	/** Highlights {@code itemId} in the gallery {@code popup}; {@code null} clears the selection. */
	static void selectGalleryItem(JPopupMenu popup, String itemId) {
		popup.putClientProperty(GALLERY_SELECTION_PROPERTY, itemId);
	}

	private static void highlightGallerySelection(javax.swing.JPanel grid, String itemId) {
		for (java.awt.Component component : grid.getComponents()) {
			if (component instanceof javax.swing.JToggleButton cell) {
				cell.setSelected(cell.getActionCommand().equals(itemId));
			}
		}
	}

	private static JComboBox<String> createComboBox(SwingRibbonModel.RibbonButton specification,
		RibbonCommandSource commandSource, UnaryOperator<String> text) {
		JComboBox<String> combo = new JComboBox<>();
		combo.setName(INPUT_NAME_PREFIX + specification.getId());
		combo.setEditable(true);
		combo.setFont(controlFont());
		for (String itemId : specification.getItemIds()) {
			if (!ITEM_SEPARATOR.equals(itemId)) {
				String itemText = text.apply(itemId + ".text");
				combo.addItem(itemText == null ? itemId : itemText);
			}
		}
		combo.setSelectedIndex(-1);
		java.awt.Dimension size = combo.getPreferredSize();
		combo.setPreferredSize(new java.awt.Dimension(size.width, controlHeight()));
		// Enter in the editor also changes the selection, which would commit a second time.
		boolean[] committingFromEditor = { false };
		if (combo.getEditor().getEditorComponent() instanceof JTextField editor) {
			editor.addActionListener(event -> {
				committingFromEditor[0] = true;
				javax.swing.SwingUtilities.invokeLater(() -> committingFromEditor[0] = false);
				commit(commandSource, specification.getId(), combo, editor.getText());
			});
		}
		combo.addActionListener(event -> {
			if (!committingFromEditor[0] && combo.getSelectedItem() != null) {
				commit(commandSource, specification.getId(), combo, combo.getSelectedItem().toString());
			}
		});
		return combo;
	}

	private static JTextField createEditBox(SwingRibbonModel.RibbonButton specification, RibbonCommandSource commandSource) {
		JTextField field = new JTextField(12);
		field.setName(INPUT_NAME_PREFIX + specification.getId());
		field.setFont(controlFont());
		java.awt.Dimension size = field.getPreferredSize();
		field.setPreferredSize(new java.awt.Dimension(size.width, controlHeight()));
		field.addActionListener(event -> commit(commandSource, specification.getId(), field, field.getText()));
		return field;
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

	/**
	 * Drop-downs show the value in effect as their label and list the choices of the
	 * {@link RibbonDropDownSource} above the fixed items of the popup. The choices
	 * are read each time the popup opens, so they follow the active document and view.
	 */
	private static void installDropDown(AbstractButton button, String commandId, JPopupMenu popup,
		Supplier<RibbonDropDownSource> sourceSupplier) {
		String caption = button.getText();
		Runnable refresh = () -> {
			RibbonDropDownSource source = sourceSupplier.get();
			String value = source == null ? null : source.currentValue(commandId);
			boolean present = value != null && !value.isBlank();
			button.setText(present ? abbreviate(value) : caption);
			button.putClientProperty(DROP_DOWN_VALUE_PROPERTY, present ? value : null);
			if (present) {
				button.getAccessibleContext().setAccessibleDescription(value);
			}
		};
		button.putClientProperty(REFRESH_PROPERTY, refresh);
		popup.addPopupMenuListener(new PopupMenuListener() {
			@Override
			public void popupMenuWillBecomeVisible(PopupMenuEvent event) {
				rebuildChoices(popup, commandId, sourceSupplier.get(), refresh);
			}

			@Override
			public void popupMenuWillBecomeInvisible(PopupMenuEvent event) {
			}

			@Override
			public void popupMenuCanceled(PopupMenuEvent event) {
			}
		});
		refresh.run();
	}

	/**
	 * Lists the {@link RibbonDropDownSource#popupEntries entries} of the source in the
	 * popup each time it is about to open, so they follow the active document.
	 */
	private static void installDynamicEntries(JPopupMenu popup, String commandId,
		Supplier<RibbonDropDownSource> sourceSupplier) {
		Runnable refresh = () -> rebuildEntries(popup, commandId, sourceSupplier.get());
		popup.putClientProperty(ENTRIES_REFRESH_PROPERTY, refresh);
		popup.addPopupMenuListener(new PopupMenuListener() {
			@Override
			public void popupMenuWillBecomeVisible(PopupMenuEvent event) {
				refresh.run();
			}

			@Override
			public void popupMenuWillBecomeInvisible(PopupMenuEvent event) {
			}

			@Override
			public void popupMenuCanceled(PopupMenuEvent event) {
			}
		});
	}

	private static void rebuildEntries(JPopupMenu popup, String commandId, RibbonDropDownSource source) {
		popup.removeAll();
		List<String> entries = source == null ? List.of() : new ArrayList<>(source.popupEntries(commandId));
		for (int index = 0; index < entries.size(); index++) {
			int entry = index;
			JMenuItem item = new JMenuItem(entries.get(index));
			item.setName(popup.getName() + ".entry." + index);
			item.addActionListener(event -> source.selectPopupEntry(commandId, entry));
			popup.add(item);
		}
	}

	/**
	 * Re-reads the dynamic entries of {@code popup} and reports whether it has anything
	 * to show. Popups without dynamic entries always do.
	 */
	static boolean prepareForShow(JPopupMenu popup) {
		if (popup.getClientProperty(ENTRIES_REFRESH_PROPERTY) instanceof Runnable refresh) {
			refresh.run();
			return popup.getComponentCount() > 0;
		}
		return true;
	}

	private static void rebuildChoices(JPopupMenu popup, String commandId, RibbonDropDownSource source, Runnable refresh) {
		for (java.awt.Component component : popup.getComponents()) {
			if (component instanceof javax.swing.JComponent swing && swing.getClientProperty(CHOICE_PROPERTY) != null) {
				popup.remove(component);
			}
		}
		List<String> choices = source == null ? List.of() : new ArrayList<>(source.choices(commandId));
		if (choices.isEmpty()) {
			return;
		}
		String current = source.currentValue(commandId);
		int position = 0;
		for (int index = 0; index < choices.size(); index++) {
			int choice = index;
			String name = choices.get(index);
			JRadioButtonMenuItem item = new JRadioButtonMenuItem(name, name.equals(current));
			item.putClientProperty(CHOICE_PROPERTY, Boolean.TRUE);
			item.setName(popup.getName() + ".choice." + index);
			item.addActionListener(event -> {
				source.select(commandId, choice);
				refresh.run();
			});
			popup.insert(item, position++);
		}
		if (popup.getComponentCount() > position) {
			javax.swing.JPopupMenu.Separator separator = new javax.swing.JPopupMenu.Separator();
			separator.putClientProperty(CHOICE_PROPERTY, Boolean.TRUE);
			popup.insert(separator, position);
		}
	}

	private static String abbreviate(String value) {
		return value.length() <= MAX_LABEL_LENGTH ? value : value.substring(0, MAX_LABEL_LENGTH - 1) + "…";
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
		// Removing the Action listener detaches the Action and clears these properties.
		String text = button.getText();
		javax.swing.Icon icon = button.getIcon();
		String tooltip = button.getToolTipText();
		String actionCommand = button.getActionCommand();
		int mnemonic = button.getMnemonic();
		for (ActionListener listener : button.getActionListeners()) {
			button.removeActionListener(listener);
		}
		button.setText(text);
		button.setIcon(icon);
		button.setToolTipText(tooltip);
		button.setActionCommand(actionCommand);
		button.setMnemonic(mnemonic);
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
		mirror.putValue(com.microproject.ui.menu.ExtToolBarFactory.POPUP_OPENER_ACTION_PROPERTY, Boolean.TRUE);
		mirror.setEnabled(command.isEnabled());
		command.addPropertyChangeListener(new WeakActionMirrorListener(mirror));
		button.setAction(mirror);
		button.setText(text);
		button.setIcon(icon);
		button.setToolTipText(tooltip);
		button.setActionCommand(actionCommand);
		button.setMnemonic(mnemonic);
	}

	/**
	 * Split buttons: a child over the arrow area opens the popup. The area is the
	 * lower half of a large button and the right edge strip of a small one; the child
	 * paints its own highlight so that the body and the arrow light up separately.
	 */
	private static void installArrowHitArea(AbstractButton button, String commandId, JPopupMenu popup) {
		JButton arrow = new JButton() {
			@Override
			protected void paintComponent(java.awt.Graphics graphics) {
				java.awt.Graphics2D g2 = (java.awt.Graphics2D) graphics.create();
				try {
					FlatUiSupport.paintRibbonSplitArrowArea(g2, button,
						new java.awt.Rectangle(0, 0, getWidth(), getHeight()),
						FlatUiSupport.ribbonSplitArrowFill(button, getModel()));
				} finally {
					g2.dispose();
				}
			}
		};
		arrow.setName(ARROW_BUTTON_NAME_PREFIX + commandId);
		arrow.setRolloverEnabled(true);
		arrow.setFocusable(true);
		arrow.setBorderPainted(false);
		arrow.setContentAreaFilled(false);
		arrow.setOpaque(false);
		arrow.setEnabled(button.isEnabled());
		arrow.getAccessibleContext().setAccessibleName(button.getText() == null ? commandId : button.getText());
		arrow.addActionListener(event -> {
			if (arrow.isShowing() && button.isEnabled() && prepareForShow(popup)) {
				popup.show(button, 0, button.getHeight());
			}
		});
		button.addPropertyChangeListener("enabled", event -> arrow.setEnabled(button.isEnabled()));
		button.putClientProperty(ARROW_BUTTON_PROPERTY, arrow);
		button.setLayout(null);
		button.add(arrow);
		Runnable resize = () -> arrow.setBounds(FlatUiSupport.ribbonSplitArrowBounds(button));
		// The size class is applied by the styler, possibly after this wiring.
		button.addPropertyChangeListener(RibbonButtonStyler.SIZE_PROPERTY, event -> resize.run());
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
