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

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;

import org.apache.commons.digester.Digester;

import com.microproject.graphic.configuration.ActionLists;
import com.microproject.graphic.configuration.BarFormat;
import com.microproject.graphic.configuration.BarStyle;
import com.microproject.graphic.configuration.BarStyles;
import com.microproject.graphic.configuration.CellStyles;
import com.microproject.graphic.configuration.SpreadSheetFieldArray;
import com.microproject.grouping.core.transform.TransformList;
import com.microproject.grouping.core.transform.ViewConfiguration;
import com.microproject.pm.calendar.WorkCalendar;
import com.microproject.script.ContextStore;
import com.microproject.scripting.FormulaFactory;
import com.microproject.strings.Messages;
import com.microproject.util.ClassUtils;
import com.microproject.util.Environment;

/**
 * A hash table of hashtables which allows finding an object by category and
 * name. Used with parsed xml view config
 */
public class Dictionary implements ProvidesDigesterEvents {
	private static Dictionary instance = null;
	public static synchronized Dictionary getInstance() {
		if (instance == null) {
			instance = new Dictionary();
			String [] files = Messages.getMetaString("DictionaryFiles").split(";");
			for (String file : files)
				ConfigurationReader.read(file, instance) ;
			if (Environment.isClientSide()) // this screws up on server and is not needed anyway
 				precompileCommonFormulas();


		}
		return instance;
	}

	public Dictionary() {
	}

	private final HashMap<String, HashMap<String, NamedItem>> mainMap = new HashMap<>();

	public static void add(NamedItem namedItem) {
		add(namedItem,false);
	}
	public static void add(NamedItem namedItem, boolean replace) {
		String categories[] = namedItem.getCategory().split(";"); // can belong to more than one if separated by ;

		for (int i = 0; i < categories.length; i++) {
			String category = categories[i];
			HashMap<String, NamedItem> subMap = getInstance().mainMap.get(category);
			if (subMap == null) {
				subMap = new HashMap<>();
				getInstance().mainMap.put(category, subMap);
			}
			if (!subMap.containsValue(namedItem)) {
				subMap.put(namedItem.getName(), namedItem);
			} else {
				if (replace)
					subMap.put(namedItem.getName(), namedItem);

//this is actually normal if overriding with another xml file				ConfigurationReader.log.warn("named item " + namedItem + " already in category " + category);
			}
		}
	}

	public static void remove(NamedItem namedItem) {
		String categories[] = namedItem.getCategory().split(";"); // can belong to more than one if separated by ;

		for (int i = 0; i < categories.length; i++) {
			String category = categories[i];
			HashMap<String, NamedItem> subMap = getInstance().mainMap.get(category);
			subMap.remove(namedItem.getName());
		}

	}
	public static NamedItem get(Object category, String name) {
		HashMap<String, NamedItem> subMap = getInstance().mainMap.get(category);
		if (subMap == null)
			return null;
		return (NamedItem) subMap.get(name);
	}

	public static Object[] getAll(Object category) {
		HashMap<String, NamedItem> subMap = getInstance().mainMap.get(category);
		NamedItem[] array = subMap.values().toArray(new NamedItem[0]);
		Arrays.sort(array,namedItemComparator);
		return array;

	}

	public static Object[] allCalendars() {
		return getAll(WorkCalendar.CALENDAR_CATEGORY);
	}

	public static WorkCalendar findCalendar(String name) {
		if (name == null)
			return null;
		return (WorkCalendar) get(WorkCalendar.CALENDAR_CATEGORY,name);
	}

	public void addDigesterEvents(Digester digester) {
		SpreadSheetFieldArray.addDigesterEvents(digester);
		BarFormat.addDigesterEvents(digester);
		BarStyles.addDigesterEvents(digester);
		TransformList.addDigesterEvents(digester);
		CellStyles.addDigesterEvents(digester);
		ActionLists.addDigesterEvents(digester);
		ViewConfiguration.addDigesterEvents(digester);
		ReportDefinition.addDigesterEvents(digester);
		ContextStore.addDigesterEvents(digester);
		ChartDefinition.addDigesterEvents(digester);
	}

	private static final Comparator<NamedItem> namedItemComparator = new NamedItemComparator();
	private static class NamedItemComparator implements Comparator<NamedItem> {
		@Override
		public int compare(NamedItem first, NamedItem second) {
			return first.getName().compareTo(second.getName());
		}
	}
	private static void precompileCommonFormulas() {
		FormulaFactory.precompileClass(BarStyle.FORMULA_PREFIX+Messages.getString("Styles.Bar.standard"));
	}

	public static String generateUniqueName(NamedItem namedItem) {
		String name = namedItem.getName();
		while (Dictionary.get(namedItem.getCategory(),name) != null)
			name += "*";
		return name;
	}

	public static void rename(NamedItem namedItem, String newName) {
		remove(namedItem);
		ClassUtils.setSimpleProperty(namedItem,"name",newName); // call setName if any
		add(namedItem);
	}
	public static String getCategoryText(String category) {
		return Messages.getString("Category."+category);
	}
}
