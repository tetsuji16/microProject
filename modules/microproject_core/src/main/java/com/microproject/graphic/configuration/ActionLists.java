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
package com.microproject.graphic.configuration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.digester.Digester;

import com.microproject.configuration.Dictionary;
import com.microproject.configuration.NamedItem;
import com.microproject.field.InvalidFormulaException;
import com.microproject.strings.Messages;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 *
 */
public class ActionLists implements NamedItem {
	private static final Logger logger = Logger.getLogger(ActionLists.class.getName());
	public static final String category="ActionListsCategory";
	public String getCategory() {
		return category;
	}
	
	String name = null;
	String id = null;
	Map<String, ActionList> actionListMap = new HashMap<String, ActionList>();
	Map<String, ActionListFactory> factoryMap = new HashMap<String, ActionListFactory>();
	List<ActionListFactory> factories = new ArrayList<ActionListFactory>();

	public ActionLists() {}
	
	
	public void addActionList(ActionListFactory factory) {
		factories.add(factory);
		factoryMap.put(factory.getId(),factory);
	}
	/**
	 * @return Returns the name.
	 */
	public String getName() {
		return name;
	}
	/**
	 * @param name The name to set.
	 */
	public void setName(String name) {
		this.name = name;
	}
	
	public void setId(String id) {
		this.id = id;
		setName(Messages.getString(id));
	}	
	/**
	 * @return Returns the id.
	 */
	public String getId() {
		return id;
	}

	public ActionList getActionList(String id) {
		ActionList actionList=actionListMap.get(id);
	    if (actionList==null){
            ActionListFactory factory=getFactory(id);
            try {
                    actionList=factory.getActionList();
                    actionListMap.put(id,actionList);
                } catch (InvalidFormulaException e) {
        			logger.severe("Formula not set: invalid formula text: " +factory.getFormulaText());
                }
	    }
	    return actionList;
	}
	public ActionList getDefaultActionList(){
	    return getActionList("ActionList.default");
	}
	
	public ActionListFactory getFactory(String id) {
		return factoryMap.get(id);
	}
	public List<ActionListFactory> getFactories() {
		return factories;
	}
	
	public static void addDigesterEvents(Digester digester){
		// main properties of bar
		digester.addObjectCreate("*/actionLists", "com.microproject.graphic.configuration.ActionLists");
	    digester.addSetProperties("*/actionLists");
		digester.addSetNext("*/actionLists", "add", "com.microproject.configuration.NamedItem");

		// start section
		digester.addObjectCreate("*/actionLists/actionList", "com.microproject.graphic.configuration.ActionListFactory");
	    digester.addSetProperties("*/actionLists/actionList");
	    digester.addCallMethod("*/actionLists/actionList/formulaText","setFormulaText",0);
	    digester.addSetNext("*/actionLists/actionList", "addActionList", "com.microproject.graphic.configuration.ActionListFactory");
	    
	}
	
	
	
	
	
	
	protected static ActionLists instance=null;
	public static ActionLists getInstance(){
	    if (instance==null) instance=(ActionLists)Dictionary.get(category,"default");
	    return instance;
	}
}
