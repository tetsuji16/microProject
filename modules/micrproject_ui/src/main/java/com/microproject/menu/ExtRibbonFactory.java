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

import com.microproject.menu.resource.MissingListenerException;
import com.microproject.menu.resource.ResourceFormatException;
import com.microproject.menu.resource.RibbonFactory;
import org.pushingpixels.flamingo.api.common.AbstractCommandButton;

/**
 *
 */
public class ExtRibbonFactory extends RibbonFactory {
	private final Map<String, List<AbstractCommandButton>> toolButtons = new LinkedHashMap<>();
	/**
	 * @param rb
	 * @param am
	 */
	public ExtRibbonFactory(ProjectMenuActionMap am,ResourceBundle...rb) {
		super(am,rb);
	}
	
	
	@Override
	public AbstractCommandButton createCommandButton(String name) throws MissingResourceException,
	ResourceFormatException, MissingListenerException {
		AbstractCommandButton button = super.createCommandButton(name);
		button.setName(name);
		registerButton(name, button);
		return button;
	}
	
	private void registerButton(String id, AbstractCommandButton button) {
		String actionName = getActionStringFromId(id);
		if (actionName != null) {
			toolButtons.computeIfAbsent(actionName, ignored -> new ArrayList<>()).add(button);
		}
	}

    public List<AbstractCommandButton> getButtonsFromId(String id) {
    	String buttonId = getActionStringFromId(id);
    	List<AbstractCommandButton> result = null;
    	if (buttonId != null)
    		result = toolButtons.get(buttonId);
    	if (result == null)
    		result = toolButtons.get(id);
    	return result;
    }
    public String getActionStringFromId(String id) {
    	return MenuLookupSupport.getActionStringFromId(this::getString, id, ExtMenuFactory.ACTION_SUFFIX);
   }

}

