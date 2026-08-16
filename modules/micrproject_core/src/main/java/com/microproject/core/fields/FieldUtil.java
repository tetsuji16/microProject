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
package com.microproject.core.fields;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.projectlibre.core.configuration.Configuration;
import org.projectlibre.core.dictionary.DictionaryCategory;
import org.projectlibre.core.dictionary.HasStringId;

import com.microproject.pm.tasks.Task;

/**
 * @author Laurent Chretienneau
 *
 */
public class FieldUtil {
	protected static Logger log = Logger.getLogger("FieldUtil");
	public static void convertFields(HasFields hasFields, Class<?> inClass, Object inObject, String[] fieldNames, boolean from){
		for (int i=0;i<fieldNames.length;){
			convertFieldSeries(hasFields, inClass, inObject, fieldNames[i++], -1, -1, fieldNames[i++], -1, -1, -1, fieldNames[i++], from);
		}
	}
	
	protected static void convertFieldSeries(HasFields hasFields, Class<?> inClass, Object inObject, String fieldName1, int startIndex1, int endIndex1, String fieldName2, int startIndex2, int endIndex2, int index, String converterName, boolean from){
//		if (fieldName2.startsWith("customCost")){
//			log.info("convertFieldSeries("+hasFields+", "+fieldName1+", "+startIndex1+", "+endIndex1+", "+fieldName2+", "+startIndex2+", "+endIndex2+", "+index+")");
//		}
		String[] elements1=fieldName1.split(":");
		if (elements1.length==3){
			fieldName1=elements1[0];
			startIndex1=Integer.parseInt(elements1[1]);
			endIndex1=Integer.parseInt(elements1[2]);
			int len=endIndex1-startIndex1+1;
			if (index==-1){
				for (int i=0;i<len;i++)
					convertFieldSeries(hasFields, inClass, inObject, fieldName1, startIndex1, endIndex1, fieldName2, startIndex2, endIndex2, i, converterName, from);
			}else convertFieldSeries(hasFields, inClass, inObject, fieldName1, startIndex1, endIndex1, fieldName2, startIndex2, endIndex2, index, converterName, from);
			return;
		}
		String[] elements2=fieldName2.split(":");
		if (elements2.length==3){
			fieldName2=elements2[0];
			startIndex2=Integer.parseInt(elements2[1]);
			endIndex2=Integer.parseInt(elements2[2]);
			int len=endIndex2-startIndex2+1;
			if (index==-1){
				for (int i=0;i<len;i++)
					convertFieldSeries(hasFields, inClass, inObject, fieldName1, startIndex1, endIndex1, fieldName2, startIndex2, endIndex2, i, converterName, from);
			}else convertFieldSeries(hasFields, inClass, inObject, fieldName1, startIndex1, endIndex1, fieldName2, startIndex2, endIndex2, index, converterName, from);
			return;
		}

		elements1=fieldName1.split(",");
		if (elements1.length==3){
			fieldName1=elements1[0];
			startIndex1=Integer.parseInt(elements1[1]);
			endIndex1=Integer.parseInt(elements1[2]);
			int len=endIndex1-startIndex1+1;
			if (index==-1){
				for (int i=0;i<len;i++)
					convertFieldSeries(hasFields, inClass, inObject, fieldName1, startIndex1, endIndex1, fieldName2, startIndex2, endIndex2, i, converterName, from);
			}else convertFieldSeries(hasFields, inClass, inObject, fieldName1, startIndex1, endIndex1, fieldName2, startIndex2, endIndex2, index, converterName, from);
			return;
		}
		elements2=fieldName2.split(",");
		if (elements2.length==3){
			fieldName2=elements2[0];
			startIndex2=Integer.parseInt(elements2[1]);
			endIndex2=Integer.parseInt(elements2[2]);
			int len=endIndex2-startIndex2+1;
			if (index==-1){
				for (int i=0;i<len;i++)
					convertFieldSeries(hasFields, inClass, inObject, fieldName1, startIndex1, endIndex1, fieldName2, startIndex2, endIndex2, i, converterName, from);
			}else convertFieldSeries(hasFields, inClass, inObject, fieldName1, startIndex1, endIndex1, fieldName2, startIndex2, endIndex2, index, converterName, from);
			return;
		}
		
		convertField(hasFields, inClass, inObject, fieldName1, index==-1? -1: index+startIndex1, fieldName2, index==-1? -1: index+startIndex2, converterName, from);
	}
	
