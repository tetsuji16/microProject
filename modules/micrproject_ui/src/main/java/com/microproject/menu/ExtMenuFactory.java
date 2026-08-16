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

import java.util.HashMap;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JMenuItem;

import com.microproject.menu.resource.MenuFactory;
import com.microproject.menu.resource.MissingListenerException;
import com.microproject.menu.resource.ResourceFormatException;

import com.microproject.pm.graphic.IconManager;

/**
 *
 */
public class ExtMenuFactory extends MenuFactory {
	private final Map<String, JMenuItem> menuItems = new HashMap<>();
	public final static String ICON_SUFFIX        = ".icon";
    public final static String ACTION_SUFFIX      = ".action";
    private final ProjectMenuActionMap actions;
	/**
	 * @param rb
	 * @param am
	 */
	ExtMenuFactory(ProjectMenuActionMap actionMap, ResourceBundle...rb) {
		super(new BatikActionMapAdapter(actionMap), rb);
		actions = actionMap;
	}
	
	JMenuItem getMenuItemFromId(String id) {
		if (id == null)
			return null;
		String actionText = getActionStringFromId(id);
		JMenuItem result = null;
		if (actionText != null) {
			result = menuItems.get(actionText);
		}
		if (result == null)
			result = menuItems.get(id);
			
		return result;
	}

    /**
     * Initializes a swing menu item
     * @param item the menu item to initialize
     * @param name the name of the menu item
     * @throws ResourceFormatException if the mnemonic is not a single
     *         character.
     * @throws MissingListenerException if then item action is not found in
     *         the action map.
     */
    protected void initializeJMenuItem(JMenuItem item, String name)
		throws ResourceFormatException,
	       MissingListenerException {
    	super.initializeJMenuItem(item,name);
    	String actionName = getActionStringFromId(name);
    	if (actionName != null)
    		menuItems.put(actionName,item);
		// Icon
		try {
		    String s = getString(name+ICON_SUFFIX);
		    ImageIcon icon = IconManager.getIcon(s);
		    if (icon != null)
		    	item.setIcon(icon);
		} catch (MissingResourceException e) {
		}
    }
    

    public Action getActionFromId(String id) {
		String actionId = getActionStringFromId(id);
		return actions.getAction(actionId == null ? id : actionId);
    }
    
    public String getStringFromAction(Action action) {
    	return actions.getStringFromAction(action);
    }

    public String getActionStringFromId(String id) {
    	return MenuLookupSupport.getActionStringFromId(this::getString, id, ACTION_SUFFIX);
    }
    public String getTextForId(String id) {
    	return getString(id+TEXT_SUFFIX);
    }
}

