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
package com.microproject.configuration;

import java.util.Collection;

import org.apache.commons.digester.Digester;

import com.microproject.field.Field;
import com.microproject.graphic.configuration.GraphicConfiguration;
import com.microproject.strings.Messages;
import com.microproject.timescale.TimeScaleManager;
import com.microproject.util.ClassUtils;

/**
 * Main access to objects described in configuration files
 */
public class Configuration implements ProvidesDigesterEvents {
	FieldDictionary fieldDictionary = null;
	TimeScaleManager timeScales = null;
	GraphicConfiguration graphicConfiguation = null;
	ScriptConfiguration scriptConfiguration = null;
	
	private static Configuration instance = null;
	// Tracks the instance currently being built so that re-entrant calls
	// (e.g. a Field subclass static initializer invoking Configuration.getFieldFromId
	// while Digester is still parsing configuration.xml) return the in-progress
	// instance instead of starting a second parse that recurses into StackOverflowError.
	// ThreadLocal (not a plain static) so the fast-path is visible only to the
	// re-entrant *same* thread; other threads block on the monitor and wait for the
	// fully built `instance`, never observing a half-initialized object.
	private static final ThreadLocal<Configuration> buildingInstance = new ThreadLocal<>();
	public static synchronized Configuration getInstance() {
		if (instance != null)
			return instance;
		Configuration inProgress = buildingInstance.get();
		if (inProgress != null)
			return inProgress;
		Configuration temp = new Configuration();
		buildingInstance.set(temp); // publish before parsing so re-entrant callers see it
		temp.fieldDictionary = new FieldDictionary(); // initialize early so re-entrant callers (e.g. classes loaded during read()) never see a null dictionary
		String [] files = Messages.getMetaString("ConfigurationFiles").split(";");
		for (String file : files) 
			ConfigurationReader.read(file, temp) ;
		temp.setDonePopulating(); // makes its hash table fast if using a FastHashMap
		instance = temp; // publish only after fully built to avoid re-entrant use of a half-initialized instance
		buildingInstance.remove();
		return instance;
	}
	public Configuration() {
		if (fieldDictionary == null)
			fieldDictionary = new FieldDictionary();
	}
	
	public void setDonePopulating() {
		if (fieldDictionary != null)
			fieldDictionary.setDonePopulating(); // makes its hash table fast if using a FastHashMap
		
	}
	/**
	 * @return Returns the fieldDictionary.
	 */
	public FieldDictionary getFieldDictionary() {
		return fieldDictionary;
	}
	/**
	 * @param fieldDictionary The fieldDictionary to set.
	 */
	public void setFieldDictionary(FieldDictionary fieldDictionary) {
		this.fieldDictionary = fieldDictionary;
	}
	
	public static Field getFieldFromId(String id) {
		return getInstance().getFieldDictionary().getFieldFromId(id);
	}
	public static final Field getFieldFromShortId(String id) {
		return getFieldFromId("Field."+id);
	}
	
	public static Collection<Field> getAllFields() {
		return getInstance().getFieldDictionary().getAllFields();
	}
	/**
	 * @return Returns the timeScales.
	 */
	public TimeScaleManager getTimeScales() {
		return timeScales;
	}
	/**
	 * @param timeScales The timeScales to set.
	 */
	public void setTimeScales(TimeScaleManager timeScales) {
		this.timeScales = timeScales;
	}
	
	public void setIntConstant(String name, int value) {
		ClassUtils.setStaticField(name,value);
	}
	
	public void setStringConstant(String name, String value) {
		ClassUtils.setStaticField(name,value);
	}
	/**
	 * @return Returns the graphicConfiguation.
	 */
	public GraphicConfiguration getGraphicConfiguation() {
		return graphicConfiguation;
	}
	/**
	 * @param graphicConfiguation The graphicConfiguation to set.
	 */
	public void setGraphicConfiguation(GraphicConfiguration graphicConfiguation) {
		this.graphicConfiguation = graphicConfiguation;
	}
	
	public ScriptConfiguration getScriptConfiguration() {
		return scriptConfiguration;
	}
	public void setScriptConfiguration(ScriptConfiguration scriptConfiguration) {
		this.scriptConfiguration = scriptConfiguration;
	}
	
	private void addGlobalDigesterEvents(Digester dg){
		dg.addCallMethod("configuration/constants/int","setIntConstant", 2, new Class[] {String.class, Integer.class});
		dg.addCallParam("configuration/constants/int/name",0);
		dg.addCallParam("configuration/constants/int/value",1);

		dg.addCallMethod("configuration/constants/String","setStringConstant", 2, new Class[] {String.class, String.class});
		dg.addCallParam("configuration/constants/String/name",0);
		dg.addCallParam("configuration/constants/String/value",1);
	}
	
	public void addDigesterEvents(Digester dg) {
		addGlobalDigesterEvents(dg);
		FieldDictionary.addDigesterEvents(dg);
		//set time scale's zoom levels
		TimeScaleManager.addDigesterEvents(dg);

		ScriptConfiguration.addDigesterEvents(dg);

		//graphic config
		GraphicConfiguration.addDigesterEvents(dg);
	}

}