	protected static void convertField(HasFields hasFields, Class<?> inClass, Object inObject, String fieldName1, int index1, String fieldName2, int index2, String converterName, boolean from){ 
		if (index1!=-1)
		 fieldName1+=index1;
		
		//index1 is ignored
		try {
			if (from) {
//				if (index2!=-1)
//					fieldName2+=index1;

				//get value
				Object value;
				if (index2==-1){
					Method m=inClass.getMethod(toGetterMethodName(fieldName2), (Class<?>[])null);
					value=m.invoke(inObject, (Object[])null);
				}else{
					Method m=inClass.getMethod(toGetterMethodName(fieldName2), new Class<?>[]{int.class});
					value=m.invoke(inObject, new Object[]{index2});
				}
				
				if (value==null) return; //skip null values, it will be considered as not set
				
				//convert
				if (converterName!=null){
					if (value==null) return;
					FieldTypeConverter converter = (FieldTypeConverter)Class.forName(converterName).getDeclaredConstructor().newInstance();
					value=converter.convert(value, from);
				}
				
				//set
				hasFields.setPropertyValue(fieldName1, value);
			} else {
				//get value
				Object value=hasFields.getPropertyValue(fieldName1);
				
				if (value==null || 
						((value instanceof Boolean) && ((Boolean)value)==false) )
					return; //skip null values, it will be considered as not set

				//convert
				if (converterName!=null){
					if (value==null) return;
					FieldTypeConverter converter = (FieldTypeConverter)Class.forName(converterName).getDeclaredConstructor().newInstance();
					value=converter.convert(value, from);
				}
				if (value==null) return;
				
				//set
				String methodToFind=toSetterMethodName(fieldName2);
				Method method=findCompatibleSetter(inClass, methodToFind, index2 != -1, value.getClass());
				if (method==null){
					log.info("Method not found: "+hasFields.getClass()+" "+methodToFind);
					return;
				}
				if (index2==-1)
					value=method.invoke(inObject, new Object[]{value});
				else {
					value=method.invoke(inObject, new Object[]{index2, value});
				}
				

				
			}
		} catch (SecurityException e) {
			log.log(java.util.logging.Level.WARNING, "Error", e);
		} catch (IllegalArgumentException e) {
			log.log(java.util.logging.Level.WARNING, "Error", e);
		} catch (ReflectiveOperationException e) {
			log.log(java.util.logging.Level.WARNING, "Error", e);
		}
	}

	private static Method findCompatibleSetter(Class<?> type, String methodName, boolean indexed, Class<?> valueType) {
		Method compatible = null;
		for (Method method : type.getMethods()) {
			if (!method.getName().equals(methodName))
				continue;
			Class<?>[] parameterTypes = method.getParameterTypes();
			int valueParameter = indexed ? 1 : 0;
			if (parameterTypes.length != valueParameter + 1 || indexed && parameterTypes[0] != int.class)
				continue;
			Class<?> parameterType = boxedType(parameterTypes[valueParameter]);
			if (!parameterType.isAssignableFrom(valueType))
				continue;
			if (parameterType == valueType)
				return method;
			if (compatible == null || boxedType(compatible.getParameterTypes()[valueParameter]).isAssignableFrom(parameterType))
				compatible = method;
		}
		return compatible;
	}

	private static Class<?> boxedType(Class<?> type) {
		if (!type.isPrimitive())
			return type;
		if (type == boolean.class) return Boolean.class;
		if (type == byte.class) return Byte.class;
		if (type == short.class) return Short.class;
		if (type == int.class) return Integer.class;
		if (type == long.class) return Long.class;
		if (type == float.class) return Float.class;
		if (type == double.class) return Double.class;
		if (type == char.class) return Character.class;
		return type;
	}
	
	protected static String toGetterMethodName(String s){
		return "get" + s.substring(0, 1).toUpperCase() + s.substring(1);
	}

	protected static String toSetterMethodName(String s){
		return "set" + s.substring(0, 1).toUpperCase() + s.substring(1);
	}
	
	protected static Field[] getDeclaredFields(Class<?> classe){
		Field[] fields=classe.getDeclaredFields();
		return fields;
	}
		
	
	public static com.microproject.core.fields.Field getField(String fieldId, String[] categories){
		com.microproject.core.fields.Field field=null;
		for (String category : categories){
			field=(com.microproject.core.fields.Field)Configuration.getInstance().getDictionary().get(
					new DictionaryCategory(com.microproject.core.fields.Field.class, category),
					fieldId);
			if (field!=null)
				return field;
		}
		return field;
	}
	
	public static Map<String,com.microproject.core.fields.Field> getFields(String[] categories){
		Map<String,com.microproject.core.fields.Field> map=new HashMap<String, com.microproject.core.fields.Field>();
		for (String category : categories){
			Map<String,HasStringId> m=Configuration.getInstance().getDictionary().get(
					new DictionaryCategory(com.microproject.core.fields.Field.class, category));
			if (m!=null)
				for (String key : m.keySet()){
					com.microproject.core.fields.Field existingField=map.get(key);
					if (existingField==null)
						map.put(key, (com.microproject.core.fields.Field)m.get(key));
				}
		}
		return map;
	}

	
	public static String[] getCategories(Class<?> cl){
		Set<String> categorySet=new HashSet<String>();
		List<String> categories=new LinkedList<String>();
		addCategories(cl, categories, categorySet);
		return categories.toArray(new String[categories.size()]);
		
	}
	
	private static void addCategories(Class<?> cl, List<String> categories, Set<String> categorySet){
		categorySet.add(cl.getName());
		categories.add(cl.getName());
		Class<?> superClass=cl.getSuperclass();
		Class<?>[] interfaces=cl.getInterfaces();
		if (superClass!=null &&
				!categorySet.contains(superClass.getName())){
			addCategories(superClass,categories,categorySet);

		}
		for (Class<?> i : interfaces){
			if (!categorySet.contains(i.getName())){
				addCategories(i,categories,categorySet);
			}
		}
	}
		
	

}
