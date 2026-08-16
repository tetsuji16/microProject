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
import com.microproject.util.FlatUiSupport;

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
	
	
	public AbstractButton createJButton(String name) throws MissingResourceException,
	ResourceFormatException, MissingListenerException {
		AbstractButton button = super.createJButton(name);
		configureButton(button, name);
    	String actionName = getActionStringFromId(name);
		if (actionName != null)
			toolButtons.computeIfAbsent(actionName, ignored -> new ArrayList<>()).add(button);
		return button;
	}

	/**
	 * Creates a command button for transient ribbon popups without registering
	 * another instance in the toolbar lookup map.
	 */
	public AbstractButton createUnregisteredJButton(String name) throws MissingResourceException,
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
		FlatUiSupport.styleToolBarButton(button);
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
	

	public List<AbstractButton> getButtonsFromId(String id) {
    	String buttonId = getActionStringFromId(id);
    	List<AbstractButton> result = null;
    	if (buttonId != null)
    		result = toolButtons.get(buttonId);
    	if (result == null)
    		result = toolButtons.get(id);
    	return result;
    }

	public synchronized void unregisterButton(AbstractButton button) {
		if (button == null)
			return;
		var iterator = toolButtons.entrySet().iterator();
		while (iterator.hasNext()) {
			var entry = iterator.next();
			List<AbstractButton> remaining = entry.getValue().stream()
				.filter(candidate -> candidate != button)
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

