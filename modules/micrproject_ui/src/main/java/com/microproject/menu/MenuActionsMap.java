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
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Action;

/**
 * The menu builder code requires an action map.  Here, I make a dummy ActionMap that defers to a dummy MenuAction.
 * This menu action is not used, though we can add things like logging for it later.
 */
public class MenuActionsMap {
	public static abstract class DocumentMenuAction extends GlobalMenuAction {
		protected boolean needsDocument() {
			return true;
		}
	}

	public static abstract class GlobalMenuAction extends AbstractAction {
		protected boolean needsDocument() {
			return false;
		}
		protected boolean allowed(boolean enable){
			return true;
		}
	}

	private final Map<String, Action> actionById;
	private final Map<Action, String> keyByAction;
	private final Set<GlobalMenuAction> documentActions;
	private final MenuManager menuManager;
	public MenuActionsMap(MenuManager menuManager) {
		this.menuManager = menuManager;
		actionById = new HashMap<>();
		keyByAction = new HashMap<>();
		documentActions = new LinkedHashSet<>();
	}
	
	public Action getConcreteAction(String key) {
		return actionById.get(key);
	}
	public String getStringFromAction(Action action) {
		return keyByAction.get(action);
	}

	public void addHandler(String menuId, AbstractAction action) {
		String actionKey = menuManager.getActionStringFromId(menuId);
		if (actionKey == null) {
			actionKey = menuId;
		}
		if (actionKey != null) {
			actionById.put(actionKey, action);
			actionById.put(menuId, action);
			keyByAction.put(action, actionKey);
		}
		if (action instanceof GlobalMenuAction globalAction && globalAction.needsDocument()) {
			documentActions.add(globalAction);
		}
    }

	
	public void setEnabledDocumentMenuActions(boolean enable) {
		for (GlobalMenuAction action : documentActions) {
			String actionText = keyByAction.get(action);
			if (actionText != null) {
				menuManager.setActionEnabled(actionText, enable && action.allowed(enable));
																//To disable save action for local project
			}
		}
	}
	
	
	
}

