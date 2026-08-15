/*******************************************************************************
 * The contents of this file are subject to the Common Public Attribution License 
 * Version 1.0 (the "License"); you may not use this file except in compliance with 
 * the License. You may obtain a copy of the License at 
 * http://www.projectlibre.com/license . The License is based on the Mozilla Public 
 * License Version 1.1 but Sections 14 and 15 have been added to cover use of 
 * software over a computer network and provide for limited attribution for the 
 * Original Developer. In addition, Exhibit A has been modified to be consistent 
 * with Exhibit B. 
 *
 * Software distributed under the License is distributed on an "AS IS" basis, 
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for the 
 * specific language governing rights and limitations under the License. The 
 * Original Code is ProjectLibre. The Original Developer is the Initial Developer 
 * and is ProjectLibre Inc. All portions of the code written by ProjectLibre are 
 * Copyright (c) 2012-2019. All Rights Reserved. All portions of the code written by 
 * ProjectLibre are Copyright (c) 2012-2019. All Rights Reserved. Contributor 
 * ProjectLibre, Inc.
 *
 * Alternatively, the contents of this file may be used under the terms of the 
 * ProjectLibre End-User License Agreement (the ProjectLibre License) in which case 
 * the provisions of the ProjectLibre License are applicable instead of those above. 
 * If you wish to allow use of your version of this file only under the terms of the 
 * ProjectLibre License and not to allow others to use your version of this file 
 * under the CPAL, indicate your decision by deleting the provisions above and 
 * replace them with the notice and other provisions required by the ProjectLibre 
 * License. If you do not delete the provisions above, a recipient may use your 
 * version of this file under either the CPAL or the ProjectLibre Licenses. 
 *
 *
 * [NOTE: The text of this Exhibit A may differ slightly from the text of the notices 
 * in the Source Code files of the Original Code. You should use the text of this 
 * Exhibit A rather than the text found in the Original Code Source Code for Your 
 * Modifications.] 
 *
 * EXHIBIT B. Attribution Information for ProjectLibre required
 *
 * Attribution Copyright Notice: Copyright (c) 2012-2019, ProjectLibre, Inc.
 * Attribution Phrase (not exceeding 10 words): 
 * ProjectLibre, open source project management software.
 * Attribution URL: http://www.projectlibre.com
 * Graphic Image as provided in the Covered Code as file: projectlibre-logo.png with 
 * alternatives listed on http://www.projectlibre.com/logo 
 *
 * Display of Attribution Information is required in Larger Works which are defined 
 * in the CPAL as a work which combines Covered Code or portions thereof with code 
 * not governed by the terms of the CPAL. However, in addition to the other notice 
 * obligations, all copies of the Covered Code in Executable and Source Code form 
 * distributed must, as a form of attribution of the original author, include on 
 * each user interface screen the "ProjectLibre" logo visible to all users. 
 * The ProjectLibre logo should be located horizontally aligned with the menu bar 
 * The 
 * logo must be at least 144 x 31 pixels. When users click on the "ProjectLibre" 
 * logo it must direct them back to http://www.projectlibre.com. 
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

