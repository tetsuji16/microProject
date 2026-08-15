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
 * and left justified on the top left of the screen adjacent to the File menu. The 
 * logo must be at least 144 x 31 pixels. When users click on the "ProjectLibre" 
 * logo it must direct them back to http://www.projectlibre.com. 
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

import com.microproject.contrib.util.Log;
import com.microproject.contrib.util.LogFactory;
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

/**
 * Dictionary of all fields
 */
public class FieldDictionary {
	private static Log log = LogFactory.getLog(FieldDictionary.class);
	private HashedMap map = new HashedMap();
	private HashMap<String, Field> actionsMap = new HashMap<>();
	public void addField(Field field) {
		if (field.isServer()&&Environment.getStandAlone()) return;
		field.setClass(clazz);
		if (field.build() || true) {
			if (field.isIndexed()) {
				for (int i=0; i < field.getIndexes(); i++) {
					Field indexField = field.createIndexedField(i);
					log.debug("adding indexfield " + clazz.getName() + "." +  indexField.getName() + " id " + indexField.getId() + " field "+ indexField);
					map.put(indexField.getId(),indexField);					
					
				}
			} else {
				log.debug("adding field " + clazz.getName() + "." +  field.getName() + " " + field);
				map.put(field.getId(),field);
				if (field.getAction() != null)
					actionsMap.put(field.getAction(),field);

			}
		} else {
			log.warn("Field not added" + field.getId());
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
			log.error("Unexpected error", e);
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
		String [] cols= colString.split("\t");
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
			log.error("Unexpected error", e);
		} catch (IOException e) {
			log.error("Unexpected error", e);
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
