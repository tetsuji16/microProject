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

import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.digester.Digester;

import com.microproject.configuration.Configuration;
import com.microproject.configuration.FieldDictionary;
import com.microproject.configuration.NamedItem;
import com.microproject.field.Field;
import com.microproject.functor.ScheduleIntervalGenerator;
import com.microproject.strings.Messages;

public class BarFormat implements NamedItem {
	private static final Logger log = Logger.getLogger(BarFormat.class.getName());
	public static final String category="BarFormatCategory";

	public BarFormat() {}
	public String getCategory() {
		return category;
	}
	
	/**
	 * @return Returns the end.
	 */
	public TexturedShape getEnd() {
		return end;
	}
	/**
	 * @param end The end to set.
	 */
	public void setEnd(TexturedShape end) {
		end.build();
		this.end = end;
	}
	/**
	 * @return Returns the from.
	 */
	public String getFrom() {
		return from;
	}
	/**
	 * @param from The from to set.
	 */
	public void setFrom(String from) {
		this.from = from;
		fromField = Configuration.getFieldFromId(from);
	}
	
	/**
	 * @return Returns the to.
	 */
	public String getTo() {
		return to;
	}
	/**
	 * @param to The to to set.
	 */
	public void setTo(String to) {
		this.to = to; 
		toField = Configuration.getFieldFromId(to);
	}
	/**
	 * @return Returns the line.
	 */
	public int getRow() {
		return row;
	}
	/**
	 * @param line The line to set.
	 */
	public void setRow(int line) {
		this.row = line;
	}
	/**
	 * @return Returns the middle.
	 */
	public TexturedShape getMiddle() {
		return middle;
	}
	/**
	 * @param middle The middle to set.
	 */
	public void setMiddle(TexturedShape middle) {
		middle.build();
		this.middle = middle;
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

	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
		setName(Messages.getString(id));
	}
	/**
	 * @return Returns the start.
	 */
	public TexturedShape getStart() {
		return start;
	}
	/**
	 * @param start The start to set.
	 */
	public void setStart(TexturedShape start) {
		start.build();
		this.start = start;
	}


	
	
	
	
	public int getLayer() {
		return layer;
	}
	public void setLayer(int layer) {
		this.layer = layer;
	}
	
	public String getIntervalGenerator() {
		return intervalGenerator;
	}
	public void setIntervalGenerator(String intervalGenerator) {
		this.intervalGenerator = intervalGenerator;
	}
	
	public ScheduleIntervalGenerator getScheduleIntervalGenerator() {
		if (intervalGenerator!=null&&scheduleIntervalGenerator==null){
			try {
				scheduleIntervalGenerator=(ScheduleIntervalGenerator)Class.forName(intervalGenerator).getDeclaredConstructor().newInstance();
			} catch (InstantiationException e) {
				log.log(Level.WARNING, "Failed to create schedule interval generator", e);
			} catch (IllegalAccessException e) {
				log.log(Level.WARNING, "Failed to create schedule interval generator", e);
			} catch (ClassNotFoundException e) {
				log.log(Level.WARNING, "Failed to create schedule interval generator", e);
			} catch (NoSuchMethodException e) {
				log.log(Level.WARNING, "Failed to create schedule interval generator", e);
			} catch (java.lang.reflect.InvocationTargetException e) {
				log.log(Level.WARNING, "Failed to create schedule interval generator", e);
			}
		}
		return scheduleIntervalGenerator;
	}
	public boolean isMain(){ //compatibily
		return main||intervalGenerator!=null;
	}
	public void setMain(boolean main) {
		this.main = main;
	}
	
	public FormFormat getForm() {
		return form;
	}
	public void setForm(FormFormat form) {
		this.form = form;
	}
/**
 * Add digester events for the bar as well as the three sections.
 * The XML root is * /bar/shape
 * 	
 */
	public static void addDigesterEvents(Digester digester){
		// main properties of bar
		digester.addObjectCreate("*/bar/format", "com.microproject.graphic.configuration.BarFormat");
	    digester.addSetProperties("*/bar/format");
		digester.addSetNext("*/bar/format", "add", "com.microproject.configuration.NamedItem"); // add to dictionary
		
		// start section
		digester.addObjectCreate("*/bar/format/start", "com.microproject.graphic.configuration.TexturedShape");
	    digester.addSetProperties("*/bar/format/start");
	    digester.addSetNext("*/bar/format/start", "setStart", "com.microproject.graphic.configuration.TexturedShape");
	    
	    //middle section
		digester.addObjectCreate("*/bar/format/middle", "com.microproject.graphic.configuration.TexturedShape");
	    digester.addSetProperties("*/bar/format/middle");
	    digester.addSetNext("*/bar/format/middle", "setMiddle", "com.microproject.graphic.configuration.TexturedShape");
	    
	    //end section
		digester.addObjectCreate("*/bar/format/end", "com.microproject.graphic.configuration.TexturedShape");
	    digester.addSetProperties("*/bar/format/end");
	    digester.addSetNext("*/bar/format/end", "setEnd", "com.microproject.graphic.configuration.TexturedShape");
	    
	    //end section
		digester.addObjectCreate("*/bar/format/form", "com.microproject.graphic.configuration.FormFormat");
	    digester.addSetProperties("*/bar/format/form");
	    digester.addSetNext("*/bar/format/form", "setForm", "com.microproject.graphic.configuration.FormFormat");
		
	    FormFormat.addDigesterEvents(digester);
	    
	}	
	
	String name = null;
	String id = null;
	int row = 0;
	String intervalGenerator=null;
	ScheduleIntervalGenerator scheduleIntervalGenerator=null;
	String from;
	String to;
	Field fromField = null;
	Field toField = null;
	TexturedShape start = null;
	TexturedShape middle = null;
	TexturedShape end = null;
	FormFormat form=null;
	boolean main=false;
	public static final int MIN_FOREGROUND_LAYER=1;
	public static final int MAX_FOREGROUND_LAYER=499;
	public static final int MIN_LINK_LAYER=500;
	public static final int MAX_LINK_LAYER=999;
	public static final int MIN_BACKGROUND_LAYER=1000;
	public static final int MAX_BACKGROUND_LAYER=1499;
	int layer=MIN_BACKGROUND_LAYER;
	String fieldId=null;
	Field field=null; //for annotations
	
	/**
	 * @return Returns the category.
	 */
	/**
	 * @return Returns the fromField.
	 */
	public Field getFromField() {
		return fromField;
	}
	/**
	 * @return Returns the toField.
	 */
	public Field getToField() {
		return toField;
	}
	
	public int getNumberOfSections() {
		int count = 0;
		if (start != null)
			count++;
		if (middle != null)
			count++;
		if (end != null)
			count++;
		return count;
	}
	
	public String getFieldId() {
		return fieldId;
	}
	public void setFieldId(String fieldId) {
		this.fieldId = fieldId;
		getField();
	}
	public Field getField(){
		if (field==null||field.getId()!=fieldId){
			if (fieldId==null) field=null;
			field=FieldDictionary.getInstance().getFieldFromId(fieldId);
		}
		return field;
	}

}
