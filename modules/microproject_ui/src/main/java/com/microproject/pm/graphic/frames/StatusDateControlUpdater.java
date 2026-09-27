/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.pm.graphic.frames;

import java.awt.Component;
import java.awt.Container;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

import javax.swing.AbstractButton;
import javax.swing.Action;

import com.microproject.menu.MenuActionConstants;
import com.microproject.menu.MenuManager;
import com.microproject.options.EditOption;
import com.microproject.pm.task.Project;

/** Updates registered, visible, and transient Status Date controls from the project state. */
final class StatusDateControlUpdater {
	private static final String RIBBON_BUTTON_ID = "RibbonStatusDate";

	private StatusDateControlUpdater() {
	}

	static void refresh(MenuManager menuManager, Component frameRoot, Project project) {
		String label = menuManager.getStringOrNull(RIBBON_BUTTON_ID + ".text");
		if (label == null)
			label = "Status Date";
		String value = project == null || !project.isStatusDateSet() ? "NA"
				: EditOption.getInstance().getDateFormat().format(new java.util.Date(project.getStatusDate()));
		// Keep the current value before the localized Status Date caption.
		String display = value + " " + label + ":";
		Action action = menuManager.getActionFromId(MenuActionConstants.ACTION_STATUS_DATE);
		if (action != null)
			action.putValue(Action.NAME, display);
		var actionButtons = menuManager.getToolButtonsFromId(MenuActionConstants.ACTION_STATUS_DATE);
		var ribbonButtons = menuManager.getToolButtonsFromId(RIBBON_BUTTON_ID);
		Container root = frameRoot instanceof Container container ? container : null;
		for (AbstractButton button : collectStatusDateButtons(root, actionButtons, ribbonButtons))
			refreshButton(button, display);
	}

	static Set<AbstractButton> collectStatusDateButtons(Container root, Collection<?>... registeredGroups) {
		Set<AbstractButton> buttons = Collections.newSetFromMap(new IdentityHashMap<>());
		for (Collection<?> group : registeredGroups) {
			if (group == null)
				continue;
			for (Object candidate : group) {
				if (candidate instanceof AbstractButton button)
					buttons.add(button);
			}
		}
		if (root != null)
			collectVisibleComponents(root, buttons);
		return buttons;
	}

	private static void collectVisibleComponents(Container parent, Set<AbstractButton> buttons) {
		for (Component child : parent.getComponents()) {
			if (child instanceof AbstractButton button && RIBBON_BUTTON_ID.equals(button.getActionCommand()))
				buttons.add(button);
			if (child instanceof Container nested)
				collectVisibleComponents(nested, buttons);
		}
	}

	private static void refreshButton(AbstractButton button, String display) {
		if (!display.equals(button.getText()))
			button.setText(display);
		button.getAccessibleContext().setAccessibleName(display);
		button.revalidate();
		button.repaint();
	}
}
