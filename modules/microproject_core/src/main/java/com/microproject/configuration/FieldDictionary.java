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

import com.microproject.util.DataUtils;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapIterator;
import org.apache.commons.collections.Predicate;
import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.digester.Digester;

import com.microproject.field.Field;
import com.microproject.pm.assignment.Assignment;
import com.microproject.pm.dependency.Dependency;
import com.microproject.pm.resource.Resource;
import com.microproject.pm.resource.ResourceImpl;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.strings.Messages;
import com.microproject.util.ClassUtils;
import com.microproject.util.Environment;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Dictionary of all fields
 */
public class FieldDictionary {
	private static final Logger logger = Logger.getLogger(FieldDictionary.class.getName());
	private HashedMap map = new HashedMap();
	private HashMap<String, Field> actionsMap = new HashMap<>();
	public void addField(Field field) {
		if (field.isServer()&&Environment.getStandAlone()) return;
		field.setClass(clazz);
		if (field.build() || true) {
			if (field.isIndexed()) {
				for (int i=0; i < field.getIndexes(); i++) {
					Field indexField = field.createIndexedField(i);
					logger.fine("adding indexfield " + clazz.getName() + "." +  indexField.getName() + " id " + indexField.getId() + " field "+ indexField);
					map.put(indexField.getId(),indexField);					
					
				}
			} else {
				logger.fine("adding field " + clazz.getName() + "." +  field.getName() + " " + field);
				map.put(field.getId(),field);
				if (field.getAction() != null)
					actionsMap.put(field.getAction(),field);

			}
		} else {
			logger.warning("Field not added" + field.getId());
		}
	}
	
	public Field getActionField(String action) {
		return actionsMap.get(action);
	}
	
	public static FieldDictionary getInstance() {
		return Configuration.getInstance().getFieldDictionary();
	}
	
	public Field getFieldFromId(String id) {
		return (Field) map.get(id);
	}
	
	private Class clazz;
	public void setClassName(String className) {
		//System.out.println("						<include name=\""+className+"\"/>");
		try {
			clazz = ClassUtils.forName(className);
		} catch (ClassNotFoundException e) {
			logger.log(Level.SEVERE, "Unexpected error", e);
		}
	}
	
	public void populateListWithFieldsOfType(List<Field> list, Class clazz) {
		populateListWithFieldsOfType(list,new Class[] {clazz});
	}

	/** Fill a collection with all fields that are applicable to one or more types
	 * specified by clazz.  The collection is sorted alpha-numerically by field name.
	 * Lists by type should probably just be cached in static variables.
	 * @param collection - collection to fill
	 * @param clazz - array of class types
	 */
	public void populateListWithFieldsOfType(List<Field> list, Class[] clazz) {
		MapIterator i = map.mapIterator();
		while (i.hasNext()) {
			Object key = i.next();
			Field field = (Field) i.getValue();
			if (field.isApplicable(clazz))
				list.add(field);
		 }
		Collections.sort(list);
	}
	
	Collection<Field> getAllFields() {
  		return map.values();
	}

	private LinkedList<Field> taskFields = new LinkedList<>();
	private LinkedList<Field> resourceFields = new LinkedList<>();
	private LinkedList<Field> assignmentFields = new LinkedList<>();
	private LinkedList<Field> dependencyFields = new LinkedList<>();
	private LinkedList<Field> projectFields = new LinkedList<>();
	private LinkedList<Field> taskAndAssignmentFields = new LinkedList<>();	
	private LinkedList<Field> resourceAndAssignmentFields = new LinkedList<>();	
	
	void setDonePopulating() { 
// in case we use a FastHashMap do this:		map.setFast(true);
		taskFields = new LinkedList<>();
		resourceFields = new LinkedList<>();
		assignmentFields = new LinkedList<>();
		dependencyFields = new LinkedList<>();
		projectFields = new LinkedList<>();
		taskAndAssignmentFields = new LinkedList<>();	
		resourceAndAssignmentFields = new LinkedList<>();	

		populateListWithFieldsOfType(taskFields,NormalTask.class);
		populateListWithFieldsOfType(resourceFields,ResourceImpl.class);
		populateListWithFieldsOfType(assignmentFields,Assignment.class);
		populateListWithFieldsOfType(dependencyFields,Dependency.class);
		populateListWithFieldsOfType(projectFields,Project.class);
		populateListWithFieldsOfType(taskAndAssignmentFields,new Class[] {NormalTask.class,Assignment.class});		
		populateListWithFieldsOfType(resourceAndAssignmentFields,new Class[] {Resource.class,Assignment.class});		
	}

