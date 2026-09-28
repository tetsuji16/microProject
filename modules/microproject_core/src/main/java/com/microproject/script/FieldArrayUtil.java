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
package com.microproject.script;

import java.util.Iterator;

import com.microproject.configuration.Dictionary;
import com.microproject.field.Field;
import com.microproject.graphic.configuration.SpreadSheetFieldArray;
import com.microproject.strings.Messages;

public class FieldArrayUtil {
	public static final String HIDDEN_SUFFIX="_Hidden";
	public static final String SERVER_SUFFIX="_Server";

	public static final String taskFieldArrayCategory="taskSpreadsheet";
	public static final String resourceFieldArrayCategory="resourceSpreadsheet";
	public static final String portfolioFieldArrayCategory="portfolioSpreadsheet";
	public static final String projectFieldArrayCategory="projectSpreadsheet";
	public static final String projectFieldArrayCategoryHidden="projectSpreadsheet"+HIDDEN_SUFFIX;
	public static final String timesheetFieldArrayCategory="timesheetSpreadsheet";


	public static SpreadSheetFieldArray removeNonWebFields(SpreadSheetFieldArray in) {
		SpreadSheetFieldArray out= (SpreadSheetFieldArray)in.clone();
		Iterator<Field> i = out.iterator();
		while (i.hasNext()) {
			Field f = i.next();
			if (f.getId().equals("Field.indicators") || /*f.isGraphical() ||*/
					(projectFieldArrayCategory.equals(out.getCategory())&&"Field.id".equals(f.getId())))
				i.remove();

		}
		return out;
	}

	public static SpreadSheetFieldArray getFieldArray(int type, String id){
		if (id==null) return null;
		String category=typetoCategory(type);
		Object o=getFromId(category, id);
		if (o==null) {
			o=getFromId(category+SERVER_SUFFIX,id);
		}
		if (o == null)
			return null;
		else return removeNonWebFields((SpreadSheetFieldArray)o);
	}
	public static SpreadSheetFieldArray getHiddenFieldArray(int type, String id){
		if (id==null) return null;
		String category=typetoHiddenCategory(type);
		Object o=getFromId(category, id);
		if (o==null) return null;
		else return removeNonWebFields((SpreadSheetFieldArray)o);
	}

	public static String typetoCategory(int type){
		if (type==ScriptRunner.TASK) return taskFieldArrayCategory;
		else if (type==ScriptRunner.RESOURCE) return resourceFieldArrayCategory;
		else if (type==ScriptRunner.PROJECT) return projectFieldArrayCategory;
		else if (type==ScriptRunner.ASSIGNMENT) return timesheetFieldArrayCategory;

		else return null;
	}
	public static String typetoHiddenCategory(int type){
		if (type==ScriptRunner.PROJECT) return projectFieldArrayCategoryHidden;

		else return null;
	}
//	public static int categoryToType(String ca){
//		if (type==ScriptRunner.TASK) return taskFieldArrayCategory;
//		else if (type==ScriptRunner.RESOURCE) return resourceFieldArrayCategory;
//		else if (type==ScriptRunner.PROJECT) return projectFieldArrayCategory;
//		else if (type==ScriptRunner.PORTFOLIO) return portfolioFieldArrayCategory;
//		else if (type==ScriptRunner.ASSIGNMENT) return timesheetFieldArrayCategory;
//
//		else return null;
//	}
//	public static String typetoCategory(int type,boolean hidden){
//		if (hidden) return typetoCategory(type)+HIDDEN_SUFFIX;
//		else return typetoCategory(type);
//	}



	//more general than SpreadSheetFieldArray.getFromId, useful?
	private static final Object getFromId(String category, String id) {
		Object result = Dictionary.get(category, Messages.getString(id));
		if (result == null)
			result = Dictionary.get(category, id);
		return result;
	}


}
