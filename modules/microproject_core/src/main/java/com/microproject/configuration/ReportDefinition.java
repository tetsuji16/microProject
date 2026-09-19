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

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.digester.Digester;

import com.microproject.graphic.configuration.SpreadSheetFieldArray;
import com.microproject.strings.Messages;

/**
 * Holds the definition of a report read in from the view config
 */
public class ReportDefinition implements NamedItem {
	
	private String name = null;
	private String id = null;
	private String file = null;
	private boolean timeBased = false;

	private int collectionType = 0;
	/**
	 * @return Returns the collectionType.
	 */
	public int getCollectionType() {
		return collectionType;
	}
	/**
	 * @param collectionType The collectionType to set.
	 */
	public void setCollectionType(int collectionType) {
		this.collectionType = collectionType;
	}
	private Object reportObject = null;
	private HashMap reportColumnDefinitions = new HashMap<>();
	/**
	 * @return Returns the columnsList.
	 */
	public ArrayList getColumnsList() {
		return columnsList;
	}
	private ArrayList columnsList = new ArrayList();

	
	/**
	 * @return Returns the timeBased.
	 */
	public boolean isTimeBased() {
		return timeBased;
	}
	/**
	 * @param timeBased The timeBased to set.
	 */
	public void setTimeBased(boolean timeBased) {
		this.timeBased = timeBased;
	}
	public static final String CATEGORY="Report";
	public String getName() {
		return name;
	}

	public String getCategory() {
		return CATEGORY;
	}

	public final void setName(String name) {
		this.name = name;
	}
	public final String getId() {
		return id;
	}
	public final void setId(String id) {
		this.id = id;
		if (name == null)
			name = Messages.getString(id);
	}
    public static void addDigesterEvents(Digester digester){
		digester.addObjectCreate("*/reports/report", "com.microproject.configuration.ReportDefinition");
		digester.addObjectCreate("*/reports/report/columns", "com.microproject.configuration.ReportColumns");
	    digester.addSetProperties("*/reports/report");
	    digester.addSetProperties("*/reports/report/columns");
		digester.addSetNext("*/reports/report", "add", "com.microproject.configuration.NamedItem");
		digester.addSetNext("*/reports/report/columns", "add", "com.microproject.configuration.ReportColumns");
	    
	}
	
	public void add(ReportColumns columns) {
		columnsList.add(columns);
	}
	
	public String getMainSpreadsheetCategory() {
		if (columnsList.size() == 0)
			return null;
		return ((ReportColumns)columnsList.get(columnsList.size()-1)).getCategorySpreadSheet();
	}
	
	public SpreadSheetFieldArray getMainFieldArray() {
		if (columnsList.size() == 0)
			return null;
		return ((ReportColumns)columnsList.get(columnsList.size()-1)).getFieldArray();
		
	}
	public final Object getReportObject(ArrayList columns) {
		if (columns == null)
			return reportObject;
		return reportColumnDefinitions.get(columns);
	}
	public final void setReportObject(Object reportObject, ArrayList columns) {
		if (columns == null)
			this.reportObject = reportObject;
		else
			reportColumnDefinitions.put(columns,reportObject);
	}
	
	public String toString() {
		return name;
	}
	/**
	 * @return Returns the file.
	 */
	public String getFile() {
		return file;
	}
	/**
	 * @param file The file to set.
	 */
	public void setFile(String file) {
		this.file = file;
	}
}