	/**
	 * @return Returns the assignmentFields.
	 */
	public LinkedList<Field> getAssignmentFields() {
		return assignmentFields;
	}
	/**
	 * @return Returns the dependencyFields.
	 */
	public LinkedList<Field> getDependencyFields() {
		return dependencyFields;
	}
	/**
	 * @return Returns the projectFields.
	 */
	public LinkedList<Field> getProjectFields() {
		return projectFields;
	}
	/**
	 * @return Returns the resourceFields.
	 */
	public LinkedList<Field> getResourceFields() {
		return resourceFields;
	}
	/**
	 * @return Returns the taskAndAssignmentFields.
	 */
	public LinkedList<Field> getTaskAndAssignmentFields() {
		return taskAndAssignmentFields;
	}
	/**
	 * @return Returns the taskFields.
	 */
	public LinkedList<Field> getTaskFields() {
		return taskFields;
	}
	/**
	 * @return Returns the resourceAndAssignmentFields.
	 */
	public LinkedList<Field> getResourceAndAssignmentFields() {
		return resourceAndAssignmentFields;
	}

/**
 * Extract fields that have extra status, and also optionally that have validOnOjbectCreate status
 */
	public static LinkedList<Field> extractExtraFields(Collection<Field> from, final boolean mustBeValidOnObjectCreate) {
		LinkedList<Field> result = new LinkedList<>();
		CollectionUtils.select(from, new Predicate() {
			public boolean evaluate(Object arg0) {
				Field f = (Field)arg0;
				return f.isExtra() && (!mustBeValidOnObjectCreate || f.isValidOnObjectCreate());
			}},result);
		return result;
	}

	public static void addDigesterEvents(Digester digester){
//		digester.addObjectCreate("*/fieldDictionary", "com.microproject.configuration.FieldDictionary");
		digester.addFactoryCreate("*/fieldDictionary", "com.microproject.configuration.FieldDictionaryFactory");
		digester.addSetNext("*/fieldDictionary", "setFieldDictionary", "com.microproject.configuration.FieldDictionary");	//TODO can we do this more easily
	    digester.addSetProperties("*/fieldDictionary/class","name","className"); // object is field dictionary
		digester.addObjectCreate("*/fieldDictionary/class/field", "com.microproject.field.Field");
		digester.addSetProperties("*/fieldDictionary/class/field");
		digester.addSetNext("*/fieldDictionary/class/field", "addField", "com.microproject.field.Field");

		digester.addObjectCreate("*/field/select", "com.microproject.field.StaticSelect"); // create a select
		// NOTE: Select implements java.util.Map, so digester's SetPropertiesRule routes the
		// "name" attribute through commons-beanutils' Map-bean path and calls put("name", <value>)
		// instead of setName(...). That spurious entry became the first dropdown item of every
		// static select (constraintType, taskType, earnedValueMethod, ...). Use an explicit
		// call-method rule so setName is invoked directly and no bogus option is injected.
		digester.addCallMethod("*/field/select", "setName", 1, new Class[] { String.class });
		digester.addCallParam("*/field/select", 0, "name");
		digester.addSetNext("*/field/select", "setSelect", "com.microproject.field.StaticSelect"); // attach to field
		

		digester.addObjectCreate("*/field/choice", "com.microproject.field.DynamicSelect"); // create a choice
		// Use explicit call-method rules instead of addSetProperties: the DynamicSelect
		// choice attributes (list/finder/allowNull) were not applied by SetPropertiesRule,
		// leaving listMethod null and the combo empty. CallMethodRule resolves setters by
		// reflection directly and works regardless of any BeanInfo on Select.
		digester.addCallMethod("*/field/choice", "setList", 1, new Class[] { String.class });
		digester.addCallParam("*/field/choice", 0, "list");
		digester.addCallMethod("*/field/choice", "setFinder", 1, new Class[] { String.class });
		digester.addCallParam("*/field/choice", 0, "finder");
		digester.addCallMethod("*/field/choice", "setAllowNull", 1, new Class[] { boolean.class });
		digester.addCallParam("*/field/choice", 0, "allowNull");
		digester.addSetNext("*/field/choice", "setSelect", "com.microproject.field.DynamicSelect"); // attach to field
		
		digester.addObjectCreate("*/field/select/option","com.microproject.field.SelectOption"); // create an option when seeing one
		digester.addSetProperties("*/field/select/option"); // get key and value properties
		digester.addSetNext("*/field/select/option","addOption","com.microproject.field.SelectOption"); // add option to select

		digester.addObjectCreate("*/field/range","com.microproject.field.Range"); // create an option when seeing one
		digester.addSetProperties("*/field/range"); // get key and value properties
		digester.addSetNext("*/field/range","setRange","com.microproject.field.Range"); // add option to select

		//non intrusive method to reduce role options, otherwise Select should be modified to depend on a specific object
		digester.addObjectCreate("*/field/filter","com.microproject.field.OptionsFilter"); 
		digester.addSetProperties("*/field/filter"); 
		digester.addSetNext("*/field/filter","setFilter","com.microproject.field.OptionsFilter");

		String fieldAccessibleClass = Messages.getMetaString("FieldAccessible");
		digester.addObjectCreate("*/field/permission",fieldAccessibleClass); 
		digester.addSetProperties("*/field/permission"); 
		digester.addSetNext("*/field/permission","setAccessControl","com.microproject.field.FieldAccessible");

	}
	private static void tabbedStringToHtmlRow(StringBuilder result,String colString, boolean header) {
		result.append("<tr>");
		String [] cols= colString.split("\t", -1);
		for (String col : cols)
			result.append(header ? "<th>" : "<td>").append(col).append(header ? "</th>" : "</td>");
		result.append("</tr>");
	}

