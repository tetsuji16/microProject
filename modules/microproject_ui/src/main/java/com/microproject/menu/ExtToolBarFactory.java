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
package com.microproject.menu;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.MissingResourceException;
import java.util.Map;
import java.util.ResourceBundle;

import javax.swing.AbstractButton;
import javax.swing.ImageIcon;

import com.microproject.menu.resource.MissingListenerException;
import com.microproject.menu.resource.ResourceFormatException;
import com.microproject.menu.resource.ToolBarFactory;

import com.microproject.pm.graphic.IconManager;

/**
 *
 */
public class ExtToolBarFactory extends ToolBarFactory {
	private final Map<String, List<AbstractButton>> toolButtons = new LinkedHashMap<>();
	/**
	 * @param rb
	 * @param am
	 */
	public ExtToolBarFactory(ProjectMenuActionMap am,ResourceBundle...rb) {
		super(am,rb);
	}
	
	
	public synchronized AbstractButton createJButton(String name) throws MissingResourceException,
	ResourceFormatException, MissingListenerException {
		AbstractButton button = super.createJButton(name);
		configureButton(button, name);
    	String actionName = getActionStringFromId(name);
		if (actionName != null) {
			List<AbstractButton> buttons = toolButtons.get(actionName);
			if (buttons == null) {
				buttons = new ArrayList<>();
				toolButtons.put(actionName, buttons);
			}
			buttons.add(button);
		}
		return button;
	}

	/**
	 * Creates a command button for transient ribbon popups without registering
	 * another instance in the toolbar lookup map.
	 */
	public synchronized AbstractButton createUnregisteredJButton(String name) throws MissingResourceException,
	ResourceFormatException, MissingListenerException {
		AbstractButton button = super.createJButton(name);
		String actionName = getActionStringFromId(name);
		List<AbstractButton> registered = actionName == null ? null : toolButtons.get(actionName);
		if (registered != null && !registered.isEmpty() && registered.get(0).getAction() != null) {
			button.setAction(registered.get(0).getAction());
		}
		configureButton(button, name);
		return button;
	}

	private void configureButton(AbstractButton button, String name) {
		try {
			String iconName = getString(name + ExtMenuFactory.ICON_SUFFIX);
			ImageIcon icon = IconManager.getIcon(iconName);
			if (icon != null)
				button.setIcon(icon);
		} catch (MissingResourceException ignored) {
			// Ribbon styling resolves its explicit SVG icon separately.
		}
		button.setActionCommand(name);
	}
	

	public synchronized List<AbstractButton> getButtonsFromId(String id) {
    	String buttonId = getActionStringFromId(id);
    	List<AbstractButton> result = null;
    	if (buttonId != null)
    		result = toolButtons.get(buttonId);
	if (result == null)
		result = toolButtons.get(id);
		// Return a snapshot so responsive ribbon rebuilds cannot invalidate a
		// caller's iterator while the registry is being updated.
		return result == null ? null : new ArrayList<>(result);
    }

	public synchronized void unregisterButton(AbstractButton button) {
		if (button == null)
			return;
		unregisterButtons(Collections.singleton(button));
	}

	/**
	 * Removes a batch of transient ribbon buttons in one pass.
	 *
	 * Ribbon density changes can discard an entire band while the same action is
	 * represented by several button instances.  Updating the registration map
	 * once per component made that lifecycle unnecessarily re-entrant and could
	 * invalidate an iterator while the old entries were being pruned.
	 */
	public synchronized void unregisterButtons(Collection<? extends AbstractButton> buttons) {
		if (buttons == null || buttons.isEmpty())
			return;
		var removed = Collections.newSetFromMap(new IdentityHashMap<AbstractButton, Boolean>());
		removed.addAll(buttons);
		var iterator = toolButtons.entrySet().iterator();
		while (iterator.hasNext()) {
			var entry = iterator.next();
			List<AbstractButton> remaining = entry.getValue().stream()
				.filter(candidate -> !removed.contains(candidate))
				.collect(java.util.stream.Collectors.toCollection(ArrayList::new));
			if (remaining.isEmpty()) {
				iterator.remove();
			} else {
				entry.setValue(remaining);
			}
		}
	}
    public String getActionStringFromId(String id) {
    	return MenuLookupSupport.getActionStringFromId(this::getString, id, ExtMenuFactory.ACTION_SUFFIX);
   }

}