	private static void fieldsToHtmlTable(final StringBuilder result,String title,Collection<Field> fields) {
		result.append("<p><b>").append(title).append("</b><br />");
		result.append("<table border='1'>");
		tabbedStringToHtmlRow(result,Field.getMetadataStringHeader(),true);
		DataUtils.forAllDo(FieldDictionary.getInstance().getProjectFields().iterator(), new Consumer<Object>() { public void accept(Object arg0) {
				tabbedStringToHtmlRow(result,((Field)arg0).getMetadataString(),false);
			}}
		);
		result.append("</table>");
		result.append("</p>");
	}
	public static void generateFieldDoc(String fileName) {
		StringBuilder result = new StringBuilder();
		result.append("<html><body>");
		fieldsToHtmlTable(result,"Project Fields",FieldDictionary.getInstance().getProjectFields());
		fieldsToHtmlTable(result,"Resource Fields",FieldDictionary.getInstance().getProjectFields());
		fieldsToHtmlTable(result,"Task Fields",FieldDictionary.getInstance().getProjectFields());
		fieldsToHtmlTable(result,"Assignment Fields",FieldDictionary.getInstance().getProjectFields());
		fieldsToHtmlTable(result,"Dependency Fields",FieldDictionary.getInstance().getProjectFields());
		result.append("</body></html>");
		
		try (FileOutputStream fos = new FileOutputStream(fileName)) {
			fos.write(result.toString().getBytes());
		} catch (FileNotFoundException e) {
			logger.log(Level.SEVERE, "Unexpected error", e);
		} catch (IOException e) {
			logger.log(Level.SEVERE, "Unexpected error", e);
		}
	}
	public static void main(String args[]) {
		generateFieldDoc("d:/pod/fields.html");
	}
	
	public static HashMap<String, String> getAliasMap() {
		HashMap<String, String> aliasMap = new HashMap<>();
		MapIterator i = getInstance().map.mapIterator();
		while (i.hasNext()) {
			Object key = i.next();
			Field field = (Field) i.getValue();
			if (field.getAlias() != null)
				aliasMap.put(field.getId(), field.getAlias());
		}
		return aliasMap;
	}

	public static void setAliasMap(HashMap<String, String> aliasMap) {
		if (aliasMap == null)
			return;
		Iterator<String> i = aliasMap.keySet().iterator();
		while (i.hasNext()) {
			String fieldId  = i.next();
			Field f = Configuration.getFieldFromId(fieldId);
			if (f != null)
				f.setAlias(aliasMap.get(fieldId));
		}
	}

}
